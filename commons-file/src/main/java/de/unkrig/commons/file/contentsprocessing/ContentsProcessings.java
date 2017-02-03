
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;

import de.unkrig.commons.file.CompressUtil;
import de.unkrig.commons.file.CompressUtil.ArchiveHandler;
import de.unkrig.commons.file.CompressUtil.CompressorHandler;
import de.unkrig.commons.file.CompressUtil.NormalContentsHandler;
import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.HardReference;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

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

                combinables.add(contentsProcessor.process(
                    entryPath,          // path
                    archiveInputStream, // inputStream
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
     * Combines the results of {@link ContentsProcessor#process(String, InputStream, long, long, ProducerWhichThrows)}
     * for all archive entries into one object.
     *
     * @param <T> The return type of {@link ContentsProcessor#process(String, InputStream, long, long,
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
     *   Archive files and compressed files are introspected iff {@code lookIntoFormat} evaluates {@code true} for
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
                long                                                              size,
                long                                                              crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {

                return CompressUtil.processStream(
                    path,                                      // path
                    inputStream,                               // inputStream
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


        return new ContentsProcessor<T>() {

            @Override @Nullable public T
            process(
                String path,
                InputStream inputStream,
                long size,
                long crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {

                return pathPredicate.evaluate(path) ? cp.process(path, inputStream, size, crc32, opener) : null;
            }
        };
    }

    /**
     * Returns a {@link ContentsProcessor} which processes a stream by feeding it into the {@code
     * normalContentsProcessor}, but automagically detects various archive formats and compression formats (also
     * nested) and processes the <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the
     * "raw" contents.
     * <p>
     *   Archive streams/entries and compressed streams/entries are introspected iff {@code lookIntoFormat} evaluates
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
                long                                                              size,
                long                                                              crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {
                ContentsProcessor<T> l = loopback.get();
                assert l != null;
                return l.process(name, inputStream, size, crc32, opener);
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
     * entries into the {@code contentsProcessor}.
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
     * Creates and returns a {@link CompressorHandler} which processes an {@link ArchiveInputStream} by feeding its
     * entries to the given {@code contentsProcessor}.
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

                return contentsProcessor.process(
                    path + '!',                                           // path
                    compressorInputStream,                                // inputStream
                    size,                                                 // size
                    -1L,                                                  // crc32
                    new ProducerWhichThrows<InputStream, IOException>() { // opener

                        @Override @Nullable public InputStream
                        produce() throws IOException {
                            return compressionFormat.compressorInputStream(AssertionUtil.notNull(opener.produce()));
                        }
                    }
                );
            }
        };
    }

    /**
     * Creates and returns a {@link TransformerWhichThrows} which processes an {@link InputStream} by feeding it into
     * the given {@code contentsProcessor}.
     *
     * @param size   -1 if unknown
     * @param crc32  See {@link ContentsProcessor#process(String, InputStream, long, long, ProducerWhichThrows)}
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
            handleNormalContents(InputStream inputStream)
            throws IOException {
                return contentsProcessor.process(
                    path,        // path
                    inputStream, // inputStream
                    size,        // size
                    crc32,       // crc32
                    opener       // opener
                );
            }
        };
    }
}
