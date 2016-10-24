
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link FilterInputStream} that transforms the byte stream through a {@link ByteFilter}. Any {@link IOException}
 * and {@link RuntimeException} that {@link ByteFilter#run} throws is caught and rethrown by the {@link #read}
 * methods of this object.
 */
public
class ByteFilterInputStream extends FilterInputStream {

    private final Thread               worker;
    @Nullable private IOException      byteFilterIOException;
    @Nullable private RuntimeException byteFilterRuntimeException;

    /**
     * @see ByteFilterInputStream
     */
    public
    ByteFilterInputStream(final InputStream in, final ByteFilter<?> byteFilter) {
        super(new PipedInputStream());

        final PipedOutputStream pos;
        try {
            pos = new PipedOutputStream((PipedInputStream) this.in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.worker = new Thread("PipeFilterInputStream") {

            @Override public void
            run() {
                try {
                    byteFilter.run(in, pos);
                } catch (IOException ioe) {
                    ByteFilterInputStream.this.byteFilterIOException = ioe;
                } catch (RuntimeException re) {
                    ByteFilterInputStream.this.byteFilterRuntimeException = re;
                } finally {

                    // This will signal "end-of-input" to the read side.
                    try { pos.close(); } catch (IOException ioe) {}
                }
            }
        };
        this.worker.start();
    }

    @Override public int
    read() throws IOException {
        byte[] buffer = new byte[1];
        for (;;) {
            int n = this.read(buffer, 0, 1);
            if (n == -1) return -1;
            if (n == 1) return 0xff & buffer[0];
            if (n != 0) throw new IllegalStateException();
        }
    }

    @NotNullByDefault(false) @Override public int
    read(byte[] b, int off, int len) throws IOException {
        final int n = this.in.read(b, off, len);

        if (this.byteFilterIOException != null) {

            // Wrap because the byteFileIOException comes from a different thread, and we want to document the call
            // stack of THIS thread as well.
            throw ExceptionUtil.wrap("ByteFilterInputStream", this.byteFilterIOException);
        }

        if (this.byteFilterRuntimeException != null) {

            // Wrap because the byteFilterRuntimeException comes from a different thread, and we want to document the
            // call stack of THIS thread as well.
            throw ExceptionUtil.wrap("ByteFilterInputStream", this.byteFilterRuntimeException);
        }

        return n;
    }

    @Override public void
    close() throws IOException {
        try {

            // This will put the pipe into the "broken" state, and the worker thread is very likely to terminate
            // quickly.
            this.in.close();
        } finally {
            for (;;) {
                try {
                    this.worker.join();
                    break;
                } catch (InterruptedException ie) {
                    ;
                }
            }
        }

        if (this.byteFilterIOException != null) {

            // Wrap because the byteFileIOException comes from a different thread, and we want to document the call
            // stack of THIS thread as well.
            throw ExceptionUtil.wrap("ByteFilterInputStream", this.byteFilterIOException);
        }

        if (this.byteFilterRuntimeException != null) {

            // Wrap because the byteFilterRuntimeException comes from a different thread, and we want to document the
            // call stack of THIS thread as well.
            throw ExceptionUtil.wrap("ByteFilterInputStream", this.byteFilterRuntimeException);
        }
    }
}
