
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
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * {@link Pattern}-related utility methods.
 */
public final
class PatternUtil {

    private static final Logger LOGGER = Logger.getLogger(PatternUtil.class.getName());
    private static final int LOOKBEHIND_LIMIT = 10;

    private PatternUtil() {}

    /**
     * Reads text from <var>in</var>, replaces all matches of <var>pattern</var> according to the
     * <var>replacementString</var>, and writes the result to <var>out</var>.
     * <p>
     *   The pattern search is stream-oriented, not line-oriented, i.e. matches are found even across line boundaries.
     *   Thus the <var>pattern</var> should have been compiled with the {@link Pattern#MULTILINE} flag.
     * </p>
     *
     * @see Matcher#appendReplacement(StringBuffer, String) For the format of the <var>replacementString</var>
     */
    public static void
    replaceAll(Reader in, Pattern pattern, String replacementString, Writer out) throws IOException {

        PatternUtil.replaceAll(
            in,
            pattern,
            PatternUtil.replacementStringReplacer(replacementString),
            out,
            8192
        );
    }

    /**
     * Return a replacer which always returns the given <var>string</var>.
     * <p>
     *   Opposed to {@link #replacementStringReplacer(String)}, "{@code $}" and "{@code \}" have no special meaning.
     * </p>
     *
     * @see #replacementStringReplacer(String)
     * @see #systemPropertyReplacer()
     */
    public static Function<Matcher, String>
    constantReplacer(@Nullable final String string) {

        return new Function<Matcher, String>() {
            @Override @Nullable public String call(@Nullable Matcher match) { return string; }
        };
    }

    /**
     * @return                                              A replacer which forms the replacement from the match and
     *                                                      the given <var>replacementString</var>
     * @see Matcher#appendReplacement(StringBuffer, String) For the format of the <var>replacementString</var>
     * @see #constantReplacer(String)
     * @see #systemPropertyReplacer()
     */
    public static Function<Matcher, String>
    replacementStringReplacer(final String replacementString) {

        return new Function<Matcher, String>() {

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

    /**
     * Returns a replacer which returns the value of the system property named by group #1 of the match.
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   PatternUtil.replaceAll(
     *       Pattern.compile("\\$\\{([^}]+)}").matcher("file.separator is ${file.separator}"),
     *       PatternUtil.systemPropertyReplacer()
     *   )
     * </pre>
     *
     * @see #constantReplacer(String)
     * @see #replacementStringReplacer(String)
     */
    public static Function<Matcher, String>
    systemPropertyReplacer() {

        return new Function<Matcher, String>() {

            @Override @Nullable public String
            call(@Nullable Matcher matcher) {
                assert matcher != null;
                return System.getProperty(matcher.group(1));
            }
        };
    }

    /**
     * Reads text from <var>in</var>, replaces all matches of
     * "<code>${</code><var>system-property-name</var><code>}</code>" with the value of the system property, and writes
     * the result to <var>out</var>.
     *
     * @return The number of replacements that were executed
     */
    public static int
    replaceSystemProperties(Reader in, Writer out) throws IOException {

        return PatternUtil.replaceAll(
            in,                                                   // in
            Pattern.compile("\\$\\{([^}]+)}", Pattern.MULTILINE), // pattern
            PatternUtil.systemPropertyReplacer(),                 // replacer
            out,                                                  // out
            4096                                                  // initialBufferCapacity
        );
    }

    /**
     * The generalized form of {@link Matcher#replaceAll(String)}: The replacement for a match is not formed from a
     * "replacement string" (with variables "$0", "$1", ...), but is computed by the <var>replacer</var>. If the
     * replacer returns {@code null} for a match, then the match is <em>not</em> replaced.
     */
    public static String
    replaceAll(Matcher matcher, Function<Matcher, String> replacer) {

        matcher.reset();

        StringBuffer sb = new StringBuffer();
        for (boolean result = matcher.find(); result; result = matcher.find()) {

            String replacement = replacer.call(matcher);

            if (replacement == null) continue;

            // It may seem odd to use "quoteReplacement()" here, but since we have no access to the matcher's text,
            // it is the only way to achieve what we want. Fortunately "quoteReplacement()" is very fast when the
            // replacement string contains no dollar signs nor backslashes.
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Reads characters from <var>in</var>, finds all matches of <var>pattern</var>, replaces each match with the
     * result of the <var>replacer</var>, and writes the result to <var>out</var>. If the replacer returns {@code null}
     * for a match, then the match is <em>not</em> replaced.
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
     * @param initialBufferCapacity The initial capacity of the temporary {@link CharBuffer} that is used for pattern
     *                              matching; the buffer will automatically be resized as necessary; 4096 may be a good
     *                              value
     * @return                      The number of replacements that were executed
     */
    public static int
    replaceAll(Reader in, Pattern pattern, Function<Matcher, String> replacer, Writer out, int initialBufferCapacity)
    throws IOException {

        PatternUtil.LOGGER.log(Level.FINE, "Replace all matches of ''{0}''", pattern);

        int        replacementCount = 0;
        CharBuffer cb               = CharBuffer.allocate(initialBufferCapacity);

        for (int from = 0;;) {

            // The CB is now in "fill mode".
            if (!cb.hasRemaining()) {

                // We hit the end; enlarge the buffer.
                PatternUtil.LOGGER.finest("Increasing buffer size");
                cb.flip();
                cb = CharBuffer.allocate(cb.capacity() * 2).append(cb);
            }

            boolean atEoi = false;
            do {
                if (in.read(cb) == -1) {
                    atEoi = true;
                    break;
                }
            } while (cb.hasRemaining());

            cb.flip();
            // The CB is now in "drain mode".

            // Find the next match.
            Matcher m = pattern.matcher(cb);
            if (m.find(from)) {
                String replacement = replacer.call(m);

                if (replacement == null) {

                    PatternUtil.LOGGER.log(Level.CONFIG, "Leaving match ''{0}'' unreplaced", m.group());

                    // Copy the text BEFORE the match AND the match to the output stream.
                    out.append(cb, from, m.end());
                } else {

                    PatternUtil.LOGGER.log(
                        Level.CONFIG,
                        "Replacing match ''{0}'' with ''{1}''",
                        new Object[] { m.group(), replacement }
                    );

                    // Copy the text BEFORE the match to the output stream.
                    out.append(cb, from, m.start());

                    // Copy the replacement to the output stream.
                    out.append(replacement);

                    replacementCount++;
                }
                from = m.end();

                // Very special case: We have a zero-length match. To avoid an endless sequence of matches, advance
                // "from" by one.
                if (m.start() == m.end()) {
                    if (m.end() == cb.limit()) break;
                    out.write(cb.charAt(from++));
                }
            } else
            if (!atEoi && m.hitEnd()) {

                // We hit the end; enlarge the buffer.
                PatternUtil.LOGGER.finest("Increasing buffer size");
                cb.flip();
                cb = CharBuffer.allocate(cb.capacity() * 2).append(cb);
                continue; // We're now in "fill mode".
            } else
            {

                // No match.
                cb.position(from);
                out.append(cb);
                from = cb.position();

                if (atEoi) break;
            }

            // Now switch the CB back to "fill mode".

            if (from <= PatternUtil.LOOKBEHIND_LIMIT) {
                cb.position(cb.limit());
                cb.limit(cb.capacity());
            } else
            if (cb.capacity() > initialBufferCapacity && cb.position() <= initialBufferCapacity) {
                PatternUtil.LOGGER.finest("Restoring initial buffer size");
                cb.flip();
                cb = CharBuffer.allocate(initialBufferCapacity).append(cb);
            } else
            {
                cb.position(from - PatternUtil.LOOKBEHIND_LIMIT);
                cb.compact();
                from = PatternUtil.LOOKBEHIND_LIMIT;
            }
        }

        PatternUtil.LOGGER.log(
            Level.FINE,
            "Replaced {0,choice,0#no matches|1#one match|1<{0} matches} of ''{1}''",
            new Object[] { replacementCount, pattern }
        );

        return replacementCount;
    }
}
