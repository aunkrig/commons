
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

package de.unkrig.commons.util.logging.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A {@link Filter} that filters {@link LogRecord}s by an ".condition" property like
 * <pre>
 *   method == "foo" &amp;&amp; message =~ ".*bla.*"
 * </pre>
 * The following variables are supported:
 * <table border="1">
 *   <tr><th>Name</th><th>Contents</th></tr>
 *   <tr><td>{@code name}</td><td>The logger's name</td></tr>
 *   <tr><td>{@code message}</td><td>The log message (before parameter substitution)</td></tr>
 *   <tr><td>{@code class}</td><td>The fully qualified name of the logged class</td></tr>
 *   <tr><td>{@code method}</td><td>The name of the logged method</td></tr>
 *   <tr><td>{@code level}</td><td>The log level name</td></tr>
 *   <tr><td>{@code parameter0},<br>{@code parameter1},<br>...</td><td>Parameter #0, #1, ...</td></tr>
 * </table>
 *
 * @see ExpressionEvaluator
 */
public
class ByExpressionFilter implements Filter {

    private Expression condition;

    /**
     * Zero-args constructor for the log manager.
     */
    public
    ByExpressionFilter() throws ParseException {
        this((String) null);
    }

    /**
     * One-arg constructor for proxies
     */
    public
    ByExpressionFilter(@Nullable String propertyNamePrefix) throws ParseException {

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        // Unfortunately, the default 'LogManager.getFilterProperty()' silently ignores any exceptions being thrown
        // by any filter zero-arg constructors, which makes debugging of logging configurations next to impossible,
        // so print a stack trace to STDERR before rethrowing the exception.
        try {
            this.condition = LogUtil.parseLoggingProperty(
                propertyNamePrefix + ".condition",
                "name", "level", "class", "method", "message", "params" // SUPPRESS CHECKSTYLE Wrap
            );
        } catch (ParseException pe) {
            pe.printStackTrace();
            throw pe;
        } catch (RuntimeException re) {
            re.printStackTrace();
            throw re;
        }
    }

    /**
     * @see ByExpressionFilter
     */
    public
    ByExpressionFilter(@Nullable Expression condition) {
        if (condition == null) throw new NullPointerException("condition");
        this.condition = condition;
    }

    @NotNullByDefault(false) @Override public boolean
    isLoggable(final LogRecord record) {

        try {
            return ExpressionEvaluator.toBoolean(this.condition.evaluate(
                "name",    record.getLoggerName(), // SUPPRESS CHECKSTYLE Wrap:5
                "level",   record.getLevel().getName(),
                "class",   record.getSourceClassName(),
                "method",  record.getSourceMethodName(),
                "message", record.getMessage(),
                "params",  record.getParameters()
            ));
        } catch (EvaluationException e) {
            return false;
        }
    }
}
