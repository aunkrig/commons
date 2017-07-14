
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
import java.util.ListIterator;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility functionality related to {@link Peekerator}s.
 */
public final
class Peekerators  {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Peekerators() {}

    /**
     * Wraps a given {@link Iterator} as a {@link Peekerator}. That peekerator implements the {@link Peekerator#peek()}
     * operation by reading ahead (at most) one element from the underlying iterator.
     */
    public static <E> Peekerator<E>
    from(final Iterator<E> delegate) {

        return new Peekerator<E>() {

            boolean     readAhead;
            @Nullable E buffer;    // Valid iff readAhead==true

            @Override public boolean
            hasNext() { return this.readAhead || delegate.hasNext(); }

            @Override @Nullable public E
            next() {
                if (this.readAhead) {
                    this.readAhead = false;
                    return this.buffer;
                }
                return delegate.next();
            }

            @Override public void
            remove() { delegate.remove(); }

            @Override @Nullable public E
            peek() {

                if (this.readAhead) return this.buffer;

                this.buffer    = delegate.next();
                this.readAhead = true;
                return this.buffer;
            }
        };
    }

    /**
     * Wraps a given {@link ListIterator} as a {@link Peekerator}. That peekerator implements the {@link
     * Peekerator#peek()} operation by calling {@link ListIterator#next()} and immediately {@link
     * ListIterator#previous()}.
     * <p>
     *   This implementation may perform slightly better than {@link #from(Iterator)}.
     * </p>
     */
    public static <E> Peekerator<E>
    from(final ListIterator<E> delegate) {

        return new Peekerator<E>() {

            @Override public boolean
            hasNext() { return delegate.hasNext(); }

            @Override @Nullable public E
            next() { return delegate.next(); }

            @Override public void
            remove() { delegate.remove(); }

            @Override @Nullable public E
            peek() {
                E result = delegate.next();
                delegate.previous();
                return result;
            }
        };
    }
}
