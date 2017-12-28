
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

import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An expression evaluates to a value each time it is {@link #evaluate(Object...) evaluated}.
 * If you want to evaluate an expression only once, you better use {@link
 * de.unkrig.commons.text.expression.ExpressionEvaluator#evaluate(String, Mapping)}.
 *
 * @see Parser#parse()
 */
public
interface Expression {

    /**
     * Computes the value of the expression
     *
     * @param variables            The values of the variables that were named when the expression was parsed. Allowed
     *                             values are {@link String}, {@link Integer}, {@link Boolean}, any other {@code
     *                             Object}, or {@code null}.
     * @return                     A {@link String}, {@link Integer}, {@link Boolean}, any other {link Object}, or
     *                             {@code null}
     * @throws EvaluationException A problem occurred during evaluation, e.g. any array element access was attempted
     *                             and the index value could not be converted to {@code int}
     * @throws RuntimeException    Any other runtime exception that occurred while evaluating the expression, e.g. an
     *                             {@link IllegalArgumentException} when accessing an element of a non-array
     */
    @Nullable Object
    evaluate(Mapping<String, ?> variables) throws EvaluationException;

    /**
     * Computes the value of the expression. This method typically verifies that all required parameters exist in
     * <var>variableNamesAndValues</var> before evaluating it.
     *
     * @param variableNamesAndValues Pairs of variable names and values; the even elements' type must be {@link
     *                               String}, the odd elements' type must be {@link String}, {@link Integer}, {@link
     *                               Boolean}, any other {@code Object}, or {@code null}.
     * @return                       A {@link String}, {@link Integer}, {@link Boolean}, any other {link Object}, or
     *                               {@code null}
     * @throws EvaluationException   A problem occurred during evaluation, e.g. any array element access was attempted
     *                               and the index value could not be converted to {@code int}
     * @throws RuntimeException      Any other runtime exception that occurred while evaluating the expression, e.g. an
     *                               {@link IllegalArgumentException} when accessing an element of a non-array
     */
    @Nullable Object
    evaluate(Object... variableNamesAndValues) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateTo(Mapping, Class)
     */
    @Nullable <T> T
    evaluateTo(Mapping<String, ?> variables, Class<T> targetType) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateTo(Class, Object...)
     */
    @Nullable <T> T
    evaluateTo(Class<T> targetType, Object... variableNamesAndValues) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateToPrimitive(Mapping, Class)
     */
    <T> T
    evaluateToPrimitive(Mapping<String, ?> variables, Class<T> targetType) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateToPrimitive(Class, Object...)
     */
    <T> T
    evaluateToPrimitive(Class<T> targetType, Object... variableNamesAndValues) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateToBoolean(Mapping)
     */
    boolean
    evaluateToBoolean(Mapping<String, ?> variables) throws EvaluationException;

    /**
     * @see AbstractExpression#evaluateToBoolean(Object...)
     */
    boolean
    evaluateToBoolean(Object... variableNamesAndValues) throws EvaluationException;

    /**
     * An expression which always evaluates to {@code true}.
     */
    Expression TRUE = ExpressionUtil.constantExpression2(true);

    /**
     * An expression which always evaluates to {@code false}.
     * */
    Expression FALSE = ExpressionUtil.constantExpression2(false);

    /**
     * An expression which always evaluates to {@code null}.
     */
    Expression NULL = ExpressionUtil.constantExpression2(null);
}
