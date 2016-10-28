
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A super-simple API for managing output of different kinds. Basically an alternative for "{@code
 * System.out.println()}" and "{@code System.err.println()}", with much more flexibility.
 * <p>
 *   This concept borrows from the various "logging" frameworks, but differs in several ways:
 * </p>
 * <ul>
 *   <li>It is made for "foreground processing", e.g. command line tools that output messages as they process data.</li>
 *   <li>It is inherently thread-oriented and not application-global.</li>
 *   <li>It is not oriented "by class" but only "by level".</li>
 *   <li>The output is intended for users and administrators, and not so much for programmers.</li>
 * </ul>
 * <p>
 *   Five levels of output exist:
 * </p>
 * <ul>
 *   <li>"Debug" output, e.g. the progress of the creation of a connection</li>
 *   <li>"Verbose" output, e.g. the names of files as they are processed</li>
 *   <li>"Info" output, e.g. the matched found by a GREP utility</li>
 *   <li>"Warn" output</li>
 *   <li>"Error" output</li>
 * </ul>
 * <p>
 *   Use the {@code error()}, {@code warn()}, {@code info()}, {@code verbose()} and {@code debug()} methods to print to
 *   the "current context".
 * </p>
 * <p>
 *   The "initial context" (which is in effect iff {@link #withPrinter(Printer, Runnable)} has not yet been called)
 *   prints errors and warnings to {@link System#err} and infos, verbose and debug to {@link System#out}.
 * </p>
 * <p>
 *   In many cases, you want to use a {@link LevelFilteredPrinter} in a very early stage to configure how output is
 *   printed:
 * </p>
 * <pre>
 *     public static void
 *     main(final String... args) {
 *
 *         final Main main = new Main();
 *         Printers.withPrinter(main.levelFilteredPrinter, new Runnable() {
 *
 *             &#64;Override public void run() { main.main2(args); }
 *         });
 *     }
 *
 *     private final LevelFilteredPrinter levelFilteredPrinter = new LevelFilteredPrinter(Printers.get());
 *
 *     private void
 *     main2(String[] args) {
 *
 *         // As an example, we enable verbose messages here. You would typically do that as a reaction to a
 *         // "--verbose" command line option.
 *         levelFilteredPrinter.setVerbose();
 *
 *         Printers.debug("foo");   // Not printed, because debug level was not enabled on the levelFilteredPrinter
 *         Printers.verbose("foo"); // Prints to STDOUT, because verbose messages were enabled above.
 *         Printers.info("foo");    // Prints to STDOUT
 *         Printers.warn("foo");    // prints to STDERR
 *         Printers.err("foo");     // prints to STDERR
 *     }
 *
 * </pre>
 *
 * @see #withPrinter(Printer, Runnable)
 * @see #withPrinter(Printer, RunnableWhichThrows)
 */
public final
class Printers {

    private Printers() {}

    private static final Printer DEFAULT_PRINTER = new AbstractPrinter() {
        @Override public void error(@Nullable String message)   { System.err.println(message); }
        @Override public void warn(@Nullable String message)    { System.err.println(message); }
        @Override public void info(@Nullable String message)    { System.out.println(message); }
        @Override public void verbose(@Nullable String message) { System.out.println(message); }
        @Override public void debug(@Nullable String message)   { System.out.println(message); }
    };

    private static final ThreadLocal<Printer> THREAD_LOCAL_PRINTER = new InheritableThreadLocal<Printer>() {
        @Override protected Printer initialValue() { return Printers.DEFAULT_PRINTER; }
    };

    /**
     * @return the context printer for this thread
     */
    public static Printer get() { return Printers.THREAD_LOCAL_PRINTER.get(); }

    /** Prints an error condition on the context printer. */
    public static void
    error(@Nullable String message) { Printers.get().error(message); }

    /**
     * Prints an error condition on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    error(String pattern, Object... arguments) { Printers.get().error(pattern, arguments); }

    /** Prints an error condition on the context printer. */
    public static void
    error(@Nullable String message, Throwable t) { Printers.get().error(message, t); }

    /**
     * Prints an error condition on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    error(String pattern, Throwable t, Object... arguments) { Printers.get().error(pattern, t, arguments); }

    /** Prints a warning condition on the context printer. */
    public static void
    warn(@Nullable String message) { Printers.get().warn(message); }

    /**
     * Prints a warning condition on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    warn(String pattern, Object... arguments) { Printers.get().warn(pattern, arguments); }

    /** Prints an informative ("normal") message on the context printer. */
    public static void
    info(@Nullable String message) { Printers.get().info(message); }

    /**
     * Prints an informative ("normal") message on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    info(String pattern, Object... arguments) { Printers.get().info(pattern, arguments); }

    /** Prints a verbose message on the context printer. */
    public static void
    verbose(@Nullable String message) { Printers.get().verbose(message); }

    /**
     * Prints a verbose message on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    verbose(String pattern, Object... arguments) { Printers.get().verbose(pattern, arguments); }

    /** Prints a debug message on the context printer. */
    public static void
    debug(@Nullable String message) { Printers.get().debug(message); }

    /**
     * Prints a debug message on the context printer.
     *
     * @see MessageFormat
     */
    public static void
    debug(String pattern, Object... arguments) { Printers.get().debug(pattern, arguments); }

    /**
     * Replaces the context printer with the given {@code printer} while the {@code runnable} is running.
     */
    public static synchronized void
    withPrinter(Printer printer, Runnable runnable) {

        final Printer oldThreadPrinter = Printers.get();

        Printers.set(printer);
        try {
            runnable.run();
        } finally {
            Printers.set(oldThreadPrinter);
        }
    }

    /**
     * Replaces the context printer with the given {@code printer} while the {@code runnable} is running.
     */
    public static synchronized <EX extends Throwable> void
    withPrinter(Printer printer, RunnableWhichThrows<EX> runnable) throws EX {

        final Printer oldThreadPrinter = Printers.get();

        Printers.set(printer);
        try {
            runnable.run();
        } finally {
            Printers.set(oldThreadPrinter);
        }
    }

    /**
     * Runs the <var>runnable</var> with its INFO output redirected into the <var>file</var>. If the <var>runnable</var>
     * throws an exception, then the <var>file</var> is closed, but <em>not</em> deleted.
     * <p>
     *    Iff the <var>file</var> is {@code null}, then the INFO output is <em>not</em> redirected.
     *  </p>
     */
    public static <EX extends Throwable> void
    redirectInfoToFile(@Nullable File file, RunnableWhichThrows<EX> runnable) throws IOException, EX {

        if (file == null) {
            runnable.run();
            return;
        }

        final PrintWriter pw = new PrintWriter(file);
        try {

            Printers.redirectInfo(pw, runnable);
            pw.close();
        } finally {
            try { pw.close(); } catch (Exception e) {}
        }
    }

    /**
     * Runs the <var>runnable</var> with its INFO output redirected to the <var>writer</var>. Iff the <var>writer</var>
     * is {@code null}, then the INFO output is <em>not</em> redirected.
     */
    public static <EX extends Throwable> void
    redirectInfo(@Nullable Writer writer, RunnableWhichThrows<EX> runnable) throws EX {

        if (writer == null) {
            runnable.run();
            return;
        }

        final PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);

        Printers.withPrinter(
            new ProxyPrinter(Printers.get()) {
                @Override public void info(@Nullable String message) { pw.println(message); }
            },
            runnable
        );
    }

    /**
     * Runs the <var>runnable</var> with its INFO output redirected to the <var>infoConsumer</var>. Iff the
     * <var>infoConsumer</var> is {@code null}, then the INFO output is <em>not</em> redirected.
     */
    public static <EX extends Throwable> void
    redirectInfo(
        @Nullable final ConsumerWhichThrows<? super String, ? extends RuntimeException> infoConsumer,
        RunnableWhichThrows<? extends EX>                                               runnable
    ) throws EX {

        if (infoConsumer == null) {
            runnable.run();
            return;
        }

        Printers.withPrinter(
            new ProxyPrinter(Printers.get()) {

                @Override public void
                info(@Nullable String message) { if (message != null) infoConsumer.consume(message); }
            },
            runnable
        );
    }

    /**
     * Sets the context printer for this thread and all its (existing and future) child threads (unless they have set
     * their own context printer, or until they set their own context printer).
     */
    private static void set(Printer printer) { Printers.THREAD_LOCAL_PRINTER.set(printer); }
}
