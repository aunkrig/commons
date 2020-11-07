
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2020, Arno Unkrig
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

package de.unkrig.commons.lang.protocol;

/**
 * Various {@link Relation}-related utility methods.
 * <p>
 *   Examples:
 * </p>
 * <pre>{@code
 *   Relation<Float> greaterOrEqual = Relations.not(Relations.lessThan());
 * }</pre>
 */
public final
class Relations {

    private
    Relations() {}

    /**
     * @return A {@link Predicate} that relates the <var>subject</var> to the <var>rhs</var>
     */
    public static <T extends Comparable<T>> Predicate<T>
    compareWithConstant(final Relation<T> relation, final T rhs) {

        return new Predicate<T>() {
            @Override public boolean evaluate(T subject) { return relation.evaluate(subject, rhs); }
            @Override public String  toString()          { return relation.toString() + " " + rhs; }
        };
    }

    /**
     * @return A {@link Relation} that evaluates {@code true} iff the <var>lhs</var> is greater than the <var>rhs</var>
     */
    @SuppressWarnings("unchecked") public static <T extends Comparable<T>> Relation<T>
    greaterThan() { return (Relation<T>) Relations.GREATER_THAN; }

    /**
     * @return A {@link Relation} that evaluates {@code true} iff the <var>lhs</var> is less than the <var>rhs</var>
     */
    @SuppressWarnings("unchecked") public static <T extends Comparable<T>> Relation<T>
    lessThan() { return (Relation<T>) Relations.LESS_THAN; }

    /**
     * @return A {@link Relation} that evaluates {@code true} iff the <var>lhs</var> is equal to the <var>rhs</var>
     */
    @SuppressWarnings("unchecked") public static <T extends Comparable<T>> Relation<T>
    equalTo() { return (Relation<T>) Relations.EQUAL_TO; }

    /**
     * @return A {@link Relation} that evaluates to the negation of the <var>delegate</var> relation
     */
    public static <T extends Comparable<T>> Relation<T>
    not(final Relation<T> delegate) {
        return new Relation<T>() {

            @Override public boolean
            evaluate(Comparable<T> lhs, Comparable<T> rhs) { return !delegate.evaluate(lhs, rhs); }

            @Override public String
            toString() { return "!" + delegate.toString(); }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) private static final Relation<? extends Comparable>
    GREATER_THAN = new Relation() {
        @Override public boolean evaluate(Comparable lhs, Comparable rhs) { return lhs.compareTo(rhs) > 0; }
        @Override public String  toString()                               { return ">"; }
    };
    @SuppressWarnings({ "rawtypes", "unchecked" }) private static final Relation<? extends Comparable>
    LESS_THAN = new Relation() {
        @Override public boolean evaluate(Comparable lhs, Comparable rhs) { return lhs.compareTo(rhs) < 0; }
        @Override public String  toString()                               { return "<"; }
    };
    @SuppressWarnings({ "rawtypes", "unchecked" }) private static final Relation<? extends Comparable>
    EQUAL_TO = new Relation() {
        @Override public boolean evaluate(Comparable lhs, Comparable rhs) { return lhs.compareTo(rhs) == 0; }
        @Override public String  toString()                               { return "=="; }
    };
}
