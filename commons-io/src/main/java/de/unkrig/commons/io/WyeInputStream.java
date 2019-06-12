
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Duplicates all bytes that it reads to an {@link OutputStream}.
 * <p>
 *   The {@link OutputStream} is flushed on end-of-input and calls to {@link #available()}.
 * </p>
 *
 * @deprecated Prefer delegation over inheritance and use {@link InputStreams#wye(InputStream, OutputStream)}
 *             instead
 */
@Deprecated @NotNullByDefault(false) public
class WyeInputStream extends FilterInputStream {

    private final OutputStream out;

    public
    WyeInputStream(InputStream in, OutputStream out) {
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
    read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) this.out.write(b, off, count);
        if (count == 0) this.out.flush();
        return count;
    }

    @Override public int
    available() throws IOException {
        this.out.flush();
        return this.in.available();
    }
}
