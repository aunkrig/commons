
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

package de.unkrig.commons.util.logging.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil.Produmer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.TimeTable;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A log record handler which writes formatted records to files which are automatically archived.
 * <p>
 *   Intially, if the "current log file" exists, it renames it (or, if it already exists and the "append" option is
 *   {@code true}, opens it in "append" mode).
 * </p>
 * <p>
 *   Initially, the "current log file" is renamed and re-created
 *   and log records are written to it. When it is time to "archive" the current log file,
 *   then it is closed, renamed and re-created.
 * </p>
 *
 * @see #ArchivingFileHandler() The zero-parameter constructor that is used for properties-based configuration
 *                              (as through the "{@code logging.properties}" file)
 */
public
class ArchivingFileHandler extends AbstractStreamHandler {

    /**
     * The zero-parameter constructor (which is used by the {@link LogManager}).
     * The {@link ArchivingFileHandler}, like all {@code java.util.logging} handlers, initializes itself with a set of
     * "logging properties" which it retrieves through the {@link LogManager}, which normally reads them from the
     * "{@code logging.properties}" file:
     * <dl>
     *   <dt><var>class-name</var>{@code .pattern}</dt>
     *   <dd>
     *     Determines the pathes of the log files; see the <var>pattern</var> parameter of {@link
     *     #ArchivingFileHandler(String, long, TimeTable, boolean, boolean, Level, Filter, Formatter, String)}.
     *     Defaults to {@value #DEFAULT_PATTERN}.
     *   </dd>
     *   <dt><var>class-name</var>{@code .sizeLimit}</dt>
     *   <dd>
     *     Determines that log file archiving triggers when the size of the current log file exceeds a given threshold;
     *     an integral value, optionally followed by one of "k", "M", "G", "T", "P". Defaults to "none".
     *   </dd>
     *   <dt><var>class-name</var>{@code .timeTable}</dt>
     *   <dd>
     *     An expression that defines the {@link TimeTable} for log file archiving with {@link Parser this syntax}.
     *     Defaults to "never".
     *     <br />
     *     Example: "{@code de.unkrig.commons.util.TimeTable.parse("*:0")}" for "every full hour".
     *   </dd>
     *   <dt><var>class-name</var>{@code .append}</dt>
     *   <dd>
     *     Whether to append to the current log file (if it initially exists); defaults to {@code false}.
     *   </dd>
     * </dl>
     */
    public
    ArchivingFileHandler() throws ParseException, EvaluationException, IOException {
        this(0, null);
    }

    /**
     * Single-arg constructor to be used by proxies.
     * Constructor for extending classes that wish to impose different logging property than that used
     * by {@link #ArchivingFileHandler()}.
     *
     * @param dummy              Only there to distinguish this constructor from {@link #ArchivingFileHandler(String)}
     * @param propertyNamePrefix The property name prefix, or {@code null} to use the qualified name of the
     *                           <em>actual</em> handler class
     */
    protected
    ArchivingFileHandler(int dummy, @Nullable String propertyNamePrefix)
    throws ParseException, EvaluationException, IOException {
        super(propertyNamePrefix);

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        // We cannot be sure how the LogManager processes exceptions thrown by this constructor, so we print a stack
        // trace to STDERR before we rethrow the exception.
        // (The JRE default log manager prints a stack trace, too, so we'll see two.)
        try {

            this.fileNamePattern = ArchivingFileHandler.preprocessPattern(
                LogUtil.getLoggingProperty(propertyNamePrefix + ".pattern", ArchivingFileHandler.DEFAULT_PATTERN)
            );
            this.currentFile = new File(ArchivingFileHandler.replaceAll(this.fileNamePattern, "%d", ""));

            // Remember size limit and time table.
            this.sizeLimit = LogUtil.getLoggingProperty(
                propertyNamePrefix + ".sizeLimit", ArchivingFileHandler.DEFAULT_SIZE_LIMIT
            );
            this.timeTable = LogUtil.getLoggingProperty(
                propertyNamePrefix + ".timeTable",
                TimeTable.class,
                ArchivingFileHandler.DEFAULT_TIME_TABLE
            );
            this.init(LogUtil.getLoggingProperty(propertyNamePrefix + ".append", false));
            this.nextArchiving = this.timeTable.next(new Date());
        } catch (ParseException pe) {
            pe.printStackTrace();
            throw pe;
        } catch (EvaluationException ee) {
            ee.printStackTrace();
            throw ee;
        } catch (RuntimeException re) {
            re.printStackTrace();
            throw re;
        }
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * {@link #DEFAULT_SIZE_LIMIT}{@code ,}
     * {@link #DEFAULT_TIME_TABLE}{@code ,}
     * {@value #DEFAULT_APPEND}{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern) throws IOException {
        this(
            pattern,                                  // pattern
            ArchivingFileHandler.DEFAULT_SIZE_LIMIT,  // sizeLimit
            ArchivingFileHandler.DEFAULT_TIME_TABLE,  // timeTable
            ArchivingFileHandler.DEFAULT_APPEND,      // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * {@value #DEFAULT_SIZE_LIMIT}{@code ,}
     * {@link #DEFAULT_TIME_TABLE}{@code ,}
     * <var>append</var>{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern, boolean append) throws IOException {
        this(
            pattern,            // pattern
            ArchivingFileHandler.DEFAULT_SIZE_LIMIT, // sizeLimit
            ArchivingFileHandler.DEFAULT_TIME_TABLE, // timeTable
            append,             // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * <var>sizeLimit</var>{@code ,}
     * {@link #DEFAULT_TIME_TABLE}{@code ,}
     * {@value de.unkrig.commons.util.logging.handler.ArchivingFileHandler#DEFAULT_APPEND}{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern, long sizeLimit) throws IOException {
        this(
            pattern,                                  // pattern
            sizeLimit,                                // sizeLimit
            ArchivingFileHandler.DEFAULT_TIME_TABLE,  // timeTable
            ArchivingFileHandler.DEFAULT_APPEND,      // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * <var>sizeLimit</var>{@code ,}
     * {@link #DEFAULT_TIME_TABLE}{@code ,}
     * <var>append</var>{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern, long sizeLimit, boolean append) throws IOException {
        this(
            pattern,                                  // pattern
            sizeLimit,                                // sizeLimit
            ArchivingFileHandler.DEFAULT_TIME_TABLE,  // timeTable
            append,                                   // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * {@value #DEFAULT_SIZE_LIMIT}{@code ,}
     * <var>timeTable</var>{@code ,}
     * {@value #DEFAULT_APPEND}{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern, TimeTable timeTable) throws IOException {
        this(
            pattern,                                  // pattern
            ArchivingFileHandler.DEFAULT_SIZE_LIMIT,  // sizeLimit
            timeTable,                                // timeTable
            ArchivingFileHandler.DEFAULT_APPEND,      // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * Equivalent with {@code ArchivingFileHandler(}<var>pattern</var>{@code ,}
     * {@value #DEFAULT_SIZE_LIMIT}{@code ,}
     * <var>timeTable</var>{@code ,}
     * <var>append</var>{@code ,}
     * {@value AbstractStreamHandler#DEFAULT_AUTO_FLUSH}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_LEVEL}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FILTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_FORMATTER}{@code ,}
     * {@link AbstractStreamHandler#DEFAULT_ENCODING}{@code )}.
     */
    public
    ArchivingFileHandler(String pattern, TimeTable timeTable, boolean append) throws IOException {
        this(
            pattern,                                  // pattern
            ArchivingFileHandler.DEFAULT_SIZE_LIMIT,  // sizeLimit
            timeTable,                                // timeTable
            append,                                   // append
            AbstractStreamHandler.DEFAULT_AUTO_FLUSH, // autoFlush
            AbstractStreamHandler.DEFAULT_LEVEL,      // level
            AbstractStreamHandler.DEFAULT_FILTER,     // filter
            AbstractStreamHandler.DEFAULT_FORMATTER,  // formatter
            AbstractStreamHandler.DEFAULT_ENCODING    // encoding
        );
    }

    /**
     * The <var>pattern</var> parameter determines the pathes of the log files, relative to the "user directory" (on
     * most systems: the "current working directory"), with the following placeholder replacements:
     * <dl>
     *   <dt>{@code %%}</dt>
     *   <dd>A literal {@code "%"}</dd>
     *
     *   <dt>{@code %h}</dt>
     *   <dd>The "user home directory" (as determined by the "{@code user.home}" system property)</dd>
     *
     *   <dt>{@code %t}</dt>
     *   <dd>The "default temp file path" (as determined by the "{@code java.io.tmpdir}" system property)</dd>
     *
     *   <dt>{@code %d}</dt>
     *   <dd>
     *     The date and time of the archiving, in the {@link DateFormat} {@code _yyyy-MM-dd_HH-mm-ss}. For the
     *     "current log file", "{@code %d}" is replaced with <em>the empty string</em>.
     *     <br />
     *     Iff the pattern does not contain a "{@code %d}" placeholder, then "{@code %d}" is silently appended to the
     *     pattern.
     *   </dd>
     * </dl>
     *
     * @param pattern   See the {@link #ArchivingFileHandler(String, long, TimeTable, boolean, boolean, Level, Filter,
     *                  Formatter, String) method description}
     * @param sizeLimit The size limit for the current file
     * @param timeTable The time table for time-based archiving
     * @param append    Whether to append to the current log file (if it initially exists)
     * @param autoFlush See {@link StreamHandler#StreamHandler(OutputStream, boolean, Level, Filter, Formatter, String)}
     * @param level     See {@link StreamHandler#StreamHandler(OutputStream, boolean, Level, Filter, Formatter, String)}
     * @param filter    See {@link StreamHandler#StreamHandler(OutputStream, boolean, Level, Filter, Formatter, String)}
     * @param formatter See {@link StreamHandler#StreamHandler(OutputStream, boolean, Level, Filter, Formatter, String)}
     * @param encoding  See {@link StreamHandler#StreamHandler(OutputStream, boolean, Level, Filter, Formatter, String)}
     */
    public
    ArchivingFileHandler(
        String           pattern,
        long             sizeLimit,
        TimeTable        timeTable,
        boolean          append,
        boolean          autoFlush,
        Level            level,
        @Nullable Filter filter,
        Formatter        formatter,
        @Nullable String encoding
    ) throws IOException {
        super(autoFlush, level, filter, formatter, encoding);

        this.fileNamePattern = ArchivingFileHandler.preprocessPattern(pattern);
        this.currentFile     = new File(ArchivingFileHandler.replaceAll(this.fileNamePattern, "%d", ""));

        // Remember size limit and time table.
        this.sizeLimit = sizeLimit;
        this.timeTable = timeTable;
        this.init(append);
        this.nextArchiving = this.timeTable.next(new Date());
    }

    @Override public synchronized void
    publish(@Nullable LogRecord record) {

        // Check the current time.
        if (new Date().compareTo(this.nextArchiving) >= 0) {
            try {
                this.archive(this.nextArchiving);
            } catch (Exception e) {
                this.reportError(null, e, ErrorManager.WRITE_FAILURE);
            }
        }

        super.publish(record);

        // Check the current file size.
        Long fileSize = this.byteCount.produce();
        assert fileSize != null;
        if (fileSize >= this.sizeLimit) {
            try {
                this.archive(new Date());
            } catch (Exception e) {
                this.reportError(null, e, ErrorManager.WRITE_FAILURE);
            }
        }
    }

    // CONFIGURATION

    /**
     * The pattern for the archive file names: Contains '%d', which must be replaced with the current date.
     */
    private String fileNamePattern;

    /**
     * The size limit for the current file.
     */
    private long sizeLimit;

    /**
     * The time table for time-based archiving.
     */
    private TimeTable timeTable;

    // STATE

    /**
     * Name of the "current file", i.e. the file that is written to, as opposed to the "archive files".
     */
    private File currentFile;

    /**
     * The point-in-time of the next time table-based archiving.
     */
    private Date nextArchiving;

    /**
     * Tracks the size of the current file.
     */
    private Produmer<Long, Number> byteCount = ConsumerUtil.cumulate(0);

    // CONSTANTS

    /**
     * A special value for the <var>sizeLimit</var> paramter of {@link ArchivingFileHandler#ArchivingFileHandler(String,
     * long, TimeTable, boolean, boolean, Level, Filter, Formatter, String)} indicating that no limit should apply.
     */
    public static final long NO_LIMIT = Long.MAX_VALUE;

    /**
     * The {@link SimpleDateFormat} pattern for the '%d' variable.
     */
    private static final String DATE_PATTERN = "_yyyy-MM-dd_HH-mm-ss";

    // Declare constants for the handler propery default values, so they can be used with the {@value} doc tag.
    // Must be public so that "@value" works for them.
    // SUPPRESS CHECKSTYLE JavadocVariable:4
    public static final String    DEFAULT_PATTERN    = "%h/java%d.log";
    public static final long      DEFAULT_SIZE_LIMIT = ArchivingFileHandler.NO_LIMIT;
    public static final TimeTable DEFAULT_TIME_TABLE = TimeTable.NEVER;
    public static final boolean   DEFAULT_APPEND     = false;

    // IMPLEMENTATION

    private void
    init(boolean append) throws IOException {

        // Rename an existing "current file" if we're not in APPEND mode or the file is too large.
        if (this.currentFile.exists() && (!append || this.currentFile.length() >= this.sizeLimit)) {
            if (!this.currentFile.renameTo(this.getArchiveFile(new Date()))) {

                // For whatever reason, the current file could not be renamed. Most likely (under Windows) another
                // program (or logger) has the file open.
                // The fallback strategy is to forget about size limit, appending etc. and start appending to the
                // current log file.
                this.sizeLimit     = ArchivingFileHandler.NO_LIMIT;
                this.nextArchiving = TimeTable.MAX_DATE;
                return;
            }
        }

        this.openCurrentFile();
    }

    private static String
    preprocessPattern(String pattern) {

        // Preprocess the file name pattern.
        pattern = pattern.replace('/', File.separatorChar);

        // Temporarily replace "%%" with "\u0001" to prevent misinterpretation of, e.g. "%%h".
        pattern = ArchivingFileHandler.replaceAll(pattern, "%%", "\u0001");
        {
            pattern = ArchivingFileHandler.replaceAll(
                pattern,
                "%h",
                System.getProperty("user.home", ".") + File.separatorChar
            );
            pattern = ArchivingFileHandler.replaceAll(
                pattern,
                "%t",
                System.getProperty("java.io.tmpdir", ".") + File.separatorChar
            );
            if (!pattern.contains("%d")) pattern += "%d";
        }
        pattern = pattern.replace('\u0001', '%');

        return pattern;
    }

    /**
     * @return A file that is guaranteed not to exist
     */
    private File
    getArchiveFile(Date date) {

        // Compute the archiv file name.
        String archiveFileName = ArchivingFileHandler.replaceAll(
            this.fileNamePattern,
            "%d",
            new SimpleDateFormat(ArchivingFileHandler.DATE_PATTERN).format(date)
        );
        File archiveFile = new File(archiveFileName);

        // If log files rotate very quickly, there might be a name clash. Append an integer number to the archive file
        // name to make it unique.
        if (archiveFile.exists()) {
            for (int uniqueId = 1;; uniqueId++) {
                File alternativeArchiveFile = new File(archiveFileName + '.' + uniqueId);
                if (!alternativeArchiveFile.exists()) {
                    archiveFile = alternativeArchiveFile;
                    break;
                }
            }
        }

        return archiveFile;
    }

    private void
    openCurrentFile() throws IOException {

        this.setOutputStream(OutputStreams.tee(
            new FileOutputStream(this.currentFile, true),
            OutputStreams.lengthWritten((this.byteCount = ConsumerUtil.cumulate(0)))
        ));
    }

    /**
     * Closes the current file, renames it, and recreates the current file.
     */
    private void
    archive(Date date) throws IOException {

        // Close the current file.
        this.close();

        // Archive (rename) the current file.
        if (!this.currentFile.renameTo(this.getArchiveFile(date))) {

            // For whatever reason, the current file could not be renamed. Most likely (under Windows) another program
            // (or logger) has the file open.
            // The fallback strategy is to forget about size limits, appending etc. and re-open the current log file.
            this.sizeLimit     = ArchivingFileHandler.NO_LIMIT;
            this.nextArchiving = TimeTable.MAX_DATE;
        } else {
            this.nextArchiving = this.timeTable.next(new Date());
        }

        // Re-open/re-create the current file.
        this.openCurrentFile();
    }

    /**
     * Replace all occurrences of <var>infix</var> within <var>subject</var> with the given <var>replacement</var>.
     */
    private static String
    replaceAll(String subject, String infix, String replacement) {
        for (int idx = subject.indexOf(infix); idx != -1; idx = subject.indexOf(infix, idx + replacement.length())) {
            subject = subject.substring(0, idx) + replacement + subject.substring(idx + infix.length());
        }
        return subject;
    }
}
