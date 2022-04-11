
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.Functions;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Finders.MatchResult2;
import de.unkrig.commons.text.pattern.PatternUtil;
import de.unkrig.commons.text.pattern.Substitutor;

public
class SubstitutorTest {

    @Test public void
    testSubstitutor1() {

        Substitutor<NoException> s = Substitutor.create(
            Pattern.compile("A..B"),
            PatternUtil.<NoException>replacementStringMatchReplacer("x")
        );

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

    @Test public void
    testSubstitutor2() {

        final Pattern pattern = Pattern.compile("(A)(B)");

        final int[] replacerCallCount = new int[1];

        FunctionWhichThrows<MatchResult2, CharSequence, NoException>
        matchReplacer = new FunctionWhichThrows<MatchResult2, CharSequence, NoException>() {

            @Override @Nullable public CharSequence
            call(@Nullable MatchResult2 mr2) {

                assert mr2 != null;

                switch (replacerCallCount[0]++) {

                case 0:
                case 2:
                case 4:
                    return null;

                case 1:
                case 3:
                    return "*";

                case 5:
                    Assert.assertSame(pattern, mr2.pattern());
                    Assert.assertEquals(mr2.group(), "AB");
                    Assert.assertEquals(mr2.groupCount(), 2);

                    Assert.assertEquals(mr2.group(1), "A");
                    Assert.assertEquals(mr2.start(1), 65);
                    Assert.assertEquals(mr2.end(1),   66);

                    Assert.assertEquals(mr2.group(2), "B");
                    Assert.assertEquals(mr2.start(2), 66);
                    Assert.assertEquals(mr2.end(2),   67);
                    return "*";

                default:
                    throw new AssertionError();
                }
            }
        };

        Substitutor<NoException> s = Substitutor.create(pattern, matchReplacer);

        StringBuilder out = new StringBuilder();
        out.append(s.transform("     AB     "));
        out.append(s.transform("     AB     "));
        out.append(s.transform("     AB     "));
        out.append(s.transform("     AB     "));
        out.append(s.transform("     AB     "));
        out.append(s.transform("     AB     "));

        Assert.assertEquals(6, replacerCallCount[0]);
        Assert.assertEquals("     AB          *          AB          *          AB          *     ", out.toString());
        Assert.assertEquals(3, s.substitutionCount());
    }

    @Test public void
    testSubstitutor3() {

        Pattern                              pattern       = Pattern.compile("A{3,5}");
        Function<MatchResult2, CharSequence> matchReplacer = Functions.<MatchResult2, CharSequence>constant("*");

        Substitutor<NoException> s  = Substitutor.create(pattern, matchReplacer);
        StringBuilder             out = new StringBuilder();

        out.append(s.transform(" AA "));
        Assert.assertEquals(" AA ", out.toString());
        out.append(s.transform(" AAA "));
        Assert.assertEquals(" AA  * ", out.toString());
        out.append(s.transform(" AAAA "));
        Assert.assertEquals(" AA  *  * ", out.toString());
        out.append(s.transform(" AAAAA "));
        Assert.assertEquals(" AA  *  *  * ", out.toString());
        out.append(s.transform(" AAAAAA "));
        Assert.assertEquals(" AA  *  *  *  *A ", out.toString());
        out.append(s.transform(" AA"));   // <= Now one match is pending.
        Assert.assertEquals(" AA  *  *  *  *A  ", out.toString());
        out.append(s.transform(""));      // Flush.
        Assert.assertEquals(" AA  *  *  *  *A  AA", out.toString());
    }
}
