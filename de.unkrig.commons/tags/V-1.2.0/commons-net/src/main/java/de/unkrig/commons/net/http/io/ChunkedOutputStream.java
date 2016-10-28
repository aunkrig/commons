
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import de.unkrig.commons.net.http.MessageHeader;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Implementation of the "chunked transfer encoding" as defined in <a
 * href="http://tools.ietf.org/html/rfc2616#section-3.6.1">RFC 2616, Section 3.6.1</a>.
 * <p>
 *   Does not write any "chunk extensions" (because the RFC does not define any concrete chunk extensions).
 * </p>
 */
@NotNullByDefault(false) public
class ChunkedOutputStream extends FilterOutputStream {

    private final List<MessageHeader> trailer = new ArrayList<MessageHeader>();

    public
    ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    @Override public void
    write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override public void
    write(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || off > b.length) throw new IndexOutOfBoundsException("off");
        if (len < 0 || off + len > b.length) throw new IndexOutOfBoundsException("len");

        if (len == 0) return;

        this.out.write((Integer.toString(len, 16) + "\r\n").getBytes());
        this.out.write(b, off, len);
        this.out.write(new byte[] { '\r', '\n' });
        this.out.flush();
    }

    @Override public void
    flush() {

        // No need to flush anything, because "write(byte[], int, int)" already flushes everything.
        ;
    }

    /**
     * Appends one message header to the trailer.
     */
    public void
    addHeader(String name, String value) {
        this.trailer.add(new MessageHeader(name, value));
    }

    @Override public void
    close() throws IOException {

        Writer w = new OutputStreamWriter(this.out, Charset.forName("ISO-8859-1"));
        {
            // Write "last chunk".
            w.write("0\r\n");

            // Write trailer.
            for (MessageHeader header : this.trailer) {
                w.write(header.getName() + ": " + header.getValue() + "\r\n");
            }

            // Write terminal CRLF.
            w.write("\r\n");
        }
        w.flush();

        super.close();
    }
}
