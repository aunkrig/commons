
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

package de.unkrig.commons.lang.security;

import java.security.Permission;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @see #catchExit(RunnableWhichThrows)
 */
public final
class ExitCatcher {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private ExitCatcher() {}

    private static final
    class ExitTrappedException extends SecurityException {

        private static final long serialVersionUID = 1L;

        private final int status;

        ExitTrappedException(int status) { this.status = status; }

        int
        getStatus() { return this.status; }
    }

    /**
     * Runs the <var>runnable</var> and catches its call to {@link System#exit(int)}.
     *
     * @return The "status" argument of the {@link System#exit(int)} call, or {@code null} iff the runnable completes
     *         normally
     */
    @Nullable public static <EX extends Throwable> Integer
    catchExit(RunnableWhichThrows<EX> runnable) throws EX {

        final SecurityManager originalSecurityManager = System.getSecurityManager();
        try {

            System.setSecurityManager(new SecurityManager() {

                @Override public void
                checkPermission(@Nullable Permission permission) {}

                @Override public void
                checkExit(int status) { throw new ExitTrappedException(status); }
            });
            try {

                runnable.run();
                return null;
            } catch (ExitTrappedException ete) {
                return ete.getStatus();
            }
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
