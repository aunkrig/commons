
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil.Produmer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility functionality related to {@link OutputStream}s.
 */
public final
class OutputStreams {

    private OutputStreams() {}

    /** An {@link OutputStream} that discards all bytes written to it. */
    public static final OutputStream
    DISCARD = new OutputStream() {
        @Override public void write(@Nullable byte[] b, int off, int len) {}
        @Override public void write(int b)                                {}
    };

    /**
     * Creates and returns an {@link OutputStream} that delegates all work to the given <var>delegates</var>:
     * <ul>
     *   <li>
     *     The {@link OutputStream#write(byte[], int, int) write()} methods write the given data to all the delegates;
     *     if any of these throw an {@link IOException}, it is rethrown, and it is undefined whether all the data was
     *     written to all the delegates.
     *   </li>
     *   <li>
     *     {@link OutputStream#flush() flush()} flushes the delegates; throws the first {@link IOException} that any
     *     of the delegates throws.
     *   </li>
     *   <li>
     *     {@link OutputStream#close() close()} attempts to close <i>all</i> the <var>delegates</var>; if any of these
     *     throw {@link IOException}s, one of them is rethrown.
     *   </li>
     * </ul>
     */
    @NotNullByDefault(false) public static OutputStream
    tee(final OutputStream... delegates) {
        return new OutputStream() {

            @Override public void
            close() throws IOException {
                IOException caughtIOException = null;
                for (OutputStream delegate : delegates) {
                    try {
                        delegate.close();
                    } catch (IOException ioe) {
                        caughtIOException = ioe;
                    }
                }
                if (caughtIOException != null) throw caughtIOException;
            }

            @Override public void
            flush() throws IOException {
                for (OutputStream delegate : delegates) delegate.flush();
            }

            @Override public void
            write(byte[] b, int off, int len) throws IOException {
                // Overriding this method is not strictly necessary, because "OutputStream.write(byte[], int, int)"
                // calls "OutputStream.write(int)", but "delegate.write(byte[], int, int)" is probably more
                // efficient. However, the behavior is different when one of the delegates throws an exception
                // while being written to.
                for (OutputStream delegate : delegates) delegate.write(b, off, len);
            }

            @Override public void
            write(int b) throws IOException {
                for (OutputStream delegate : delegates) delegate.write(b);
            }
        };
    }

    /**
     * Invokes <var>writeContents</var>{@code .consume()} with an output stream subject that writes the data through to
     * the given <var>outputStream</var>.
     *
     * @return The number of bytes that were written through
     */
    public static long
    writeAndCount(
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents,
        OutputStream                                                     outputStream
    ) throws IOException {

        Produmer<Long, Long> count = ConsumerUtil.store();

        writeContents.consume(
            OutputStreams.tee(
                outputStream,
                OutputStreams.lengthWritten(ConsumerUtil.cumulate(count, 0L))
            )
        );

        Long result = count.produce();
        return result == null ? 0L : result;
    }

    /**
     * Creates and returns an {@link OutputStream} which writes at most <var>byteCountLimits</var>{@code .produce()}
     * bytes to <var>delegates</var>{@code .produce()} before closing it and writing the next
     * <var>byteCountLimits</var>{@code .produce()} bytes to <var>delegates</var>{@code .produce()}, and so on.
     *
     * @param delegates       Must produce a (non-{@code null}) series of {@link OutputStream}s
     * @param byteCountLimits Must produce a (non-{@code null}) series of {@link Long}s
     */
    public static OutputStream
    split(
        final ProducerWhichThrows<? extends OutputStream, ? extends IOException> delegates,
        final Producer<? extends Long>                                           byteCountLimits
    ) throws IOException {

        return new OutputStream() {

            /** Current delegate to write to. */
            private OutputStream delegate = AssertionUtil.notNull(delegates.produce(), "'delegates' produced <null>");

            /** Number of remaining bytes to be written. */
            private long delegateByteCount = AssertionUtil.notNull(
                byteCountLimits.produce(),
                "'byteCountLimits' produced <null>"
            );

            @Override public void
            write(int b) throws IOException { this.write(new byte[] { (byte) b }, 0, 1); }

            @Override public synchronized void
            write(@Nullable byte[] b, int off, int len) throws IOException {

                while (len > this.delegateByteCount) {
                    this.delegate.write(b, off, (int) this.delegateByteCount);
                    this.delegate.close();
                    off += this.delegateByteCount;
                    len -= this.delegateByteCount;

                    this.delegate = AssertionUtil.notNull(
                        delegates.produce(),
                        "'delegates' produced <null>"
                    );
                    this.delegateByteCount = AssertionUtil.notNull(
                        byteCountLimits.produce(),
                        "'byteCountLimits' produced <null>"
                    );
                }

                this.delegate.write(b, off, len);
                this.delegateByteCount -= len;
            }

            @Override public void
            flush() throws IOException { this.delegate.flush(); }

            @Override public void
            close() throws IOException { this.delegate.close(); }
        };
    }

    /**
     * @return An {@link OutputStream} which ignores all invocations of {@link OutputStream#close()}
     * @deprecated Use {@link #unclosable(OutputStream)} instead
     */
    @Deprecated
    public static OutputStream
    unclosableOutputStream(OutputStream delegate) {
        return OutputStreams.unclosable(delegate);
    }

    /**
     * @return An {@link OutputStream} which ignores all invocations of {@link OutputStream#close()}
     */
    public static OutputStream
    unclosable(OutputStream delegate) {

        return new FilterOutputStream(delegate) {

            @Override public void
            close() {}

            @Override public void
            write(@Nullable byte[] b, int off, int len) throws IOException { this.out.write(b, off, len); }
        };
    }

    /**
     * Writes <var>count</var> bytes of value <var>b</var> to the given <var>outputStream</var>.
     */
    public static void
    fill(OutputStream outputStream, byte b, long count) throws IOException {

        if (count > 8192) {
            byte[] ba = new byte[8192];
            if (b != 0) Arrays.fill(ba, b);
            do {
                outputStream.write(ba);
                count -= 8192;
            } while (count > 8192);
        }

        byte[] ba = new byte[(int) count];
        Arrays.fill(ba, b);
        outputStream.write(ba);
    }

    /**
     * @return An output stream which feeds the data to the <var>delegate</var> byte consumer
     */
    public static OutputStream
    byteConsumerOutputStream(final ConsumerWhichThrows<? super Byte, ? extends IOException> delegate) {

        return new OutputStream() {

            @Override public void
            write(int b) throws IOException { delegate.consume((byte) b); }
        };
    }

    /**
     * Creates and returns an array of <var>n</var> {@link OutputStream}s.
     * <p>
     *   Iff exactly the same bytes are written to all of these streams, and then all the streams are closed, then
     *   <var>whenIdentical</var> will be run (exactly once).
     * </p>
     * <p>
     *   Otherwise, when the first non-identical byte is written to one of the streams, or at the latest when that
     *   stream is closed, <var>whenNotIdentical</var> will be run (possibly more than once).
     * </p>
     */
    public static OutputStream[]
    compareOutput(final int n, final Runnable whenIdentical, final Runnable whenNotIdentical) {

        /**
         * Logs checksums of the first n1, n2, n3, ... bytes written.
         * <p>
         *   This class is used to compare the data written to multiple output streams without storing the entire data
         *   in memory.
         * </p>
         * <p>
         *   n1, n2, n3, ... is an exponentially growing series, starting with a very small value.
         * </p>
         */
        abstract
        class ChecksumOutputStream extends OutputStream {

            /** The checksum of the bytes written to this stream so far. */
            private final Checksum checksum  = new CRC32();

            /** The number of bytes written to this stream so far. */
            private long count;

            /**
             * {@code checksums[i]} is the checksum of the first {@code THRESHOLD[i]} that were written to this stream.
             * <p>
             *   After this stream was closed, {@code checksums[idx - 1]} is the checksum of <b>all</b> bytes that were
             *   written to this stream.
             * </p>
             */
            protected final long[] checksums = new long[OutputStreams.THRESHOLDS.length];

            /** The number of checksums in the {@link #checksums} array. */
            protected int idx;

            /**
             * Indicates that this stream is closed and that {@code checksums[idx - 1]} is the checksum of <b>all</b>
             * bytes that were written to this stream.
             */
            private boolean closed;

            @Override public void
            write(int b) throws IOException {
                if (this.closed) throw new IOException("Stream is closed");

                if (this.count == OutputStreams.THRESHOLDS[this.idx]) this.pushChecksum();
                this.checksum.update(b);
                this.count++;
            }

            @Override public void
            write(@Nullable byte[] b, int off, int len) throws IOException {
                assert b != null;
                if (this.closed) throw new IOException("Stream is closed");

                while (this.count + len > OutputStreams.THRESHOLDS[this.idx]) {
                    int part = (int) Math.min(Integer.MAX_VALUE, OutputStreams.THRESHOLDS[this.idx] - this.count);
                    this.checksum.update(b, off, part);
                    this.count = OutputStreams.THRESHOLDS[this.idx];
                    this.pushChecksum();
                    off += part;
                    len -= part;
                }

                this.checksum.update(b, off, len);
                this.count += len;
            }

            private void
            pushChecksum() {
                this.checksums[this.idx] = this.checksum.getValue();
                this.checksumWasPushed(this.idx);
                this.idx++;
            }

            /**
             * Is called when another checksum is entered in {@link #checksums}.
             *
             * @param idx The index in {@link #checksums} where the checksum was stored
             */
            abstract void checksumWasPushed(int idx);

            @Override public void
            close() {
                if (this.closed) return;
                this.pushChecksum();
                this.closed = true;
                this.wasClosed();
            }

            /**
             * Is called after this stream has been closed (for the first time).
             */
            abstract void wasClosed();
        }

        final ChecksumOutputStream[] result = new ChecksumOutputStream[n];
        for (int i = 0; i < n; i++) {
            result[i] = new ChecksumOutputStream() {

                @Override void
                checksumWasPushed(int idx) {
                    for (int i = 0; i < n; i++) {
                        if (result[i].idx == idx + 1 && result[i].checksums[idx] != this.checksums[idx]) {
                            whenNotIdentical.run();
                            return;
                        }
                    }
                }

                @Override void
                wasClosed() {
                    for (int i = 0; i < n; i++) {
                        if (!result[i].closed) return;
                        if (
                            result[i].idx != this.idx
                            || result[i].checksums[this.idx - 1] != this.checksums[this.idx - 1]
                        ) {
                            whenNotIdentical.run();
                            return;
                        }
                    }
                    whenIdentical.run();
                }
            };
        }

        return result;
    }

    private static final long[] THRESHOLDS;
    static {
        THRESHOLDS = new long[126];
        long x = 2;
        for (int i = 0; i < OutputStreams.THRESHOLDS.length; x <<= 1) {
            OutputStreams.THRESHOLDS[i++] = x;
            OutputStreams.THRESHOLDS[i++] = x + (x >> 1);
        }
    }
    /**
     * Creates and returns an {@link OutputStream} which ignores the data written to it and only honors the
     * <i>number of bytes written</i>:
     * <p>
     *   Every time data is written to the {@link OutputStream}, it invokes the {@link Consumer#consume(Object)
     *   consume()} method on the <var>delegate</var> with the number of bytes written (not the <i>cumulated</i> number
     *   of bytes written!).
     * </p>
     */
    public static OutputStream
    lengthWritten(final ConsumerWhichThrows<? super Integer, ? extends RuntimeException> delegate) {

        return new OutputStream() {

            @Override public void
            write(int b) { delegate.consume(1); }

            @Override public void
            write(@Nullable byte[] b, int off, int len) { delegate.consume(len); }
        };
    }

    /**
     * @param checksum E.g. "{@code new java.util.zip.CRC32()}"
     * @return An {@link OutputStream} that updates the <var>checksum</var> for all data written to it
     */
    public static OutputStream
    updatesChecksum(final Checksum checksum) {

        return new OutputStream() {
            @Override public void                          write(int b)                      { checksum.update(b); }
            @NotNullByDefault(false) @Override public void write(byte[] b, int off, int len) { checksum.update(b, off, len); }
        };
    }

    /**
     * @return Counts all file operations in the given <var>eventCounter</var>
     */
    public static OutputStream
    statisticsOutputStream(OutputStream delegate, final EventCounter eventCounter) {

        return new FilterOutputStream(delegate) {

            @Override public void
            write(int b) throws IOException {
                super.write(b);
                eventCounter.countEvent("write", 1);
            }

            @Override @NotNullByDefault(false) public void
            write(byte[] b) throws IOException {
                super.write(b);
                eventCounter.countEvent("write", b.length);
            }

            @Override @NotNullByDefault(false) public void
            write(byte[] b, int off, int len) throws IOException {
                super.write(b, off, len);
                eventCounter.countEvent("write", len);
            }

            @Override public void
            flush() throws IOException {
                super.flush();
                eventCounter.countEvent("flush");
            }

            @Override public void
            close() throws IOException {
                eventCounter.countEvent("close");
                super.close();
            }
        };
    }

    /**
     * @return An {@link OutputStream} that forwards all {@code write()} operations to <var>out</var>,
     *         and flushes <var>out</var> after each {@code write()} operation
     */
    public static OutputStream
    autoFlushing(OutputStream out) {

        return new FilterOutputStream(out) {

            @Override public void
            write(int b) throws IOException {
                this.out.write(b);
                this.out.flush();
            }

            @Override @NotNullByDefault(false) public void
            write(byte[] b, int off, int len) throws IOException {
                this.out.write(b, off, len);
                this.out.flush();
            }
        };
    }

    /**
     * Creates an {@link OutputStream} that operates exactly like {@code new FileOutputStream(file)}, except that
     * iff the file exists and is overwritten with exactly the same data and then closed, then the file is not
     * re-created and written to, but is left totally untouched, which is typically tremendously faster.
     */
    public static OutputStream
    newOverwritingFileOutputStream(final File file) throws IOException {

        if (!file.exists()) return new FileOutputStream(file);

        final InputStream is      = new MarkableFileInputStream(file);
        final File        newFile = new File(file.getParent(), file.getName() + ".new");

        return new OutputStream() {

            /**
             * After this has changed to non-null, "is", "readBuffer" and "pos" are no longer used.
             */
            @Nullable OutputStream os = null;

            byte[] readBuffer = new byte[4096];
            long   pos        = 0; // Position of first unread byte in this.is

            @Override public void
            write(int b) throws IOException { this.write(new byte[] { (byte) b}); }

            @Override @NotNullByDefault(false) public void
            write(byte[] b, int off, int len) throws IOException {

                if (this.os != null) {
                    this.os.write(b, off,  len);
                    return;
                }

                if (len > this.readBuffer.length) this.readBuffer = new byte[len];

                int off2 = 0; // Offset in this.readBuffer
                while (len > 0) {
                    int n = is.read(this.readBuffer, off2, len);
                    if (n == -1) {

                        // Input file is at EOF.
                        this.switchToFos();
                        @SuppressWarnings("null") @NotNull OutputStream os2 = this.os; os2.write(b, off, len);
                        return;
                    }

                    if (!OutputStreams.arrayEquals(this.readBuffer, off2, off2 + n, b, off, off + n)) {

                        // Contents of input file differs.
                        this.switchToFos();
                        @SuppressWarnings("null") @NotNull OutputStream os2 = this.os; os2.write(b, off, len);
                        return;
                    }

                    this.pos  += n;
                    off2 += n;
                    off += n;
                    len -= n;
                }
            }

            @Override public void
            flush() throws IOException {
                if (this.os != null) this.os.flush();
            }

            @Override public void
            close() throws IOException {

                if (this.os != null) {
                    is.close();
                    @SuppressWarnings("null") @NotNull OutputStream os2 = this.os; os2.close();
                    if (!file.delete()) throw new IOException("Could not delete original file \"" + file + "\"");
                    if (!newFile.renameTo(file)) throw new IOException("Could not rename new file \"" + newFile + "\" to original file \"" + file + "\"");
                    return;
                }

                int b = is.read();
                if (b == -1) {

                    // Input file has identical contents.
                    is.close();
                    return;
                }

                // Input file has trailing contents.
                this.switchToFos();
                @SuppressWarnings("null") @NotNull OutputStream os2 = this.os; os2.close();

                if (!file.delete()) throw new IOException("Could not delete original file \"" + file + "\"");
                if (!newFile.renameTo(file)) throw new IOException("Could not rename new file \"" + newFile + "\" to original file \"" + file + "\"");
            }

            private void
            switchToFos() throws IOException {
                OutputStream os2 = this.os = new FileOutputStream(newFile);
                is.reset();
                if (IoUtil.copy(is, os2, this.pos) != this.pos) throw new EOFException();
                is.close();
            }
        };
    }

    /**
     * Surrogate for {@code Arrays.equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int
     * bToIndex)}, which exists only in JRE 9+. Lives here because we don't want to create a dependency on
     * {commons-util}.
     */
    static boolean
    arrayEquals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        int aLen = aToIndex - aFromIndex;
        int bLen = bToIndex - bFromIndex;

        if (aLen < 0) new IllegalArgumentException("fromIndex(" + aFromIndex + ") > toIndex(" + aToIndex + ")");
        if (bLen < 0) new IllegalArgumentException("fromIndex(" + bFromIndex + ") > toIndex(" + bToIndex + ")");

        if (aLen != bLen) return false;

        for (; aLen > 0; aLen--) {
            if (a[aFromIndex++] != b[bFromIndex++]) return false;
        }
        return true;
    }
}
