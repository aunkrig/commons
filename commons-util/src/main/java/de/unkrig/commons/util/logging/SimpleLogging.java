
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

package de.unkrig.commons.util.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.formatter.PrintfFormatter;
import de.unkrig.commons.util.logging.formatter.SelectiveFormatter;
import de.unkrig.commons.util.logging.handler.ProxyHandler;
import de.unkrig.commons.util.logging.handler.StderrHandler;
import de.unkrig.commons.util.logging.handler.StdoutHandler;

/**
 * A utility class that simplifies the usage of Java&trade;'s {@code java.util.logging} facility.
 * <p>
 *   Typically, you simply call
 * </p>
 * <blockquote>{@code SimpleLogging.init();}</blockquote>
 * <p>
 *   as soon as possible when your Java program starts (perhaps from a static initializer of your main class).
 * </p>
 * <p>
 *   Later, at any possible point of time, you'd optionally call one of the following:
 * </p>
 * <table border="1" cellpadding="3" cellspacing="0">
 *   <tr>
 *     <td>{@link #setQuiet()}</td>
 *     <td>Suppress {@code LOGGER.info()} messages</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setNoWarn()}</td>
 *     <td>Suppress {@code LOGGER.info()} and {@code LOGGER.warning()} messages</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setNormal()}</td>
 *     <td>(Resets SimpleLogging to ist default behavior.)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setVerbose()}</td>
 *     <td>Also print {@code LOGGER.config()} messages to STDOUT</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setDebug()}</td>
 *     <td>Also print {@code LOGGER.fine()} messages to STDERR<sup>*</sup></td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setDebug()}<br>{@link #setDebug()}</td>
 *     <td>Also print {@code LOGGER.finer()} messages to STDERR<sup>*</sup></td>
 *   </tr>
 *   <tr>
 *     <td>{@link #setDebug()}<br>{@link #setDebug()}<br>{@link #setDebug()}</td>
 *     <td>Also print {@code LOGGER.finest()} messages to STDERR<sup>*</sup></td>
 *   </tr>
 * </table>
 * <p>
 *   <sup>*</sup> These messages are printed with source class name, source method name, exception (if any) and stack
 *   trace.
 * </p>
 * <p>
 *   Alternatively, you can call {@link #setLevel(Level)} with a respective <var>level</var> parameter.
 * </p>
 * <p>
 *   Alternatively, you may want to call {@link #configureLoggers(String)} to configure log levels, handlers and
 *   formats for individual loggers. For example,
 * </p>
 * <blockquote>
 * {@code SimpleLogging.configureLoggers("FINEST:com.acme:FileHandler(\"foo.txt\"):java.util.logging.XMLFormatter");}
 * </blockquote>
 * <p>
 *   activates full debugging for logger "com.acme" and its children (e.g. "com.acme.foo"), and writes to the file
 *   "foo.txt" in XML format.
 * </p>
 */
public final
class SimpleLogging {

    private
    SimpleLogging() {}

    /**
     * Logs the messages of all levels below {@link Level#CONFIG CONFIG} (FINE, FINER, FINEST, ...) to STDERR.
     */
    private static final Handler DEBUG_HANDLER;

    /**
     * Logs the messages of levels {@link Level#CONFIG CONFIG} (inclusive) through {@link Level#WARNING WARNING}
     * (exclusive) to STDOUT or a custom {@link Handler} (see {@link #setOut(Handler)}.
     */
    private static final ProxyHandler OUT_HANDLER;

    /**
     * Logs the messages of levels {@link Level#WARNING WARNING} and higher to STDERR.
     */
    private static final Handler STDERR_HANDLER;

    // Default formatters for the handlers.

    private static final Formatter DEFAULT_DEBUG_FORMATTER = new PrintfFormatter("%10$27s::%7$-15s %8$s%9$s%n");
    private static final Formatter DEFAULT_OUT_FORMATTER   = SelectiveFormatter.loggerLevelGreaterThan(
        Level.FINE,
        PrintfFormatter.MESSAGE_AND_EXCEPTION,
        PrintfFormatter.MESSAGE_AND_STACK_TRACE
    );
    private static final Formatter DEFAULT_STDERR_FORMATTER = SimpleLogging.DEFAULT_OUT_FORMATTER;

    static {
        DEBUG_HANDLER = new StderrHandler();
        SimpleLogging.DEBUG_HANDLER.setFilter(LogUtil.LESS_THAN_CONFIG);
        SimpleLogging.DEBUG_HANDLER.setLevel(Level.ALL);
        SimpleLogging.DEBUG_HANDLER.setFormatter(SimpleLogging.DEFAULT_DEBUG_FORMATTER);
        LogUtil.ROOT_LOGGER.addHandler(SimpleLogging.DEBUG_HANDLER);

        OUT_HANDLER = new ProxyHandler(new StdoutHandler());
        SimpleLogging.OUT_HANDLER.setFilter(LogUtil.LESS_THAN_WARNING);
        SimpleLogging.OUT_HANDLER.setLevel(Level.CONFIG);
        SimpleLogging.OUT_HANDLER.setFormatter(SimpleLogging.DEFAULT_OUT_FORMATTER);
        LogUtil.ROOT_LOGGER.addHandler(SimpleLogging.OUT_HANDLER);

        STDERR_HANDLER = new StderrHandler();
        SimpleLogging.STDERR_HANDLER.setLevel(Level.WARNING);
        SimpleLogging.STDERR_HANDLER.setFormatter(SimpleLogging.DEFAULT_STDERR_FORMATTER);
        LogUtil.ROOT_LOGGER.addHandler(SimpleLogging.STDERR_HANDLER);
    }

    /**
     * Sets up the default configuration.
     * <ul>
     *   <li>
     *     Messages with levels >= {@link Level#WARNING WARNING} are printed to STDERR.
     *   </li>
     *   <li>
     *     Messages with levels {@link Level#INFO INFO} are printed to STDOUT.
     *   </li>
     *   <li>
     *     Messages with levels &lt;= {@link Level#CONFIG CONFIG} are not printed.
     *   </li>
     * </ul>
     */
    public static void
    init() {

        // Everything is already done in static initializers.
        ;
    }

    /**
     * Configures the logging of a command-line utility as usual.
     * <table border="1" cellpadding="3" cellspacing="0">
     *   <tr>
     *     <th>Typical command line option</th>
     *     <th>level</th>
     *     <th>Levels logged to STDERR</th>
     *     <th>Levels logged to STDOUT</th>
     *   </tr>
     *   <tr><td>-nowarn</td><td>SEVERE</td><td>SEVERE</td><td>-</td></tr>
     *   <tr><td>-quiet</td><td>WARNING</td><td>SEVERE, WARNING</td><td>-</td></tr>
     *   <tr><td>(none)</td><td>INFO</td><td>SEVERE, WARNING</td><td>INFO</td></tr>
     *   <tr><td>-verbose</td><td>CONFIG</td><td>SEVERE, WARNING</td><td>INFO, CONFIG</td></tr>
     *   <tr>
     *     <td>-debug</td>
     *     <td>FINEST</td>
     *     <td>SEVERE, WARNING<br>FINE, FINER, FINEST<sup>*</sup></td>
     *     <td>INFO, CONFIG</td>
     *   </tr>
     * </table>
     * <sup>*</sup>: FINE, FINER and FINEST log records are printed with class, method, source and line number
     */
    public static synchronized void
    setLevel(Level level) {
        LogUtil.ROOT_LOGGER.setLevel(level);
    }

    /**
     * @return               The currently configured level for the root logger
     * @see #setLevel(Level)
     */
    public static Level
    getLevel() {
        return LogUtil.ROOT_LOGGER.getLevel();
    }

    /**
     * Sets the formatter for all three handlers (debug, out and stderr).
     *
     * @param spec The expression to parse and to evaluate
     * @see Parser The expression syntax
     */
    public static void
    setFormatter(String spec) throws ParseException, EvaluationException {
        SimpleLogging.setFormatter(
            SimpleLogging.FORMATTER_INSTANTIATOR.evaluateTo(
                spec,
                Mappings.<String, Object>none(),
                Formatter.class
            )
        );
    }

    /**
     * Sets the formatter for all three handlers (debug, out and stderr).
     */
    public static void
    setFormatter(Formatter formatter) {
        SimpleLogging.DEBUG_HANDLER.setFormatter(formatter);
        SimpleLogging.OUT_HANDLER.setFormatter(formatter);
        SimpleLogging.STDERR_HANDLER.setFormatter(formatter);
    }

    /**
     * Shorthand for {@code setLevel(Level.WARNING + 1)}: Messages of levels {@code INFO} and {@code WARNING} are
     * suppressed.
     */
    public static void
    setNoWarn() {
        SimpleLogging.setLevel(LogUtil.WARNING_PLUS_1);
    }

    /**
     * Shorthand for {@code setLevel(Level.INFO + 1)}: Messages of level {@link Level#INFO}, i.e. 'normal output' are
     * suppressed.
     */
    public static void
    setQuiet() {
        SimpleLogging.setLevel(LogUtil.INFO_PLUS_1);
    }

    /**
     * Shorthand for {@code setLevel(Level.INFO)}: Messages of level {@link Level#INFO INFO}, i.e. 'normal output' and
     * above ({@link Level#WARNING WARNING} and {@link Level#SEVERE SEVERE}) are logged.
     */
    public static void
    setNormal() {
        SimpleLogging.setLevel(Level.INFO);
    }

    /**
     * Shorthand for {@code setLevel(Level.CONFIG)}: Messages of level {@link Level#CONFIG CONFIG}, i.e. 'verbose
     * output' are logged.
     */
    public static void
    setVerbose() {
        SimpleLogging.setLevel(Level.CONFIG);
    }

    /**
     * Shorthand for {@code setLevel(FINE)}. All messages down to level {@link Level#FINE FINE} are logged.
     * <p>
     * Calling this method multiply lowers the level to {@link Level#FINER} and then {@link Level#FINEST}.
     */
    public static void
    setDebug() {
        Level l = SimpleLogging.getLevel();
        SimpleLogging.setLevel(
            l == Level.FINER ? Level.FINEST :
            l == Level.FINE  ? Level.FINER  :
            Level.FINE
        );
    }

    /**
     * Installs a {@link Handler} which writes messages of levels {@link Level#INFO INFO} (inclusive) through {@link
     * Level#WARNING WARNING} (exclusive) to STDOUT.
     */
    public static void
    setStdout() {
        SimpleLogging.setOut(new StdoutHandler());
    }

    /**
     * Sets a {@link Handler} which writes messages of levels {@link Level#INFO INFO} (inclusive) through {@link
     * Level#WARNING WARNING} (exclusive) to the given {@link File}.
     */
    public static void
    setOut(File value) throws IOException {
        SimpleLogging.setOut(new FileHandler(value.getPath()));
    }

    /**
     * Sets the given {@link Handler} for messages of levels {@link Level#INFO INFO} (inclusive) through {@link
     * Level#WARNING WARNING} (exclusive).
     * <p>
     * Clients may want to use this method to redirect their "normal" output (not the errors, warnings and debug
     * output) elsewhere, e.g. into an "output file".
     *
     * @param handler {@code null} to reset to the 'normal' behavior (print to STDOUT)
     */
    public static synchronized void
    setOut(@Nullable Handler handler) {

        // Close the old delegate.
        SimpleLogging.OUT_HANDLER.close();

        // Set the new delegate.
        SimpleLogging.OUT_HANDLER.setDelegate(handler == null ? new StdoutHandler() : handler);

        // Configure the new delegate.
        SimpleLogging.OUT_HANDLER.setFilter(LogUtil.LESS_THAN_WARNING);
        SimpleLogging.OUT_HANDLER.setLevel(Level.INFO);
        SimpleLogging.OUT_HANDLER.setFormatter(SimpleLogging.DEFAULT_OUT_FORMATTER);
    }

    static {
        for (Handler handler : LogUtil.ROOT_LOGGER.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.close();
                LogUtil.ROOT_LOGGER.removeHandler(handler);
            }
        }
        SimpleLogging.setNormal();
    }

    private static final ExpressionEvaluator
    HANDLER_INSTANTIATOR = new ExpressionEvaluator(PredicateUtil.<String>never()).setImports(new String[] {
        "java.util.logging",
        "de.unkrig.commons.util.logging.handler",
    });

    private static final ExpressionEvaluator
    FORMATTER_INSTANTIATOR = new ExpressionEvaluator(PredicateUtil.<String>never()).setImports(new String[] {
        "java.util.logging",
        "de.unkrig.commons.util.logging.formatter",
    });

    // SUPPRESS CHECKSTYLE LineLength:8
    /**
     * Sets the <var>level</var> of the named loggers, adds the given <var>handler</var> on them and sets the given
     * <var>formatter</var> on the <var>handler</var>.
     * <p>
     *   The <var>spec</var> is parsed as follows:
     * </p>
     * <pre>
     * <var>spec</var> := [ <var>level</var> ] [ ':' [ <var>logger-names</var> ] [ ':' [ <var>handler</var> ] [ ':' [ <var>formatter</var> ] [ ':' <var>use-parent-handlers</var> ] ] ] ]
     * <var>logger-names</var> := <var>logger-name</var> [ ',' <var>logger-name</var> ]...
     * </pre>
     * <p>
     *   The <var>level</var> component determines the log level of the <var>handler</var>, or, iff no
     *   <var>handler</var> is given, the level of the <var>loggers</var>.
     *   It must be parsable by {@link Level#parse(String)}, i.e. it must be a decimal number, or one of {@code
     *   SEVERE}, {@code WARNING}, {@code INFO}, {@code CONFIG}, {@code FINE}, {@code FINER}, {@code FINEST} or {@code
     *   ALL}.
     * </p>
     * <p>
     *   The <var>handler</var> and <var>formatter</var> components denote {@link Parser expressions}, with the
     *   automatically imported packages "{@code java.util.logging}", "{@code de.unkrig.commons.util.logging.handler}"
     *   and "{@code de.unkrig.commons.util.logging.formatter}". Notice that neither expression may contain colons.
     * </p>
     * <p>
     *   Example <var>spec</var>:
     * </p>
     * <pre>
     * FINE:de.unkrig:ConsoleHandler:FormatFormatter("%5$tF %5$tT.%5$tL %10$-20s %3$2d %8$s%9$s%n")
     * </pre>
     * <p>
     *   If any of the components of the <var>spec</var> is missing or empty, a reasonable default value is assumed:
     * </p>
     * <dl>
     *   <dt><var>level</var></dt>
     *   <dd>
     *     Logger: Level inherited from parent logger
     *     <br />
     *     Handler: Handler's default log level, typically {@code ALL}
     *   </dd>
     *
     *   <dt><var>loggers</var></dt>
     *   <dd>
     *     The root logger
     *   </dd>
     *
     *   <dt><var>handler</var></dt>
     *   <dd>
     *     {@link StdoutHandler}
     *   </dd>
     *
     *   <dt><var>formatter</var></dt>
     *   <dd>
     *     {@link PrintfFormatter#MESSAGE_AND_EXCEPTION MESSAGE_AND_EXCEPTION}
     *   </dd>
     *
     *   <dt><var>use-parent-handlers</var></dt>
     *   <dd>
     *     {@code true}
     *   </dd>
     * </dl>
     * <p>
     *   It is recommended that command line tools call this method from their {@code main(String[])} method, on each
     *   occurrence of a {@code "-log} <var>spec</var>{@code "} command line option.
     * </p>
     *
     * @see Parser The expression syntax of the <var>handler</var> and the <var>formatter</var> components
     */
    public static void
    configureLoggers(String spec) {
        String[] args = new String[5];
        {
            String[] sa = spec.split(":", 5);
            for (int i = 0; i < args.length && i < sa.length; i++) {
                String s = sa[i].trim();
                if (!"".equals(s)) args[i] = s;
            }
        }

        // Argument 1 == level
        Level level;
        if (args[0] != null) {
            level = Level.parse(args[0]);
        } else {
            level = null;
        }

        // Argument 2 == loggers
        Logger[] loggers;
        if (args[1] != null) {
            String   loggerNames = args[1];
            String[] tmp         = loggerNames.split(",");
            loggers = new Logger[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                loggers[i] = Logger.getLogger(tmp[i]);
            }
        } else {
            loggers = new Logger[] { LogUtil.ROOT_LOGGER };
        }

        // Argument 3 == handler
        Handler handler = null;
        if (args[2] != null) {
            String handlerSpec = args[2];
            try {
                handler = SimpleLogging.HANDLER_INSTANTIATOR.evaluateTo(
                    handlerSpec,
                    Mappings.<String, Object>none(),
                    Handler.class
                );
            } catch (Exception e) {
                throw ExceptionUtil.wrap(handlerSpec + ": " + e.getMessage(), e, RuntimeException.class);
            }
        }

        // Argument 4 == formatter
        Formatter formatter;
        if (args[3] != null) {
            String formatterSpec = args[3];
            try {
                formatter = SimpleLogging.FORMATTER_INSTANTIATOR.evaluateTo(
                    formatterSpec,
                    Mappings.<String, Object>none(),
                    Formatter.class
                );
            } catch (Exception e) {
                throw ExceptionUtil.wrap(
                    "Evaluating formatter spec \"" + formatterSpec + "\": " + e.getMessage(),
                    e,
                    RuntimeException.class
                );
            }
        } else {
            formatter = PrintfFormatter.MESSAGE_AND_EXCEPTION;
        }

        // Argument 5 == useParenthandlers
        boolean useParentHandlers = true;
        if (args[4] != null) {
            useParentHandlers = Boolean.parseBoolean(args[4]);
        }

        if (handler == null) {

            for (Logger logger : loggers) {
                logger.setLevel(level);
            }

            handler = new StdoutHandler();
        }

        if (level != null) handler.setLevel(level);
        handler.setFormatter(formatter);

        for (Logger logger : loggers) {

            logger.addHandler(handler);

            if (level == null || !logger.isLoggable(level)) logger.setLevel(level);

            logger.setUseParentHandlers(useParentHandlers);
        }
    }
}
