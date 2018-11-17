
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

import java.util.regex.Matcher;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
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
    public static Function<Matcher, String>
    parse(final String spec) throws ParseException {
        return ExpressionMatchReplacer.parse(spec, Mappings.<String, Object>none(), PredicateUtil.<String>never());
    }

    /**
     * @see #parse(String, Mapping, Predicate)
     */
    public static Function<Matcher, String>
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
    public static Function<Matcher, String>
    parse(final String spec, final Mapping<String, ?> variables, Predicate<String> isValidVariableName)
    throws ParseException {

        Predicate<String> variableNamePredicate = PredicateUtil.or(isValidVariableName, PredicateUtil.equal("m"));

        final Expression expression = new ExpressionEvaluator(variableNamePredicate).parse(spec);

        return ExpressionMatchReplacer.get(expression, variables);
    }

    /**
     * @see #get(Expression, Mapping)
     */
    public static Function<Matcher, String>
    get(final Expression expression, Object... variableNamesAndValues) {
        return ExpressionMatchReplacer.get(expression, Mappings.<String, Object>mapping(variableNamesAndValues));
    }

    /**
     * Creates and returns a "match replacer" that is suitable for {@link PatternUtil#replaceSome(Matcher,
     * FunctionWhichThrows)} and implements the substitution through the given <var>expression</ver>.
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
    public static Function<Matcher, String>
    get(final Expression expression, final Mapping<String, ?> variables) {

        return new Function<Matcher, String>() {

            @Override @Nullable public String
            call(@Nullable Matcher matcher) {
                assert matcher != null;

                Mapping<String, ?> v2 = Mappings.augment(
                    variables,
                    "m", matcher // SUPPRESS CHECKSTYLE Wrap
                );

                try {
                    return expression.evaluateTo(v2, String.class);
                } catch (EvaluationException e) {
                    throw ExceptionUtil.wrap("Evaluating \"" + expression + "\"", e, IllegalArgumentException.class);
                }
            }
        };
    }
}
