
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

package de.unkrig.commons.util.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link AbstractExecutorService} which {@link #execute(Runnable) execute}s runnables immediately in the calling
 * thread.
 */
@NotNullByDefault(false) public
class LinearExecutorService extends AbstractExecutorService {

    private volatile int runState;

    /** Normal, not-shutdown mode */
    static final int RUNNING    = 0;
    /** Controlled shutdown mode */
    static final int SHUTDOWN   = 1;
    /** Immediate shutdown mode */
    static final int STOP       = 2;
    /** Final state */
    static final int TERMINATED = 3;

    @Override public void
    shutdown() {
        this.runState = LinearExecutorService.TERMINATED;
    }

    @Override public List<Runnable>
    shutdownNow() {
        this.runState = LinearExecutorService.TERMINATED;
        return Collections.emptyList();
    }

    @Override public boolean
    isShutdown() {
        return this.runState != LinearExecutorService.RUNNING;
    }

    @Override public boolean
    isTerminated() {
        return this.runState == LinearExecutorService.TERMINATED;
    }

    @Override public boolean
    awaitTermination(long timeout, TimeUnit unit) {
        return this.runState == LinearExecutorService.TERMINATED;
    }

    @Override public void
    execute(Runnable command) {
        if (this.runState != LinearExecutorService.RUNNING) throw new RejectedExecutionException();
        command.run();
    }
}
