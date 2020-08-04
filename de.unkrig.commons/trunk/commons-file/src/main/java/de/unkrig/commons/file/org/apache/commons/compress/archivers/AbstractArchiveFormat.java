
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Default implementation for an archive file format
 */
public abstract
class AbstractArchiveFormat implements ArchiveFormat {

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) throws StreamingNotSupportedException, ArchiveException {
        throw new StreamingNotSupportedException(this.getName());
    }

    @Override public ArchiveInputStream
    open(File archiveFile) throws IOException, StreamingNotSupportedException, ArchiveException {
        return this.archiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
    }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) throws StreamingNotSupportedException, ArchiveException {
        throw new StreamingNotSupportedException(this.getName());
    }

    @Override public ArchiveOutputStream
    create(File archiveFile) throws IOException, ArchiveException {
        throw new ArchiveException("Creation of \"" + this.getName() + "\" archives not supported");
    }

    @Override public final void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        String                                                           name,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException { this.writeEntry(archiveOutputStream, name, null, writeContents); }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final String                                                           name,
        @Nullable final Date                                                   lastModifiedDate,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException { throw new IllegalArgumentException(archiveOutputStream.getClass().getName()); }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException {
        throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException { throw new IllegalArgumentException(archiveOutputStream.getClass().getName()); }
}
