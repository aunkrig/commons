
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

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link OutputStream} that counts the number of bytes written to it.
 *
 * @deprecated Use {@link OutputStreams#lengthWritten(de.unkrig.commons.lang.protocol.ConsumerWhichThrows)} and {@link
 *             ConsumerUtil#cumulate(de.unkrig.commons.lang.protocol.ConsumerWhichThrows, long)} instead.
 */
@Deprecated @NotNullByDefault(false) public
class CountingOutputStream extends OutputStream {

    private final AtomicLong count;

    public
    CountingOutputStream() { this.count = new AtomicLong(); }

    public
    CountingOutputStream(long initialCount) { this.count = new AtomicLong(initialCount); }

    @Override public void
    write(int b) { this.count.incrementAndGet(); }

    @Override public void
    write(byte[] b, int off, int len) { this.count.addAndGet(len); }

    /**
     * @return The current count
     */
    public long
    getCount() { return this.count.get(); }
}
