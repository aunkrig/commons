
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.lang.protocol;

import java.util.Collection;

/**
 * Various {@link Runnable}-related utility methods.
 */
public final
class RunnableUtil {

    private
    RunnableUtil() {}

    /** A {@link Runnable} that does simply nothing. */
    public static final Runnable NOP = new Runnable() { @Override public void run() {} };

    /**
     * Runs <var>delegate1</var>, then <var>delegate2</var> in the current thread.
     *
     * @return A runnable that runs <var>delegate1</var> and then <var>delegate2</var>
     *
     * @see RunnableWhichThrows#run()
     */
    public static <EX extends Throwable> RunnableWhichThrows<EX>
    runSequentially(final RunnableWhichThrows<EX> delegate1, final RunnableWhichThrows<EX> delegate2) {
        return new RunnableWhichThrows<EX>() {

            /**
             * @throws EX The exception thrown by <var>delegate1</var> or <var>delegate2</var>; if thrown by
             *            <var>delegate1</var>, then <var>delegate2</var> is not run
             */
            @Override public void
            run() throws EX {
                delegate1.run();
                delegate2.run();
            }
        };
    }

    /**
     * Runs the given <var>delegates</var> in the current thread, in the order of the collection's iterator.
     *
     * @return    A runnable that runs all the <var>delegates</var> in strict sequence
     *
     * @see RunnableWhichThrows#run()
     */
    public static <EX extends Throwable> RunnableWhichThrows<EX>
    runSequentially(final Collection<RunnableWhichThrows<EX>> delegates) {
        return new RunnableWhichThrows<EX>() {

            /**
             * @throws EX The exception thrown by one of the <var>delegates</var>; the following runnables are not run
             */
            @Override public void
            run() throws EX {
                for (RunnableWhichThrows<EX> delegate : delegates) {
                    delegate.run();
                }
            }
        };
    }

    /**
     * Converts a {@link RunnableWhichThrows} into a {@link Runnable}, which is possible iff the source runnable's
     * exception is a subclass of {@link RuntimeException}.
     *
     * @param <EX> The source runnable's exception
     */
    public static <EX extends RuntimeException> Runnable
    asRunnable(final RunnableWhichThrows<EX> delegate) {

        return new Runnable() { @Override public void run() { delegate.run(); } };
    }

    /**
     * Converts a {@link Runnable} into a {@link RunnableWhichThrows}.
     */
    public static <EX extends Throwable> RunnableWhichThrows<EX>
    asRunnableWhichThrows(final Runnable delegate) {

        return new RunnableWhichThrows<EX>() { @Override public void run() { delegate.run(); } };
    }

    /**
     * The returned {@link Runnable} runs the delegate iff the <var>condition</var> evaluates to {@code true}.
     * <p>
     *   (The <var>condition</var> is evaluated with {@code null} as the <var>subject</var> argument.)
     * </p>
     *
     * @param subject The <var>subject</var> for the <var>condition</var>
     */
    public static <ST> Runnable
    sparingRunnable(final Runnable delegate, final Predicate<? super ST> condition, final ST subject) {
        return new Runnable() {

            @Override public void
            run() { if (condition.evaluate(subject)) delegate.run(); }
        };
    }

    /**
     * The returned {@link Runnable} runs the delegate iff the <var>condition</var> produces {@link Boolean#TRUE}.
     *
     * @see PredicateUtil#after(long)
     * @see ProducerUtil#every(long)
     */
    public static Runnable
    sparingRunnable(final Runnable delegate, final Producer<? extends Boolean> condition) {
        return new Runnable() {

            @Override public void
            run() { if (Boolean.TRUE.equals(condition.produce())) delegate.run(); }
        };
    }

    /**
     * Runs <var>runnable1</var> and then <var>runnable2</var>, unless <var>swap</var> is {@code true}, when the
     * running order is swapped (<var>runnable2</var>, then <var>runnable1</var>).
     */
    public static <EX extends Throwable> void
    swapIf(boolean swap, RunnableWhichThrows<EX> runnable1, RunnableWhichThrows<EX> runnable2) throws EX {

        if (swap) {
            runnable2.run();
            runnable1.run();
        } else {
            runnable1.run();
            runnable2.run();
        }
    }

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught and ignored.
     */
    public static <EX extends Throwable> Runnable
    ignoreExceptions(final Class<EX> exceptionClass, final RunnableWhichThrows<EX> delegate) {

        return new Runnable() {

            @Override public void
            run() {

                try {
                    delegate.run();
                } catch (RuntimeException re) {
                    if (!exceptionClass.isAssignableFrom(re.getClass())) throw re;
                    ;
                } catch (Error e) {     // SUPPRESS CHECKSTYLE IllegalCatch
                    if (!exceptionClass.isAssignableFrom(e.getClass())) throw e;
                    ;
                } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                    assert exceptionClass.isAssignableFrom(t.getClass());
                    ;
                }
            }
        };
    }
}
