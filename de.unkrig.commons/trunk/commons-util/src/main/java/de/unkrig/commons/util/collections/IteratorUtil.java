
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

package de.unkrig.commons.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.Transformer;
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
    filter(final Iterator<? extends T> delegate, final Predicate<? super T> qualifies) {

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
}
