
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

// CHECKSTYLE Javadoc:OFF

package test.pipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.pipe.Pipe;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.InputOutputStreams;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

public
class PipeTest {

    private static final long MY_SEED = 0xcafebabe12345678L;

    @Test public void
    byteArrayRingBuffer() throws Exception {

        int capacity = 100;
        PipeTest.testPipe(PipeFactory.byteArrayRingBuffer(capacity));
    }

    @Ignore // Takes too long to wait for it. SUPPRESS CHECKSTYLE WrapAndIndent
    @Test public void
    largeByteArrayRingBuffer() throws Exception {

        int capacity = 501 * 1000 * 1000;
        PipeTest.testLargePipe(PipeFactory.byteArrayRingBuffer(capacity), 2 * capacity);
    }

    @Test public void
    randomAccessFileRingBuffer() throws Exception {

        final File tempFile = File.createTempFile("mBBRB-", ".tmp", new File("."));

        long capacity = 100;
        PipeTest.testPipe(PipeFactory.randomAccessFileRingBuffer(tempFile, capacity, true));
    }

    @Ignore // Would take about 10 minutes! SUPPRESS CHECKSTYLE WrapAndIndent
    @Test public void
    largeRandomAccessFileRingBuffer() throws Exception {

        final File tempFile = File.createTempFile("mBBRB-", ".tmp", new File("."));

        long capacity = 5001L * 1000 * 1000;
        PipeTest.testLargePipe(
            PipeFactory.randomAccessFileRingBuffer(tempFile, capacity, true),
            2 * capacity
        );
    }

    @Test public void
    nonDirectByteBufferRingBuffer() throws Exception {

        int capacity = 100;
        PipeTest.testPipe(
            PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(capacity))
        );
    }

    @Test public void
    directByteBufferRingBuffer() throws Exception {

        int capacity = 100;
        PipeTest.testPipe(
            PipeFactory.byteBufferRingBuffer(ByteBuffer.allocateDirect(capacity))
        );
    }

    @Test public void
    mappedFileRingBuffer() throws Exception {

        int capacity = 100;
        PipeTest.testPipe(PipeFactory.mappedTempFileRingBuffer(capacity));
    }

    @Ignore // Takes too long to wait for it. SUPPRESS CHECKSTYLE WrapAndIndent
    @Test public void
    largeMappedFileRingBuffer() throws Exception {

        final File tempFile = File.createTempFile("mBBRB-", ".tmp", new File("."));

        int capacity = 2101 * 1000 * 1000;
        PipeTest.testLargePipe(
            PipeFactory.mappedFileRingBuffer(tempFile, capacity, false),
            2L * capacity
        );
    }

    @Test public void
    elasticPipe() throws Exception {

        PipeTest.testPipe(PipeFactory.elasticPipe(new ProducerWhichThrows<Pipe, IOException>() {

            @Override @Nullable public Pipe
            produce() { return PipeFactory.byteArrayRingBuffer(20000); }
        }));
    }

    // ------------------------------------------------------

    private static void
    testPipe(Pipe pipe) throws IOException {

        InputOutputStreams ioss = PipeUtil.asInputOutputStreams(pipe);

        InputStream  is  = ioss.getInputStream();
        OutputStream os  = ioss.getOutputStream();
        InputStream  ris = IoUtil.byteProducerInputStream(ProducerUtil.randomByteProducer(PipeTest.MY_SEED));
        OutputStream ros = AssertIo.assertEqualData(ProducerUtil.randomByteProducer(PipeTest.MY_SEED));

        final int n1 = 50;
        for (int i = 0; i < n1; i++) {
            IoUtil.copy(ris, os, 42);
            IoUtil.copy(is, ros, 41);
        }

        int n2 = 100000;
        for (int i = 0; i < n2; i++) {
            IoUtil.copy(ris, os, 37);
            IoUtil.copy(is, ros, 37);
        }

        os.close();
        Assert.assertEquals(n1, IoUtil.copy(is, ros));
        is.close();
    }

    private static void
    testLargePipe(Pipe pipe, long count) throws IOException {

        InputOutputStreams ioss = PipeUtil.asInputOutputStreams(pipe);

        class MyByteProducer implements Producer<Byte> {
            byte b;
            @Override public Byte produce() { return this.b++; }
        }
        InputStream  is  = ioss.getInputStream();
        OutputStream os  = ioss.getOutputStream();
        InputStream  ris = IoUtil.byteProducerInputStream(new MyByteProducer());
        OutputStream ros = AssertIo.assertEqualData(new MyByteProducer());

        IoUtil.copy(ris, os, 7);

        final int bs = 100000;
        for (long n = 0; n < count; n += bs) {
            IoUtil.copy(ris, os, Math.min(bs, count));
            IoUtil.copy(is, ros, Math.min(bs, count));
        }

        os.close();
        Assert.assertEquals(7, IoUtil.copy(is, ros));
        is.close();
    }
}
