
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.util.concurrent.ObjectSequentializer;

/**
 * Test case for the {@link ObjectSequentializer}.
 */
public
class ObjectSequentializerTest {

    /**
     * Test a simple merge of three streams with very little data.
     */
    @Test public void
    test() throws CancellationException, InterruptedException, ExecutionException {

        List<Integer> buffer = new ArrayList<Integer>();

        ObjectSequentializer<Integer, RuntimeException> s = new ObjectSequentializer<Integer, RuntimeException>(
            ConsumerUtil.addToCollection(buffer),
            new ScheduledThreadPoolExecutor(3, ThreadUtil.DAEMON_THREAD_FACTORY)
        );

        s.submit(new Consumer<ConsumerWhichThrows<? super Integer, ? extends RuntimeException>>() {

            @Override public void
            consume(ConsumerWhichThrows<? super Integer, ? extends RuntimeException> c) {

                ObjectSequentializerTest.sleep(100);
                c.consume(1);
                ObjectSequentializerTest.sleep(100);
                c.consume(2);
                ObjectSequentializerTest.sleep(100);
                c.consume(3);
            }
        });

        s.submit(new Consumer<ConsumerWhichThrows<? super Integer, ? extends RuntimeException>>() {

            @Override public void
            consume(ConsumerWhichThrows<? super Integer, ? extends RuntimeException> c) {
                ObjectSequentializerTest.sleep(70);
                c.consume(4);
                ObjectSequentializerTest.sleep(70);
                c.consume(5);
                ObjectSequentializerTest.sleep(70);
                c.consume(6);
            }
        });

        s.submit(new Consumer<ConsumerWhichThrows<? super Integer, ? extends RuntimeException>>() {

            @Override public void
            consume(ConsumerWhichThrows<? super Integer, ? extends RuntimeException> c) {
                ObjectSequentializerTest.sleep(140);
                c.consume(7);
                ObjectSequentializerTest.sleep(140);
                c.consume(8);
                ObjectSequentializerTest.sleep(140);
                c.consume(9);
            }
        });

        s.awaitCompletion();

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), buffer);
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
