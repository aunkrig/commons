
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

import java.util.Collection;
import java.util.Map;

/**
 * Various {@link Predicate}-related utility methods.
 */
public final
class PredicateUtil {

    private
    PredicateUtil() {}

    /**
     * @return A {@link Predicate} which evaluates to the <var>value</var> (regardless of the <var>subject</var>)
     */
    public static <T> Predicate<T>
    always(boolean value) { return value ? PredicateUtil.<T>always() : PredicateUtil.<T>never(); }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} (regardless of the subject)
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    always() { return PredicateUtil.ALWAYS; }

    /**
     * @return A {@link Predicate} which evaluates to {@code false} (regardless of the <var>subject</var>)
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    never() { return PredicateUtil.NEVER; }

    @SuppressWarnings("rawtypes") private static final Predicate
    ALWAYS = new Predicate() {

        @Override public boolean
        evaluate(Object subject) { return true; }

        @Override public String
        toString() { return "ALWAYS"; }
    };

    @SuppressWarnings("rawtypes") private static final Predicate
    NEVER = new Predicate() {

        @Override public boolean
        evaluate(Object subject) { return false; }

        @Override public String
        toString() { return "NEVER"; }
    };

    /**
     * Creates and returns a {@link Predicate} for which the <em>first</em> evaluation will yield {@code true}, and,
     * after that, the time interval between adjacent {@code true} evaluations will (A) be minimal and (B) never
     * shorter than <var>milliseconds</var>.
     *
     * @deprecated Use "{@link PredicateUtil}{@code .}{@link PredicateUtil#ignoreSubject ignoreSubject}{@code (}{@link
     *             ProducerUtil}{@code .}{@link ProducerUtil#atMostEvery(long) atMostEvery}{@code
     *             (}<var>milliseconds</var>{@code ))}" instead
     */
    @Deprecated
    public static <T> Predicate<T>
    atMostEvery(final long milliseconds) {

        return PredicateUtil.ignoreSubject(ProducerUtil.atMostEvery(milliseconds));
    }

    /**
     * Returns a {@link Predicate} which evaluates to {@code true} iff both <var>lhs</var> and <var>rhs</var> evaluate
     * to {@code true}.
     * <p>
     *   Notice that iff either of the <var>lhs</var> and the <var>rhs</var> is {@link #never()}, then none of the
     *   two will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    and(final Predicate<? super T> lhs, final Predicate<? super T> rhs) {

        if (lhs == PredicateUtil.NEVER || rhs == PredicateUtil.NEVER) return PredicateUtil.NEVER;
        if (lhs == PredicateUtil.ALWAYS) return (Predicate<T>) rhs;
        if (rhs == PredicateUtil.ALWAYS) return (Predicate<T>) lhs;

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return lhs.evaluate(subject) && rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + " && " + rhs; }
        };
    }

    /**
     * Returns a {@link Predicate} which evaluates to {@code true} iff both the <var>rhs</var> is {@code true}
     * and the <var>lhs</var> evaluates to {@code true}.
     * <p>
     *   Notice that iff the <var>rhs</var> is {@code false}, then the <var>lhs</var> will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    and(Predicate<? super T> lhs, boolean rhs) {
        return (
            !rhs ? PredicateUtil.NEVER :
            lhs == PredicateUtil.ALWAYS ? PredicateUtil.always(rhs) :
            (Predicate<T>) lhs
        );
    }

    /**
     * Returns a {@link Predicate} which evaluates to {@code true} iff both the <var>lhs</var> is {@code true} and
     * the <var>rhs</var> evaluates to {@code true}.
     * <p>
     *   Notice that iff the <var>lhs</var> is {@code false}, then the <var>rhs</var> will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    and(boolean lhs, Predicate<? super T> rhs) {
        return (
            !lhs ? PredicateUtil.NEVER :
            rhs == PredicateUtil.ALWAYS ? PredicateUtil.always(lhs) :
            (Predicate<T>) rhs
        );
    }

    /**
     * Returns {@link Predicate} which evaluates to {@code true} if <var>lhs</var>, or <var>rhs</var>, or both evaluate
     * to {@code true}.
     * <p>
     *   Notice that iff either of the <var>lhs</var> and the <var>rhs</var> is {@link #always()}, then none of the
     *   two will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    or(final Predicate<? super T> lhs, final Predicate<? super T> rhs) {

        if (lhs == PredicateUtil.ALWAYS || rhs == PredicateUtil.ALWAYS) return PredicateUtil.ALWAYS;
        if (lhs == PredicateUtil.NEVER) return (Predicate<T>) rhs;
        if (rhs == PredicateUtil.NEVER) return (Predicate<T>) lhs;

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return lhs.evaluate(subject) || rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + " || " + rhs; }
        };
    }

    /**
     * Returns {@link Predicate} which evaluates to {@code true} if <var>lhs</var>, or <var>rhs</var>, or both evaluate
     * to {@code true}.
     * <p>
     *   Notice that iff either of the <var>lhs</var> and the <var>rhs</var> is {@link #always()}, then none of the
     *   two will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T, EX extends Throwable> PredicateWhichThrows<? super T, ? extends EX>
    or(
        final PredicateWhichThrows<? super T, ? extends EX> lhs,
        final PredicateWhichThrows<? super T, ? extends EX> rhs
    ) {

        if (lhs == PredicateUtil.ALWAYS || rhs == PredicateUtil.ALWAYS) return PredicateUtil.ALWAYS;
        if (lhs == PredicateUtil.NEVER) return rhs;
        if (rhs == PredicateUtil.NEVER) return lhs;

        return new PredicateWhichThrows<T, EX>() {

            @Override public boolean
            evaluate(T subject) throws EX { return lhs.evaluate(subject) || rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + " || " + rhs; }
        };
    }

    /**
     * Returns a {@link Predicate} which evaluates to {@code true} the <var>lhs</var> evaluates to {@code true}, or the
     * <var>rhs</var> is {@code true}, or both.
     * <p>
     *   Notice that iff the <var>rhs</var> is {@code true}, then the <var>lhs</var> will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    or(Predicate<? super T> lhs, boolean rhs) {
        return (
            rhs ? PredicateUtil.ALWAYS :
            lhs == PredicateUtil.NEVER ? PredicateUtil.always(rhs) :
            (Predicate<T>) lhs
        );
    }

    /**
     * Returns a {@link Predicate} which evaluates to {@code true} if the <var>lhs</var> is {@code true}, or the
     * <var>rhs</var> evaluates to {@code true}, or both.
     * <p>
     *   Notice that iff the <var>lhs</var> is {@code true}, then the <var>rhs</var> will ever be evaluated.
     * </p>
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    or(boolean lhs, Predicate<? super T> rhs) {
        return (
            lhs ? PredicateUtil.ALWAYS :
            rhs == PredicateUtil.NEVER ? PredicateUtil.always(lhs) :
            (Predicate<T>) rhs
        );
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} iff exactly <em>one</em> of <var>lhs</var> or
     *         <var>rhs</var> evaluate to {@code true}
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    xor(final Predicate<? super T> lhs, final Predicate<? super T> rhs) {

        if (lhs == PredicateUtil.ALWAYS) {
            if (rhs == PredicateUtil.ALWAYS) return PredicateUtil.NEVER;
            if (rhs == PredicateUtil.NEVER)  return PredicateUtil.ALWAYS;
        }
        if (lhs == PredicateUtil.NEVER) {
            if (rhs == PredicateUtil.ALWAYS) return PredicateUtil.ALWAYS;
            if (rhs == PredicateUtil.NEVER)  return PredicateUtil.NEVER;
        }

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return lhs.evaluate(subject) ^ rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + " ^ " + rhs; }
        };
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} iff either the <var>lhs</var> evaluates to {@code
     *         true} and <var>rhs</var> is {@code false}, or the <var>lhs</var> evaluates to {@code false} and
     *         <var>rhs</var> is {@code true}
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    xor(Predicate<? super T> lhs, boolean rhs) { return rhs ? PredicateUtil.not(lhs) : (Predicate<T>) lhs; }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} iff either <var>lhs</var> is {@code true} and the
     *         <var>rhs</var> evaluates to {@code false}, or the <var>lhs</var> is {@code false} and the
     *         <var>rhs</var> evaluates to {@code true}
     */
    @SuppressWarnings("unchecked") public static <T> Predicate<T>
    xor(boolean lhs, Predicate<? super T> rhs) { return lhs ? PredicateUtil.not(rhs) : (Predicate<T>) rhs; }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} iff the <var>delegate</var> evaluates to {@code
     *         false}
     */
    public static <T> Predicate<T>
    not(final Predicate<? super T> delegate) {

        if (delegate == PredicateUtil.NEVER) return PredicateUtil.always();
        if (delegate == PredicateUtil.ALWAYS) return PredicateUtil.never();

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return !delegate.evaluate(subject); }

            @Override public String
            toString() { return "!" + delegate; }
        };
    }

    /**
     * Shorthand for "{@link #always(boolean) always}{@code (}<var>value</var>{@code )}".
     *
     * @return A {@link Predicate} which evaluates to the negation of the <var>value</var> (regardless of the subject)
     */
    public static <T> Predicate<T>
    not(boolean value) {
        return PredicateUtil.always(!value);
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} for any <var>subject</var> that is less than the
     *         <var>other</var>
     */
    public static <C extends Comparable<C>> Predicate<C>
    less(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) > 0; }

            @Override public String
            toString() { return "< " + other; }
        };
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} for any given <var>subject</var> that is less than or
     *         equal to the <var>other</var>
     */
    public static <C extends Comparable<C>> Predicate<C>
    lessEqual(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) >= 0; }

            @Override public String
            toString() { return "<= " + other; }
        };
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} for any given <var>subject</var> that is greater
     *         than the <var>other</var>
     */
    public static <C extends Comparable<C>> Predicate<C>
    greater(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) < 0; }

            @Override public String
            toString() { return "> " + other; }
        };
    }

    /**
     * @return A {@link Predicate} which evaluates to {@code true} for any given <var>subject</var> that is greater
     *         than or equal to the <var>other</var>
     */
    public static <C extends Comparable<C>> Predicate<C>
    greaterEqual(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) <= 0; }

            @Override public String
            toString() { return ">= " + other; }
        };
    }

    /**
     * @return                           A {@link Predicate} which evaluates to {@code true} for any given
     *                                   <var>subject</var> that is (arithmetically) equal to the <var>other</var>
     * @see Comparable#compareTo(Object)
     */
    public static <C extends Comparable<C>> Predicate<C>
    equal(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) == 0; }

            @Override public String
            toString() { return "== " + other; }
        };
    }

    /**
     * @return                           A {@link Predicate} which evaluates to {@code true} for any given
     *                                   <var>subject</var> that is (arithmetically) not equal to the <var>other</var>
     * @see Comparable#compareTo(Object)
     */
    public static <C extends Comparable<C>> Predicate<C>
    notEqual(final C other) {

        return new Predicate<C>() {

            @Override public boolean
            evaluate(C subject) { return other.compareTo(subject) != 0; }

            @Override public String
            toString() { return "!= " + other; }
        };
    }

    /**
     * @return A predicate that evaluates to
     *         "<var>min</var> {@code <=} <var>subject</var> {@code <=} <var>max</var>",
     *         or, if <var>min</var> {@code >} <var>max</var>, to
     *         "<var>subject</var> {@code >=} <var>min</var> {@code ||} <var>subject</var> {@code <=} <var>max</var>"
     */
    public static <C extends Comparable<C>> Predicate<C>
    between(final C min, final C max) {

        return (
            max.compareTo(min) >= 0
            ? new Predicate<C>() {

                @Override public boolean
                evaluate(C subject) { return min.compareTo(subject) <= 0 && max.compareTo(subject) >= 0; }

                @Override public String
                toString() { return min + "-" + max; }
            }
            : new Predicate<C>() {

                @Override public boolean
                evaluate(C subject) { return min.compareTo(subject) >= 0 || max.compareTo(subject) <= 0; }

                @Override public String
                toString() { return min + "-" + max; }
            }
        );
    }

    /**
     * Value equality, as opposed to arithmetical equality.
     *
     * @see #equal(Comparable)
     */
    public static <T> Predicate<T>
    equal(final T other) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return other.equals(subject); }

            @Override public String
            toString() { return "eq " + other; }
        };
    }

    /** Value inequality. */
    public static <T> Predicate<T>
    notEqual(final T other) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return !other.equals(subject); }

            @Override public String
            toString() { return "nq " + other; }
        };
    }

    /**
     * Returns a "{@link Predicate}{@code <Object>}" that evaluates to {@code true} iff the current time is after the
     * given <var>expirationTime</var> (in milliseconds).
     * <p>
     *   (The returned predicate ignores its <var>subject</var> argument.)
     * </p>
     *
     * @param expirationTime Milliseconds since Jan 1 1970, UTC
     * @deprecated           Use "{@link PredicateUtil}{@code .}{@link PredicateUtil#ignoreSubject(Producer)
     *                       ignoreSubject}{@code (}{@link ProducerUtil}{@code .}{@link ProducerUtil#after(long)
     *                       after}{@code (}<var>expirationTime</var>{@code ))}" instead
     */
    @Deprecated public static Predicate<Object>
    after(final long expirationTime) { return PredicateUtil.ignoreSubject(ProducerUtil.after(expirationTime)); }

    /**
     * Returns a predicate that evaluates the <var>format</var> against the <var>delegate</var> after each "*" in the
     * <var>format</var> has been replaced with the subject.
     */
    public static <T extends Predicate<String>> T
    forString(final String format, final T delegate) {

        @SuppressWarnings("unchecked") T result = (T) new Predicate<String>() {

            @Override public boolean
            evaluate(String subject) { return delegate.evaluate(format.replace("*", subject)); }
        };

        return result;
    }

    /**
     * Converts a {@link Predicate} into a {@link PredicateWhichThrows}, which is possible iff the source's element
     * type is a subclass of the target's element type.
     *
     * @param <T>  The subject type
     * @param <EX> The target predicate's exception
     */
    public static <T, EX extends Throwable> PredicateWhichThrows<T, EX>
    asPredicateWhichThrows(final Predicate<? super T> source) {

        return new PredicateWhichThrows<T, EX>() {

            @Override public boolean
            evaluate(T subject) { return source.evaluate(subject); }
        };
    }

    /**
     * Converts a {@link PredicateWhichThrows} into a {@link Predicate}, which is possible iff the source's exception
     * is a subclass of {@link RuntimeException} and the source's element type is a subclass of the target's element
     * type.
     *
     * @param <T>  The predicate subject type
     * @param <EX> The source predicate's exception
     */
    public static <T, EX extends RuntimeException> Predicate<T>
    asPredicate(final PredicateWhichThrows<T, EX> source) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) { return source.evaluate(subject); }
        };
    }

    /**
     * @return A predicate which checks whether the given <var>collection</var> contains the <var>subject</var>
     */
    public static <T> Predicate<T>
    contains(final Collection<? extends T> collection) {

        return new Predicate<T>() {
            @Override public boolean evaluate(T subject) { return collection.contains(subject); }
        };
    }

    /**
     * @return A {@link Predicate} that evaluates to {@code true} iff the <var>map</var> contains a key equal to the
     *         predicate subject
     */
    public static <K> Predicate<K>
    containsKey(final Map<K, ?> map) {

        return new Predicate<K>() {
            @Override public boolean evaluate(K subject) { return map.containsKey(subject); }
        };
    }

    /**
     * @return A {@link Predicate} that evaluates to {@code true} iff the <var>mapping</var> contains a key equal to the
     *         predicate subject
     */
    public static <K> Predicate<K>
    containsKey(final Mapping<K, ?> mapping) {

        return new Predicate<K>() {
            @Override public boolean evaluate(K subject) { return mapping.containsKey(subject); }
        };
    }

    /**
     * @return A predicate which, when evaluated, ignores its <var>subject</var>, and returns the next product
     *         of the <var>delegate</var>
     */
    public static <T, EX extends Throwable> PredicateWhichThrows<T, EX>
    ignoreSubject(final ProducerWhichThrows<Boolean, EX> delegate) {

        return new PredicateWhichThrows<T, EX>() {

            @Override public boolean
            evaluate(T subject) throws EX {
                Boolean b = delegate.produce();
                return b != null && b;
            }
        };
    }

    /**
     * @return A predicate which, when evaluated, ignores its <var>subject</var>, and returns the next product
     *         of the <var>delegate</var>
     */
    public static <T, EX extends Throwable> Predicate<T>
    ignoreSubject(final Producer<Boolean> delegate) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) {
                Boolean b = delegate.produce();
                return b != null && b;
            }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught, ignored, and the
     * <var>defaultValue</var> is returned.
     */
    public static <T, EX extends Throwable> Predicate<T>
    ignoreExceptions(
        final Class<EX>                   exceptionClass,
        final PredicateWhichThrows<T, EX> delegate,
        final boolean                     defaultValue
    ) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) {

                try {
                    return delegate.evaluate(subject);
                } catch (RuntimeException re) {
                    if (!exceptionClass.isAssignableFrom(re.getClass())) throw re;
                } catch (Error e) {     // SUPPRESS CHECKSTYLE IllegalCatch
                    if (!exceptionClass.isAssignableFrom(e.getClass())) throw e;
                } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                    assert exceptionClass.isAssignableFrom(t.getClass());
                }

                return defaultValue;
            }
        };
    }

    /**
     * Creates and returns a proxy to the <var>delegate</var> with a synchronized {@link
     * PredicateWhichThrows#evaluate(Object) evaluate()} method.
     *
     * @see PredicateWhichThrows
     */
    public static <T, EX extends Throwable> PredicateWhichThrows<T, EX>
    synchronizedPredicate(final PredicateWhichThrows<? super T, EX> delegate) {

        return new PredicateWhichThrows<T, EX>() {

            @Override public synchronized boolean
            evaluate(T subject) throws EX { return delegate.evaluate(subject); }
        };
    }
}
