
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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.NotNull;

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
    identity() {
        return TransformerUtil.IDENTITY;
    }

    @SuppressWarnings("rawtypes") private static final Transformer IDENTITY = new Transformer() {

        @Override public Object
        transform(Object in) { return in; }
    };

    /**
     * Converts a {@link Transformer} into a {@link TransformerWhichThrows}.
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
    asTransformerWhichThrows(final Transformer<? super I, ? extends O> source) {

        return new TransformerWhichThrows<I, O, EX>() {
            @Override @NotNull public O transform(I in) { return source.transform(in); }
        };
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
            @Override @NotNull public O transform(I in) { return source.transform(in); }
        };
    }

    /**
     * @return A {@link Transformer} which calls the {@code delegate}, except when the {@code subject} equals the
     *         {@code extraInput}, when it returns {@code extraOutput}
     */
    public static <I, O> Transformer<I, O>
    combine(final I extraInput, final O extraOutput, final Transformer<? super I, O> delegate) {
        return new Transformer<I, O>() {

            @Override @NotNull public O
            transform(I in) {
                if (in.equals(extraInput)) return extraOutput;
                return delegate.transform(in);
            }
        };
    }

    /**
     * @return A {@link TransformerWhichThrows} which calls the {@code delegate}, except when the {@code subject}
     *         equals the {@code extraInput}, when it returns {@code extraOutput}
     */
    public static <I, O, EX extends Throwable> TransformerWhichThrows<I, O, EX>
    combine(final I extraInput, final O extraOutput, final TransformerWhichThrows<? super I, O, EX> delegate) {
        return new TransformerWhichThrows<I, O, EX>() {

            @Override @NotNull public O
            transform(I in) throws EX {
                if (in.equals(extraInput)) return extraOutput;
                return delegate.transform(in);
            }
        };
    }

    /**
     * @return A {@link Transformer} in which the given keys and values override the mappings of the delegate.
     */
    @SuppressWarnings("unchecked") public static <I, O> Transformer<I, O>
    addMappings(final Transformer<? super I, O> transformer, Object... keysAndValues) {

        final Map<I, O> m = new HashMap<I, O>();

        for (int i = 0; i < keysAndValues.length;) {
            I key   = (I) keysAndValues[i++];
            assert key != null;

            O value = (O) keysAndValues[i++];
            assert value != null;

            m.put(key, value);
        }

        return new Transformer<I, O>() {

            @Override @NotNull public O
            transform(I in) {
                O out = m.get(in);
                if (out != null) return out;
                return transformer.transform(in);
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

            @Override @NotNull public O
            transform(I in) { return m.get(in); }
        };
    }

    /**
     * A transformer which lets a <var>delegate</var> transform the inputs, but at most once for each non-equal
     * input.
     */
    public static <I, O> Transformer<I, O>
    cache(final Transformer<? super I, O> delegate) {

        return new Transformer<I, O>() {

            final WeakHashMap<I, O> cache = new WeakHashMap<I, O>();

            @Override @NotNull public O
            transform(I in) {

                O out = this.cache.get(in);
                if (out != null) return out;

                out = delegate.transform(in);
                this.cache.put(in, out);

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
