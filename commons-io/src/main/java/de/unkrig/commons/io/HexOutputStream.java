
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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * An {@link OutputStream} that formats and prints the data written to it in lines of 32 bytes.
 */
public
class HexOutputStream extends OutputStream {

    private static final int BYTES_PER_LINE = 32; // Must be a multiple of 2.

    private final PrintWriter out;
    private final byte[]      buffer = new byte[HexOutputStream.BYTES_PER_LINE];
    private int               cur;
    private long              offset;

    public
    HexOutputStream(Writer out) {
        this.out = new PrintWriter(out, true);
    }

    @Override public void
    flush() {

        if (this.cur == 0) return;

        this.out.printf("%06x", this.offset);

        int j = 0;
        {
            for (; j < HexOutputStream.BYTES_PER_LINE && j < this.cur; ++j) {
                this.out.printf(
                    j == HexOutputStream.BYTES_PER_LINE / 2 ? "-%02x" : " %02x",
                    0xff & this.buffer[j]
                );
            }
        }
        for (; j < HexOutputStream.BYTES_PER_LINE; ++j) {
            this.out.print("   ");
        }
        this.out.print("-");
        for (j = 0; j < HexOutputStream.BYTES_PER_LINE && j < this.cur; j++) {
            int c = 0xff & this.buffer[j];
            this.out.print(c >= 32 && c <= 126 ? (char) c : '.');
        }
        this.out.println();

        this.offset += this.cur;
        this.cur    = 0;
    }

    @Override public void
    write(int b) {
        if (this.crPending) {
            if (b != '\n') this.flush();
            this.crPending = false;
        }
        this.buffer[this.cur++] = (byte) b;
        if (b == '\n' || this.cur == this.buffer.length) {
            this.flush();
        } else {
            if (b == '\r') this.crPending = true;
        }
    }
    private boolean crPending;
}
