
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.EnumSet;

import javax.swing.text.AbstractWriter;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/** A basic implementation of the {@link Printer} interface. */
public abstract
class AbstractPrinter implements Printer {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    public enum Level { ERROR, WARN, INFO, VERBOSE, DEBUG }

    private static final ThreadLocal<AbstractPrinter>
    THREAD_LOCAL_PRINTER = new InheritableThreadLocal<AbstractPrinter>() {
        @Override protected AbstractPrinter initialValue() { return Printers.DEFAULT_PRINTER; }
    };

    /**
     * Returns the context printer for this thread. The initial context printer for the "main thread" is the {@link
     * #DEFAULT_PRINTER}.
     * <p>
     *   Notice that it is (intentionally) not possible to explicitly <em>set</em> the current thread's context
     *   printer; instead you would use {@link #run(Runnable)}.
     * </p>
     */
    public static AbstractPrinter
    getContextPrinter() { return AbstractPrinter.THREAD_LOCAL_PRINTER.get(); }

    /**
     * Wraps a {@link Printer} as an {@link AbstractPrinter}.
     */
    public static AbstractPrinter
    fromPrinter(final Printer printer) {
        return printer instanceof AbstractPrinter ? (AbstractPrinter) printer : new ProxyPrinter(printer);
        }

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
        this.warn(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    info(String pattern, Object... arguments) {
        this.info(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    verbose(String pattern, Object... arguments) {
        this.verbose(AbstractPrinter.format(pattern, arguments));
    }

    @Override public void
    debug(String pattern, Object... arguments) {
        this.debug(AbstractPrinter.format(pattern, arguments));
    }

    /**
     * Sets the current thread's context printer to {@code this}, runs the <var>runnable</var>, and eventually
     * restores the original context printer.
     */
    public final void
    run(Runnable runnable) { this.run(RunnableUtil.<RuntimeException>asRunnableWhichThrows(runnable)); }

    /**
     * Sets the current thread's context printer to {@code this}, runs the <var>runnable</var>, and eventually
     * restores the original context printer.
     */
    public final <EX extends Throwable> void
    run(RunnableWhichThrows<EX> runnable) throws EX {

        final AbstractPrinter previousThreadPrinter = AbstractPrinter.setContextPrinter(this);
        try {
            runnable.run();
        } finally {
            AbstractPrinter next = AbstractPrinter.setContextPrinter(previousThreadPrinter);
            assert next == this;
        }
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

    // ========================== Redirection to Writer ==========================

    /** @see #redirect(Level, Writer) */
    public final AbstractPrinter redirectError(@Nullable Writer w) { return this.redirect(Level.ERROR, w); }
    /** @see #redirect(Level, Writer) */
    public final AbstractPrinter redirectWarn(@Nullable Writer w) { return this.redirect(Level.WARN, w); }
    /** @see #redirect(Level, Writer) */
    public final AbstractPrinter redirectInfo(@Nullable Writer w) { return this.redirect(Level.INFO, w); }
    /** @see #redirect(Level, Writer) */
    public final AbstractPrinter redirectVerbose(@Nullable Writer w) { return this.redirect(Level.VERBOSE, w); }
    /** @see #redirect(Level, Writer) */
    public final AbstractPrinter redirectDebug(@Nullable Writer w) { return this.redirect(Level.DEBUG, w); }

    /**
     * Creates and returns an {@link AbstractPrinter} which writes messages of the given <var>level</var>to the given
     * <var>writer</var>, and forwards all other messages to {@code this} {@link AbstractWriter}.
     * <p>
     *   Iff <var>level</var> {@code == null ||} <var>writer</var> {@code == null}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    redirect(@Nullable final Level level, @Nullable Writer writer) {

        if (level == null || writer == null) return this;

        final PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (level == Level.ERROR)   { pw.println(message); } else { AbstractPrinter.this.error(message);   } }
            @Override public void warn(@Nullable String message)    { if (level == Level.WARN)    { pw.println(message); } else { AbstractPrinter.this.warn(message);    } }
            @Override public void info(@Nullable String message)    { if (level == Level.INFO)    { pw.println(message); } else { AbstractPrinter.this.info(message);    } }
            @Override public void verbose(@Nullable String message) { if (level == Level.VERBOSE) { pw.println(message); } else { AbstractPrinter.this.verbose(message); } }
            @Override public void debug(@Nullable String message)   { if (level == Level.DEBUG)   { pw.println(message); } else { AbstractPrinter.this.debug(message);   } }
        };
    }

    /**
     * Creates and returns an {@link AbstractPrinter} which writes messages of the given <var>levels</var>to the given
     * <var>writer</var>, and forwards all other messages to {@code this} {@link AbstractWriter}.
     * <p>
     *   Iff <var>levels</var> {@code == null ||} <var>levels</var>{@code .isEmpty() ||} <var>writer</var> {@code ==
     *   null}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    redirect(@Nullable final EnumSet<Level> levels, @Nullable Writer writer) {

        if (levels == null || levels.isEmpty() || writer == null) return this;

        final PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (levels.contains(Level.ERROR))   { pw.println(message); } else { AbstractPrinter.this.error(message);   } }
            @Override public void warn(@Nullable String message)    { if (levels.contains(Level.WARN))    { pw.println(message); } else { AbstractPrinter.this.warn(message);    } }
            @Override public void info(@Nullable String message)    { if (levels.contains(Level.INFO))    { pw.println(message); } else { AbstractPrinter.this.info(message);    } }
            @Override public void verbose(@Nullable String message) { if (levels.contains(Level.VERBOSE)) { pw.println(message); } else { AbstractPrinter.this.verbose(message); } }
            @Override public void debug(@Nullable String message)   { if (levels.contains(Level.DEBUG))   { pw.println(message); } else { AbstractPrinter.this.debug(message);   } }
        };
    }

    // ========================== Redirection to Consumer<String> ==========================

    /** @see #redirect(Level, Consumer) */
    public final AbstractPrinter redirectError(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> errorConsumer)     { return this.redirect(Level.ERROR, errorConsumer);     }
    /** @see #redirect(Level, Consumer) */
    public final AbstractPrinter redirectWarn(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> warnConsumer)       { return this.redirect(Level.WARN, warnConsumer);       }
    /** @see #redirect(Level, Consumer) */
    public final AbstractPrinter redirectInfo(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> infoConsumer)       { return this.redirect(Level.INFO, infoConsumer);       }
    /** @see #redirect(Level, Consumer) */
    public final AbstractPrinter redirectVerbose(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> verboseConsumer) { return this.redirect(Level.VERBOSE, verboseConsumer); }
    /** @see #redirect(Level, Consumer) */
    public final AbstractPrinter redirectDebug(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> debugConsumer)     { return this.redirect(Level.DEBUG, debugConsumer);     }

    /**
     * Creates and returns an {@link AbstractPrinter} which sends all non-{@code null} messages of the given
     * <var>level</var>to the given <var>messageConsumer</var>, and forwards all other messages to {@code this} {@link
     * AbstractWriter}.
     * <p>
     *   Iff <var>level</var> {@code == null ||} <var>messageConsumer</var> {@code == null}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    redirect(
        @Nullable final Level                                                           level,
        @Nullable final ConsumerWhichThrows<? super String, ? extends RuntimeException> messageConsumer
    ) {

        if (level == null || messageConsumer == null) return this;

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (level == Level.ERROR)   { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.error(message);   } }
            @Override public void warn(@Nullable String message)    { if (level == Level.WARN)    { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.warn(message);    } }
            @Override public void info(@Nullable String message)    { if (level == Level.INFO)    { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.info(message);    } }
            @Override public void verbose(@Nullable String message) { if (level == Level.VERBOSE) { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.verbose(message); } }
            @Override public void debug(@Nullable String message)   { if (level == Level.DEBUG)   { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.debug(message);   } }
        };
    }

    /**
     * Creates and returns an {@link AbstractPrinter} which sends all non-{@code null} messages of the given
     * <var>levels</var>to the given <var>messageConsumer</var>, and forwards all other messages to {@code this} {@link
     * AbstractWriter}.
     * <p>
     *   Iff <var>levels</var> {@code == null ||} <var>levels</var>{@code .isEmpty() ||} <var>messageConsumer</var>
     *   {@code == null}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    redirect(
        @Nullable final EnumSet<Level>                                                  levels,
        @Nullable final ConsumerWhichThrows<? super String, ? extends RuntimeException> messageConsumer
    ) {

        if (levels == null || levels.isEmpty() || messageConsumer == null) return this;

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (levels.contains(Level.ERROR))   { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.error(message);   } }
            @Override public void warn(@Nullable String message)    { if (levels.contains(Level.WARN))    { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.warn(message);    } }
            @Override public void info(@Nullable String message)    { if (levels.contains(Level.INFO))    { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.info(message);    } }
            @Override public void verbose(@Nullable String message) { if (levels.contains(Level.VERBOSE)) { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.verbose(message); } }
            @Override public void debug(@Nullable String message)   { if (levels.contains(Level.DEBUG))   { if (message != null) messageConsumer.consume(message); } else { AbstractPrinter.this.debug(message);   } }
        };
    }

    // ========================== Discarding individual message levels ==========================

    /** @see #discard(Level) */
    public final AbstractPrinter discardError(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> errorConsumer)     { return this.discard(Level.ERROR);     }
    /** @see #discard(Level) */
    public final AbstractPrinter discardWarn(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> warnConsumer)       { return this.discard(Level.WARN);       }
    /** @see #discard(Level) */
    public final AbstractPrinter discardInfo(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> infoConsumer)       { return this.discard(Level.INFO);       }
    /** @see #discard(Level) */
    public final AbstractPrinter discardVerbose(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> verboseConsumer) { return this.discard(Level.VERBOSE); }
    /** @see #discard(Level) */
    public final AbstractPrinter discardDebug(@Nullable ConsumerWhichThrows<? super String, ? extends RuntimeException> debugConsumer)     { return this.discard(Level.DEBUG);     }

    /**
     * Creates and returns an {@link AbstractPrinter} which discards all messages of the given <var>level</var>, and
     * forwards all other messages to {@code this} {@link AbstractWriter}.
     * <p>
     *   Iff <var>level</var> {@code == null}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    discard(@Nullable final Level level) {

        if (level == null) return this;

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (level != Level.ERROR)   AbstractPrinter.this.error(message);   }
            @Override public void warn(@Nullable String message)    { if (level != Level.WARN)    AbstractPrinter.this.warn(message);    }
            @Override public void info(@Nullable String message)    { if (level != Level.INFO)    AbstractPrinter.this.info(message);    }
            @Override public void verbose(@Nullable String message) { if (level != Level.VERBOSE) AbstractPrinter.this.verbose(message); }
            @Override public void debug(@Nullable String message)   { if (level != Level.DEBUG)   AbstractPrinter.this.debug(message);   }
        };
    }

    /**
     * Creates and returns an {@link AbstractPrinter} which discards all messages of the given <var>levels</var>, and
     * forwards all other messages to {@code this} {@link AbstractWriter}.
     * <p>
     *   Iff <var>levels</var> {@code == null ||} <var>levels</var>{@code .isEmpty()}, then {@code this} object is returned instead.
     * </p>
     */
    public final AbstractPrinter
    discard(@Nullable final EnumSet<Level> levels) {

        if (levels == null || levels.isEmpty()) return this;

        return new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { if (!levels.contains(Level.ERROR))   AbstractPrinter.this.error(message);   }
            @Override public void warn(@Nullable String message)    { if (!levels.contains(Level.WARN))    AbstractPrinter.this.warn(message);    }
            @Override public void info(@Nullable String message)    { if (!levels.contains(Level.INFO))    AbstractPrinter.this.info(message);    }
            @Override public void verbose(@Nullable String message) { if (!levels.contains(Level.VERBOSE)) AbstractPrinter.this.verbose(message); }
            @Override public void debug(@Nullable String message)   { if (!levels.contains(Level.DEBUG))   AbstractPrinter.this.debug(message);   }
        };
    }

    /**
     * Sets the context printer for this thread and all its (existing and future) child threads (unless they have set
     * their own context printer, or until they set their own context printer).
     *
     * @return The context printer that was in effect <em>before</em> this method was invoked
     */
    private static AbstractPrinter
    setContextPrinter(AbstractPrinter printer) {
        AbstractPrinter previous = AbstractPrinter.THREAD_LOCAL_PRINTER.get();
        AbstractPrinter.THREAD_LOCAL_PRINTER.set(printer);
        return previous;
    }
}
