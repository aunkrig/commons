
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

package de.unkrig.commons.util.logging.formatter;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A {@link Formatter} that uses {@link PrettyPrinter} to beautify the log record's parameters, and then calls a
 * delegate {@link Formatter}.
 */
@NotNullByDefault(false) public
class PrettyPrintFormatter extends Formatter {

    private Formatter delegate;

    public
    PrettyPrintFormatter() throws ParseException, EvaluationException {
        String propertyNamePrefix = this.getClass().getName();
        this.init(LogUtil.getLoggingProperty(propertyNamePrefix + ".delegate", Formatter.class));
    }

    public
    PrettyPrintFormatter(Formatter delegate) {
        this.init(delegate);
    }

    private void
    init(Formatter delegate) {
        this.delegate = delegate;
    }

    @Override public String
    format(LogRecord record) {
        Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) return this.delegate.format(record);

        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = PrettyPrinter.toString(parameters[i]);
        }

        record.setParameters(parameters); // Not really necessary, but maybe some day...

        return this.delegate.format(record);
    }
}
