
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.LinkedList;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * The methods of this class create the various {@link Pipe} implementations:
 * <dl>
 *   <dt>{@link PipeFactory#byteArrayRingBuffer(int)}</dt>
 *   <dd>A pipe which is backed by an (internal) byte array</dd>
 *   <dt>{@link PipeFactory#byteBufferRingBuffer(ByteBuffer)}</dt>
 *   <dd>A pipe which is backed by a {@link java.nio.ByteBuffer}</dd>
 *   <dt>{@link PipeFactory#elasticPipe()}</dt>
 *   <dd>
 *     A Pipe that implements infinite capacity and good performance by first allocating a small in-memory ring buffer,
 *     then, if that fills up, a larger one that uses a memory-mapped file, and eventually one based on a random access
 *     file with practically unlimited size
 *   </dd>
 *   <dt>{@link PipeFactory#elasticPipe(de.unkrig.commons.lang.protocol.ProducerWhichThrows)}</dt>
 *   <dd>
 *     A Pipe that implements infinite capacity by allocating delegate pipes as needed (and closing them when they are
 *     no longer needed)
 *   </dd>
 *   <dt>{@link PipeFactory#mappedFileRingBuffer(java.io.File, int, boolean)}</dt>
 *   <dd>
 *     A pipe which is backed by a {@link FileChannel#map(MapMode, long, long) memory-mapped file}, which will be
 *     unmapped and (optionally) deleted when the pipe is closed
 *   <dt>{@link PipeFactory#mappedTempFileRingBuffer(int)}</dt>
 *   <dd>
 *     A pipe which is backed by a {@link FileChannel#map(MapMode, long, long) memory-mapped} temporary file, which
 *     will be unmapped and deleted when the pipe is closed</dd>
 *   <dt>{@link PipeFactory#randomAccessFileRingBuffer(java.io.File, long, boolean)}</dt>
 *   <dd>
 *     A pipe which is backed by a {@link RandomAccessFile}, which will (optionally) be deleted when the pipe is
 *     closed
 *   </dd>
 *   <dt>{@link PipeFactory#randomAccessTempFileRingBuffer(long)}</dt>
 *   <dd>A pipe which is backed by a temporary {@link RandomAccessFile}, which is deleted when the pipe is closed</dd>
 * </dl>
 * The characteristics of these implementations are as follows:
 * <table border="1">
 *   <tr>
 *     <th>&nbsp;</th>
 *     <th>Performance</th>
 *     <th>Resource usage</th>
 *     <th>Size limits</th>
 *   </tr>
 *   <tr>
 *     <td>{@code byteArrayRingBuffer(int)}</td>
 *     <td>Fast</td>
 *     <td>Heap memory</td>
 *     <td>2 GB</td>
 *   </tr>
 *   <tr>
 *     <td>{@code byteBufferRingBuffer(ByteBuffer.allocate(int))}</td>
 *     <td>Fast</td>
 *     <td>Heap memory</td>
 *     <td>2 GB</td>
 *   </tr>
 *   <tr>
 *     <td>{@code byteBufferRingBuffer(ByteBuffer.allocateDirect(int))}</td>
 *     <td>Fast</td>
 *     <td>Off-heap memory</td>
 *     <td>2 GB</td>
 *   </tr>
 *   <tr>
 *     <td>{@code mappedFileRingBuffer(File, int, boolean)}</td>
 *     <td>Medium</td>
 *     <td>Low</td>
 *     <td>2 GB, Address space, disk space</td>
 *   </tr>
 *   <tr>
 *     <td>{@code randomAccessFileRingBuffer(File, long, boolean)}</td>
 *     <td>Low</td>
 *     <td>Low</td>
 *     <td>Disk space</td>
 *   </tr>
 * </table>
 */
public final
class PipeFactory {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private PipeFactory() {}

    /**
     * @return A pipe which is backed by an (internal) byte array of size <var>capacity</var>
     */
    public static Pipe
    byteArrayRingBuffer(final int capacity) {

        if (capacity < 1) throw new IllegalArgumentException();

        return new AbstractRingBuffer(capacity) {

            final byte[] ba = new byte[capacity];

            @Override public void
            get(long pos, byte[] buf, int off, int len) {
                System.arraycopy(this.ba, (int) pos, buf, off, len);
            }

            @Override public void
            put(byte[] buf, int off, int len, long pos) {
                System.arraycopy(buf, off, this.ba, (int) pos, len);
            }

            @Override public void
            close() {}
        };
    }

    /**
     * @return A pipe which is backed by a temporary {@link RandomAccessFile}, which is deleted when the pipe is
     *         closed
     */
    public static Pipe
    randomAccessTempFileRingBuffer(long capacity) throws IOException {
        return PipeFactory.randomAccessFileRingBuffer(File.createTempFile("rATFRB-", ".tmp"), capacity, true);
    }

    /**
     * @return                  A pipe which is backed by the <var>file</var>
     * @param file              Will be created if it does not exist; any existing contents will progressively be
     *                          overwritten by {@link Pipe#write(byte[], int, int)}
     * @param deleteFileOnClose Whether to delete the <var>file</var> when the pipe is closed
     * @throws RuntimeException <var>capacity</var> is less than one
     */
    public static Pipe
    randomAccessFileRingBuffer(final File file, long capacity, boolean deleteFileOnClose)
    throws IOException {

        if (capacity < 1) throw new IllegalArgumentException();

        if (deleteFileOnClose) file.deleteOnExit();

        final RandomAccessFile raf = new RandomAccessFile(file, "rw");

        Pipe result = new AbstractRingBuffer(capacity) {

            @Override public void
            get(long pos, byte[] buf, int off, int len) throws IOException {
                raf.seek(pos);
                raf.read(buf, off, len);
            }

            @Override public void
            put(byte[] buf, int off, int len, long pos) throws IOException {
                raf.seek(pos);
                raf.write(buf, off, len);
            }

            @Override public synchronized void
            close() throws IOException { raf.close(); }
        };

        return deleteFileOnClose ? PipeUtil.deleteFileOnClose(result, file) : result;
    }

    /**
     * @return A pipe which is backed by the <var>delegate</var> {@link ByteBuffer} and has the same size
     * @see    ByteBuffer#allocate(int)
     * @see    ByteBuffer#allocateDirect(int)
     */
    public static Pipe
    byteBufferRingBuffer(final ByteBuffer delegate) {

        return new AbstractRingBuffer(delegate.capacity()) {

            @Nullable ByteBuffer delegate2 = delegate;

            @Override public void
            get(long pos, byte[] buf, int off, int len) {
                assert pos <= Integer.MAX_VALUE;

                ByteBuffer d = this.delegate2;
                if (d == null) throw new IllegalStateException("Pipe closed");

                d.position((int) pos);
                d.get(buf, off, len);
            }

            @Override public void
            put(byte[] buf, int off, int len, long pos) {
                assert pos <= Integer.MAX_VALUE;

                ByteBuffer d = this.delegate2;
                if (d == null) throw new IllegalStateException("Pipe closed");

                d.position((int) pos);
                d.put(buf, off, len);
            }

            @Override public void
            close() { this.delegate2 = null; }
        };
    }

    /**
     * @return A pipe which is backed by a {@link FileChannel#map(MapMode, long, long) memory-mapped} temporary file,
     *         which will be unmapped and deleted when the pipe is closed
     */
    public static Pipe
    mappedTempFileRingBuffer(int capacity) throws IOException {
        return PipeFactory.mappedFileRingBuffer(File.createTempFile("mTFRB-", ".tmp"), capacity, true);
    }

    /**
     * @param capacity          The number of bytes in the <var>file</var> to use for buffering
     * @param deleteFileOnClose Whether the <var>file</var> should be deleted when the pipe is closed
     * @return                  A pipe which is backed by the {@link FileChannel#map(MapMode, long, long)
     *                          memory-mapped} <var>file</var>, which will be unmapped and (optionally) deleted when
     *                          the pipe is closed
     */
    public static Pipe
    mappedFileRingBuffer(final File file, int capacity, boolean deleteFileOnClose)
    throws IOException {

        if (deleteFileOnClose) file.deleteOnExit();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        Pipe pipe = PipeFactory.mappedChannelRingBuffer(raf.getChannel(), capacity);

        raf.close();

        return deleteFileOnClose ? PipeUtil.deleteFileOnClose(pipe, file) : pipe;
    }

    /**
     * @param capacity The number of bytes in the <var>fileChannel</var> to use for buffering
     * @return         A pipe that is backed by the <var>fileChannel</var>
     */
    private static Pipe
    mappedChannelRingBuffer(FileChannel fileChannel, int capacity) throws IOException {

        final MappedByteBuffer mbb = fileChannel.map(MapMode.READ_WRITE, 0, capacity);

        // Make sure that the mapped byte buffer is unmapped when the pipe is closed! Otherwise the backing file
        // cannot be deleted.
        return PipeFactory.mappedByteBufferRingBuffer(mbb, true);
    }

    /**
     * @param unmapOnClose Whether the <var>mappedByteBuffer</var> should automatically be closed when the pipe is
     *                     closed
     * @return             A pipe that is backed by the <var>mappedByteBuffer</var> and has the same capacity
     */
    private static Pipe
    mappedByteBufferRingBuffer(final MappedByteBuffer mappedByteBuffer, boolean unmapOnClose) {

        Pipe pipe = PipeFactory.byteBufferRingBuffer(mappedByteBuffer);

        return unmapOnClose ? PipeUtil.onClose(
            pipe,
            new RunnableWhichThrows<IOException>() {
                @Override public void run() { PipeFactory.unmap(mappedByteBuffer); }
            }
        ) : pipe;
    }

    /**
     * Unmaps a {@link MappedByteBuffer} that originated from {@link FileChannel#map(MapMode, long, long)}. Substitutes
     * the {@code MappedByteBuffer.unmap()} that is missing painfully from the JRE.
     */
    private static void
    unmap(final MappedByteBuffer mappedByteBuffer) {

        // Only ORACLE knows why the "sun.nio.ch.FileChannelImpl.unmap()" method is PRIVATE - there is no other way to
        // unmap a MappedByteBuffer!
        try {
            PipeFactory.UNMAP_METHOD.invoke(null, mappedByteBuffer);
        } catch (Exception e) {
            throw ExceptionUtil.wrap("Unmapping file channel", e, RuntimeException.class);
        }
    }
    private static final Method UNMAP_METHOD;
    static {
        try {

            Class<?> fileChannelImplClass = Class.forName("sun.nio.ch.FileChannelImpl");

            UNMAP_METHOD = fileChannelImplClass.getDeclaredMethod("unmap", java.nio.MappedByteBuffer.class);
            PipeFactory.UNMAP_METHOD.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * @return A {@link Pipe} that implements infinite capacity and good performance by first allocating a small
     *         in-memory ring buffer, then, if that fills up, a larger one that uses a memory-mapped temporary file,
     *         and eventually one that is backed by a random access file with practically unlimited size
     */
    public static Pipe
    elasticPipe() {

        return PipeFactory.elasticPipe(
            ProducerUtil.fromIndexTransformer(new TransformerWhichThrows<Integer, Pipe, IOException>() {

                @Override public Pipe
                transform(Integer index) throws IOException {
                    switch (index) {

                    case 0:
                        return PipeFactory.byteBufferRingBuffer(ByteBuffer.allocateDirect(300000));

                    case 1:
                        return PipeFactory.mappedTempFileRingBuffer(10000000);

                    case 3:
                        return PipeFactory.randomAccessTempFileRingBuffer(Long.MAX_VALUE);

                    default:
                        throw new IllegalStateException("elasticPipe");
                    }
                }
            })
        );
    }

    /**
     * @param pipes Is invoked when another pipe is needed; the produced pipes are closed when they are no longer
     *              needed, or when the returned pipe is closed
     * @return      A {@link Pipe} that implements infinite capacity by allocating delegate pipes as needed (and
     *              closing them when they are no longer needed)
     */
    public static Pipe
    elasticPipe(final ProducerWhichThrows<? extends Pipe, ? extends IOException> pipes) {

        return new AbstractPipe() {

            final LinkedList<Pipe> curr = new LinkedList<Pipe>();

            @Override public int
            read(byte[] buf, int off, int len) throws IOException {

                if (len == 0) return 0;

                synchronized (this) {
                    for (;;) {
                        if (this.curr.isEmpty()) return 0;
                        int n = this.curr.getFirst().read(buf, off, len);
                        if (n > 0) return n;
                        this.curr.removeFirst().close();
                    }
                }
            }

            @Override public int
            write(byte[] buf, int off, int len) throws IOException {

                if (len == 0) return 0;

                synchronized (this) {
                    for (;;) {
                        if (this.curr.isEmpty()) this.curr.add(pipes.produce());
                        int n = this.curr.getLast().write(buf, off, len);
                        if (n > 0) return n;
                        this.curr.add(pipes.produce());
                    }
                }
            }

            @Override public void
            close() throws IOException {

                synchronized (this) {

                    Iterator<Pipe> it = this.curr.iterator();
                    try {
                        while (it.hasNext()) it.next().close();
                    } finally {
                        while (it.hasNext()) {
                            try { it.next().close(); } catch (Exception ignored) {}
                        }
                    }
                }
            }

            @Override public boolean
            isFull() { return false; }

            @Override public boolean
            isEmpty() {
                if (this.curr.isEmpty()) return true;
                if (!this.curr.getFirst().isEmpty()) return false;
                this.curr.removeFirst();
                return this.curr.isEmpty();
            }
        };
    }
}
