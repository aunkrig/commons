
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

import static de.unkrig.commons.text.pattern.Glob.INCLUDES_EXCLUDES;
import static de.unkrig.commons.text.pattern.Glob.compile;
import static de.unkrig.commons.text.pattern.Pattern2.WILDCARD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class GlobTest {

    @Test public void
    testCompile() {
        // CHECKSTYLE L_PAREN__METH_INVOCATION:OFF
        assertTrue (compile("", WILDCARD).matches(""));
        assertFalse(compile("x", WILDCARD).matches(""));
        assertFalse(compile("?", WILDCARD).matches(""));
        assertTrue (compile("*", WILDCARD).matches(""));

        assertTrue (compile("x", WILDCARD).matches("x"));
        assertFalse(compile("x", WILDCARD).matches("y"));
        assertTrue (compile("xxx", WILDCARD).matches("xxx"));
        assertFalse(compile("xxx", WILDCARD).matches("xx"));
        assertFalse(compile("xxx", WILDCARD).matches("xxxx"));
        assertFalse(compile("xxx", WILDCARD).matches("xxy"));
        assertFalse(compile("xxx", WILDCARD).matches("yxx"));
        assertFalse(compile("xxx", WILDCARD).matches("xyx"));

        assertTrue (compile("?xx", WILDCARD).matches("xxx"));
        assertTrue (compile("?xx", WILDCARD).matches("axx"));
        assertFalse(compile("?xx", WILDCARD).matches("aax"));
        assertFalse(compile("?xx", WILDCARD).matches("xx"));
        assertFalse(compile("?xx", WILDCARD).matches("xxxx"));
        assertTrue (compile("x?x", WILDCARD).matches("xxx"));
        assertTrue (compile("x?x", WILDCARD).matches("xax"));
        assertFalse(compile("x?x", WILDCARD).matches("aax"));
        assertFalse(compile("x?x", WILDCARD).matches("xx"));
        assertFalse(compile("x?x", WILDCARD).matches("xxxx"));
        assertTrue (compile("xx?", WILDCARD).matches("xxx"));
        assertTrue (compile("xx?", WILDCARD).matches("xxa"));
        assertFalse(compile("xx?", WILDCARD).matches("xaa"));
        assertFalse(compile("xx?", WILDCARD).matches("xx"));
        assertFalse(compile("xx?", WILDCARD).matches("xxxx"));

        assertTrue (compile("*xx", WILDCARD).matches("XXxx"));
        assertFalse(compile("*xx", WILDCARD).matches("XXx"));
        assertTrue (compile("x*x", WILDCARD).matches("xXXx"));
        assertTrue (compile("xx*", WILDCARD).matches("xxXX"));
        assertTrue (compile("x*x", WILDCARD).matches("xx"));
        assertTrue (compile("x*x", WILDCARD).matches("xAx"));
        assertTrue (compile("x*x", WILDCARD).matches("xAAx"));
        assertTrue (compile("x*x", WILDCARD).matches("xAAAx"));
        assertTrue (compile("x*x", WILDCARD).matches("xAAAx"));
        assertFalse(compile("x*x", WILDCARD).matches("xA/Ax"));
        assertFalse(compile("x*x", WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        assertFalse(compile("x*x", WILDCARD).matches("xA!Ax"));
        assertTrue (compile("x**x", WILDCARD).matches("xAAAx"));
        assertTrue (compile("x**x", WILDCARD).matches("xA/Ax"));
        assertTrue (compile("x**x", WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        assertFalse(compile("x**x", WILDCARD).matches("xA!Ax"));
        assertTrue (compile("x***x", WILDCARD).matches("xAAAx"));
        assertTrue (compile("x***x", WILDCARD).matches("xA/Ax"));
        assertTrue (compile("x***x", WILDCARD).matches("xA" + File.separatorChar + "Ax"));
        assertTrue (compile("x***x", WILDCARD).matches("xA!Ax"));

        assertTrue (compile("[ab]", WILDCARD).matches("a"));
        assertTrue (compile("[ab]", WILDCARD).matches("b"));
        assertFalse(compile("[ab]", WILDCARD).matches("c"));
        assertFalse(compile("[ab]", WILDCARD).matches(""));
        assertFalse(compile("[^ab]", WILDCARD).matches("a"));
        assertFalse(compile("[^ab]", WILDCARD).matches("b"));
        assertTrue (compile("[^ab]", WILDCARD).matches("c"));
        assertFalse(compile("[h-j]", WILDCARD).matches("g"));
        assertTrue (compile("[h-j]", WILDCARD).matches("h"));
        assertTrue (compile("[h-j]", WILDCARD).matches("i"));
        assertTrue (compile("[h-j]", WILDCARD).matches("j"));
        assertFalse(compile("[h-j]", WILDCARD).matches("k"));
        assertTrue (compile("[^h-j]", WILDCARD).matches("g"));
        assertFalse(compile("[^h-j]", WILDCARD).matches("h"));
        assertFalse(compile("[^h-j]", WILDCARD).matches("i"));
        assertFalse(compile("[^h-j]", WILDCARD).matches("j"));
        assertTrue (compile("[^h-j]", WILDCARD).matches("k"));

        assertTrue (compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaaddd"));
        assertTrue (compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabbddd"));
        assertTrue (compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaaccddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaaxddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabcddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabbbddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabbbbddd"));
        assertFalse(compile("aaa(bb|cc){0,1}ddd", WILDCARD).matches("aaabbccddd"));
        assertTrue (compile("aaa(bb|cc){0,}ddd", WILDCARD).matches("aaaddd"));
        assertTrue (compile("aaa(bb|cc){0,}ddd", WILDCARD).matches("aaabbddd"));
        assertTrue (compile("aaa(bb|cc){0,}ddd", WILDCARD).matches("aaaccbbddd"));
        assertTrue (compile("aaa(bb|cc){0,}ddd", WILDCARD).matches("aaabbccbbddd"));
        assertFalse(compile("aaa(bb|cc){0,}ddd", WILDCARD).matches("aaabbccbbbddd"));
        assertTrue (compile("aaa(bb|cc){1,}ddd", WILDCARD).matches("aaabbddd"));
        assertTrue (compile("aaa(bb|cc){1,}ddd", WILDCARD).matches("aaaccddd"));
        assertTrue (compile("aaa(bb|cc){1,}ddd", WILDCARD).matches("aaaccbbddd"));
        assertFalse(compile("aaa(bb|cc){1,}ddd", WILDCARD).matches("aaaddd"));
        assertFalse(compile("aaa(bb|cc){1,}ddd", WILDCARD).matches("aaabccbddd"));
        assertTrue (compile("aaa(bb|cc)ddd", WILDCARD).matches("aaabbddd"));
        assertTrue (compile("aaa(bb|cc)ddd", WILDCARD).matches("aaaccddd"));
        assertFalse(compile("aaa(bb|cc)ddd", WILDCARD).matches("aaaddd"));
        assertFalse(compile("aaa(bb|cc)ddd", WILDCARD).matches("aaabbbbddd"));

        assertTrue (compile("aaa\\*ddd",     WILDCARD).matches("aaa*ddd"));
        assertTrue (compile("aaa\\[a-b]ddd", WILDCARD).matches("aaa[a-b]ddd"));

        assertTrue (compile("a/b", WILDCARD).matches("a" + File.separatorChar + "b"));

        // Container matches.
        assertTrue (compile("a/b/c", WILDCARD).matches("a/b/"));
        assertTrue (compile("a/b!c", WILDCARD).matches("a/b!"));
        assertTrue (compile("**",    WILDCARD).matches("a/b/"));
        assertFalse(compile("*",     WILDCARD).matches("a/b/"));
        // CHECKSTYLE L_PAREN__METH_INVOCATION:ON
    }

    @Test public void
    testAlternatives() {
        // CHECKSTYLE L_PAREN__METH_INVOCATION:OFF
        assertTrue (compile("x", WILDCARD | INCLUDES_EXCLUDES).matches("x"));
        assertFalse(compile("x", WILDCARD | INCLUDES_EXCLUDES).matches("y"));
        assertTrue (compile("*", WILDCARD | INCLUDES_EXCLUDES).matches(""));
        assertTrue (compile("*", WILDCARD | INCLUDES_EXCLUDES).matches("xxx"));

        assertTrue (compile("a,b,c", WILDCARD | INCLUDES_EXCLUDES).matches("a"));
        assertTrue (compile("a,b,c", WILDCARD | INCLUDES_EXCLUDES).matches("b"));
        assertTrue (compile("a,b,c", WILDCARD | INCLUDES_EXCLUDES).matches("c"));
        assertFalse(compile("a,b,c", WILDCARD | INCLUDES_EXCLUDES).matches("d"));

        assertFalse(compile("a,b",   WILDCARD | INCLUDES_EXCLUDES).matches("a,b"));
        assertTrue (compile("a\\,b", WILDCARD | INCLUDES_EXCLUDES).matches("a,b"));

        assertFalse(compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches(""));
        assertTrue (compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches("a"));
        assertFalse(compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches("aa"));
        assertFalse(compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches("aaa"));
        assertTrue (compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches("aaaa"));
        assertFalse(compile("a*~aa*,aaaa", WILDCARD | INCLUDES_EXCLUDES).matches("aaaaa"));

        // Container matches.
        assertTrue (compile("~a/a",    WILDCARD | INCLUDES_EXCLUDES).matches("a/"));
        assertTrue (compile("~a/**a*", WILDCARD | INCLUDES_EXCLUDES).matches("a/a/"));
        // CHECKSTYLE L_PAREN__METH_INVOCATION:ON
    }
}
