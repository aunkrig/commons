
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
import java.io.InputStream;
import java.io.Reader;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Reads characters from a delegate {@link Reader} and encodes them into data bytes (assuming ISO-8859-1 encoding).
 * In a sense, this class is the complement of the {@link java.io.OutputStreamWriter}.
 */
@NotNullByDefault(false) public
class ReaderInputStream extends InputStream {

    private final Reader delegate;

    public
    ReaderInputStream(Reader delegate) { this.delegate = delegate; }

    @Override public int
    read() throws IOException {
        int result = this.delegate.read();
        return result == -1 ? -1 : 0xff & result;
    }

    @Override public int
    available() throws IOException { return this.delegate.ready() ? 1 : 0; }

    @Override public void
    mark(int readlimit) {
        try {
            this.delegate.mark(readlimit);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override public synchronized void
    reset() throws IOException { this.delegate.reset(); }

    @Override public boolean
    markSupported() { return this.delegate.markSupported(); }

    @Override public void
    close() throws IOException {
        this.delegate.close();
    }
}
