
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.lang;

import java.util.Comparator;
import java.util.Map;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Comparator}-related utility methods.
 */
public final
class Comparators {

    private
    Comparators() {}

    /**
     * @return A {@link Comparator} that compares subjects by their {@link Comparable#compareTo(Object)} method (and
     *         is thus not {@code null}-safe)
     */
    public static <T extends Comparable<T>> Comparator<T>
    naturalOrderComparator() {

        @SuppressWarnings("unchecked") Comparator<T>
        tmp = (Comparator<T>) Comparators.NATURAL_ORDER_COMPARATOR;

        return tmp;
    }

    private static final Comparator<?>
    NATURAL_ORDER_COMPARATOR = new Comparator<Comparable<Object>>() {

        @Override @NotNullByDefault(false) public int
        compare(Comparable<Object> c1, Comparable<Object> c2) { return c1.compareTo(c2); }
    };

    /**
     * @return {@code 0} if both arguments are {@code null},
     *         or {@code -1} if only <var>o1</var> is {@code null},
     *         or {@code 1} if only <var>o2</var> is {@code null},
     *         and otherwise <var>o1</var>{@code .compareTo(}<var>o2</var>{@code )}
     */
    public static <T extends Comparable<T>> int
    compareNullSafe(@Nullable T c1, @Nullable T c2) {

        return (
            c1 == null ? (c2 == null ? 0 : -1) :
            c2 == null ? 1 :
            c1.compareTo(c2)
        );
    }

    /**
     * Wraps the <var>delegate</var> comparator such that the cases "<var>o1</var> {@code == null}" and/or
     * "<var>o2</var> {@code == null}" are detected and handled as follows:
     * <dl>
     *   <dt>{@code o1 == null && o2 == null}</dt>
     *   <dd>{@code 0} (as if <var>o1</var>{@code .compareTo(}<var>o2</var>{@code ) == 0})</dd>
     *   <dt>{@code o1 == null && o2 != null}</dt>
     *   <dd>{@code -1} (as if <var>o1</var>{@code .compareTo(}<var>o2</var>{@code ) < 0})</dd>
     *   <dt>{@code o1 != null && o2 == null}</dt>
     *   <dd>{@code 1} (as if <var>o1</var>{@code .compareTo(}<var>o2</var>{@code ) > 0})</dd>
     *   <dt>{@code o1 != null && o2 != null}</dt>
     *   <dd><var>delegate</var>{@code .compare(}<var>o1</var>{@code ,} <var>o2</var>{@code )}</dd>
     * </dl>
     * <p>
     *   The <var>delegate</var> is only invoked iff <var>o1</var> {@code != null &&} <var>o2</var> {@code != null}.
     * </p>
     */
    public static <T> Comparator<T>
    nullSafeComparator(final Comparator<T> delegate) {

        return new Comparator<T>() {

            @Override public int
            compare(@Nullable T o1, @Nullable T o2) {

                return (
                    o1 == null ? (o2 == null ? 0 : -1) :
                    o2 == null ? 1 :
                    delegate.compare(o1, o2)
                );
            }
        };
    }

    /**
     * @return A {@code Comparator<Map.Entry>)} that compares {@link java.util.Map.Entry}s by comparing their
     *         <em>keys</em>
     */
    public static <T> Comparator<Map.Entry<T, ?>>
    keyComparator(final Comparator<T> delegate) {

        return new Comparator<Map.Entry<T, ?>>() {

            @Override @NotNullByDefault(false) public int
            compare(Map.Entry<T, ?> e1, Map.Entry<T, ?> e2) { return delegate.compare(e1.getKey(), e2.getKey()); }
        };
    }

    /**
     * @return A {@code Comparator<Map.Entry>)} that compares {@link java.util.Map.Entry}s by comparing their
     *         <em>values</em>
     */
    public static <T> Comparator<Map.Entry<?, T>>
    valueComparator(final Comparator<T> delegate) {

        return new Comparator<Map.Entry<?, T>>() {

            @Override @NotNullByDefault(false) public int
            compare(Map.Entry<?, T> e1, Map.Entry<?, T> e2) { return delegate.compare(e1.getValue(), e2.getValue()); }
        };
    }
}
