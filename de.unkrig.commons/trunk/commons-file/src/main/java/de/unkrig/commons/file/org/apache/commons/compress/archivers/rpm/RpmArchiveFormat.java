
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2020, Arno Unkrig
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.rpm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Collections;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.AbstractArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.lang.ExceptionUtil;

/**
 * Representation of the 'rpm' archive format.
 */
public final
class RpmArchiveFormat extends AbstractArchiveFormat {

    private static final FileNameUtil
    FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".rpm", ""), ".rpm");

    private RpmArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return RpmArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new RpmArchiveFormat();

    @Override public String
    getName() { return "rpm"; }

    @Override public boolean
    isArchiveFileName(String fileName) { return RpmArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return RpmArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) throws ArchiveException {
        try {
            return RpmArchiveFormat.archiveInputStream2(is);
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(null, ioe, ArchiveException.class);
        }
    }

    /**
     * Skips the LEAD, SIGNATURE and HEADER sections of the RPM archive, then decompresses the payload and returns it
     * as an {@link CpioArchiveInputStream}.
     */
    static CpioArchiveInputStream
    archiveInputStream2(InputStream is) throws IOException {

        org.redline_rpm.ReadableChannelWrapper in = new org.redline_rpm.ReadableChannelWrapper(Channels.newChannel(is));

        // Skip the LEAD section.
        new org.redline_rpm.header.Lead().read(in);

        // Skip the SIGNATURE section.
        new org.redline_rpm.header.Signature().read(in);

        // Read the HEADER section.
        org.redline_rpm.header.Header header = new org.redline_rpm.header.Header();
        header.read(in);

        // Decode the PAYLOAD section, which is a compressed CPIO archive.
        return new CpioArchiveInputStream(org.redline_rpm.Util.openPayloadStream(header, is));
    }

    @Override public ArchiveInputStream
    open(File archiveFile) throws IOException {
        return RpmArchiveFormat.archiveInputStream2(new BufferedInputStream(new FileInputStream(archiveFile)));
    }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) throws ArchiveException {
        throw new ArchiveException("Creation of 'rpm' archives not supported");
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) {
        return (
            signatureLength >= 4
            && (0xff & signature[0]) == 0xed
            && (0xff & signature[1]) == 0xab
            && (0xff & signature[2]) == 0xee
            && (0xff & signature[3]) == 0xdb
        );
    }

    @Override public String
    toString() { return this.getName(); }
}
