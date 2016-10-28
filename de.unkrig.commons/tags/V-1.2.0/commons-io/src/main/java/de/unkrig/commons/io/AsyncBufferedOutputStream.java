
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link FilterOutputStream} that forwards data <i>asynchronously</i> (with a background thread) to the delegate
 * {@link OutputStream}.
 */
@NotNullByDefault(false) public
class AsyncBufferedOutputStream extends FilterOutputStream {

    private ByteBuffer buffer;

    private
    enum State {
        CLOSED,
        EMPTY, // head == tail
        PART,  // head != tail
        FULL   // head == tail
    }
    private State state = State.EMPTY;

    private int       tail, head;
    private final int capacity;

    private final ReentrantLock lock;
    private final Condition     notEmpty;
    private final Condition     notFull;

    private Exception exception;

    /**
     * @param buffer Typically {@code ByteBuffer.allocate(capacity)} for a heap buffer or {@code
     *               ByteBuffer.allocateDirect(capacity)} for an off-heap buffer
     * @param fair   See {@link ReentrantLock}
     */
    public
    AsyncBufferedOutputStream(OutputStream out, final ByteBuffer buffer, boolean fair) {
        super(out);
        this.buffer   = buffer;
        this.capacity = buffer.capacity();
        this.lock     = new ReentrantLock(fair);
        this.notEmpty = this.lock.newCondition();
        this.notFull  = this.lock.newCondition();

        new Thread() {

            @Override public void
            run() {
                try {
                    AsyncBufferedOutputStream.this.drain();
                } catch (Exception e) {
                    AsyncBufferedOutputStream.this.exception = e;
                }
            }
        }.start();
    }

    /**
     * @throws IOException The background thread got this exception
     */
    @Override public void
    write(int b) throws IOException {
        this.lock.lock();
        try {
            if (this.state == State.CLOSED) throw new IOException("Stream is closed");

            this.waitUntilNotFull();

            // Put the byte.
            this.buffer.put(this.tail, (byte) b);
            this.tail  = (this.tail + 1) % this.capacity;
            this.state = this.tail == this.head ? State.FULL : State.PART;

            // Wake up the background thread.
            this.notEmpty.signal();
        } finally {
            this.lock.unlock();
        }
    }

    @Override public void
    write(byte[] ba, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > ba.length) throw new IndexOutOfBoundsException();

        if (len == 0) return;

        this.lock.lock();
        try {
            if (this.state == State.CLOSED) throw new IOException("Stream is closed");

            for (;;) {
                this.waitUntilNotFull();

                this.buffer.position(this.tail);

                // How many bytes can be put in one block?
                int n = this.head <= this.tail ? this.capacity - this.tail : this.head - this.tail;

                // Put that many bytes.
                if (len <= n) {
                    this.buffer.put(ba, off, len);
                    this.tail  += len;
                    this.state = State.PART;
                    this.notEmpty.signal();
                    return;
                }
                this.buffer.put(ba, off, n);
                this.tail  = (this.tail + n) % this.capacity;
                this.state = this.tail == this.head ? State.FULL : State.PART;
                off        += n;
                len        -= n;

                // Wake up the background thread.
                this.notEmpty.signal();
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override public void
    flush() throws IOException {
        this.lock.lock();
        try {
            if (this.state == State.CLOSED) throw new IOException("Stream is closed");

            this.waitUntilEmpty();

            this.out.flush();
        } finally {
            this.lock.unlock();
        }
    }

    @Override public void
    close() throws IOException {
        this.lock.lock();
        try {
            this.flush();
            this.state  = State.CLOSED;
            this.buffer = null;
            this.out.close();
            this.notEmpty.signal();
        } finally {
            this.lock.unlock();
        }
    }

    private void
    waitUntilNotFull() throws IOException {
        try {
            while (this.state == State.FULL) this.notFull.await();
        } catch (InterruptedException ie) {
            throw new InterruptedIOException(); // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
        this.checkException();
    }

    private void
    waitUntilEmpty() throws IOException {
        try {
            while (this.state != State.EMPTY) this.notFull.await();
        } catch (InterruptedException ie) {
            throw new InterruptedIOException(); // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
        this.checkException();
    }

    private void
    waitUntilNotEmpty() throws IOException {
        try {
            while (this.state == State.EMPTY) this.notEmpty.await();
        } catch (InterruptedException e) {
            throw new InterruptedIOException(); // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
        this.checkException();
    }

    private void
    checkException() throws IOException {
        if (this.exception == null) return;

        if (this.exception instanceof IOException) {
            throw ExceptionUtil.wrap(null, (IOException) this.exception);
        } else
        if (this.exception instanceof RuntimeException) {
            throw ExceptionUtil.wrap(null, (RuntimeException) this.exception);
        } else
        {
            throw ExceptionUtil.wrap(null, this.exception, IOException.class);
        }
    }

    private void
    drain() throws IOException {

        this.lock.lock();
        try {
            while (this.state != State.CLOSED) {
                this.waitUntilNotEmpty();

                for (;;) {

                    // Get one chunk of data from the buffer.
                    byte[] ba;
                    {
                        if (this.state != State.PART && this.state != State.FULL) break;

                        int n = this.head >= this.tail ? this.capacity - this.head : this.tail - this.head;
                        if (n > 4096) n = 4096;

                        ba = new byte[n];
                        this.buffer.position(this.head);
                        this.buffer.get(ba);

                        this.head = (this.head + n) % this.capacity;
                        if (this.head == this.tail) this.state = State.EMPTY;
                        this.notFull.signal();
                    }

                    this.out.write(ba);
                }
            }
        } finally {
            this.lock.unlock();
        }
    }
}
