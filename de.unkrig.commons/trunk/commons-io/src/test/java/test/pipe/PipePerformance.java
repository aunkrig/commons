
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

// CHECKSTYLE Javadoc:OFF

package test.pipe;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.pipe.Pipe;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.InputOutputStreams;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

public
class PipePerformance {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    public static final long MY_SEED = 0xcafebabe12345678L;

    @Test public void
    byteArrayRingBuffer() throws Exception {

        final int capacity = 20000;
        System.err.println("byteArrayRingBuffer(" + capacity + ")");
        PipePerformance.testPipe2(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() { return PipeFactory.byteArrayRingBuffer(capacity); }
        });
    }

    @Test public void
    randomAccessFileRingBuffer() throws Exception {

        final long capacity = 1000000;
        System.err.println("randomAccessBufferRingBuffer(" + capacity + ")");

        PipePerformance.testPipe(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() throws IOException {

                return PipeFactory.randomAccessTempFileRingBuffer(capacity);
            }
        });
    }

    @Test public void
    nonDirectByteBufferRingBuffer() throws Exception {

        final int capacity = 20000;
        System.err.println("nonDirectByteBufferRingBuffer(" + capacity + ")");

        PipePerformance.testPipe2(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() { return PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(capacity)); }
        });
    }

    @Test public void
    directByteBufferRingBuffer() throws Exception {

        final int capacity = 20000;
        System.err.println("directByteBufferRingBuffer(" + capacity + ")");

        PipePerformance.testPipe2(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() { return PipeFactory.byteBufferRingBuffer(ByteBuffer.allocateDirect(capacity)); }
        });
    }

    @Test public void
    mappedFileRingBuffer() throws Exception {

        final int capacity = 1000000;
        System.err.println("mappedByteBufferRingBuffer(" + capacity + ")");

        PipePerformance.testPipe(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() throws IOException {
                return PipeFactory.mappedTempFileRingBuffer(capacity);
            }
        });
    }

    @Test public void
    elasticPipe() throws Exception {

        final int capacity = 20000;
        System.err.println("elasticPipe(" + capacity + ")");

        PipePerformance.testPipe2(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() {
                return PipeFactory.elasticPipe(new ProducerWhichThrows<Pipe, IOException>() {

                    @Override @Nullable public Pipe
                    produce() { return PipeFactory.byteArrayRingBuffer(capacity); }
                });
            }
        });
    }

    // ------------------------------------------------------

    private static void
    testPipe2(final ProducerWhichThrows<? extends Pipe, ? extends IOException> subject) throws IOException {

        PipePerformance.testPipe(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() throws IOException {
                Pipe pipe = subject.produce();
                assert pipe != null;
                return pipe;
            }
        });
    }

    private static void
    testPipe(final ProducerWhichThrows<? extends Pipe, ? extends IOException> subject) throws IOException {

        PipePerformance.repeat(4, new ProducerWhichThrows<Long, IOException>() {

            @Override public Long
            produce() throws IOException {

                final InputStream  is;
                final OutputStream os;
                {
                    Pipe pipe = subject.produce();
                    assert pipe != null;

                    InputOutputStreams ioss = PipeUtil.asInputOutputStreams(pipe);
                    is = ioss.getInputStream();
                    os = new BufferedOutputStream(ioss.getOutputStream(), 8192);
                }

                long n = 0;
                for (int i = 0; i < 1000/*00*/; i++) {
                    try {
                        IoUtil.fill(os, (byte) 0, 10000);
                        os.flush();
                        is.skip(10000);

                        n += 10000;
                    } catch (AssertionError ae) {
                        throw ExceptionUtil.wrap("i=" + i, ae);
                    }
                }

                os.close();
                Assert.assertEquals(0, is.skip(1));
                is.close();

                return n;
            }
        });
    }

    private static <EX extends Exception> void
    repeat(int repeatCount, ProducerWhichThrows<? extends Long, ? extends EX> runnable) throws EX {

        long totalDuration = 0;
        long totalCount    = 0;

        for (int i = 0; i < repeatCount; i++) {
            long begin = System.currentTimeMillis();
            long count;
            try {
                Long tmp = runnable.produce();
                assert tmp != null;
                count = tmp;
            } catch (AssertionError ae) {
                throw ExceptionUtil.wrap("i=" + i, ae);
            }

            long end      = System.currentTimeMillis();
            long duration = end - begin;
            System.err.print(" ");
            PipePerformance.printRate(System.err, duration, count);

            totalDuration += duration;
            totalCount    += count;
        }
        System.err.print(" (avg ");
        PipePerformance.printRate(System.err, totalDuration, totalCount);
        System.err.println(")");
    }

    private static void
    printRate(PrintStream ps, long durationMs, long count) {
        if (durationMs == 0) {
            ps.print("inf");
        } else {
            ps.printf(Locale.US, "%,d/sec", 1000 * count / durationMs);
        }
    }

    public static void
    main(String[] args) throws Exception {
        new PipePerformance().byteArrayRingBuffer();
    }
}
