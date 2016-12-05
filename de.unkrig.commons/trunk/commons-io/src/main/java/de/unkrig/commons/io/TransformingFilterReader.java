
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

import javax.swing.text.Segment;

import de.unkrig.commons.lang.protocol.StringTransformers;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link FilterReader} which transforms the character stream on-the-fly with a {@link Transformer
 * Transformer&lt;String>}.
 * <p>
 *   When the delegate reader is at end-of-input, then the <var>transformer</var> is invoked with an empty char
 *   sequence, which tells it to "flush itself". This is relevant for stateful transformers.
 * </p>
 */
public
class TransformingFilterReader extends FilterReader {

    private final Transformer<? super CharSequence, ? extends CharSequence> transformer;

    /**
     * Sometimes we have "too many" characters than we can return, so we set them aside in this buffer.
     */
    private String buffer = "";

    /**
     * This method is sometimes more efficient than calling "{@code new TransformingFilterReader()}".
     */
    public static Reader
    create(
        Reader                                                          delegate,
        final Transformer<? super CharSequence, ? extends CharSequence> transformer
    ) {

        if (transformer == TransformerUtil.<CharSequence, CharSequence>identity()) return delegate;

        if (transformer == StringTransformers.TO_EMPTY) return Readers.EMPTY_READER;

        return new TransformingFilterReader(delegate, transformer);
    }

    public
    TransformingFilterReader(
        Reader                                                          delegate,
        final Transformer<? super CharSequence, ? extends CharSequence> transformer
    ) {
        super(delegate);
        this.transformer = transformer;
    }

    @Override public int
    read() throws IOException {

        if (!this.buffer.isEmpty()) {
            char c = this.buffer.charAt(0);
            this.buffer = this.buffer.substring(1);
            return c;
        }

        int c = this.in.read();
        if (c == -1) {
            CharSequence csq = this.transformer.transform("");
            if (csq.length() == 0) return -1;
            char c2 = csq.charAt(0);
            this.buffer = csq.subSequence(1, csq.length()).toString();
            return c2;
        }

        CharSequence csq = this.transformer.transform(new String(new char[] { (char) c }));
        if (csq.length() == 0) return -1;
        char c2 = csq.charAt(0);
        this.buffer = csq.subSequence(1, csq.length()).toString();
        return c2;
    }

    @Override public int
    read(@Nullable char[] cbuf, int off, int len) throws IOException {

        while (this.buffer.isEmpty()) {
            char[] ca = new char[1024];

            int n = this.in.read(ca);
            if (n < 0) {
                this.buffer = this.transformer.transform("").toString();
                if (this.buffer.isEmpty()) return -1;
                break;
            } else
            if (n == 0) {
                return 0;
            } else
            if (n > 0) {
                this.buffer = this.transformer.transform(new Segment(ca, 0, n)).toString();
            }
        }

        int bl = this.buffer.length();
        if (bl < len) {
            System.arraycopy(this.buffer.toCharArray(), 0, cbuf, off, bl);
            this.buffer = "";
            return bl;
        } else {
            System.arraycopy(this.buffer.toCharArray(), 0, cbuf, off, len);
            this.buffer = this.buffer.substring(len);
            return len;
        }
    }

    @Override public long
    skip(long n) throws IOException {

        if (n < 0L) throw new IllegalArgumentException("skip value is negative");

        int nn = (int) Math.min(n, TransformingFilterReader.MAX_SKIP_BUFFER_SIZE);

        char[] skipb = this.skipBuffer;
        if (skipb == null || skipb.length < nn) skipb = (this.skipBuffer = new char[nn]);

        int nc;
        for (long r = n; r > 0; r -= nc) {
            nc = this.read(skipb, 0, (int) Math.min(r, nn));
            if (nc == -1) return n - r;
        }
        return 0;
    }
    private static final int MAX_SKIP_BUFFER_SIZE = 8192;
    @Nullable private char[] skipBuffer;

    @Override public boolean
    ready() { return !this.buffer.isEmpty(); }

    @Override public boolean
    markSupported() { return false; }

    @Override public void
    mark(int readAheadLimit) throws IOException { throw new IOException("mark() not supported"); }

    @Override public void
    reset() throws IOException { throw new IOException("reset() not supported"); }
}
