
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.AbstractArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.pipe.Pipe;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the 'zip' archive format.
 */
public final
class ZipArchiveFormat extends AbstractArchiveFormat {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final FileNameUtil FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".zip", ""), ".zip");

    private ZipArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return ZipArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new ZipArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.ZIP; }

    @Override public boolean
    isArchiveFileName(String fileName) { return ZipArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return ZipArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) { return new ZipArchiveInputStream(is); }

    @Override public ArchiveInputStream
    open(File archiveFile)
    throws IOException { return ZipArchiveFormat.zipArchiveInputStream(archiveFile); }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) { return new ZipArchiveOutputStream(os); }

    @Override public ArchiveOutputStream
    create(File archiveFile) throws IOException { return new ZipArchiveOutputStream(archiveFile); }

    @Override public void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        String                                                           name,
        @Nullable final Date                                             lastModifiedDate,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof ZipArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Entry names ending in "/" designate DIRECTORIES, so strip all trailing slashes.
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);

        ZipArchiveEntry zae = new ZipArchiveEntry(name);
        if (lastModifiedDate != null) zae.setTime(lastModifiedDate.getTime());
        archiveOutputStream.putArchiveEntry(zae);

        writeContents.consume(archiveOutputStream);

        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException {
        if (!(archiveOutputStream instanceof ZipArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // The way to designate a DIRECTORY entry is a trailing slash.
        if (!name.endsWith("/")) name += '/';

        archiveOutputStream.putArchiveEntry(new ZipArchiveEntry(name));
        archiveOutputStream.closeArchiveEntry();
    }

    @Override public void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        ArchiveEntry                                                     archiveEntry,
        @Nullable String                                                 name,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof ZipArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        ZipArchiveEntry nzae = new ZipArchiveEntry(name != null ? name : archiveEntry.getName());
        try {
            nzae.setTime(archiveEntry.getLastModifiedDate().getTime());
        } catch (UnsupportedOperationException uoe) {

            // Some ArchiveEntry implementations (e.g. SevenZArchiveEntry) throw UOE when "a
            // last modified date is not set".
            ;
        }

        if (archiveEntry instanceof ZipArchiveEntry) {
            ZipArchiveEntry zae  = (ZipArchiveEntry) archiveEntry;

            nzae.setComment(zae.getComment());
            nzae.setExternalAttributes(zae.getExternalAttributes());
            nzae.setExtraFields(zae.getExtraFields(true));
            nzae.setGeneralPurposeBit(zae.getGeneralPurposeBit());
            nzae.setInternalAttributes(zae.getInternalAttributes());
            nzae.setMethod(zae.getMethod());
            if (nzae.isDirectory()) {
                nzae.setSize(0);
                nzae.setCrc(0);
            }
        }

        if (archiveEntry.isDirectory()) {
            archiveOutputStream.putArchiveEntry(nzae);
        } else
        if (nzae.getMethod() != ZipArchiveOutputStream.STORED) {
            archiveOutputStream.putArchiveEntry(nzae);
            writeContents.consume(archiveOutputStream);
        } else
        {

            // Work around
            //    java.util.zip.ZipException: uncompressed size is required for STORED method when not writing to a file
            //    java.util.zip.ZipException: crc checksum is required for STORED method when not writing to a file
            final Pipe ep = PipeFactory.elasticPipe();
            try {
                CRC32 crc32 = new CRC32();

                // Copy the entry contents to the elastic pipe, and, at the same time, count the bytes and compute
                // the CRC32.
                long uncompressedSize = OutputStreams.writeAndCount(writeContents, OutputStreams.tee(
                    OutputStreams.updatesChecksum(crc32),
                    new OutputStream() {

                        @Override public void
                        write(int b) throws IOException { this.write(new byte[] { (byte) b }, 0, 1); }

                        @NotNullByDefault(false) @Override public void
                        write(byte[] b, int off, int len) throws IOException {
                            while (len > 0) {
                                int x = ep.write(b, off, len);
                                assert x > 0;
                                off += x;
                                len -= x;
                            }
                        }
                    }
                ));

                nzae.setSize(uncompressedSize);
                nzae.setCrc(crc32.getValue());
                archiveOutputStream.putArchiveEntry(nzae);

                byte[] buffer = new byte[8192];
                while (!ep.isEmpty()) {
                    int n = ep.read(buffer);
                    archiveOutputStream.write(buffer, 0, n);
                }

                ep.close();
            } finally {
                try { ep.close(); } catch (Exception e) {}
            }
        }

        archiveOutputStream.closeArchiveEntry();
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) { return ZipArchiveInputStream.matches(signature, signatureLength); }

    @Override @Nullable public String
    getCompressionMethod(ArchiveEntry ae) {
        return ZipMethod.getMethodByCode(((ZipArchiveEntry) ae).getMethod()).toString();
    }

    private static ArchiveInputStream
    zipArchiveInputStream(final File file) throws IOException {

        return new ArchiveInputStream() {

            final ZipFile                      zipFile = new ZipFile(file);
            final Enumeration<ZipArchiveEntry> entries = this.zipFile.getEntriesInPhysicalOrder();
            @Nullable private InputStream      stream;

            @Override public int
            getCount() { throw new UnsupportedOperationException("getCount"); }

            @Override public long
            getBytesRead() { throw new UnsupportedOperationException("getBytesRead"); }

            @Override public int
            read(@Nullable byte[] b, int off, int len) throws IOException {
                InputStream is = this.stream;
                if (is == null) throw new IllegalStateException();
                return is.read(b, off, len);
            }

            @Override public void
            close() throws IOException { this.zipFile.close(); }

            @Override @Nullable public ArchiveEntry
            getNextEntry() throws IOException {

                if (!this.entries.hasMoreElements()) {
                    this.stream = null;
                    return null;
                }

                ZipArchiveEntry zae = this.entries.nextElement();
                this.stream = this.zipFile.getInputStream(zae);
                return zae;
            }

            @Override public String
            toString() { return file.toString(); }
        };
    }

    @Override public String
    toString() { return this.getName(); }
}
