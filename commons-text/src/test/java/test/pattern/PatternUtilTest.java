
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package test.pattern;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.text.pattern.PatternUtil;
import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF

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
        this.assertReplaceAllEquals("xxxAxxx\nxxxBxxx",     "xxxAxxx\nxxxBxxx", "A.*B",     "C");
        this.assertReplaceAllEquals("xxxCxxx",              "xxxAxxx\nxxxBxxx", "A.*\n.*B", "C");
        this.assertReplaceAllEquals("xxxCxxx",              "xxxAxxx\nxxxBxxx", "(?s)A.*B", "C"); // "(?s)" = DOTALL
        this.assertReplaceAllEquals("[xxxAxxx]\n[xxxBxxx]", "xxxAxxx\nxxxBxxx", ".+",       "[$0]");
        this.assertReplaceAllEquals("[xxxAxxx]\n[xxxBxxx]", "xxxAxxx\nxxxBxxx", ".*",       "[$0]");
        this.assertReplaceAllEquals("[xxx]",                "xxx",              ".*",       "[$0]");
        this.assertReplaceAllEquals("[xxxAxxx\nxxxBxxx]",   "xxxAxxx\nxxxBxxx", "(?s).*",   "[$0]"); // "(?s)" = DOTALL
    }

    @Test public void
    testSystemPropertyReplacer() {
        Assert.assertEquals(
            "file.separator is " + File.separator,
            PatternUtil.replaceAll(
                Pattern.compile("\\$\\{([^}]+)}").matcher("file.separator is ${file.separator}"),
                PatternUtil.systemPropertyReplacer()
            )
        );
    }

    @Test public void
    testConstantReplacer() {
        Assert.assertEquals(
            "H$1llo world",
            PatternUtil.replaceAll(
                Pattern.compile("e").matcher("Hello world"),
                PatternUtil.constantReplacer("$1")
            )
        );
    }

    @Test public void
    testReplacementStringReplacer() {
        Assert.assertEquals(
            "Hxe1xllo world",
            PatternUtil.replaceAll(
                Pattern.compile("e").matcher("Hello world"),
                PatternUtil.replacementStringReplacer("x$01x")
            )
        );
        Assert.assertEquals(
            "Hxexllo world",
            PatternUtil.replaceAll(
                Pattern.compile("(e)").matcher("Hello world"),
                PatternUtil.replacementStringReplacer("x$01x")
            )
        );
    }

    public void
    assertReplaceAllEquals(String expected, String subject, String regex, String replacement) throws IOException {
        StringWriter sw = new StringWriter();
        PatternUtil.replaceAll(new StringReader(subject), Pattern.compile(regex), replacement, sw);
        TestCase.assertEquals(expected, sw.toString());
    }
}
