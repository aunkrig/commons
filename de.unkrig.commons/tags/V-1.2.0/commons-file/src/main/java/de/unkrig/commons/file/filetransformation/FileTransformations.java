
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.file.filetransformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;

import de.unkrig.commons.file.CompressUtil;
import de.unkrig.commons.file.CompressUtil.ArchiveHandler;
import de.unkrig.commons.file.CompressUtil.CompressorHandler;
import de.unkrig.commons.file.CompressUtil.NormalContentsHandler;
import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.file.contentstransformation.ContentsTransformations;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Glob;

/**
 * Various {@link FileTransformer}-related utility methods.
 * <p>
 *   A typical application is to wrap file transformers and content transformers as follows:
 * </p>
 * <pre>
 *              |
 *              v
 *     directoryTreeTransformer()
 *              |
 *              v
 *     recursiveCompressedAndArchiveFileTransformer()
 *              |
 *              v
 *     (some contents fransformer, e.g. 'ContentsTransformerUtil.NOP')
 * </pre>
 * <p>
 *   The {@link #directoryTreeTransformer(Comparator, Predicate, Glob, DirectoryCombiner, FileTransformer, boolean,
 *   boolean, ExceptionHandler) directoryTreeTransformer())} can be left out if you don't need recursive directory
 *   traversal.
 * </p>
 * <p>
 *   The {@link #recursiveCompressedAndArchiveFileTransformer(Predicate, Predicate, Glob, ArchiveCombiner,
 *   ContentsTransformer, boolean, ExceptionHandler) recursiveCompressedAndArchiveFileTransformer()} can be left out if
 *   you don't want to look into archives nor compressed files.
 * </p>
 */
public final
class FileTransformations {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * If <var>in</var> equals <var>out</var> ("in-place transformation"), then file <var>in</var> is deleted. Otherwise
     * ("out-of-place transformation") nothing is done, i.e. a file <var>out</var> is <i>not</i> created.
     */
    public static FileTransformer
    remove() {

        return new FileTransformer() {

            @Override public void
            transform(String path, File in, File out, Mode mode) throws IOException {

                switch (mode) {

                case CHECK:
                    throw FileTransformer.NOT_IDENTICAL;

                case CHECK_AND_TRANSFORM:
                    throw AssertionUtil.<Error>fail("Must not invoke 'remove()' in mode CHECK_AND_TRANSFORM");

                case TRANSFORM:
                    if (in.equals(out)) {
                        if (!in.delete()) throw new IOException("Could not delete '" + in + "'");
                    }
                    return;

                default:
                    throw AssertionUtil.<Error>fail("Unexpected mode '" + mode + "'");
                }
            }
        };
    }

    /**
     * If <var>in</var> equals <var>out</var> ("in-place transformation"), then file is left untouched. Otherwise
     * ("out-of-place transformation") <var>in</var> is copied byte-by-byte to <var>out</var>.
     */
    public static final FileTransformer
    UNCHANGED = new FileTransformer() {

        @Override public void
        transform(String path, File in, File out, Mode mode) throws IOException {

            switch (mode) {

            case CHECK:
                return;

            case CHECK_AND_TRANSFORM:
            case TRANSFORM:
                if (!in.equals(out)) IoUtil.copy(in, out);
                return;

            default:
                throw AssertionUtil.<Error>fail("Unexpected mode '" + mode + "'");
            }
        }
    };

    private
    FileTransformations() {}

    /**
     * @see DirectoryCombiner#combineDirectory(String, ConsumerWhichThrows)
     * @see ArchiveCombiner#combineArchive(String, ConsumerWhichThrows)
     */
    public
    interface NameAndContents {

        /** @return A simple name (must not contain separators) */
        String getName();

        /**
         * @return A stream producing the contents
         * @throws FileNotFoundException
         */
        InputStream open() throws FileNotFoundException;
    }

    /** @see #combineDirectory(String, ConsumerWhichThrows) */
    public
    interface DirectoryCombiner {

        /**
         * Is invoked after the members of the directory have been transformed.
         * <p>
         *   The name of the interface and this method is historical; actually nothing is "combined".
         * </p>
         *
         * @param directoryPath The path designating the directory being transformed
         * @param memberAdder   Can be called to add members to the output directory
         */
        void
        combineDirectory(
            String                                                              directoryPath,
            ConsumerWhichThrows<? super NameAndContents, ? extends IOException> memberAdder
        ) throws IOException;

        /**
         * An {@link DirectoryCombiner} that does <i>nothing</i>.
         */
        DirectoryCombiner NOP = new DirectoryCombiner() {

            @Override public void
            combineDirectory(
                String                                                              directoryPath,
                ConsumerWhichThrows<? super NameAndContents, ? extends IOException> memberAdder
            ) {}
        };
    }

    /** @see #combineArchive(String, ConsumerWhichThrows) */
    public
    interface ArchiveCombiner {

        /**
         * Combines the results of {@link ContentsTransformer#transform(String, InputStream, java.io.OutputStream)}
         * for all archive entries into one object.
         *
         * @param archivePath The path designating the archive being transformed
         * @param entryAdder  Can be called to add entries to the archive
         */
        void
        combineArchive(
            String                                                              archivePath,
            ConsumerWhichThrows<? super NameAndContents, ? extends IOException> entryAdder
        ) throws IOException;

        /**
         * An {@link ArchiveCombiner} that does <i>nothing</i>.
         */
        ArchiveCombiner NOP = new ArchiveCombiner() {

            @Override public void
            combineArchive(
                String                                                              archivePath,
                ConsumerWhichThrows<? super NameAndContents, ? extends IOException> entryAdder
            ) {}
        };
    }

    /**
     * Creates and returns a {@link DirectoryTransformer} which implements the following features:
     * <ul>
     *   <li>Recursive processing of directories</li>
     *   <li>Removal, renaming and addition of directory members</li>
     * </ul>
     *
     * @param directoryMemberNameComparator The comparator used to sort a directory's members; a {@code null} value
     *                                      means to NOT sort the members, i.e. leave them in their 'natural' order as
     *                                      {@link File#list()} returns them
     * @param directoryMemberRemoval        Whether to remove (i.e. not copy) a member; the subject is the path of the
     *                                      member
     * @param directoryMemberRenaming       {@link Glob#replace(String) Transforms the name} of a member
     * @param saveSpace                     If {@code true}, then the method attempts to save file system space for
     *                                      in-place transformations by transforming <i>each file</i>, otherwise it
     *                                      creates a copy of the entire directory tree before deleting the original
     */
    public static DirectoryTransformer
    directoryTreeTransformer(
        @Nullable Comparator<Object>  directoryMemberNameComparator,
        Predicate<? super String>     directoryMemberRemoval,
        Glob                          directoryMemberRenaming,
        DirectoryCombiner             directoryCombiner,
        FileTransformer               regularFileTransformer,
        boolean                       saveSpace,
        boolean                       keepOriginals,
        ExceptionHandler<IOException> exceptionHandler
    ) {
        ProxyFileTransformer loopback = new ProxyFileTransformer();

        DirectoryTransformer result = FileTransformations.directoryTransformer(
            regularFileTransformer,        // regularFileTransformer
            directoryMemberNameComparator, // directoryMemberNameComparator
            directoryMemberRemoval,        // directoryMemberRemoval
            directoryMemberRenaming,       // directoryMemberRenaming
            loopback,                      // directoryMemberTransformer
            directoryCombiner,             // directoryCombiner
            saveSpace,                     // saveSpace
            keepOriginals,                 // keepOriginals
            exceptionHandler               // exceptionHandler
        );

        loopback.set(result);

        return result;
    }

    /**
     * @param fileRemovedResult Is called when a file was removed to produce a {@code T}
     */
    private static DirectoryTransformer
    directoryTransformer(
        FileTransformer                 regularFileTransformer,
        @Nullable Comparator<Object>    directoryMemberNameComparator,
        final Predicate<? super String> directoryMemberRemoval,
        final Glob                      directoryMemberRenaming,
        final FileTransformer           directoryMemberTransformer,
        final DirectoryCombiner         directoryCombiner,
        final boolean                   saveSpace,
        final boolean                   keepOriginals,
        ExceptionHandler<IOException>   exceptionHandler
    ) {

        return new DirectoryTransformer(
            regularFileTransformer,                          // regularFileTransformer
            directoryMemberNameComparator,                   // directoryMemberNameComparator
            FileTransformations.renameRemoveFileTransformer( // directoryMemberTransformer
                directoryMemberRemoval,
                directoryMemberRenaming,
                directoryMemberTransformer,
                keepOriginals
            ),
            directoryCombiner,                               // directoryCombiner
            saveSpace,                                       // saveSpace
            keepOriginals,                                   // keepOriginals
            exceptionHandler                                 // exceptionHandler
        );
    }

    /**
     * Wraps the given <var>delegate</var> in a {@link FileTransformer} which handles removal and renaming of files.
     *
     * @return A file transformer which implements removal and renaming of files.
     */
    public static FileTransformer
    renameRemoveFileTransformer(
        final Predicate<? super String> removal,
        final Glob                      renaming,
        final FileTransformer           delegate,
        final boolean                   keepOriginals
    ) {

        return new FileTransformer() {

            @Override public void
            transform(String path, File inputFile, File outputFile, Mode mode) throws IOException {

                boolean inPlace = inputFile.equals(outputFile);

                // Remove file?
                if (removal.evaluate(path)) {
                    if (mode == Mode.CHECK) throw FileTransformer.NOT_IDENTICAL;
                    if (inPlace) {
                        if (keepOriginals) {
                            File origFile = FileTransformations.origFile(inputFile);
                            if (origFile.exists()) FileUtil.deleteRecursively(origFile);
                            FileUtil.rename(inputFile, origFile);
                        } else {
                            inputFile.delete();
                        }
                    }
                    return;
                }

                // Rename file?
                String newPath = renaming.replace(path);
                if (newPath != null) {
                    File newOutputFile = new File(newPath);
                    if (!new File(newPath).getParentFile().equals(inputFile.getParentFile())) {
                        throw new IOException(
                            "Cannot rename '"
                            + path
                            + "' across directory boundaries"
                        );
                    }
                    newOutputFile = new File(outputFile.getParentFile(), newOutputFile.getName());
                    if (!newOutputFile.equals(outputFile)) {

                        if (mode == FileTransformer.Mode.CHECK) throw FileTransformer.NOT_IDENTICAL;

                        delegate.transform(path, inputFile, newOutputFile, mode);

                        if (inPlace) {
                            if (keepOriginals) {
                                File origFile = FileTransformations.origFile(inputFile);
                                if (origFile.exists()) FileUtil.deleteRecursively(origFile);
                                FileUtil.rename(inputFile, origFile);
                            } else {
                                inputFile.delete();
                            }
                        }

                        return;
                    }
                }

                delegate.transform(path, inputFile, outputFile, mode);
            }
        };
    }

    /**
     * Returns a {@link FileTransformer} which transforms files by feeding their contents through the {@code
     * normalContentsTransformer} (just like the {@link FileContentsTransformer}), or, iff the file is an archive
     * file, by feeding the archive file's entries through the <var>archiveEntryContentsTransformer</var>, or, iff the
     * file is a compressed file, be feeding the compressed file's decompressed contents through the {@code
     * compressedContentsTransformer}.
     * <p>
     *   Archive files and compressed files are introspected iff <var>lookIntoFormat</var> evaluates to {@code true}
     *   for "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static FileTransformer
    compressedAndArchiveFileTransformer(
        final Predicate<? super String>     lookIntoFormat,
        final Predicate<? super String>     archiveEntryRemoval,
        final Glob                          archiveEntryRenaming,
        final ArchiveCombiner               archiveCombiner,
        final ContentsTransformer           archiveEntryContentsTransformer,
        final ContentsTransformer           compressedContentsTransformer,
        final ContentsTransformer           normalContentsTransformer,
        final boolean                       keepOriginals,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return new FileTransformer() {

            @Override public void
            transform(final String path, File in, final File out, Mode mode) throws IOException {

                switch (mode) {

                case CHECK:
                    FileContentsTransformer.checkIdentity(
                        path,
                        new FileInputStream(in),
                        ContentsTransformations.compressedAndArchiveContentsTransformer(
                            lookIntoFormat,
                            archiveEntryRemoval,
                            archiveEntryRenaming,
                            archiveEntryContentsTransformer,
                            archiveCombiner,
                            compressedContentsTransformer,
                            normalContentsTransformer,
                            exceptionHandler
                        ),
                        true // closeInputStream
                    );
                    return;

                case CHECK_AND_TRANSFORM:
                    try {

                        // Transform in 'check identity' mode.
                        this.transform(path, in, out, Mode.CHECK);

                        if (!in.equals(out)) {
                            IoUtil.copy(in, out);
                        }

                        return;
                    } catch (RuntimeException re) {
                        if (re != FileTransformer.NOT_IDENTICAL) throw re;

                        // Non-identical transformation.
                        this.transform(path, in, out, Mode.TRANSFORM);
                        return;
                    }

                case TRANSFORM:
                    if (!in.equals(out)) {
                        this.transform2(path, in, out);
                        return;
                    }

                    File newFile = FileTransformations.newFile(out);
                    if (newFile.exists()) FileUtil.deleteRecursively(newFile);

                    this.transform2(path, in, newFile);

                    if (keepOriginals) {
                        File origFile = FileTransformations.origFile(in);
                        if (origFile.exists()) FileUtil.deleteRecursively(origFile);
                        FileUtil.rename(in, origFile);
                    } else {
                        if (!in.delete()) throw new IOException("Could not delete '" + in + "'");
                    }

                    FileUtil.rename(newFile, out);

                    return;

                default:
                    throw AssertionUtil.<Error>fail("Unexpected mode '" + mode + "'");
                }
            }

            /**
             * Same as {@link #transform}, but does not care about in-place transformations and {@link Mode}.
             */
            private void
            transform2(final String path, File in, final File out) throws IOException {

                CompressUtil.<Void>processFile(
                    path,                                      // path
                    in,                                        // file
                    lookIntoFormat,                            // lookIntoFormat
                    FileTransformations.archiveHandler(        // archiveHandler
                        path,
                        out,
                        archiveEntryRemoval,
                        archiveEntryRenaming,
                        archiveEntryContentsTransformer,
                        archiveCombiner,
                        exceptionHandler
                    ),
                    FileTransformations.compressorHandler(     // compressorHandler
                        path,
                        out,
                        compressedContentsTransformer
                    ),
                    FileTransformations.normalContentsHandler( // normalContentsHandler
                        path,
                        out,
                        normalContentsTransformer
                    )
                );
            }

            @Override public String
            toString() { return "compressedAndArchiveFileTransformer"; }

        };
    }

    /**
     * Returns a {@link FileTransformer} which transforms files by feeding their contents through the
     * <var>delegate</var> (just like the {@link FileContentsTransformer}), but automagically detects various archive
     * and compression formats (also nested) and processes the <em>entries of the archive</em> and the <em>uncompressed
     * contents</em> instead of the "raw" contents.
     *
     * @param lookIntoFormat       Whether the file/entry should be introspected; the subject is
     *                             <code>"<i>format</i>:<i>container-path</i>"</code>
     * @param archiveEntryRemoval  Whether to remove (i.e. not copy) a subject file or an archive entry; the subject is
     *                             the path of the subject file resp. the archive entry
     * @param archiveEntryRenaming {@link Glob#replace(String) Transforms the name} of a subject file or an archive
     *                             entry
     * @param archiveCombiner      Is invoked after the processing of each archive
     */
    public static FileTransformer
    recursiveCompressedAndArchiveFileTransformer(
        final Predicate<? super String> lookIntoFormat,
        final Predicate<? super String> archiveEntryRemoval,
        final Glob                      archiveEntryRenaming,
        final ArchiveCombiner           archiveCombiner,
        final ContentsTransformer       delegate,
        boolean                         keepOriginals,
        ExceptionHandler<IOException>   exceptionHandler
    ) {

        ContentsTransformer tmp = ContentsTransformations.recursiveCompressedAndArchiveContentsTransformer(
            lookIntoFormat,       // lookIntoFormat
            archiveEntryRemoval,  // archiveEntryRemoval
            archiveEntryRenaming, // archiveEntryRenaming
            archiveCombiner,      // archiveCombiner
            delegate,             // delegate
            exceptionHandler      // exceptionHandler
        );

        return FileTransformations.compressedAndArchiveFileTransformer(
            lookIntoFormat,       // lookIntoFormat
            archiveEntryRemoval,  // archiveEntryRemoval
            archiveEntryRenaming, // archiveEntryRenaming
            archiveCombiner,      // archiveCombiner
            tmp,                  // archiveEntryContentsTransformer
            tmp,                  // compressedContentsTransformer
            delegate,             // normalContentsTransformer
            keepOriginals,        // keepOriginals
            exceptionHandler      // exceptionHandler
        );
    }

    private static ArchiveHandler<Void>
    archiveHandler(
        final String                        path,
        final File                          out,
        final Predicate<? super String>     archiveEntryRemoval,
        final Glob                          archiveEntryRenaming,
        final ContentsTransformer           contentsTransformer,
        final ArchiveCombiner               archiveCombiner,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return new ArchiveHandler<Void>() {

            @Override @Nullable public Void
            handleArchive(final ArchiveInputStream archiveInputStream, final ArchiveFormat archiveFormat)
            throws IOException {

                ArchiveOutputStream aos;
                try {
                    aos = archiveFormat.create(out);
                } catch (ArchiveException ae) {
                    throw new IOException(ae);
                }
                try {

                    ContentsTransformations.transformArchive(
                        path,
                        archiveInputStream,
                        aos,
                        archiveEntryRemoval,
                        archiveEntryRenaming,
                        contentsTransformer,
                        archiveCombiner,
                        exceptionHandler
                    );

                    aos.close();

                    return null;
                } finally {
                    try { aos.close(); } catch (Exception e) {}
                }
            }
        };
    }

    private static CompressorHandler<Void>
    compressorHandler(
        final String              path,
        final File                out,
        final ContentsTransformer contentsTransformer
    ) {

        return new CompressorHandler<Void>() {

            @Override @Nullable public Void
            handleCompressor(CompressorInputStream compressorInputStream, CompressionFormat compressorFormat)
            throws IOException {
                FileOutputStream os = new FileOutputStream(out);
                try {
                    contentsTransformer.transform(
                        path + '!',
                        compressorInputStream,
                        compressorFormat.compressorOutputStream(os)
                    );
                    os.close();

                    return null;
                } catch (CompressorException ce) {
                    throw new IOException(ce);
                } finally {
                    try { os.close(); } catch (Exception e) {}
                }
            }
        };
    }

    private static NormalContentsHandler<Void>
    normalContentsHandler(final String path, final File out, final ContentsTransformer contentsTransformer) {

        return new NormalContentsHandler<Void>() {

            @Override @Nullable public Void
            handleNormalContents(InputStream inputStream) throws IOException {
                FileOutputStream os = new FileOutputStream(out);
                try {
                    contentsTransformer.transform(path, inputStream, os);
                    os.close();

                    return null;
                } finally {
                    try { os.close(); } catch (Exception e) {}
                }
            }
        };
    }

    /**
     * Transforms a set of files based on an array of strings, similar to many UNIX&trade; command line tools.
     * <p>
     *   If {@code args.length == 1}, then the file or directory {@code args[0]} is transformed in-place.
     * </p>
     * <p>
     *   If {@code args.length == 2} and {@code args[1]} is not an existing directory, then the file {@code args[0]} is
     *   transformed into the file {@code args[1]}.
     * </p>
     * <p>
     *   If {@code args.length >= 2} and {@code args[args.length - 1]} is an existing directory, then the files {@code
     *   args[0]} ... {@code args[args.length - 2]} are transformed into files with the same names in the directory
     *   {@code args[args.length - 1]}.
     * </p>
     *
     * @param exceptionHandler If the transformation of one of the <var>inputFiles</var> throws an {@link IOException}
     *                         or a {@link RuntimeException}, then {@link ExceptionHandler#handle(String, Exception)}
     *                         resp. {@link ExceptionHandler#handle(String, RuntimeException)} is called. Iff that call
     *                         completes normally, then processing continues with the next <var>inputFile</var>.
     * @throws IOException     {@code args.length == 0}
     * @throws IOException     {@code args.length > 2} and {@code args[args.length - 1]} is not an existing directory
     */
    public static void
    transform(
        String[]                      args,
        FileTransformer               fileTransformer,
        FileTransformer.Mode          mode,
        ExceptionHandler<IOException> exceptionHandler
    ) throws IOException {

        if (args.length == 0) {
            throw new IOException("Input file name missing");
        } else
        if (args.length == 1) {

            // In-place transformation.
            File file = new File(args[0]);
            FileTransformations.transformOneFile(
                file,             // in
                file,             // out
                fileTransformer,
                mode,
                exceptionHandler
            );
        } else
        if (args.length == 2) {
            File in  = new File(args[0]);
            File out = new File(args[1]);

            if (out.isDirectory()) {

                // Transform file into directory.
                FileTransformations.transformOneFile(
                    in,
                    new File(out, in.getName()),
                    fileTransformer,
                    mode,
                    exceptionHandler
                );
            } else {

                // Transform one file to into another file.
                FileTransformations.transformOneFile(
                    in,
                    out,
                    fileTransformer,
                    mode,
                    exceptionHandler
                );
            }
        } else
        {

            // Transform files into output directory.
            File outputDirectory = new File(args[args.length - 1]);
            if (!outputDirectory.isDirectory()) {
                throw new IOException("Output directory '" + outputDirectory + "' does not exist");
            }

            for (int i = 0; i < args.length - 1; i++) {
                File in = new File(args[i]);
                FileTransformations.transformOneFile(
                    in,
                    new File(outputDirectory, in.getName()),
                    fileTransformer,
                    mode,
                    exceptionHandler
                );
            }
        }
    }

    private static void
    transformOneFile(
        File                          in,
        File                          out,
        FileTransformer               fileTransformer,
        FileTransformer.Mode          mode,
        ExceptionHandler<IOException> exceptionHandler
    ) throws IOException {

        String path = in.getPath();

        try {
            fileTransformer.transform(path, in, out, mode);
        } catch (IOException ioe) {
            exceptionHandler.handle(path, ioe);
        } catch (RuntimeException re) {
            exceptionHandler.handle(path, re);
        }
    }

    /**
     * @return A file derived from the <var>file</var> which is typically used to create a temporary file which is
     *         later renamed to replace some "original" file
     */
    public static File
    newFile(File file) { return new File(file.getParentFile(), "." + file.getName() + ".new"); }

    /**
     * @return A file derived from the <var>file</var> which is typically used as a container to keep an "original"
     *         file
     */
    public static File
    origFile(File file) { return new File(file.getParentFile(), "." + file.getName() + ".orig"); }
}
