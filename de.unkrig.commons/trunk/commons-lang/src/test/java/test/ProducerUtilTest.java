
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class ProducerUtilTest {

    class Naturals implements Producer<Integer> {
        private final AtomicInteger ai = new AtomicInteger();
        @Override public Integer produce() { return this.ai.incrementAndGet(); }
    }

    class Bistable implements Predicate<Object> {
        private boolean state;
        @Override public synchronized boolean evaluate(@Nullable Object subject) { return (this.state = !this.state); }
    }

    class Toggle implements Producer<Boolean> {
        private boolean state;
        @Override public synchronized Boolean produce() { return (this.state = !this.state); }
    }

    @Test public void
    testSparingProducer() {

        @SuppressWarnings("deprecation") Producer<? extends Integer>
        p = ProducerUtil.sparingProducer(new Naturals(), new Bistable(), "");

        Integer[] ia = new Integer[5];
        for (int i = 0; i < ia.length; i++) {
            ia[i] = p.produce();
        }
        Assert.assertArrayEquals(new Integer[] { 1, 1, 2, 2, 3 }, ia);
    }

    @Test public void
    testProducerUtilCache1() {
        ProducerWhichThrows<Integer, RuntimeException> p = ProducerUtil.cache(new Naturals(), new Toggle());
        for (int expected : new int[] { 1, 2, 2, 3, 3, 4, 4, 5, 5, }) {
            Assert.assertEquals((Integer) expected, p.produce());
        }
    }

    @Test public void
    testProducerUtilCache2() {
        ProducerWhichThrows<Integer, RuntimeException> p = ProducerUtil.cache(99, new Naturals(), new Toggle());
        for (int expected : new int[] { 99, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, }) {
            Assert.assertEquals((Integer) expected, p.produce());
        }
    }

    @Test public void
    testProducerUtilCache3() throws Exception {

        ProducerWhichThrows<Integer, RuntimeException>
        p = ProducerUtil.cache(new Naturals(), ProducerUtil.atMostEvery(200, false, true));

        Thread.sleep(100);

        assertProductEqualsFor(1, p, 200);

        Thread.sleep(100);
        Assert.assertEquals((Integer) 2, p.produce());
        Thread.sleep(100);
        assertProductEqualsFor(2, p, 100);
        Assert.assertEquals((Integer) 3, p.produce());
    }

    @SuppressWarnings("null")
    @Test public void
    testCacheAsynchronously() {
        
        ProducerWhichThrows<Future<Integer>, RuntimeException> fp = new ProducerWhichThrows<Future<Integer>, RuntimeException>() {
            
            @Override @Nullable public Future<Integer> produce() {

                ProducerUtilTest.this.nextValue++;
                
                return new Future<Integer>() {

                    @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; } 
                    @Override public boolean isCancelled()                         { return false; }
                    @Override public boolean isDone()                              { return true; }
                    @Override public Integer get(long timeout, TimeUnit unit)      { return this.get(); }

                    @Override public Integer get() { return (ProducerUtilTest.this.nextValue += 2) - 2; }
                };
            }
        };

        this.nextValue = 1;
        ProducerWhichThrows<Integer, RuntimeException> p = ProducerUtil.cacheAsynchronously(
            fp,
            ProducerUtil.alternate(true, false), // invalidationCondition
            false // prefetch
        );
        Assert.assertEquals(1, this.nextValue);
        Assert.assertEquals(2, (int) p.produce());
        Assert.assertEquals(4, this.nextValue);
        Assert.assertEquals(2, (int) p.produce());
        Assert.assertEquals(5, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(7, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(7, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(8, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(10, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(10, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(11, this.nextValue);
        Assert.assertEquals(11, (int) p.produce());
        Assert.assertEquals(13, this.nextValue);
        Assert.assertEquals(11, (int) p.produce());
        Assert.assertEquals(13, this.nextValue);
        Assert.assertEquals(11, (int) p.produce());
        Assert.assertEquals(14, this.nextValue);
        
        this.nextValue = 1;
        p = ProducerUtil.cacheAsynchronously(
            fp,
            ProducerUtil.alternate(true, false), // invalidationCondition
            true // prefetch
        );
        Assert.assertEquals(2, this.nextValue); // <= Only THIS value is different!
        Assert.assertEquals(2, (int) p.produce());
        Assert.assertEquals(4, this.nextValue);
        Assert.assertEquals(2, (int) p.produce());
        Assert.assertEquals(5, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(7, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(7, this.nextValue);
        Assert.assertEquals(5, (int) p.produce());
        Assert.assertEquals(8, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(10, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(10, this.nextValue);
        Assert.assertEquals(8, (int) p.produce());
        Assert.assertEquals(11, this.nextValue);
        Assert.assertEquals(11, (int) p.produce());
        Assert.assertEquals(13, this.nextValue);
        Assert.assertEquals(11, (int) p.produce());
        Assert.assertEquals(13, this.nextValue);
    }
    private int nextValue;
    
    @SuppressWarnings("null") @Test public void
    testProducerUtilAtMostEvery1() throws Exception {
        Producer<Boolean> p = ProducerUtil.atMostEvery(200, false, false);

        Thread.sleep(100);
        assertProductEqualsFor(false, p, 200);
        Assert.assertTrue(p.produce());

        Thread.sleep(100);
        assertProductEqualsFor(false, p, 200);
        Assert.assertTrue(p.produce());
    }

    @SuppressWarnings("null") @Test public void
    testProducerUtilAtMostEvery2() throws Exception {
        Producer<Boolean> p = ProducerUtil.atMostEvery(200, false, true);

        Thread.sleep(100);
        assertProductEqualsFor(false, p, 200);
        Assert.assertTrue(p.produce());
        Thread.sleep(100);
        assertProductEqualsFor(false, p, 100);
        Assert.assertTrue(p.produce());
    }

    @SuppressWarnings("null") @Test public void
    testProducerUtilAtMostEvery3() throws Exception {
        Producer<Boolean> p = ProducerUtil.atMostEvery(200, true, false);

        Assert.assertTrue(p.produce());

        Thread.sleep(100);
        assertProductEqualsFor(false, p, 200);
        Assert.assertTrue(p.produce());

        Thread.sleep(100);
        assertProductEqualsFor(false, p, 200);
        Assert.assertTrue(p.produce());
    }

    @SuppressWarnings("null") @Test public void
    testProducerUtilAtMostEvery4() throws Exception {
        Producer<Boolean> p = ProducerUtil.atMostEvery(200, true, true);

        Assert.assertTrue(p.produce());
        Thread.sleep(100);
        assertProductEqualsFor(false, p, 100);

        Assert.assertTrue(p.produce());
        Thread.sleep(100);
        assertProductEqualsFor(false, p, 100);
        Assert.assertTrue(p.produce());
    }

    /**
     * Checks frequently that <var>p</var> produces <var>expected</var> within the next <var>milliseconds</var>.
     */
    private static <T, EX extends Throwable> void
    assertProductEqualsFor(T expected, ProducerWhichThrows<T, EX> p, int milliseconds) throws Exception, EX {

        Assert.assertEquals(expected, p.produce());

        long begin = System.currentTimeMillis();
        for (;;) {
            Thread.sleep(10);
            long elapsed = System.currentTimeMillis() - begin;

            if (elapsed > milliseconds + 10) return;

            if (elapsed < milliseconds - 10) {
                Assert.assertEquals("After " + elapsed + "ms", expected, p.produce());
            }
        }
    }
}
