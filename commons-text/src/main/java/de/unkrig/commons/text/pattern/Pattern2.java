
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

package de.unkrig.commons.text.pattern;

import java.io.File;
import java.util.regex.Pattern;

/**
 * This class extends the concepts of the JDK {@link Pattern java.util.regex.Pattern} class with a new {@link
 * #WILDCARD} compilation flag, which modifies the pattern compilation such that it combines the well-known
 * <a href="http://en.wikipedia.org/wiki/Wildmat">wildcard pattern matching</a> with the power of regular
 * expressions.
 *
 * @see #compile(String, int)
 */
public final
class Pattern2 {

    private Pattern2() {}

    /**
     * Modifies the pattern compilation as follows:
     * <p>
     * The meaning of the '*' and '?' metacharacters is now different, and '.' is no longer a metacharacter.
     * <p>
     * '/' is now a metacharacter, i.e. to include it literally in the pattern, it must be escaped with a backslash.
     * <p>
     * The semantics of '*', '?' and '.' are as follows:
     * <table border="1">
     *   <tr align="left">
     *     <th bgcolor="#CCCCFF" align="left" id="construct">Construct</th>
     *     <th bgcolor="#CCCCFF" align="left" id="matches">Matches</th>
     *   </tr>
     *   <tr align="left">
     *     <th colspan="2" id="alternatives"><font color="red">Wildcards</font></th>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>*</tt></td>
     *     <td headers="matches">Zero or more characters except '/', the file separator and '!'</td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>**</tt></td>
     *     <td headers="matches">Zero or more characters except '!'</td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>***</tt></td>
     *     <td headers="matches">Zero or more characters</td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>?</tt></td>
     *     <td headers="matches">Any character except '/', the file separator and '!'</td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>.</tt></td>
     *     <td headers="matches">The '.'</td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><tt>/</tt></td>
     *     <td headers="matches">
     *       '/' or the system-dependent file separator (see {@link java.io.File#separatorChar separatorChar})
     *     </td>
     *   </tr>
     * </table>
     * Naturally, '*' is no longer the regex quantifier '*', so if you need to quantify 'zero or more', then you'd have
     * to write '<code>{0,}</code>'. Similarly, to quantify 'zero or one', you can no longer write '{@code ?}', but
     * must use '<code>{0,1}</code>'.
     */
    public static final int WILDCARD = 0x20000000;

    /**
     * Like {@link Pattern#compile(String,int)}, but with support for the {@link #WILDCARD} flag.
     * <p>
     *   Notice that iff {@link #WILDCARD} is given, then {@link #toString()} returns the regular expression
     *   that was generated from the wildcard pattern (and not the wildcard pattern, as you'd probably expect).
     * </p>
     *
     * @see #WILDCARD
     * @see Pattern#CANON_EQ
     * @see Pattern#CASE_INSENSITIVE
     * @see Pattern#COMMENTS
     * @see Pattern#DOTALL
     * @see Pattern#LITERAL
     * @see Pattern#MULTILINE
     * @see Pattern#UNICODE_CASE
     * @see Pattern#UNIX_LINES
     */
    public static Pattern
    compile(String pattern, int flags) {

        if ((flags & (Pattern.LITERAL | Pattern2.WILDCARD)) != Pattern2.WILDCARD) {
            return Pattern.compile(pattern, flags);
        }

        String metaCharacters = "*?./{";
        for (
            int idx = Pattern2.findMeta(metaCharacters, pattern, 0);
            idx != pattern.length();
            idx = Pattern2.findMeta(metaCharacters, pattern, idx)
        ) {
            switch (pattern.charAt(idx)) {

            case '*':
                {
                    String s = pattern.substring(idx);
                    if (s.startsWith("***")) {
                        pattern = pattern.substring(0, idx) + ".*" + pattern.substring(idx + 3);
                        idx     += 2;
                    } else
                    if (s.startsWith("**")) {
                        pattern = pattern.substring(0, idx) + "[^!]*" + pattern.substring(idx + 2);
                        idx     += 5;
                    } else
                    if (File.separatorChar != '/') {
                        pattern = (
                            pattern.substring(0, idx)
                            + "[^/\\"
                            + File.separatorChar
                            + "!]*"
                            + pattern.substring(idx + 1)
                        );
                        idx     += 8;
                    } else
                    {
                        pattern = pattern.substring(0, idx) + "[^/!]*" + pattern.substring(idx + 1);
                        idx     += 6;
                    }
                }
                break;

            case '?':
                if (File.separatorChar != '/') {
                    pattern = (
                        pattern.substring(0, idx)
                        + "[^/\\"
                        + File.separatorChar
                        + "!]"
                        + pattern.substring(idx + 1)
                    );
                    idx     += 7;
                } else {
                    pattern = pattern.substring(0, idx) + "[^/!]" + pattern.substring(idx + 1);
                    idx     += 5;
                }
                break;

            case '.':
                pattern = pattern.substring(0, idx) + "\\." + pattern.substring(idx + 1);
                idx     += 2;
                break;

            case '/':
                if (File.separatorChar != '/') {
                    pattern = (
                        pattern.substring(0, idx)
                        + "[/\\"
                        + File.separatorChar
                        + "]"
                        + pattern.substring(idx + 1)
                    );
                    idx     += 5;
                } else {
                    idx++;
                }
                break;

            case '{':
                {
                    if (pattern.regionMatches(idx, "{0,1}", 0, 5)) {
                        pattern = pattern.substring(0, idx) + "?" + pattern.substring(idx + 5);
                        idx++;
                    } else
                    if (pattern.regionMatches(idx, "{0,}", 0, 4)) {
                        pattern = pattern.substring(0, idx) + "*" + pattern.substring(idx + 4);
                        idx++;
                    } else
                    {
                        idx++;
                    }
                }
                break;

            default:
                throw new IllegalStateException();
            }
        }

        return Pattern.compile(pattern, flags);
    }

    /**
     * Splits the given string into "pattern" and "replacement". The "pattern" is the text <em>before</em> the first
     * non-escaped equals sign ("="), the "replacement" is the text <em>after</em> that equals sign.
     * <p>
     *   Example:
     * </p>
     * <p>
     *   {@code "(foo)=$1.bak"} results in <code>{ "(foo)", "$1.bak" }</code>.
     * </p>
     * <p>
     *   Iff there is no non-escaped equals sign, then the resulting replacement is {@code null}.
     * </p>
     *
     * @return An array of <code>{ <var>pattern</var>, <var>replacement</var> }</code>
     */
    public static String[]
    parsePatternAndReplacement(String pattern) {

        String replacement = null;

        int idx = Pattern2.findMeta("=", pattern, 0);
        if (idx != pattern.length()) {
            replacement = pattern.substring(idx + 1);
            pattern     = pattern.substring(0, idx);
        }

        return new String[] { pattern, replacement };
    }

    /**
     * Finds the next unescaped occurrence of one of the <var>metaCharacters</var> within <var>subject</var>, starting
     * at position <var>offset</var>. Metacharacters can be escaped by backslashes or by '{@code \Q ... \E}'.
     *
     * @return The position of the next meta character, or {@code subject.length()} iff no meta character is found
     */
    public static int
    findMeta(String metaCharacters, String subject, int offset) {
        int     cc    = 0;     // Character class count (character classes my be nested).
        boolean q     = false; // Inside curly-brace quantifier, e.g. "{0,}"
        int     state = 0;
        for (; offset != subject.length(); offset++) {
            char c = subject.charAt(offset);
            switch (state) {
            case 0:
                if (c == '\\') {
                    state = 1;
                } else
                if (c == '[') {
                    cc++;
                } else
                if (c == ']' && cc > 0) {
                    cc--;
                } else
                if (cc > 0) {
                    ;
                } else
                if (metaCharacters.indexOf(c) != -1 && !q) {
                    return offset;
                } else
                if (c == '{') {
                    q = true;
                } else
                if (c == '}') {
                    q = false;
                }
                break;
            case 1: // After backslash.
                state = c == 'Q' ? 2 : 0;
                break;
            case 2: // In quoted section.
                if (c == '\\') state = 3;
                break;
            case 3: // Inquoted section, after backslash.
                if (c == 'E') {
                    state = 0;
                } else
                if (c != '\\') {
                    state = 2;
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
        return offset;
    }
}
