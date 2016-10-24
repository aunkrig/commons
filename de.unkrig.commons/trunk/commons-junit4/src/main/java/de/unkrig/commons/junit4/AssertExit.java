
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

package de.unkrig.commons.junit4;

import java.security.Permission;

import org.junit.Assert;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to JUNIT and {@link System#exit(int)}.
 */
public final
class AssertExit {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private AssertExit() {}

    /**
     * Asserts that the given <var>runnable</var> invokes {@link System#exit(int)}, and that the exit status is
     * <var>expected</var>.
     *
     * @see System#exit(int)
     */
    public static <EX extends Exception> void
    assertExitStatusEqual(int expected, RunnableWhichThrows<EX> runnable) throws EX {
        assertExitStatusEqual(null, expected, runnable);
    }

    /**
     * Asserts that the given <var>runnable</var> invokes {@link System#exit(int)}, and that the exit status is
     * <em>not</em> <var>expected</var>.
     */
    public static <EX extends Exception> void
    assertExitStatusNotEqual(int notExpected, RunnableWhichThrows<EX> runnable) throws EX {
        assertExitStatusNotEqual(null, notExpected, runnable);
    }

    /**
     * Asserts that the given <var>runnable</var> invokes {@link System#exit(int)}, and that the exit status is
     * <var>expected</var>.
     *
     * @see System#exit(int)
     */
    public static <EX extends Exception> void
    assertExitStatusEqual(@Nullable String message, int expected, RunnableWhichThrows<EX> runnable) throws EX {
        assertExit(message, PredicateUtil.equal(expected), runnable);
    }

    /**
     * Asserts that the given <var>runnable</var> invokes {@link System#exit(int)}, and that the exit status is
     * <em>not</em> <var>expected</var>.
     */
    public static <EX extends Exception> void
    assertExitStatusNotEqual(@Nullable String message, int notExpected, RunnableWhichThrows<EX> runnable) throws EX {
        assertExit(message, PredicateUtil.notEqual(notExpected), runnable);
    }

    /**
     * Asserts that the given <var>runnable</var> invokes {@link System#exit(int)}, and that <var>expected</var>
     * evaluates to {@code true} for the exit status.
     *
     * @see System#exit(int)
     */
    public static <EX extends Exception> void
    assertExit(@Nullable String message, Predicate<Integer> expected, RunnableWhichThrows<EX> runnable) throws EX {

        class ExitError extends Error {

            private static final long serialVersionUID = 1L;
            private final int         status;

            ExitError(int status) { this.status = status; }

            public int getStatus() { return this.status; }
        }

        @NotNullByDefault(false)
        class NoExitSecurityManager extends SecurityManager {

            @Override public void checkExit(int status)            { throw new ExitError(status); }
            @Override public void checkPermission(Permission perm) {}
        }

        SecurityManager oldSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        try {
            runnable.run();
            String m = "Did not exit";
            Assert.fail(message == null ? m : message + ": " + m);
        } catch (ExitError ee) {
            Assert.assertTrue(message, expected.evaluate(ee.getStatus()));
        } finally {
            System.setSecurityManager(oldSecurityManager);
        }
    }
}
