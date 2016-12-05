
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

import javax.swing.text.Segment;

import de.unkrig.commons.lang.protocol.StringTransformers;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link Writer} which transforms the character stream on-the-fly with a {@link Transformer Transformer&lt;String>}.
 * <p>
 *   When the returned writer is flushed or closed, then the <var>transformer</var> is invoked with an empty
 *   char sequence, which tells it to "flush itself". This is relevant for stateful transformers.
 * </p>
 */
public
class TransformingFilterWriter extends Writer {

    private final Transformer<? super CharSequence, ? extends CharSequence> transformer;
    private final Appendable                                                delegate;

    /**
     * This method is sometimes more efficient than calling "{@code new TransformingFilterWriter()}".
     */
    public static Writer
    create(Transformer<? super CharSequence, ? extends CharSequence> transformer, Appendable delegate) {

        if (transformer == TransformerUtil.<CharSequence, CharSequence>identity()) {
            return Writers.fromAppendable(delegate);
        }

        if (transformer == StringTransformers.TO_EMPTY) return Writers.DISCARD;

        return new TransformingFilterWriter(transformer, delegate);
    }

    public
    TransformingFilterWriter(
        Transformer<? super CharSequence, ? extends CharSequence> transformer,
        Appendable                                                delegate
    ) {
        this.transformer = transformer;
        this.delegate    = delegate;
    }

    @NotNullByDefault(false) @Override public void
    write(char[] cbuf, int off, int len) throws IOException {
        this.append(new Segment(cbuf, off, len));
    }

    @NotNullByDefault(false) @Override public void
    write(String str, int off, int len) throws IOException {
        this.append(str, off, len);
    }

    @Override public void
    flush() throws IOException {
        this.delegate.append(this.transformer.transform(""));
        if (this.delegate instanceof Flushable) ((Flushable) this.delegate).flush();
    }

    @Override public void
    close() throws IOException {
        this.delegate.append(this.transformer.transform(""));
        if (this.delegate instanceof Closeable) ((Closeable) this.delegate).close();
    }

    @NotNullByDefault(false) @Override public Writer
    append(CharSequence csq) throws IOException {
        if (csq.length() > 0) this.delegate.append(this.transformer.transform(csq));
        return this;
    }

    @NotNullByDefault(false) @Override public Writer
    append(CharSequence csq, int start, int end) throws IOException {
        if (end > start) this.append(csq.subSequence(start, end));
        return this;
    }
}
