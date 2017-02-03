
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

package de.unkrig.commons.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link Executor} which executes tasks through a delegate {@link ExecutorService}. The {@link #awaitCompletion()}
 * methods waits until all tasks of the squad are complete.
 *
 * @param <T> The futures' result type
 */
@NotNullByDefault(false) public
class SquadExecutor<T> implements Executor {

    private final ExecutorService  delegate;
    private final Queue<Future<T>> futures = new ConcurrentLinkedQueue<Future<T>>();

    /** @see SquadExecutor */
    public
    SquadExecutor(ExecutorService delegate) { this.delegate = delegate; }

    @Override public void
    execute(Runnable command) { this.submit(command); }

    /**
     * Submits a {@link Runnable} task for execution.
     *
     * @param task The task to submit
     * @return     A future representing pending completion of the task
     */
    public Future<T>
    submit(Runnable task) {
        if (task == null) throw new NullPointerException();

        FutureTask<T> ftask = new FutureTask<T>(task, null);
        this.futures.add(ftask);
        this.delegate.execute(ftask);
        return ftask;
    }

    /**
     * Submits a Runnable task for execution.
     *
     * @param task   The task to submit
     * @param result The result to return
     * @return       A future representing pending completion of the task
     */
    public Future<T>
    submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();

        FutureTask<T> ftask = new FutureTask<T>(task, result);
        this.futures.add(ftask);
        this.delegate.execute(ftask);
        return ftask;
    }

    /**
     * Submits a value-returning task for execution.
     *
     * @param task The task to submit
     * @return     A future representing pending completion of the task
     */
    public Future<T>
    submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        FutureTask<T> ftask = new FutureTask<T>(task);
        this.futures.add(ftask);
        this.delegate.execute(ftask);
        return ftask;
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(Callable)}, {@link #submit(Runnable,
     * Object)}, {@link #submit(Runnable, Object)} and {@link #execute(Runnable)} have completed.
     *
     * @return                       The futures of the tasks
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    public List<Future<T>>
    awaitCompletion() throws InterruptedException, ExecutionException, CancellationException {

        List<Future<T>> result = new ArrayList<Future<T>>();
        for (Future<T> f = this.futures.poll(); f != null; f = this.futures.poll()) {
            f.get();
            result.add(f);
        }

        return result;
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(Callable)}, {@link #submit(Runnable,
     * Object)}, {@link #submit(Runnable, Object)} and {@link #execute(Runnable)} have completed, or when then
     * <var>timeout</var> expires.
     *
     * @return                       The futures of the tasks
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    public List<Future<T>>
    awaitCompletion(long timeout, TimeUnit unit)
    throws CancellationException, ExecutionException, InterruptedException {
        if (unit == null) throw new NullPointerException();

        long            nanos    = unit.toNanos(timeout);
        List<Future<T>> result   = new ArrayList<Future<T>>();
        long            lastTime = System.nanoTime(); // SUPPRESS CHECKSTYLE UsageDistance

        for (Future<T> f = this.futures.poll(); f != null; f = this.futures.poll()) {

            if (nanos <= 0) return result;

            try {
                f.get(nanos, TimeUnit.NANOSECONDS);
            } catch (TimeoutException toe) {
                return result;
            }
            result.add(f);

            long now = System.nanoTime();
            nanos    -= now - lastTime;
            lastTime = now;
        }

        return result;
    }
}
