
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

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Like {@link Producer}, but the {@link #produce()} method is permitted to throw a given exception.
 *
 * <h3>IMPORTANT NOTICE:</h3>
 * <p>
 *   When using this type in a variable, parameter or field declaration, <b>never</b> write:
 * </p>
 * <pre>ProducerWhichThrows&lt;<i>product-type</i>, <i>thrown-exception</i>></pre>
 * <p>
 *   , but always:
 * </p>
 * <pre>ProducerWhichThrows&lt;? extends <i>product-type</i>, ? extends <i>thrown-exception</i>></pre>
 *
 * @param <T>  The type of the products
 * @param <EX> The throwable type that {@link #produce()} may throw; use {@link NoException} to indicate that {@link
 *             #produce()} does not declare any checked exceptions
 */
@NotNullByDefault(false) public
interface ProducerWhichThrows<T, EX extends Throwable> {

    /**
     * Produces the next instance of type {@code T}, a so-called 'product'. A {@code null} return value typically, but
     * not necessarily indicates 'end-of-input'.
     * <p>
     *   Generally products should either be immutable, or the 'ownership' of the product should pass from the producer
     *   to the caller. Particularly, the producer should not return one (mutable) instance more than once.
     * </p>
     */
    @Nullable T
    produce() throws EX;

    /**
     * Returns a human-readable text which describes the origin of the previously produced element, e.g. file name,
     * line number and column number
     */
    @Override @Nullable String
    toString();
}
