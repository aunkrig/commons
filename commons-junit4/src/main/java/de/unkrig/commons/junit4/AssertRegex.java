
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

package de.unkrig.commons.junit4;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to JUNIT and {@code java.util.regex}.
 */
public final
class AssertRegex {

    private AssertRegex() {}

    /**
     * Fails unless the <var>actual</var> string matches the given <var>expectedRegex</var>.
     */
    public static void
    assertMatches(String expectedRegex, String actual) {
        assertMatches(null, expectedRegex, actual);
    }

    /**
     * Fails with the given <var>message</var> unless the <var>actual</var> string matches the given
     * <var>expectedRegex</var>.
     */
    public static void
    assertMatches(@Nullable String message, String expectedRegex, String actual) {
        Pattern pattern = Pattern.compile(expectedRegex);
        Matcher matcher = pattern.matcher(actual);
        if (!matcher.matches()) {
            int mismatchOffset = mismatchOffset(pattern, actual);

            String s = actual.substring(mismatchOffset);
            if (s.length() > 10) s = s.substring(0, 8) + "...";

            Assert.fail(
                (message == null ? "[" : message + ": [")
                + actual
                + "] did not match regex ["
                + expectedRegex
                + "] at offset "
                + mismatchOffset
                + ": \""
                + s
                + "\""
            );
        }
    }

    /**
     * @return The offset of the first character in the <var>subject</var> that causes a mismatch
     */
    private static int
    mismatchOffset(Pattern pattern, String subject) {
        for (int i = subject.length(); i >= 0; i--) {
            Matcher matcher = pattern.matcher(subject.substring(0, i));
            if (matcher.matches()) return i;
            if (matcher.hitEnd()) return i;
        }
        return 0;
    }

    /**
     * Fails unless each of the <var>actuals</var> matches the respective regex.
     */
    public static void
    assertMatches(List<String> expectedRegexes, List<String> actuals) {
        assertMatches(null, expectedRegexes, actuals);
    }

    /**
     * Fails with the given <var>message</var> unless each of the <var>actuals</var> matches the respective regex.
     */
    public static void
    assertMatches(@Nullable String message, List<String> expectedRegexes, List<String> actuals) {
        for (int i = 0; i < expectedRegexes.size() && i < actuals.size(); i++) {
            String expectedRegex = expectedRegexes.get(i);
            String actual        = actuals.get(i);

            assertMatches((message == null ? "Element " : message + ": Element ") + i, expectedRegex, actual);
        }
        if (expectedRegexes.size() != actuals.size()) {
            Assert.fail(
                (message == null ? "Expected " : message + ": Expected ")
                + expectedRegexes.size()
                + " strings, but got "
                + actuals.size()
            );
        }
    }
}
