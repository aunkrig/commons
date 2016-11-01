
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package test.concurrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.util.concurrent.ByteStreamSequentializer;

/**
 * Test case for the {@link ByteStreamSequentializer}.
 */
public
class ByteStreamSequentializerTest {

    /**
     * Test a simple merge of three streams with very little data.
     */
    @Test public void
    test() throws CancellationException, InterruptedException, ExecutionException, IOException {

        ByteArrayOutputStream target = new ByteArrayOutputStream();

        ByteStreamSequentializer s = new ByteStreamSequentializer(
            target,
            new ScheduledThreadPoolExecutor(100, ThreadUtil.DAEMON_THREAD_FACTORY)
        );

        s.submit(new ConsumerWhichThrows<OutputStream, IOException>() {

            @Override public void
            consume(OutputStream c) throws IOException {
                ByteStreamSequentializerTest.sleep(100);
                c.write(1);
                ByteStreamSequentializerTest.sleep(100);
                c.write(2);
                ByteStreamSequentializerTest.sleep(100);
                c.write(3);
            }
        });

        s.submit(new ConsumerWhichThrows<OutputStream, IOException>() {

            @Override public void
            consume(OutputStream c) throws IOException {
                ByteStreamSequentializerTest.sleep(70);
                c.write(4);
                ByteStreamSequentializerTest.sleep(70);
                c.write(5);
                ByteStreamSequentializerTest.sleep(70);
                c.write(6);
            }
        });

        s.submit(new ConsumerWhichThrows<OutputStream, IOException>() {

            @Override public void
            consume(OutputStream c) throws IOException {
                ByteStreamSequentializerTest.sleep(140);
                c.write(7);
                ByteStreamSequentializerTest.sleep(140);
                c.write(8);
                ByteStreamSequentializerTest.sleep(140);
                c.write(9);
            }
        });

        s.awaitCompletion();

        Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, target.toByteArray());
    }

    private static void
    sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Assert.fail(e.toString());
        }
    }
}
