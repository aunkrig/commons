
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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;

/**
 * Measures real time, cpu time and user time for the current thread.
 */
public
class ThreadBenchmark {
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private static final MessageFormat FORMAT1 = new MessageFormat("Real={0}ms Cpu={1}ns User={2}ns");
    private static final MessageFormat FORMAT2 = new MessageFormat("{0}ms");

    private final long realTime;
    private long       cpuTime;
    private long       userTime;

    public
    ThreadBenchmark() {
        this.realTime = System.currentTimeMillis();
        if (ThreadBenchmark.THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported()) {
            this.cpuTime  = ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadCpuTime();
            this.userTime = ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadUserTime();
        }
    }

    /**
     * @return The real (clock) time in seconds since this object was created
     */
    public double
    getRealTime() {
        return 0.001D * (System.currentTimeMillis() - this.realTime);
    }

    /**
     * Returns the total CPU time for the current thread in seconds since this object was created. If the
     * implementation distinguishes between user mode time and system mode time, the returned CPU time is the amount
     * of time that the current thread has executed in user mode or system mode.
     * <p>
     * The return value is undefined if this object was created by a different thread.
     */
    public double
    getCpuTime() {
        return 0.000000001D * (ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadCpuTime() - this.cpuTime);
    }

    /**
     * Returns the CPU time that the current thread has executed in user mode in seconds.
     * <p>
     * The return value is undefined if this object was created by a different thread.
     */
    public double
    getUserTime() {
        return 0.000000001D * (ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadUserTime() - this.userTime);
    }

    /**
     * @return A nicely formatted string telling the real, cpu and user time since this object was created.
     */
    public String
    getMessage() {
        if (ThreadBenchmark.THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported()) {
            return ThreadBenchmark.FORMAT1.format(new Object[] {
                System.currentTimeMillis() - this.realTime,
                ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadCpuTime() - this.cpuTime,
                ThreadBenchmark.THREAD_MX_BEAN.getCurrentThreadUserTime() - this.userTime
            });
        }

        return ThreadBenchmark.FORMAT2.format(Long.valueOf(System.currentTimeMillis() - this.realTime));
    }
}
