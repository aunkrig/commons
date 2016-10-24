
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

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An entity that transforms an "input value" into "a output value".
 *
 * <h3>IMPORTANT NOTICE:</h3>
 * <p>
 *   When using this type in a variable, parameter or field declaration, <b>never</b> write
 * </p>
 * <pre>FunctionEhichThrows&lt;
 *    <i>input-type</i>,
 *    <i>output-type</i>,
 *    <i>thrown-exception</i>
 *></pre>
 * <p>
 *   , but always
 * </p>
 * <pre>FunctionEhichThrows&lt;
 *    ? super <i>input-type</i>,
 *    ? extends <i>output-type</i>,
 *    ? extends <i>thrown-exception</i>
 *></pre>
 * <p>
 *   .
 * </p>
 *
 * @param <I>  The type of the parameter of {@link #call(Object)}
 * @param <O>  The return type of {@link #call(Object)}
 * @param <EX> The exception type that {@link #call(Object)} is allowed to throw
 * @see        #call(Object)
 */
public
interface FunctionWhichThrows<I, O, EX extends Throwable> {

    /**
     * Calculates a value of type <var>O</var> from an <var>argument</var> of type <var>I</var>.
     */
    @Nullable O
    call(@Nullable I argument) throws EX;
}
