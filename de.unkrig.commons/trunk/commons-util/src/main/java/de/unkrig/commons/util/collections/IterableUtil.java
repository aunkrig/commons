
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

package de.unkrig.commons.util.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Iterable}-related utility methods.
 */
public final
class IterableUtil {

    private
    IterableUtil() {}

    /**
     * @return An {@link Iterable} which can't be modified through the {@link Iterator#remove()} method
     */
    public static <T> Iterable<T>
    unmodifiableIterable(final Iterable<? extends T> i) {
        return new Iterable<T>() {

            @Override public Iterator<T>
            iterator() {
                final Iterator<? extends T> it = i.iterator();

                return new Iterator<T>() {

                    @Override public boolean
                    hasNext() { return it.hasNext(); }

                    @Override public T
                    next() { return it.next(); }

                    @Override public void
                    remove() { throw new UnsupportedOperationException("remove"); }
                };
            }
        };
    }

    /**
     * @return An {@link Iterable} producing the  given <var>subject</var> <var>n</var> times
     */
    public static <T> Iterable<T>
    repeat(final T subject, final int n) {
        if (n < 0) throw new IllegalArgumentException(Integer.toString(n));

        return new Iterable<T>() {

            @Override public Iterator<T>
            iterator() {
                return new Iterator<T>() {

                    final AtomicInteger index = new AtomicInteger();

                    @Override public boolean
                    hasNext() {
                        return this.index.intValue() < n;
                    }

                    @Override public T
                    next() {
                        if (this.index.getAndIncrement() >= n) throw new NoSuchElementException();

                        return subject;
                    }

                    @Override public void
                    remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }
        };
    }

    /**
     * @return An {@link Iterable} that traverses the elements of the given {@link List} in reverse order.
     */
    public static <E> Iterable<E>
    reverseList(final List<? extends E> list) {

        return new Iterable<E>() {

            @Override public Iterator<E>
            iterator() {
                final ListIterator<? extends E> li = list.listIterator(list.size());
                return new Iterator<E>() {

                    @Override public boolean
                    hasNext() { return li.hasPrevious(); }

                    @Override public E
                    next() { return li.previous(); }

                    @Override public void
                    remove() { li.remove(); }
                };
            }
        };
    }

    /**
     * @return An iterator for <var>element1</var> and the elements of <var>element2</var>
     */
    public static <T> Iterable<T>
    concat(T element1, Iterable<? extends T> element2) {
        return IterableUtil.concat(Collections.singletonList(element1), element2);
    }

    /**
     * @return An iterator for <var>element1</var> <var>element2</var> and the elements of <var>element3</var>
     */
    public static <T> Iterable<T>
    concat(T element1, T element2, Iterable<? extends T> element3) {
        return IterableUtil.concat(Collections.singletonList(element1), Collections.singletonList(element2), element3);
    }

    /**
     * @return An iterator for the elements of <var>element1</var> and <var>element2</var>
     */
    public static <T> Iterable<T>
    concat(Iterable<? extends T> element1, T... element2) {
        return IterableUtil.concat(element1, Arrays.asList(element2));
    }

    /**
     * @return An iterator for the elements of <var>element1</var> and <var>element2</var>
     */
    public static <T> Iterable<T>
    concat(Iterable<? extends T> element1, Iterable<? extends T> element2) {
        List<Iterable<? extends T>> l = new ArrayList<Iterable<? extends T>>(2);
        l.add(element1);
        l.add(element2);
        return IterableUtil.concat(l);
    }

    /**
     * @return An iterator for the elements of <var>element1</var>, <var>element2</var> and <var>element3</var>
     */
    public static <T> Iterable<T>
    concat(Iterable<? extends T> element1, Iterable<? extends T> element2, Iterable<? extends T> element3) {
        List<Iterable<? extends T>> l = new ArrayList<Iterable<? extends T>>(3);
        l.add(element1);
        l.add(element2);
        l.add(element3);
        return IterableUtil.concat(l);
    }

    /**
     * @return An iterator for the elements of <var>element1</var>, <var>element2</var>, <var>element3</var> and
     *         <var>element4</var>
     */
    public static <T> Iterable<T>
    concat(
        Iterable<? extends T> element1,
        Iterable<? extends T> element2,
        Iterable<? extends T> element3,
        Iterable<? extends T> element4
    ) {
        List<Iterable<? extends T>> l = new ArrayList<Iterable<? extends T>>(4);
        l.add(element1);
        l.add(element2);
        l.add(element3);
        l.add(element4);
        return IterableUtil.concat(l);
    }

    /**
     * @return An iterator for the elements of the given <var>elements</var>
     */
    public static <T> Iterable<T>
    concat(final Iterable<? extends Iterable<? extends T>> elements) {

        return new Iterable<T>() {

            @Override public Iterator<T>
            iterator() {
                return new Iterator<T>() {

                    Iterator<? extends Iterable<? extends T>> outer = elements.iterator();
                    Iterator<? extends T>                     inner = IteratorUtil.atEnd();

                    @Override public boolean
                    hasNext() {

                        while (!this.inner.hasNext()) {
                            if (!this.outer.hasNext()) return false;
                            this.inner = this.outer.next().iterator();
                        }

                        return true;
                    }

                    @Override public T
                    next() throws NoSuchElementException {

                        if (!this.hasNext()) throw new NoSuchElementException();

                        return this.inner.next();
                    }

                    @Override public void
                    remove() {

                        this.inner.remove();
                    }
                };
            }
        };
    }

    /**
     * Returns an {@link Iterable} which hides the elements of the <var>delegate</var> which do not qualifiy.
     */
    public static <T> Iterable<T>
    filter(final Iterable<? extends T> delegate, final Predicate<? super T> qualifies) {

        return new Iterable<T>() {
            @Override public Iterator<T> iterator() { return IteratorUtil.filter(delegate.iterator(), qualifies); }
        };
    }

    /**
     * Returns an iterable which contains the <i>transformed</i> elements of the <var>delegate</var>
     */
    public static <I, O> Iterable<O>
    transform(final Iterable<? extends I> delegate, final Transformer<? super I, ? extends O> transform) {
        return new Iterable<O>() {

            @Override public Iterator<O>
            iterator() { return IteratorUtil.transform(delegate.iterator(), transform); }
        };
    }

    /**
     * Wraps the given {@link Iterable} in a collection. As enforced by the nature of the {@link Iterable}, the only
     * supported modifying operation of the returned collection is element removal.
     *
     * @see Collection#remove(Object)
     * @see Collection#removeAll(Collection)
     * @see Collection#retainAll(Collection)
     * @see Collection#clear()
     */
    public static <T> Collection<T>
    asCollection(final Iterable<T> delegate) {

        return new AbstractCollection<T>() {

            @Override public Iterator<T>
            iterator() { return delegate.iterator(); }

            @Override public int
            size() {
                int result = 0;
                for (Iterator<T> it = this.iterator(); it.hasNext(); it.next()) result++;
                return result;
            }

            @Override public boolean
            isEmpty() { return !this.iterator().hasNext(); }

            @Override public boolean
            addAll(@Nullable Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * A helper interface which extends the "iterator" pattern with the possibility to access not only the "current"
     * element, but also the previous and next element.
     * @param <T>
     */
    public
    interface ElementWithContext<T> {

        /**
         * @return The element before the current element, or {@code null} iffthe current element is the first element
         */
        @Nullable T previous();

        /**
         * @return The current element (always non-{@code null}
         */
        T current();

        /**
         * @return The element after the current element, or {@code null} iff the current element is the last element
         */
        @Nullable T next();
    }

    /**
     * @return An iterable which wraps the given <var>delegate</var>
     */
    public static <T> Iterable<ElementWithContext<T>>
    iterableWithContext(final Iterable<? extends T> delegate) {

        return new Iterable<IterableUtil.ElementWithContext<T>>() {

            @Override public Iterator<ElementWithContext<T>>
            iterator() { return IterableUtil.iteratorWithContext(delegate); }
        };
    }

    /**
     * @param iterable Must produce non-{@code null} values
     * @return         An iterator that produces {@link ElementWithContext}s for the elements of the given
     *                 <var>iterable</var>
     */
    public static <T> Iterator<ElementWithContext<T>>
    iteratorWithContext(final Iterable<? extends T> iterable) {

        class ElementWithContextIterator implements Iterator<IterableUtil.ElementWithContext<T>> {

            @Nullable private T                 current, next;
            private final Iterator<? extends T> it = iterable.iterator();

            @Override public boolean
            hasNext() {
                return this.next != null || this.it.hasNext();
            }

            @Override public ElementWithContext<T>
            next() {

                final T previous = this.current;
                if (this.next == null) {
                    T e = this.it.next();
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

                        if (!ElementWithContextIterator.this.it.hasNext()) return null;
                        T e = ElementWithContextIterator.this.it.next();
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
}
