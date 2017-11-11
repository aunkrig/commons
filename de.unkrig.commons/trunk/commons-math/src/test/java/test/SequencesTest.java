
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.math.Sequences;

public
class SequencesTest {

    @Test public void
    testRandomSequence() {

        long seed = 13487678623842343L; // SUPPRESS CHECKSTYLE UsageDistance

        for (int period = 1; period < 30000; period += 1 + period / 20) {

            long[] r = new long[period];
            for (int i = 0; i < period; i++) r[i] = -1L;

            int[] had = new int[period];
            for (int i = 0; i < period; i++) had[i] = -1;

            Producer<Long> rs = Sequences.randomSequence((seed += 23L), period);

            for (int i = 0; i < period; i++) {
                @SuppressWarnings("null") long x      = rs.produce();
                String                         prefix = "Period " + period + ", index " + i + ": ";
                assertTrue(prefix + x + " < 0", x >= 0L);
                assertTrue(prefix + x + " >= " + period, x < period);
                assertTrue(prefix + "Had " + x + " before, at index " + had[(int) x], had[(int) x] == -1);
                had[(int) x] = i;
                r[i]         = x;
            }
            for (int i = 0; i < period; i++) {
                @SuppressWarnings("null") long x      = rs.produce();
                String                         prefix = "Period " + period + ", index " + i + ": ";
                assertTrue(prefix + x + " < 0", x >= 0L);
                assertTrue(prefix + x + " >= " + period, x < period);
                assertEquals(prefix + r[i] + " != " + x, r[i], x);
            }
        }
    }
}
