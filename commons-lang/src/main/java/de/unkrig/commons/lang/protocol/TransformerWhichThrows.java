
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

/**
 * Like {@link Transformer}, but the {@link #transform(Object)} method is permitted to throw a given exception.
 *
 * <h3>IMPORTANT NOTICE:</h3>
 * When using this type in a variable, parameter or field declaration, <b>never</b> write
 * <pre>TransformerWhichThrows&lt;
 *    <i>input-type</i>,
 *    <i>output-type</i>,
 *    <i>thrown-exception</i>
 *></pre>
 * , but always
 * <pre>TransformerWhichThrows&lt;
 *    ? super <i>input-type</i>,
 *    ? extends <i>output-type</i>,
 *    ? extends <i>thrown-exception</i>
 *></pre>
 * .
 *
 * @param <I>  The type of the consumed subject
 * @param <O>  The type of the product
 * @param <EX> The throwable type that {@link #transform(Object)} may throw
 * @see #transform(Object)
 */
public
interface TransformerWhichThrows<I, O, EX extends Throwable> {

    /**
     * Transforms a (non-null) object of type {@code I} into a (non-null) object of type {@code O}.
     */
    O
    transform(I in) throws EX;
}
