
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

package de.unkrig.commons.text.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.Scanner.TokenType;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StringScanner;

/**
 * Utility class for {@link Expression}.
 */
public final
class ExpressionUtil {

    private
    ExpressionUtil() {}

    /**
     * @return An {@link Expression} that evaluates to a constant value.
     */
    public static Expression
    constantExpression(@Nullable final Object value) {

        return new AbstractExpression() {

            @Nullable @Override public Object
            evaluate(Mapping<String, ?> variables) { return value; }

            @Override public String
            toString() {
                return (
                    value instanceof String    ? "\"" + value + '"' :
                    value instanceof Character ? "'" + value + '\'' :
                    value instanceof Long      ? value + "L" :
                    value instanceof Float     ? value + "F" :
                    value instanceof Double    ? value + "D" :
                    String.valueOf(value)
                );
            }
        };
    }

    private static Expression
    concat(final List<Expression> expressions) {
        if (expressions.size() == 1) return expressions.get(0);
        return new AbstractExpression() {

            @Override public Object
            evaluate(Mapping<String, ?> variables) throws EvaluationException {
                StringBuilder sb = new StringBuilder();
                for (Expression expression : expressions) {
                    sb.append(expression.evaluate(variables));
                }
                return sb.toString();
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder();
                for (Expression expression : expressions) {
                    sb.append('#').append(expression).append('#');
                }
                return sb.toString();
            }
        };
    }

    /**
     * Turns the given string into an expression. If the string contains '#' characters, then the text between two
     * '#' characters is parsed as an expression.
     *
     * @param s             The string to expand
     * @param variableNames All contained variable names can be used in the expression
     * @return              The expanded string
     */
    public static Expression
    expand(String s, final Set<String> variableNames) throws ParseException {

        return ExpressionUtil.expand(s, new Predicate<String>() {
            @Override public boolean evaluate(String subject) { return variableNames.contains(subject); }
        });
    }

    /**
     * Turns the given string into an expression. If the string contains '#' characters, then the text between two
     * '#' characters is parsed as an expression.
     *
     * @param s                   The string to expand
     * @param isValidVariableName Evalutaes to whether the {@code subject} is a valid variable name
     * @return                    The expanded string
     */
    public static Expression
    expand(String s, Predicate<? super String> isValidVariableName) throws ParseException {

        // Does the string contain a '#...#' expression at all?
        int idx = s.indexOf('#');
        if (idx == -1) return ExpressionUtil.constantExpression(s);

        List<Expression> expressions = new ArrayList<Expression>();
        if (idx != 0) expressions.add(ExpressionUtil.constantExpression(s.substring(0, idx)));

        for (;;) {

            // 'idx' points to the opening '#'.

            final String                   input         = s.substring(idx + 1);
            final StringScanner<TokenType> stringScanner = Scanner.stringScanner().setInput(input);
            Expression                     exp           = new ExpressionEvaluator(isValidVariableName).parse(
                new ProducerWhichThrows<Token<TokenType>, ScanException>() {

                    @Override @Nullable public Token<TokenType>
                    produce() throws ScanException {
                        if (input.charAt(stringScanner.getOffset()) == '#') return null;
                        return stringScanner.produce();
                    }

                    @Override @Nullable public String
                    toString() {
                        return stringScanner.toString();
                    }
                }
            );
            expressions.add(exp);
            idx += 1 + stringScanner.getOffset() + 1;
            if (idx >= s.length()) break;
            int idx2 = s.indexOf('#', idx);
            if (idx2 == -1) {
                expressions.add(ExpressionUtil.constantExpression(s.substring(idx)));
                break;
            }
            expressions.add(ExpressionUtil.constantExpression(s.substring(idx, idx2)));
            idx = idx2;
        }

        return ExpressionUtil.concat(expressions);
    }

    /**
     * @return The result of the evaluation, or a verbose HTML comment reporting the error
     */
    public static String
    evaluateLeniently(Expression expression, Mapping<String, ?> variables) {
        try {
            return String.valueOf(expression.evaluate(variables));
        } catch (EvaluationException ee) {
            return "<!-- " + ee.getMessage() + " -->";
        } catch (Exception e) {
            return "<!-- Evaluating '" + expression + "': " + e + " -->";
        }
    }

    /**
     * @return                     The evaluated value
     * @throws EvaluationException The expression evaluates to {@code null}
     * @throws EvaluationException A problem occurred during evaluation, e.g. a class could not be loaded, or a type
     *                             cast could not be performed
     */
    @Nullable public static <T> T
    evaluateTo(Expression expression, Mapping<String, ?> variables, Class<T> targetClass) throws EvaluationException {
        return ExpressionEvaluator.to(expression.evaluate(variables), targetClass);
    }

    /**
     * @param parameterName The name under which the predicate subject is accessible for the {@code expression}
     * @return              A {@link Predicate} which evaluates to the value of the given {@code expression}
     */
    public static <T> Predicate<T>
    toPredicate(final Expression expression, final String parameterName) {

        return new Predicate<T>() {

            @Override public boolean
            evaluate(T subject) {

                Object result;
                try {
                    result = expression.evaluate(Mappings.<String, Object>mapping(parameterName, subject));
                } catch (EvaluationException ee) {
                    throw new RuntimeException(ee);
                }

                return ExpressionEvaluator.toBoolean(result);
            }

            @Override public String
            toString() { return expression.toString(); }
        };
    }

    /**
     * @return An {@link Expression} which evaluates to the result of the given {@code predicate}, where the subject
     *         to the predicate is the value of the named variable of the expression
     */
    public static Expression
    fromPredicate(final Predicate<? super String> predicate, final String variableName) {

        return new AbstractExpression() {

            @Override @Nullable public Object
            evaluate(Mapping<String, ?> variables) throws EvaluationException {

                Object variableValue = variables.get(variableName);
                if (variableValue == null) throw new EvaluationException("Variable '" + variableName + "' not set");

                if (!(variableValue instanceof String)) {
                    throw new EvaluationException(
                        "Variable '"
                        + variableName
                        + "' has unexpected type '"
                        + variableValue.getClass()
                        + "'"
                    );
                }
                return predicate.evaluate((String) variableValue);
            }
        };
    }

    /**
     * @return An {@link Expression} which evaluates the {@code operand1}, and, if that is {@code true} the
     *         {@code operand2}
     */
    public static Expression
    logicalAnd(final Expression operand1, final Expression operand2) {

        if (operand1 == Expression.FALSE) return Expression.FALSE;
        if (operand1 == Expression.TRUE)  return operand2;

        if (operand2 == Expression.TRUE) return operand1;

        return new AbstractExpression() {

            @Override @Nullable public Object
            evaluate(Mapping<String, ?> variables) throws EvaluationException {

                return (
                    ExpressionEvaluator.toBoolean(operand1.evaluate(variables))
                    ? operand2.evaluate(variables)
                    : false
                );
            }
        };
    }

    /**
     * @return An {@link Expression} which evaluates the {@code operand1}, and, if that is {@code false} the
     *         {@code operand2}
     */
    public static Expression
    logicalOr(final Expression operand1, final Expression operand2) {

        if (operand1 == Expression.TRUE)  return Expression.TRUE;
        if (operand1 == Expression.FALSE) return operand2;

        if (operand2 == Expression.FALSE) return operand1;

        return new AbstractExpression() {

            @Override @Nullable public Object
            evaluate(Mapping<String, ?> variables) throws EvaluationException {

                return (
                    ExpressionUtil.evaluateTo(operand1, variables, boolean.class)
                    ? true
                    : operand2.evaluate(variables)
                );
            }
        };
    }
}
