
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import de.unkrig.commons.nullanalysis.Nullable;

class RpmArchiveInputStream extends ArchiveInputStream {

    private final ArchiveInputStream cpioArchiveInputStream;

    RpmArchiveInputStream(InputStream is) throws IOException {
        this.cpioArchiveInputStream = RpmArchiveFormat.archiveInputStream2(is);
    }

    @Override public int
    getCount() { throw new UnsupportedOperationException("getCount"); }

    @Override public long
    getBytesRead() { throw new UnsupportedOperationException("getBytesRead"); }

    @Override public int
    read(@Nullable byte[] b, int off, int len) throws IOException {
        return this.cpioArchiveInputStream.read(b, off, len);
    }

    @Override public void
    close() throws IOException { this.cpioArchiveInputStream.close(); }

    @Override public ArchiveEntry
    getNextEntry() throws IOException { return this.cpioArchiveInputStream.getNextEntry(); }
}
