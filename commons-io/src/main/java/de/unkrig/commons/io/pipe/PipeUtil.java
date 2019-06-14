
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

package de.unkrig.commons.io.pipe;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import de.unkrig.commons.io.BlockingException;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to the {@link Pipe} interface.
 */
public final
class PipeUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private PipeUtil() {}

    /**
     * A tuple of one {@link InputStream} and one {@link OutputStream}.
     *
     * @see PipeUtil#asInputOutputStreams(Pipe)
     */
    public
    interface InputOutputStreams {

        /** @see InputOutputStreams */
        InputStream  getInputStream();

        /** @see InputOutputStreams */
        OutputStream getOutputStream();
    }

    /**
     * Creates and returns a pair of output stream and input stream which write to and read from the given {@code
     * pipe}. The streams block the calling thread while the pipe is full resp. empty.
     * <p>
     *   This method is equivalent with
     * </p>
     * <pre>
     *   asInputOutputStream(pipe, true);
     * </pre>
     */
    public static InputOutputStreams
    asInputOutputStreams(final Pipe pipe) { return PipeUtil.asInputOutputStreams(pipe, true); }

    /**
     * Creates and returns a pair of output stream and input stream which write to and read from the given {@code
     * pipe}.
     * <p>
     *   While the pipe is full: If <var>blocking</var>{@code == true} then the output stream blocks until space is
     *   available. Otherwise, if <var>blocking</var>{@code == false}, the output stream throws a {@link
     *   BlockingException}.
     * </p>
     * <p>
     *   While the pipe is empty: If <var>blocking</var>{@code == true} then the input stream blocks until data is
     *   available, otherwise, if <var>blocking</var>{@code == false}, the input stream throws a {@link
     *   BlockingException}.
     * </p>
     * <p>
     *   Closing the output stream indicates an end-of-input condition to the input stream.
     * </p>
     * <p>
     *   Closing the input stream closes the <var>pipe</var>, and the next write attempt to the output stream will cause
     *   an {@link EOFException}.
     * </p>
     */
    public static InputOutputStreams
    asInputOutputStreams(final Pipe pipe, final boolean blocking) {

        class MyInputOutputStreams implements InputOutputStreams {

            boolean inputStreamClosed, outputStreamClosed;

            InputStream inputStream = new InputStream() {

                @Override public int
                available() { return pipe.isEmpty() ? 0 : 1; }

                @Override public int
                read() throws IOException {
                    byte[] ba = new byte[1];
                    if (this.read(ba, 0, 1) == -1) return -1;
                    return 0xff & ba[0];
                }

                @Override public synchronized int
                read(@Nullable byte[] buf, int off, int len) throws IOException {
                    assert buf != null;

                    for (;;) {

                        boolean wasFull = pipe.isFull();

                        int n = pipe.read(buf, off, len);
                        if (n > 0) {
                            if (wasFull) {
                                synchronized (MyInputOutputStreams.this.outputStream) {
                                    MyInputOutputStreams.this.outputStream.notifyAll();
                                }
                            }
                            return n;
                        }

                        if (MyInputOutputStreams.this.outputStreamClosed) return -1;

                        if (!blocking) throw new BlockingException();

                        try {
                            this.wait();
                        } catch (InterruptedException ie) {
                            throw (InterruptedIOException) new InterruptedIOException().initCause(ie);
                        }
                    }
                }

                @Override public void
                close() throws IOException {
                    MyInputOutputStreams.this.inputStreamClosed = true;
                    pipe.close();
                }
            };

            OutputStream outputStream = new OutputStream() {

                @Override public void
                write(int b) throws IOException { this.write(new byte[] { (byte) b }, 0, 1); }

                @Override public synchronized void
                write(@Nullable byte[] buf, int off, int len) throws IOException {
                    assert buf != null;

                    for (;;) {

                        if (MyInputOutputStreams.this.inputStreamClosed) throw new EOFException();
                        boolean wasEmpty = pipe.isEmpty();

                        int n = pipe.write(buf, off, len);
                        if (wasEmpty) {
                            synchronized (MyInputOutputStreams.this.inputStream) {
                                MyInputOutputStreams.this.inputStream.notifyAll();
                            }
                        }
                        if (n == len) return;
                        if (n == 0) {
                            if (!blocking) throw new BlockingException();
                            try {
                                this.wait();
                            } catch (InterruptedException ie) {
                                throw (InterruptedIOException) new InterruptedIOException().initCause(ie);
                            }
                        } else {
                            off += n;
                            len -= n;
                        }
                    }
                }

                @Override public void
                close() { MyInputOutputStreams.this.outputStreamClosed = true; }
            };

            @Override public InputStream
            getInputStream() { return this.inputStream; }

            @Override public OutputStream
            getOutputStream() { return this.outputStream; }
        }

        return new MyInputOutputStreams();
    }

    /**
     * @return A pipe which forwards all operations to the <var>delegate</var>, and, in addition, deletes the
     *         <var>file</var> when the pipe is {@link Pipe#close() closed}
     */
    public static Pipe
    deleteFileOnClose(final Pipe delegate, final File file) {

        return PipeUtil.onClose(delegate, new RunnableWhichThrows<IOException>() {

            @Override public void
            run() throws IOException {
                if (!file.delete()) throw new IOException("delete");
            }
        });
    }

    /**
     * @return A pipe which forwards all operations to the given <var>delegate</var>, and, in addition, runs the given
     *         <var>runnable</var> on {@link Pipe#close()} AFTER having closed the <var>delegate</var>
     */
    public static Pipe
    onClose(final Pipe delegate, final RunnableWhichThrows<IOException> runnable) {

        return new AbstractPipe() {

            @Override public int     read(byte[] buf, int off, int len) throws IOException  { return delegate.read(buf, off, len);  } // SUPPRESS CHECKSTYLE LineLength:3
            @Override public int     write(byte[] buf, int off, int len) throws IOException { return delegate.write(buf, off, len); }
            @Override public boolean isEmpty()                                              { return delegate.isEmpty();            }
            @Override public boolean isFull()                                               { return delegate.isFull();             }

            @Override public void
            close() throws IOException {

                try {
                    delegate.close();
                } finally {
                    runnable.run();
                }
            }
        };
    }

    /**
     * Convenience method for {@link #temporaryStorage(Filler, Drainer)} when the filler wants to pass information to
     * the drainer.
     */
    public static void
    temporaryStorage(FillerAndDrainer fillerAndDrainer) throws IOException {
        PipeUtil.temporaryStorage(fillerAndDrainer, fillerAndDrainer);
    }

    /**
     * Creates a temporary {@link PipeFactory#elasticPipe()}, invokes the <var>filler</var> (which fills the pipe),
     * then invokes the <var>drainer</var> (which drains the pipe), and eventually closes the pipe.
     */
    public static void
    temporaryStorage(Filler filler, Drainer drainer) throws IOException {

        InputOutputStreams ios = PipeUtil.asInputOutputStreams(PipeFactory.elasticPipe());

        OutputStream os  = ios.getOutputStream();
        InputStream  is  = ios.getInputStream();
        try {
            filler.fill(os);
            os.close();
            drainer.drain(is);
            is.close();
        } finally {
            try { is.close(); } catch (Exception e) {}
            try { os.close(); } catch (Exception e) {}
        }
    }

    /**
     * Fills 'something' by writing data to an output stream.
     *
     * @see #fill(OutputStream)
     * @see PipeUtil#temporaryStorage(FillerAndDrainer)
     */
    @SuppressWarnings("null") @NotNullByDefault public
    interface Filler {

        /**
         * Writes data to the given output stream.
         */
        void fill(OutputStream os) throws IOException;
    }

    /**
     * Drains 'something' by reading data from an input stream.
     *
     * @see #drain(InputStream)
     * @see PipeUtil#temporaryStorage(FillerAndDrainer)
     */
    @SuppressWarnings("null") @NotNullByDefault public
    interface Drainer {

        /**
         * Reads form the given input stream exactly the data that was previously written.
         *
         * @see Filler#fill(OutputStream)
         */
        void drain(InputStream is) throws IOException;
    }

    /**
     * @see Filler
     * @see Drainer
     * @see PipeUtil#temporaryStorage(FillerAndDrainer)
     */
    @SuppressWarnings("null") @NotNullByDefault public
    interface FillerAndDrainer extends Filler, Drainer {}
}
