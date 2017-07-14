
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

package de.unkrig.commons.util.logging.handler;

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * Adds an 'autoFlush' feature to the {@link java.util.logging.StreamHandler}, and a one-arg constructor with a
 * <i>variable</i> property name prefix.
 */
public abstract
class AbstractStreamHandler extends java.util.logging.StreamHandler {

    // Declare a set of constants for the handler property default values, so they can be used with the {@value} doc
    // tag.
    // SUPPRESS CHECKSTYLE Javadoc:OFF
    public static final Level            DEFAULT_LEVEL      = Level.INFO;
    public static final boolean          DEFAULT_AUTO_FLUSH = true;
    @Nullable public static final Filter DEFAULT_FILTER     = null;
    public static final SimpleFormatter  DEFAULT_FORMATTER  = new SimpleFormatter();
    @Nullable public static final String DEFAULT_ENCODING   = null;
    // SUPPRESS CHECKSTYLE Javadoc:ON

    private boolean autoFlush;

    public
    AbstractStreamHandler() throws ParseException, EvaluationException { this(null); }

    /**
     * One-arg constructor to be used by derived classes.
     */
    public
    AbstractStreamHandler(@Nullable String propertyNamePrefix) throws ParseException, EvaluationException {

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        // We cannot be sure how the LogManager processes exceptions thrown by this constructor, so we print a stack
        // trace to STDERR before we rethrow the exception.
        // (The JRE default log manager prints a stack trace, too, so we'll see two.)
        try {
            // SUPPRESS CHECKSTYLE LineLength:6
            this.init(
                LogUtil.getLoggingProperty(propertyNamePrefix + ".autoFlush",                  AbstractStreamHandler.DEFAULT_AUTO_FLUSH),
                LogUtil.getLoggingProperty(propertyNamePrefix + ".level",                      AbstractStreamHandler.DEFAULT_LEVEL),
                LogUtil.getLoggingProperty(propertyNamePrefix + ".filter",    Filter.class),   // DEFAULT_FILTER == null
                LogUtil.getLoggingProperty(propertyNamePrefix + ".formatter", Formatter.class, AbstractStreamHandler.DEFAULT_FORMATTER),
                LogUtil.getLoggingProperty(propertyNamePrefix + ".encoding")                   // DEFAULT_ENCODING == null
            );
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

    public
    AbstractStreamHandler(
        boolean          autoFlush,
        Level            level,
        @Nullable Filter filter,
        Formatter        formatter,
        @Nullable String encoding
    ) { this.init(autoFlush, level, filter, formatter, encoding); }

    @Override public synchronized void
    publish(@Nullable LogRecord record) {
        super.publish(record);
        if (this.autoFlush) this.flush();
    }

    private void
    init(
        boolean             autoFlush,
        Level               level,
        @Nullable Filter    filter,
        @Nullable Formatter formatter,
        @Nullable String    encoding
    ) {

        this.autoFlush = autoFlush;

        // Re-configure the underlying 'java.util.logging.StreamHandler' with the CORRECT values.
        this.setLevel(level);
        this.setFilter(filter);
        this.setFormatter(formatter);
        try {
            this.setEncoding(encoding);
        } catch (Exception e) {
            try { this.setEncoding(null); } catch (Exception e2) {}
        }
    }
}
