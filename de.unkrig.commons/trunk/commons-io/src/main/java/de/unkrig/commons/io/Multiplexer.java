
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;

/**
 * A thin wrapper for the JDK {@link Selector} that also manages timers and multiple threads.
 */
public
class Multiplexer implements RunnableWhichThrows<IOException>, Stoppable {

    private final SortedMap<Long, List<Runnable>> timers = new TreeMap<Long, List<Runnable>>();
    private volatile boolean                      stopping;

    public
    Multiplexer() throws IOException {
    }

    /**
     * Handles channels and timers; returns never. Must be called by exactly one thread. Will complete normally shortly
     * after {@link #stop()} was called.
     */
    @Override public synchronized void
    run() throws IOException {

        // The multiplexer main loop.
        this.stopping = false;
        while (!this.stopping) {
            long timeout = 0L;

            // Remove and execute one expired timer.
            TIMER: {
                Runnable runnable;
                synchronized (this.timers) {
                    Iterator<Map.Entry<Long, List<Runnable>>> it = this.timers.entrySet().iterator();
                    if (!it.hasNext()) break TIMER; // No timers at all.

                    Entry<Long, List<Runnable>> entry  = it.next();
                    Long                        expiry = entry.getKey();
                    timeout = expiry - System.currentTimeMillis();
                    if (timeout > 0) break TIMER; // No expired timers.

                    List<Runnable> runnables = entry.getValue();
                    runnable = runnables.remove(0);
                    if (runnables.isEmpty()) it.remove();
                }

                runnable.run();
            }

            // Block until one of the channels pops up.
            if (Multiplexer.LOGGER.isLoggable(Level.FINE)) Multiplexer.LOGGER.log(
                Level.FINE,
                "Waiting for {0} key(s)",
                new Object[] { this.selector.keys().size() }
            );
            this.selector.select(Math.min(0L, timeout));

            // Execute the runnable of the first selected key.
            SELECTED_KEY: {
                RunnableWhichThrows<IOException> runnable;
                synchronized (this.selector) {
                    Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
                    if (!it.hasNext()) break SELECTED_KEY;

                    @SuppressWarnings("unchecked") RunnableWhichThrows<IOException> tmp = (
                        (RunnableWhichThrows<IOException>) it.next().attachment()
                    );
                    runnable = tmp;

                    it.remove();
                }

                runnable.run();
            }
        }
    }

    /**
     * Executes the given runnable exactly once iff the channel becomes {@link SelectionKey#OP_ACCEPT acceptable},
     * {@link SelectionKey#OP_CONNECT connected}, {@link SelectionKey#OP_READ readable} and/or {@link
     * SelectionKey#OP_WRITE writable}.
     * <p>
     *   To cancel the reigstration, invoke {@link SelectionKey#cancel()} on the returned object.
     * </p>
     */
    public SelectionKey
    register(SelectableChannel sc, int ops, RunnableWhichThrows<IOException> runnable) throws ClosedChannelException {
        final SelectionKey key = sc.register(this.selector, ops, runnable);
        this.selector.wakeup();
        return key;
    }

    /**
     * An identifier for a created timer.
     */
    public
    interface TimerKey {

        /**
         * Cancels the timer associated with this key.
         */
        void
        cancel();
    }

    /**
     * Registers the given runnable for exactly one execution by this {@link Multiplexer}'s {@link #run()} method
     * when the current time is equal to (or slightly greater than) {@code expiry}.
     * <p>
     *   Registering the same runnable more than once (and even with equal {@code expiry}) will lead to the runnable
     *   being run that many times.
     * </p>
     * <p>
     *   Registering a runnable for an expiry that lies in the past will lead to the runnable being executed by
     *   this {@link Multiplexer}'s {@link #run()} method very soon.
     * </p>
     * <p>
     *   Invoking the {@link TimerKey#cancel()} method on the returned object prevents the runnable from being
     *   executed (iff its execution has not yet begun).
     * </p>
     */
    public TimerKey
    timer(long expiry, final Runnable runnable) {

        // Register the runnable in the timers map.
        final List<Runnable> runnables;
        synchronized (this.timers) {
            runnables = Multiplexer.get2(this.timers, expiry);
            runnables.add(runnable);

            // Wake up the blocking 'run()' method so it recognizes the newly added timer.
            if (this.timers.firstKey() == expiry) {
                this.selector.wakeup();
            }
        }

        return new TimerKey() {

            @Override public void
            cancel() {

                synchronized (Multiplexer.this.timers) {
                    runnables.remove(runnable);
                }
            }
        };
    }

    /**
     * Causes {@link #run()} to complete normally soon; if {@link #run()} is not currently being executed, then
     * calling {@link #stop()} has no effect.
     */
    @Override public void
    stop() {
        this.stopping = true;
    }

    // IMPLEMENTATION

    private static final Logger LOGGER = Logger.getLogger(Multiplexer.class.getName());

    private final Selector selector = Selector.open();

    /**
     * Returns the {@link List} associated with the {@code key}, or, if no {@link List} is associated with the {@code
     * key} yet, creates an {@link ArrayList}, associates it with the {@code key}, and returns it.
     */
    private static <K, LE> List<LE>
    get2(Map<K, List<LE>> map, K key) {
        List<LE> l = map.get(key);
        if (l == null) {
            l = new ArrayList<LE>();
            map.put(key, l);
        }
        return l;
    }
}
