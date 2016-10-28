
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

import java.io.IOException;

/**
 * Abstract implementation of a {@link Pipe} by a "backing store" of bytes with fixed size (the "capacity").
 * <p>
 *   Implementation must implement methods {@link #get(long, byte[], int, int)} and {@link #put(byte[], int, int, long)}
 *   to provide access to the backing store.
 * </p>
 */
public abstract
class AbstractRingBuffer extends AbstractPipe {

    /**
     * @param capacity The size of the backing store
     */
    public
    AbstractRingBuffer(long capacity) { this.capacity = capacity; }

    /**
     * Transfers {@code len} bytes from the backing store to {@code buf[off]...}.
     *
     * @param pos 0...{@code capacity}
     * @param off 0...{@code buf.length}
     * @param len 0...{@code min(capacity - pos, buf.length - off}
     * @see #AbstractRingBuffer(long)
     */
    protected abstract void
    get(long pos, byte[] buf, int off, int len) throws IOException;

    /**
     * Transfers {@code len} bytes from {@code buf[off]...} to the backing store.
     *
     * @param off 0...{@code buf.length}
     * @param len 0...{@code min(capacity - pos, buf.length - off}
     * @param pos 0...{@code capacity}
     * @see #AbstractRingBuffer(long)
     */
    protected abstract void
    put(byte[] buf, int off, int len, long pos) throws IOException;

    // ------------ END PUBLIC API --------------

    /**
     * Size of the backing store; greater than zero.
     */
    private final long capacity;

    /**
     * Index of the next byte to {@link #read(byte[], int, int)} from the backing store; {@code 0 ... capacity-1};
     * {@code -1} means this ring buffer is empty.
     */
    private long head = -1;

    /**
     * Index in the backing store where the next byte written will be stored; {@code 0 ... capacity-1}; undefined
     * iff this ring buffer is empty.
     */
    private long tail;

    @Override public final int
    read(byte[] buf, int off, int len) throws IOException {

        if (len == 0) return 0;

        if (this.head == -1) return 0;  // This ring buffer is empty.

        if (this.head == this.tail) {

            // This ring buffer is full.
            // Buffer structure: [ >= 0 allocated] [(tail = head) >= 1 allocated ] (capacity)
            long av = this.capacity - this.head;
            if (len < av) {
                this.get(this.head, buf, off, len);
                this.head += len;
            } else
            if (len >= this.capacity) {
                this.get(this.head, buf, off, (int) av);
                this.get(0, buf, off + (int) av, (int) (this.capacity - av));
                this.head = -1;
                len       = (int) this.capacity;
            } else
            {
                this.get(this.head, buf, off, (int) av);
                this.get(0, buf, off + (int) av, len - (int) av);
                this.head = len - av;
            }

            return len;
        }

        if (this.tail > this.head) {

            // Buffer structure: [ >= 0 free] [(head) >= 1 allocated ] [(tail) >= 1 free ] (capacity)
            long av = this.tail - this.head;
            if (len < av) {
                this.get(this.head, buf, off, len);
                this.head += len;
                return len;
            } else {
                this.get(this.head, buf, off, (int) av);
                this.head = -1;
                return (int) av;
            }
        } else {

            // Buffer structure: [ >= 0 allocated] [(tail) >= 0 free ] [(head) >= 1 allocated ] (capacity)
            long av = this.capacity - this.head;
            if (len < av) {
                this.get(this.head, buf, off, len);
                this.head += len;
                return len;
            } else
            if (len < av + this.tail) {
                this.get(this.head, buf, off, (int) av);
                this.get(0, buf, off + (int) av, len - (int) av);
                this.head = len - av;
                return len;
            } else
            {
                this.get(this.head, buf, off, (int) av);
                this.get(0, buf, off + (int) av, (int) this.tail);
                this.head = -1;
                return (int) (av + this.tail);
            }
        }
    }

    @Override public final int
    write(byte[] buf, int off, int len) throws IOException {

        if (len == 0) return 0;

        if (this.head == -1) {

            // This ring buffer is empty.
            if (len > this.capacity) len = (int) this.capacity;
            this.put(buf, off, len, 0);
            this.head = 0;
            this.tail = len;

            return len;
        }

        if (this.tail > this.head) {

            // Buffer structure: [ >= 0 free] [(head) >= 1 allocated ] [(tail) >= 1 free ] (capacity)
            long av = this.capacity - this.tail;
            if (len < av) {
                this.put(buf, off, len, this.tail);
                this.tail += len;
                return len;
            } else
            if (len == av) {
                this.put(buf, off, len, this.tail);
                this.tail = 0;
                return len;
            } else
            if (len > av + this.head) {
                this.put(buf, off, (int) av, this.tail);
                this.put(buf, off + (int) av, (int) this.head, 0);
                this.tail = this.head;
                return (int) (av + this.head);
            } else
            {
                this.put(buf, off, (int) av, this.tail);
                this.put(buf, off + (int) av, (int) (len - av), 0);
                this.tail = len - av;
                return len;
            }
        } else {

            // Buffer structure: [ >= 0 allocated] [(tail) >= 0 free ] [(head) >= 1 allocated ] (capacity)
            long av = this.head - this.tail;
            if (len > av) len = (int) av;
            this.put(buf, off, len, this.tail);
            this.tail += len;
            return len;
        }
    }

    @Override public final boolean
    isEmpty() { return this.head == -1; }

    @Override public final boolean
    isFull() { return this.head == this.tail; }
}
