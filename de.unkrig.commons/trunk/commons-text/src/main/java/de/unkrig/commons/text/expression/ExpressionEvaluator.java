
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

package de.unkrig.commons.text.expression;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.PredicateWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.reflect.ReflectUtil;
import de.unkrig.commons.text.Notations;
import de.unkrig.commons.text.expression.Parser.BinaryOperator;
import de.unkrig.commons.text.expression.Parser.UnaryOperator;
import de.unkrig.commons.text.expression.Scanner.TokenType;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.Pattern2;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StringScanner;

/**
 * Supports two operation modes:
 * <ul>
 *   <li>Scans, parses and evaluates an expression immediately (see {@link #evaluate(String, Mapping)})</li>
 *   <li>
 *     Scans and parses an expression (see {@link #parse(String)}) into an {@link Expression} object for repeated
 *     evaluation (see {@link Expression#evaluate(Mapping)}).
 *   </li>
 * </ul>
 */
public
class ExpressionEvaluator {

    private String[]    imports     = new String[] { "java.lang" };
    private ClassLoader classLoader = this.getClass().getClassLoader();

    private final PredicateWhichThrows<? super String, ? extends RuntimeException>
    isValidVariableName;

    /**
     * @param isValidVariableName Evaluates whether a string is a valid variable name; if not, then the parser will
     *                            throw a {@link ParseException}
     */
    public
    ExpressionEvaluator(PredicateWhichThrows<? super String, ? extends RuntimeException> isValidVariableName) {
        this.isValidVariableName = isValidVariableName;
    }

    /**
     * @param variableNames Contains all valid variable names
     */
    public
    ExpressionEvaluator(Collection<String> variableNames) {
        this.isValidVariableName = PredicateUtil.contains(variableNames);
    }

    /**
     * @param variableNames The names of the variables that can be referred to in an expression
     */
    public
    ExpressionEvaluator(String... variableNames) {
        this.isValidVariableName = PredicateUtil.contains(Arrays.asList(variableNames));
    }

    /**
     * @return The currently configured imports
     */
    public String[]
    getImports() { return this.imports.clone(); }

    /**
     * @param imports Names of imported packages
     */
    public ExpressionEvaluator
    setImports(String[] imports) {
        this.imports = imports.clone();
        return this;
    }

    /**
     * @return  The currently configured {@link ClassLoader}
     */
    public ClassLoader
    getClassLoader() { return this.classLoader; }

    /**
     * @param classLoader Used to load classes named in the expression; by default the ClassLoader which loaded
     *                    the {@link ExpressionEvaluator} class.
     */
    public ExpressionEvaluator
    setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Parses an expression.
     *
     * @param spec The text to be parsed
     * @return     {@link Expression#TRUE} iff the <var>expression</var> is constant and evaluates to {@code true};
     *             {@link Expression#FALSE} iff the <var>expression</var> is constant and evaluates to {@code false};
     *             {@link Expression#NULL} iff the <var>expression</var> is constant and evaluates to {@code null}
     * @see Parser The expression syntax
     */
    public Expression
    parse(String spec) throws ParseException {
        return this.<RuntimeException>parser(Scanner.stringScanner().setInput(spec)).parse();
    }

    /**
     * Parses an expression from the <var>spec</var>, but only as far as it is possible without a parse error.
     * E.g. {@code "a + b)"} is parsed up to and including "b".
     *
     * @param spec            The text to be parsed
     * @param offset          Returns the position of the first character within the <var>space</var> that could not be
     *                        parsed
     * @throws ParseException The expression cannot be parsed, e.g. {@code "a +)"} (the second operand of the "+"
     *                        operator is missing)
     * @see Parser            The expression syntax
     */
    public Expression
    parsePart(CharSequence spec, int[] offset) throws ParseException {

        StringScanner<TokenType> scanner = Scanner.stringScanner().setInput(spec);

        Expression result = this.<RuntimeException>parser(scanner).parsePart();

        offset[0] = scanner.getPreviousTokenOffset();

        return result;
    }

    /**
     * Parses an expression from a <var>tokenProducer</var>.
     *
     * @see Parser The expression syntax
     */
    public Expression
    parse(ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> tokenProducer)
    throws ParseException {

        return this.<RuntimeException>parser(tokenProducer).parse();
    }

    /**
     * @param spec The text to be parsed
     * @param <EX> Irrelevant
     * @return     A {@link Parser} for expression parsing
     * @see Parser The expression syntax
     */
    public <EX extends Exception> Parser<Expression, EX>
    parser(String spec) {
        return this.parser(Scanner.stringScanner().setInput(spec));
    }

    /**
     * @param tokenProducer The source of tokens to be parsed, e.g. {@link Scanner#stringScanner()}
     * @param <EX>          Irrelevant
     * @return              A {@link Parser} for expression parsing
     * @see Parser          The expression syntax
     */
    @SuppressWarnings("null") public <EX extends Throwable> Parser<Expression, EX>
    parser(ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> tokenProducer) {

        return new Parser<Expression, EX>(tokenProducer) {

            @Override protected Expression
            conditional(final Expression lhs, final Expression mhs, final Expression rhs) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.conditional(
                            lhs.evaluate(variables),
                            mhs.evaluate(variables),
                            rhs.evaluate(variables)
                        );
                    }

                    @Override public String
                    toString() { return lhs + " ? " + mhs + " : " + rhs; }
                };
            }

            @Override protected Expression
            unaryOperation(final UnaryOperator operator, final Expression operand) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.unaryOperation(operator, operand.evaluate(variables));
                    }

                    @Override public String
                    toString() { return operator.toString() + operand; }
                };
            }

            @Override protected Expression
            binaryOperation(final Expression lhs, final BinaryOperator op, final Expression rhs) {

                // "ExpressionUtil.logicalAnd()/logicalOr()" implement valueable optimizations.
                switch (op) {
                case LOGICAL_AND: return ExpressionUtil.logicalAnd(lhs, rhs);
                case LOGICAL_OR:  return ExpressionUtil.logicalOr(lhs, rhs);
                default:          ;
                }

                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.binaryOperation(
                            lhs.evaluate(variables),
                            op,
                            rhs.evaluate(variables)
                        );
                    }

                    @Override public String
                    toString() { return lhs + " " + op + ' ' + rhs; }
                };
            }

            @Override protected Expression
            methodInvocation(final Expression target, final String methodName, final List<Expression> arguments) {

                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {

                        // Determine arguments' values.
                        List<Object> argumentValues = new ArrayList<Object>(arguments.size());
                        for (Expression argument : arguments) {
                            argumentValues.add(argument.evaluate(variables));
                        }

                        return ExpressionEvaluator.invokeMethod(
                            target.evaluate(variables),
                            methodName,
                            argumentValues
                        );
                    }

                    @Override public String
                    toString() {
                        return target + "." + methodName + '.' + '(' + StringUtil.join(arguments, ", ") + ')';
                    }
                };
            }

            @Override protected Expression
            staticMethodInvocation(final Class<?> target, final String methodName, final List<Expression> arguments) {

                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {

                        // Determine arguments' values.
                        List<Object> argumentValues = new ArrayList<Object>(arguments.size());
                        for (Expression argument : arguments) argumentValues.add(argument.evaluate(variables));

                        return ExpressionEvaluator.invokeStaticMethod(target, methodName, argumentValues);
                    }

                    @Override public String
                    toString() {
                        return target.getName() + "." + methodName + '.' + '(' + StringUtil.join(arguments, ", ") + ')';
                    }
                };
            }

            @Override protected Expression
            fieldReference(final Expression target, final String fieldName) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.<EvaluationException>getAttributeValue(
                            target.evaluate(variables),
                            fieldName
                        );
                    }

                    @Override public String
                    toString() { return target + "." + fieldName; }
                };
            }

            @Override protected Expression
            staticFieldReference(final Class<?> type, final String fieldName) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.<EvaluationException>getStaticAttributeValue(
                            type,
                            fieldName
                        );
                    }

                    @Override public String
                    toString() { return type.getName() + '.' + fieldName; }
                };
            }

            @Override protected Expression
            parenthesized(final Expression exp) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return exp.evaluate(variables);
                    }

                    @Override public String
                    toString() { return '(' + exp.toString() + ')'; }
                };
            }

            @Override protected Expression
            instanceoF(final Expression lhs, final Class<?> rhs) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.isInstanceOf(lhs.evaluate(variables), rhs);
                    }

                    @Override public String
                    toString() { return lhs + " instanceof " + rhs; }
                };
            }

            @Override protected Expression
            newClass(final Class<?> clasS, final List<Expression> arguments) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        List<Object> argumentValues = new ArrayList<Object>();
                        for (Expression argument : arguments) argumentValues.add(argument.evaluate(variables));
                        return ExpressionEvaluator.instantiateClass(clasS, argumentValues);
                    }

                    @Override public String
                    toString() {
                        return "new " + clasS.getName() + '(' + StringUtil.join(arguments, ", ") + ')';
                    }
                };
            }

            @Override protected Expression
            newArray(final Class<?> clasS, final List<Expression> dimensions) {
                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        int   n               = dimensions.size();
                        int[] dimensionValues = new int[n];
                        for (int i = 0; i < n; i++) {
                            dimensionValues[i] = ExpressionUtil.evaluateTo(
                                dimensions.get(i),
                                variables,
                                Integer.class
                            );
                        }

                        return ExpressionEvaluator.newArrayInstance(clasS, dimensionValues);
                    }

                    @Override public String
                    toString() {
                        int      brackets = 0;
                        Class<?> c        = clasS;
                        for (; c.isArray(); c = c.getComponentType()) brackets++;
                        StringBuilder sb = new StringBuilder(c.getName());
                        for (Expression dimension : dimensions) sb.append('[').append(dimension).append(']');
                        for (int i = 0; i < brackets; i++) sb.append("[]");
                        return sb.toString();
                    }
                };
            }

            @Override protected Expression
            cast(final Class<?> targetClass, final Expression rhs) {

                return new AbstractExpression() {

                    @Override @Nullable public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.cast(targetClass, rhs.evaluate(variables));
                    }

                    @Override public String
                    toString() { return '(' + targetClass.getName() + ") " + rhs; }
                };
            }

            @Override protected Expression
            literal(@Nullable Object o) { return ExpressionUtil.constantExpression(o); }

            @Override protected Expression
            variableReference(final String variableName) throws ParseException {

                if (!ExpressionEvaluator.this.isValidVariableName.evaluate(variableName)) {
                    throw new ParseException("Unknown variable '" + variableName + "'");
                }

                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) {

                        Object value = variables.get(variableName);

                        if (value == null && !variables.containsKey(variableName)) {
                            throw new IllegalStateException("Variable '" + variableName + "' missing");
                        }

                        return value;
                    }

                    @Override public String
                    toString() { return variableName; }
                };
            }

            @Override protected Expression
            arrayAccess(final Expression lhs, final Expression rhs) {

                return new AbstractExpression() {

                    @Override public Object
                    evaluate(Mapping<String, ?> variables) throws EvaluationException {
                        return ExpressionEvaluator.arrayAccess(lhs.evaluate(variables), rhs.evaluate(variables));
                    }

                    @Override public String
                    toString() { return lhs.toString() + '[' + rhs.toString() + ']'; }
                };
            }
        }.setImports(this.imports);
    }

    /**
     * Scans, parses and evaluates an expression.
     *
     * @throws ParseException <var>spec</var> refers to a variable which is not contained in <var>variables</var>
     * @throws ParseException Any other parse error
     * @see Parser            The expression syntax
     */
    @SuppressWarnings("null") @Nullable public Object
    evaluate(String spec, final Mapping<String, ?> variables) throws ParseException, EvaluationException {

        return new Parser<Object, EvaluationException>(spec) {

            @Override protected Object
            conditional(Object lhs, Object mhs, Object rhs) throws EvaluationException {
                return ExpressionEvaluator.conditional(lhs, mhs, rhs);
            }

            @Override protected Object
            unaryOperation(UnaryOperator operator, Object operand) throws EvaluationException {
                return ExpressionEvaluator.unaryOperation(operator, operand);
            }

            @Override protected Object
            binaryOperation(Object lhs, BinaryOperator op, Object rhs) throws EvaluationException {
                return ExpressionEvaluator.binaryOperation(lhs, op, rhs);
            }

            @Override protected Object
            methodInvocation(Object target, String methodName, List<Object> arguments) throws EvaluationException {
                return ExpressionEvaluator.invokeMethod(target, methodName, arguments);
            }

            @Override protected Object
            staticMethodInvocation(
                Class<?>     target,
                String       methodName,
                List<Object> arguments
            ) throws EvaluationException {
                return ExpressionEvaluator.invokeStaticMethod(target, methodName, arguments);
            }

            @Override protected Object
            fieldReference(Object target, String fieldName) throws EvaluationException {
                return ExpressionEvaluator.getAttributeValue(target, fieldName);
            }

            @Override protected Object
            staticFieldReference(Class<?> type, String fieldName) throws EvaluationException {
                return ExpressionEvaluator.getStaticAttributeValue(type, fieldName);
            }

            @Override protected Object
            parenthesized(Object exp) { return exp; }

            @Override protected Object
            instanceoF(@Nullable Object lhs, Class<?> rhs) {
                return ExpressionEvaluator.isInstanceOf(lhs, rhs);
            }

            @Override protected Object
            newClass(Class<?> clasS, List<Object> arguments) throws EvaluationException {
                return ExpressionEvaluator.instantiateClass(clasS, arguments);
            }

            @Override protected Object
            newArray(Class<?> clasS, List<Object> dimensions) {
                int   n               = dimensions.size();
                int[] dimensionValues = new int[n];
                for (int i = 0; i < n; i++) dimensionValues[i] = (Integer) dimensions.get(i);

                return ExpressionEvaluator.newArrayInstance(clasS, dimensionValues);
            }

            @Override protected Object
            cast(Class<?> targetClass, Object rhs) throws EvaluationException {
                return ExpressionEvaluator.cast(targetClass, rhs);
            }

            @Override protected Object
            literal(Object o) { return o; }

            @Override protected Object
            variableReference(String variableName) throws ParseException {

                Object value = variables.get(variableName);

                if (value == null && !variables.containsKey(variableName)) {
                    throw new ParseException("Unknown variable '" + variableName + "'");
                }

                return value;
            }

            @Override protected Object
            arrayAccess(Object lhs, Object rhs) throws EvaluationException {
                return ExpressionEvaluator.arrayAccess(lhs, rhs);
            }
        }.setImports(this.imports).parse();
    }

    private static Object
    newArrayInstance(Class<?> type, int[] dimensionValues) { return Array.newInstance(type, dimensionValues); }

    private static boolean
    isInstanceOf(@Nullable Object value, Class<?> type) {
        return value != null && type.isAssignableFrom(value.getClass());
    }

    private static Object
    instantiateClass(Class<?> clasS, List<Object> argumentValues) throws EvaluationException {
        try {

            // Find most specific constructor.
            Constructor<?> mostSpecificConstructor = ReflectUtil.getMostSpecificConstructor(
                clasS,
                ReflectUtil.getTypes(argumentValues)
            );

            // Create instance.
            return mostSpecificConstructor.newInstance(argumentValues.toArray());
        } catch (Exception e) {
            throw ExceptionUtil.wrap("Instantiating " + clasS, e, EvaluationException.class);
        }
    }

    @Nullable private static Object
    cast(Class<?> targetClass, @Nullable Object operand) throws EvaluationException {
        if (operand == null) return null;

        if (!targetClass.isAssignableFrom(operand.getClass())) {
            throw new EvaluationException(
                "Cannot cast '"
                + operand.getClass().getName()
                + "' to '"
                + targetClass.getName()
                + "'"
            );
        }

        return operand;
    }

    @Nullable private static Object
    arrayAccess(Object lhs, Object rhs) throws EvaluationException {

        Object  lhsv = ExpressionEvaluator.to(lhs, Object.class);
        Integer rhsv = ExpressionEvaluator.toPrimitive(rhs, int.class);

        return Array.get(lhsv, rhsv);
    }

    /**
     * @return {@code subject.toString()} or {@code null} iff {@code subject == null}
     */
    public static String
    toString(@Nullable Object subject) { return subject == null ? "" : subject.toString(); }

    @Nullable private static Object
    invokeMethod(@Nullable Object lhsv, String methodName, List<Object> argumentValues) throws EvaluationException {
        if (lhsv == null) return null;

        // Determine arguments' types.
        Class<?>[] argumentTypes = ReflectUtil.getTypes(argumentValues);

        // Determine target class.
        Class<?> clasS = lhsv.getClass();

        // Find the most specific method
        Method mostSpecificMethod;
        try {
            mostSpecificMethod = ReflectUtil.getMostSpecificMethod(clasS, methodName, argumentTypes);
        } catch (NoSuchMethodException nsme) {
            throw new EvaluationException(nsme);
        }

        // Invoke the method.
        try {
            return mostSpecificMethod.invoke(lhsv, argumentValues.toArray());
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    @Nullable private static Object
    invokeStaticMethod(Class<?> clasS, String methodName, List<Object> argumentValues) throws EvaluationException {

        // Determine arguments' types.
        Class<?>[] argumentTypes = ReflectUtil.getTypes(argumentValues);

        // Find the most specific method
        Method mostSpecificMethod;
        try {
            mostSpecificMethod = ReflectUtil.getMostSpecificMethod(clasS, methodName, argumentTypes);
        } catch (NoSuchMethodException nsme) {
            throw new EvaluationException(nsme);
        }

        if (!Modifier.isStatic(mostSpecificMethod.getModifiers())) {
            throw new EvaluationException(
                "Cannot invoke non-static method '"
                + clasS.getName()
                + '.'
                + methodName
                + "()' in static context"
            );
        }

        // Invoke the method.
        try {
            return mostSpecificMethod.invoke(null, argumentValues.toArray());
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    /**
     * Return the value of the given attribute of the given <var>target</var> object. An attribute is either a PUBLIC
     * field, or it is retrieved by invoking a getter ("xyz()" or "getXyz()").
     */
    @Nullable private static <E extends Exception> Object
    getAttributeValue(@Nullable Object target, String attributeName) throws EvaluationException {
        if (target == null) return null;

        Class<?> clasS = target.getClass();

        try {
            try {
                return clasS.getField(attributeName).get(target);
            } catch (NoSuchFieldException nsfe) {}
            try {
                return clasS.getMethod(attributeName).invoke(target);
            } catch (NoSuchMethodException nsme) {}
            try {
                return clasS.getMethod(
                    Notations.fromCamelCase(attributeName).prepend("get").toLowerCamelCase()
                ).invoke(target);
            } catch (NoSuchMethodException nsme) {}
        } catch (Exception e) {
            throw ExceptionUtil.wrap(
                "Retrieving attribute '" + attributeName + "' of '" + clasS.getName() + "'",
                e,
                EvaluationException.class
            );
        }

        throw new EvaluationException(
            "'"
            + clasS.getName()
            + "' has no field '"
            + attributeName
            + "' nor a method '"
            + attributeName
            + "()' or 'get"
            + attributeName
            + "()' method"
        );
    }

    /**
     * Return the value of the given attribute of the given <var>target</var> object. An attribute is either a PUBLIC
     * field, or it is retrieved by invoking a getter ("xyz()" or "getXyz()").
     */
    @Nullable private static <E extends Exception> Object
    getStaticAttributeValue(Class<?> target, String attributeName) throws EvaluationException {

        try {
            try {
                return target.getField(attributeName).get(null);
            } catch (NoSuchFieldException nsfe) {}
            try {
                return target.getMethod(attributeName).invoke(null);
            } catch (NoSuchMethodException nsme) {}
            try {
                return target.getMethod(
                    Notations.fromCamelCase(attributeName).prepend("get").toLowerCamelCase()
                ).invoke(null);
            } catch (NoSuchMethodException nsme) {}
        } catch (NullPointerException npe) {
            throw new EvaluationException( // SUPPRESS CHECKSTYLE AvoidHidingCause
                "Cannot retrieve nonstatic attribute '" + attributeName + "' in static context"
            );
        } catch (Exception e) {
            throw ExceptionUtil.wrap(
                "Retrieving attribute '" + attributeName + "' of '" + target.getName() + "'",
                e,
                EvaluationException.class
            );
        }

        throw new EvaluationException(
            "'"
            + target.getName()
            + "' has no static field '"
            + attributeName
            + "' nor a static method '"
            + attributeName
            + "()' or 'get"
            + ExpressionEvaluator.capitalizeFirstCharacter(attributeName)
            + "()'"
        );
    }

    private static String
    capitalizeFirstCharacter(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    @Nullable private static Object
    conditional(@Nullable Object lhs, @Nullable Object mhs, @Nullable Object rhs) throws EvaluationException {
        return ExpressionEvaluator.to(lhs, Boolean.class) ? mhs : rhs;
    }

    @Nullable private static Object
    unaryOperation(UnaryOperator operator, @Nullable Object operand) throws EvaluationException {
        switch (operator) {
        case LOGICAL_COMPLEMENT:
            return !ExpressionEvaluator.toBoolean(operand);
        case MINUS:
            {
                operand = ExpressionEvaluator.unaryNumericPromotion(operand);
                if (operand == null) return null;
                Class<?> clazz = operand.getClass();
                if (clazz == Integer.class) return -(Integer) operand;
                if (clazz == Long.class)    return -(Long)    operand;
                if (clazz == Float.class)   return -(Float)   operand;
                if (clazz == Double.class)  return -(Double)  operand;
                throw new EvaluationException("'" + clazz.getName() + "' operand cannot be negated");
            }
        case BITWISE_COMPLEMENT:
            {
                operand = ExpressionEvaluator.unaryNumericPromotion(operand);
                if (operand == null) return null;
                Class<?> clazz = operand.getClass();
                if (clazz == Integer.class) return ~(Integer) operand;
                if (clazz == Long.class)    return ~(Long)    operand;
                throw new EvaluationException("'" + clazz.getName() + "' operand cannot be complemented");
            }
        default:
            throw new IllegalStateException();
        }
    }

    @Nullable private static Object
    binaryOperation(@Nullable Object lhsv, BinaryOperator op, @Nullable Object rhsv) throws EvaluationException {

        lhsv = ExpressionEvaluator.binaryNumericPromotion(lhsv, rhsv);
        rhsv = ExpressionEvaluator.binaryNumericPromotion(rhsv, lhsv);
        Class<? extends Object> lhsc = lhsv == null ? null : lhsv.getClass();
        Class<? extends Object> rhsc = rhsv == null ? null : rhsv.getClass();

        // Check for ARITHMETIC operations first.

        if (lhsc == Integer.class && rhsc == Integer.class) {
            @SuppressWarnings("null") int lhsi = (Integer) lhsv, rhsi = (Integer) rhsv;
            switch (op) {
            case BITWISE_OR:   return lhsi |   rhsi;
            case BITWISE_XOR:  return lhsi ^   rhsi;
            case BITWISE_AND:  return lhsi &   rhsi;
            case LEFT_SHIFT:   return lhsi <<  rhsi;
            case RIGHT_SHIFT:  return lhsi >>  rhsi;
            case RIGHT_USHIFT: return lhsi >>> rhsi;
            case MULTIPLY:     return lhsi *   rhsi;
            case DIVIDE:       return lhsi /   rhsi;
            case MODULO:       return lhsi %   rhsi;
            case PLUS:         return lhsi +   rhsi;
            case MINUS:        return lhsi -   rhsi;
            default:           ;
            }
        } else
        if (lhsc == Long.class && rhsc == Long.class) {
            @SuppressWarnings("null") long lhsl = (Long) lhsv, rhsl = (Long) rhsv;
            switch (op) {
            case BITWISE_OR:   return lhsl |   rhsl;
            case BITWISE_XOR:  return lhsl ^   rhsl;
            case BITWISE_AND:  return lhsl &   rhsl;
            case LEFT_SHIFT:   return lhsl <<  rhsl;
            case RIGHT_SHIFT:  return lhsl >>  rhsl;
            case RIGHT_USHIFT: return lhsl >>> rhsl;
            case MULTIPLY:     return lhsl *   rhsl;
            case DIVIDE:       return lhsl /   rhsl;
            case MODULO:       return lhsl %   rhsl;
            case PLUS:         return lhsl +   rhsl;
            case MINUS:        return lhsl -   rhsl;
            default:           ;
            }
        } else
        if (lhsc == Float.class && rhsc == Float.class) {
            @SuppressWarnings("null") float lhsf = (Float) lhsv, rhsf = (Float) rhsv;
            switch (op) {
            case MULTIPLY:     return lhsf * rhsf;
            case DIVIDE:       return lhsf / rhsf;
            case MODULO:       return lhsf % rhsf;
            case PLUS:         return lhsf + rhsf;
            case MINUS:        return lhsf - rhsf;
            default:           ;
            }
        } else
        if (lhsc == Double.class && rhsc == Double.class) {
            @SuppressWarnings("null") double lhsd = (Double) lhsv, rhsd = (Double) rhsv;
            switch (op) {
            case MULTIPLY:     return lhsd * rhsd;
            case DIVIDE:       return lhsd / rhsd;
            case MODULO:       return lhsd % rhsd;
            case PLUS:         return lhsd + rhsd;
            case MINUS:        return lhsd - rhsd;
            default:           ;
            }
        }

        // Now for the NON-ARITHMETIC operations.

        switch (op) {

        case GLOB: // <string> =* <glob>
            return Glob.compile(
                ExpressionEvaluator.toString(rhsv),
                Glob.INCLUDES_EXCLUDES | Glob.REPLACEMENT | Pattern2.WILDCARD
            ).replace(ExpressionEvaluator.toString(lhsv));
        case REGEX: // <string> =~ <regex>
            return Glob.compile(
                ExpressionEvaluator.toString(rhsv),
                Glob.REPLACEMENT | Pattern.DOTALL
            ).replace(ExpressionEvaluator.toString(lhsv));

        case LOGICAL_AND: // PERLish behavior.
            return ExpressionEvaluator.toBoolean(lhsv) ? rhsv : Boolean.FALSE;
        case LOGICAL_OR: // PERLish behavior.
            return ExpressionEvaluator.toBoolean(lhsv) ? lhsv : rhsv;
        case BITWISE_OR: // Boolean logical operator '|'.
            return ExpressionEvaluator.toBoolean(lhsv) | ExpressionEvaluator.toBoolean(rhsv);
        case BITWISE_XOR: // Boolean logical operator '^'.
            return ExpressionEvaluator.toBoolean(lhsv) ^ ExpressionEvaluator.toBoolean(rhsv);
        case BITWISE_AND: // Boolean logical operator '&'.
            return ExpressionEvaluator.toBoolean(lhsv) & ExpressionEvaluator.toBoolean(rhsv);

        case LEFT_SHIFT:
        case RIGHT_SHIFT:
        case RIGHT_USHIFT:
            throw new EvaluationException(
                "Incompatible types for operator '"
                + op
                + "' ('"
                + ExpressionEvaluator.className(lhsc)
                + "' and '"
                + ExpressionEvaluator.className(rhsc)
                + "')"
            );

        case EQUAL:         return ObjectUtil.equals(lhsv, rhsv);  // VALUE comparison
        case NOT_EQUAL:     return !ObjectUtil.equals(lhsv, rhsv); // VALUE comparison
        case LESS:          return ExpressionEvaluator.compare(lhsv, PredicateUtil.less(0),         rhsv);
        case LESS_EQUAL:    return ExpressionEvaluator.compare(lhsv, PredicateUtil.lessEqual(0),    rhsv);
        case GREATER:       return ExpressionEvaluator.compare(lhsv, PredicateUtil.greater(0),      rhsv);
        case GREATER_EQUAL: return ExpressionEvaluator.compare(lhsv, PredicateUtil.greaterEqual(0), rhsv);

        case PLUS:
            return ExpressionEvaluator.toString(lhsv) + ExpressionEvaluator.toString(rhsv);

        case MINUS:
        case MULTIPLY:
        case DIVIDE:
        case MODULO:
            throw new EvaluationException(
                "Incompatible types for operator '"
                + op
                + "' ('"
                + ExpressionEvaluator.className(lhsc)
                + "' and '"
                + ExpressionEvaluator.className(rhsc)
                + "')"
            );

        default:
            throw new IllegalStateException();
        }
    }

    private static String
    className(@Nullable Class<? extends Object> clasS) {
        return clasS == null ? "null" : clasS.getName();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) private static boolean
    compare(
        @Nullable Object           lhsv,
        Predicate<? super Integer> condition,
        @Nullable Object           rhsv
    ) throws EvaluationException {

        if (lhsv == null || rhsv == null) return false;

        if (lhsv instanceof Comparable && rhsv instanceof Comparable) {
            return condition.evaluate(((Comparable) lhsv).compareTo(rhsv));
        }

        if (lhsv instanceof CharSequence && rhsv instanceof CharSequence) {
            return condition.evaluate(StringUtil.compareTo((CharSequence) lhsv, (CharSequence) rhsv));
        }

        throw new EvaluationException(
            "Operands '"
            + lhsv.getClass().getName()
            + "' and '"
            + rhsv.getClass().getName()
            + "' cannot be lexicographically compared"
        );
    }

    @Nullable private static Object
    unaryNumericPromotion(@Nullable Object value) {
        if (value == null) return null;

        Class<? extends Object> valueClass = value.getClass();
        if (valueClass == Byte.class)      return Integer.valueOf(((Byte) value));
        if (valueClass == Short.class)     return Integer.valueOf(((Short) value));
        if (valueClass == Character.class) return Integer.valueOf(((Character) value));

        return value;
    }

    /**
     * Converts and returns the <var>value</var> to {@link Integer}, {@link Long}, {@link Float}, {@link Double} or
     * {@link String} as appropriate for comparison with <var>other</var>.
     *
     * <p>Returns the original <var>value</var> iff the <var>value</var> is incomparable with <var>other</var>.
     *
     * <p>Examples:
     * <table border="1">
     *   <tr><th><var>value</var></th><th><var>other</var></th><th>Result</th></tr>
     *   <tr><td>null</td><td>any</td><td>null</td></tr>
     *   <tr><td>any</td><td>null</td><td>value</td></tr>
     *   <tr><td>non-primitive</td><td>any</td><td>value</td></tr>
     *   <tr><td>any</td><td>non-primitive</td><td>value</td></tr>
     *   <tr><td>Byte</td><td>Short</td><td>Integer</td></tr>
     *   <tr><td>Long</td><td>Short</td><td>Long</td></tr>
     *   <tr><td>Byte</td><td>String</td><td>String</td></tr>
     * </table>
     */
    @Nullable protected static Object
    binaryNumericPromotion(@Nullable Object value, @Nullable Object other) {
        if (value == null || other == null) return value;

        Class<? extends Object> valueClass = value.getClass();
        Class<? extends Object> otherClass = other.getClass();

        if (valueClass == Byte.class) {
            byte byteValue = ((Byte) value);
            if (otherClass == Byte.class)  return Integer.valueOf(byteValue);
            if (otherClass == Short.class)   return Integer.valueOf(byteValue);
            if (otherClass == Integer.class) return Integer.valueOf(byteValue);
            if (otherClass == Long.class)    return Long.valueOf(byteValue);
            if (otherClass == Float.class)   return Float.valueOf(byteValue);
            if (otherClass == Double.class)  return Double.valueOf(byteValue);
            if (otherClass == String.class)  return String.valueOf(byteValue);
        } else
        if (valueClass == Short.class) {
            short shortValue = ((Short) value);
            if (otherClass == Byte.class)    return Integer.valueOf(shortValue);
            if (otherClass == Short.class)   return Integer.valueOf(shortValue);
            if (otherClass == Integer.class) return Integer.valueOf(shortValue);
            if (otherClass == Long.class)    return Long.valueOf(shortValue);
            if (otherClass == Float.class)   return Float.valueOf(shortValue);
            if (otherClass == Double.class)  return Double.valueOf(shortValue);
            if (otherClass == String.class)  return String.valueOf(shortValue);
        } else
        if (valueClass == Character.class) {
            char charValue = ((Character) value);
            if (otherClass == Byte.class)    return Integer.valueOf(charValue);
            if (otherClass == Short.class)   return Integer.valueOf(charValue);
            if (otherClass == Integer.class) return Integer.valueOf(charValue);
            if (otherClass == Long.class)    return Long.valueOf(charValue);
            if (otherClass == Float.class)   return Float.valueOf(charValue);
            if (otherClass == Double.class)  return Double.valueOf(charValue);
            if (otherClass == String.class)  return String.valueOf(charValue);
        } else
        if (valueClass == Integer.class) {
            int intValue = ((Integer) value);
            if (otherClass == Long.class)   return Long.valueOf(intValue);
            if (otherClass == Float.class)  return Float.valueOf(intValue);
            if (otherClass == Double.class) return Double.valueOf(intValue);
            if (otherClass == String.class) return String.valueOf(intValue);
        } else
        if (valueClass == Long.class) {
            long longValue = ((Long) value);
            if (otherClass == Float.class)  return Float.valueOf(longValue);
            if (otherClass == Double.class) return Double.valueOf(longValue);
            if (otherClass == String.class) return String.valueOf(longValue);
        } else
        if (valueClass == Float.class) {
            float floatValue = ((Float) value);
            if (otherClass == Double.class) return Double.valueOf(floatValue);
            if (otherClass == String.class) return String.valueOf(floatValue);
        } else
        if (valueClass == Double.class) {
            double doubleValue = ((Double) value);
            if (otherClass == String.class) return String.valueOf(doubleValue);
        }
        return value;
    }

    /**
     * Scans, parses, evaluates and returns an expression.
     *
     * @return                     An object of type {@code T}, or {@code null}
     * @throws EvaluationException The value is not assignable to {@code T}
     * @see Parser                 The expression syntax
     */
    @Nullable public <T> T
    evaluateTo(String spec, Mapping<String, ?> variables, Class<T> targetType)
    throws ParseException, EvaluationException {

        Object o = this.evaluate(spec, variables);

        if (o == null) return null;

        if (!targetType.isAssignableFrom(o.getClass())) {
            throw new EvaluationException("'" + o + "' (type " + o.getClass() + ") is not a " + targetType);
        }

        @SuppressWarnings("unchecked") T result = (T) o;
        return result;
    }

    /**
     * Scans, parses and evaluates an expression.
     *
     * @throws EvaluationException The expression evaluates to {@code null} (and the targetType is not boolean.class)
     * @throws EvaluationException The expression value cannot be converted to the given targetType
     * @return                     The (wrapped) expression value
     * @see Parser                 The expression syntax
     */
    public Object
    evaluateToPrimitive(String spec, Mapping<String, ?> variables, Class<?> targetType)
    throws ParseException, EvaluationException {

        return ExpressionEvaluator.toPrimitive(this.evaluate(spec, variables), targetType);
    }

    /**
     * Scans, parses and evaluates an expression.
     *
     * @return     {@code null} iff the <var>spec</var> is {@code null}; otherwise the expresasion value
     * @see Parser The expression syntax
     */
    public boolean
    evaluateToBoolean(@Nullable String spec, Mapping<String, ?> variables) throws ParseException, EvaluationException {

        return spec != null && ExpressionEvaluator.toBoolean(this.evaluate(spec, variables));
    }

    /**
     * Converts the given <var>subject</var> to the given <var>targetType</var>.
     * Special processing applies for target types {@link String} and {@link Boolean}.
     *
     * @see #toBoolean(Object)
     * @see #toString(Object)
     */
    @Nullable @SuppressWarnings("unchecked") public static <T> T
    to(@Nullable Object subject, Class<T> targetType) throws EvaluationException {

        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) (ExpressionEvaluator.FALSES.contains(subject) ? Boolean.FALSE : Boolean.TRUE);
        }

        if (subject == null) return null;

        if (targetType == String.class) {
            return (T) subject.toString();
        }

        Class<? extends Object> sourceClass = subject.getClass();

        if (targetType.isAssignableFrom(sourceClass)) return (T) subject;

        throw new EvaluationException(
            "Cannot convert '"
            + sourceClass.getName()
            + "' to '"
            + targetType.getName()
            + "'"
        );
    }

    /**
     * Converts the given <var>subject</var> to the given primitive target type. Special processing applies for target
     * type {@code boolean.class}, see {@link #toBoolean(Object)}.
     *
     * @throws EvaluationException The <var>subject</var> is {@code null} (and the <var>targetType</var> is not {@code
     *                             boolean.class})
     * @throws EvaluationException The <var>subject</var> cannot be converted to the given <var>targetType</var>
     */
    @SuppressWarnings("unchecked") public static <T> T
    toPrimitive(@Nullable Object subject, Class<T> targetType) throws EvaluationException {

        if (!targetType.isPrimitive()) throw new AssertionError(targetType);

        if (targetType == boolean.class) return (T) Boolean.valueOf(ExpressionEvaluator.toBoolean(subject));

        if (subject == null) {
            throw new EvaluationException("Cannot convert 'null' to '" + targetType.getName() + "'");
        }

        Class<?> wrapperType = (
            targetType == char.class   ? Character.class :
            targetType == byte.class   ? Byte.class      :
            targetType == short.class  ? Short.class     :
            targetType == int.class    ? Integer.class   :
            targetType == long.class   ? Long.class      :
            targetType == float.class  ? Float.class     :
            targetType == double.class ? Double.class    :
            ExceptionUtil.<Class<?>>throwAssertionError(targetType)
        );

        if (subject.getClass() != wrapperType) {
            throw new EvaluationException("Cannot convert '" + subject.getClass() + "' to '" + targetType + "'");
        }

        return (T) subject;
    }

    /**
     * @return Whether the <var>subject</var> equals on of the {@link #FALSES}
     */
    public static boolean
    toBoolean(@Nullable Object subject) { return !ExpressionEvaluator.FALSES.contains(subject); }

    /**
     * All values the are implicitly regarded als {@code false}:
     * <ul>
     *   <li>{@code null}
     *   <li>{@code ""}
     *   <li>{@link Boolean#FALSE}
     *   <li>{@code (byte) 0}
     *   <li>{@code (short) 0}
     *   <li>{@code (integer) 0}
     *   <li>{@code (long) 0}
     * </ul>
     */
    public static final Set<Object> FALSES = new HashSet<Object>();
    static {
        ExpressionEvaluator.FALSES.add(null);
        ExpressionEvaluator.FALSES.add("");
        ExpressionEvaluator.FALSES.add(Boolean.FALSE);
        ExpressionEvaluator.FALSES.add(Byte.valueOf((byte) 0));
        ExpressionEvaluator.FALSES.add(Short.valueOf((short) 0));
        ExpressionEvaluator.FALSES.add(Integer.valueOf(0));
        ExpressionEvaluator.FALSES.add(Long.valueOf(0));
    }
}
