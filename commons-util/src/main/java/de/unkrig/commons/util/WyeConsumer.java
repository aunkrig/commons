
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

package de.unkrig.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;

/**
 * Collects the subjects produced by several producers and forwards them to one consumer in the right order. This is
 * useful for parallelized tasks from which the results must be collected and be brought into the original order.
 *
 * @param <T>  The type of the subjects
 * @param <EX> The exception that the producers may throw
 * @see #run()
 */
public
class WyeConsumer<T, EX extends Throwable> implements RunnableWhichThrows<EX> {

    private final ConsumerWhichThrows<? super T, EX> target;
    private final List<Producer<? extends T>>        producers = new ArrayList<Producer<? extends T>>();

    /**
     * @see #run()
     */
    public
    WyeConsumer(ConsumerWhichThrows<? super T, EX> target) {
        this.target = target;
    }

    /**
     * @see #run()
     */
    public Consumer<T>
    newConsumer(int capacity) {

        final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<T>(capacity);

        this.producers.add(new Producer<T>() {

            @Override public T
            produce() {
                for (;;) {
                    try {
                        return queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        return new Consumer<T>() {

            @Override public void
            consume(T subject) { queue.add(subject); }
        };
    }

    /**
     * Copies the subjects that are sent to the {@link Consumer} that was returned by the first call to {@link
     * #newConsumer(int)} up to, but not including, the first {@code null} reference, to the <var>target</var>
     * consumer, then the objects that are sent to the {@link Consumer} that was returned by the second call to {@link
     * #newConsumer(int)}, and so forth.
     */
    @Override public void
    run() throws EX {

        for (Producer<? extends T> producer : this.producers) {

            for (;;) {
                T subject = producer.produce();
                if (subject == null) break;
                this.target.consume(subject);
            }
        }
    }
}
