
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link FilterReader} that counts characters and line breaks in the stream. As usual, a line break is either
 * a {@code '\r'}, a {@code '\n'}, or a sequence {@code "\r\n"}.
 *
 * @see #offset()
 * @see #lineNumber()
 * @see #columnNumber()
 */
@NotNullByDefault(false) public
class CountingReader extends FilterReader {

    private int     offset, lineNumber = 1, columnNumber;
    private boolean crPending;

    public CountingReader(Reader in) { super(in); }

    /**
     * @return The number of characters read so far
     */
    public int offset() { return this.offset; }

    /**
     * Returns the line number of the previously read character, or '1' if no character has been read
     * yet.
     * <p>
     *   If the previously read character is CR or LF, then the result is the number of the line
     *   <i>following</i> the line separator.
     * </p>
     */
    public int lineNumber() { return this.lineNumber; }

    /**
     * Returns the column number of the previously read character, or '0' if no character has been read
     * yet. The column number of the first character in a line is '1'.
     * <p>
     *   If the previously read character is CR or LF, then the result is '0'.
     * </p>
     */
    public int columnNumber() { return this.columnNumber; }

    @Override public int
    read() throws IOException {
        int c = super.read();
        if (c == -1) return -1;

        this.offset++;

        if (this.crPending) {
            this.crPending = false;
            if (c == '\n') return c;
        }

        if (c == '\r' || c == '\n') {
            this.lineNumber++;
            this.columnNumber = 0;
            if (c == '\r') this.crPending = true;
        } else {
            this.columnNumber++;
        }
        return c;
    }

    @Override public int
    read(char[] cbuf, int off, int len) throws IOException {
        if (len <= 0) return 0;
        int c = this.read();
        if (c == -1) return -1;
        int n = 1; // SUPPRESS CHECKSTYLE UsageDistance
        for (;;) {
            cbuf[off++] = (char) c;
            if (--len == 0) return n;
            c = this.read();
            if (c == -1) return n;
            n++;
        }
    }
}
