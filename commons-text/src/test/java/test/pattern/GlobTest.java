
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test.pattern;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.Pattern2;

public
class GlobTest {

    @Test public void
    testCompile() {

        // SUPPRESS CHECKSTYLE L_PAREN__METH_INVOCATION|Whitespace:103
        Assert.assertTrue (Glob.compile("x", Pattern2.WILDCARD).matches("x"));
        Assert.assertFalse(Glob.compile("x", Pattern2.WILDCARD).matches("y"));
        Assert.assertTrue (Glob.compile("xxx", Pattern2.WILDCARD).matches("xxx"));
        Assert.assertFalse(Glob.compile("xxx", Pattern2.WILDCARD).matches("xx"));
        Assert.assertFalse(Glob.compile("xxx", Pattern2.WILDCARD).matches("xxxx"));
        Assert.assertFalse(Glob.compile("xxx", Pattern2.WILDCARD).matches("xxy"));
        Assert.assertFalse(Glob.compile("xxx", Pattern2.WILDCARD).matches("yxx"));
        Assert.assertFalse(Glob.compile("xxx", Pattern2.WILDCARD).matches("xyx"));

        Assert.assertTrue (Glob.compile("?xx", Pattern2.WILDCARD).matches("xxx"));
        Assert.assertTrue (Glob.compile("?xx", Pattern2.WILDCARD).matches("axx"));
        Assert.assertFalse(Glob.compile("?xx", Pattern2.WILDCARD).matches("aax"));
        Assert.assertFalse(Glob.compile("?xx", Pattern2.WILDCARD).matches("xx"));
        Assert.assertFalse(Glob.compile("?xx", Pattern2.WILDCARD).matches("xxxx"));
        Assert.assertTrue (Glob.compile("x?x", Pattern2.WILDCARD).matches("xxx"));
        Assert.assertTrue (Glob.compile("x?x", Pattern2.WILDCARD).matches("xax"));
        Assert.assertFalse(Glob.compile("x?x", Pattern2.WILDCARD).matches("aax"));
        Assert.assertFalse(Glob.compile("x?x", Pattern2.WILDCARD).matches("xx"));
        Assert.assertFalse(Glob.compile("x?x", Pattern2.WILDCARD).matches("xxxx"));
        Assert.assertTrue (Glob.compile("xx?", Pattern2.WILDCARD).matches("xxx"));
        Assert.assertTrue (Glob.compile("xx?", Pattern2.WILDCARD).matches("xxa"));
        Assert.assertFalse(Glob.compile("xx?", Pattern2.WILDCARD).matches("xaa"));
        Assert.assertFalse(Glob.compile("xx?", Pattern2.WILDCARD).matches("xx"));
        Assert.assertFalse(Glob.compile("xx?", Pattern2.WILDCARD).matches("xxxx"));

        Assert.assertTrue (Glob.compile("*xx", Pattern2.WILDCARD).matches("XXxx"));
        Assert.assertFalse(Glob.compile("*xx", Pattern2.WILDCARD).matches("XXx"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xXXx"));
        Assert.assertTrue (Glob.compile("xx*", Pattern2.WILDCARD).matches("xxXX"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xx"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xAx"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xAAx"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xAAAx"));
        Assert.assertTrue (Glob.compile("x*x", Pattern2.WILDCARD).matches("xAAAx"));
        Assert.assertFalse(Glob.compile("x*x", Pattern2.WILDCARD).matches("xA/Ax"));
        Assert.assertFalse(Glob.compile("x*x", Pattern2.WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        Assert.assertFalse(Glob.compile("x*x", Pattern2.WILDCARD).matches("xA!Ax"));
        Assert.assertTrue (Glob.compile("x**x", Pattern2.WILDCARD).matches("xAAAx"));
        Assert.assertTrue (Glob.compile("x**x", Pattern2.WILDCARD).matches("xA/Ax"));
        Assert.assertTrue (Glob.compile("x**x", Pattern2.WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        Assert.assertFalse(Glob.compile("x**x", Pattern2.WILDCARD).matches("xA!Ax"));
        Assert.assertTrue (Glob.compile("x***x", Pattern2.WILDCARD).matches("xAAAx"));
        Assert.assertTrue (Glob.compile("x***x", Pattern2.WILDCARD).matches("xA/Ax"));
        Assert.assertTrue (Glob.compile("x***x", Pattern2.WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        Assert.assertTrue (Glob.compile("x***x", Pattern2.WILDCARD).matches("xA!Ax"));

        Assert.assertTrue (Glob.compile("[ab]", Pattern2.WILDCARD).matches("a"));
        Assert.assertTrue (Glob.compile("[ab]", Pattern2.WILDCARD).matches("b"));
        Assert.assertFalse(Glob.compile("[ab]", Pattern2.WILDCARD).matches("c"));
        Assert.assertTrue (Glob.compile("[ab]", Pattern2.WILDCARD).matches("")); // <= Container match
        Assert.assertFalse(Glob.compile("[^ab]", Pattern2.WILDCARD).matches("a"));
        Assert.assertFalse(Glob.compile("[^ab]", Pattern2.WILDCARD).matches("b"));
        Assert.assertTrue (Glob.compile("[^ab]", Pattern2.WILDCARD).matches("c"));
        Assert.assertFalse(Glob.compile("[h-j]", Pattern2.WILDCARD).matches("g"));
        Assert.assertTrue (Glob.compile("[h-j]", Pattern2.WILDCARD).matches("h"));
        Assert.assertTrue (Glob.compile("[h-j]", Pattern2.WILDCARD).matches("i"));
        Assert.assertTrue (Glob.compile("[h-j]", Pattern2.WILDCARD).matches("j"));
        Assert.assertFalse(Glob.compile("[h-j]", Pattern2.WILDCARD).matches("k"));
        Assert.assertTrue (Glob.compile("[^h-j]", Pattern2.WILDCARD).matches("g"));
        Assert.assertFalse(Glob.compile("[^h-j]", Pattern2.WILDCARD).matches("h"));
        Assert.assertFalse(Glob.compile("[^h-j]", Pattern2.WILDCARD).matches("i"));
        Assert.assertFalse(Glob.compile("[^h-j]", Pattern2.WILDCARD).matches("j"));
        Assert.assertTrue (Glob.compile("[^h-j]", Pattern2.WILDCARD).matches("k"));

        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaaddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaaccddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaaxddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabcddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabbbddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabbbbddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,1}ddd", Pattern2.WILDCARD).matches("aaabbccddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,}ddd", Pattern2.WILDCARD).matches("aaaddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,}ddd", Pattern2.WILDCARD).matches("aaabbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,}ddd", Pattern2.WILDCARD).matches("aaaccbbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){0,}ddd", Pattern2.WILDCARD).matches("aaabbccbbddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){0,}ddd", Pattern2.WILDCARD).matches("aaabbccbbbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){1,}ddd", Pattern2.WILDCARD).matches("aaabbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){1,}ddd", Pattern2.WILDCARD).matches("aaaccddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc){1,}ddd", Pattern2.WILDCARD).matches("aaaccbbddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){1,}ddd", Pattern2.WILDCARD).matches("aaaddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc){1,}ddd", Pattern2.WILDCARD).matches("aaabccbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc)ddd", Pattern2.WILDCARD).matches("aaabbddd"));
        Assert.assertTrue (Glob.compile("aaa(bb|cc)ddd", Pattern2.WILDCARD).matches("aaaccddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc)ddd", Pattern2.WILDCARD).matches("aaaddd"));
        Assert.assertFalse(Glob.compile("aaa(bb|cc)ddd", Pattern2.WILDCARD).matches("aaabbbbddd"));

        Assert.assertTrue (Glob.compile("aaa\\*ddd",     Pattern2.WILDCARD).matches("aaa*ddd"));
        Assert.assertTrue (Glob.compile("aaa\\[a-b]ddd", Pattern2.WILDCARD).matches("aaa[a-b]ddd"));

        Assert.assertTrue (Glob.compile("a/b", Pattern2.WILDCARD).matches("a" + File.separatorChar + "b"));

        // Container matches.
        Assert.assertTrue (Glob.compile("", Pattern2.WILDCARD).matches(""));
        Assert.assertTrue (Glob.compile("x", Pattern2.WILDCARD).matches(""));
        Assert.assertTrue (Glob.compile("?", Pattern2.WILDCARD).matches(""));
        Assert.assertTrue (Glob.compile("*", Pattern2.WILDCARD).matches(""));

        Assert.assertTrue (Glob.compile("a/b/c", Pattern2.WILDCARD).matches("a/b/"));
        Assert.assertTrue (Glob.compile("a/b!c", Pattern2.WILDCARD).matches("a/b!"));
        Assert.assertTrue (Glob.compile("**",    Pattern2.WILDCARD).matches("a/b/"));
        Assert.assertFalse(Glob.compile("*",     Pattern2.WILDCARD).matches("a/b/"));
    }

    @Test public void
    testAlternatives() {
        // SUPPRESS CHECKSTYLE L_PAREN__METH_INVOCATION|Whitespace:25
        Assert.assertTrue (Glob.compile("x", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("x"));
        Assert.assertFalse(Glob.compile("x", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("y"));
        Assert.assertTrue (Glob.compile("*", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches(""));
        Assert.assertTrue (Glob.compile("*", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("xxx"));

        Assert.assertTrue (Glob.compile("a,b,c", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a"));
        Assert.assertTrue (Glob.compile("a,b,c", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("b"));
        Assert.assertTrue (Glob.compile("a,b,c", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("c"));
        Assert.assertFalse(Glob.compile("a,b,c", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("d"));

        Assert.assertFalse(Glob.compile("a,b",   Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a,b"));
        Assert.assertTrue (Glob.compile("a\\,b", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a,b"));

        Assert.assertTrue (Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches(""));
        Assert.assertTrue (Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a"));
        Assert.assertFalse(Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("aa"));
        Assert.assertFalse(Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("aaa"));
        Assert.assertTrue (Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("aaaa"));
        Assert.assertFalse(Glob.compile("a*~aa*,aaaa", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("aaaaa"));

        // Container matches.
        Assert.assertTrue (Glob.compile("~a/a",    Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a/"));
        Assert.assertTrue (Glob.compile("~a/**a*", Pattern2.WILDCARD | Glob.INCLUDES_EXCLUDES).matches("a/a/"));
    }
}
