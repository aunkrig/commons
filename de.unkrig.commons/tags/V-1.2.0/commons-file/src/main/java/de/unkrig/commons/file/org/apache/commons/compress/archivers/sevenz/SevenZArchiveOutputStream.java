
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.file.org.apache.commons.compress.archivers.sevenz;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

class SevenZArchiveOutputStream extends ArchiveOutputStream {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private final SevenZOutputFile szof;

    SevenZArchiveOutputStream(SevenZOutputFile szof) { this.szof = szof; }

    @Override public void
    putArchiveEntry(@Nullable ArchiveEntry ae) throws IOException { this.szof.putArchiveEntry(ae); }

    @Override public void
    write(int b) throws IOException { this.szof.write(b); }

    @Override public void
    write(@Nullable byte[] b, int off, int len) throws IOException { this.szof.write(b, off, len); }

    @Override public void
    closeArchiveEntry() throws IOException { this.szof.closeArchiveEntry(); }

    @Override public void
    finish() throws IOException { this.szof.finish(); }

    @Override public void
    close() throws IOException { this.szof.close(); }

    @Override public ArchiveEntry
    createArchiveEntry(@Nullable File inputFile, @Nullable String entryName) {
        assert inputFile != null;
        assert entryName != null;

        SevenZArchiveEntry szae = new SevenZArchiveEntry();
        szae.setCreationDate(new Date(inputFile.lastModified()));
        szae.setHasCreationDate(true);
        szae.setName(entryName);
        // 'setSize()' is automatically done by 'SevenZOutputFile.closeArchiveEntry()'.

        return szae;
    }
}
