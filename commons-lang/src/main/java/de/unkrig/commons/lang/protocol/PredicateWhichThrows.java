
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

/**
 * An entity that tests whether a subject of type {@code T} fulfils a particular condition.
 *
 * <h3>IMPORTANT NOTICE:</h3>
 * <p>
 *   When using this type in a variable, parameter or field declaration, <b>never</b> write
 * </p>
 * <pre>PredicateWhichThrows&lt;<i>subject-type</i>, <i>thrown-exception</i>></pre>
 * <p>
 *   , but always
 * </p>
 * <pre>PredicateWhichThrows&lt;? super <i>subject-type</i>, ? extends <i>thrown-exception</i>></pre>
 * <p>
 *   .
 * </p>
 *
 * @param <T>  The type of the evaluated subject
 * @param <EX> The throwable type that {@link #evaluate(Object)} may throw
 */
public
interface PredicateWhichThrows<T, EX extends Throwable> {

    /**
     * @return Whether the {@code subject} fulfils a particular condition
     */
    boolean
    evaluate(T subject) throws EX;
}
