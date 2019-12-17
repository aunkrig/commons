
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

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Replaces pattern matches in a stream of strings ("chunks"). Matches are allowed across chunks; thus, this
 * transformer is stateful.
 * <p>
 *   As the substitutor <em>consumes</em> chunks, it <em>produces</em> a sequence of strings, and the concatenation of
 *   the chunks read equals the concatenation of the produced strings, except that all matches of the
 *   <var>pattern</var> are substituted with the replacements generated by the <var>matchReplacer</var>.
 * </p>
 * <p>
 *   Iff the input to the transformer is {@code ""} (the empty string), then the "rest" of any pending matches is
 *   returned. This is sort of a "flush" operation.
 * </p>
 * <p>
 *   The substitutor attempts to keep as little characters as possible in memory, which make it possible to find
 *   matches in documents that are too large to load into memory.
 * </p>
 * <p>
 *   If you plan to use "look-behinds" (the most common of which is "^"), bear in mind that the look-behind memory is
 *   limited; by default to {@value #DEFAULT_LOOKBEHIND_LIMIT}. If you need more look-behind space, use {@link
 *   #Substitutor(Pattern, FunctionWhichThrows, int)}.
 * </p>
 * <p>
 *   Also bear in mind that, specifically when using "greedy quantifiers", that it may happen quite easily that the
 *   entire input must be read into memory, only to find that there is no match. E.g. the pattern {@code "a.*b"}, as
 *   soon as it hits the letter "a", will load all the remaining text into memory, because there might still come
 *   (another) "b".
 * </p>
 *
 * @param <EX> The exception type that the "match replacer" (see {@link #Substitutor(Pattern, FunctionWhichThrows)}
 *             is allowed to throw; use {@link NoException} if your match replacer does not throw any (checked)
 *             exceptions
 */
public
class Substitutor<EX extends Throwable> implements TransformerWhichThrows<CharSequence, CharSequence, EX> {

    /**
     * The number of characters that can safely be used for look-behind, unless a different value is configured through
     * {@link #Substitutor(Pattern, FunctionWhichThrows, int)}.
     */
    public static final int DEFAULT_LOOKBEHIND_LIMIT = 10;

    // CONFIGURATION

    private final Pattern                                                                    pattern;
    private final FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends EX> matchReplacer;
    private final int                                                                        lookBehindLimit;

    /**
     * Contains a suffix of the input char sequence.
     */
    StringBuilder buffer = new StringBuilder();

    /**
     * Offset in {@link #buffer}
     */
    int start;

    /**
     * Number of substitutions executed so far.
     */
    private int substitutionCount;

    private int offsetDelta;

    /**
     * @deprecated The {@link Matcher} that is fed to the <var>matchReplacer</var> has incorrect offsets; use {@link
     *             #create(Pattern, FunctionWhichThrows)} instead
     */
    @Deprecated public
    Substitutor(
        Pattern                                                                    pattern,
        FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends EX> matchReplacer
    ) {
        this(pattern, matchReplacer, Substitutor.DEFAULT_LOOKBEHIND_LIMIT);
    }

    /**
     * @deprecated The {@link Matcher} that is fed to the <var>matchReplacer</var> has incorrect offsets; use {@link
     *             #create(Pattern, FunctionWhichThrows, int)} instead
     */
    @Deprecated public
    Substitutor(
        Pattern                                                                    pattern,
        FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends EX> matchReplacer,
        int                                                                        lookBehindLimit
    ) {
        this.pattern         = pattern;
        this.matchReplacer   = matchReplacer;
        this.lookBehindLimit = lookBehindLimit;
    }

    /**
     * Equivalent with {@link #create(Pattern, FunctionWhichThrows, int) create}{@code (}<var>pattern</var>{@code ,}
     * <var>matchReplacer</var>{@code ,} {@link #DEFAULT_LOOKBEHIND_LIMIT}{@code )}.
     */
    public static <EX extends Throwable> Substitutor<EX>
    create(
        Pattern                                                                        pattern,
        FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends EX> matchReplacer
    ) {
        return Substitutor.create(pattern, matchReplacer, Substitutor.DEFAULT_LOOKBEHIND_LIMIT);
    }

    /**
     * @return A {@link Substitutor} that substitutes all matches of the <var>pattern</var> through the
     *         <var>matchReplacer</var>
     */
    public static <EX extends Throwable> Substitutor<EX>
    create(
        Pattern                                                                              pattern,
        final FunctionWhichThrows<? super MatchResult, ? extends CharSequence, ? extends EX> matchReplacer,
        int                                                                                  lookBehindLimit
    ) {
        final Substitutor<?>[] s = new Substitutor<?>[1];

        Substitutor<EX> result = new Substitutor<EX>(pattern, new FunctionWhichThrows<Matcher, CharSequence, EX>() {

            @Override @Nullable public CharSequence
            call(final @Nullable Matcher m) throws EX {
                assert m != null;
                return matchReplacer.call(new MatchResult() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override public int    start(int group) { return m.start(group) + s[0].offsetDelta; }
                    @Override public int    start()          { return m.start()      + s[0].offsetDelta; }
                    @Override public int    groupCount()     { return m.groupCount();                    }
                    @Override public String group(int group) { return m.group(group);                    }
                    @Override public String group()          { return m.group();                         }
                    @Override public int    end(int group)   { return m.end(group)   + s[0].offsetDelta; }
                    @Override public int    end()            { return m.end()        + s[0].offsetDelta; }

					@Override public String
					toString() {
						String s = "[match(" + m.start() + '-' + m.end() + ")=" + this.group();
						for (int i = 1; i <= this.groupCount(); i++) {
							s += ", group#" + i + '(' + m.start(i) + '-' + m.end(i) + ")=" + m.group(i);
						}
						s += ']';
						return s;
					}
                });
            }
        }, lookBehindLimit);
        s[0] = result;
        return result;
    }

    /**
     * Substitutes all matches in the <var>subject</var> and returns it. If there is a "partial match" at the
     * end of the subject, then only a prefix of the result is returned, and the suffix is processed as part of
     * following invocations.
     */
    @Override public CharSequence
    transform(CharSequence in) throws EX {

        if (in.length() == 0) return this.flush();

        this.buffer.append(in);

        final StringBuilder result = new StringBuilder();
        final Matcher       m      = this.pattern.matcher(this.buffer);
        m.useTransparentBounds(true);
        m.useAnchoringBounds(false);

        for (;;) {
            m.region(this.start, this.buffer.length());
            if (m.lookingAt()) {
                if (m.hitEnd()) {

                    // E.g. "A.*B" => "AxxxBxx"
                    break;
                }

                // E.g. "A" => "Axxx"
                CharSequence replacement = this.matchReplacer.call(m);

                if (replacement == null) {
                    result.append(this.buffer.charAt(this.start++));
                } else {
                    this.substitutionCount++;
                    result.append(replacement);
                    this.substitutionCount++;
                    if (m.end() == m.start()) {
                        result.append(this.buffer.charAt(this.start++));
                    } else {
                        this.start = m.end();
                    }
                }
            } else {
                if (m.hitEnd()) {

                    // E.g. "Axxxxxx" => "Axxx"
                    break;
                }

                // E.g. "A" => "Bxx"
                if (this.start == this.buffer.length()) break;
                result.append(this.buffer.charAt(this.start++));
            }
        }

        if (this.start > this.lookBehindLimit) {

            // Truncate the buffer to save memory.
            this.buffer.delete(0, this.start - this.lookBehindLimit);
            this.start = this.lookBehindLimit;
        }

        if (this.buffer.capacity() > 10 * this.buffer.length()) this.buffer.trimToSize();

        return result.toString();
    }

    /**
     * @return The number of substitutions executed so far, i.e. the number of invocations of the
     *         <var>matchReplacer</var> that returned a non-{@code null} value
     */
    public int
    substitutionCount() { return this.substitutionCount; }

    private CharSequence
    flush() throws EX {

        if (this.buffer.length() == 0) return "";

        Matcher m = this.pattern.matcher(this.buffer);
        m.useTransparentBounds(true);
        m.useAnchoringBounds(false);
        m.region(this.start, this.buffer.length());

        if (!m.find(this.start)) {

            // No match in "the rest" - just return "the rest".
            final String result = this.buffer.substring(this.start);
            this.buffer.setLength(0);
            this.start = 0;
            return result;
        }

        StringBuilder result = new StringBuilder();
        do {

            CharSequence replacement = this.matchReplacer.call(m);
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
}
