
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

package de.unkrig.commons.util;

import java.lang.reflect.Array;

/**
 * Various array-related utility methods.
 */
public final
class ArrayUtil {

    private ArrayUtil() {}

    /**
     * @return An array of size {@code a.length + values.length}, filled with the values from <var>a</var> and
     *         <var>values</var>
     */
    public static <T> T[]
    append(T[] a, T... values) {
        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(
            a.getClass().getComponentType(),
            a.length + values.length
        );
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(values, 0, result, a.length, values.length);
        return result;
    }

    /**
     * @return An array of size {@code a.length + values.length}, filled with the values from <var>a</var> and
     *         <var>values</var>
     */
    public static char[]
    append(char[] a, char... values) {
        char[] result = new char[a.length + values.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(values, 0, result, a.length, values.length);
        return result;
    }

    /**
     * @param cas All elements must have equal length
     * @return    The mirrored two-dimensional {@code char} array <var>cas</var>
     */
    public static char[][]
    mirror(char[]... cas) {

        int n1 = cas.length;
        if (n1 == 0) return cas;

        int n2 = cas[0].length;

        char[][] result = new char[n2][n1];

        for (int j = 0; j < n2; j++) {
            result[j][0] = cas[0][j];
        }

        for (int i = 1; i < n1; i++) {

            if (cas[i].length != n2) {
                throw new IllegalArgumentException(
                    "Length of element #"
                    + i
                    + " should be "
                    + n2
                    + ", but is "
                    + cas[i].length
                );
            }

            for (int j = 0; j < n2; j++) result[j][i] = cas[i][j];
        }

        return result;
    }
}
