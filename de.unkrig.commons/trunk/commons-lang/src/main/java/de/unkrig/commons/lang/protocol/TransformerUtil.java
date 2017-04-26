
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * Various {@link Transformer}-related utility methods.
 */
public final
class TransformerUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private
    TransformerUtil() {}

    /**
     * @return A {@link Transformer} that transforms any object reference to itself.
     */
    @SuppressWarnings("unchecked") public static <O, I extends O> Transformer<I, O>
    identity() { return TransformerUtil.IDENTITY; }

    @SuppressWarnings("rawtypes") private static final Transformer
    IDENTITY = new Transformer() { @Override public Object transform(Object in) { return in; } };

    /**
     * @return A transformer that feeds subjects through a chain of delegate transformers
     */
    public static <T, EX extends Throwable> TransformerWhichThrows<? super T, ? extends T, ? extends EX>
    chain(final TransformerWhichThrows<? super T, ? extends T, ? extends EX>... transformers) {

        if (transformers.length == 0) return TransformerUtil.asTransformerWhichThrows(TransformerUtil.<T, T>identity());

        if (transformers.length == 1) return transformers[0];

        return new TransformerWhichThrows<T, T, EX>() {

            @Override public T
            transform(T in) throws EX {
                for (TransformerWhichThrows<? super T, ? extends T, ? extends EX> transformer : transformers) {
                    in = transformer.transform(in);
                }
                return in;
            }
        };
    }

    /**
     * Converts a {@link TransformerWhichThrows}{@code <I, O, ? extends RuntimeException>} into a {@link
     * TransformerWhichThrows}{@code <I, O, EX>}.
     * <p>
     *   That is possible iff:
     * </p>
     * <ul>
     *   <li>The target's input type is a subclass of the source's input type, and
     *   <li>The source's output type is a subclass of the target's output type.
     * </ul>
     *
     * @param <I>  The transformers' input type
     * @param <O>  The transformers' output type
     * @param <EX> The target transformer's exception
     */
    public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    asTransformerWhichThrows(final TransformerWhichThrows<? super I, ? extends O, ? extends RuntimeException> source) {

        @SuppressWarnings("unchecked") TransformerWhichThrows<I, O, EX>
        result = (TransformerWhichThrows<I, O, EX>) source;

        return result;
    }

    /**
     * Converts a {@link TransformerWhichThrows} into a {@link Transformer}.
     * <p>
     *   That is possible iff:
     * </p>
     * <ul>
     *   <li>The target's input type is a subclass of the source's input type, and
     *   <li>The source's output type is a subclass of the target's output type
     *   <li>The source's exception is a subclass of {@link RuntimeException}.
     * </ul>
     *
     * @param <I>  The transformers' input type
     * @param <O>  The transformers' output type
     * @param <EX> The source transformer's exception
     */
    public static <I, O, EX extends RuntimeException> Transformer<I, O>
    asTransformer(final TransformerWhichThrows<? super I, ? extends O, EX> source) {

        return new Transformer<I, O>() {
            @Override public O transform(I in) { return source.transform(in); }
        };
    }

    /**
     * @return A {@link TransformerWhichThrows} which calls the <var>delegate</var>, except when the <var>subject</var>
     *         equals the <var>extraInput</var>, when it returns <var>extraOutput</var>
     */
    public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    combine(
        final I                                                  extraInput,
        final O                                                  extraOutput,
        final TransformerWhichThrows<? super I, ? extends O, EX> delegate
    ) {
        return new TransformerWhichThrows<I, O, EX>() {

            @Override public O
            transform(I in) throws EX {
                if (in.equals(extraInput)) return extraOutput;
                return delegate.transform(in);
            }
        };
    }

    /**
     * @return A {@link TransformerWhichThrows} in which the given keys and values override the mappings of the
     *         <var>delegate</var>
     */
    @SuppressWarnings("unchecked") public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    addMappings(
        final TransformerWhichThrows<? super I, ? extends O, ? extends EX> delegate,
        Object...                                                          keysAndValues
    ) {

        final Map<I, O> m = new HashMap<I, O>();

        for (int i = 0; i < keysAndValues.length;) {
            I key   = (I) keysAndValues[i++];
            assert key != null;

            O value = (O) keysAndValues[i++];
            assert value != null;

            m.put(key, value);
        }

        return new TransformerWhichThrows<I, O, EX>() {

            @Override public O
            transform(I in) throws EX {
                O out = m.get(in);
                if (out != null) return out;
                return delegate.transform(in);
            }
        };
    }

    /**
     * @return A {@link Transformer} in which the given keys and values override the mappings of the delegate.
     */
    @SuppressWarnings("unchecked") public static <I, O> Transformer<I, O>
    fromMappings(Object... keysAndValues) {

        final Map<I, O> m = new HashMap<I, O>();

        for (int i = 0; i < keysAndValues.length;) {
            m.put((I) keysAndValues[i++], (O) keysAndValues[i++]);
        }

        return new Transformer<I, O>() {

            @Override public O
            transform(I in) { return m.get(in); }
        };
    }

    /**
     * A transformer which lets a <var>delegate</var> transform the inputs, but at most once for each non-equal
     * input.
     * <p>
     *   This method is not thread-safe. To get a thread-safe cache, use {@link #cache(TransformerWhichThrows, Map)}.
     * </p>
     */
    public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    cache(final TransformerWhichThrows<? super I, ? extends O, ? extends EX> delegate) {

        return TransformerUtil.cache(delegate, new WeakHashMap<I, O>());
    }

    /**
     * A transformer which lets a <var>delegate</var> transform the inputs, and remembers the result in the
     * <var>cache</var> map.
     * <p>
     *   This method is not thread-safe. To make it thread-safe, pass a {@link Collections#synchronizedMap(Map)} as
     *   the <var>cache</var> argument.
     * </p>
     *
     * @param cache Typically a {@link HashMap}, or a {@link TreeMap#TreeMap(java.util.Comparator)}, or a
     *              {@link WeakHashMap}, or a {@link Collections#synchronizedMap(Map)} (for thread-safety)
     */
    public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    cache(final TransformerWhichThrows<? super I, ? extends O, ? extends EX> delegate, final Map<I, O> cache) {

        return new TransformerWhichThrows<I, O, EX>() {

            @Override public O
            transform(I in) throws EX {

                O out = cache.get(in);
                if (out != null) return out;

                out = delegate.transform(in);
                cache.put(in, out);

                return out;
            }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught, ignored, and the
     * <var>defaultValue</var> is returned.
     */
    public static <I, O, EX extends Throwable> Transformer<I, O>
    ignoreExceptions(
        final Class<EX>                        exceptionClass,
        final TransformerWhichThrows<I, O, EX> delegate,
        final O                                defaultValue
    ) {

        return new Transformer<I, O>() {

            @Override public O
            transform(I in) {

                try {
                    return delegate.transform(in);
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
}
