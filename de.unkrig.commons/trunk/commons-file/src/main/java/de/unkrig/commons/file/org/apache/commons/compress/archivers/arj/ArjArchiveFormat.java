
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.arj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the 'ar' archive format.
 */
public final
class ArjArchiveFormat implements ArchiveFormat {

    private static final FileNameUtil  FILE_NAME_UTIL = new FileNameUtil(Collections.singletonMap(".arj", ""), ".arj");

    private ArjArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return ArjArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new ArjArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.ARJ; }

    @Override public boolean
    isArchiveFileName(String fileName) { return ArjArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return ArjArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) throws ArchiveException { return new ArjArchiveInputStream(is); }

    @Override public ArchiveInputStream
    open(File archiveFile)
    throws IOException, ArchiveException {
        return new ArjArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
    }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) throws ArchiveException {
        throw new ArchiveException("Creation of 'arj' archives not supported");
    }

    @Override public ArchiveOutputStream
    create(File archiveFile)
    throws ArchiveException { throw new ArchiveException("Creation of 'arj' archives not supported"); }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final String                                                           name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) { throw new IllegalArgumentException(archiveOutputStream.getClass().getName()); }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) {
        throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) { throw new IllegalArgumentException(archiveOutputStream.getClass().getName()); }

    @Override public boolean
    matches(byte[] signature, int signatureLength) { return ArjArchiveInputStream.matches(signature, signatureLength); }

    @Override public String
    toString() { return this.getName(); }
}
