
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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Producer}-related utility methods.
 */
public final
class ProducerUtil {

    private
    ProducerUtil() {}

    /**
     * The returned {@link Producer} calls the {@code delegate} iff the {@code condition} returns {@code true},
     * otherwise it returns the previous product of the {@code delegate}, or {@code null} iff the delegate has
     * not yet been called.
     *
     * @param subject The {@code subject} argument for the {@code condition}
     * @deprecated    Use {@link #cache(ProducerWhichThrows, ProducerWhichThrows)} instead, which has very similar (but
     *                not identical) semantics.
     */
    @Deprecated public static <T, ST> Producer<T>
    sparingProducer(final Producer<? extends T> delegate, final Predicate<? super ST> condition, final ST subject) {

        return new Producer<T>() {

            @Nullable private T product;

            @Override @Nullable public T
            produce() {
                if (condition.evaluate(subject)) this.product = delegate.produce();
                return this.product;
            }
        };
    }

    /**
     * Returns a {@code Producer<Boolean>} who's first evaluation result is {@code true}, and each following result is
     * {@code true} iff the last {@code true} result was returned at least the given {@code interval} milliseconds ago.
     * In other words, the interval between two returned {@code true} values is never shorter than {@code interval}
     * milliseconds.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static Producer<Boolean>
    every(final long interval) {

        return new Producer<Boolean>() {

            private long expirationTime;

            @Override @Nullable public synchronized Boolean
            produce() {
                long now = System.currentTimeMillis();
                if (now >= this.expirationTime) {
                    this.expirationTime = now + interval;
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    /**
     * Converts the <var>source</var> into a {@link ProducerWhichThrows ProducerWhichThrows&lt;T, EX>}.
     * <p>
     *   This is always possible, because the <var>source</var> is only allowed to throw {@link RuntimeException}s.
     * </p>
     * <p>
     *   Notice {@code Producer<T>} <em>extends</em> {@code ProducerWhichThrows<T, RuntimeException>}, thus you don't
     *   need this method to convert to {@code ProducerWhichThrows<T, RuntimeException>}.
     * </p>
     *
     * @param <T>  The product type
     * @param <EX> The target producer's exception
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    asProducerWhichThrows(final Producer<? extends T> source) {

        @SuppressWarnings("unchecked") ProducerWhichThrows<T, EX> result = (ProducerWhichThrows<T, EX>) source;

        return result;
    }

    /**
     * Converts the <var>source</var> into a {@link Producer Producer&lt;T>}.
     * <p>
     *   This is always possible, because both are only allowed to throw {@link RuntimeException}s.
     * </p>
     *
     * @param <T> The product type
     */
    public static <T> Producer<T>
    asProducer(final ProducerWhichThrows<? extends T, ? extends RuntimeException> source) {

        @SuppressWarnings("unchecked") Producer<T> result = (Producer<T>) source;

        return result;
    }

    /**
     * Creates and returns a {@link Producer} that produced the given {@code elements}.
     *
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    fromElements(final T... elements) {

        return new Producer<T>() {

            int idx;

            @Override @Nullable public T
            produce() {
                return this.idx == elements.length ? null : elements[this.idx++];
            }
        };
    }

    /**
     * @deprecated Use "{@code fromIterator(}<var>delegate</var>{@code .iterator(), true)}" instead
     */
    @Deprecated public static <T> Producer<T>
    fromCollection(Collection<T> delegate) { return ProducerUtil.fromIterator(delegate.iterator(), true); }

    /**
     * @deprecated Use "{@code fromIterator(}<var>delegate</var>{@code .iterator(), true)}" instead
     */
    @Deprecated public static <T> Producer<T>
    fromIterable(Iterable<T> delegate, boolean remove) {
        return ProducerUtil.fromIterator(delegate.iterator(), remove);
    }

    /**
     * Produces the elements of the <var>delegate</var> array, in ascending index order, and after that an infinite
     * sequence of {@code null}s.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> FromArrayProducer<T>
    fromArray(final T[] delegate) {

        return ProducerUtil.fromArray(delegate, 0, delegate.length);
    }

    /**
     * Produces the elements <var>from</var> ... <var>to</var>-1 of the {@code delegate} array, and after that an
     * infinite sequence of {@code null}s.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     *
     * @throws IllegalArgumentException <var>from</var> is less than 0
     * @throws IllegalArgumentException <var>to</var> is less than <var>from</var>
     * @throws IllegalArgumentException <var>to</var> is greater than <var>delegate</var>{@code .length}
     */
    public static <T> FromArrayProducer<T>
    fromArray(final T[] delegate, final int from, final int to) {

        if (from < 0 || to < from || to > delegate.length) throw new IllegalArgumentException();

        return new FromArrayProducer<T>() {

            int idx = from;

            @Override @Nullable public T
            produce() { return this.idx < to ? delegate[this.idx++] : null; }

            @Override public int
            index() { return this.idx; }
        };
    }

    /**
     * Extends the concept of the {@link Producer} by an "index".
     *
     * @param <T> See {@link Producer}
     * @see       #index()
     */
    public
    interface FromArrayProducer<T> extends Producer<T> {

        /**
         * @return The index of the next element that will be returned by {@link #produce()};
         *         <var>delegate</var>{@code .length} on end-of-array
         */
        int index();
    }

    /**
     * Produces the products of the {@code iterator}, or {@code null} iff the {@code iterator} has no more elements.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    fromIterator(final Iterator<T> delegate) { return ProducerUtil.fromIterator(delegate, false); }

    /**
     * Produces the products of the {@code iterator}, or {@code null} iff the {@code iterator} has no more elements.
     * <p>
     *   If <var>remove</var> is {@code true}, then products are removed from the underlying collections as the
     *   elements are iterated.
     * </p>
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    fromIterator(final Iterator<T> delegate, final boolean remove) {

        return new Producer<T>() {

            @Override @Nullable public T
            produce() {

                if (!delegate.hasNext()) return null;

                T product = delegate.next();
                if (remove) delegate.remove();
                return product;
            }
        };
    }

    /**
     * Produces objects based on the number of preceding invocations, i.e. the {@code indexTransformer} is invoked
     * with subjects '0', '1', '2', ...
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    fromIndexTransformer(final Transformer<? super Integer, T> indexTransformer) {

        return new Producer<T>() {

            private int index;

            @Override @Nullable public T
            produce() { return indexTransformer.transform(this.index++); }
        };
    }

    /**
     * Produces objects based on the number of preceding invocations, i.e. the {@code indexTransformer} is invoked
     * with subjects '0', '1', '2', ...
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    fromIndexTransformer(final TransformerWhichThrows<? super Integer, T, EX> indexTransformer) {

        return new ProducerWhichThrows<T, EX>() {

            private int index;

            @Override @Nullable public T
            produce() throws EX { return indexTransformer.transform(this.index++); }
        };
    }

    /**
     * Transforms each product of the <var>delegate</var> through the <var>transformer</var>.
     * <p>
     *   {@code null} products are <em>not</em> transformed and give a {@code null} product.
     * </p>
     */
    public static <T1, T2, EX extends Throwable> ProducerWhichThrows<T2, EX>
    transform(
        final ProducerWhichThrows<? extends T1, ? extends EX>      delegate,
        final TransformerWhichThrows<? super T1, ? extends T2, EX> transformer
    ) {

        return new ProducerWhichThrows<T2, EX>() {

            @Override @Nullable public T2
            produce() throws EX {
                T1 product = delegate.produce();
                return product == null ? null : transformer.transform(product);
            }
        };
    }

    /**
     * Transforms each product of the <var>delegate</var> through the <var>function</var>.
     * <p>
     *   {@code null} products are <em>not</em> transformed and give a {@code null} product.
     * </p>
     */
    public static <T1, T2, EX extends Throwable> ProducerWhichThrows<T2, EX>
    transform(
        final ProducerWhichThrows<? extends T1, ? extends EX>   delegate,
        final FunctionWhichThrows<? super T1, ? extends T2, EX> function
    ) {

        return new ProducerWhichThrows<T2, EX>() {

            @Override @Nullable public T2
            produce() throws EX { return function.call(delegate.produce()); }
        };
    }

    /**
     * @return A producer which produces bytes through {@code new java.util.Random(seed).nextInt(0x100)}
     */
    public static Producer<Byte>
    randomByteProducer(final long seed) {

        return new Producer<Byte>() {

            final Random r = new Random(seed);

            @Override public Byte
            produce() { return (byte) this.r.nextInt(0x100); }
        };
    }

    /**
     * @return A producer which always produces the <var>constant</var>
     */
    public static <T> Producer<T>
    constantProducer(final T constant) {

        return new Producer<T>() {
            @Override public T produce() { return constant; }
        };
    }

    /**
     * Discards the elements that the <var>delegate</var> produces while they are <var>compressable</var>. After that,
     * it produces the elements that the <var>delegate</var> produces and are <i>not</i> <var>compressable</var>, and
     * reduces sequences of one or more <var>compressable</var> elements to <var>compressed</var>.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    compress(final Producer<? extends T> delegate, final Predicate<? super T> compressable, final T compressed) {

        return new Producer<T>() {

            boolean     initial = true;
            @Nullable T lookahead;

            @Override @Nullable public T
            produce() {

                if (this.initial) {
                    T product;
                    do {
                        product = delegate.produce();
                        if (product == null) return null;
                    } while (compressable.evaluate(product));
                    this.initial = false;
                    return product;
                }

                {
                    T tmp = this.lookahead;
                    if (tmp != null) {
                        this.lookahead = null;
                        return tmp;
                    }
                }

                T product = delegate.produce();
                if (product == null) return null;
                if (!compressable.evaluate(product)) return product;

                do {
                    product = delegate.produce();
                    if (product == null) return null;
                } while (compressable.evaluate(product));
                this.lookahead = product;
                return compressed;
            }
        };
    }

    /**
     * Creates and returns a {@link Producer} that produces <var>first</var>, <var>second</var>, <var>first</var>,
     * <var>second</var>, ...
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    alternate(final T first, final T second) {

        return new Producer<T>() {

            boolean toggle;

            @Override @Nullable public T
            produce() {
                this.toggle = !this.toggle;
                return this.toggle ? first : second;
            }
        };
    }

    /**
     * Creates and returns a {@link Producer} that produces 0, 1, 2, 3, ...
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static Producer<Integer>
    increasing() {

        return new Producer<Integer>() {

            int value;

            @Override @Nullable public Integer
            produce() { return this.value++; }
        };
    }

    /**
     * Creates and returns a producer that produces the products of <var>delegate1</var>, and, when that produces
     * {@code null}, the products of <var>delegate2</var>.
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T> Producer<T>
    concat(final Producer<? extends T> delegate1, final Producer<? extends T> delegate2) {

        return new Producer<T>() {

            boolean second;

            @Override @Nullable public T
            produce() {
                if (this.second) return delegate2.produce();
                T product = delegate1.produce();
                if (product != null) return product;
                this.second = true;
                return delegate2.produce();
            }
        };
    }

    /**
     * The first product is the first product of the <var>delegate</var>; each following product is the next product
     * of the <var>delegate</var> if the <var>invalidationCondition</var> evaluates to {@code true}, otherwise it is
     * the <em>previous</em> product.
     * <p>
     *   Example:
     * </p>
     * <pre>ProducerUtil.cache(delegate, ProducerUtil.atMostEvery(milliseconds, false, true))</pre>
     * <p>
     *   caches the products of the <var>delegate</var> for <var>milliseconds</var>' time.
     * </p>
     * <p>
     *   The returned producer is not synchronized and therefore not thread-safe; to get a thread-safe producer, use
     *   {@link ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    cache(
        final ProducerWhichThrows<T, ? extends EX>       delegate,
        final ProducerWhichThrows<Boolean, ? extends EX> invalidationCondition
    ) {

        return new ProducerWhichThrows<T, EX>() {

            @Nullable T cache;
            boolean     isCached;

            @Override @Nullable public T
            produce() throws EX {

                if (this.isCached && !Boolean.TRUE.equals(invalidationCondition.produce())) return this.cache;

                T tmp = delegate.produce();
                this.isCached = true;
                this.cache    = tmp;
                return tmp;
            }
        };
    }

    /**
     * The first product is the <var>firstProduct</var>; each following product is the next product of the
     * <var>delegate</var> iff the <var>invalidationCondition</var> evaluates to {@code true}, otherwise it is the
     * <em>previous</em> product.
     * <p>
     *   The returned {@link PredicateWhichThrows} is not synchronized and therefore not thread-safe.
     * </p>
     *
     * @see #cache(ProducerWhichThrows, ProducerWhichThrows)
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    cache(
        @Nullable final T                                firstProduct,
        final ProducerWhichThrows<T, ? extends EX>       delegate,
        final ProducerWhichThrows<Boolean, ? extends EX> invalidationCondition
    ) {

        return new ProducerWhichThrows<T, EX>() {

            @Nullable T cache = firstProduct;

            @Override @Nullable public T
            produce() throws EX {

                Boolean b = invalidationCondition.produce();
                if (b != null && b) return this.cache;

                return (this.cache = delegate.produce());
            }
        };
    }

    /**
     * Creates and returns a producer which caches the products of a delegate producer <em>asynchronously</em>.
     * <p>
     *   Iff <var>prefetch</var> is {@code true}, then this method uses the <var>delegate</var> immediately to start
     *   the "background fetch".
     * </p>
     * <p>
     *   On the first call to {@link ProducerWhichThrows#produce() produce()} of the returned producer:
     * </p>
     * <ul>
     *   <li>
     *     Iff the "background fetch" is not pending: Uses the <var>delegate</var> to start it.
     *   </li>
     *   <li>Waits for the "background fetch" to complete (synchronously!).</li>
     *   <li>Remembers its result as "the cached value" and returns it.</li>
     * </ul>
     * <p>
     *   On all <em>consecutive</em> calls to {@link ProducerWhichThrows#produce() produce()} of the returned producer:
     * </p>
     * <ul>
     *   <li>
     *     <p>Iff the "background fetch" is <em>not</em> pending:</p>
     *     <ul>
     *       <li>Evaluates the <var>invalidationCondition</var>:</li>
     *       <ul>
     *         <li>
     *           <p>Iff {@code false}:</p>
     *           <ul>
     *             <li>Returns the "cached value".</li>
     *           </ul>
     *         </li>
     *         <li>
     *           <p>Iff {@code true}</p>
     *           <ul>
     *             <li>Uses the <var>delegate</var> to start the "background fetch".</li>
     *             <li><em>Still</em> returns the "cached value".</li>
     *           </ul>
     *         </li>
     *       </ul>
     *     </ul>
     *   </li>
     *   <li>
     *     <p>Iff the "background fetch" <em>is</em> pending:</p>
     *     <ul>
     *       <li>
     *         <p>Iff it has not yet completed:</p>
     *         <ul>
     *           <li>Returns "the cached value".</li>
     *         </ul>
     *       </li>
     *       <li>
     *         <p>Otherwise, iff the "background fetch" <em>has</em> completed:</p>
     *         <ul>
     *           <li>Retrieves its result (now the "background fetch" is no longer pending).</li>
     *           <li>Remembers the result as "the cached value".</li>
     *           <li>Returns it.</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     *   Notice that the <var>invalidationCondition</var> is never evaluated while the "background fetch" is pending.
     * </p>
     * <p>
     *   The returned producer is not thread-safe; to get a thread-safe asynchronous cache, use {@link
     *   ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     * <p>
     *   {@link #atMostEvery(long, boolean, boolean) atMostEvery(milliseconds, false, false} may be a good choice for
     *   the <var>invalidationCondition</var> to set up an expiration-time-based cache.
     * </p>
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    cacheAsynchronously(
        final ProducerWhichThrows<Future<T>, ? extends EX> delegate,
        final ProducerWhichThrows<Boolean, ? extends EX>   invalidationCondition,
        final boolean                                      prefetch
    ) throws EX {

        final Future<T> f = prefetch ? delegate.produce() : null;

        return new ProducerWhichThrows<T, EX>() {

            @Nullable T         cache;
            boolean             isCached; // FALSE before the first call, TRUE afterwards.
            @Nullable Future<T> future = f;

            @Override @Nullable public T
            produce() throws EX {

                try {
                    Future<T> f = this.future;

                    if (!this.isCached) {
                        // This is the first invocation.

                        if (f == null) {

                            // Prefetching was not configured, so create the FUTURE for the first product NOW.
                            f = delegate.produce();
                            assert f != null;
                            this.future = f;
                        }

                        // Wait synchronously until the FUTURE has computed its result.
                        T result = f.get();

                        // Cache and return the result;
                        this.cache    = result;
                        this.isCached = true;
                        this.future   = null;
                        return result;
                    }

                    // This is NOT the first invocation.

                    if (f != null) {
                        // A background refetch is currently pending.

                        if (f.isDone()) {

                            // The background refetch has just completed! Fetch, cache and return its result.
                            this.future = null;
                            this.cache  = f.get();
                        }

                        return this.cache;
                    }

                    // Check the "invalidation condition".
                    if (Boolean.TRUE.equals(invalidationCondition.produce())) {

                        // Start the "background refresh".
                        f = delegate.produce();
                        assert f != null;
                        this.future = f;
                    }

                    return this.cache;
                } catch (InterruptedException ie) {
                    throw new IllegalStateException(ie);
                } catch (ExecutionException ee) {
                    throw new IllegalStateException(ee);
                }
            }
        };
    }

    /**
     * Creates and returns a producer for which the <em>first</em> product is {@code true}, and, for all following
     * products, the time interval between adjacent {@code true} products will (A) be minimal and (B) never
     * shorter than <var>milliseconds</var>.
     * <p>
     *   Calling this method is equivalent to calling
     * </p>
     * <pre>
     *     ProducerUtil.atMostEvery(<var>milliseconds</var>, true, true)
     * </pre>
     * <p>
     *   The returned producer is not thread-safe; to get a thread-safe producer, use {@link
     *   ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     *
     * @see #atMostEvery(long, boolean, boolean)
     */
    public static Producer<Boolean>
    atMostEvery(final long milliseconds) { return ProducerUtil.atMostEvery(milliseconds, true, true); }

    /**
     * Creates and returns a producer which first produces <var>firstProduct</var>, and afterwards products that are
     * {@code true} iff they are produced <var>milliseconds</var> or more after the most recent {@code true} product,
     * or, iff {@code !}<var>startAtTrueProduct</var>, after the first {@code false} product after the most recent
     * {@code true} product.
     * <p>
     *   The returned producer is not thread-safe; to get a thread-safe producer, use {@link
     *   ProducerUtil#synchronizedProducer(ProducerWhichThrows)}.
     * </p>
     */
    public static Producer<Boolean>
    atMostEvery(final long milliseconds, final boolean firstProduct, final boolean startAtTrueProduct) {

        return new Producer<Boolean>() {

            long next = Long.MIN_VALUE;

            @Override @Nullable public Boolean
            produce() {

                long now = System.currentTimeMillis();

                if (now < this.next) {

                    // Not yet expired.
                    if (this.next == Long.MAX_VALUE) {
                        this.next = now + milliseconds;
                    }
                    return false;
                }

                if (this.next == Long.MIN_VALUE && !firstProduct) {

                    // Let the expiration interval begin NOW.
                    this.next = now + milliseconds;
                    return false;
                }

                if (startAtTrueProduct) {

                    // Let the expiration interval begin NOW.
                    this.next = now + milliseconds;
                } else {

                    // Let the expiration interval begin LATER, when the first "FALSE" product is produced.
                    this.next = Long.MAX_VALUE;
                }
                return true;
            }

            @Override public String
            toString() {
                return (
                    "At most every "
                    + milliseconds
                    + " ms; next expiration at "
                    + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
                );
            }
        };
    }

    /**
     * Returns a "{@link Producer}{@code <Object>}" that produces {@code true} iff the current time is after the
     * given <var>expirationTime</var> (in milliseconds).
     *
     * @param expirationTime Milliseconds since Jan 1 1970, UTC
     */
    public static Producer<Boolean>
    after(final long expirationTime) {

        return new Producer<Boolean>() {

            @Override @Nullable public Boolean
            produce() { return System.currentTimeMillis() > expirationTime; }

            @Override public String
            toString() { return "After " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US); }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught, ignored, and the
     * <var>defaultValue</var> is returned.
     */
    public static <T, EX extends Throwable> Producer<T>
    ignoreExceptions(final Class<EX> exceptionClass, final ProducerWhichThrows<T, EX> delegate, final T defaultValue) {

        return new Producer<T>() {

            @Override @Nullable public T
            produce() {

                try {
                    return delegate.produce();
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
     * Creates and returns a proxy to the <var>delegate</var> with a synchronized {@link ProducerWhichThrows#produce()
     * produce()} method.
     *
     * @see ProducerWhichThrows
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    synchronizedProducer(final ProducerWhichThrows<? extends T, EX> delegate) {
        return new ProducerWhichThrows<T, EX>() {

            @Override @Nullable public synchronized T
            produce() throws EX { return delegate.produce(); }
        };
    }

    /**
     * @return              An endless sequence of {@link Boolean}s, where every one-out-of-<var>n</var> is {@code
     *                      true}
     * @param initialFalses How many {@code false} products appear before the first {@code true} product
     */
    public static Producer<Boolean>
    oneOutOf(final int n, final int initialFalses) {

        return new Producer<Boolean>() {

            final AtomicInteger counter = new AtomicInteger(-initialFalses);

            @Override @Nullable public Boolean
            produce() { return this.getAndIncrement(this.counter, n) == 0; }

            private int
            getAndIncrement(AtomicInteger atomicInteger, int modulo) {
                for (;;) {
                    int current = atomicInteger.get();

                    int next = current + 1;
                    if (next >= modulo) next %= modulo;

                    if (atomicInteger.compareAndSet(current, next)) return current;
                }
            }
        };
    }
}