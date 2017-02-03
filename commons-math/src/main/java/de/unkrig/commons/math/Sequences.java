
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

package de.unkrig.commons.math;

import java.util.Arrays;

import de.unkrig.commons.lang.protocol.Producer;

/**
 * A collection of utility methods related to mathematical sequences.
 */
public final
class Sequences {

    private Sequences() {}

    /**
     * Produces a periodic sequence of random, but unique {@link Long}s with values ranging from 0 to {@code period -
     * 1}.
     */
    public static Producer<Long>
    randomSequence(long period) {
        return randomSequence(System.nanoTime(), period);
    }

    /**
     * Produces a periodic sequence of random, but unique {@link Long}s with values ranging from 0 to {@code period -
     * 1}. For identical <var>seed</var>s, the returned sequences will be identical.
     */
    public static Producer<Long>
    randomSequence(final long seed, final long period) {
        if (period < 0L) throw new IllegalArgumentException();

        int idx = Arrays.binarySearch(M, period + 1);

        if (idx < 0) idx = -idx - 1;
        if (idx == M.length) idx--;  // Period is in the range 2^63 - 25 ... 2^63 - 1

        final long           m   = M[idx];
        long                 a   = A[idx];
        final Producer<Long> lcg = multiplicativeCongruentialGenerator(seed, m, a);

        return new Producer<Long>() {

            @Override public Long
            produce() {
                for (;;) {
                    @SuppressWarnings("null") long result = lcg.produce() - 1;
                    if (result < period) return result;
                }
            }
        };
    }
    private static final long[] M = {
        // CHECKSTYLE WrapAndIndent:OFF
                                                                            (1L <<  3) -   1L,
           (1L <<  4) -  3L,    (1L <<  5) -   1L,     (1L <<  6) -  3L,    (1L <<  7) -   1L,
           (1L <<  8) -  5L,    (1L <<  9) -   3L,     (1L << 10) -  3L,    (1L << 11) -   9L,
           (1L << 12) -  3L,    (1L << 13) -   1L,     (1L << 14) -  3L,    (1L << 15) -  19L,
           (1L << 16) - 15L,    (1L << 17) -   1L,     (1L << 18) -  5L,    (1L << 19) -   1L,
           (1L << 20) -  3L,    (1L << 21) -   9L,     (1L << 22) -  3L,    (1L << 23) -  15L,
           (1L << 24) -  3L,    (1L << 25) -  39L,     (1L << 26) -  5L,    (1L << 27) -  39L,
           (1L << 28) - 57L,    (1L << 29) -   3L,     (1L << 30) - 35L,    (1L << 31) -   1L,
           (1L << 32) -  5L,    (1L << 33) -   9L,     (1L << 34) - 41L,    (1L << 35) -  31L,
           (1L << 36) -  5L,    (1L << 37) -  25L,     (1L << 38) - 45L,    (1L << 39) -   7L,
           (1L << 40) - 87L,    (1L << 41) -  21L,     (1L << 42) - 11L,    (1L << 43) -  57L,
           (1L << 44) - 17L,    (1L << 45) -  55L,     (1L << 46) - 21L,    (1L << 47) - 115L,
           (1L << 48) - 59L,    (1L << 48) -  59L,     (1L << 50) - 27L,    (1L << 51) - 129L,
           (1L << 52) - 47L,    (1L << 53) - 111L,     (1L << 54) - 33L,    (1L << 55) -  55L,
           (1L << 56) -  5L,    (1L << 57) -  13L,     (1L << 58) - 27L,    (1L << 59) -  55L,
           (1L << 60) - 93L,    (1L << 61) -   1L,     (1L << 62) - 57L,    (1L << 63) -  25L,
           Long.MAX_VALUE,
       // CHECKSTYLE WrapAndIndent:ON
    };
    private static final long[] A = {
        // CHECKSTYLE WrapAndIndent:OFF
                                                                                           5L,
                         7L,                  11L,                  31L,                  23L,
                        33L,                  35L,                  65L,                 995L,
                       209L,                 884L,                 572L,                 219L,
                     17364L,               43165L,               92717L,              283741L,
                    380985L,              360889L,              914334L,              653276L,
                   6423135L,            25907312L,            26590841L,            45576512L,
                 246049789L,           520332806L,           771645345L,          1583458089L,
                1588635695L,          7425194315L,          5295517759L,          3124199165L,
               49865143810L,         76886758244L,         17838542566L,         61992693052L,
             1038914804222L,        140245111714L,       2214813540776L,       4928052325348L,
             6307617245999L,      25933916233908L,      63975993200055L,      72624924005429L,
            49235258628958L,     265609885904224L,    1087141320185010L,     349044191547257L,
          4359287924442956L,    2082839274626558L,    9131148267933071L,   33266544676670489L,
          4595551687825993L,   75953708294752990L,  101565695086122187L,  346764851511064641L,
        561860773102413563L, 1351750484049952003L, 2774243619903564593L, 4645906587823291368L,
        // CHECKSTYLE WrapAndIndent:ON
    };

    /**
     * Produces a periodic sequence of random, but unique {@link Long}s with values ranging from <var>min</var> to
     * <var>max</var>{@code - 1}. The period of the sequence is {@code max - min}. For identical <var>seed</var>s, the
     * returned sequences will be identical.
     */
    public static Producer<Long>
    randomSequence(long seed, final long min, long max) {

        final Producer<Long> delegate = randomSequence(seed, max - min);

        return new Producer<Long>() {

            @SuppressWarnings("null") @Override public Long
            produce() {
                return delegate.produce() + min;
            }
        };
    }

    /**
     * See <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">Wikipedia article: Linear congruential
     * generator</a>.
     */
    public static Producer<Long>
    linearCongruentialGenerator(final long seed, final long m, final long a, final long c) {

        return new Producer<Long>() {

            long x = seed;

            @Override public Long
            produce() {
                this.x = (a * this.x + c) % m;
                return this.x;
            }
        };
    }

    /**
     * See <a href="http://en.wikipedia.org/wiki/Linear_congruential_generator">Wikipedia article: Linear congruential
     * generator</a>.
     *
     * Notice that this variant of a congruential generator produces values starting at <i>one</i>, not starting at
     * <i>zero</i>!
     */
    public static Producer<Long>
    multiplicativeCongruentialGenerator(final long seed, final long m, final long a) {

        return new Producer<Long>() {

            long x = 1 + ((0x7fffffffffffffffL & seed) % (m - 1));

            @Override public Long
            produce() {
                this.x = (a * this.x) % m;
                return this.x;
            }
        };
    }
}
