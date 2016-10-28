
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

package de.unkrig.commons.io;

import java.io.IOException;
import java.io.OutputStream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link OutputStream} which writes the data written to it to <i>two</i> delegate {@link OutputStream}s.
 *
 * @deprecated Prefer delegation over inheritance and use {@link IoUtil#tee(OutputStream...)} instead.
 */
@Deprecated @NotNullByDefault(false) public
class TeeOutputStream extends OutputStream {
    private final OutputStream left, right;

    public
    TeeOutputStream(OutputStream left, OutputStream right) {
        this.left  = left;
        this.right = right;
    }

    @Override public void
    close() throws IOException {
        try {
            this.left.close();
        } finally {
            this.right.close();
        }
    }

    @Override public void
    flush() throws IOException {
        this.left.flush();
        this.right.flush();
    }

    @Override public void
    write(byte[] b) throws IOException {
        this.left.write(b);
        this.right.write(b);
    }

    @Override public void
    write(byte[] b, int off, int len) throws IOException {
        this.left.write(b, off, len);
        this.right.write(b, off, len);
    }

    @Override public void
    write(int b) throws IOException {
        this.left.write(b);
        this.right.write(b);
    }
}
