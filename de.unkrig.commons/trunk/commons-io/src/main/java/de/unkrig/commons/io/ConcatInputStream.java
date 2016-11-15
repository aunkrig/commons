
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

import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Concatenates the contents of several {@link InputStream}s.
 */
@NotNullByDefault(false) public
class ConcatInputStream extends InputStream {

    private final Producer<? extends InputStream> delegates;
    private final boolean                         closeStreams;

    /** {@code null} means 'end of input'. */
    @Nullable private InputStream current = IoUtil.EMPTY_INPUT_STREAM;

    /**
     * @param delegates    A {@code null} product means 'end-of-input'
     * @param closeStreams Whether to close each delegate input stream right after its data has been read
     */
    public
    ConcatInputStream(Producer<? extends InputStream> delegates, boolean closeStreams) {
        this.closeStreams = closeStreams;
        this.delegates    = delegates;
    }

    @Override public int
    available() throws IOException {
        InputStream is = this.current;
        return is == null ? 0 : is.available();
    }

    @Override public void
    close() throws IOException {
        if (this.current != null) {
            for (;;) {
                InputStream is = this.delegates.produce();
                if (is == null) break;
                is.close();
            }
        }
    }

    @Override public int
    read() throws IOException {
        byte[] buffer = new byte[1];
        return this.read(buffer, 0, 1) == -1 ? -1 : 0xff & buffer[0];
    }

    @Override public int
    read(byte[] b, int off, int len) throws IOException {
        for (;;) {
            InputStream is =  this.current;
            if (is == null) break;

            int n = is.read(b, off, len);
            if (n != -1) return n;

            if (this.closeStreams) is.close();
            this.current = this.delegates.produce();
        }
        return -1;
    }

    @Override public long
    skip(long n) throws IOException {
        for (;;) {
            InputStream is = this.current;
            if (is == null) return 0L;

            long result = is.skip(n);
            if (result > 0) return result;

            this.current = this.delegates.produce();
        }
    }
}
