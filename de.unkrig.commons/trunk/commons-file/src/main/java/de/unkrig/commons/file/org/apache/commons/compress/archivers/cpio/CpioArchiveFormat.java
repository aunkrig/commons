
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.cpio;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.AbstractArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.FillerAndDrainer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the 'ar' archive format.
 */
public final
class CpioArchiveFormat extends AbstractArchiveFormat {

    private static final FileNameUtil
    FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".cpio", ""), ".cpio");

    private CpioArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return CpioArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new CpioArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.CPIO; }

    @Override public boolean
    isArchiveFileName(String fileName) { return CpioArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return CpioArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) { return new CpioArchiveInputStream(is); }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) { return new CpioArchiveOutputStream(os); }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final String                                                           name,
        @Nullable final Date                                                   lastModifiedDate,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof CpioArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Copy the contents to a temporary storage in order to count the bytes.
        PipeUtil.temporaryStorage(new FillerAndDrainer() {

            long count;

            @Override public void
            fill(OutputStream os) throws IOException {
                this.count = OutputStreams.writeAndCount(writeContents, os);
            }

            @Override public void
            drain(InputStream is) throws IOException {
                CpioArchiveEntry ae = new CpioArchiveEntry(name, this.count);
                ae.setTime((lastModifiedDate != null ? lastModifiedDate.getTime() : System.currentTimeMillis()) / 1000);
                archiveOutputStream.putArchiveEntry(ae);
                IoUtil.copy(is, archiveOutputStream);
                archiveOutputStream.closeArchiveEntry();
            }
        });
    }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException {
        if (!(archiveOutputStream instanceof CpioArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        CpioArchiveEntry cae = new CpioArchiveEntry(name);
        cae.setMode(CpioConstants.C_ISDIR);
        archiveOutputStream.putArchiveEntry(cae);
        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof CpioArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Copy the contents to a temporary storage in order to count the bytes.
        class MyPipeUser implements FillerAndDrainer {

            int  checksum;
            long count;

            @Override public void
            fill(OutputStream os) throws IOException {

                if (archiveEntry.isDirectory()) return;

                this.count = OutputStreams.writeAndCount(
                    writeContents,
                    new FilterOutputStream(os) {

                        @Override public void
                        write(int b) throws IOException {
                            MyPipeUser.this.checksum += b;
                            this.out.write(b);
                            super.close();
                        }
                    }
                );
            }

            @Override public void
            drain(InputStream is) throws IOException {

                CpioArchiveEntry ncae = new CpioArchiveEntry(name != null ? name : archiveEntry.getName());
                ncae.setSize(this.count);
                try {
                    ncae.setTime(archiveEntry.getLastModifiedDate().getTime());
                } catch (UnsupportedOperationException uoe) {

                    // Some ArchiveEntry implementations (e.g. SevenZArchiveEntry) throw UOE when "a
                    // last modified date is not set".
                    ;
                }

                ncae.setChksum(this.checksum & 0xff);

                if (archiveEntry instanceof CpioArchiveEntry) {
                    CpioArchiveEntry cae = (CpioArchiveEntry) archiveEntry;
                    ncae.setDevice(cae.getDevice());
                    ncae.setGID(cae.getGID());
                    ncae.setInode(cae.getInode());
                    ncae.setMode(cae.getMode());
                    ncae.setNumberOfLinks(cae.getNumberOfLinks());
                    ncae.setRemoteDevice(cae.getRemoteDevice());
                    ncae.setUID(cae.getUID());
                }

                archiveOutputStream.putArchiveEntry(ncae);
                IoUtil.copy(is, archiveOutputStream);
                archiveOutputStream.closeArchiveEntry();
            }
        }

        PipeUtil.temporaryStorage(new MyPipeUser());
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) {
        return CpioArchiveInputStream.matches(signature, signatureLength);
    }

    @Override public String
    toString() { return this.getName(); }
}
