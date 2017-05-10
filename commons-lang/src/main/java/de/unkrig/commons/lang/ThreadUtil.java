
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

package de.unkrig.commons.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.HardReference;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@code java.lang.Thread}-related utility methods.
 */
public final
class ThreadUtil {

    private static final Logger LOGGER = Logger.getLogger(ThreadUtil.class.getName());

    private
    ThreadUtil() {}

    /**
     * Execute the given <var>runnable</var> in a background thread
     *
     * @return The background runnable can be interrupted through this object
     */
    public static Stoppable
    runInBackground(Runnable runnable, @Nullable String threadName) {
        final Thread t = new Thread(runnable, threadName == null ? runnable.toString() : threadName);
        t.setDaemon(true);

        if (ThreadUtil.LOGGER.isLoggable(Level.FINE)) ThreadUtil.LOGGER.log(
            Level.FINE,
            "Starting daemon thread {0} / ''{1}''",
            new Object[] { t.getId(), t.getName() }
        );

        t.start();

        return new Stoppable() {

            @Override public void
            stop() {
                if (!t.isAlive()) {
                    if (ThreadUtil.LOGGER.isLoggable(Level.FINE)) {
                        ThreadUtil.LOGGER.fine("Background runnable '" + t.getName() + "' is not running");
                    }
                    return;
                }

                if (ThreadUtil.LOGGER.isLoggable(Level.FINE)) {
                    ThreadUtil.LOGGER.fine("Stopping background runnable '" + t.getName() + "'");
                }

                t.interrupt();
                try {
                    t.join();
                } catch (InterruptedException ie) {
                    return;
                }

                if (ThreadUtil.LOGGER.isLoggable(Level.FINE)) {
                    ThreadUtil.LOGGER.fine("Background runnable '" + t.getName() + "' stopped");
                }
            }
        };
    }

    /**
     * @see #runInBackground(Runnable, String)
     */
    public static <EX extends Throwable> Stoppable
    runInBackground(final RunnableWhichThrows<EX> runnable, @Nullable String threadName) {
        return ThreadUtil.runInBackground(new Runnable() {

            @Override public void
            run() {
                try {
                    runnable.run();
                } catch (Throwable e) { // SUPPRESS CHECKSTYLE IllegalCatch
                    ThreadUtil.LOGGER.log(Level.INFO, "Exception caught from runnable", e);
                }
            }
        }, threadName);
    }

    /**
     * Executes the given <var>runnable</var>.
     */
    public static <EX extends Throwable> void
    runInForeground(RunnableWhichThrows<EX> runnable) throws EX {
        runnable.run();
    }

    /**
     * Executes the two <var>runnable</var>s in parallel before it returns.
     */
    public static <EX extends Throwable> void
    parallel(
        RunnableWhichThrows<EX> runnable1,
        RunnableWhichThrows<EX> runnable2,
        Stoppable               stoppable
    ) throws EX {
        List<RunnableWhichThrows<EX>> l = new ArrayList<RunnableWhichThrows<EX>>();
        l.add(runnable1);
        l.add(runnable2);
        ThreadUtil.parallel(l, Collections.<Stoppable>singleton(stoppable));
    }

    /**
     * @see #parallel(Runnable[], Iterable)
     */
    public static <EX extends Throwable> void
    parallel(Iterable<? extends RunnableWhichThrows<EX>> runnables) throws EX {
        ThreadUtil.parallel(runnables, Collections.<Stoppable>emptyList());
    }

    /**
     * @see #parallel(Runnable[], Iterable)
     */
    public static <EX extends Throwable> void
    parallel(Iterable<? extends RunnableWhichThrows<EX>> runnables, Iterable<Stoppable> stoppables) throws EX {

        final HardReference<EX> caughtException = new HardReference<EX>();

        List<Runnable> l = new ArrayList<Runnable>();
        for (final RunnableWhichThrows<EX> runnable : runnables) {
            l.add(new Runnable() {

                @Override public void
                run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Error er) {     // SUPPRESS CHECKSTYLE IllegalCatch
                        throw er;
                    } catch (Throwable th) { // SUPPRESS CHECKSTYLE IllegalCatch

                        // "th" can only be an EX, because that is the only checked exception that the delegate
                        // runnables will throw.
                        @SuppressWarnings("unchecked") EX ex = (EX) th;
                        caughtException.set(ex);
                    }
                }
            });
        }

        ThreadUtil.parallel(l.toArray(new Runnable[l.size()]), stoppables);

        EX ex = caughtException.get();
        if (ex != null) throw ex;
    }

    /**
     * Invokes the {@link Runnable#run run} method of all <var>runnables</var> in parallel threads (including the
     * current thread). When the first of these invocations returns, then all threads are {@link Thread#interrupt()
     * interrupt}ed (which awakes them from blocking I/O), and also on all <var>stoppables</var> {@link
     * Stoppable#stop()} is called. When all the threads have been {@link Thread#join() join}ed, this method returns.
     */
    public static void
    parallel(Runnable[] runnables, final Iterable<Stoppable> stoppables) {

        final HardReference<RuntimeException> caughtRuntimeException = new HardReference<RuntimeException>();

        // Run all runnables.
        final Thread[] threads = new Thread[runnables.length - 1];
        for (int i = 0; i < runnables.length; i++) {
            final Runnable runnable  = runnables[i];
            Runnable       runnable2 = new Runnable() {

                @Override public void
                run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException re) {
                        caughtRuntimeException.set(re);
                    }
                    for (Thread thread : threads) {
                        if (thread != null) thread.interrupt();
                    }
                    for (Stoppable stoppable : stoppables) {
                        if (stoppable != null) stoppable.stop();
                    }
                }
            };

            Thread t;
            if (i < runnables.length - 1) {
                t = new Thread(runnable2);
                t.setName(Thread.currentThread().getName());
                t.start();
                threads[i] = t;
            } else {
                runnable2.run();
            }
        }

        // Join pending threads.
        for (Thread thread : threads) {
            for (;;) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    ;
                }
            }
        }

        RuntimeException re = caughtRuntimeException.get();
        if (re != null) throw re;
    }

    /**
     * Runs all but the last of <var>runnables</var> in the background, and the last of <var>runnables</var> in the
     * foreground.
     */
    public static <R extends RunnableWhichThrows<EX>, EX extends Throwable> void
    runInForeground(Iterable<R> runnables) throws EX {
        Iterator<R>     it         = runnables.iterator();
        List<Stoppable> stoppables = new ArrayList<Stoppable>();
        for (;;) {
            RunnableWhichThrows<EX> r = it.next();
            if (!it.hasNext()) {
                ThreadUtil.runInForeground(r);
                break;
            }
            stoppables.add(ThreadUtil.runInBackground(r, null));
        }
        for (Stoppable stoppable : stoppables) stoppable.stop();
    }

    /**
     * Produces daemon threads; handy for "{@code new} {@link ScheduledThreadPoolExecutor}{@code (... , ThreadFactory,
     * ...)}", because with the default {@link ThreadFactory} the JVM won't terminate when it shuts down orderly (i.e.
     * "{@code main()}" returns or "{@code System.exit()}" is invoked).
     */
    public static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {

        final ThreadFactory delegate = Executors.defaultThreadFactory();

        @NotNullByDefault(false) @Override public Thread
        newThread(Runnable r) {
            Thread result = this.delegate.newThread(r);
            result.setName("Daemon-" + result.getName());
            result.setDaemon(true);
            return result;
        }
    };
}
