
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

package de.unkrig.commons.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * This stream enforces that an exact number of bytes is written to it before it is closed.
 */
@NotNullByDefault(false) public
class FixedLengthOutputStream extends FilterOutputStream {

    private long remaining;

    public
    FixedLengthOutputStream(OutputStream out, long n) {
        super(out);
        this.remaining = n;
    }

    @Override public void
    write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override public void
    write(byte[] b, int off, int len) throws IOException {
        if (len > this.remaining) {
            throw new IOException("Attempt to write " + (len - this.remaining) + " more bytes than allowed");
        }
        this.out.write(b, off, len);
        this.remaining -= len;
    }

    @Override public void
    close() throws IOException {
        if (this.remaining > 0) throw new IOException("Stream closed when " + this.remaining + " bytes were unwritten");
        this.out.close();
    }
}
