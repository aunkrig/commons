
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link Formatter} calls one of two delegates, depending on the evaluation result of a {@link LogRecord} {@link
 * Predicate}.
 */
public
class SelectiveFormatter extends Formatter {

    private final Predicate<? super LogRecord> predicate;
    private final Formatter                    delegate1, delegate2;

    /**
     * @param delegate1 Is called for {@link LogRecord} for which the <var>predicate</var> evaluates to {@code true}
     * @param delegate2 Is called for {@link LogRecord} for which the <var>predicate</var> evaluates to {@code false}
     */
    public
    SelectiveFormatter(Predicate<? super LogRecord> predicate, Formatter delegate1, Formatter delegate2) {
        this.predicate = predicate;
        this.delegate1 = delegate1;
        this.delegate2 = delegate2;
    }

    /**
     * Calls <var>delegate1</var> if the <var>predicate</var> evaluates to {@code true} for the <var>logRecord</var>,
     * otherwise <var>delegate2</var>.
     *
     * @see #SelectiveFormatter(Predicate, Formatter, Formatter)
     */
    @Override public String
    format(@Nullable LogRecord logRecord) {

        if (logRecord == null) return "null";

        return (
            this.predicate.evaluate(logRecord)
            ? this.delegate1.format(logRecord)
            : this.delegate2.format(logRecord)
        );
    }

    /**
     * Returns a {@link Formatter} that for each {@link LogRecord} invokes <var>delegate1</var> if the level of the
     * processing {@link Logger} is greater than <var>threshold</var>, and for all other the <var>delegate2</var>.
     * <p>
     * A typical application is to format log records WITHOUT stack traces, except if the logger's level is set to
     * {@link Level#FINE} or lower.
     */
    public static final Formatter
    loggerLevelGreaterThan(final Level threshold, Formatter delegate1, Formatter delegate2) {
        return new SelectiveFormatter(new Predicate<LogRecord>() {

            @Override public boolean
            evaluate(@Nullable LogRecord logRecord) {

                // Getting the logger by name is not exactly superfast, but, hey, we're here for FORMATTING, which
                // is the second-but-last step in the logging chain, so the performance hit should be small.
                // Why the heck does the log record keep the logger NAME, and not a REFERENCE TO THE LOGGER? Is see
                // no good reasons for this choice. Bummer.
                return logRecord != null && !Logger.getLogger(logRecord.getLoggerName()).isLoggable(threshold);
            }
        }, delegate1, delegate2);
    }

    /**
     * Returns a {@link Formatter} that for each {@link LogRecord} invokes <var>delegate1</var> if the level of the log
     * record is greater than ("more severe than") <var>threshold</var>, and for all other the <var>delegate2</var>.
     * <p>
     * A typical application is to format log records WITHOUT stack traces, except if the logger's level is set to
     * {@link Level#FINE} or lower.
     */
    public static final Formatter
    logRecordLevelGreaterThan(final Level threshold, Formatter delegate1, Formatter delegate2) {
        return new SelectiveFormatter(new Predicate<LogRecord>() {

            @Override public boolean
            evaluate(@Nullable LogRecord logRecord) {
                return logRecord != null && logRecord.getLevel().intValue() > threshold.intValue();
            }
        }, delegate1, delegate2);
    }
}
