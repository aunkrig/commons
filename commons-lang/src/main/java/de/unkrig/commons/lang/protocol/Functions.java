
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

package de.unkrig.commons.lang.protocol;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Function}- and {@link FunctionWhichThrows}-related utility methods.
 */
public final
class Functions {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private
    Functions() {}

    /**
     * @return A {@link Function} that transforms any object reference to itself.
     */
    @SuppressWarnings("unchecked") public static <O, I extends O> Function<I, O>
    identity() {
        return Functions.IDENTITY;
    }

    @SuppressWarnings("rawtypes") private static final Function IDENTITY = new Function() {

        @Override @Nullable public Object
        call(@Nullable Object argument) { return argument; }
    };

    /**
     * Converts a {@link Function} into a {@link FunctionWhichThrows}.
     * <p>
     *   That is possible iff:
     * </p>
     * <ul>
     *   <li>The target's input type is a subclass of the source's input type, and
     *   <li>The source's output type is a subclass of the target's output type.
     * </ul>
     *
     * @param <I>  The functions' input type
     * @param <O>  The functions' output type
     * @param <EX> The target function's exception
     */
    public static <I, O, EX extends Throwable> FunctionWhichThrows<I, O, EX>
    asFunctionWhichThrows(final Function<? super I, ? extends O> source) {

        return new FunctionWhichThrows<I, O, EX>() {
            @Override @Nullable public O call(@Nullable I argument) { return source.call(argument); }
        };
    }

    /**
     * Converts a {@link FunctionWhichThrows} into a {@link Function}.
     * <p>
     *   That is possible iff:
     * </p>
     * <ul>
     *   <li>The target's input type is a subclass of the source's input type, and
     *   <li>The source's output type is a subclass of the target's output type
     *   <li>The source's exception is a subclass of {@link RuntimeException}.
     * </ul>
     *
     * @param <I>  The functions' input type
     * @param <O>  The functions' output type
     * @param <EX> The source function's exception
     */
    public static <I, O, EX extends RuntimeException> Function<I, O>
    asFunction(final FunctionWhichThrows<? super I, ? extends O, EX> source) {

        return new Function<I, O>() {
            @Override @Nullable public O call(@Nullable I argument) { return source.call(argument); }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught, ignored, and the
     * <var>defaultValue</var> is returned.
     */
    public static <I, O, EX extends Throwable> Function<I, O>
    ignoreExceptions(
        final Class<EX>                     exceptionClass,
        final FunctionWhichThrows<I, O, EX> delegate,
        @Nullable final O                   defaultValue
    ) {

        return new Function<I, O>() {

            @Override @Nullable public O
            call(@Nullable I argument) {

                try {
                    return delegate.call(argument);
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
