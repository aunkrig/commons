
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

package de.unkrig.commons.junit4;

import java.util.List;

import org.junit.Assert;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to JUNIT and {@code java.lang.String}s.
 */
public final
class AssertString {

    private AssertString() {}

    /**
     * Fails unless the <var>actual</var> string contains the given <var>expectedInfix</var>.
     */
    public static void
    assertContains(String expectedInfix, String actual) {
        assertContains(null, expectedInfix, actual);
    }

    /**
     * Fails with the given <var>message</var> unless the <var>actual</var> string matches the given
     * <var>expectedRegex</var>.
     */
    public static void
    assertContains(@Nullable String message, String expectedInfix, String actual) {
        
        if (!actual.contains(expectedInfix)) {

            Assert.fail(
                (message == null ? "[" : message + ": [")
                + actual
                + "] is not contained in ["
                + expectedInfix
                + "]"
            );
        }
    }

    /**
     * Fails unless each of the <var>actuals</var> contains the respective infix.
     */
    public static void
    assertContains(List<String> expectedInfixes, List<String> actuals) {
        assertContains(null, expectedInfixes, actuals);
    }

    /**
     * Fails with the given <var>message</var> unless each of the <var>actuals</var> contains the respective infix.
     */
    public static void
    assertContains(@Nullable String message, List<String> expectedInfixes, List<String> actuals) {
        for (int i = 0; i < expectedInfixes.size() && i < actuals.size(); i++) {
            String expectedInfix = expectedInfixes.get(i);
            String actual        = actuals.get(i);

            assertContains((message == null ? "Element " : message + ": Element ") + i, expectedInfix, actual);
        }
        if (expectedInfixes.size() != actuals.size()) {
            Assert.fail(
                (message == null ? "Expected " : message + ": Expected ")
                + expectedInfixes.size()
                + " strings, but got "
                + actuals.size()
            );
        }
    }
}
