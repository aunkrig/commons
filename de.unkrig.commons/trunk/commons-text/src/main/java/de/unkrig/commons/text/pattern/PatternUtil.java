
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

package de.unkrig.commons.text.pattern;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.TransformingFilterReader;
import de.unkrig.commons.io.TransformingFilterWriter;
import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * {@link Pattern}-related utility methods.
 */
public final
class PatternUtil {

    private PatternUtil() {}

    /** @deprecated Use {@link #constantMatchReplacer(String)} instead */
    @Deprecated public static Function<MatchResult, String>
    constantReplacer(@Nullable final String string) { return PatternUtil.constantMatchReplacer(string); }

    /**
     * Returns a match replacer which always returns the given <var>string</var>.
     * <p>
     *   Opposed to {@link #replacementStringMatchReplacer(String)}, "{@code $}" and "{@code \}" have no special
     *   meaning.
     * </p>
     *
     * @see #replacementStringMatchReplacer(String)
     * @see #SYSTEM_PROPERTY_MATCH_REPLACER
     */
    public static Function<MatchResult, String>
    constantMatchReplacer(@Nullable final String string) {

        return new Function<MatchResult, String>() {
            @Override @Nullable public String call(@Nullable MatchResult matchResult) { return string; }
        };
    }

    /** @deprecated Use {@link #replacementStringMatchReplacer(String)} instead */
    @Deprecated public static <EX extends Throwable> FunctionWhichThrows<MatchResult, String, ? extends EX>
    replacementStringReplacer(final String replacementString) {
        return PatternUtil.<EX>replacementStringMatchReplacer(replacementString);
    }

    /**
     * @param replacementString See {@link Matcher#appendReplacement(StringBuffer, String)}
     * @return                  A match replacer which forms the replacement from the match and the given
     *                          ({@link Matcher#replaceAll(String)}-compatible) <var>replacementString</var>
     * @see                     #constantMatchReplacer(String)
     * @see                     #SYSTEM_PROPERTY_MATCH_REPLACER
     * @see                     Matcher#replaceAll(String)
     */
    public static <EX extends Throwable> FunctionWhichThrows<MatchResult, String, ? extends EX>
    replacementStringMatchReplacer(final String replacementString) {

        return new FunctionWhichThrows<MatchResult, String, EX>() {

            @Override @Nullable public String
            call(@Nullable MatchResult matchResult) {
                assert matchResult != null;

                // Process replacement string to replace group references with groups.
                StringBuffer result = new StringBuffer();

                for (int idx = 0; idx < replacementString.length();) {

                    char c = replacementString.charAt(idx);
                    if (c == '\\') {
                        result.append(replacementString.charAt(++idx));
                        idx++;
                    } else
                    if (c == '$') {

                        idx++;

                        // Scan first digit of reference number.
                        int referenceNumber = replacementString.charAt(idx) - '0';
                        if (referenceNumber < 0 || referenceNumber > 9) {
                            throw new IllegalArgumentException("Illegal group reference");
                        }
                        idx++;

                        // Scan following digits of reference number.
                        while (idx < replacementString.length()) {
                            int nextDigit = replacementString.charAt(idx) - '0';
                            if (nextDigit < 0 || nextDigit > 9) break;

                            int newRefNum = (referenceNumber * 10) + nextDigit;
                            if (matchResult.groupCount() < newRefNum) break;
                            referenceNumber = newRefNum;
                            idx++;
                        }

                        // Append group.
                        String group = matchResult.group(referenceNumber);
                        if (group != null) result.append(group);
                    } else {
                        result.append(c);
                        idx++;
                    }
                }

                return result.toString();
            }
        };
    }

    /** @deprecated Use {@link #SYSTEM_PROPERTY_MATCH_REPLACER} instead */
    @Deprecated public static Function<MatchResult, String>
    systemPropertyReplacer() { return PatternUtil.SYSTEM_PROPERTY_MATCH_REPLACER; }

    /**
     * A match replacer which returns the value of the system property named by group #1 of the match.
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   PatternUtil.replaceAll(
     *       Pattern.compile("\\$\\{([^}]+)}").matcher("file.separator is ${file.separator}"),
     *       PatternUtil.SYSTEM_PROPERTY_MATCH_REPLACER
     *   )
     * </pre>
     *
     * @see #constantMatchReplacer(String)
     * @see #replacementStringMatchReplacer(String)
     */
    public static final Function<MatchResult, String>
    SYSTEM_PROPERTY_MATCH_REPLACER = new Function<MatchResult, String>() {

        @Override @Nullable public String
        call(@Nullable MatchResult matchResult) {
            assert matchResult != null;
            return System.getProperty(matchResult.group(1));
        }
    };

    /**
     * Reads text from <var>in</var>, replaces all matches of <var>pattern</var> according to the
     * <var>replacementString</var>, and writes the result to <var>out</var>.
     * <p>
     *   The pattern search is stream-oriented, not line-oriented, i.e. matches are found even across line boundaries.
     *   Thus the <var>pattern</var> should have been compiled with the {@link Pattern#MULTILINE} flag.
     * </p>
     *
     * @return The number of replacements that were executed
     * @see    Matcher#appendReplacement(StringBuffer, String) For the format of the <var>replacementString</var>
     */
    public static long
    replaceAll(Reader in, Pattern pattern, String replacementString, Appendable out) throws IOException {

        return PatternUtil.replaceSome(
            in,
            pattern,
            PatternUtil.<IOException>replacementStringMatchReplacer(replacementString),
            out,
            8192
        );
    }

    /**
     * Reads text from <var>in</var>, replaces all matches of
     * "<code>${</code><var>system-property-name</var><code>}</code>" with the value of the system property, and writes
     * the result to <var>out</var>.
     *
     * @return The number of replacements that were executed
     */
    public static long
    replaceSystemProperties(Reader in, Appendable out) throws IOException {

        return PatternUtil.replaceSome(
            in,                                                   // in
            Pattern.compile("\\$\\{([^}]+)}", Pattern.MULTILINE), // pattern
            PatternUtil.SYSTEM_PROPERTY_MATCH_REPLACER,           // matchReplacer
            out,                                                  // out
            4096                                                  // initialBufferCapacity
        );
    }

    /**
     * The generalized form of {@link Matcher#replaceAll(String)}: The replacement for a match is not formed from a
     * "replacement string" (with variables "$0", "$1", ...), but is computed by the <var>matchReplacer</var>. If the
     * <var>matchReplacer</var> returns {@code null} for a match, then the match is <em>not</em> replaced.
     */
    public static <EX extends Throwable> String
    replaceSome(
        Matcher                                                                        matcher,
        FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends EX> matchReplacer
    ) throws EX {

        matcher.reset();

        StringBuffer sb = new StringBuffer();
        for (boolean result = matcher.find(); result; result = matcher.find()) {

            CharSequence replacement = matchReplacer.call(matcher);

            if (replacement == null) continue;

            // It may seem odd to use "quoteReplacement()" here, but since we have no access to the matcher's text,
            // it is the only way to achieve what we want. Fortunately "quoteReplacement()" is very fast when the
            // replacement string contains no dollar signs nor backslashes.
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Reads characters from <var>in</var>, finds all matches of <var>pattern</var>, replaces each match with the
     * result of the <var>matchReplacer</var>, and writes the result to <var>out</var>. If the <var>matchReplacer</var>
     * returns {@code null} for a match, then the match is <em>not</em> replaced.
     * <p>
     *   The pattern search is stream-oriented, not line-oriented, i.e. matches are found even across line boundaries.
     *   Therefore the <var>pattern</var> should have been compiled with the {@link Pattern#MULTILINE} flag.
     * </p>
     * <p>
     *   This method attempts to load as few characters into memory as possible. Notice, however, that in particular
     *   the usage of "greedy quantifiers", for example "{@code a.*b}", can lead to all the remaining content being
     *   read into memory.
     * </p>
     *
     * @param bufferCapacity The number of chars that are read repeatedly, until end-of-input, from <var>in</var>
     * @return               The number of replacements that were executed
     */
    public static <EX extends Throwable> int
    replaceSome(
        Reader                                                                         in,
        Pattern                                                                        pattern,
        FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends EX> matchReplacer,
        Appendable                                                                     out,
        int                                                                            bufferCapacity
    ) throws IOException, EX {

        Substitutor<EX> substitutor = Substitutor.create(pattern, matchReplacer);

        IoUtil.copyAndTransform(in, substitutor, out, bufferCapacity);

        return substitutor.substitutionCount();
    }

    /**
     * Creates and returns a filter {@link Reader} which replaces matches of the <var>pattern</var> within the
     * character stream on-the-fly through the <var>matchReplacer</var>.
     */
    public static Reader
    replaceAllFilterReader(
        Reader                                                                                        delegate,
        final Pattern                                                                                 pattern,
        final FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends IOException> matchReplacer
    ) { return TransformingFilterReader.create(delegate, Substitutor.create(pattern, matchReplacer)); }

    /**
     * Creates and returns a filter {@link Writer} which replaces matches of the <var>pattern</var> within the
     * character stream on-the-fly through the <var>matchReplacer</var>.
     */
    public static Writer
    replaceAllFilterWriter(
        final Pattern                                                                                 pattern,
        final FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends IOException> matchReplacer,
        final Appendable                                                                              delegate
    ) { return TransformingFilterWriter.create(Substitutor.create(pattern, matchReplacer), delegate); }

    /**
     * Creates and returns a {@link Substitutor} which replaces {@link Pattern} matches in a stream of strings through
     * the <var>replacementString</var>.
     * <p>
     *   As the returned transformer <em>consumes</em> a sequence of strings, it <em>produces</em> a sequence of
     *   strings, and the concatenation of the consumed strings equals the concatenation of the produced strings,
     *   except that all matches of the <var>pattern</var> are substituted with the <var>replacementString</var>.
     * </p>
     * <p>
     *   Iff the input to the transformer is {@code ""} (the empty string), then the "rest" of any pending matches is
     *   returned.
     * </p>
     *
     * @param replacementString See {@link Matcher#appendReplacement(StringBuffer, String)}
     */
    public static <EX extends Throwable> Substitutor<EX>
    substitutor(Pattern pattern, String replacementString) {
        return Substitutor.create(pattern, PatternUtil.<EX>replacementStringMatchReplacer(replacementString));
    }

    /**
     * Splits the <var>regex</var> in exactly two substrings; the first is the (possibly empty) "constant prefix"
     * and the (possibly empty) second starts with the first non-constant regex construct. Examples:
     * <table border="1">
     *   <tr><td>{@code "abc*"}</td><td>{@code "ab", "c*"}</td></tr>
     *   <tr><td>{@code "abc\\*"}</td><td>{@code "abc*", ""}</td><td>(Escaped quantifier)</td></tr>
     *   <tr><td>{@code "\x61bc*"}</td><td>{@code "ab", "c*"}</td><td>(2-digit hex literal)</td></tr>
     * </table>
     * Notice that, as in the second and third example, the constant prefix is not necessarily a strict prefix of the
     * regex, because various escape mechanisms apply.
     *
     * @param regex As compilable by {@link Pattern#compile(String)}
     */
    public static String[]
    constantPrefix(String regex) {
        StringBuilder result = new StringBuilder();
        int state = 0;
        int offset = 0;
        int beforeChar1 = -1, beforeChar2 = -1;
        INFIX:
        for (;; offset++) {
            char c = offset < regex.length() ? regex.charAt(offset) : 0;

            // Quantifier? If so, then the preceding char is *not* literal; e.g. "abc*" => only "ab" is literal.
            if (state == 0 && "?*+{".indexOf(c) != -1) {
                if (beforeChar1 == -1) throw new PatternSyntaxException("Regex starts with quantifier", regex, offset);
                offset = beforeChar1;
                result.delete(beforeChar2, Integer.MAX_VALUE);
                break INFIX;
            }

            // Remember position of literal because if it is followed by a quanitifier, then it is not a literal.
            if (state == 0 || state == 2) {
                beforeChar1 = offset;
                beforeChar2 = result.length();
            }

            switch (state) {
            case 0:
                if (c == 0) {
                    break INFIX;
                } else
                if (c == '\\') {
                    state = 1;
                } else
                if ("[.^$|(".indexOf(c) != -1) {
                    break INFIX;
                } else
                {
                    // In addition to "normal" characters, these are also literals: ] } )
                    result.append(c);
                }
                break;
            case 1: // After backslash.
                if (c == 0) {
                    throw new PatternSyntaxException("Trailing backslash", regex, offset);
                } else
                if ("\\[.^$?*+{|(".indexOf(c) != -1) { // Quoted metacharacter.
                    result.append(c);
                    state = 0;
                } else
                if (c == '0') { // Octal literal ("\0n" or "\0nn" or "\0mnn")
                    state = 4;
                } else
                if (c == 'x') { // 2-digit hex literal ("\xhh") or multi-digit hex literal ("\x{h...h}").
                    state = 7;
                } else
                if (c == 'u') { // 4-digit hex literal ("/uhhhh")
                    state = 9;
                } else
                if (c == 'N') { // Unicode character name literal ("\N{name}")
                    break INFIX; // Too complicated, give up.
                } else
                if ("tnrfae".indexOf(c) != -1) { // Control character (e.g. "\n")
                       result.append("\t\n\r\f\u0007\u001b".charAt("tnrfae".indexOf(c)));
                } else
                if (c == 'c') { // Control character ("\cx")
                    state = 15;
                } else
                if (c == 'Q') { // Beginning of quoted section ("\Q...\E")
                    state = 2;
                } else
                {
                    // Predefined character classes (e.g. "\d"), or boundary matchers (e.g. "\b"), or similar.
                    break INFIX;
                }
                break;
            case 2: // In quoted section.
                if (c == 0) {
                    break INFIX;
                } else
                if (c == '\\') {
                    state = 3;
                } else {
                    result.append(c);
                }
                break;
            case 3: // In quoted section, after backslash.
                if (c == 0) {
                    result.append('\\');
                    break INFIX;
                } else
                if (c == 'E') {
                    state = 0;
                } else {
                    result.append('\\');
                    result.append(c);
                    state = 2;
                }
                break;
            case 4: // Octal literal (after "\0")
                if (Character.digit(c, 8) == -1) throw new PatternSyntaxException("Octal literal does not start with an ocatal digit", regex, offset);
                state = 5;
                break;
            case 5: // Octal literal (after "\0n")
                if (Character.digit(c, 8) != -1) {
                    state = 6;
                } else {
                    result.append((char) Integer.parseInt(regex.substring(offset - 1, offset), 8)); // \0n
                    state = 0;
                    offset--;
                }
                break;
            case 6: // Octal literal (after "\0nn")
                if (Character.digit(c, 8) != -1 && "0123".indexOf(regex.charAt(offset - 2)) != -1) { // \0mnn
                    result.append((char) Integer.parseInt(regex.substring(offset - 2, offset + 1), 8));
                    state = 0;
                } else {
                    result.append((char) Integer.parseInt(regex.substring(offset - 2, offset), 8)); // \0nn
                    state = 0;
                    offset--;
                }
                break;
            case 7: // Hex literal (after "\x")
                if (c == '{') {
                    state = 13;
                } else
                if (Character.digit(c, 16) != -1) {
                    state = 8;
                } else
                {
                    throw new PatternSyntaxException("Hex literal does not start with \"{\" or hex digit", regex, offset);
                }
                break;
            case 8: // 2-digit hex literal (after "\xh")
                if (Character.digit(c, 16) == -1) throw new PatternSyntaxException("2-digit hex literal lacks second hex digit", regex, offset);
                result.append((char) Integer.parseInt(regex.substring(offset - 1, offset + 1), 16)); // \xhh
                state = 0;
                break;
            case 9: // 4-digit hex literal (after "\ u")
            case 10: // 4-digit hex literal (after "\ uh")
            case 11: // 4-digit hex literal (after "\ uhh")
                if (Character.digit(c, 16) == -1) throw new PatternSyntaxException("4-digit hex literal lacks hex digit", regex, offset);
                state++;
                break;
            case 12: // 4-digit hex literal (after "\ uhhh")
                if (Character.digit(c, 16) == -1) throw new PatternSyntaxException("4-digit hex literal lacks fourth hex digit", regex, offset);
                result.append((char) Integer.parseInt(regex.substring(offset - 3, offset + 1), 16)); // \ uhhhh
                state = 0;
                break;
            case 13: // multi-digit hex literal (after "\x{")
                if (Character.digit(c, 16) == -1) throw new PatternSyntaxException("Multi-digit hex literal lacks first hex digit", regex, offset);
                state = 14;
                break;
            case 14: // multi-digit hex literal (after "\x{h")
                if (Character.digit(c, 16) != -1) {
                    ;
                } else
                if (c == '}') {         // \x{h...h}
                    int cp = Integer.parseInt(
                        regex.substring(regex.lastIndexOf('{', offset) + 1, offset), // h...h
                        16
                    );
                    result.append((char) (0xd800 + ((cp >> 10) & 0x3ff)));
                    result.append((char) (0xec00 + (cp         & 0x3ff)));
                    state = 0;
                } else
                {
                    throw new PatternSyntaxException("Unexpected character in multi-digit hex literal", regex, offset);
                }
                break;
            case 15: // Control character (after "\c")
                if (c == 0) throw new PatternSyntaxException("Control character missing control character literal", regex, offset);
                result.append((char) (c & 0x1f)); // \cx
                state = 0;
                break;
            default:
                throw new AssertionError(state);
            }
        }

        return new String[] { result.toString(), regex.substring(offset) };
    }
}
