
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

package de.unkrig.commons.io.pipe;

import java.io.Closeable;
import java.io.IOException;

/**
 * {@link #read(byte[], int, int)} produces exactly the bytes that were previously {@link #write(byte[], int, int)
 * written}.
 */
public
interface Pipe extends Closeable {

    /**
     * Reads at most <var>buf</var>{@code .length} bytes from this pipe into the <var>buf</var>.
     *
     * @return The number of bytes read ({@code 1 ...} <var>buf</var>{@code .length}); 0 iff this pipe is empty
     */
    int read(byte[] buf) throws IOException;

    /**
     * Reads at most <var>len</var> bytes from this pipe into the <var>buf</var> at offset <var>off</var>.
     *
     * @return                  The number of bytes read ({@code 1 ... len}); 0 iff this pipe is empty
     * @throws RuntimeException {@code off} is negative
     * @throws RuntimeException {@code len} is negative
     * @throws RuntimeException {@code off+len} is greater than <var>buf</var>{@code .length}
     */
    int read(byte[] buf, int off, int len) throws IOException;

    /**
     * Writes at most <var>buf</var>{@code .length} bytes from <var>buf</var> to this pipe.
     *
     * @return The number of bytes written ({@code 1 ...} <var>buf</var>{@code .length}); 0 iff this pipe is full
     */
    int write(byte[] buf) throws IOException;

    /**
     * Writes at most <var>len</var> bytes from <var>buf</var> at offset <var>off</var> to this pipe.
     *
     * @return                  The number of bytes written ({@code 1 ... len}); 0 iff this pipe is full
     * @throws RuntimeException {@code off} is negative
     * @throws RuntimeException {@code len} is negative
     * @throws RuntimeException {@code off+len} is greater than {@code buf.length}
     */
    int write(byte[] buf, int off, int len) throws IOException;

    /**
     * Releases any resources allocated for this pipe. The results of subsequent invocations of {@link #read(byte[],
     * int, int)} and {@link #write(byte[], int, int)} are undefined.
     */
    @Override void close() throws IOException;

    /**
     * @return Whether the next invocation of {@link #write(byte[], int, int)} will return 0
     */
    boolean isFull();

    /**
     * @return Whether the next invocation of {@link #read(byte[], int, int)} will return 0
     */
    boolean isEmpty();
}
