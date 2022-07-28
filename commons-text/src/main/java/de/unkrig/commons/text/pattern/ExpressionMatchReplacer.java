
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.text.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.text.parser.ParseException;

/**
 * Utility class that implements various flavors of {@link Expression}-based replace-all functionality in the style
 * of {@link Matcher#replaceAll(String)}.
 */
public final
class ExpressionMatchReplacer {

    private ExpressionMatchReplacer() {}

    /**
     * Substitutes all matches of the <var>matcher</var> with the value of an expression.
     * <p>
     *   The expression uses a single variable, {@code "m"}, which is the {@link Matcher} of the current match.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   Matcher matcher = Pattern.compile(regex).matcher(input);
     *   String output = PatternUtil.replaceSome(matcher, "m.group.toUpperCase()");
     * </pre>
     * <p>
     *   If you plan to use the same expression for <em>mutiple</em> {@link #replaceSome(Matcher, String)} operations,
     *   you can reduce the overhead of parsing the <var>spec</var> by calling {@link #parse(String)} (once) and {@link
     *   PatternUtil#replaceSome(Matcher, FunctionWhichThrows)} (repeatedly).
     * </p>
     *
     * @param spec            Specifies the expression to use; when the expression evaluates to {@code null}, then
     *                        the respective match is <em>not</em> replaced
     * @return                The input of the <var>matcher</var> with all matches replaced with the expression result
     * @throws ParseException A problem occurred when the <var>spec</var> was parsed
     * @see Parser            The expression syntax
     */
    public static String
    replaceSome(Matcher matcher, String spec) throws ParseException {
        return PatternUtil.replaceSome(matcher, ExpressionMatchReplacer.parse(spec));
    }

    /**
     * Creates and returns a "match replacer" that is suitable for {@link PatternUtil#replaceSome(Matcher,
     * FunctionWhichThrows)} and implements the substitution through an {@link Expression}.
     * <p>
     *   The expression uses a single variable, {@code "m"}, which is the {@link Matcher} of the current match.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   Function<Matcher, String> matchReplacer = ExpressionMatchReplacer.parse("m.group.toUpperCase()");
     *   ...
     *   Matcher matcher = ...;
     *   System.out.println(PatternUtil.replaceSome(matcher, matchReplacer));
     * </pre>
     * <p>
     *   If you want to use more variables than just the matcher, use {@link #parse(String, Mapping, Predicate)}
     *   instead.
     * </var>
     *
     * @param spec            The text to be parsed
     * @throws ParseException A problem occurred when the <var>spec</var> was parsed
     * @see Parser            The expression syntax
     * @see                   #parse(String, Mapping, Predicate)
     * @see                   #get(Expression, Mapping)
     */
    public static Function<MatchResult, String>
    parse(final String spec) throws ParseException {
        return ExpressionMatchReplacer.parse(
            spec,                            // spec
            Mappings.<String, Object>none(), // variables
            PredicateUtil.<String>never()    // isValidVariableName
        );
    }

    /**
     * @see #parse(String, Mapping, Predicate)
     */
    public static Function<MatchResult, String>
    parse(final String spec, Object... variableNamesAndValues) throws ParseException {

        Mapping<String, Object> variables           = Mappings.mapping(variableNamesAndValues);
        Predicate<String>       isValidVariableName = Mappings.containsKeyPredicate(variables);

        return ExpressionMatchReplacer.parse(spec, variables, isValidVariableName);
    }

    /**
     * Creates and returns a "match replacer" that is suitable for {@link PatternUtil#replaceSome(Matcher,
     * FunctionWhichThrows)} and implements the substitution through an {@link Expression}.
     * <p>
     *   The expression uses the named variables, plus one more variable {@code "m"}, which is the {@link Matcher} of
     *   the current match.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   Function&lt;Matcher, String> matchReplacer = ExpressionMatchReplacer.parse(
     *       "prefix + new StringBuilder(m.group).reverse()",
     *       Mappings.mapping("prefix", "pre-"),
     *       "prefix"
     *   );
     *   ...
     *   Matcher matcher = ...;
     *   System.out.println(PatternUtil.replaceSome(matcher, matchReplacer));
     * </pre>
     * <p>
     *   If you want to use the <em>same</em> expression, but <em>different</em> variable values, then use {@link
     *   #get(Expression, Mapping)} instead.
     * </p>
     *
     * @param spec                The expression that will later be used for all substitutions
     * @param isValidVariableName Defines the names of all variables that the expression can use
     * @param variables           The variables' values that will take effect when the expression is evaluated for each
     *                            match
     * @throws ParseException     A problem occurred when the <var>spec</var> was parsed
     * @see Parser                The expression syntax
     */
    public static Function<MatchResult, String>
    parse(final String spec, final Mapping<String, ?> variables, Predicate<String> isValidVariableName)
    throws ParseException {
        return ExpressionMatchReplacer.parse(spec, isValidVariableName).transform(variables);
    }

    /**
     * Creates a factory for creating match replacers from a set of variables.
     *
     * @param spec An expression in the syntax of {@link ExpressionEvaluator#parse(String)}
     */
    public static Transformer<Mapping<String, ?>, Function<MatchResult, String>>
    parse(final String spec, Predicate<String> isValidVariableName) throws ParseException {

        Predicate<String> variableNamePredicate = PredicateUtil.or(isValidVariableName, PredicateUtil.equal("m"));

        final Expression expression = new ExpressionEvaluator(variableNamePredicate).parse(spec);

        return new Transformer<Mapping<String, ?>, Function<MatchResult, String>>() {

            @Override public Function<MatchResult, String>
            transform(Mapping<String, ?> variables) throws NoException {
                assert variables != null;
                return ExpressionMatchReplacer.get(expression, variables);
            }

            @Override public String toString() { return spec; }
        };
    }

    /**
     * Creates a factory for creating match replacers from a set of variables.
     * Semantically identical with {@link #parse(String, Predicate)}, but implements a different syntax, downwards
     * compatible with that of {@link Matcher#appendReplacement(StringBuffer, String)}:
     * <dl>
     *   <dt>$<var>n</var></dt>
     *   <dd>The Text captured by the <var>n</var>th group</dd>
     *   <dt>$<var>xxx</var></dt>
     *   <dd>The value of variable <var>xxx</var></dd>
     *   <dt>${<var>expr</var>}</dt>
     *   <dd>The value of the expression <var>expr</var></dd>
     *   <dt><var>any-other-char</var></dt>
     *   <dd>That char, literally</dd>
     * </dl>
     * <p>
     *   Usage example:
     * </p>
     * <pre>
     *     Pattern            pattern   = Pattern.compile("(155\\d{10})(?!\\d)");
     *     Mapping&lt;String, ?> variables = Mappings.mapping("df", new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"));
     *
     *     final Transformer&lt;Mapping&lt;String, ?>, Function&lt;MatchResult, String>>
     *     matchReplacer = ExpressionMatchReplacer.parseExt(
     *         "${df.format(new java.util.Date(Long.parseLong(m.group(1))))}",
     *         Mappings.containsKeyPredicate(variables)
     *     );
     *
     *     String result = PatternUtil.replaceSome(
     *         pattern.matcher(in),               // matcher
     *         matchReplacer.transform(variables) // matchReplacer
     *     );
     * </pre>
     */
    public static Transformer<Mapping<String, ?>, Function<MatchResult, String>>
    parseExt(final String spec, Predicate<String> isValidVariableName) throws ParseException {
        int specLength = spec.length();

        Predicate<String> variableNamePredicate = PredicateUtil.or(isValidVariableName, PredicateUtil.equal("m"));

        // Parse the spec into a sequence of "segments".
        final List<Transformer<Mapping<String, ?>, String>>
        segments = new ArrayList<Transformer<Mapping<String, ?>, String>>();

        for (int idx = 0; idx < specLength;) {
            if (spec.charAt(idx) == '$' && idx <= specLength - 2) {
                char c2 = spec.charAt(idx + 1);
                if (Character.isDigit(c2)) {

                    // $123
                    int to = idx + 2;
                    while (to < specLength && Character.isDigit(spec.charAt(to))) to++;
                    final int groupNumber = Integer.parseInt(spec.substring(idx + 1, to));

                    segments.add(new Transformer<Mapping<String, ?>, String>() {

                        @Override public String
                        transform(Mapping<String, ?> variables) {
                            Object m = variables.get("m");
                            assert m != null;
                            return ((MatchResult) m).group(groupNumber);
                        }

                        @Override public String toString() { return "$" + groupNumber; }
                    });
                    idx = to;
                    continue;
                }
                if (Character.isJavaIdentifierStart(c2)) {

                    // $xxx
                    int to = idx + 2;
                    while (to < specLength && Character.isJavaIdentifierPart(spec.charAt(to))) to++;
                    final String variableName = spec.substring(idx + 1, to);

                    segments.add(new Transformer<Mapping<String, ?>, String>() {

                        @Override public String
                        transform(Mapping<String, ?> variables) {
                            Object variableValue = variables.get(variableName);
                            assert variableValue != null;
                            return variableValue.toString();
                        }

                        @Override public String toString() { return "$" + variableName; }
                    });
                    idx = to;
                    continue;
                }
                if (c2 == '{') {

                    ExpressionEvaluator ee         = new ExpressionEvaluator(variableNamePredicate);
                    int[]               offset     = new int[1];
                    final Expression    expression = ee.parsePart(spec.substring(idx + 2), offset);
                    int                 to         = idx + 2 + offset[0];

                    if (to < specLength && spec.charAt(to) == '}') {
                        final int idx2 = idx;

                        // ${expr}
                        segments.add(new Transformer<Mapping<String, ?>, String>() {

                            @Override public String
                            transform(Mapping<String, ?> variables) {
                                try {
                                    return ObjectUtil.or(expression.evaluateTo(variables, String.class), "");
                                } catch (EvaluationException ee) {
                                    throw ExceptionUtil.wrap(
                                        "Evaluating \"" + expression + "\"",
                                        ee,
                                        IllegalArgumentException.class
                                    );
                                }
                            }

                            @Override public String toString() { return spec.substring(idx2, to + 1); }
                        });
                        idx = to + 1;
                        continue;
                    }
                }
            }

            // xxx (literal text)
            int to = idx + 1;
            while (to < specLength && spec.charAt(to) != '$') to++;
            final String s = spec.substring(idx, to);
            segments.add(new Transformer<Mapping<String, ?>, String>() {

                @Override public String
                transform(Mapping<String, ?> in) { return s; }

                @Override public String toString() { return s; }
            });
            idx = to;
        }

        return new Transformer<Mapping<String, ?>, Function<MatchResult, String>>() {

            @Override public Function<MatchResult, String>
            transform(final Mapping<String, ?> variables) {

                return new Function<MatchResult, String>() {

                    @Override @Nullable public String
                    call(@Nullable MatchResult matchResult) throws NoException {
                        final Mapping<String, ?> variables2 = Mappings.override(variables, "m", matchResult);

                        StringBuilder sb = new StringBuilder();
                        for (Transformer<Mapping<String, ?>, String> segment : segments) {
                            sb.append(segment.transform(variables2));
                        }
                        return sb.toString();
                    }
                };
            }

            @Override public String toString() { return spec; }
        };
    }

    /**
     * Simplified version of {@link #parseExt(String, Predicate)} for expressions that do not use variables (in
     * addition to the predefined variable "m").
     */
    public static Function<MatchResult, String>
    parseExt(final String spec) throws ParseException {

        return ExpressionMatchReplacer.parseExt(
            spec,                         // spec
            PredicateUtil.<String>never() // isValidVariableName
        ).transform(
            Mappings.<String, Object>none() // variables
        );
    }

    /**
     * @param expression             See {@link #get(Expression, Mapping)}
     * @param variableNamesAndValues An alternating sequence of keys and values; even elements must be {@link String}s,
     *                               odd elements can be any {@link Object}
     */
    public static Function<MatchResult, String>
    get(final Expression expression, Object... variableNamesAndValues) {
        return ExpressionMatchReplacer.get(expression, Mappings.<String, Object>mapping(variableNamesAndValues));
    }

    /**
     * Creates and returns a "match replacer" that is suitable for {@link PatternUtil#replaceSome(Matcher,
     * FunctionWhichThrows)} and implements the substitution through the given <var>expression</var>.
     * <p>
     *   When the expression is evaluated, it gets an additional variable {@code "m"}, which is the {@link Matcher} of
     *   the current match.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   // Parsing the expression is relatively slow.
     *   Expression expression = new ExpressionEvaluator(
     *       "prefix",  // <= This is "our" variable
     *       "m"        // <= Also declare variable "m", which will automatically be available
     *   ).parse(
     *       "prefix + new StringBuilder(m.group).reverse()"
     *   );
     *
     *   // ...
     *
     *   // Creating the match replacer with the actual variable values is fast.
     *   Function<Matcher, String> matchReplacer = ExpressionMatchReplacer.get(
     *       expression,
     *       Mappings.<String, Object>mapping("prefix", "pre-") // <= pass the value for variable "prefix"
     *   );
     *
     *   // Now use the match replacer to substitute regex matches.
     *   Matcher matcher = ...;
     *   System.out.println(PatternUtil.replaceSome(matcher, matchReplacer));
     * </pre>
     *
     * @param expression      Computes the replacement for each substitution
     * @param variables       The variables' values for the <var>expression</var>
     * @see Parser            The expression syntax
     */
    public static Function<MatchResult, String>
    get(final Expression expression, final Mapping<String, ?> variables) {

        return new Function<MatchResult, String>() {

            @Override @Nullable public String
            call(@Nullable MatchResult matchResult) {
                assert matchResult != null;

                Mapping<String, ?> v2 = Mappings.augment(
                    variables,
                    "m", matchResult // SUPPRESS CHECKSTYLE Wrap
                );

                try {
                    return expression.evaluateTo(v2, String.class);
                } catch (EvaluationException ee) {
                    throw ExceptionUtil.wrap("Evaluating \"" + expression + "\"", ee, IllegalArgumentException.class);
                }
            }

            @Override public String
            toString() { return expression.toString(); }
        };
    }
}
