
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2018, Arno Unkrig
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
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.text.pattern.Searcher;

public
class SearcherTest {

    @Test public void
    testSearcher1() {
        SearcherTest.assertSearcher(
            Pattern.compile("A..B"), // pattern
            "  AyyB  AyyC  Ay",      // subject
            "2-6"                    // expected
        );
    }

    @Test public void
    testSearcherlookBehind() {
        SearcherTest.assertSearcher(
            Pattern.compile("(?<=XXX)A"), // pattern
            "  A XA XXA XXXA XXXXA ",     // subject
            "14-15",                      // expected
            "20-21"
        );
    }

    @Test public void
    testSearcherlookBehindLimit() {
        SearcherTest.assertSearcher(
            Pattern.compile("(?<=X.{0,20})A"),                          // pattern
            "  X.A X........A X.........A X..........A X...........A ", // subject
            "4-5",                                                      // expected
            "15-16",
            "27-28",
            "40-41"
            //"54-55"
        );
    }

    private static void
    assertSearcher(Pattern pattern, String subject, String... expected) {
        final List<String> result = new ArrayList<String>();

        Searcher<NoException> s = new Searcher<NoException>(
            pattern,
            new ConsumerWhichThrows<MatchResult, NoException>() {
                @Override public void
                consume(MatchResult m) throws NoException {
                    result.add(m.start() + "-" + m.end());
                }
            }
        );

        // Feed the "subject" into the searcher char-ba-char.
        for (Character c : StringUtil.asIterable(subject)) {
            s.consume(new String(new char[] { c }));
        }

        Assert.assertArrayEquals(expected, result.toArray());
    }
}
