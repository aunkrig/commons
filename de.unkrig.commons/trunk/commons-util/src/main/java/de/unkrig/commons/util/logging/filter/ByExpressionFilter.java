
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

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.NotNull;
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
 *   method == "foo" && message =~ ".*bla.*"
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
@NotNullByDefault(false) public
class ByExpressionFilter implements Filter {

    private static final Predicate<? super String> IS_VALID_VARIABLE_NAME = new Predicate<String>() {

        @Override public boolean
        evaluate(@NotNull String variableName) {
            return (
                "name".equals(variableName)
                || "level".equals(variableName)
                || "class".equals(variableName)
                || "method".equals(variableName)
                || "message".equals(variableName)
                || "params".equals(variableName)
            );
        }
    };

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
    ByExpressionFilter(String propertyNamePrefix) throws ParseException {

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        // Unfortunately, the default 'LogManager.getFilterProperty()' silently ignores any exceptions being thrown
        // by any filter zero-arg constructors, which makes debugging of logging configurations next to impossible,
        // so print a stack trace to STDERR before rethrowing the exception.
        try {
            this.init(LogUtil.parseLoggingProperty(
                propertyNamePrefix + ".condition",
                ByExpressionFilter.IS_VALID_VARIABLE_NAME
            ));
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
    ByExpressionFilter(Expression condition) {
        this.init(condition);
    }

    private void
    init(Expression condition) {
        if (condition == null) throw new NullPointerException("condition");
        this.condition = condition;
    }

    @Override public boolean
    isLoggable(final LogRecord record) {

        Mapping<String, Object> variables = new Mapping<String, Object>() {

            @Override public boolean
            containsKey(@Nullable Object key) {
                return key instanceof String && ByExpressionFilter.IS_VALID_VARIABLE_NAME.evaluate((String) key);
            }

            @Override public Object
            get(@Nullable Object key) {
                return (
                    "name".equals(key)    ? record.getLoggerName() :
                    "level".equals(key)   ? record.getLevel().getName() :
                    "class".equals(key)   ? record.getSourceClassName() :
                    "method".equals(key)  ? record.getSourceMethodName() :
                    "message".equals(key) ? record.getMessage() :
                    "params".equals(key)  ? record.getParameters() :
                    ExceptionUtil.throW(new IllegalStateException("Value of variable '" + key + "' missing"))
                );
            }
        };

        try {
            return ExpressionEvaluator.toBoolean(this.condition.evaluate(variables));
        } catch (EvaluationException e) {
            return false;
        }
    }
}
