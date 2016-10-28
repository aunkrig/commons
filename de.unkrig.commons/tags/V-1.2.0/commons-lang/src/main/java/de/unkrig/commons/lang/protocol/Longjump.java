
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

package de.unkrig.commons.lang.protocol;

import java.io.PrintStream;
import java.io.PrintWriter;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Indicates that an operation has completed abnormally, but this condition has already been handled, e.g. by
 * displaying an error message. Code that catches the {@link Longjump} should <i>not</i> print, process or log
 * it; instead, it should 'continue with the next element' or take a similar action. Consequently, has neither
 * a {@code message} nor a {@code cause}.
 */
public
class Longjump extends Throwable {

    private static final long  serialVersionUID = 1L;

    /**
     * Override {@link Throwable#fillInStackTrace()} with a NOP operation; this throwing and catching fast as
     * lightning.
     */
    @Override public Throwable
    fillInStackTrace() { return this; }

    @Override public String
    getMessage() { throw new UnsupportedOperationException("getMessage"); }

    @Override public String
    getLocalizedMessage() { throw new UnsupportedOperationException("getLocalizedMessage"); }

    @Override public Throwable
    getCause() { throw new UnsupportedOperationException("getCause"); }

    @Override public Throwable
    initCause(@Nullable Throwable cause) { throw new UnsupportedOperationException("initCause"); }

    @Override public String
    toString() { return "Longjump"; }

    @Override public void
    printStackTrace(@Nullable PrintStream s) { throw new UnsupportedOperationException("printStackTrace"); }

    @Override public void
    printStackTrace(@Nullable PrintWriter s) { throw new UnsupportedOperationException("printStackTrace"); }

    @Override public StackTraceElement[]
    getStackTrace() { throw new UnsupportedOperationException("getStackTrace"); }

    /**
     * @return <var>delegate</var>{@code .produce()}, or the <var>defaultValue</var> iff <var>delegate</var>{@code
     *         .produce()} throws a {@link Longjump}
     */
    @Nullable public static <T> T
    catchLongjump(ProducerWhichThrows<T, Longjump> delegate, @Nullable T defaultValue) {
        try {
            return delegate.produce();
        } catch (Longjump l) {
            return defaultValue;
        }
    }

    /**
     * Calls <var>delegate</var>{@code .consume(}<var>subject</var>{@code )}, and catches and ignores any {@link
     * Longjump} it throws.
     */
    public static <T> void
    catchLongjump(ConsumerWhichThrows<T, Longjump> delegate, T subject) {
        try {
            delegate.consume(subject);
        } catch (Longjump l) {}
    }

    /**
     * Calls <var>delegate</var>{@code .run()}, and catches and ignores any {@link Longjump} it throws.
     */
    public static void
    catchLongjump(RunnableWhichThrows<Longjump> delegate) {
        try {
            delegate.run();
        } catch (Longjump l) {}
    }
}
