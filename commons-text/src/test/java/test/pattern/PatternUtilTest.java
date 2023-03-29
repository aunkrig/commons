
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

// SUPPRESS CHECKSTYLE JavadocMethod|JavadocType|LineLength:9999

package test.pattern;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.text.Segment;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.CharFilter;
import de.unkrig.commons.io.CharFilterReader;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.PatternUtil;
import de.unkrig.commons.text.pattern.Substitutor;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

public
class PatternUtilTest extends TestCase {

    @Test public void
    test1() throws IOException {
        this.assertReplaceAllEquals("xxxByyy",     "xxxAyyy",     "A", "B");
        this.assertReplaceAllEquals("xxxAyyy",     "xxxAyyy",     "B", "C");
        this.assertReplaceAllEquals("xxxByyyBzzz", "xxxAyyyAzzz", "A", "B");
    }

    @Test public void
    test2() throws IOException {
        this.assertReplaceAllEquals("xxx>>>A<<<yyy", "xxxAyyy", "A",   ">>>$0<<<");
        this.assertReplaceAllEquals("xxx>>>A<<<yyy", "xxxAyyy", "(A)", ">>>$1<<<");
    }

    @Test public void
    test3() throws IOException {
        this.assertReplaceAllEquals("xxxxxxxB",     "xxxxxxxA",     "A",  "B");
        this.assertReplaceAllEquals("xxxxxxxByyy",  "xxxxxxxAyyy",  "A",  "B");
        this.assertReplaceAllEquals("xxxxxxxBB",    "xxxxxxxAA",    "AA", "BB");
        this.assertReplaceAllEquals("xxxxxxxBByyy", "xxxxxxxAAyyy", "AA", "BB");
        this.assertReplaceAllEquals("xxxxxxxxB",    "xxxxxxxxA",    "A",  "B");
        this.assertReplaceAllEquals("xxxxxxxxByyy", "xxxxxxxxAyyy", "A",  "B");
    }

    @Test public void
    test4() throws IOException {
        this.assertReplaceAllEquals("[AAA][]",                  "AAA",              ".*",       "[$0]");
        this.assertReplaceAllEquals("xxxAxxx\nxxxBxxx",         "xxxAxxx\nxxxBxxx", "A.*B",     "C");
        this.assertReplaceAllEquals("xxxCxxx",                  "xxxAxxx\nxxxBxxx", "A.*\n.*B", "C");
        this.assertReplaceAllEquals("xxxCxxx",                  "xxxAxxx\nxxxBxxx", "(?s)A.*B", "C");    // "(?s)" = DOTALL
        this.assertReplaceAllEquals("[xxxAxxx]\n[xxxBxxx]",     "xxxAxxx\nxxxBxxx", ".+",       "[$0]");
        this.assertReplaceAllEquals("_xxxAxxx___\n_xxxBxxx___", "xxxAxxx\nxxxBxxx", ".*",       "_$0_");
        this.assertReplaceAllEquals("_xxx___",                  "xxx",              ".*",       "_$0_");
        this.assertReplaceAllEquals("[xxxAxxx\nxxxBxxx][]",     "xxxAxxx\nxxxBxxx", "(?s).*",   "[$0]"); // "(?s)" = DOTALL
        this.assertReplaceAllEquals("aaa\nBbb\nccc",            "aaa\nbbb\nccc",    "(?m)^b",   "B");    // "(?m)" = MULTILINE
    }

    @Test public void
    testSystemPropertyReplacer() {
        Assert.assertEquals(
            "file.separator is " + File.separator,
            PatternUtil.replaceSome(
                Pattern.compile("\\$\\{([^}]+)}").matcher("file.separator is ${file.separator}"),
                PatternUtil.SYSTEM_PROPERTY_MATCH_REPLACER
            )
        );
    }

    @Test public void
    testConstantReplacer() {
        Assert.assertEquals(
            "H$1llo world",
            PatternUtil.replaceSome(
                Pattern.compile("e").matcher("Hello world"),
                PatternUtil.constantMatchReplacer("$1")
            )
        );
    }

    @Test public void
    testReplacementStringReplacer() {
        Assert.assertEquals(
            "Hxe1xllo world",
            PatternUtil.replaceSome(
                Pattern.compile("e").matcher("Hello world"),
                PatternUtil.<NoException>replacementStringMatchReplacer("x$01x")
            )
        );
        Assert.assertEquals(
            "Hxexllo world",
            PatternUtil.replaceSome(
                Pattern.compile("(e)").matcher("Hello world"),
                PatternUtil.<NoException>replacementStringMatchReplacer("x$01x")
            )
        );
    }

    @Test public void
    testReplacementWithCharFilterReader() throws IOException {
        Reader r = new CharFilterReader(
            new StringReader("line1\nline2 \t// COMMENT\nline3"),
            new CharFilter<Void>() {

                @Override @Nullable public Void
                run(Reader in, Writer out) throws IOException {
                    PatternUtil.replaceAll(in, Pattern.compile("\\s*//.*$", Pattern.MULTILINE), "", out); // Strip C++-style comments.
                    return null;
                }
            }
        );
        Assert.assertEquals("line1\nline2\nline3", PatternUtilTest.readAll(r));
    }

    /**
     * Reads THIS source file, executes a transformation, then a reverse stransformation, then checks that the result
     * equals the original.
     */
    @Test public void
    test5() throws IOException {
        Reader r = new InputStreamReader(new FileInputStream("src/test/java/" + this.getClass().getName().replace('.', '/') + ".java"), Charset.forName("UTF-8"));
        try {
            char[] buffer = new char[300];

            StringBuilder            orig      = new StringBuilder();
            StringBuilder            patched   = new StringBuilder();
            StringBuilder            repatched = new StringBuilder();
            Substitutor<NoException> patcher   = PatternUtil.substitutor(Pattern.compile("StringBuilder"), "STRING_" + "BUILDER");
            Substitutor<NoException> repatcher = PatternUtil.substitutor(Pattern.compile("ST" + "RING_BUILDER"), "StringBuilder");
            for (;;) {
                int n = r.read(buffer);
                if (n == -1) break;
                CharSequence s = new Segment(buffer, 0, n);

                orig.append(s);

                s = patcher.transform(s);
                if (s.length() > 0) {
                    patched.append(s);

                    s = repatcher.transform(s);
                    if (s.length() > 0) {
                        repatched.append(s);
                    }
                }
            }
            r.close();

            CharSequence s = patcher.transform("");
            if (s.length() > 0) {
                patched.append(s);
                repatched.append(repatcher.transform(s));
            }
            repatched.append(repatcher.transform(""));

            Assert.assertNotEquals(orig.toString(), patched.toString());
            Assert.assertNotEquals(patched.toString(), repatched.toString());
            Assert.assertEquals(orig.toString(), repatched.toString());

            String x = patcher.transform(orig).toString() + patcher.transform("");
            Assert.assertEquals(patched.toString(), x);
            x = repatcher.transform(x).toString() + repatcher.transform("");
            Assert.assertEquals(orig.toString(), x);
        } finally {
            try { r.close(); } catch (Exception e) {}
        }
    }

    @Test public void
    testOverlappingMatches() throws IOException {
        this.assertReplaceAllEquals("BaaaBaaaBaaaBaaaBaaaBaaaBaaaBaaa", "aaaaaaaa", "a", "Baaa");
        PatternUtilTest.assertReplaceNoneEquals("aaaaaaaa",                        "aaaaaaaa", "a");
    }

    @Test public void
    testLongLookbehind9() throws IOException {
        this.assertReplaceAllEquals("abcdefghijklmnopqrSt", "abcdefghijklmnopqrst", "(?<=jklmnopqr)s", "S");
    }

    @Test public void
    testLongLookbehind10() throws IOException {
        this.assertReplaceAllEquals("abcdefghijklmnopqrSt", "abcdefghijklmnopqrst", "(?<=ijklmnopqr)s", "S");
    }

    @Test public void
    testLongLookbehind11() throws IOException {

        // The "Substitutor" has a limit look-behind (by default 10); so it cannot recognize the following 11-char
        // lookbehind.
        try {
            this.assertReplaceAllEquals("abcdefghijklmnopqrSt", "abcdefghijklmnopqrst", "(?<=hijklmnopqr)s", "S");
            TestCase.fail();
        } catch (ComparisonFailure cf) {}
    }

    @Test public void
    testLiteralInfix() {
        Assert.assertArrayEquals(new String[] { "abc", "" }, PatternUtil.constantPrefix("abc"));

        // Meta characters.
        Assert.assertArrayEquals(new String[] { "abc",       "." }, PatternUtil.constantPrefix("abc."));
        Assert.assertArrayEquals(new String[] { "abc.*?",    ""  }, PatternUtil.constantPrefix("abc\\Q.*?"));
        Assert.assertArrayEquals(new String[] { "abc.*?",    "." }, PatternUtil.constantPrefix("abc\\Q.*?\\E."));
        Assert.assertArrayEquals(new String[] { "abc.*?def", ""  }, PatternUtil.constantPrefix("abc\\Q.*?\\Edef"));
        Assert.assertArrayEquals(new String[] { ".",         ""  }, PatternUtil.constantPrefix("\\."));

        // Octal literals.
        Assert.assertArrayEquals(new String[] { "\0",         "" }, PatternUtil.constantPrefix("\\00"));
        Assert.assertArrayEquals(new String[] { "\0def",      "" }, PatternUtil.constantPrefix("\\00def"));
        Assert.assertArrayEquals(new String[] { "\u0001",     "" }, PatternUtil.constantPrefix("\\01"));
        Assert.assertArrayEquals(new String[] { "\u0001def",  "" }, PatternUtil.constantPrefix("\\01def"));
        Assert.assertArrayEquals(new String[] { "\n",         "" }, PatternUtil.constantPrefix("\\012"));
        Assert.assertArrayEquals(new String[] { "\ndef",      "" }, PatternUtil.constantPrefix("\\012def"));
        Assert.assertArrayEquals(new String[] { "\u0053",     "" }, PatternUtil.constantPrefix("\\0123"));
        Assert.assertArrayEquals(new String[] { "\u0053def",  "" }, PatternUtil.constantPrefix("\\0123def"));
        Assert.assertArrayEquals(new String[] { "\u00232",    "" }, PatternUtil.constantPrefix("\\0432"));
        Assert.assertArrayEquals(new String[] { "\u00232def", "" }, PatternUtil.constantPrefix("\\0432def"));

        // Hex literals. (SOUTH WEST POINTING LEAF = U+1F651 (128593) = 125*1024 + 593 = D87D,EE51)
        Assert.assertArrayEquals(new String[] { "\ud87d\uee51def", "" }, PatternUtil.constantPrefix("\\x{1f651}def"));
    }

    // ---------------------------------------------------------------

    private static String
    readAll(Reader r) throws IOException {
        StringWriter sw = new StringWriter();
        IoUtil.copy(r, true, sw, false);
        return sw.toString();
    }

    public void
    assertReplaceAllEquals(String expected, String subject, String regex, String replacementString) throws IOException {

        Pattern pattern = Pattern.compile(regex);

        // First of all, verify that "java.util.regex.Pattern" actually yields the SAME result.
        Assert.assertEquals("Matcher.replaceAll()", expected, pattern.matcher(subject).replaceAll(replacementString));

        // Now, test "PatternUtil.replaceAll(Reader, ...)".
        {
            StringWriter sw = new StringWriter();
            PatternUtil.replaceAll(new StringReader(subject), pattern, replacementString, sw);
            TestCase.assertEquals("PatternUtil.replaceAll()", expected, sw.toString());
        }

        // Then, test "Substitutor".
        {
            Substitutor<NoException> t = PatternUtil.substitutor(pattern, replacementString);
            TestCase.assertEquals("PatternUtil.substitutor()", expected, t.transform(subject).toString() + t.transform(""));
        }

        // Test the "Substitutor" with a sequence of single-character strings.
        {
            Substitutor<NoException> t  = PatternUtil.substitutor(pattern, replacementString);
            StringBuilder            sb = new StringBuilder();
            for (Character c : StringUtil.asIterable(subject)) sb.append(t.transform(new String(new char[] { c })));
            sb.append(t.transform(""));
            TestCase.assertEquals("PatternUtil.substitutor()", expected, sb.toString());
        }

        // Then, test the "replaceAllFilterReader()".
        Assert.assertEquals(
            "PatternUtil.replaceAllFilterReader()",
            expected,
            PatternUtilTest.readAll(PatternUtil.replaceAllFilterReader(
                new StringReader(subject),
                pattern,
                PatternUtil.<IOException>replacementStringMatchReplacer(replacementString)
            ))
        );

        // Then, test the "replaceAllFilterWriter()".
        {
            StringWriter sw = new StringWriter();

            Writer w = PatternUtil.replaceAllFilterWriter(
                pattern,
                PatternUtil.<IOException>replacementStringMatchReplacer(replacementString),
                sw
            );

            w.write(subject);
            w.close();
            Assert.assertEquals("PatternUtil.replaceAllFilterReader()", expected, sw.toString());
        }
    }

    private static void
    assertReplaceNoneEquals(String expected, String subject, String regex) {
        Assert.assertEquals(expected, PatternUtil.replaceSome(
            Pattern.compile(regex).matcher(subject),
            new Function<MatchResult, CharSequence>() {
                @Override @Nullable public CharSequence call(@Nullable MatchResult matchResult) { return null; }
            }
        ));
    }
}
