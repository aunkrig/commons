
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

package de.unkrig.commons.lang;

import java.io.IOException;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link Appendable} which appends to a delegate appendable. This is very much in analogy with the {@link
 * java.io.FilterWriter}.
 * <p>
 *   {@link #append(CharSequence)} and {@link #append(CharSequence, int, int)} call {@link #append(char)},
 *   so extending classes must override at least {@link #append(char)}.
 * </p>
 */
public abstract
class FilterAppendable implements Appendable {

    protected final Appendable delegate;

    public
    FilterAppendable(Appendable delegate) { this.delegate = delegate; }

    @Override @NotNullByDefault(false) public Appendable
    append(CharSequence csq) throws IOException {
        this.delegate.append(csq);
        return this;
    }

    @Override @NotNullByDefault(false) public Appendable
    append(CharSequence csq, int start, int end) throws IOException {
        this.delegate.append(csq, start, end);
        return this;
    }

    @Override public Appendable
    append(char c) throws IOException {
        this.delegate.append(c);
        return this;
    }
}
