
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @Deprecated public static Function<Matcher, String>
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
    public static Function<Matcher, String>
    constantMatchReplacer(@Nullable final String string) {

        return new Function<Matcher, String>() {
            @Override @Nullable public String call(@Nullable Matcher match) { return string; }
        };
    }

    /** @deprecated Use {@link #replacementStringMatchReplacer(String)} instead */
    @Deprecated public static <EX extends Throwable> FunctionWhichThrows<Matcher, String, ? extends EX>
    replacementStringReplacer(final String replacementString) {
        return PatternUtil.replacementStringMatchReplacer(replacementString);
    }

    /**
     * @param replacementString See {@link Matcher#appendReplacement(StringBuffer, String)}
     * @return                  A match replacer which forms the replacement from the match and the given
     *                          <var>replacementString</var>
     * @see                     #constantMatchReplacer(String)
     * @see                     #SYSTEM_PROPERTY_MATCH_REPLACER
     */
    public static <EX extends Throwable> FunctionWhichThrows<Matcher, String, ? extends EX>
    replacementStringMatchReplacer(final String replacementString) {

        return new FunctionWhichThrows<Matcher, String, EX>() {

            @Override @Nullable public String
            call(@Nullable Matcher matcher) {
                assert matcher != null;

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
                            if (matcher.groupCount() < newRefNum) break;
                            referenceNumber = newRefNum;
                            idx++;
                        }

                        // Append group.
                        String group = matcher.group(referenceNumber);
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
    @Deprecated public static Function<Matcher, String>
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
    public static final Function<Matcher, String>
    SYSTEM_PROPERTY_MATCH_REPLACER = new Function<Matcher, String>() {

        @Override @Nullable public String
        call(@Nullable Matcher matcher) {
            assert matcher != null;
            return System.getProperty(matcher.group(1));
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
        Matcher                                                                    matcher,
        FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends EX> matchReplacer
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
        Reader                                                                     in,
        Pattern                                                                    pattern,
        FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends EX> matchReplacer,
        Appendable                                                                 out,
        int                                                                        bufferCapacity
    ) throws IOException, EX {

        Substitutor<EX> substitutor = new Substitutor<EX>(pattern, matchReplacer);

        IoUtil.copyAndTransform(in, substitutor, out, bufferCapacity);

        return substitutor.substitutionCount();
    }

    /**
     * Creates and returns a filter {@link Reader} which replaces matches of the <var>pattern</var> within the
     * character stream on-the-fly through the <var>matchReplacer</var>.
     */
    public static Reader
    replaceAllFilterReader(
        Reader                                                                                    delegate,
        final Pattern                                                                             pattern,
        final FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException> matchReplacer
    ) { return TransformingFilterReader.create(delegate, new Substitutor<IOException>(pattern, matchReplacer)); }

    /**
     * Creates and returns a filter {@link Writer} which replaces matches of the <var>pattern</var> within the
     * character stream on-the-fly through the <var>matchReplacer</var>.
     */
    public static Writer
    replaceAllFilterWriter(
        final Pattern                                                                             pattern,
        final FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException> matchReplacer,
        final Appendable                                                                          delegate
    ) { return TransformingFilterWriter.create(new Substitutor<IOException>(pattern, matchReplacer), delegate); }

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
        return new Substitutor<EX>(pattern, PatternUtil.<EX>replacementStringMatchReplacer(replacementString));
    }
}
