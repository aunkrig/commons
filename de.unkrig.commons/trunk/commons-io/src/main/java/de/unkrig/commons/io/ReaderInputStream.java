
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Reads characters from a delegate {@link Reader} and encodes them into data bytes. In a sense, this class is the
 * complement of the {@link java.io.InputStreamReader}.
 */
@NotNullByDefault(false) public
class ReaderInputStream extends InputStream {

    private final Reader         delegate;
    private final CharsetEncoder encoder;

    private final CharBuffer in  = CharBuffer.allocate(1024);
    private final ByteBuffer out = ByteBuffer.allocate(128);

    private CoderResult lastCoderResult;
    private boolean     endOfInput;

    /**
     * Creates a {@link ReaderInputStream} with the JVM default charset.
     */
    public
    ReaderInputStream(Reader delegate) { this(delegate, Charset.defaultCharset()); }

    public
    ReaderInputStream(Reader delegate, String charsetName) { this(delegate, Charset.forName(charsetName)); }

    public
    ReaderInputStream(Reader delegate, Charset charset) {
        this(
            delegate,
            (
                charset
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
            ).onUnmappableCharacter(CodingErrorAction.REPLACE)
        );
    }

    public
    ReaderInputStream(Reader delegate, CharsetEncoder encoder) {
        this.delegate = delegate;
        this.encoder  = encoder;
        this.in.flip();
        this.out.flip();
    }

    @Override public int
    read() throws IOException {

        for (;;) {

            if (this.out.hasRemaining()) return this.out.get() & 0xff;

            this.fillBuffer();

            if (this.endOfInput && !this.out.hasRemaining()) return -1;
        }
    }

    @Override public int
    read(byte[] b, int off, int len) throws IOException {

        if (b == null)                       throw new NullPointerException("buf");
        if (off < 0)                         throw new IndexOutOfBoundsException("off");
        if (len < 0 || off + len > b.length) throw new IndexOutOfBoundsException("len");

        if (len == 0) return 0;

        int count = 0;
        while (len > 0) {
            if (this.out.hasRemaining()) {
                int c = Math.min(this.out.remaining(), len);
                this.out.get(b, off, c);
                off   += c;
                len   -= c;
                count += c;
            } else {
                this.fillBuffer();
                if (this.endOfInput && !this.out.hasRemaining()) break;
            }
        }

        return count == 0 && this.endOfInput ? -1 : count;
    }

    @Override public int
    available() throws IOException { return this.out.hasRemaining() || this.delegate.ready() ? 1 : 0; }

    @Override public void
    close() throws IOException { this.delegate.close(); }

    private void
    fillBuffer() throws IOException {

        if (!this.endOfInput && (this.lastCoderResult == null || this.lastCoderResult.isUnderflow())) {

            this.in.compact();

            int count = this.delegate.read(this.in.array(), this.in.position(), this.in.remaining());

            if (count == -1) {
                this.endOfInput = true;
            } else {
                this.in.position(this.in.position() + count);
            }

            this.in.flip();
        }

        this.out.compact();
        this.lastCoderResult = this.encoder.encode(this.in, this.out, this.endOfInput);
        this.out.flip();
    }
}
