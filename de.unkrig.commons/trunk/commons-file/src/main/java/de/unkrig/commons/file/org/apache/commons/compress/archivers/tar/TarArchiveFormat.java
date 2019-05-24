
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.tar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.FillerAndDrainer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the 'tar' archive format.
 */
public final
class TarArchiveFormat implements ArchiveFormat {

    private static final FileNameUtil FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".tar", ""), ".tar");

    private TarArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return TarArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new TarArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.TAR; }

    @Override public boolean
    isArchiveFileName(String fileName) { return TarArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return TarArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) { return new TarArchiveInputStream(is); }

    @Override public ArchiveInputStream
    open(File archiveFile)
    throws IOException { return new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile))); }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) { return new TarArchiveOutputStream(os); }

    @Override public ArchiveOutputStream
    create(File archiveFile)
    throws IOException { return new TarArchiveOutputStream(new FileOutputStream(archiveFile)); }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        String                                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof TarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Entry names ending in "/" designate DIRECTORIES, so strip all trailing slashes.
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);

        final TarArchiveEntry tae = new TarArchiveEntry(name);

        // Copy the contents to a temporary storage in order to count the bytes.
        PipeUtil.temporaryStorage(new FillerAndDrainer() {

            long count;

            @Override public void
            fill(OutputStream os) throws IOException {
                this.count = OutputStreams.writeAndCount(writeContents, os);
            }

            @Override public void
            drain(InputStream is) throws IOException {

                tae.setSize(this.count);

                archiveOutputStream.putArchiveEntry(tae);
                IoUtil.copy(is, archiveOutputStream);
                archiveOutputStream.closeArchiveEntry();
            }
        });
    }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException {
        if (!(archiveOutputStream instanceof TarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // The way to designate a DIRECTORY entry is a trailing slash.
        if (!name.endsWith("/")) name += '/';

        archiveOutputStream.putArchiveEntry(new TarArchiveEntry(name));
        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof TarArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Copy the contents to a temporary storage in order to count the bytes.
        PipeUtil.temporaryStorage(new FillerAndDrainer() {

            long count;

            @Override public void
            fill(OutputStream os) throws IOException {

                if (!archiveEntry.isDirectory()) this.count = OutputStreams.writeAndCount(writeContents, os);
            }

            @Override public void
            drain(InputStream is) throws IOException {

                TarArchiveEntry ntae = new TarArchiveEntry(name != null ? name : archiveEntry.getName());
                ntae.setModTime(archiveEntry.getLastModifiedDate());
                ntae.setSize(this.count);

                if (archiveEntry instanceof TarArchiveEntry) {
                    TarArchiveEntry tae = (TarArchiveEntry) archiveEntry;
                    ntae.setDevMajor(tae.getDevMajor());
                    ntae.setDevMinor(tae.getDevMinor());
                    ntae.setGroupId(tae.getGroupId());
                    ntae.setGroupName(tae.getGroupName());
                    ntae.setLinkName(tae.getLinkName());
                    ntae.setMode(tae.getMode());
                    ntae.setUserId(tae.getUserId());
                    ntae.setUserName(tae.getUserName());
                }

                archiveOutputStream.putArchiveEntry(ntae);
                IoUtil.copy(is, archiveOutputStream);
                archiveOutputStream.closeArchiveEntry();
            }
        });
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) { return TarArchiveInputStream.matches(signature, signatureLength); }

    @Override public String
    toString() { return this.getName(); }
}
