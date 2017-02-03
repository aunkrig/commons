
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

package de.unkrig.commons.file.filetransformation;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.file.filetransformation.FileTransformations.DirectoryCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformations.NameAndContents;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @see #transform(String, File, File, de.unkrig.commons.file.filetransformation.FileTransformer.Mode)
 */
public
class DirectoryTransformer implements FileTransformer {

    /**
     * Sorts ascendingly by name (for the default locale).
     * <p>
     *   A good default value for the <var>memberNameComparator</var> parameter of {@link
     *   #DirectoryTransformer(FileTransformer, Comparator, FileTransformer, FileTransformations.DirectoryCombiner,
     *   boolean, boolean, ExceptionHandler)}.
     * </p>
     *
     * @see #DirectoryTransformer(FileTransformer, Comparator, FileTransformer, FileTransformations.DirectoryCombiner,
     *      boolean, boolean, ExceptionHandler)
     */
    public static final Collator DEFAULT_MEMBER_NAME_COMPARATOR = Collator.getInstance();

    private final FileTransformer               regularFileTransformer;
    private final ExceptionHandler<IOException> exceptionHandler;
    private final FileTransformer               directoryMemberTransformer;
    private final DirectoryCombiner             directoryCombiner;
    @Nullable private final Comparator<Object>  directoryMemberNameComparator;
    private final boolean                       saveSpace;
    private final boolean                       keepOriginals;

    /**
     * @param directoryMemberNameComparator The comparator used to sort a directory's members; a {@code null} value
     *                                      means to NOT sort the members, i.e. leave them in their 'natural' order as
     *                                      {@link File#list()} returns them
     * @see #transform(String, File, File, Mode)
     * @see #DEFAULT_MEMBER_NAME_COMPARATOR
     */
    public
    DirectoryTransformer(
        FileTransformer               regularFileTransformer,
        @Nullable Comparator<Object>  directoryMemberNameComparator,
        FileTransformer               directoryMemberTransformer,
        DirectoryCombiner             directoryCombiner,
        boolean                       saveSpace,
        boolean                       keepOriginals,
        ExceptionHandler<IOException> exceptionHandler
    ) {
        this.regularFileTransformer        = regularFileTransformer;
        this.directoryMemberNameComparator = directoryMemberNameComparator;
        this.directoryMemberTransformer    = directoryMemberTransformer;
        this.directoryCombiner             = directoryCombiner;
        this.saveSpace                     = saveSpace;
        this.keepOriginals                 = keepOriginals;
        this.exceptionHandler              = exceptionHandler;
    }

    /**
     * If <var>in</var> is a directory, then <var>out</var> is taken as a directory (created if missing), and {@link
     * #DirectoryTransformer(FileTransformer, Comparator, FileTransformer, FileTransformations.DirectoryCombiner,
     * boolean, boolean, ExceptionHandler) directoryMemberTransformer}{@code .transform()} is called for each member of
     * <var>in</var>.
     * <p>
     *   If that method throws an {@link IOException} or a {@link RuntimeException}, then that exception is caught, and
     *   {@link #DirectoryTransformer(FileTransformer, Comparator, FileTransformer,
     *   FileTransformations.DirectoryCombiner, boolean, boolean, ExceptionHandler) exceptionHandler}{@code .handle()}
     *   is called. (This includes the case when {@link FileTransformer#NOT_IDENTICAL} is thrown in mode {@link
     *   FileTransformer.Mode#CHECK}.)
     * </p>
     * <p>
     *   If "<var>exceptionHandler</var>{@code .}{@link ExceptionHandler#handle(String, RuntimeException)}" completes
     *   normally (i.e. it doesn't throw an exception), then processing continues with the next member of directory
     *   <var>in</var>.
     * </p>
     * <p>
     *   Iff <var>in</var> is <em>not</em> a directory, then the <var>regularFileTransformer</var> is invoked.
     * </p>
     */
    @Override public void
    transform(String path, File in, final File out, Mode mode) throws IOException {

        // Delegate to "this.regularFileTransformer" if the "in" file is not a directory.
        if (!in.isDirectory()) {
            this.regularFileTransformer.transform(path, in, out, mode);
            return;
        }

        // Now transform the directory and its members.
        if (in.equals(out)) {
            this.transformDirectoryInPlace(path, in, mode);
        } else {
            this.transformDirectoryOutOfPlace(path, in, out, mode);
        }
    }

    private void
    transformDirectoryInPlace(String path, File directory, Mode mode) throws IOException {

        if (this.saveSpace || mode == Mode.CHECK) {

            // In "save space" mode, transform the members in-place.
            this.transformMembers(path, directory, directory, mode);
            return;
        }

        // Prepare the "newDirectory".
        File newDirectory = FileTransformations.newFile(directory);
        if (newDirectory.exists()) FileUtil.deleteRecursively(newDirectory);
        if (!newDirectory.mkdirs()) throw new IOException("Could not create directory '" + newDirectory + "'");

        // Transform the "directory" members into the "newDirectory".
        try {
            this.transformMembers(path, directory, newDirectory, mode);
        } catch (IOException ioe) {
            try { FileUtil.deleteRecursively(newDirectory); } catch (Exception e) {}
            this.exceptionHandler.handle(path, ioe);
        } catch (RuntimeException re) {
            try { FileUtil.deleteRecursively(newDirectory); } catch (Exception e) {}
            this.exceptionHandler.handle(path, re);
        }

        // Rename the "directory" to "origDirectory".
        if (this.keepOriginals) {
            File origDirectory = FileTransformations.origFile(directory);
            if (origDirectory.exists()) FileUtil.deleteRecursively(origDirectory);
            FileUtil.rename(directory, origDirectory);
        } else {
            FileUtil.deleteRecursively(directory);
        }

        // Rename the "newDirectory" to "directory".
        FileUtil.rename(newDirectory, directory);
    }

    private void
    transformDirectoryOutOfPlace(String path, File in, File out, Mode mode) throws IOException {

        for (File p = out.getParentFile(); p != null; p = p.getParentFile()) {
            if (p.equals(in)) {
                throw new IOException(
                    "Output directory '"
                    + out
                    + "' must not be created under input directory '"
                    + in
                    + "'"
                );
            }
        }

        // Create output directory if necessary.
        boolean mkdirsOut = (
            (mode == Mode.TRANSFORM || mode == Mode.CHECK_AND_TRANSFORM)
            && !out.isDirectory()
        );
        if (mkdirsOut) if (!out.mkdirs()) throw new IOException("Could not create directory '" + out + "'");

        // Now transform all members.
        try {

            this.transformMembers(path, in, out, mode);
        } catch (IOException ioe) {
            if (mkdirsOut) try { FileUtil.deleteRecursively(out); } catch (Exception e) {}
            this.exceptionHandler.handle(path, ioe);
        } catch (RuntimeException re) {
            if (mkdirsOut) try { FileUtil.deleteRecursively(out); } catch (Exception e) {}
            this.exceptionHandler.handle(path, re);
        }
    }

    private void
    transformMembers(String path, File inputDirectory, final File outputDirectory, Mode mode) throws IOException {

        // Transform all directory members.
        String[] memberNames = inputDirectory.list();
        if (memberNames == null) {

            // MS WINDOWS 7: Read-protected directory produces:
            // isDirectory() => true
            // canRead()     => true
            // list()        => null
            // listFiles()   => null
            throw new IOException(inputDirectory + ": Permission denied");
        }

        // Sort the members, if requested.
        if (this.directoryMemberNameComparator != null) Arrays.sort(memberNames, this.directoryMemberNameComparator);

        for (String memberName : memberNames) {
            String memberPath = path + File.separatorChar + memberName;

            // Now transform each member.
            try {
                this.directoryMemberTransformer.transform(
                    memberPath,
                    new File(inputDirectory, memberName),
                    new File(outputDirectory, memberName),
                    mode
                );
            } catch (IOException ioe) {
                this.exceptionHandler.handle(memberPath, ioe);
            } catch (RuntimeException re) {
                this.exceptionHandler.handle(memberPath, re);
            }
        }

        this.directoryCombiner.combineDirectory(
            path,                                                                         // directoryPath
            new ConsumerWhichThrows<FileTransformations.NameAndContents, IOException>() { // memberAdder

                @Override public void
                consume(NameAndContents nac) throws IOException {
                    IoUtil.copy(nac.open(), true, new File(outputDirectory, nac.getName()));
                }
            }
        );
    }

    @Override public String
    toString() { return "DIRECTORY=>" + this.regularFileTransformer; }
}
