
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

package de.unkrig.commons.util.collections;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import de.unkrig.commons.lang.protocol.PredicateWhichThrows;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Iterator}-related utility methods.
 */
public final
class IteratorUtil {

    private
    IteratorUtil() {}

    /**
     * An iterator which is at its end.
     * <p>
     *   To get the non-rawe type, use {@link #atEnd()}.
     * </p>
     */
    @SuppressWarnings("rawtypes") public static final Iterator
    AT_END = new Iterator() {
        @Override public boolean hasNext() { return false; }
        @Override public Object  next()    { throw new NoSuchElementException(); }
        @Override public void    remove()  { throw new UnsupportedOperationException(); }
    };

    /**
     * @return An {@link Iterator} which is always at its end
     */
    @SuppressWarnings("unchecked") public static <T> Iterator<T>
    atEnd() { return IteratorUtil.AT_END; }

    /**
     * Returns an iterator which skips the elements of the <var>delegate</var> which do not qualifiy.
     */
    public static <T> Iterator<T>
    filter(
        final Iterator<? extends T>                                       delegate,
        final PredicateWhichThrows<? super T, ? extends RuntimeException> qualifies
    ) {

        return new Iterator<T>() {

            @Nullable T lookahead;

            @Override public boolean
            hasNext() {
                if (this.lookahead != null) return true;
                for (;;) {
                    if (!delegate.hasNext()) return false;
                    T tmp = delegate.next();
                    if (qualifies.evaluate(tmp)) {
                        this.lookahead = tmp;
                        return true;
                    }
                }
            }

            @Override public T
            next() {
                if (this.lookahead != null) {
                    T tmp = this.lookahead;
                    this.lookahead = null;
                    return tmp;
                }
                for (;;) {
                    T tmp = delegate.next();
                    if (qualifies.evaluate(tmp)) return tmp;
                }
            }

            @Override public void
            remove() { delegate.remove(); }
        };
    }

    /**
     * Returns an iterator which iterates the <i>transformed</i> elements of the <var>delegate</var>
     */
    public static <I, O> Iterator<O>
    transform(final Iterator<? extends I> delegate, final Transformer<? super I, ? extends O> transform) {

        return new Iterator<O>() {

            @Override public boolean hasNext() { return delegate.hasNext(); }
            @Override public O       next()    { return transform.transform(delegate.next()); }
            @Override public void    remove()  { delegate.remove(); }
        };
    }

    /**
     * @return An iterator that produces an infinite sequence of <var>value</var> elements
     */
    public static <T> Iterator<T>
    repeat(final T value) {
        return new Iterator<T>() {
            @Override public boolean hasNext() { return true; }
            @Override public T       next()    { return value; }
            @Override public void    remove()  { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * Returns a list iterator that traverses the list elements in the reverse order of the given <var>delegate</var>.
     * <p>
     *   Notice that iff {@code !}<var>delegate</var>{@code .hasPrevious()}, then {@code
     *   !reverse(}<var>delegate</var>{@code ).hasNext()}. In other words, you will often use
     * </p>
     * <pre>{@code IteratorUtil.reverse(list.listIterator(list.size()))}</pre>
     * <p>
     *   , and not simply
     * </p>
     * <pre>{@code IteratorUtil.reverse(list.listIterator())}</pre>
     */
    public static <E> ListIterator<E>
    reverse(final ListIterator<E> delegate) {

        return new ListIterator<E>() {

            @Override public boolean                       hasNext()     { return delegate.hasPrevious(); }
            @Override public E                             next()        { return delegate.previous();    }
            @Override public boolean                       hasPrevious() { return delegate.hasNext();     }
            @Override public E                             previous()    { return delegate.next();        }
            @Override @NotNullByDefault(false) public void set(E e)      { delegate.set(e);               }
            @Override @NotNullByDefault(false) public void add(E e)      { delegate.set(e);               }
            @Override public void                          remove()      { delegate.remove();             }

            @Override public int nextIndex()     { throw new UnsupportedOperationException("nextIndex"); }
            @Override public int previousIndex() { throw new UnsupportedOperationException("previousIndex"); }
        };
    }

    /**
     * @return An iterator that produces a sequence of <var>n</var> <var>value</var> elements
     */
    public static <T> Iterator<T>
    repeat(final int n, final T value) {
        return new Iterator<T>() {

            int count;

            @Override public boolean
            hasNext() { return this.count < n; }

            @Override public T
            next() {
                if (this.count >= n) throw new NoSuchElementException();
                this.count++;
                return value;
            }

            @Override public void
            remove() { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * An {@link Iterator} that has a notion of an "array index", which is the index of the "next" element in an
     * array.
     *
     * @param <T> See {@link Iterator}
     */
    public
    interface ArrayIterator<T> extends Iterator<T> {

        /**
         * @return The index of the "next" element in the <var>array</var>; <var>array</var>{@code .length} on
         *         end-of-array
         */
        int index();
    }

    /**
     * @return An iterator over the elements of the <var>array</var>
     */
    public static <T> ArrayIterator<T>
    iterator(final T[] array) {

        return new ArrayIterator<T>() {

            int index;

            @Override public boolean
            hasNext() { return this.index < array.length; }

            @Override public T
            next() {
                if (this.index >= array.length) throw new NoSuchElementException();
                return array[this.index++];
            }

            @Override public void
            remove() { throw new UnsupportedOperationException("remove"); }

            @Override public int
            index() { return this.index; }
        };
    }

    /**
     * Retrieves, counts and discards all elements remaining on the <var>iterator</var>
     */
    public static int
    elementCount(Iterator<Integer> iterator) {
        int n = 0;
        for (; iterator.hasNext(); iterator.next()) n++;
        return n;
    }

    /**
     * Wraps the <var>delegate</var> iterator in an {@code Iterator<ElementWithContext>}.
     *
     * @param delegate Must produce non-{@code null} values
     * @return         An iterator that produces {@link ElementWithContext}s from the sequence produced by the
     *                 <var>delegate</var>
     */
    public static <T> Iterator<ElementWithContext<T>>
    iteratorWithContext(final Iterator<? extends T> delegate) {

        class ElementWithContextIterator implements Iterator<ElementWithContext<T>> {

            @Nullable private T current, next;

            @Override public boolean
            hasNext() {
                return this.next != null || delegate.hasNext();
            }

            @Override public ElementWithContext<T>
            next() {

                final T previous = this.current;

                if (this.next == null) {
                    T e = delegate.next();
                    assert e != null : "Delegate must produce non-null values";
                    this.current = e;
                } else {
                    this.current = this.next;
                    this.next    = null;
                }

                return new ElementWithContext<T>() {

                    @Override @Nullable public T
                    previous() { return previous; }

                    @Override @NotNull public T
                    current() {
                        T result = ElementWithContextIterator.this.current;
                        assert result != null;
                        return result;
                    }

                    @Override @Nullable public T
                    next() {
                        if (ElementWithContextIterator.this.next != null) return ElementWithContextIterator.this.next;

                        if (!delegate.hasNext()) return null;
                        T e = delegate.next();
                        assert e != null : "Delegate must produce non-null values";
                        ElementWithContextIterator.this.next = e;
                        return ElementWithContextIterator.this.next;
                    }
                };
            }

            @Override public void
            remove() { throw new UnsupportedOperationException("remove"); }
        }

        return new ElementWithContextIterator();
    }

    /**
     * Equivalent with {@link #foR(int, int, int) foR}{@code (}<var>start</var>{@code ,} <var>end</var>{@code ,1)}.
     *
     * @see #foR(int, int, int)
     */
    public static Iterator<Integer>
    foR(final int start, final int end) { return IteratorUtil.foR(start, end, 1); }

    /**
     * Creates and returns an {@link Iterator Iterator&lt;Integer>} that counts from <var>start</var> (inclusively) to
     * <var>end</var> (exclusively), with a <var>step</var> increment.
     * <p>
     *   More precise: Iff <var>step</var> is greater than zero, then the returned iterator produces the values
     *   <var>start</var>, <var>start</var> {@code +} <var>step</var>, and so forth, and ends with the last value which
     *   is less than <var>end</var>.
     * <p>
     * <p>
     *   Otherwise, iff <var>step</var> is less than zero, then the returned iterator produces the values <var>start</var>,
     *   <var>start</var> {@code +} <var>step</var>, and so forth, and ends with the last value which is greater than
     *   <var>end</var>.
     * <p>
     * <p>
     *   Otherwise, <var>step</var> is zero, and the returned iterator produces either an
     *   infinite sequence of values <var>start</var>, or, iff <var>start</var> {@code == } <var>end</var>, an empty
     *   sequence.
     * </p>
     *
     * @throws IllegalArgumentException <var>step</var> {@code > 0 &&} <var>end</var> {@code <} <var>start</var>
     * @throws IllegalArgumentException <var>step</var> {@code < 0 &&} <var>end</var> {@code >} <var>start</var>
     */
    public static Iterator<Integer>
    foR(final int start, final int end, final int step) {

        if (step == 0) {

            // Optimize for the "step == 0" case.
            return start == end ? IteratorUtil.<Integer>atEnd() : IteratorUtil.repeat(start);
        } else
        if (step > 0) {
            if (end < start) throw new IllegalArgumentException("step > 0 and end < start");

            return new Iterator<Integer>() {

                int nextValue = start;

                @Override public boolean
                hasNext() {
                    return this.nextValue < end;
                }

                @Override public Integer
                next() {
                    if (this.nextValue >= end) throw new NoSuchElementException();
                    int result = this.nextValue;
                    this.nextValue += step;
                    return result;
                }

                @Override public void
                remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } else
        {
            if (end > start) throw new IllegalArgumentException("step < 0 and end > start");

            return new Iterator<Integer>() {

                int nextValue = start;

                @Override public boolean
                hasNext() {
                    return this.nextValue > end;
                }

                @Override public Integer
                next() {
                    if (this.nextValue <= end) throw new NoSuchElementException();
                    int result = this.nextValue;
                    this.nextValue += step;
                    return result;
                }

                @Override public void
                remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };

        }
    }
}
