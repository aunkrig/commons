
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

package de.unkrig.commons.util.logging.formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A {@link Formatter} that formats a {@link LogRecord} through a format string (as described for {@link
 * java.util.Formatter}) which is specified through the {@code de.unkrig.commons.util.logging.FormatFormatter.format}
 * logging property.
 * <p>
 * <b>Notice:</b><br/>
 * Since Java 7, the {@link java.util.logging.SimpleFormatter} also has a '.format' property and thus implements the
 * key feature of this class.
 * <p>
 * This class is a COPY of {@code de.unkrig.ext.logging.formatter.PrintfFormatter}, and should be kept in sync
 * with that.
 */
public
class PrintfFormatter extends Formatter {

    // Declare default values as PUBLIC constants so they can be used with the "{@value}" doc tag.
    // SUPPRESS CHECKSTYLE JavadocVariable:7
    public static final String FORMAT_STRING_BENCHMARK               = "%12$s %10$-20s %3$2d %8$s%n";
    public static final String FORMAT_STRING_COMPACT                 = "%4$s %6$s::%7$s %8$s%9$s%n";
    public static final String FORMAT_STRING_MESSAGE                 = "%8$s%n";
    public static final String FORMAT_STRING_MESSAGE_AND_EXCEPTION   = "%8$s%11$s%n";
    public static final String FORMAT_STRING_MESSAGE_AND_STACK_TRACE = "%8$s%9$s%n";
    public static final String FORMAT_STRING_SIMPLE                  = "%5$tF %5$tT.%5$tL %10$-20s %3$2d %8$s%9$s%n";
    public static final String FORMAT_STRING_TIME_MESSAGE            = "%5$tF %5$tT.%5$tL %8$s%n";

    /**
     * Formats log records with the format string "{@value #FORMAT_STRING_BENCHMARK}", which is very suitable for
     * benchmarking.
     * <p>
     *   Example:
     * </p>
     * <pre>
     *     3.000000;    2.000000;    1.000000 MyClass               0 Message
     * </pre>
     */
    public static final PrintfFormatter
    BENCHMARK = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_BENCHMARK);

    /**
     * Formats log records with the format string "{@value #FORMAT_STRING_COMPACT}", which produces a rather
     * terse logging format.
     * <p>
     *   Examples:
     * </p>
     * <pre>
     * FINE pkg.MyClass::main Log message #1
     *
     * FINE pkg.MyClass::main Log message #2
     * java.io.IOException: Exception message
     *     at pkg.MyClass.main()
     * </pre>
     *
     * @see #format(LogRecord)
     */
    public static final PrintfFormatter
    COMPACT = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_COMPACT);

    /**
     * Formats log records with the format string {@value #FORMAT_STRING_MESSAGE} (which expands to the log message).
     * <p>
     *   Example:
     * </p>
     * <pre>
     * Log message
     * </pre>
     *
     * @see #format(LogRecord)
     */
    public static final PrintfFormatter
    MESSAGE = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_MESSAGE);

    /**
     * Formats log records with the format string {@value #FORMAT_STRING_MESSAGE_AND_EXCEPTION} (which expands to the
     * log message plus the (optional) exception name and message).
     * <p>
     *   Examples:
     * </p>
     * <pre>
     * Log message
     * Log message: java.io.FileNotFoundException: Exception message
     * </pre>
     *
     * @see #format(LogRecord)
     */
    public static final PrintfFormatter
    MESSAGE_AND_EXCEPTION = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_MESSAGE_AND_EXCEPTION);

    /**
     * Formats log records with the format string {@value #FORMAT_STRING_MESSAGE_AND_STACK_TRACE} (which expands to the
     * log message, plus the (optional) exception name, message and stack trace).
     * <p>
     *   Examples:
     * </p>
     * <pre>
     * Log message #1
     *
     * Log message #2
     * java.io.IOException: Exception message
     *     at pkg.MyClass.main()
     * </pre>
     *
     * @see #format(LogRecord)
     */
    public static final PrintfFormatter
    MESSAGE_AND_STACK_TRACE = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_MESSAGE_AND_STACK_TRACE);

    /**
     * Formats log records with the format string {@value #FORMAT_STRING_SIMPLE} (which produces a simple one-line
     * format with date, time, simple class name, thread id, log message and the (optional) stack trace).
     * <p>
     *   Example:
     * </p>
     * <pre>
     * 2012-03-08 10:10:28.515 MyClass               0 Log message #1
     *
     * 2012-03-08 10:10:28.516 MyClass               0 Log message #2
     * java.io.IOException: Exception message
     *     at pkg.MyClass.main()
     * </pre>
     *
     * @see #format(LogRecord)
     */
    public static final PrintfFormatter
    SIMPLE = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_SIMPLE);

    /**
     * Formats log records with the format string {@value #FORMAT_STRING_TIME_MESSAGE} (which produces a simple
     * one-line format with date, time and log message).
     * <p>
     *   Example:
     * </p>
     * <pre>
     * 2012-12-31 23:59:59.999 Message
     * </pre>
     */
    public static final PrintfFormatter
    TIME_MESSAGE = new PrintfFormatter(PrintfFormatter.FORMAT_STRING_TIME_MESSAGE);

    /**
     * The "default formatter", which is {@link #SIMPLE}.
     */
    public static final PrintfFormatter DEFAULT               = PrintfFormatter.SIMPLE;
    private static final String         FORMAT_STRING_DEFAULT = PrintfFormatter.FORMAT_STRING_SIMPLE;

    private String format;

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private static
    class State { long realTime, cpuTime, userTime; }

    /**
     * Contains the previously recorded real time, cpu time and user time for each thread.
     */
    private static final ThreadLocal<State> STATE_TL = new ThreadLocal<State>();

    private static final Object THE_BENCHMARK = new Object() {

        @Override public String
        toString() {
            State state = PrintfFormatter.STATE_TL.get();
            if (state == null) {
                state = new State();
                PrintfFormatter.STATE_TL.set(state);
                state.realTime = System.nanoTime();
                state.cpuTime  = PrintfFormatter.THREAD_MX_BEAN.getCurrentThreadCpuTime();
                state.userTime = PrintfFormatter.THREAD_MX_BEAN.getCurrentThreadUserTime();
                return "    -.------;    -.------;    -.------";
            }

            final long realTime = System.nanoTime();
            final long cpuTime  = PrintfFormatter.THREAD_MX_BEAN.getCurrentThreadCpuTime();
            final long userTime = PrintfFormatter.THREAD_MX_BEAN.getCurrentThreadUserTime();

            state.realTime = realTime;
            state.cpuTime  = cpuTime;
            state.userTime = userTime;

            return (
                this.ns2ms(realTime - state.realTime)
                + ';'
                + this.ns2ms(cpuTime - state.cpuTime)
                + ';'
                + this.ns2ms(userTime - state.userTime)
            );
        }

        private String
        ns2ms(long ns) {
            StringBuilder sb = new StringBuilder();

            String s1 = Integer.toString((int) (ns / 1000000L));
            if (s1.length() < 5) sb.append("x    ".substring(s1.length()));
            sb.append(s1);

            sb.append('.');

            String s2 = Integer.toString((int) (ns % 1000000L));
            if (s2.length() < 6) sb.append("x00000".substring(s2.length()));
            sb.append(s2);

            return sb.toString();
        }
    };

    /**
     * The format string is retrieved from the {@code "de.unkrig.commons.util.logging.formatter.PrintfFormatter.format"}
     * logging property.
     * <p>
     *   Logging properties are typically defined in the file "{@code $JAVA_HOME/jre/lib/logging.properties}".
     * </p>
     *
     * @see LogManager
     * @see Logger
     */
    public
    PrintfFormatter() {
        this(0, null);
    }

    /**
     * Constructor for derived classes which wish to impose a different ".format" logging property than that used
     * by {@link #PrintfFormatter()}.
     *
     * @param dummy              Only there to distinguish this constructor from {@link #PrintfFormatter(String)}
     * @param propertyNamePrefix The property name prefix, or {@code null} to use the qualified name of the
     *                           <em>actual</em> formatter class
     * @see #format(LogRecord)
     */
    protected
    PrintfFormatter(int dummy, @Nullable String propertyNamePrefix) {

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        this.format = PrintfFormatter.cookFormat(LogUtil.getLoggingProperty(propertyNamePrefix + ".format"));
    }

    /**
     * A format string with placeholders as described for {@link #format(LogRecord)}.
     * <p>
     * The following special format strings are recognized:
     * <ul>
     *   <li>{@link #SIMPLE                  SIMPLE}
     *   <li>{@link #MESSAGE_AND_EXCEPTION   MESSAGE_AND_EXCEPTION}
     *   <li>{@link #MESSAGE                 MESSAGE}
     *   <li>{@link #MESSAGE_AND_STACK_TRACE MESSAGE_AND_STACK_TRACE}
     *   <li>{@link #COMPACT                 COMPACT}
     *   <li>{@link #DEFAULT                 null}
     * </ul>
     * @see #format(LogRecord)
     */
    public
    PrintfFormatter(String format) {
        this.format = PrintfFormatter.cookFormat(format);
    }

    /**
     * Sets the format string for this logger.
     * <p>
     *   The following special format string values designate certain "predefined" formats:
     * </p>
     * <ul>
     *   <li>{@link #BENCHMARK               "BENCHMARK"}</li>
     *   <li>{@link #COMPACT                 "COMPACT"}</li>
     *   <li>{@link #MESSAGE                 "MESSAGE"}</li>
     *   <li>{@link #MESSAGE_AND_EXCEPTION   "MESSAGE_AND_EXCEPTION"}</li>
     *   <li>{@link #MESSAGE_AND_STACK_TRACE "MESSAGE_AND_STACK_TRACE"}</li>
     *   <li>{@link #SIMPLE                  "SIMPLE"}</li>
     *   <li>{@code null} (means {@link #DEFAULT})</li>
     *   <li>{@link #DEFAULT "DEFAULT"}</li>
     * </ul>
     *
     * @see java.util.Formatter   The syntax of the <var>format</var> string
     * @see #format(LogRecord)    The arguments' meanings
     */
    public final void
    setFormat(String format) { this.format = PrintfFormatter.cookFormat(format); }

    private static String
    cookFormat(@Nullable String format) {

        return (
            format == null                           ? PrintfFormatter.FORMAT_STRING_DEFAULT :
            "BENCHMARK".equals(format)               ? PrintfFormatter.FORMAT_STRING_BENCHMARK :
            "COMPACT".equals(format)                 ? PrintfFormatter.FORMAT_STRING_COMPACT :
            "DEFAULT".equals(format)                 ? PrintfFormatter.FORMAT_STRING_DEFAULT :
            "MESSAGE".equals(format)                 ? PrintfFormatter.FORMAT_STRING_MESSAGE :
            "MESSAGE_AND_EXCEPTION".equals(format)   ? PrintfFormatter.FORMAT_STRING_MESSAGE_AND_EXCEPTION :
            "MESSAGE_AND_STACK_TRACE".equals(format) ? PrintfFormatter.FORMAT_STRING_MESSAGE_AND_STACK_TRACE :
            "SIMPLE".equals(format)                  ? PrintfFormatter.FORMAT_STRING_SIMPLE :
            format
        );
    }

    /**
     * @return The currently configured format string.
     */
    public String
    getFormat() {
        return this.format;
    }

    /**
     * Formats a {@link LogRecord} with a PRINTF format string.
     * <table border="1" cellpadding="3" cellspacing="0">
     *   <tr class="TableSubHeadingColor"><th>Placeholder</th><th>Description</th><th>Example</th></tr>
     *   <tr>
     *     <td>%1$d</td>
     *     <td>Sequence number</td>
     *     <td>&nbsp;</td>
     *   </tr>
     *   <tr>
     *     <td>%2$s</td>
     *     <td>Logger name</td>
     *     <td>&nbsp;</td>
     *   </tr>
     *   <tr>
     *     <td>%3$d</td>
     *     <td>Thread ID</td>
     *     <td>{@code 1}</td>
     *   </tr>
     *   <tr>
     *     <td>%4$s</td>
     *     <td>Level</td>
     *     <td>{@link java.util.logging.Level#FINE FINE}</td>
     *   </tr>
     *   <tr>
     *     <td>%5$tF&nbsp;%5$tT.%5$tL</td>
     *     <td>Date/time</td>
     *     <td>{@code 2012-03-08 10:10:28.468}</td>
     *   </tr>
     *   <tr>
     *     <td>%6$s</td>
     *     <td>Source class name</td>
     *     <td>{@code pkg.MyClass}</td>
     *   </tr>
     *   <tr>
     *     <td>%7$s</td>
     *     <td>Source method name</td>
     *     <td>{@code main}</td>
     *   </tr>
     *   <tr>
     *     <td>%8$s</td>
     *     <td>Message</td>
     *     <td>&nbsp;</td>
     *   </tr>
     *   <tr valign="top">
     *     <td>%9$s</td>
     *     <td>
     *       The empty string iff the log record contains no throwable, otherwise:
     *       <ul>
     *         <li>A colon</li>
     *         <li>A line break</li>
     *         <li>The throwable type name</li>
     *         <li>Another colon</li>
     *         <li>A space</li>
     *         <li>The throwable's message</li>
     *         <li>Another line break</li>
     *         <li>The throwable's stack trace (without the trailing line break)</li>
     *     </td>
     *     <td>
     *       <code>
     *         <i>preceeding-text</i>:<br>
     *         pkg.MyException: Exception message<br>
     *         &nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;pkg.Class.method(File.java:123)<br>
     *         &nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;Main.main(Main.java:20)
     *       </code>
     *     </td>
     *   </tr>
     *   <tr>
     *     <td>%10$s</td>
     *     <td>Simple source class name</td>
     *     <td>{@code MyClass}</td>
     *   </tr>
     *   <tr>
     *     <td>%11$s</td>
     *     <td>Colon, space, throwable converted to string (typically class name, colon, space, localized message)</td>
     *     <td>
     *       <code>
     *         <i>preceeding-text</i>: pkg.MyEception
     *       </code>
     *     </td>
     *   </tr>
     *   <tr valign="top">
     *     <td>%12$s</td>
     *     <td>Real, CPU and user time in milliseconds since this thread last logged to this handler</td>
     *     <td><pre>3.000000;    2.000000;    1.000000</pre></td>
     *   </tr>
     *   <tr>
     *     <td>%n</td>
     *     <td>Line separator</td>
     *     <td>&nbsp;</td>
     *   </tr>
     *   <tr>
     *     <td>%%</td>
     *     <td>Percent character ('%')</td>
     *     <td>&nbsp;</td>
     *   </tr>
     * </table>
     */
    @Override public String
    format(@Nullable LogRecord record) {

        if (record == null) return "null";

        Throwable thrown = record.getThrown();

        String thrownText;
        {
            if (thrown == null) {
                thrownText = "";
            } else {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);

                pw.println(":");

                pw.print(thrown.getClass().getName());
                pw.print(':');
                String message = thrown.getLocalizedMessage();
                if (message != null) {
                    pw.print(' ');
                    pw.print(message);
                }
                pw.println();

                thrown.printStackTrace(pw);

                thrownText = sw.toString();
                thrownText = StringUtil.lessTrailingLineSeparators(thrownText);
            }
        }

        String sourceClassName       = record.getSourceClassName();
        String simpleSourceClassName = (
            sourceClassName == null
            ? null
            : sourceClassName.substring(sourceClassName.lastIndexOf('.') + 1)
        );

        return String.format(
            this.format,
            record.getSequenceNumber(),                     // %1$d
            record.getLoggerName(),                         // %2$s
            record.getThreadID(),                           // %3$d
            record.getLevel(),                              // %4$s
            new Date(record.getMillis()),                   // %5$tT
            sourceClassName,                                // %6$s
            record.getSourceMethodName(),                   // %7$s
            this.formatMessage(record),                     // %8$s
            thrownText,                                     // %9$s
            simpleSourceClassName,                          // %10$s
            thrown == null ? "" : ": " + thrown.toString(), // %11$s
            PrintfFormatter.THE_BENCHMARK                   // %12$s
        );
    }
}
