
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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Signals end-of-input after exactly {@code limit} bytes were read from the delegate. Throws an EOFException iff
 * the delegate produces less than {@code limit} bytes.
 */
@NotNullByDefault(false) public
class FixedLengthInputStream extends FilterInputStream {
    private final long limit;
    private long       count;

    public
    FixedLengthInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    @Override public int
    read() throws IOException {
        if (this.count >= this.limit) return -1;
        int res = super.read();
        if (res == -1) throw new EOFException();
        ++this.count;
        return res;
    }

    @Override public int
    read(byte[] b, int off, int len) throws IOException {
        if (this.count + len > this.limit) {
            len = (int) (this.limit - this.count);
            if (len == 0) {
                this.in.available();
                return -1;
            }
        }
        int res = super.read(b, off, len);
        if (res == -1) {
            throw new EOFException("Input ended after " + this.count + " bytes; expected " + this.limit);
        }
        this.count += res;
        return res;
    }

    @Override public long
    skip(long n) throws IOException {
        if (n > this.limit - this.count) { // Beware! "n" may be Long.MAX_VALUE!
            n = this.limit - this.count;
        }
        return super.skip(n);
    }

    @Override public void
    close() {
        ; // Don't close the underlying stream!
    }
}
