
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

package de.unkrig.commons.net.http.io;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Implementation of the "chunked transfer encoding" as defined in <a
 * href="http://tools.ietf.org/html/rfc2616#section-3.6.1">RFC 2616, Section 3.6.1</a>.
 * <p>
 *   Features that are <b>not</b> implemented include: Chunk extensions, trailer.
 * </p>
 */
@NotNullByDefault(false) public
class ChunkedInputStream extends FilterInputStream {

    private static final Logger LOGGER = Logger.getLogger(ChunkedInputStream.class.getName());

    /**
     * Number of data bytes still available in the current chunk. "0" means that the next chunk must be opened to
     * get more data; "-1" means that the last chunk is exhausted and no more bytes will be available
     * ("end-of-input").
     */
    int available;

    public
    ChunkedInputStream(InputStream in) {
        super(in);
    }

    @Override public int
    read() throws IOException {
        byte[] b = new byte[1];
        if (this.read(b) == -1) return -1;
        return 0xff & b[0];
    }

    @Override public int
    read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;

        if (b == null) throw new NullPointerException("b");
        if (off < 0 || off > b.length) throw new IllegalArgumentException("off");
        if (len < 0 || off + len > b.length) throw new IllegalArgumentException("len");

        // At EOI?
        if (this.available == -1) return -1;

        LOGGER.log(Level.FINER, "{0} byte(s) available; len={1}", new Object[] { this.available, len });

        // At end-of-chunk?
        if (this.available == 0) {

            // Current chunk is exhausted, turn to the next one.
            String line = this.readLine();
            if (line.length() == 0) line = this.readLine(); // Ignore the blank line between chunks.

            // Strip the chunk extension.
            {
                int idx = line.indexOf(';');
                if (idx != -1) line = line.substring(0, idx);
            }

            // Parse and validate the chunk size.
            try {
                this.available = Integer.parseInt(line, 16);
            } catch (NumberFormatException nfe) {
                throw new IOException("Invalid chunk size field '" + line + "'"); // SUPPRESS CHECKSTYLE HidingCause
            }
            if (this.available < 0) throw new IOException("Negative chunk size field '" + line + "'");

            // Last chunk?
            if (this.available == 0) {
                LOGGER.log(Level.FINER, "End-of-input");
                this.available = -1;
                return -1;
            }

            LOGGER.log(Level.FINER, "{0} more byte(s) available", this.available);
        }

        if (len > this.available) len = this.available;

        int result = super.read(b, off, len);
        this.available -= result;

        return result;
    }

    @Override public long
    skip(long n) throws IOException {
        if (n <= 0) return 0;

        byte[] skipBuffer = new byte[4096];

        long remaining = n;
        while (remaining > 0) {
            int count = this.read(skipBuffer, 0, (int) Math.min(skipBuffer.length, remaining));
            if (count < 0) break;
            remaining -= count;
        }

        return n - remaining;
    }

    @Override public int
    available() {
        return this.available;
    }

    @Override public synchronized void
    mark(int readlimit) {
    }

    @Override public synchronized void
    reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override public boolean
    markSupported() {
        return false;
    }

    /**
     * Reads a ISO-8859-1-encoded sequence of bytes until CRLF.
     *
     * @return              The (possible zero-length) line that was read, not including the terminal CRLF
     * @throws EOFException The underlying stream signals EOI before a CR was read
     * @throws IOException  The CR was not followed by an LF
     */
    private String
    readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            int c = this.in.read();
            if (c == -1) throw new EOFException();
            if (c == '\r') {
                c = this.in.read();
                if (c != '\n') throw new IOException("CR is not followed by NL, but by " + c);
                return sb.toString();
            }
            sb.append((char) c);
        }
    }
}
