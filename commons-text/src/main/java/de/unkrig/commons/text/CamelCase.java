
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

package de.unkrig.commons.text;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * A collection of utility methods related to <a href="http://en.wikipedia.org/wiki/CamelCase">camel case</a>.
 *
 * @deprecated Use {@link Notations} instead.
 */
@Deprecated public final
class CamelCase {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private CamelCase() {}

    /**
     * Concatenates the given {@code strings}, with the first letter of each string <i>except the first string</i>
     * capitalized.
     * <p>
     * Examples:
     * <table>
     *   <tr><td>{@code "list", "length"}     </td><td>=></td><td>{@code "listLength"}     </td></tr>
     *   <tr><td>{@code "My", "class"}        </td><td>=></td><td>{@code "MyClass"}        </td></tr>
     *   <tr><td>{@code "oneTwo", "threeFour"}</td><td>=></td><td>{@code "oneTwoThreefour"}</td></tr>
     * </table>
     *
     * @throws ArrayIndexOutOfBoundsException {@code strings.length} was zero
     * @throws IndexOutOfBoundsException      One of the {@code strings} has zero length
     * @throws NullPointerException           One of the {@code strings} was {@code null}
     */
    public static String
    cat(String... strings) {

        if (strings.length == 1) return strings[0];

        StringBuilder sb = new StringBuilder(strings[0]);

        int i = 1;
        do {
            String s = strings[i];
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        } while (++i < strings.length);

        return sb.toString();
    }

    /**
     * @return The string with the first character converted to lower case
     */
    public static String
    toLowerCamelCase(String s) {

        assert s.length() >= 1;

        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * @return The string with the first character converted to upper case
     */
    public static String
    toUpperCamelCase(String s) {

        assert s.length() >= 1;

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Breaks the string up into words at upper-case letters, and the concatenates the words with hyphens.
     */
    public static String
    toHyphenSeparated(String s) {

        StringBuilder sb = new StringBuilder().append(Character.toLowerCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Converts the camel-case string <var>s</var> into a capitalized string with words separated with underscores.
     */
    public static String
    toUpperCaseUnderscoreSeparated(String s) {

        StringBuilder sb = new StringBuilder().append(Character.toUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }

        return sb.toString();
    }
}
