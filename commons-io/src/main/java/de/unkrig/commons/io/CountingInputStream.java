
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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
import java.util.concurrent.atomic.AtomicLong;

import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link FilterInputStream} that counts the number of bytes read through it.
 *
 * @deprecated Use {@link InputStreams#wye(InputStream, java.io.OutputStream)}, {@link
 *             OutputStreams#lengthWritten(de.unkrig.commons.lang.protocol.ConsumerWhichThrows)}, {@link
 *             ConsumerUtil#cumulate(de.unkrig.commons.lang.protocol.Consumer, long)} and {@link ConsumerUtil#store()}
 *             instead.
 */
@Deprecated
@NotNullByDefault(false) public
class CountingInputStream extends FilterInputStream {

    private final AtomicLong count;

    public
    CountingInputStream(InputStream in) {
        super(in);
        this.count = new AtomicLong();
    }

    public
    CountingInputStream(InputStream in, long initialCount) {
        super(in);
        this.count = new AtomicLong(initialCount);
    }

    @Override public int
    read() throws IOException {
        int c = this.in.read();
        if (c != -1) this.count.incrementAndGet();
        return c;
    }

    @Override public int
    read(byte[] b, int off, int len) throws IOException {
        int n = this.in.read(b, off, len);
        if (n > 0) this.count.addAndGet(n);
        return n;
    }

    @Override public long
    skip(long n) throws IOException {
        long skipped = this.in.skip(n);
        this.count.addAndGet(skipped);
        return skipped;
    }

    @Override public synchronized void
    mark(int readlimit) {}

    @Override public synchronized void
    reset() throws IOException { throw new IOException("mark/reset not supported"); }

    @Override public boolean
    markSupported() { return false; }
}
