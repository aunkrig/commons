
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

package de.unkrig.commons.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProxyConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Executes multiple tasks asynchronously, but forwards their output to a delegate {@link Consumer} (or {@link
 * ConsumerWhichThrows}) <em>in the order the tasks were submitted</em>, i.e. the last subject written by one task
 * appears <em>before</em> the output of all subsequently submitted tasks.
 *
 * @param <T>  The type of the objects that pose the "output" of the tasks
 * @param <EX> The exception type that the tasks may throw
 */
public
class ObjectSequentializer<T, EX extends Throwable> {

    private final SquadExecutor<Void> squadExecutor;

    /** The consumer to which the next task must send its output. */
    private ConsumerWhichThrows<? super T, ? extends EX> nextTarget;

    /** @see ObjectSequentializer */
    public
    ObjectSequentializer(ConsumerWhichThrows<? super T, ? extends EX> delegate, ExecutorService squadExecutor) {

        // The QuadExecutor will allow us to await the completion of all submitted tasks.
        this.squadExecutor = new SquadExecutor<Void>(squadExecutor);

        // The first task will write directly to the delegate.
        this.nextTarget = delegate;
    }

    /**
     * Subjects written to the <var>task</var>'s subject will be written to the {@link
     * #ObjectSequentializer(ConsumerWhichThrows, ExecutorService) delegate} <em>after</em> all subjects written by the
     * previously submitted tasks, and <em>before</em> the subjects of all tasks submitted afterwards.
     * <p>
     *   Notice that when a task submits another task (the "subtask"), then the output of the subtask will appear
     *   <em>after the output of the task</em> (and the output of all other tasks submitted in the meantime).
     *   If you want to have the output of subtasks to be <em>in the middle of the task's output</em>, then you should
     *   create a second {@link ObjectSequentializer} and await its completion.
     * </p>
     */
    public synchronized void
    submit(final ConsumerWhichThrows<? super ConsumerWhichThrows<? super T, ? extends EX>, ? extends Exception> task) {

        final ConsumerWhichThrows<? super T, ? extends EX> previousTarget = this.nextTarget;

        final List<T> buffer = new ArrayList<T>();

        final ProxyConsumerWhichThrows<T, EX>
        pc = new ProxyConsumerWhichThrows<T, EX>(ConsumerUtil.addToCollection(buffer), 0);

        // The next task(s) will write to the proxy consumer.
        this.nextTarget = pc;

        this.squadExecutor.submit(new Callable<Void>() {

            @Nullable @Override public Void
            call() throws Exception {

                // Run the task.
                task.consume(previousTarget);

                synchronized (pc) {

                    // Copy the buffered products of the FOLLOWING tasks to the output.
                    for (T t : buffer) {
                        try {
                            previousTarget.consume(t);
                        } catch (Exception e) {
                            throw e;
                        } catch (Error e) {      // SUPPRESS CHECKSTYLE IllegalCatch
                            throw e;
                        } catch (Throwable th) { // SUPPRESS CHECKSTYLE IllegalCatch
                            throw new Exception(th);
                        }
                    }

                    // Redirect FUTURE output of the FOLLOWING tasks directly to MY output.
                    pc.setDelegate(previousTarget);
                }

                return null;
            }
        });
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(ConsumerWhichThrows)} have completed.
     *
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    public void
    awaitCompletion() throws InterruptedException, ExecutionException, CancellationException {

        this.squadExecutor.awaitCompletion();
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(Consumer)
     * Object)}, {@link #submit(Runnable, Object)} and {@link #execute(Runnable)} have completed, or when then
     * <var>timeout</var> expires.
     *
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    void
    awaitCompletion(long timeout, TimeUnit unit)
    throws CancellationException, ExecutionException, InterruptedException {

        this.squadExecutor.awaitCompletion(timeout, unit);
    }
}
