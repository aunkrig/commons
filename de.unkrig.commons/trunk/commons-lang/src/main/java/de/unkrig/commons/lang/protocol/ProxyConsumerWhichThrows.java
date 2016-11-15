
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

import de.unkrig.commons.nullanalysis.NotNull;

/**
 * A consumer which redirects the subjects it consumes to a delegate consumer.
 *
 * @param <T>  See {@link ConsumerWhichThrows}
 * @param <EX> See {@link ConsumerWhichThrows}
 */
public
class ProxyConsumerWhichThrows<T, EX extends Throwable> implements ConsumerWhichThrows<T, EX> {

    private ConsumerWhichThrows<? super T, ? extends EX> delegate;

    public
    ProxyConsumerWhichThrows(ConsumerWhichThrows<? super T, ? extends EX> delegate) {
        this.delegate = delegate;
    }

    public
    ProxyConsumerWhichThrows(ConsumerWhichThrows<? super T, ? extends RuntimeException> delegate, int unused) {
        this.delegate = ConsumerUtil.widen2(delegate);
    }

    @Override public void
    consume(@NotNull T subject) throws EX { this.delegate.consume(subject); }

    /**
     * Changes the delegate that was previously set through {@link #ProxyConsumerWhichThrows(ConsumerWhichThrows)} or
     * {@link #setDelegate(ConsumerWhichThrows)}.
     */
    public void
    setDelegate(ConsumerWhichThrows<? super T, ? extends EX> delegate) {
        this.delegate = delegate;
    }
}
