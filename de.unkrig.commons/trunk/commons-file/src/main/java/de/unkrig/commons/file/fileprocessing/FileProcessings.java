
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

package de.unkrig.commons.file.fileprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;

import de.unkrig.commons.file.CompressUtil;
import de.unkrig.commons.file.CompressUtil.ArchiveHandler;
import de.unkrig.commons.file.CompressUtil.CompressorHandler;
import de.unkrig.commons.file.CompressUtil.NormalContentsHandler;
import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings.ArchiveCombiner;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessor;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.HardReference;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.concurrent.ConcurrentUtil;
import de.unkrig.commons.util.concurrent.SquadExecutor;

/**
 * Various file processing utility methods.
 */
public final
class FileProcessings {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final Logger LOGGER = Logger.getLogger(FileProcessings.class.getName());

    private
    FileProcessings() {}

    /**
     * @return A {@link FileProcessor} that does <i>nothing</i> and returns {@code null}.
     */
    @SuppressWarnings("unchecked") public static <T> FileProcessor<T>
    nop() { return (FileProcessor<T>) FileProcessings.NOP_FILE_PROCESSOR; }

    private static final FileProcessor<?> NOP_FILE_PROCESSOR = new FileProcessor<Object>() {
        @Override @Nullable public Object process(String path, File in) { return null;  }
        @Override public String           toString()                    { return "NOP"; }
    };

    /**
     * @see #process(List, FileProcessor, ExceptionHandler)
     */
    public static <T> void
    process(List<File> files, FileProcessor<T> fileProcessor) throws IOException, InterruptedException {
        FileProcessings.process(files, fileProcessor, ExceptionHandler.<IOException>defaultHandler());
    }

    /**
     * Invokes the <var>fileProcessor</var> for each of the <var>files</var>.
     *
     * @param exceptionHandler Called if the processing of one of the <var>files</var> throws an {@link IOException}
     *                         or a {@link RuntimeException}
     */
    public static <T> void
    process(List<File> files, FileProcessor<T> fileProcessor, ExceptionHandler<IOException> exceptionHandler)
    throws IOException, InterruptedException {
        for (File file : files) {

            String path = file.getPath();

            try {
                fileProcessor.process(path, file);
            } catch (IOException ioe) {
                exceptionHandler.handle(path, ioe);
            } catch (RuntimeException re) {
                exceptionHandler.handle(path, re);
            }
        }
    }

    /**
     * Returns a {@link FileProcessor} which processes directories and regular files exactly like the {@link
     * FileProcessor} returned by {@link #directoryProcessor(Predicate, FileProcessor, Comparator, FileProcessor,
     * DirectoryCombiner, SquadExecutor, ExceptionHandler)}, except that it processes directory members
     * <i>recursively</i>.
     * <p>
     *   Notice that the list passed to the <var>directoryCombiner</var> can contain {@code null} values iff the
     *   <var>regularFileProcessor</var> returns {@code null} values.
     * </p>
     *
     * @param directoryMemberNameComparator The comparator used to sort a directory's members; a {@code null} value
     *                                      means to NOT sort the members, i.e. leave them in their 'natural' order as
     *                                      {@link File#list()} returns them
     * @see                                 #directoryProcessor(Predicate, FileProcessor, Comparator, FileProcessor,
     *                                      DirectoryCombiner, SquadExecutor, ExceptionHandler)
     */
    public static <T> FileProcessor<T>
    directoryTreeProcessor(
        Predicate<? super String>      pathPredicate,
        FileProcessor<T>               regularFileProcessor,
        @Nullable Comparator<Object>   directoryMemberNameComparator,
        DirectoryCombiner<T>           directoryCombiner,
        SquadExecutor<T>               squadExecutor,
        ExceptionHandler<IOException>  exceptionHandler
    ) {

        final HardReference<FileProcessor<T>> loopback = new HardReference<FileProcessor<T>>();

        FileProcessor<T> directoryMemberProcessor = new FileProcessor<T>() {

            @Override @Nullable public T
            process(String path, File file) throws IOException, InterruptedException {
                FileProcessor<T> fp = loopback.get();
                assert fp != null;
                return fp.process(path, file);
            }
        };

        FileProcessor<T> result = FileProcessings.directoryProcessor(
            pathPredicate,
            regularFileProcessor,
            directoryMemberNameComparator,
            directoryMemberProcessor,
            directoryCombiner,
            squadExecutor,
            exceptionHandler
        );

        loopback.set(result);

        return result;
    }

    /**
     * Returns a {@link FileProcessor} which processes directories and regular files.
     * <p>
     *   Its behavior is as follows:
     * </p>
     * <ul>
     *   <li>
     *     Iff the subject file is not a directory, the <var>regularFileProcessor</var> is invoked and its result is
     *     returned.
     *   </li>
     *   <li>
     *     Otherwise the subject file is a directory.
     *     <ul>
     *       <li>
     *         Iff, according to the <var>pathPredicate</var>, the directory can impossibly contain relevant documents,
     *         {@code null} is returned.
     *       </li>
     *       <li>Otherwise, the directory members are listed.</li>
     *       <li>
     *         Iff the <var>directoryMemberNameComparator</var> is not {@code null}, the members are sorted according
     *         to the <var>directoryMemberNameComparator</var>
     *       </li>
     *       <li>
     *         The <var>directoryMemberProcessor</var> is invoked for each member, and the return values are stored in
     *         a list.
     *       </li>
     *       <li>
     *         The <var>directoryCombiner</var> is invoked with that list. (Notice that the list may contain {@code
     *         null} values iff the <var>directoryMemberProcessor</var> returns {@code null} values.)
     *       </li>
     *       <li>
     *         The value produced by the <var>directoryCombiner</var> is returned.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     *   Notice that the <var>pathPredicate</var> is only used to avoid unnecessary directory scans; apart from that
     *   the <var>regularFileProcessor</var> and the <var>directoryMemberProcessor</var> are called for <i>any</i> file
     *   without further ado.
     * </p>
     * <p>
     *   If you use {@link Void} for {@code <T>}, then {@link #nopDirectoryCombiner()} is the right {@code
     *   directoryCombiner}.
     * </p>
     *
     * @param <T>                           The return type of all {@link FileProcessor#process(String, File)} methods
     * @param squadExecutor                 Is used to process independent subtrees - could be {@link
     *                                      ConcurrentUtil#SEQUENTIAL_EXECUTOR_SERVICE}
     * @param directoryMemberNameComparator The comparator used to sort a directory's members; a {@code null} value
     *                                      means to NOT sort the members, i.e. leave them in their 'natural' order as
     *                                      {@link File#list()} returns them
     */
    public static <T> FileProcessor<T>
    directoryProcessor(
        final Predicate<? super String>      pathPredicate,
        final FileProcessor<T>               regularFileProcessor,
        @Nullable final Comparator<Object>   directoryMemberNameComparator,
        final FileProcessor<T>               directoryMemberProcessor,
        final DirectoryCombiner<T>           directoryCombiner,
        final SquadExecutor<T>               squadExecutor,
        final ExceptionHandler<IOException>  exceptionHandler
    ) {

        final FileProcessor<T> dp = new FileProcessor<T>() {

            /**
             * @throws IOException <var>directory</var> is not a listable directory
             * @see                #DirectoryProcessor(Comparator, FileProcessor, DirectoryCombiner, ExceptionHandler)
             */
            @Override @Nullable public T
            process(final String directoryPath, final File directory) throws IOException, InterruptedException {

                FileProcessings.LOGGER.log(
                    Level.FINER,
                    "Processing directory \"{0}\" (path is \"{1}\")",
                    new Object[] { directory, directoryPath }
                );

                String[] memberNames = directory.list();
                if (memberNames == null) throw new IOException("'" + directory + "' is not a listable directory");

                if (directoryMemberNameComparator != null) Arrays.sort(memberNames, directoryMemberNameComparator);

                // Submit callables that do the actual work for each member.
                List<Future<T>> futures = new ArrayList<Future<T>>(memberNames.length);
                for (final String memberName : memberNames) {

                    futures.add(squadExecutor.submit(new Callable<T>() {

                        @Override @Nullable public T
                        call() throws IOException, InterruptedException {

                            String memberPath = directoryPath + File.separatorChar + memberName;

                            try {
                                return directoryMemberProcessor.process(memberPath, new File(directory, memberName));
                            } catch (IOException ioe) {
                                exceptionHandler.handle(memberPath, ioe);
                            } catch (RuntimeException re) {
                                exceptionHandler.handle(memberPath, re);
                            }
                            return null;
                        }
                    }));
                }

                // Now wait until the callables complete and pick their results.
                final List<T> combinables = new ArrayList<T>(memberNames.length);
                for (Future<T> future : futures) {
                    try {
                        combinables.add(future.get());
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        if (cause instanceof IOException) {
                            throw (IOException) cause; // SUPPRESS CHECKSTYLE AvoidHidingCause
                        }
                        throw new IllegalStateException(ee);
                    }
                }

                // Now call the "directory combiner" with the directory members' results.
                return directoryCombiner.combine(directoryPath, directory, combinables);
            }
        };

        return new FileProcessor<T>() {

            @Override @Nullable public T
            process(String path, File file) throws IOException, InterruptedException {

                if (file.isDirectory()) {
                    return pathPredicate.evaluate(path + '/') ? dp.process(path, file)                   : null;
                } else {
                    return pathPredicate.evaluate(path)       ? regularFileProcessor.process(path, file) : null;
                }
            }
        };
    }

    /**
     * Transforms the return values of the invocations of <var>directoryMemberProcessor</var> for each member.
     *
     * @param <T> The return type of {@link FileProcessor#process(String, File)}
     */
    public
    interface DirectoryCombiner<T> {

        /** @see DirectoryCombiner */
        @Nullable T combine(String directoryPath, File directory, List<T> combinables);
    }

    /**
     * A {@link DirectoryCombiner} which ignores the combinables and returns {@code null}.
     */
    @SuppressWarnings("unchecked") public static <T> DirectoryCombiner<T>
    nopDirectoryCombiner() { return (DirectoryCombiner<T>) FileProcessings.NOP_DIRECTORY_COMBINER; }

    private static final DirectoryCombiner<?> NOP_DIRECTORY_COMBINER = new DirectoryCombiner<Object>() {

        @Override @Nullable public Object
        combine(String directoryPath, File directory, List<Object> combinables) { return null; }
    };




    /**
     * @deprecated Was renamed to {@link FileProcessings#compressedAndArchiveFileProcessor}
     */
    @Deprecated public static <T> FileProcessor<T>
    archiveCompressedAndNormalFileProcessor(
        final Predicate<? super String>     lookIntoFormat,
        final ContentsProcessor<T>          archiveContentsProcessor,
        final ArchiveCombiner<T>            archiveEntryCombiner,
        final ContentsProcessor<T>          compressedContentsProcessor,
        final ContentsProcessor<T>          normalContentsProcessor,
        final ExceptionHandler<IOException> exceptionHandler
    ) {
        return FileProcessings.compressedAndArchiveFileProcessor(
            lookIntoFormat,
            PredicateUtil.<String>always(), // pathPredicate
            archiveContentsProcessor,
            archiveEntryCombiner,
            compressedContentsProcessor,
            normalContentsProcessor,
            exceptionHandler
        );
    }

    /**
     * Returns a {@link FileProcessor} which processes files by feeding their contents either to the {@code
     * archiveContentsProcessor}, the <var>compressedContentsProcessor</var> or the <var>normalContentsProcessor</var>.
     * <p>
     *   Archive files and compressed files are introspected iff <var>lookIntoFormat</var> evaluates {@code true} for
     *   "<i>format</i><b>:</b><i>path</i>".
     * </p>
     *
     * @param lookIntoFormat   See {@link CompressUtil#processFile(String, File, Predicate, ArchiveHandler,
     *                         CompressorHandler, NormalContentsHandler)}
     */
    public static <T> FileProcessor<T>
    compressedAndArchiveFileProcessor(
        final Predicate<? super String>     lookIntoFormat,
        Predicate<? super String>           pathPredicate,
        final ContentsProcessor<T>          archiveContentsProcessor,
        final ArchiveCombiner<T>            archiveEntryCombiner,
        final ContentsProcessor<T>          compressedContentsProcessor,
        final ContentsProcessor<T>          normalContentsProcessor,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return FileProcessings.select(pathPredicate, new FileProcessor<T>() {

            @Override @Nullable public T
            process(final String path, final File file) throws FileNotFoundException, IOException {

                return CompressUtil.<T>processFile(
                    path,
                    file,                                     // file
                    lookIntoFormat,                           // lookIntoFormat
                    FileProcessings.<T>archiveHandler(        // archiveHandler
                        path,
                        archiveContentsProcessor,
                        archiveEntryCombiner,
                        file,
                        exceptionHandler
                    ),
                    FileProcessings.<T>compressorHandler(     // compressorHandler
                        path,
                        compressedContentsProcessor,
                        file
                    ),
                    FileProcessings.<T>normalContentsHandler( // normalContentsHandler
                        path,
                        normalContentsProcessor,
                        file
                    )
                );
            }

            @Override public String
            toString() { return "compressedAndArchiveFileProcessor"; }
        });
    }

    /**
     * Wraps the <var>delegate</var> in a {@link SelectiveFileProcessor}.
     */
    public static <T> FileProcessor<T>
    select(Predicate<? super String> pathPredicate, FileProcessor<T> delegate) {
        return FileProcessings.select(pathPredicate, delegate, null);
    }

    /**
     * Wraps the <var>delegate</var> in a {@link SelectiveFileProcessor}.
     */
    public static <T> FileProcessor<T>
    select(Predicate<? super String> pathPredicate, FileProcessor<T> trueFp, @Nullable FileProcessor<T> falseFp) {

        if (falseFp == null) falseFp = FileProcessings.nop();

        if (pathPredicate == PredicateUtil.always()) return trueFp;
        if (pathPredicate == PredicateUtil.never())  return falseFp;

        return new SelectiveFileProcessor<T>(pathPredicate, trueFp, falseFp);
    }

    /**
     * Returns a {@link FileProcessor} which processes files by feeding their contents through the <var>delegate</var>
     * (just like the {@link FileContentsProcessor}), but automagically detects various archive and compression formats
     * (also nested) and processes the <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of
     * the "raw" contents.
     * <p>
     *   Archive files/entries and compressed files/entries are introspected iff <var>lookIntoFormat</var> evaluates
     *   {@code true} for "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static <T> FileProcessor<T>
    recursiveCompressedAndArchiveFileProcessor(
        final Predicate<? super String>     lookIntoFormat,
        final Predicate<? super String>     pathPredicate,
        ArchiveCombiner<T>                  archiveEntryCombiner,
        final ContentsProcessor<T>          delegate,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        ContentsProcessor<T> tmp = ContentsProcessings.recursiveCompressedAndArchiveContentsProcessor(
            lookIntoFormat,
            pathPredicate,
            archiveEntryCombiner,
            delegate,
            exceptionHandler
        );

        return FileProcessings.archiveCompressedAndNormalFileProcessor(
            lookIntoFormat,       // lookIntoFormat
            tmp,                  // archiveContentsProcessor
            archiveEntryCombiner, // archiveEntryCombiner
            tmp,                  // compressedContentsProcessor
            delegate,             // normalContentsProcessor
            exceptionHandler      // exceptionHandler
        );
    }

    private static <T> ArchiveHandler<T>
    archiveHandler(
        final String                        path,
        final ContentsProcessor<T>          contentsProcessor,
        final ArchiveCombiner<T>            archiveEntryCombiner,
        final File                          archiveFile,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return new ArchiveHandler<T>() {

            @Nullable @Override public T
            handleArchive(
                final ArchiveInputStream                             archiveInputStream,
                final ArchiveFormat                                  archiveFormat
            ) throws IOException {

                return ContentsProcessings.processArchive(
                    path,                                                        // archivePath
                    archiveInputStream,                                          // archiveInputStream
                    contentsProcessor,                                           // contentsProcessor
                    archiveEntryCombiner,                                        // archiveEntryCombiner
                    new ProducerWhichThrows<ArchiveInputStream, IOException>() { // archiveOpener

                        @Override @Nullable public ArchiveInputStream
                        produce() throws IOException {
                            try {
                                return archiveFormat.open(archiveFile);
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

    private static <T> CompressorHandler<T>
    compressorHandler(
        final String               path,
        final ContentsProcessor<T> contentsProcessor,
        final File                 compressedFile
    ) {

        return new CompressorHandler<T>() {

            @Nullable @Override public T
            handleCompressor(CompressorInputStream compressorInputStream, final CompressionFormat compressionFormat)
            throws IOException {

                @SuppressWarnings("deprecation") long
                uncompressedSize = CompressionFormatFactory.getUncompressedSize(compressorInputStream);

                return contentsProcessor.process(
                    path + '%',                                           // path
                    compressorInputStream,                                // compressorInputStream
                    uncompressedSize,                                     // size
                    -1L,                                                  // crc32
                    new ProducerWhichThrows<InputStream, IOException>() { // opener

                        @Override @Nullable public InputStream
                        produce() throws IOException { return compressionFormat.open(compressedFile); }
                    }
                );
            }
        };
    }

    private static <T> NormalContentsHandler<T>
    normalContentsHandler(final String path, final ContentsProcessor<T> contentsProcessor, final File normalFile) {

        return new NormalContentsHandler<T>() {

            @Override @Nullable public T
            handleNormalContents(InputStream inputStream)
            throws IOException {
                return contentsProcessor.process(
                    path,                                                 // path
                    inputStream,                                          // inputStream
                    -1L,                                                  // size
                    -1L,                                                  // crc32
                    new ProducerWhichThrows<InputStream, IOException>() { // opener

                        @Override @Nullable public InputStream
                        produce() throws IOException { return new FileInputStream(normalFile); }
                    }
                );
            }
        };
    }
}
