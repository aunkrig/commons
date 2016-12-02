
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

import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Segment;

import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * {@link Pattern}-related utility methods.
 */
public final
class PatternUtil {

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
     * @return The number of replacements that were executed
     * @see    Matcher#appendReplacement(StringBuffer, String) For the format of the <var>replacementString</var>
     */
    public static int
    replaceAll(Reader in, Pattern pattern, String replacementString, Writer out) throws IOException {

        return PatternUtil.replaceAll(
            in,
            pattern,
            PatternUtil.replacementStringMatchReplacer(replacementString),
            out,
            8192
        );
    }

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
     * @see #systemPropertyMatchReplacer()
     */
    public static Function<Matcher, String>
    constantMatchReplacer(@Nullable final String string) {

        return new Function<Matcher, String>() {
            @Override @Nullable public String call(@Nullable Matcher match) { return string; }
        };
    }

    /** @deprecated Use {@link #replacementStringMatchReplacer(String)} instead */
    @Deprecated public static Function<Matcher, String>
    replacementStringReplacer(final String replacementString) {
        return PatternUtil.replacementStringMatchReplacer(replacementString);
    }

    /**
     * @return A match replacer which forms the replacement from the match and the given <var>replacementString</var>
     * @see    Matcher#appendReplacement(StringBuffer, String) For the format of the <var>replacementString</var>
     * @see    #constantMatchReplacer(String)
     * @see    #systemPropertyMatchReplacer()
     */
    public static Function<Matcher, String>
    replacementStringMatchReplacer(final String replacementString) {

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

    /** @deprecated Use {@link #systemPropertyMatchReplacer()} instead */
    @Deprecated public static Function<Matcher, String>
    systemPropertyReplacer() { return PatternUtil.systemPropertyMatchReplacer(); }

    /**
     * Returns a match replacer which returns the value of the system property named by group #1 of the match.
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   PatternUtil.replaceAll(
     *       Pattern.compile("\\$\\{([^}]+)}").matcher("file.separator is ${file.separator}"),
     *       PatternUtil.systemPropertyMatchReplacer()
     *   )
     * </pre>
     *
     * @see #constantMatchReplacer(String)
     * @see #replacementStringMatchReplacer(String)
     */
    public static Function<Matcher, String>
    systemPropertyMatchReplacer() {

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
            PatternUtil.systemPropertyMatchReplacer(),            // matchReplacer
            out,                                                  // out
            4096                                                  // initialBufferCapacity
        );
    }

    /**
     * The generalized form of {@link Matcher#replaceAll(String)}: The replacement for a match is not formed from a
     * "replacement string" (with variables "$0", "$1", ...), but is computed by the <var>matchReplacer</var>. If the
     * <var>matchReplacer</var> returns {@code null} for a match, then the match is <em>not</em> replaced.
     */
    public static String
    replaceAll(Matcher matcher, Function<Matcher, String> matchReplacer) {

        matcher.reset();

        StringBuffer sb = new StringBuffer();
        for (boolean result = matcher.find(); result; result = matcher.find()) {

            String replacement = matchReplacer.call(matcher);

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
     * @param initialBufferCapacity The initial capacity of the temporary {@link CharBuffer} that is used for pattern
     *                              matching; the buffer will automatically be resized as necessary; 4096 may be a good
     *                              value
     * @return                      The number of replacements that were executed
     */
    public static int
    replaceAll(
        Reader                    in,
        Pattern                   pattern,
        Function<Matcher, String> matchReplacer,
        Writer                    out,
        int                       initialBufferCapacity
    ) throws IOException {

        AllReplacer allReplacer = PatternUtil.replaceAll(pattern, matchReplacer);

        char[] buffer = new char[initialBufferCapacity];
        for (;;) {
            int n = in.read(buffer);
            if (n == -1) break;
            out.write(allReplacer.transform(new String(buffer, 0, n)));
        }

        out.write(allReplacer.flush());

        return allReplacer.substitutionCount();
    }

    /**
     * Creates and returns a {@link FilterReader} which replaces matches of the <var>pattern</var> within the character
     * stream on-the-fly with the <var>matchReplacer</var>.
     */
    public static Reader
    replaceAllFilterReader(Reader delegate, final Pattern pattern, final Function<Matcher, String> matchReplacer) {

        return new FilterReader(delegate) {

            AllReplacer allReplacer = PatternUtil.replaceAll(pattern, matchReplacer);

            /**
             * Sometimes we have "too many" characters than we can return, so we set them aside in this buffer.
             */
            String buffer = "";

            @Override public int
            read() throws IOException {

                if (!this.buffer.isEmpty()) {
                    char c = this.buffer.charAt(0);
                    this.buffer = this.buffer.substring(1);
                    return c;
                }

                int c = this.in.read();
                if (c == -1) {
                    String s = this.allReplacer.flush();
                    if (s.isEmpty()) return -1;
                    char c2 = s.charAt(0);
                    this.buffer = s.substring(1);
                    return c2;
                }

                String s = this.allReplacer.transform(new String(new char[] { (char) c }));
                if (s.isEmpty()) return -1;
                char c2 = s.charAt(0);
                this.buffer = s.substring(1);
                return c2;
            }

            @Override public int
            read(@Nullable char[] cbuf, int off, int len) throws IOException {

                String s;
                while (this.buffer.isEmpty()) {
                    char[] ca = new char[1024];

                    int n = this.in.read(ca);
                    if (n < 0) {
                        this.buffer = this.allReplacer.transform("");
                        if (this.buffer.isEmpty()) return -1;
                        break;
                    } else
                    if (n == 0) {
                        return 0;
                    } else
                    if (n > 0) {
                        this.buffer = this.allReplacer.transform(new String(ca, 0, n));
                    }
                }

                int bl = this.buffer.length();
                if (bl < len) {
                    System.arraycopy(this.buffer.toCharArray(), 0, cbuf, off, bl);
                    this.buffer = "";
                    return bl;
                } else {
                    System.arraycopy(this.buffer.toCharArray(), 0, cbuf, off, len);
                    this.buffer = this.buffer.substring(len);
                    return len;
                }
            }

            @Override public long
            skip(long n) throws IOException {

                if (n < 0L) throw new IllegalArgumentException("skip value is negative");

                int nn = (int) Math.min(n, MAX_SKIP_BUFFER_SIZE);

                char[] skipb = this.skipBuffer;
                if (skipb == null || skipb.length < nn) skipb = (this.skipBuffer = new char[nn]);

                int nc;
                for (long r = n; r > 0; r -= nc) {
                    nc = this.read(skipb, 0, (int) Math.min(r, nn));
                    if (nc == -1) return n - r;
                }
                return 0;
            }
            private static final int MAX_SKIP_BUFFER_SIZE = 8192;
            @Nullable private char[] skipBuffer;

            @Override public boolean
            ready() { return !this.buffer.isEmpty(); }

            @Override public boolean
            markSupported() { return false; }

            @Override public void
            mark(int readAheadLimit) throws IOException { throw new IOException("mark() not supported"); }

            @Override public void
            reset() throws IOException { throw new IOException("reset() not supported"); }
        };
    }

    /**
     * Creates and returns a {@link FilterWriter} which replaces matches of the <var>pattern</var> within the character
     * stream on-the-fly with the <var>matchReplacer</var>.
     */
    public static Writer
    replaceAllFilterWriter(final Pattern pattern, final Function<Matcher, String> matchReplacer, Writer delegate) {

        return new FilterWriter(delegate) {

            final AllReplacer allReplacer = PatternUtil.replaceAll(pattern, matchReplacer);

            @Override public void
            write(int c) throws IOException {
                this.write(new char[] { (char) c }, 0, 1);
            }

            @Override public void
            write(@Nullable char[] cbuf, int off, int len) throws IOException {
                this.append(new Segment(cbuf, off, len));
            }

            @Override public void
            write(@Nullable String str, int off, int len) throws IOException {
                this.append(str, off, len);
            }

            @Override public void
            flush() throws IOException {
                this.out.write(this.allReplacer.flush());
                this.out.flush();
            }

            @Override public void
            close() throws IOException {
                this.out.write(this.allReplacer.flush());
                this.out.close();
            }

            @NotNullByDefault(false) @Override public Writer
            append(CharSequence csq) throws IOException {
                if (csq.length() > 0) {
                    this.out.write(this.allReplacer.transform(csq));
                }
                return this;
            }

            @NotNullByDefault(false) @Override public Writer
            append(CharSequence csq, int start, int end) throws IOException {
                this.append(csq.subSequence(start, end));
                return this;
            }
        };
    }

    public
    interface AllReplacer extends Transformer<CharSequence, String> {

        /**
         * Substitutes all matches in the <var>subject</var> and returns it. If there is a "partial match" at the
         * end of the subject, then only a prefix of the result is returned, and the suffix is processed as part of
         * following invocations.
         */
        @Override String transform(CharSequence subject);

        /**
         * Executes any pending substitutions and returns the result. Eventually this replacer is in its initial
         * state, except that its {@link #substitutionCount()} is not reset to zero.
         */
        String flush();

        /**
         * @return The number of substitutions executed so far, i.e. the number of invocations of the
         *         <var>matchReplacer</var> that returned a non-{@code null} value
         */
        int substitutionCount();
    }

    /**
     * Replaces pattern matches in a stream of strings.
     * <p>
     *   As the returned transformer <em>consumes</em> a sequence of strings, it <em>produces</em> a sequence of
     *   strings, and the concatenation of the consumed strings equals the concatenation of the produced strings,
     *   except that all matches of the <var>pattern</var> are substituted with the <var>replacementString</var>.
     * </p>
     * <p>
     *   Iff the input to the transformer is {@code ""} (the empty string), then the "rest" of any pending matches is
     *   returned.
     * </p>
     */
    public static AllReplacer
    replaceAll(Pattern pattern, String replacementString) {
        return PatternUtil.replaceAll(pattern, PatternUtil.replacementStringMatchReplacer(replacementString));
    }

    /**
     * Replaces pattern matches in a stream of strings.
     * <p>
     *   As the returned transformer <em>consumes</em> a sequence of strings, it <em>produces</em> a sequence of
     *   strings, and the concatenation of the consumed strings equals the concatenation of the produced strings,
     *   except that all matches of the <var>pattern</var> are substituted with the replacements generated by the
     *   <var>matchReplacer</var>.
     * </p>
     * <p>
     *   Iff the input to the transformer is {@code ""} (the empty string), then the "rest" of any pending matches is
     *   returned.
     * </p>
     */
    public static AllReplacer
    replaceAll(final Pattern pattern, final Function<Matcher, String> matchReplacer) {

        return new AllReplacer() {

            /**
             * Contains a suffix of the input char sequence.
             */
            StringBuilder buffer = new StringBuilder();

            /**
             * Offset in {@link #buffer}
             */
            int start;

            private int substitutionCount;

            @Override public String
            transform(CharSequence in) {

                if (in.length() == 0) return this.flush();

                this.buffer.append(in);

                StringBuilder result = new StringBuilder();
                Matcher m = pattern.matcher(this.buffer);
                if (m.find(this.start) && !m.hitEnd()) {
                    do {

                        String replacement = matchReplacer.call(m);
                        if (replacement != null) {
                            result.append(this.buffer, this.start, m.start()).append(replacement);
                            this.start = m.end();
                            this.substitutionCount++;
                        }

                        if (m.start() == m.end()) {

                            // Special case: Zero-length match.
                            if (this.start == this.buffer.length()) break;
                            result.append(this.buffer.charAt(this.start++));
                        }
                    } while (m.find(this.start) &&!m.hitEnd());
                }

                if (m.hitEnd()) {
                    if (this.start > PatternUtil.LOOKBEHIND_LIMIT) {
                        this.buffer.delete(0, this.start - PatternUtil.LOOKBEHIND_LIMIT);
                        this.start = PatternUtil.LOOKBEHIND_LIMIT;
                    }
                } else {
                    result.append(this.buffer.substring(this.start));
                    if (this.buffer.length() <= PatternUtil.LOOKBEHIND_LIMIT) {
                        this.start = this.buffer.length();
                    } else {
                        this.buffer.delete(0, this.buffer.length() - PatternUtil.LOOKBEHIND_LIMIT);
                        this.start = PatternUtil.LOOKBEHIND_LIMIT;
                    }
                }

                return result.toString();
            }

            @Override public String
            flush() {

                if (this.buffer.length() == 0) return "";

                Matcher m = pattern.matcher(this.buffer);
                if (!m.find(this.start)) {

                    // No match in "the rest" - just return "the rest".
                    String result = this.buffer.substring(this.start);
                    this.buffer.setLength(0);
                    this.start = 0;
                    return result;
                }

                StringBuilder result = new StringBuilder();
                do {

                    String replacement = matchReplacer.call(m);
                    if (replacement != null) {
                        result.append(this.buffer, this.start, m.start()).append(replacement);
                        this.start = m.end();
                        this.substitutionCount++;
                    }

                    if (m.start() == m.end()) {

                        // Special case: Zero-length match.
                        if (this.start == this.buffer.length()) break;
                        result.append(this.buffer.charAt(this.start++));
                    }
                } while (m.find(this.start));

                result.append(this.buffer, this.start, this.buffer.length());
                this.buffer.setLength(0);
                this.start = 0;
                return result.toString();
            }

            @Override
            public int substitutionCount() { return this.substitutionCount; }
        };
    }
}
