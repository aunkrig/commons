
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2022, Arno Unkrig
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
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.text.pattern.Finders;

public
class FindersTest {

    @Test public void
    test1() {
        FindersTest.testPatternFinder("   [$0=A]   ", Pattern.compile("A"),  "   A   ");
    }

    @Test public void
    testZeroWidthMatches() {
        FindersTest.testPatternFinder("[$0=] [$0=A][$0=] [$0=]", Pattern.compile("A*"), " A ", "");
        FindersTest.testPatternFinder("[$0=] [$0=A][$0=]",       Pattern.compile("A*"), " A", "");
    }

    private static void
    testPatternFinder(String expected, Object... patternsAndInputs) {

        List<Pattern> patterns = new ArrayList<Pattern>();
        List<String>  inputs   = new ArrayList<String>();
        {
            int i = 0;
            for (; i < patternsAndInputs.length; i++) {
                Object o = patternsAndInputs[i];
                if (!(o instanceof Pattern)) break;
                patterns.add((Pattern) o);
            }
            for (; i < patternsAndInputs.length; i++) {
                Object o = patternsAndInputs[i];
                if (!(o instanceof String)) break;
                inputs.add((String) o);
            }
            if (i != patternsAndInputs.length) throw new IllegalArgumentException(i + ": " + patternsAndInputs[i]);
        }

        FindersTest.testPatternFinder(
            expected,                                       // expected
            patterns.toArray(new Pattern[patterns.size()]), // patterns
            inputs.toArray(new String[inputs.size()])       // inputs
        );
    }

    private static void
    testPatternFinder(String expected, Pattern[] patterns, String[] in) {

        // Try "Finders.patternFinder()":
        {
            final StringBuilder result = new StringBuilder();

            ConsumerWhichThrows<CharSequence, NoException> finder = Finders.patternFinder(
                patterns,                     // patterns
                new Consumer<MatchResult>() { // match

                    @Override public void
                    consume(MatchResult m) {
                        result.append("[$0=").append(m.group());
                        for (int i = 1; i <= m.groupCount(); i++) result.append(" $" + i + "=" + m.group(i));
                        result.append("]");
                    }
                },
                new Consumer<Character>() {   // nonMatch
                    @Override public void consume(Character c) { result.append(c); }
                }
            );

            for (String s : in) finder.consume(s);
            Assert.assertEquals("Finders.patternFinder()", expected, result.toString());
        }

        // Try "Matcher.replaceAll()":
        {
            StringBuilder sb = new StringBuilder();
            for (String s : in) sb.append(s);
            Assert.assertEquals("Matcher.replaceAll()", expected, patterns[0].matcher(sb).replaceAll("[\\$0=$0]"));
        }
    }
}

