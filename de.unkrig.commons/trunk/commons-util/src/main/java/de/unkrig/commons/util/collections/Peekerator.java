
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Extends the {@link Iterator} with the capability to "peek" an element.
 *
 * @param <E>   Der ELement-Typ
 * @see #peek()
 */
@NotNullByDefault(false) public
interface Peekerator<E> extends Iterator<E> {

    /**
     * Consumes and returns the next element in this iteration.
     *
     * @return                        The next element in this iteration
     * @throws NoSuchElementException This iteration has no more elements
     */
    @Override E next();

    /**
     * Just like {@link #next()}, but does <em>not</em> consume the next element.
     * I.e. the next invocation of {@link #peek()} of {@link #next()} will return the <em>same</em> element that this
     * invocation returns.
     *
     * @return                        The next element in this iteration
     * @throws NoSuchElementException This iteration has no more elements
     */
    E peek();

    /**
     * Removes the last element returned by {@link #next()} or {@link #peek()} from the underlying collection.
     *
     * @throws UnsupportedOperationException This operation is not supported by this peekerator, probably because
     *                                       this peekerator is not backed by a collection
     * @throws IllegalStateException         The {@link #next()} or {@link #peek()} methods have not yet been called
     * @throws IllegalStateException         The {@link #remove()} method has already been called after the last call
     *                                       to {@link #next()} or {@link #peek()}
     */
    @Override void remove();
}
