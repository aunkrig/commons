
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.AbstractArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the 'ar' archive format.
 */
public final
class JarArchiveFormat extends AbstractArchiveFormat {

    private static final FileNameUtil FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".jar", ""), ".jar");

    private JarArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return JarArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new JarArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.JAR; }

    @Override public boolean
    isArchiveFileName(String fileName) { return JarArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return JarArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) { return new JarArchiveInputStream(is); }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) { return new JarArchiveOutputStream(os); }

    @Override public ArchiveOutputStream
    create(File archiveFile)
    throws IOException { return new JarArchiveOutputStream(new FileOutputStream(archiveFile)); }

    @Override public void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        String                                                           name,
        @Nullable final Date                                             lastModifiedDate,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof JarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Entry names ending in "/" designate DIRECTORIES, so strip all trailing slashes.
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);

        JarArchiveEntry jae = new JarArchiveEntry(name);
        if (lastModifiedDate != null) jae.setTime(lastModifiedDate.getTime());
        archiveOutputStream.putArchiveEntry(jae);
        writeContents.consume(archiveOutputStream);
        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException {
        if (!(archiveOutputStream instanceof JarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // The way to designate a DIRECTORY entry is a trailing slash.
        if (!name.endsWith("/")) name += '/';

        archiveOutputStream.putArchiveEntry(new JarArchiveEntry(name));
        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof JarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        JarArchiveEntry njae = new JarArchiveEntry(name != null ? name : archiveEntry.getName());
        try {
            njae.setTime(archiveEntry.getLastModifiedDate().getTime());
        } catch (UnsupportedOperationException uoe) {

            // Some ArchiveEntry implementations (e.g. SevenZArchiveEntry) throw UOE when "a
            // last modified date is not set".
            ;
        }


        if (archiveEntry instanceof JarArchiveEntry) {
            JarArchiveEntry jae = (JarArchiveEntry) archiveEntry;

            njae.setComment(jae.getComment());
            njae.setExternalAttributes(jae.getExternalAttributes());
            njae.setExtraFields(jae.getExtraFields(true));
            njae.setGeneralPurposeBit(jae.getGeneralPurposeBit());
            njae.setInternalAttributes(jae.getInternalAttributes());
            njae.setMethod(jae.getMethod());
        }

        archiveOutputStream.putArchiveEntry(njae);

        if (!archiveEntry.isDirectory()) writeContents.consume(archiveOutputStream);

        archiveOutputStream.closeArchiveEntry();
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) { return JarArchiveInputStream.matches(signature, signatureLength); }

    @Override public String
    toString() { return this.getName(); }
}
