
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

package de.unkrig.commons.util.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Utility methods related to "{@code java.util.concurrent}".
 */
public final
class ConcurrentUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private ConcurrentUtil() {}

    /**
     * An {@link ExecutorService} which executes callables in the same thread immediately.
     */
    public static final ExecutorService SEQUENTIAL_EXECUTOR_SERVICE = new AbstractExecutorService() {

        @NotNullByDefault(false) @Override public void
        execute(Runnable command) { command.run(); }

        @Override public List<Runnable>
        shutdownNow() { throw new UnsupportedOperationException("shutdownNow"); }

        @Override public void
        shutdown() { throw new UnsupportedOperationException("shutdown"); }

        @Override public boolean
        isTerminated() { return false; }

        @Override public boolean
        isShutdown() { return false; }

        @NotNullByDefault(false) @Override public boolean
        awaitTermination(long timeout, TimeUnit unit) { return true; }
    };

    /**
     * Creates and returns a {@link Runnable} which, when run, acts as follows:
     * <dl>
     *   <dt>First ... (<var>n</var> - 1)th run:</dt>
     *   <dd>
     *     Does nothing
     *   </dd>
     *   <dt><var>n</var>th run:</dt>
     *   <dd>
     *     Runs the <var>delegate</var>
     *   </dd>
     *   <dt>(<var>n</var> + 1)th and all following runs:</dt>
     *   <dd>
     *     Throws an {@link IllegalStateException}
     *   </dd>
     * </dl>
     * The returned {@link Runnable} can be run from any thread.
     */
    public static Runnable
    count(final int n, final Runnable delegate) {

        return new Runnable() {

            AtomicInteger ai = new AtomicInteger(n);

            @Override public void
            run() {
                int i = this.ai.decrementAndGet();
                if (i < 0) throw new IllegalStateException();
                if (i == 0) delegate.run();
            }
        };
    }

    /**
     * Creates and returns a {@link Consumer} which, when invoked, acts as follows:
     * <dl>
     *   <dt>First ... (<var>n</var> - 1)th invocation:</dt>
     *   <dd>
     *     The <var>subject</var> is collected (internally)
     *   </dd>
     *   <dt><var>n</var>th invocation:</dt>
     *   <dd>
     *     Invokes the <var>delegate</var> with the previously collected subjects, in the same order as the
     *     <var>n</var> invocations.
     *   </dd>
     *   <dt>(<var>n</var> + 1)th and all following invocations:</dt>
     *   <dd>
     *     Throws an {@link IllegalStateException}
     *   </dd>
     * </dl>
     * The returned {@link Consumer} can be invoked from any thread.
     */
    public static <T> Consumer<T>
    aggregate(final int n, final Consumer<? super List<T>> delegate) {

        return new Consumer<T>() {

            final List<T> collectedSubjects = new ArrayList<T>();

            @Override public void
            consume(@NotNull T subject) {

                int size;
                synchronized (this.collectedSubjects) {
                    size = this.collectedSubjects.size();
                    if (size >= n) throw new IllegalStateException();
                    this.collectedSubjects.add(subject);
                    size++;
                }
                if (size == n) {
                    delegate.consume(Collections.unmodifiableList(this.collectedSubjects));
                }
            }
        };
    }

    /**
     * Creates and returns a list of <var>n</var> consumers; when each of these has been invoked exactly once,
     * then the <var>delegate</var> is invoked with a list of the consumed subjects, in the same order as the
     * consumers.
     * <p>
     *   When one of the returned consumers is invoked more than once, then it throws an {@link IllegalStateException}.
     * </p>
     */
    public static <T> List<Consumer<T>>
    collect(final int n, final Consumer<? super List<T>> delegate) {

        final List<T> collectedSubjects = new ArrayList<T>(n);

        final AtomicBoolean[] invoked = new AtomicBoolean[n];
        for (int i = 0; i < n; i++) invoked[i] = new AtomicBoolean(false);

        final AtomicInteger remaining = new AtomicInteger(n);

        List<Consumer<T>> result = new ArrayList<Consumer<T>>(n);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            result.set(i, new Consumer<T>() {

                @Override public void
                consume(@NotNull T subject) {
                    if (!invoked[idx].compareAndSet(false, true)) throw new IllegalStateException();
                    collectedSubjects.set(idx, subject);
                    int r = remaining.decrementAndGet();
                    assert r >= 0;
                    if (r == 0) delegate.consume(collectedSubjects);
                }
            });
        }

        return result;
    }
}
