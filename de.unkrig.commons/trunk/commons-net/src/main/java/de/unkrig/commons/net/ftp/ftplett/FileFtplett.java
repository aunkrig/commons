
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.net.ftp.ftplett;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Formatter;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An implementation of {@link Ftplett} that is based on a file directory tree.
 */
public
class FileFtplett implements Ftplett {

    public
    FileFtplett(File rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory.getCanonicalFile();
    }

    @Override public boolean
    changeWorkingDirectory(@Nullable String directoryName) throws IOException {
        if (directoryName == null) {
            this.currentWorkingDirectory = "/";
            return true;
        }
        if (!this.toFile(directoryName).isDirectory()) {
            return false;
        }
        this.currentWorkingDirectory = directoryName;
        return true;
    }

    @Override public String
    getWorkingDirectory() {
        return this.currentWorkingDirectory;
    }

    @Override @Nullable public InputStream
    retrieve(String fileName) throws IOException {
        File file = this.toFile(fileName);
        if (!file.isFile()) return null;
        return new FileInputStream(file);
    }

    @Override public OutputStream
    store(String fileName) throws IOException {
        File file = this.toFile(fileName);
        return new FileOutputStream(file);
    }

    @Override public boolean
    list(@Nullable String name, ConsumerWhichThrows<String, IOException> out) throws IOException {
        final File file = this.toFile(name);

        if (file.isFile()) {
            FileFtplett.list(file, out);
        } else
        if (file.isDirectory()) {
            for (File member : file.listFiles()) FileFtplett.list(member, out);
        } else
        {
            return false;
        }
        return true;
    }

    @Override public boolean
    nameList(@Nullable String name, ConsumerWhichThrows<String, IOException> out) throws IOException {
        final File file = this.toFile(name);

        if (file.isFile()) {
            FileFtplett.nameList(file, out);
        } else
        if (file.isDirectory()) {
            for (File member : file.listFiles()) FileFtplett.nameList(member, out);
        } else
        {
            return false;
        }
        return true;
    }

    @Override public boolean
    delete(String resourceName) throws IOException {
        return this.toFile(resourceName).delete();
    }

    @Override public boolean
    rename(String from, String to) throws IOException {
        return this.toFile(from).renameTo(this.toFile(to));
    }

    @Override @Nullable public Date
    getModificationTime(String resourceName) throws IOException {
        long lm = this.toFile(resourceName).lastModified();
        return lm == 0L ? null : new Date(lm);
    }

    // CONFIGURATION

    private static final Logger LOGGER = Logger.getLogger(FileFtplett.class.getName());
    private final File          rootDirectory;

    // STATE

    private String currentWorkingDirectory = "/";

    // IMPLEMENTATION

    private static void
    list(File member, ConsumerWhichThrows<String, IOException> out) throws IOException {
        final Formatter formatter = new Formatter();
        String          line      = formatter.format(
            "%1$tm-%1$td-%1$ty %1$tI:%1$tM%1$Tp  %2$20s %3$s",
            new Date(member.lastModified()),
            (
                member.isDirectory() ? "<DIR>" :
                member.isFile() ? member.length() :
                "???"
            ),
            member.getName()
        ).toString();
        formatter.close();
        LOGGER.fine("  >>> " + line);
        out.consume(line);
    }

    private static void
    nameList(File member, ConsumerWhichThrows<String, IOException> out) throws IOException {
        String line = member.getName();
        LOGGER.fine("  >>> " + line);
        out.consume(line);
    }

    /**
     * @return The file or directory denoted by the <var>path</var>, or the current working directory if the
     *         <var>path</var> is {@code null}
     */
    private File
    toFile(@Nullable String path) throws IOException {

        File file;
        if (path == null) {
            file = new File(this.rootDirectory, this.currentWorkingDirectory);
        } else
        if (path.startsWith("/") || path.startsWith("\\")) {
            file = new File(this.rootDirectory, path);
        } else
        {
            file = new File(this.rootDirectory, this.currentWorkingDirectory + File.separatorChar + path);
        }

        file = file.getCanonicalFile();
        if (!isDescendantOf(file, this.rootDirectory) && !file.equals(this.rootDirectory)) {
            return this.rootDirectory;
        }
        return file;
    }

    private static boolean
    isDescendantOf(final File file1, File file2) {
        for (File f = file1.getParentFile(); f != null; f = f.getParentFile()) {
            if (f.equals(file2)) return true;
        }
        return false;
    }
}
