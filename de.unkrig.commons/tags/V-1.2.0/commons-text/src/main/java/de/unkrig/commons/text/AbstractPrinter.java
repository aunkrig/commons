
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

package de.unkrig.commons.text;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import de.unkrig.commons.nullanalysis.Nullable;

/** A basic implementation of the {@link Printer} interface. */
public abstract
class AbstractPrinter implements Printer {

    @Override public boolean isWarnEnabled()    { return true; }
    @Override public boolean isInfoEnabled()    { return true; }
    @Override public boolean isVerboseEnabled() { return true; }
    @Override public boolean isDebugEnabled()   { return true; }

    @Override public void
    error(@Nullable String message, @Nullable Throwable t) {

        if (t != null) {
            StringWriter sw = new StringWriter();
            {
                PrintWriter pw = new PrintWriter(sw);
                if (message != null) {
                    pw.print(message);
                    pw.print(": ");
                }
                t.printStackTrace(pw);
                pw.flush();
            }
            message = sw.toString().trim();
        }

        this.error(message);
    }

    @Override public void
    error(String pattern, @Nullable Throwable t, Object... arguments) {
        this.error(AbstractPrinter.format(pattern, arguments), t);
    }

    @Override public void
    error(String pattern, Object... arguments) {
        this.error(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    warn(String pattern, Object... arguments) {
        if (this.isWarnEnabled()) this.warn(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    info(String pattern, Object... arguments) {
        if (this.isInfoEnabled()) this.info(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    verbose(String pattern, Object... arguments) {
        if (this.isVerboseEnabled()) this.verbose(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    debug(String pattern, Object... arguments) {
        if (this.isDebugEnabled()) this.debug(AbstractPrinter.format(pattern, arguments));
    }

    private static String
    format(String pattern, Object[] arguments) {

        // MessageFormat is known for throwing all kinds of exceptions if the arguments are not "good". We want to be
        // robust for these cases, so we catch these exception and do some minimal formatting.
        try {
            return MessageFormat.format(pattern, arguments);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(pattern);
            if (arguments.length > 0) {
                sb.append(": ").append(arguments[0]);
                for (int i = 1; i < arguments.length; i++) {
                    sb.append(", ").append(arguments[i]);
                }
            }
            return sb.toString();
        }
    }
}
