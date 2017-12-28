
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

// SUPPRESS CHECKSTYLE Javadoc|LineLength:9999

package test.expression;

import static de.unkrig.commons.text.expression.Parser.Extension.NEW_CLASS_WITHOUT_KEYWORD;
import static de.unkrig.commons.text.expression.Parser.Extension.NEW_CLASS_WITHOUT_PARENTHESES;
import static de.unkrig.commons.text.expression.Parser.Extension.OPERATOR_GLOB;
import static de.unkrig.commons.text.expression.Parser.Extension.OPERATOR_REGEX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.expression.ExpressionUtil;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.text.expression.Parser.Extension;
import de.unkrig.commons.text.parser.ParseException;

public
class ExpressionEvaluatorTest {

    private static final Object[] VARIABLES = {
        "bo", false,                           // SUPPRESS CHECKSTYLE WrapMethod:11
        "by", (byte) 1,
        "sh", (short) 2,
        "in", 3,
        "lo", 4L,
        "ch", '5',
        "fl", 6F,
        "do", 7D,
        "st", "8",
        "da", new Date(9),
        "ia", new int[] { 1, 2, 3 },
        "sa", new String[] { "alpha", "beta", "gamma" }
    };
    private static final String[] VARIABLE_NAMES;
    static {
        VARIABLE_NAMES = new String[ExpressionEvaluatorTest.VARIABLES.length / 2];
        for (int i = 0; i < ExpressionEvaluatorTest.VARIABLE_NAMES.length; i++) {
            ExpressionEvaluatorTest.VARIABLE_NAMES[i] = (String) ExpressionEvaluatorTest.VARIABLES[2 * i];
        }
    }

    @Test public void
    testStringLiterals() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("123",                                     "\"123\"");
    }

    @Test public void
    testIntegerLiterals() throws EvaluationException, ParseException {

        // INT
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(123,                                            "123");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Integer.MAX_VALUE,                              "2147483647");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '2147483648' out of range",   "2147483648");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Integer.MIN_VALUE,                              "-2147483648");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '-2147483649' out of range",  "-2147483649");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(0xcafebabe,                                     "0xcafebabe");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(-0xcafebabe,                                    "-0xcafebabe");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(0xffffffff,                                     "0xffffffff");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '0x100000000' out of range",  "0x100000000");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(-0xffffffff,                                    "-0xffffffff");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '-0x100000000' out of range", "-0x100000000");

        // LONG
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(2147483648L,                                             "2147483648l");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(2147483648L,                                             "2147483648L");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Long.MAX_VALUE,                                          "9223372036854775807L");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '9223372036854775808L' out of range",  "9223372036854775808L");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Long.MIN_VALUE,                                          "-9223372036854775808L");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Integer literal '-9223372036854775809L' out of range", "-9223372036854775809L");
    }

    @Test public void
    testOtherLiterals() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(null,                                      "null");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(true,                                      "true");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(false,                                     "false");
    }

    @Test public void
    testVariables() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "bo");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Byte.valueOf((byte) 1),                    "by");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Short.valueOf((short) 2),                  "sh");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Integer.valueOf(3),                        "in");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Long.valueOf(4L),                          "lo");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Character.valueOf('5'),                    "ch");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Float.valueOf(6F),                         "fl");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Double.valueOf(7D),                        "do");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("8",                                       "st");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(new Date(9),                               "da");
    }

    @Test public void
    testLogicalOr() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "false || false");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,                              "true || false");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,                              "false || true");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,                              "true || true");
    }

    @Test public void
    testLogicalAnd() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "false && false");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "true && false");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "false && true");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,                              "true && true");
    }

    @Test public void
    testBitwiseOr() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(7,                                         "3 | 6");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(true,                                      "0 | true");
    }

    @Test public void
    testBitwiseXor() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(5,                                         "3 ^ 6");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(false,                                     "true ^ true");
    }

    @Test public void
    testBitwiseAnd() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(6,                                         "7 & 14");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(true,                                      "1 & true");
    }

    @Test public void
    testShift() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(65536,                                     "1 << 16");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(7,                                         "448 >> 6");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(-1,                                        "0xe0000000 >> 29");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(7,                                         "0xe0000000 >>> 29");
    }

    @Test public void
    testRelational() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,            "1     == 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     == 1");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     == by");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "2     == sh");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "3     == in");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "4     == lo");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "53    == ch");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "6     == fl");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "7     == do");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "8     == st");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "da    == da");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "null  == null");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,            "da    == null");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     != 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     <  2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     <= 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,            "1     >  2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,            "1     >= 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "'C'   == 67D");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     <= 2.0f");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     <= \"ABC\"");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "1     <= \"ABC\"");
        ExpressionEvaluatorTest.assertExpressionEvaluationFails(
            "Operands 'java.lang.Integer' and 'java.lang.Object' cannot be lexicographically compared",
            "1     <= Object"
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(
            Boolean.TRUE,
            "1 != Object",
            NEW_CLASS_WITHOUT_KEYWORD,
            NEW_CLASS_WITHOUT_PARENTHESES
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "\"x\" == \"x\"");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,             "\"x\" != \"y\"");
    }

    @Test public void
    testWildcard() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(null,                     "1 =* 2",      OPERATOR_GLOB);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =* 1",      OPERATOR_GLOB);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =* \"1*\"", OPERATOR_GLOB);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =* \"*\"",  OPERATOR_GLOB);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =* \"?\"",  OPERATOR_GLOB);
    }

    @Test public void
    testRegex() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(null,                     "1 =~ 2",       OPERATOR_REGEX);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =~ \".\"",   OPERATOR_REGEX);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =~ \".*\"",  OPERATOR_REGEX);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("1",                      "1 =~ \"[1]\"", OPERATOR_REGEX);
    }

    @Test public void
    testAdditive() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(3,                                         "1 + 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(3L,                                        "1 + 2L");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("12",                                      "1 + \"2\"");
        ExpressionEvaluatorTest.assertExpressionEvaluationFails(
            "Incompatible types for operator '-' ('java.lang.String' and 'java.lang.String')",
            "1 - \"2\""
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(4,                                         "1 + 2 - 3 + 4");
    }

    @Test public void
    testMultiplicative() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(3D,                                        "12.0 / 8 * 2");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(2,                                         "by * sh");
    }

    @Test public void
    testUnary() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(-10D,                                      "-10F");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.FALSE,                             "!true");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Boolean.TRUE,                              "!null");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(0x12345678,                                "~0xedcba987");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(true,                                      "!false");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(false,                                     "!true");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(false,                                     "!7");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(-77,                                       "-77");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(77,                                        "--77");
    }

    @Test public void
    testParentheses() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(10,                                        "2 * 3 + 4");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(14,                                        "2 * (3 + 4)");
    }

    @Test public void
    testNewClass() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                         "String",                NEW_CLASS_WITHOUT_KEYWORD, NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                         "String + String",       NEW_CLASS_WITHOUT_KEYWORD, NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(true,                       "String().isEmpty",      NEW_CLASS_WITHOUT_KEYWORD);
        ExpressionEvaluatorTest.assertExpressionEvaluationFails(
            "Cannot retrieve nonstatic attribute 'isEmpty' in static context",
            "String.isEmpty"
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                                   "java.lang.String",      NEW_CLASS_WITHOUT_KEYWORD, NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionParsingFails("Unknown variable 'javaa'",          "javaa.lang.String");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                                   "String()",              NEW_CLASS_WITHOUT_KEYWORD);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("foo",                                "String(\"foo\")",       NEW_CLASS_WITHOUT_KEYWORD);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                                   "new String",            NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                                   "new java.lang.String",  NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionParsingFails("Cannot load \"javaa.lang.String\"", "new javaa.lang.String");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                                   "new String()");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("foo",                                "new String(\"foo\")");

        ExpressionEvaluatorTest.assertExpressionEvaluationFails(
            "Cannot invoke non-static method 'java.lang.String.toString()' in static context",
            "String.toString()"
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                         "new String.toString()", NEW_CLASS_WITHOUT_PARENTHESES);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("",                         "new String.toString",   NEW_CLASS_WITHOUT_PARENTHESES);
    }

    @Test public void
    testNewArray() throws EvaluationException, ParseException {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(new String[2],                             "new String[2]");
        ExpressionEvaluatorTest.assertExpressionEvaluationFails("Argument is not an array",            "String[2]");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(new String[2][3][][],                      "new String[2][3][][]");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Unexpected end of input",                "new int");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Primary expected instead of ']'",        "new int[]");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(new int[3],                                "new int[3]");
        ExpressionEvaluatorTest.assertExpressionParsingFails("Primary expected instead of 'int'",      "int[3]");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(new int[2][3][][],                         "new int[2][3][][]");
    }

    @Test public void
    testSample() throws Exception {
        Expression e = new ExpressionEvaluator(new String[] { "a", "b" }).parse("a + b");
        Assert.assertEquals(7, e.evaluate("a", 3, "b", 4));
    }

    @Test public void
    testCast() throws Exception {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("abc",                                     "(Object) \"abc\"");
        ExpressionEvaluatorTest.assertExpressionEvaluationFails(
            "Cannot cast 'java.lang.String' to 'java.util.Map'",
            "(java.util.Map) \"abc\""
        );
    }

    @Test public void
    testArrayAccess() throws Exception {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(1,                                         "ia[0]");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("beta",                                    "sa[1]");
    }

    @Test public void
    testMisc() throws Exception {
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo("abc",                  "java.lang.String(\"abc\")", NEW_CLASS_WITHOUT_KEYWORD);
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(
            "",
            "java.lang.String",
            NEW_CLASS_WITHOUT_KEYWORD,
            NEW_CLASS_WITHOUT_PARENTHESES
        );
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(7,                      "7");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(System.out,             "System.out");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(System.out,             "java.lang.System.out");
        ExpressionEvaluatorTest.assertExpressionEvaluatesTo(Collections.EMPTY_LIST, "java.util.Collections.emptyList()");
    }

    @Test public void
    testExpandVariable() throws Exception {
        ExpressionEvaluatorTest.assertExpandedEquals("abc",                                            "abc");
        ExpressionEvaluatorTest.assertExpandedEquals("abc7.0def",                                      "abc#do#def");
        ExpressionEvaluatorTest.assertExpandingParseException("Unknown variable 'unknown'",            "abc#unknown#def");
    }

    @Test public void
    testExpandAttribute() throws Exception {
        ExpressionEvaluatorTest.assertExpandedEquals("abc9def",                                        "abc#da.Time#def");
        ExpressionEvaluatorTest.assertExpandingParseException("'IDENTIFIER' expected instead of '.'",  "abc#da..Time#def");
        ExpressionEvaluatorTest.assertExpandedEquals(
            "<!-- 'java.util.Date' has no field 'TTime' nor a method 'TTime()' or 'getTTime()' method -->",
            "abc#da.TTime#def"
        );
    }

    @Test public void
    testParsePart() throws Exception {

        ExpressionEvaluator ee = new ExpressionEvaluator();

        int[] offset = new int[1];

        ee.parsePart("1 + 3}xx", offset);
        Assert.assertEquals(5, offset[0]);
    }

    /**
     * For all possible parsing {@link Extension} combinations that contain the given <var>extensionVarargs</var>:
     * Asserts that the <var>expression</var> parses and evaluates to <var>expected</var>.
     * <p>
     *   For all possible parsing {@link Extension} combinations that <em>do not</em> contain all of the the given
     *   <var>extensionVarargs</var> (or if <var>extensionVarargs</var> is empty):
     *   Asserts that the <var>expression</var> does <em>not</em> parse.
     * </p>
     */
    private static void
    assertExpressionEvaluatesTo(
        @Nullable Object    expected,
        String              expression,
        Parser.Extension... extensionsVarargs
    ) throws EvaluationException, ParseException {
        EnumSet<Extension> extensions = (
            extensionsVarargs.length == 0
            ? EnumSet.noneOf(Extension.class)
            : EnumSet.copyOf(Arrays.asList(extensionsVarargs))
        );

        ExpressionEvaluator ee = new ExpressionEvaluator(ExpressionEvaluatorTest.VARIABLE_NAMES);

        for (Set<Extension> es : ExpressionEvaluatorTest.allSubsets(extensions)) {

            Parser<Expression, RuntimeException> parser = ee.parser(expression);
            parser.setExtensions(es);

            if (es.containsAll(extensions)) {

                // Expect NO parse exception iff the given extensions are enabled (plus any number of unrelated
                // extensions).
                try {
                    ExpressionEvaluatorTest.assertEquals2(expected, parser.parse().evaluate(ExpressionEvaluatorTest.VARIABLES));
                } catch (ParseException pe) {
                    throw ExceptionUtil.wrap("Parsing with extensions " + extensions, pe);
                }
            } else {

                // EXPECT a parse exception if not all of the given extensions are enabled.
                try {
                    parser.parse();
                    Assert.fail("Parse exception expected with extensions " + extensions + " disabled");
                } catch (ParseException pe) {
                    ;
                }
            }
        }
    }

    private static void
    assertExpressionParsingFails(String expectedExceptionMessageSuffix, String expression)
    throws EvaluationException {

        try {
            new ExpressionEvaluator(ExpressionEvaluatorTest.VARIABLE_NAMES).parse(expression);
            Assert.fail("ParseException expected");
        } catch (ParseException pe) {
            ExpressionEvaluatorTest.assertEndsWith(expectedExceptionMessageSuffix, pe.getMessage());
        }

        try {
            new ExpressionEvaluator(ExpressionEvaluatorTest.VARIABLE_NAMES).evaluate(expression, ExpressionEvaluatorTest.VARIABLES);
            Assert.fail("ParseException expected");
        } catch (ParseException pe) {
            ExpressionEvaluatorTest.assertEndsWith(expectedExceptionMessageSuffix, pe.getMessage());
        }
    }

    private static void
    assertExpressionEvaluationFails(String expectedExceptionMessageSuffix, String expression)
    throws ParseException {

        Expression parsedExpression = new ExpressionEvaluator(ExpressionEvaluatorTest.VARIABLE_NAMES).parse(expression);
        try {
            parsedExpression.evaluate(ExpressionEvaluatorTest.VARIABLES);
            Assert.fail("Exception expected on evaluation");
        } catch (Exception e) {
            ExpressionEvaluatorTest.assertEndsWith(expectedExceptionMessageSuffix, e.getMessage());
        }

        try {
            new ExpressionEvaluator(ExpressionEvaluatorTest.VARIABLE_NAMES).evaluate(expression, ExpressionEvaluatorTest.VARIABLES);
            Assert.fail("Exception expected on evaluation");
        } catch (Exception e) {
            ExpressionEvaluatorTest.assertEndsWith(expectedExceptionMessageSuffix, e.getMessage());
        }
    }

    // ----------------------

    private static void
    assertExpandedEquals(String expected, String s) throws Exception {
        Expression expression = ExpressionUtil.expand(s, ExpressionEvaluatorTest.VARIABLE_NAMES);
        Assert.assertEquals(expected, ExpressionUtil.evaluateLeniently(expression, ExpressionEvaluatorTest.VARIABLES));
    }

    private static void
    assertExpandingParseException(String expectedExceptionMessageSuffix, String s) {
        try {
            ExpressionUtil.expand(s, ExpressionEvaluatorTest.VARIABLE_NAMES);
            Assert.fail("ParseException expected");
        } catch (ParseException pe) {
            ExpressionEvaluatorTest.assertEndsWith(expectedExceptionMessageSuffix, pe.getMessage());
        }
    }

    // ----------------------

    private static void
    assertEquals2(@Nullable Object expected, @Nullable Object actual) {

        if (expected instanceof Object[] && actual instanceof Object[]) {
            Assert.assertArrayEquals((Object[]) expected, (Object[]) actual);
        } else
        if (expected instanceof int[] && actual instanceof int[]) {
            Assert.assertArrayEquals((int[]) expected, (int[]) actual);
        } else
        {
            Assert.assertEquals(expected, actual);
        }
    }

    private static void
    assertEndsWith(String suffix, String actual) {
        if (!actual.endsWith(suffix)) throw new ComparisonFailure("", suffix, actual);
    }

    /**
     * Copied from "de.unkrig.util.collections.Sets" to avoid a mutual dependency between commons-util and
     * commons-text.
     */
    public static <E> List<Set<E>>
    allSubsets(Set<E> set) {
        return ExpressionEvaluatorTest.allSubsets(set.iterator());
    }

    private static <E> List<Set<E>>
    allSubsets(Iterator<E> it) {

        if (!it.hasNext()) {
            List<Set<E>> result = new ArrayList<Set<E>>();
            result.add(Collections.<E>emptySet());
            return result;
        }

        E firstElement = it.next();

        List<Set<E>> allSubsets = ExpressionEvaluatorTest.allSubsets(it);

        List<Set<E>> tmp = new ArrayList<Set<E>>(allSubsets.size());
        for (Set<E> ss : allSubsets) {
            if (ss.isEmpty()) {
                tmp.add(Collections.singleton(firstElement));
            } else {
                Set<E> tmp2 = new HashSet<E>(ss);
                tmp2.add(firstElement);
                tmp.add(tmp2);
            }
        }

        allSubsets.addAll(tmp);

        return allSubsets;
    }
}
