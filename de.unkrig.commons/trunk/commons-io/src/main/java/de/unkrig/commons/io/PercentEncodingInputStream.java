
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Implements the decoding of <a href="http://en.wikipedia.org/wiki/Percent-encoding">percent-encoded</a> bytes.
 */
@NotNullByDefault(false) public
class PercentEncodingInputStream extends FilterInputStream {

    public
    PercentEncodingInputStream(InputStream in) {
        super(in);
    }

    @Override public int
    read() throws IOException {
        int b = this.in.read();

        if (b != '%') return b;

        int hex1 = Character.digit(this.in.read(), 16);
        if (hex1 == -1) throw new MalformedInputException(2);
        int hex2 = Character.digit(this.in.read(), 16);
        if (hex2 == -1) throw new MalformedInputException(3);

        return (hex1 << 4) + hex2;
    }

    @Override public int
    read(byte[] buf, int off, int len) throws IOException {

        if (off < 0 || off > buf.length) throw new IndexOutOfBoundsException("off");
        if (len < 0 || len > buf.length - off) throw new IndexOutOfBoundsException("len");

        if (len == 0) return 0;

        int b = this.read();
        if (b == -1) return -1;

        buf[off] = (byte) b;

        for (int i = 1; i < len; i++) {
            try {
                b = this.read();
            } catch (IOException ioe) {
                return i;
            }
            if (b == -1) return i;
            buf[off + i] = (byte) b;
        }

        return len;
    }

    @Override public long
    skip(long n) throws IOException {

        if (n <= 0) return 0;

        byte[] buf = new byte[4096];

        for (long remaining = n;;) {
            int count = this.read(buf, 0, (int) Math.min(buf.length, remaining));
            if (count == -1) return n - remaining;
            remaining -= count;
            if (remaining == 0L) return n;
        }
    }

    @Override public int
    available() {
        return 0;
    }
}
