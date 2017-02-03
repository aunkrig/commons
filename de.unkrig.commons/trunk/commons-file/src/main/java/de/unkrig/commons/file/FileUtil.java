
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

package de.unkrig.commons.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility class for various file operations.
 */
public final
class FileUtil {

    private
    FileUtil() {}

    /**
     * Similar to {@link File#delete()}, but clears and deletes directory trees recursively.
     *
     * @param file The regular file or directory to delete
     * @return     Whether the regular file or the directory and its members could successfully be deleted
     */
    public static boolean
    attemptToDeleteRecursively(File file) {

        if (file.isDirectory()) {
            boolean success = true;

            File[] members = file.listFiles();
            if (members == null) {

                // MS WINDOWS 7: Read-protected directory produces:
                // isDirectory() => true
                // canRead()     => true
                // list()        => null
                // listFiles()   => null
                return false;
            }

            for (File member : members) {
                success &= FileUtil.attemptToDeleteRecursively(member);
            }
            if (!success) return false;
        }

        return file.delete();
    }

    /**
     * Deletes the given regular file, or clears and deletes the given directory recursively.
     *
     * @throws IOException The <var>file</var> could not be deleted
     */
    public static void
    deleteRecursively(File file) throws IOException {

        if (file.isDirectory()) {

            File[] members = file.listFiles();
            if (members == null) {

                // MS WINDOWS 7: Read-protected directory produces:
                // isDirectory() => true
                // canRead()     => true
                // list()        => null
                // listFiles()   => null
                throw new IOException(file + ": Permission denied");
            }

            for (File member : members) {
                FileUtil.deleteRecursively(member);
            }
        }

        if (!file.delete()) throw new IOException("Cannot delete '" + file + "'");
    }

    /**
     * @throws IOException Renaming failed
     * @see                File#renameTo(File)
     */
    public static void
    rename(File oldFile, File newFile) throws IOException {

        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Could not rename '" + oldFile + "' to '" + newFile + "'");
        }
    }

    /**
     * Opens the named <var>file</var>, lets the <var>processor</var> read text from it, and closes the file.
     *
     * @param charset The charset to be used for reading
     * @throws EX     The throwable that the <var>processor</var> may throw
     */
    public static <EX extends Throwable> void
    processContent(
        File                                              file,
        Charset                                           charset,
        ConsumerWhichThrows<? super Reader, ? extends EX> processor
    ) throws IOException, EX {

        FileUtil.processContent(new InputStreamReader(new FileInputStream(file), charset), processor);
    }

    /**
     * Passes the given <var>reader</var> to the <var>processor</var>, and then closes the file.
     *
     * @throws EX The throwable that the <var>processor</var> may throw
     */
    public static <EX extends Throwable> void
    processContent(
        Reader                                            reader,
        ConsumerWhichThrows<? super Reader, ? extends EX> processor
    ) throws IOException, EX {

        try {
            processor.consume(reader);
            reader.close();
        } finally {
            try { reader.close(); } catch (Exception e2) {}
        }
    }

    /**
     * @return On opener which produces {@link FileInputStream}s for the given <var>file</var>
     */
    public static ProducerWhichThrows<FileInputStream, IOException>
    opener(final File file) {

        return new ProducerWhichThrows<FileInputStream, IOException>() {

            @Override @Nullable public FileInputStream
            produce() throws IOException { return new FileInputStream(file); }
        };
    }

    /**
     * @return On opener which produces {@link ByteArrayInputStream}s for the given <var>data</var>
     */
    public static ProducerWhichThrows<ByteArrayInputStream, IOException>
    opener(final byte[] data) {

        return new ProducerWhichThrows<ByteArrayInputStream, IOException>() {
            @Override @Nullable public ByteArrayInputStream produce() { return new ByteArrayInputStream(data); }
        };
    }
}
