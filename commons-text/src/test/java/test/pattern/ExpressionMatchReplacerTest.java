
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

package test.pattern;

import java.text.SimpleDateFormat;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.pattern.ExpressionMatchReplacer;
import de.unkrig.commons.text.pattern.PatternUtil;

// SUPPRESS CHECKSTYLE Javadoc:9999

public
class ExpressionMatchReplacerTest {

    @Test public void
    testReplaceAll() throws ParseException {

        Assert.assertEquals(
            "To boldly go Where no coW has gone before",
            ExpressionMatchReplacer.replaceSome(
                Pattern.compile("w").matcher("To boldly go where no cow has gone before"),
                "m.group.toUpperCase()"
            )
        );
    }

    @Test public void
    testParse1() throws ParseException {

        Function<MatchResult, String> matchReplacer = ExpressionMatchReplacer.parse("m.group.toUpperCase()");

        Pattern p = Pattern.compile("\\w");

        Matcher m = p.matcher("This is a mad, mad world...");
        Assert.assertEquals("THIS IS A MAD, MAD WORLD...", PatternUtil.replaceSome(m, matchReplacer));
    }

    @Test public void
    testParse2() throws ParseException {

        Function<MatchResult, String> matchReplacer = ExpressionMatchReplacer.parse(
            "prefix + new StringBuilder(m.group).reverse()",
            "prefix", "pre-" // SUPPRESS CHECKSTYLE Wrap
        );

        Pattern p = Pattern.compile("\\w+");

        Matcher m = p.matcher("This is a mad, mad world...");

        Assert.assertEquals(
            "pre-sihT pre-si pre-a pre-dam, pre-dam pre-dlrow...",
            PatternUtil.replaceSome(m, matchReplacer)
        );
    }

    @Test public void
    testGet() throws ParseException {

        Expression
        expression = new ExpressionEvaluator("prefix", "m").parse("prefix + new StringBuilder(m.group).reverse()");

        Function<MatchResult, String> matchReplacer = ExpressionMatchReplacer.get(
            expression,
            "prefix", "pre-" // SUPPRESS CHECKSTYLE Wrap
        );

        Pattern p = Pattern.compile("\\w+");

        Matcher m = p.matcher("This is a mad, mad world...");

        Assert.assertEquals(
            "pre-sihT pre-si pre-a pre-dam, pre-dam pre-dlrow...",
            PatternUtil.replaceSome(m, matchReplacer)
        );
    }

    @Test public void
    testParseExt1() throws ParseException {

        Mapping<String, Object> variables = Mappings.<String, Object>mapping("abc", "ABC");

        Transformer<Mapping<String, ?>, Function<MatchResult, String>>
        matchReplacer = ExpressionMatchReplacer.parseExt(
            "FOO $1 $abc ${abc + \"\"} BAR \\x00 \\u0000 \\x{fffff} \\n \\\\ \\Z \\Q\\Q$\\E \\\\ \\Q\\$",
            Mappings.containsKeyPredicate(variables)
        );

        Matcher m = Pattern.compile("(.*)").matcher("ONE");
        Assert.assertTrue(m.matches());

        Assert.assertEquals(
            "FOO ONE ABC ABC BAR \0 \0 " + new String(Character.toChars(0xfffff)) + " \n \\ Z \\Q$ \\ \\$",
            matchReplacer.transform(variables).call(m)
        );
    }

    @Test public void
    testParseExt2() throws ParseException {

        Mapping<String, Object> variables = Mappings.<String, Object>mapping("map", Mappings.mapping(3, 3.0));

        final Transformer<Mapping<String, ?>, Function<MatchResult, String>>
        matchReplacer = ExpressionMatchReplacer.parseExt(
            "${map.get(3)}",
            Mappings.containsKeyPredicate(variables)
        );

        Matcher m = Pattern.compile("(.*)").matcher("ONE");
        Assert.assertTrue(m.matches());

        Assert.assertEquals("3.0", matchReplacer.transform(variables).call(m));
    }

    @Test public void
    testParseExt3() throws ParseException {

        // Real-world example: Replace milliseconds-since-1970 integer with the clear-text date.
        String in  = "importlog-1550068207009.txt";
        String out = "importlog-2019-02-13T03:30:07.009.txt";

        Mapping<String, ?> variables = Mappings.mapping("df", new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"));

        final Transformer<Mapping<String, ?>, Function<MatchResult, String>>
        matchReplacer = ExpressionMatchReplacer.parseExt(
            "${df.format(new java.util.Date(Long.parseLong(m.group(1))))}",
            Mappings.containsKeyPredicate(variables)
        );

        Assert.assertEquals(
            out,
            PatternUtil.replaceSome(
                Pattern.compile("(155\\d{10})(?!\\d)").matcher(in), // matcher
                matchReplacer.transform(variables)                  // matchReplacer
            )
        );
    }
}
