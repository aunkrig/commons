
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.file.contentsprocessing;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import de.unkrig.commons.file.CompressUtil;
import de.unkrig.commons.file.CompressUtil.ArchiveHandler;
import de.unkrig.commons.file.CompressUtil.CompressorHandler;
import de.unkrig.commons.file.CompressUtil.NormalContentsHandler;
import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.fileprocessing.FileProcessings;
import de.unkrig.commons.file.fileprocessing.FileProcessor;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.HardReference;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.Pattern2;
import de.unkrig.commons.util.concurrent.ConcurrentUtil;
import de.unkrig.commons.util.concurrent.SquadExecutor;

/** {@link ContentsProcessor}-related utility methods. */
public final
class ContentsProcessings {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private ContentsProcessings() {}

    /**
     * @return A {@link ContentsProcessor} that does not read any of the contents, and evaluates to {@code null}.
     */
    @SuppressWarnings("unchecked") public static <T> ContentsProcessor<T>
    nopContentsProcessor() { return (ContentsProcessor<T>) ContentsProcessings.NOP_CONTENTS_PROCESSOR; }

    private static final ContentsProcessor<?> NOP_CONTENTS_PROCESSOR = new ContentsProcessor<Object>() {

        @Override @Nullable public Object
        process(
            String                                                            path,
            InputStream                                                       inputStream,
            @Nullable Date                                                    lastModifiedDate,
            long                                                              size,
            long                                                              crc32,
            ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
        ) { return null; }
    };

    /**
     * @return An {@link ArchiveCombiner} which always returns {@code null}
     */
    @SuppressWarnings("unchecked") public static <T> ArchiveCombiner<T>
    nopArchiveCombiner() { return (ArchiveCombiner<T>) ContentsProcessings.NOP_ARCHIVE_COMBINER; }

    private static final ArchiveCombiner<?>
    NOP_ARCHIVE_COMBINER = new ArchiveCombiner<Object>() {
        @Override @Nullable public Object combine(String archivePath, List<Object> combinables) { return null; }
    };

    /**
     * Process the given <var>archiveInputStream</var> by feeding the contents of each archive entry through the given
     * <var>contentsProcessor</var>.
     * <p>
     *   "Directory archive entries" are silently ignored, because they have no contents and thus it makes no sense to
     *   process them with the <var>contentsProcessor</var>.
     * </p>
     *
     * @param archiveOpener    Re-produces the archive input stream
     * @param exceptionHandler Invoked if the <var>contentsProcessor</var> throws an exception; if it completes
     *                         normally, then processing continues with the next archive entry
     */
    @Nullable public static <T> T
    processArchive(
        String                                                                         archivePath,
        ArchiveInputStream                                                             archiveInputStream,
        ContentsProcessor<T>                                                           contentsProcessor,
        ArchiveCombiner<T>                                                             entryCombiner,
        final ProducerWhichThrows<? extends ArchiveInputStream, ? extends IOException> archiveOpener,
        ExceptionHandler<IOException>                                                  exceptionHandler
    ) throws IOException {

        List<T> combinables = new ArrayList<T>();
        for (ArchiveEntry ae = archiveInputStream.getNextEntry(); ae != null; ae = archiveInputStream.getNextEntry()) {

            if (ae.isDirectory()) continue;

            final String entryName = ArchiveFormatFactory.normalizeEntryName(ae.getName());
            final String entryPath = archivePath + '!' + entryName;

            try {

                ProducerWhichThrows<? extends InputStream, ? extends IOException>
                opener = new ProducerWhichThrows<InputStream, IOException>() {

                    @Override @Nullable public InputStream
                    produce() throws IOException {
                        ArchiveInputStream ais = AssertionUtil.notNull(archiveOpener.produce());
                        for (ArchiveEntry ae2 = ais.getNextEntry(); ae2 != null; ae2 = ais.getNextEntry()) {
                            String name2 = ArchiveFormatFactory.normalizeEntryName(ae2.getName());
                            if (name2.equals(entryName)) return ais;
                        }
                        ais.close();
                        throw new IOException(entryPath);
                    }
                };

                @SuppressWarnings("deprecation") long crc32 = ArchiveFormatFactory.getEntryCrc32(ae);

                Date lastModifiedDate;
                try {
                    lastModifiedDate = ae.getLastModifiedDate();
                } catch (UnsupportedOperationException uoe) {
                    lastModifiedDate = null;
                }

                combinables.add(contentsProcessor.process(
                    entryPath,          // path
                    archiveInputStream, // inputStream
                    lastModifiedDate,   // lastModifiedDate
                    ae.getSize(),       // size
                    crc32,              // crc32
                    opener              // opener
                ));
            } catch (IOException ioe) {
                exceptionHandler.handle(entryPath, ioe);
            } catch (RuntimeException re) {
                exceptionHandler.handle(entryPath, re);
            }
        }

        return entryCombiner.combine(archivePath, combinables);
    }

    /**
     * Combines the results of {@link ContentsProcessor#process(String, InputStream, Date, long, long,
     * ProducerWhichThrows)} for all archive entries into one object.
     *
     * @param <T> The return type of {@link ContentsProcessor#process(String, InputStream, Date, long, long,
     *            ProducerWhichThrows)}
     */
    public
    interface ArchiveCombiner<T> {

        /**
         * Is invoked after all entries of an archive have been processed.
         */
        @Nullable T
        combine(String archivePath, List<T> combinables);
    }

    /**
     * Returns a {@link ContentsProcessor} which processes contents by feeding it to the {@code
     * normalContentsProcessor}, but automagically detects various archive and compression formats and processes the
     * <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the "raw" contents.
     * <p>
     *   Archive files and compressed files are introspected iff <var>lookIntoFormat</var> evaluates {@code true} for
     *   "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static <T> ContentsProcessor<T>
    compressedAndArchiveContentsProcessor(
        final Predicate<? super String>     lookIntoFormat,
        final Predicate<? super String>     pathPredicate,
        final ContentsProcessor<T>          archiveContentsProcessor,
        final ArchiveCombiner<T>            archiveEntryCombiner,
        final ContentsProcessor<T>          compressedContentsProcessor,
        final ContentsProcessor<T>          normalContentsProcessor,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        final ContentsProcessor<T> cp = new ContentsProcessor<T>() {

            @Override @Nullable public T
            process(
                final String                                                      path,
                InputStream                                                       inputStream,
                @Nullable Date                                                    lastModifiedDate,
                long                                                              size,
                long                                                              crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {

                return CompressUtil.processStream(
                    path,                                      // path
                    inputStream,                               // inputStream
                    lastModifiedDate,                          // lastModifiedDate
                    lookIntoFormat,                            // lookIntoFormat
                    ContentsProcessings.archiveHandler(        // archiveHandler
                        path,
                        archiveContentsProcessor,
                        archiveEntryCombiner,
                        opener,
                        exceptionHandler
                    ),
                    ContentsProcessings.compressorHandler(     // compressorHandler
                        path,
                        compressedContentsProcessor,
                        opener
                    ),
                    ContentsProcessings.normalContentsHandler( // normalContentsHandler
                        path,
                        normalContentsProcessor,
                        size,
                        crc32,
                        opener
                    )
                );
            }

            @Override public String
            toString() { return "compressedAndArchiveContentsProcessor"; }
        };

        return ContentsProcessings.select(pathPredicate, cp);
    }

    /**
     * Wraps the <var>delegate</var> in a {@link SelectiveContentsProcessor}.
     */
    public static <T> ContentsProcessor<T>
    select(Predicate<? super String> pathPredicate, ContentsProcessor<T> delegate) {
        return ContentsProcessings.select(pathPredicate, delegate, null);
    }

    /**
     * Wraps the <var>delegate</var> in a {@link SelectiveContentsProcessor}.
     */
    public static <T> ContentsProcessor<T>
    select(
        Predicate<? super String>      pathPredicate,
        ContentsProcessor<T>           trueCp,
        @Nullable ContentsProcessor<T> falseCp
    ) {

        if (falseCp == null) falseCp = ContentsProcessings.nopContentsProcessor();

        if (pathPredicate == PredicateUtil.always()) return trueCp;
        if (pathPredicate == PredicateUtil.never())  return falseCp;

        return new SelectiveContentsProcessor<T>(pathPredicate, trueCp, falseCp);
    }

    /**
     * Returns a {@link ContentsProcessor} which processes a stream by feeding it into the {@code
     * normalContentsProcessor}, but automagically detects various archive formats and compression formats (also
     * nested) and processes the <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the
     * "raw" contents.
     * <p>
     *   Archive streams/entries and compressed streams/entries are introspected iff <var>lookIntoFormat</var> evaluates
     *   {@code true} for "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static <T> ContentsProcessor<T>
    recursiveCompressedAndArchiveContentsProcessor(
        final Predicate<? super String> lookIntoFormat,
        final Predicate<? super String> pathPredicate,
        final ArchiveCombiner<T>        archiveEntryCombiner,
        final ContentsProcessor<T>      normalContentsProcessor,
        ExceptionHandler<IOException>   exceptionHandler
    ) {
        final HardReference<ContentsProcessor<T>> loopback = new HardReference<ContentsProcessor<T>>();

        // To implement the "feedback look" it is necessary to create a temporary contents Processor.
        ContentsProcessor<T> tmp = new ContentsProcessor<T>() {

            @Override @Nullable public T
            process(
                String                                                            name,
                InputStream                                                       inputStream,
                @Nullable Date                                                    lastModifiedDate,
                long                                                              size,
                long                                                              crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {
                ContentsProcessor<T> l = loopback.get();
                assert l != null;
                return l.process(name, inputStream, lastModifiedDate, size, crc32, opener);
            }
        };

        ContentsProcessor<T> result = ContentsProcessings.compressedAndArchiveContentsProcessor(
            lookIntoFormat,          // lookIntoFormat
            pathPredicate,           // pathPredicate
            tmp,                     // archiveContentsProcessor
            archiveEntryCombiner,    // archiveEntryCombiner
            tmp,                     // compressedContentsProcessor
            normalContentsProcessor, // normalContentsProcessor
            exceptionHandler         // exceptionHandler
        );

        loopback.set(result);

        return result;
    }

    /**
     * Creates and returns an {@link ArchiveHandler} which processes {@link ArchiveInputStream}s by feeding their
     * entries into the <var>contentsProcessor</var>.
     *
     * @param opener Will be invoked later when the returned archive handler processes a concrete archive
     */
    public static <T> ArchiveHandler<T>
    archiveHandler(
        final String                                                            path,
        final ContentsProcessor<T>                                              contentsProcessor,
        final ArchiveCombiner<T>                                                archiveEntryCombiner,
        final ProducerWhichThrows<? extends InputStream, ? extends IOException> opener,
        final ExceptionHandler<IOException>                                     exceptionHandler
    ) {

        return new ArchiveHandler<T>() {

            @Override @Nullable public T
            handleArchive(
                final ArchiveInputStream archiveInputStream,
                final ArchiveFormat      archiveFormat
            ) throws IOException {

                return ContentsProcessings.processArchive(
                    path,                                                        // archivePath
                    archiveInputStream,                                          // ArchiveInputStream
                    contentsProcessor,                                           // contentsProcessor
                    archiveEntryCombiner,                                        // entryCombiner
                    new ProducerWhichThrows<ArchiveInputStream, IOException>() { // opener

                        @Override @Nullable public ArchiveInputStream
                        produce() throws IOException {
                            try {
                                InputStream is = opener.produce();
                                assert is != null;
                                return archiveFormat.archiveInputStream(is);
                            } catch (ArchiveException ae) {
                                throw ExceptionUtil.wrap(null, ae, IOException.class);
                            }
                        }
                    },
                    exceptionHandler                                             // exceptionHandler
                );
            }
        };
    }

    /**
     * Creates and returns a {@link CompressorHandler} which processes an {@link CompressorInputStream} by feeding its
     * contents to the given <var>contentsProcessor</var>.
     *
     * @param opener Re-produces the input stream
     */
    public static <T> CompressorHandler<T>
    compressorHandler(
        final String                                                            path,
        final ContentsProcessor<T>                                              contentsProcessor,
        final ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
    ) {

        return new CompressorHandler<T>() {

            @Override @Nullable public T
            handleCompressor(
                CompressorInputStream   compressorInputStream,
                final CompressionFormat compressionFormat
            ) throws IOException {

                @SuppressWarnings("deprecation") long
                size = CompressionFormatFactory.getUncompressedSize(compressorInputStream);

                try {
                    return contentsProcessor.process(
                        path + '%',                                           // path
                        compressorInputStream,                                // inputStream
                        null,                                                 // lastModifiedDate
                        size,                                                 // size
                        -1L,                                                  // crc32
                        new ProducerWhichThrows<InputStream, IOException>() { // opener

                            @Override @Nullable public InputStream
                            produce() throws IOException {
                                return compressionFormat.compressorInputStream(AssertionUtil.notNull(opener.produce()));
                            }
                        }
                    );
                } catch (EOFException eofe) {
                    if (CompressorStreamFactory.GZIP.equals(compressionFormat.getName())) {
                        eofe = ExceptionUtil.wrap(
                            (
                                "Maybe a \"normal\" file was accidentially detected as gzip-compressed; "
                                + "consider using \"--look-into '~gz:***'\""
                            ),
                            eofe
                        );
                    }
                    throw eofe;
                }
            }
        };
    }

    /**
     * Creates and returns a {@link TransformerWhichThrows} which processes an {@link InputStream} by feeding it into
     * the given <var>contentsProcessor</var>.
     *
     * @param size   -1 if unknown
     * @param crc32  See {@link ContentsProcessor#process(String, InputStream, Date, long, long, ProducerWhichThrows)}
     * @param opener Re-produces the
     */
    public static <T> NormalContentsHandler<T>
    normalContentsHandler(
        final String                                                            path,
        final ContentsProcessor<T>                                              contentsProcessor,
        final long                                                              size,
        final long                                                              crc32,
        final ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
    ) {

        return new NormalContentsHandler<T>() {

            @Override @Nullable public T
            handleNormalContents(InputStream inputStream, @Nullable Date lastModifiedDate)
            throws IOException {
                return contentsProcessor.process(
                    path,             // path
                    inputStream,      // inputStream
                    lastModifiedDate, // lastModifiedDate
                    size,             // size
                    crc32,            // crc32
                    opener            // opener
                );
            }
        };
    }

    /**
     * Expands the given <var>pattern</var> (which is typeically created with {@link Pattern2#compile(String, int)})
     * to a set of contents, which are passed to the <var>fp</var>. Notice that directories, archives and compressed
     * files are <em>not</em> contents.
     * <p>
     *   Examples:
     * </p>
     * <table border="1">
     *   <tr>
     *     <td>{@code "*.c"}</td>
     *     <td>{@code "foo.c", "bar.c"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/dir/*"}</td>
     *     <td>{@code "C:/dir/file"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/dir/**"}</td>
     *     <td>{@code "C:/dir/file" "C:/dir/subdir/file"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/dir/*}{@code /*"}</td>
     *     <td>{@code "C:/dir/subdir/file"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/dir**"}</td>
     *     <td>{@code "C:/dir/file" "C:/dir/subdir/file"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/file.zip!*"}</td>
     *     <td>{@code "C:/file.zip!file"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/file.gz"}</td>
     *     <td>{@code "C:/file.gz%"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code "C:/***file"}</td>
     *     <td>{@code "C:/file" "C:/file.zip!file" "C:/file.tgz/file" "C:/file.gz%"}</td>
     *   </tr>
     * </table>
     */
    public static void
    glob(final Pattern pattern, final ContentsProcessor<Void> cp) throws IOException, InterruptedException {

        File sf = FileProcessings.starterFile(pattern.pattern().replace("[/\\\\]", "/"));

        Predicate<String> pathPredicate = Glob.compileRegex(pattern);

        FileProcessor<Void> fp = FileProcessings.recursiveCompressedAndArchiveFileProcessor(
            PredicateUtil.always(),                              // lookIntoFormat
            pathPredicate,                                       // pathPredicate
            ContentsProcessings.<Void>nopArchiveCombiner(),      // archiveEntryCombiner
            new SelectiveContentsProcessor<Void>(                // regularFileProcessor
                pathPredicate,
                cp,
                ContentsProcessings.<Void>nopContentsProcessor()
            ),
            ExceptionHandler.<IOException>defaultHandler()       // exceptionHandler
        );
        fp = FileProcessings.directoryTreeProcessor(
            pathPredicate,                                                       // pathPredicate
            fp,                                                                  // regularFileProcessor
            Collator.getInstance(),                                              // directoryMemberNameComparator
            FileProcessings.<Void>nopDirectoryCombiner(),                        // directoryCombiner
            false,                                                               // includeDirs
            new SquadExecutor<Void>(ConcurrentUtil.SEQUENTIAL_EXECUTOR_SERVICE), // squadExecutor
            ExceptionHandler.<IOException>defaultHandler()                       // exceptionHandler
        );

        if (sf != null) {
            fp.process(sf.getPath(), sf);
        } else {
            fp.process("", new File("."));
        }
    }
}
