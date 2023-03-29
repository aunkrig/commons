
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2022, Arno Unkrig
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
import java.io.Writer;

import javax.swing.text.Segment;

import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link Writer} which transfers the character stream to a {@link Consumer}.
 * <p>
 *   When the returned writer is flushed or closed, then the <var>consumer</var> is invoked with an empty
 *   char sequence, which tells it to "flush itself". This is relevant for stateful consumers.
 * </p>
 */
public
class ConsumingWriter extends Writer {

    private final ConsumerWhichThrows<? super CharSequence, ? extends IOException> consumer;

    /**
     * This method is sometimes more efficient than calling "{@code new ConsumingWriter()}".
     */
    public static Writer
    create(ConsumerWhichThrows<? super CharSequence, ? extends IOException> consumer) {
        return new ConsumingWriter(consumer);
    }

    public
    ConsumingWriter(ConsumerWhichThrows<? super CharSequence, ? extends IOException> consumer) {
        this.consumer = consumer;
    }

    @NotNullByDefault(false) @Override public void
    write(char[] cbuf, int off, int len) throws IOException { this.append(new Segment(cbuf, off, len)); }

    @NotNullByDefault(false) @Override public void
    write(String str, int off, int len) throws IOException { this.append(str, off, len); }

    @Override public void
    flush() throws IOException { this.consumer.consume(""); }

    @Override public void
    close() throws IOException { this.flush(); }

    @NotNullByDefault(false) @Override public Writer
    append(CharSequence csq) throws IOException {
        if (csq.length() > 0) this.consumer.consume(csq);
        return this;
    }

    @NotNullByDefault(false) @Override public Writer
    append(CharSequence csq, int start, int end) throws IOException {
        if (end > start) this.append(csq.subSequence(start, end));
        return this;
    }
}
