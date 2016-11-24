
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

package de.unkrig.commons.text;

import java.text.MessageFormat;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A very generic interface for applications to "print messages".
 * <p>
 *   Like the various logging mechanisms, the underlying concept is to define several "levels" of logging, and process
 *   messages of different levels differently. Unlike logging, printing is not JVM-global, but applies only to
 *   specific contexts. In other words, it is not possible to "set" a global printer, but only to run runnables
 *   "in the context" of a printer (see {@link Printers#withPrinter(Printer, Runnable)}). This is useful e.g. in
 *   multi-threaded environments like tasks in a build system or a script.
 * </p>
 */
public
interface Printer {

    /**
     * Prints an error condition.
     */
    void error(@Nullable String message);

    /**
     * Prints an error condition.
     *
     * @see MessageFormat#format(Object)
     */
    void error(String pattern, Object... arguments);

    /**
     * Prints an error condition.
     */
    void error(@Nullable String message, @Nullable Throwable t);

    /**
     * Prints an error condition.
     *
     * @see MessageFormat#format(Object)
     */
    void error(String pattern, @Nullable Throwable t, Object... arguments);

    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Prints a warning condition.
     */
    void warn(@Nullable String message);

    /**
     * Prints a warning condition.
     *
     * @see MessageFormat#format(Object)
     */
    void warn(String pattern, Object... arguments);

    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Prints an informative ("normal") message.
     */
    void info(@Nullable String message);

    /**
     * Prints an informative ("normal") message.
     *
     * @see MessageFormat#format(Object)
     */
    void info(String pattern, Object... arguments);

    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Prints a verbose message.
     */
    void verbose(@Nullable String message);

    /**
     * Prints a verbose message.
     *
     * @see MessageFormat#format(Object)
     */
    void verbose(String pattern, Object... arguments);

    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Prints a debug message.
     */
    void debug(@Nullable String message);

    /**
     * Prints a debug message.
     *
     * @see MessageFormat#format(Object)
     */
    void debug(String pattern, Object... arguments);
}
