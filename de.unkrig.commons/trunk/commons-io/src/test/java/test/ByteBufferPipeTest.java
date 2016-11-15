
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.BlockingException;
import de.unkrig.commons.io.pipe.Pipe;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.InputOutputStreams;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class ByteBufferPipeTest {

    @Test public void
    test1() throws IOException {
        InputOutputStreams ios = PipeUtil.asInputOutputStreams(
            PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(5))
        );
        for (int i = 0; i < 13; i++) {
            byte[] ba = new byte[] { (byte) (3 * i), (byte) (3 * i + 1), (byte) (3 * i + 2) };
            ios.getOutputStream().write(ba);
            byte[] ba2 = new byte[3];
            Assert.assertEquals(3, ios.getInputStream().read(ba2));
            Assert.assertArrayEquals("i=" + i, ba, ba2);
        }
        ios.getOutputStream().close();
        Assert.assertEquals(-1, ios.getInputStream().read());
    }

    @Test public void
    testInputClosed() throws IOException {
        InputOutputStreams ios = PipeUtil.asInputOutputStreams(
            PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(5))
        );
        for (int i = 0; i < 13; i++) {
            ios.getOutputStream().write(i);
            Assert.assertEquals(i, ios.getInputStream().read());
        }
        ios.getInputStream().close();
        try {
            ios.getOutputStream().write(99);
            Assert.fail();
        } catch (IOException ioe) {}
    }

    /**
     * Fill, then empty an elastic pipe.
     */
    @Test public void
    testElasticPipe1() throws IOException {
        final int[] count        = { 0 };
        final int[] nextCapacity = { 3 };

        InputOutputStreams
        ios = PipeUtil.asInputOutputStreams(PipeFactory.elasticPipe(
            new ProducerWhichThrows<Pipe, IOException>() {

                @Override @Nullable public Pipe
                produce() {
                    int capacity = nextCapacity[0];
                    nextCapacity[0] *= 2;
                    count[0]++;
                    return PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(capacity));
                }
            }
        ));

        final OutputStream os = ios.getOutputStream();
        final InputStream  is = ios.getInputStream();

        Assert.assertEquals(0, count[0]);

        os.write(0); Assert.assertEquals(1, count[0]);
        os.write(1); Assert.assertEquals(1, count[0]);
        os.write(2); Assert.assertEquals(1, count[0]);
        os.write(3); Assert.assertEquals(2, count[0]);
        os.write(4); Assert.assertEquals(2, count[0]);
        os.write(5); Assert.assertEquals(2, count[0]);
        os.write(6); Assert.assertEquals(2, count[0]);
        os.write(7); Assert.assertEquals(2, count[0]);
        os.write(8); Assert.assertEquals(2, count[0]);
        os.write(9); Assert.assertEquals(3, count[0]);

        os.close();
        Assert.assertEquals(3, count[0]);

        Assert.assertEquals(0, is.read());
        Assert.assertEquals(1, is.read());
        Assert.assertEquals(2, is.read());
        Assert.assertEquals(3, is.read());
        Assert.assertEquals(4, is.read());
        Assert.assertEquals(5, is.read());
        Assert.assertEquals(6, is.read());
        Assert.assertEquals(7, is.read());
        Assert.assertEquals(8, is.read());
        Assert.assertEquals(9, is.read());
        Assert.assertEquals(-1, is.read());
        Assert.assertEquals(3, count[0]);
    }

    /**
     * Verify how an elastic pipe throws a {@link BlockingException} when if gets empty.
     */
    @Test public void
    testElasticPipe2() throws IOException {
        final int[] count        = { 0 };
        final int[] nextCapacity = { 3 };

        InputOutputStreams
        ios = PipeUtil.asInputOutputStreams(
            PipeFactory.elasticPipe(
                new ProducerWhichThrows<Pipe, IOException>() {

                    @Override @Nullable public Pipe
                    produce() {
                        int capacity = nextCapacity[0];
                        nextCapacity[0] *= 2;
                        count[0]++;
                        return PipeFactory.byteBufferRingBuffer(ByteBuffer.allocate(capacity));
                    }
                }
            ),
            false // blocking
        );

        final OutputStream os = ios.getOutputStream();
        final InputStream  is = ios.getInputStream();

        os.write(0); Assert.assertEquals(1, count[0]);
        os.write(1); Assert.assertEquals(1, count[0]);
        os.write(2); Assert.assertEquals(1, count[0]);
        os.write(3); Assert.assertEquals(2, count[0]);
        os.write(4); Assert.assertEquals(2, count[0]);
        os.write(5); Assert.assertEquals(2, count[0]);
        os.write(6); Assert.assertEquals(2, count[0]);
        os.write(7); Assert.assertEquals(2, count[0]);
        os.write(8); Assert.assertEquals(2, count[0]);
        os.write(9); Assert.assertEquals(3, count[0]);

        Assert.assertEquals(0, is.read());
        Assert.assertEquals(1, is.read());
        Assert.assertEquals(2, is.read());
        Assert.assertEquals(3, is.read());
        Assert.assertEquals(4, is.read());
        Assert.assertEquals(5, is.read());
        Assert.assertEquals(6, is.read());
        Assert.assertEquals(7, is.read());
        Assert.assertEquals(8, is.read());
        Assert.assertEquals(9, is.read());

        Assert.assertEquals(3, count[0]);

        try {
            Assert.assertEquals(-1, is.read());
            Assert.fail("BlockingException expected");
        } catch (BlockingException be) {}
        Assert.assertEquals(3, count[0]);

        os.write(10);
        Assert.assertEquals(4, count[0]);

        Assert.assertEquals(10, is.read());

        try {
            Assert.assertEquals(-1, is.read());
            Assert.fail("BlockingException expected");
        } catch (BlockingException be) {}

        Assert.assertEquals(4, count[0]);

        for (int i = 0; i < 48; i++) {
            os.write(11); Assert.assertEquals(5, count[0]);
        }

        os.write(12); Assert.assertEquals(6, count[0]);
    }
}
