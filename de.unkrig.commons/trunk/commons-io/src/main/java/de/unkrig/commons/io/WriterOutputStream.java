
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Decodes the data bytes into characters, and writes them to a given delegate {@link Writer}. In a sense, this class
 * is the complement of the {@link java.io.OutputStreamWriter}.
 */
@NotNullByDefault(false) public
class WriterOutputStream extends OutputStream {

    private final Writer         delegate;
    private final CharsetDecoder decoder;

    private final ByteBuffer in  = ByteBuffer.allocate(128);
    private final CharBuffer out = CharBuffer.allocate(128);

    /** @see WriterOutputStream */
    public
    WriterOutputStream(Writer delegate) { this(delegate, Charset.defaultCharset()); }

    /** @see WriterOutputStream */
    public
    WriterOutputStream(Writer delegate, String charsetName) { this(delegate, Charset.forName(charsetName)); }

    /** @see WriterOutputStream */
    public
    WriterOutputStream(Writer delegate, Charset charset) {
        this(
            delegate,
            (
                charset
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
            ).replaceWith("?")
        );
    }

    /** @see WriterOutputStream */
    public
    WriterOutputStream(Writer delegate, CharsetDecoder decoder) {
        this.delegate = delegate;
        this.decoder  = decoder;
    }

    @Override public void
    close() throws IOException {
        this.processInput(true);
        this.delegate.close();
    }

    @Override public void
    flush() throws IOException {
        this.delegate.flush();
    }

    @Override public void
    write(byte[] buf, int off, int len) throws IOException {

        while (len > 0) {
            int n = Math.min(len, this.in.remaining());
            this.in.put(buf, off, n);
            this.processInput(false);
            len -= n;
            off += n;
        }

        this.writeOutput();
    }

    @Override public void
    write(int b) throws IOException { this.write(new byte[] { (byte) b }, 0, 1); }

    private void
    processInput(boolean endOfInput) throws IOException {

        this.in.flip();

        for (;;) {

            CoderResult coderResult = this.decoder.decode(this.in, this.out, endOfInput);

            if (coderResult.isUnderflow()) break;

            if (coderResult.isOverflow()) {
                this.writeOutput();
            } else {
                throw new AssertionError(coderResult);
            }
        }

        this.in.compact();
    }

    private void
    writeOutput() throws IOException {

        if (this.out.position() == 0) return;

        this.delegate.write(this.out.array(), 0, this.out.position());

        // Java 9 added "@Override public final CharBuffer CharBuffer.rewind() { ..." -- leads easily to a
        //     java.lang.NoSuchMethodError: java.nio.CharBuffer.rewind()Ljava/nio/CharBuffer;
        // Cast to "Buffer" is the workaround:
        ((Buffer) this.out).rewind();
    }
}
