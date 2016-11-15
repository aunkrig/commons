
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.MarkableFileInputStream;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.RunnableUtil;

/**
 * A {@link FileTransformer} that transforms a file be feeding its contents through a {@link ContentsTransformer}.
 */
public
class FileContentsTransformer implements FileTransformer {

    private final ContentsTransformer contentsTransformer;
    private final boolean             keepOriginals;

    /**
     * @param keepOriginals Whether to keep a copy of the original file; only relevant for in-place transformations
     */
    public
    FileContentsTransformer(ContentsTransformer contentsTransformer, boolean keepOriginals) {
        this.contentsTransformer = contentsTransformer;
        this.keepOriginals       = keepOriginals;
    }

    /**
     * Opens the {@code in} file for reading, opens the {@code out} file for writing, reads the contents, feeds it
     * through the given {@link ContentsTransformer}, and writes it to the {@code out} file.
     */
    @Override public void
    transform(String path, File in, File out, Mode mode) throws IOException {

        switch (mode) {

        case CHECK:
            FileContentsTransformer.checkIdentity(
                path,
                new MarkableFileInputStream(in),
                this.contentsTransformer,
                true                             // closeInputStream
            );
            return;

        case CHECK_AND_TRANSFORM:

            // Execute the transformation in CHECK mode to determine whether the transformed contents are
            // identical with the contents of the input file.
            try {
                this.transform(path, in, out, Mode.CHECK);

                // The transformed contents is identical with the contents of the input file.
                if (in.equals(out)) {

                    if (this.keepOriginals) {

                        // No need to create a '.orig' copy when the contents does not change.
                        ;
                    }
                } else {
                    IoUtil.copy(in, out);
                }
            } catch (RuntimeException re) {
                if (re != FileTransformer.NOT_IDENTICAL) throw re;

                // The transformed contents is NOT identical with the contents of the input file.
                // Execute the transformation again, but this time in TRANSFORM mode.
                this.transform(path, in, out, Mode.TRANSFORM);
            }
            return;

        case TRANSFORM:
            if (in.equals(out)) {
                this.transformInPlace(in);
            } else {
                this.transformOutOfPlace(in, out);
            }

            if (!out.setLastModified(in.lastModified())) {
                throw new IOException("Could not set modification time of '" + out + "'");
            }
            return;

        default:
            throw AssertionUtil.<Error>fail("Unexpected mode '" + mode + "'");
        }
    }


    /**
     * Transforms the contents of the <var>file</var> and writes the results the same <var>file</var>.
     * <p>
     *   Iff {@link #keepOriginals} is set, then a copy of the original contents is kept in a file named {@link
     *   FileTransformations#origFile(File)}.
     * </p>
     * <p>
     *   Iff the transformation fails, the original <var>file</var> is left untouched.
     * </p>
     *
     * @throws IOException
     */
    private void
    transformInPlace(File file) throws IOException {
        File newFile = FileTransformations.newFile(file);

        // Transform the "file" file into "newFile".
        this.transformOutOfPlace(file, newFile);

        // Now replace the file with the "newFile".
        try {
            if (this.keepOriginals) {
                File origFile = FileTransformations.origFile(file);
                if (origFile.exists()) FileUtil.deleteRecursively(origFile);
                FileUtil.rename(file, origFile);
            } else {
                FileUtil.deleteRecursively(file);
            }
        } catch (IOException ioe) {
            FileUtil.attemptToDeleteRecursively(newFile);
            throw ioe;
        } catch (RuntimeException re) {
            FileUtil.attemptToDeleteRecursively(newFile);
            throw re;
        }

        // Rename the "newFile" to "out".
        FileUtil.rename(newFile, file);
    }

    /**
     * Transforms the contents of the <var>inputFile</var> and writes the results to the <var>outputFile</var>,
     * overwriting a possibly existing file.
     * <p>
     *   Iff the transformation fails, the <var>outputFile</var> (which is probably half-written) is deleted.
     * </p>
     * <p>
     *   If <var>inputFile</var> and <var>outputFile</var> address the same file, then the result is undefined.
     * </p>
     *
     * @throws IOException
     */
    private void
    transformOutOfPlace(File inputFile, File outputFile) throws  IOException {

        InputStream is = new MarkableFileInputStream(inputFile);
        try {

            OutputStream os = new FileOutputStream(outputFile);
            try {

                this.contentsTransformer.transform(inputFile.getPath(), is, os);
                os.close();
            } catch (IOException ioe) {
                try { os.close(); } catch (Exception e) {}
                outputFile.delete();
                throw ExceptionUtil.wrap("Transforming file '" + inputFile + "' into '" + outputFile + "'", ioe);
            } catch (RuntimeException re) {
                try { os.close(); } catch (Exception e) {}
                outputFile.delete();
                throw ExceptionUtil.wrap("Transforming file '" + inputFile + "' into '" + outputFile + "'", re);
            }
            is.close();
        } finally {
            try { is.close(); } catch (IOException ioe) {}
        }
    }

    /**
     * Consumes the {@code inputStream} and feeds it through the {@code contentsTransformer}.
     *
     * @throws RuntimeException {@link FileTransformer#NOT_IDENTICAL} iff the transformer contents differs from the
     *                          original contents
     */
    public static void
    checkIdentity(
        String              path,
        InputStream         inputStream,
        ContentsTransformer contentsTransformer,
        boolean             closeInputStream
    ) throws IOException {

        try {

            OutputStream[] oss = IoUtil.compareOutput(2, RunnableUtil.NOP, FileTransformer.THROW_NOT_IDENTICAL);

            contentsTransformer.transform(path, IoUtil.wye(inputStream, oss[0]), oss[1]);

            oss[0].close();
            oss[1].close();
            if (closeInputStream) inputStream.close();
        } finally {
            if (closeInputStream) try { inputStream.close(); } catch (Exception e) {}
        }
    }
}
