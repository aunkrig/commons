
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

import java.io.OutputStream;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A log record handler which writes formatted records to an {@link OutputStream} which must be specified through
 * a ".outputStream" logging property.
 * <p>
 * Example 'logging.properties' setup:
 * <pre>
 * .handlers = de.unkrig.commons.util.logging.handler.StreamHandler
 * de.unkrig.commons.util.logging.handler.StreamHandler.outputStream=\
 *    de.unkrig.commons.net.stream.PassiveSocketOutputStream(9999)
 * </pre>
 */
public
class StreamHandler extends AbstractStreamHandler {

    /**
     * No-arg constructor for the {@link java.util.logging.LogManager}.
     */
    public
    StreamHandler() throws ParseException, EvaluationException {
        this(null);
    }

    /**
     * Single-arg constructor to be used by proxies.
     */
    public
    StreamHandler(@Nullable String propertyNamePrefix) throws ParseException, EvaluationException {
        super(propertyNamePrefix);

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        // We cannot be sure how the LogManager processes exceptions thrown by this constructor, so we print a stack
        // trace to STDERR before we rethrow the exception.
        // (The JRE default log manager prints a stack trace, too, so we'll see two.)
        OutputStream os;
        try {
            os = LogUtil.getLoggingProperty(propertyNamePrefix + ".outputStream", OutputStream.class);
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

        this.setOutputStream(os);
    }

    public
    StreamHandler(
        OutputStream outputStream,
        boolean      autoFlush,
        Level        level,
        Filter       filter,
        Formatter    formatter,
        String       encoding
    ) {
        super(autoFlush, level, filter, formatter, encoding);
        this.setOutputStream(outputStream);
    }
}
