
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2022, Arno Unkrig
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

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;

/**
 * Finding "matches" of {@link Pattern}s in streams of characters.
 */
public
class Finders {

    /**
     * The number of characters that can safely be used for look-behind, unless a different value is configured through
     * {@link Substitutor#Substitutor(Pattern[], FunctionWhichThrows, int)}.
     */
    public static final int DEFAULT_LOOKBEHIND_LIMIT = 10;

    /**
     * Equivalent with {@link #patternFinder(Pattern[], ConsumerWhichThrows, ConsumerWhichThrows, RunnableWhichThrows,
     * int) patternFinder}{@code (}<var>patterns</var>{@code ,} <var>match</var>{@code ,} <var>nonMatch</var>{@code ,}
     * {@link RunnableUtil#NOP}{@code ,} {@link #DEFAULT_LOOKBEHIND_LIMIT}{@code )}.
     */
    public static <EX extends Throwable> ConsumerWhichThrows<CharSequence, EX>
    patternFinder(
        Pattern[]                                                patterns,
        ConsumerWhichThrows<? super MatchResult2, ? extends EX>  match,
        ConsumerWhichThrows<? super Character, ? extends EX>     nonMatch
    ) {
        return Finders.patternFinder(
            patterns,
            match,
            nonMatch,
            RunnableUtil.asRunnableWhichThrows(RunnableUtil.NOP), // flush
            Finders.DEFAULT_LOOKBEHIND_LIMIT                      // lookBehindLimit
        );
    }

    /**
     * Equivalent with {@link #patternFinder(Pattern[], ConsumerWhichThrows, ConsumerWhichThrows, RunnableWhichThrows,
     * int) patternFinder}{@code (}<var>patterns</var>{@code ,} <var>match</var>{@code ,} <var>nonMatch</var>{@code ,}
     * <var>flush</var>{@code ,} {@link #DEFAULT_LOOKBEHIND_LIMIT}{@code )}.
     */
    public static <EX extends Throwable> ConsumerWhichThrows<CharSequence, EX>
    patternFinder(
        Pattern[]                                                patterns,
        ConsumerWhichThrows<? super MatchResult2, ? extends EX>  match,
        ConsumerWhichThrows<? super Character, ? extends EX>     nonMatch,
        RunnableWhichThrows<? extends EX>                        flush
    ) { return Finders.patternFinder(patterns, match, nonMatch, flush, Finders.DEFAULT_LOOKBEHIND_LIMIT); }

    /**
     * Equivalent with {@link #patternFinder(Pattern[], ConsumerWhichThrows, ConsumerWhichThrows, RunnableWhichThrows,
     * int) patternFinder}{@code (}<var>patterns</var>{@code ,} <var>match</var>{@code ,} <var>nonMatch</var>{@code ,}
     * {@link RunnableUtil#NOP}{@code ,} <var>lookBehindLimit</var>{@code )}.
     */
    public static <EX extends Throwable> ConsumerWhichThrows<CharSequence, EX>
    patternFinder(
        final Pattern[]                                               patterns,
        final ConsumerWhichThrows<? super MatchResult2, ? extends EX> match,
        final ConsumerWhichThrows<? super Character, ? extends EX>    nonMatch,
        final int                                                     lookBehindLimit
    ) {
        return Finders.patternFinder(
            patterns,
            match,
            nonMatch,
            RunnableUtil.asRunnableWhichThrows(RunnableUtil.NOP), // flush
            lookBehindLimit
        );
    }

    /**
     * Creates and returns a {@link ConsumerWhichThrows} which consumes chunks of text, and invokes <var>match</var>
     * and <var>nonMatch</var> such that the sequence of these invocations matches exactly the input chunk stream.
     * <p>
     *   Because matches may stretch across multiple input chunks, the pattern finder may store the input in an internal
     *   buffer and invoke <var>match</var> and <var>noMatch</var> only later, when the match is complete. Notice
     *   that greedy quantifiers may cause the pattern finder to store lots and lots of characters in its buffer.
     * </p>
     * <p>
     *   To indicate the end of the input, let the returned consumer consume an empty string ({@code ""}).
     * </p>
     * <p>
     *   For example, with one pattern {@code "A"}, the input <code>{ "__A__", "" }</code> would invoke
     * </p>
     * <ul>
     *   <li>{@code nonMatch('_')}
     *   <li>{@code nonMatch('_')}
     *   <li>{@code match(<A>)}
     *   <li>{@code nonMatch('_')}
     *   <li>{@code nonMatch('_')}
     * </ul>
     * <p>
     *   If more than one of the patterns matches at the same position, then the <em>first</em> of the patterns will be
     *   applied (even if the other patterns would yield longer matches).
     * </p>
     */
    public static <EX extends Throwable> ConsumerWhichThrows<CharSequence, EX>
    patternFinder(
        final Pattern[]                                               patterns,
        final ConsumerWhichThrows<? super MatchResult2, ? extends EX> match,
        final ConsumerWhichThrows<? super Character, ? extends EX>    nonMatch,
        final RunnableWhichThrows<? extends EX>                       flush,
        final int                                                     lookBehindLimit
    ) {
        return new ConsumerWhichThrows<CharSequence, EX>() {

            /**
             * Contains a suffix of the input char sequence.
             */
            StringBuilder buffer = new StringBuilder();

            /**
             * Offset in {@link #buffer}
             */
            int start;

            private int bufferOffset;

            private boolean flushed;

            @Override public void
            consume(CharSequence in) throws EX {

                // Unfortunately, "flush()" is not idempotent... must make sure that the finder is never multi-flushed.
                if (in.length() == 0) {
                    if (this.flushed) return;
                    this.flush();
                    flush.run();
                    this.flushed = true;
                    return;
                }
                this.flushed = false;

                this.buffer.append(in);

                NEXT_CHAR:
                while (this.start < this.buffer.length()) {

                    for (final Pattern pattern : patterns) {

                        final Matcher m = pattern.matcher(this.buffer);
                        m.useTransparentBounds(true);
                        m.useAnchoringBounds(false);
                        m.region(this.start, this.buffer.length());

                        if (m.lookingAt()) {
                            if (m.hitEnd()) {

                                // E.g. "A.*B" => "AxxxBxx" => matches, but more input could lead to a different match.
                                break NEXT_CHAR;
                            }

                            // E.g. "A" => "Axxx" => matches, and more input would not change the match.
                            match.consume(Finders.offset(m, this.bufferOffset));

                            this.start = m.end();

                            if (m.end() == m.start() && this.start < this.buffer.length()) { // Special case: Zero-length match.
                                nonMatch.consume(this.buffer.charAt(this.start++));
                            }
                            continue NEXT_CHAR;
                        } else {
                            if (m.hitEnd()) {

                                // E.g. "Axxxxxx" => "Axxx" => No match, but more input could lead to a match.
                                break NEXT_CHAR;
                            }
                        }
                    }

                    // E.g. "A" => "Bxx" => No match, and more input would not lead to a match (starting within "Bxx").
                    nonMatch.consume(this.buffer.charAt(this.start++));
                }

                if (this.start > lookBehindLimit) {

                    // Truncate the buffer to save memory.
                    int n = this.start - lookBehindLimit;

                    this.buffer.delete(0, n);
                    this.start        -= n;
                    this.bufferOffset += n;
                }

                if (this.buffer.capacity() > 10 * this.buffer.length()) this.buffer.trimToSize();
            }

            private void
            flush() throws EX {

                NEXT_CHAR:
                for (;;) {

                    for (Pattern pattern : patterns) {

                        Matcher m = pattern.matcher(this.buffer);
                        m.useTransparentBounds(true);
                        m.useAnchoringBounds(false);
                        m.region(this.start, this.buffer.length());

                        if (m.lookingAt()) {

                            // E.g. "A" => "Axxx" => matches.
                            match.consume(Finders.offset(m, this.bufferOffset));

                            this.start = m.end();

                            if (m.end() == m.start()) {

                                // Special case: Zero-length match.
                                if (this.start >= this.buffer.length()) break NEXT_CHAR;

                                nonMatch.consume(this.buffer.charAt(this.start++));
                            }

                            continue NEXT_CHAR;
                        }
                    }

                    if (this.start >= this.buffer.length()) break;

                    // E.g. "A" => "Bxx" => No match.
                    nonMatch.consume(this.buffer.charAt(this.start++));
                }

                this.buffer.setLength(0);
                this.start = 0;
            }
        };
    }

    protected static MatchResult2
    offset(final Matcher m, final int bufferOffset) {
        return new MatchResult2() {

            // SUPPRESS CHECKSTYLE LineLength:7
            @Override public int     start(int group) { return m.start(group) + bufferOffset; }
            @Override public int     start()          { return m.start()      + bufferOffset; }
            @Override public int     groupCount()     { return m.groupCount();                }
            @Override public String  group(int group) { return m.group(group);                }
            @Override public String  group()          { return m.group();                     }
            @Override public int     end(int group)   { return m.end(group)   + bufferOffset; }
            @Override public int     end()            { return m.end()        + bufferOffset; }
            @Override public Pattern pattern()        { return m.pattern();                   }

            @Override public String
            toString() {
                String s = "[match(" + m.start() + '-' + m.end() + ")=" + this.group();
                for (int i = 1; i <= this.groupCount(); i++) {
                    s += ", group#" + i + '(' + m.start(i) + '-' + m.end(i) + ")=" + m.group(i);
                }
                s += ']';
                return s;
            }
        };
    }

    public
    interface MatchResult2 extends MatchResult {
        Pattern pattern();
    }
}
