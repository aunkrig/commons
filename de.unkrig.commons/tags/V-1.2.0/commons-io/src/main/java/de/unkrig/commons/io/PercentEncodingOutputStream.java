
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

package de.unkrig.commons.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Implements <a href="http://en.wikipedia.org/wiki/Percent-encoding">Percent-Encoding</a>.
 */
public
class PercentEncodingOutputStream extends FilterOutputStream {

    public
    PercentEncodingOutputStream(OutputStream out) {
        super(out);
    }

    @Override public void
    write(int b) throws IOException {
        if (PercentEncoding.isUnreserved(b)) {
            this.out.write(b);
        } else {
            this.writePercentEncoded(b);
        }
    }

    /**
     * Writes a given byte to the delegate <i>without</i> percent-encoding it.
     */
    public void
    writeUnencoded(int b) throws IOException {
        this.out.write(b);
    }

    private void
    writePercentEncoded(int b) throws IOException {
        this.out.write(new byte[] {
            '%',
            (byte) Character.forDigit(b >> 4 & 0xf, 16),
            (byte) Character.forDigit(b & 0xf, 16),
        });
    }
}