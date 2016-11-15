
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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
import java.io.Writer;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Duplicates all bytes that it reads to a {@link Writer}.
 * <p>
 *   The {@link Writer} is flushed on end-of-input.
 * </p>
 * <p>
 *   The {@link Writer} is <i>not</i> closed on {@link #close()}.
 * </p>
 */
public
class WyeReader extends FilterReader {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private final Writer out;

    public
    WyeReader(Reader in, Writer out) {
        super(in);
        this.out = out;
    }

    @Override public int
    read() throws IOException {
        int b = super.read();
        if (b == -1) {
            this.out.flush();
        } else {
            this.out.write(b);
        }
        return b;
    }

    @Override public int
    read(@Nullable char[] cbuf, int off, int len) throws IOException {
        assert cbuf != null;

        int count = super.read(cbuf, off, len);
        if (count > 0) this.out.write(cbuf, off, count);
        if (count == 0) this.out.flush();

        return count;
    }
}
