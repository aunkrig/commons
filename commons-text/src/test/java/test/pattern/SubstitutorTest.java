
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.text.pattern.PatternUtil;
import de.unkrig.commons.text.pattern.Substitutor;

/**
 * @author Arno
 *
 */
public class SubstitutorTest {

    @Test public void
    testSubstitutor() {
        Substitutor s = new Substitutor(Pattern.compile("A..B"), PatternUtil.replacementStringMatchReplacer("x"));

        List<CharSequence> result = new ArrayList<CharSequence>();
        for (Character c : StringUtil.asIterable("  AyyB  AyyC  Ay")) {
            result.add(s.transform(new String(new char[] { c })));
        }
        result.add(s.transform(""));

        Assert.assertArrayEquals(new String[] {
            " ",    // " "
            " ",    // " "
            "",     // "A"
            "",     // "y"
            "",     // "y"
            "x",    // "B"
            " ",    // " "
            " ",    // " "
            "",     // "A"
            "",     // "y"
            "",     // "y"
            "AyyC", // "C"
            " ",    // " "
            " ",    // " "
            "",     // "A"
            "",     // "y"
            "Ay",   // ""
        }, result.toArray());
    }
}
