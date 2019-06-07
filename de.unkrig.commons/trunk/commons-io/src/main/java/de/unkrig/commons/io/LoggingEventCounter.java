
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2019, Arno Unkrig
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

package de.unkrig.commons.io;

import java.util.Formatter;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Counts named (and optionally parameterized) events, and logs the current state.
 */
public
class LoggingEventCounter extends EventCounter {

    private final String name;
    private final Logger logger;
    private final Level  level;

    private final long start = System.currentTimeMillis();

    public
    LoggingEventCounter(String name, Logger logger, Level level) {
        this.name   = name;
        this.logger = logger;
        this.level  = level;
    }

    /**
     * Counts each event and invokes {@link #log()}.
     */
    @Override public void
    countEvent(String eventName, @Nullable Object arg) {

        // Short circuit if/while the events are not loggable. Particularly, events are <em>not</em> counted, which
        // may or may not be what you'd expect.
        if (!this.logger.isLoggable(this.level)) return;

        super.countEvent(eventName, arg);

        this.log();
    }

    /**
     * Logs the current state in a compact, human-readable format to the configured logger.
     */
    protected void
    log() {

        long interval = System.currentTimeMillis() - this.start;

        Formatter f = new Formatter();
        f.format("%s statistics:", this.name);
        for (Entry<String, AtomicLong> e : this.sortedEvents) {
            String eventName = e.getKey();
            long   arg       = e.getValue().get();

            f.format(" %s=%,d(%,d/sec)", eventName, arg, arg / interval);
        }

        this.logger.log(this.level, f.toString());
    }
}
