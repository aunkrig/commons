
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2018, Arno Unkrig
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
import de.unkrig.commons.lang.protocol.NoException;

/**
 * Searches for pattern matches in a stream of strings ("chunks"). Matches are allowed across chunks; thus, this
 * class is stateful.
 * <p>
 *   As the searcher consumes chunks, it invokes a "match handler" for each match.
 * </p>
 * <p>
 *   The {@link Searcher} attempts to keep as little characters as possible in memory, which make it possible to find
 *   matches in documents that are too large to load into memory.
 * </p>
 * <p>
 *   If you plan to use "look-behinds" (the most common of which is "^"), bear in mind that the look-behind memory is
 *   limited; by default to {@value #DEFAULT_LOOKBEHIND_LIMIT}. If you need more look-behind space, use {@link
 *   #Searcher(Pattern, ConsumerWhichThrows, int)}.
 * </p>
 * <p>
 *   Also bear in mind that, specifically when using "greedy quantifiers", that it may happen quite easily that the
 *   entire input must be read into memory, only to find that there is no match. E.g. the pattern {@code "a.*b"}, as
 *   soon as it hits the letter "a", will load all the remaining text into memory, because there might still come
 *   (another) "b".
 * </p>
 *
 * @param <EX> The exception type that the "match handler" (see {@link #Searcher(Pattern, ConsumerWhichThrows)}
 *             is allowed to throw; use {@link NoException} if your match handler does not throw any (checked)
 *             exceptions
 * @deprecated Use the more modern {@link Finders} instead
 */
@Deprecated
public
class Searcher<EX extends Throwable> implements ConsumerWhichThrows<CharSequence, EX> {

    /**
     * The number of characters that can safely be used for look-behind, unless a different value is configured through
     * {@link #Searcher(Pattern, ConsumerWhichThrows, int)}.
     */
    public static final int DEFAULT_LOOKBEHIND_LIMIT = 10;

    // CONFIGURATION

    private final Pattern                                                pattern;
    private final ConsumerWhichThrows<? super MatchResult, ? extends EX> matchHandler;
    private final int                                                    lookBehindLimit;

    /**
     * Contains a suffix of the input char sequence.
     */
    StringBuilder buffer = new StringBuilder();

    /**
     * Offset in {@link #buffer}
     */
    int start;

    /**
     * Number of matches so far.
     */
    private int matchCount;

    private int offsetDelta;

    /**
     * Equivalent with {@link #Searcher(Pattern, ConsumerWhichThrows, int) Searcher}{@code (}<var>pattern</var>{@code
     * ,} <var>matchHandler</var>{@code ,} {@link #DEFAULT_LOOKBEHIND_LIMIT}{@code )}.
     */
    public
    Searcher(
        Pattern                                                pattern,
        ConsumerWhichThrows<? super MatchResult, ? extends EX> matchHandler
    ) {
        this(pattern, matchHandler, Searcher.DEFAULT_LOOKBEHIND_LIMIT);
    }

    public
    Searcher(
        Pattern                                                pattern,
        ConsumerWhichThrows<? super MatchResult, ? extends EX> matchHandler,
        int                                                    lookBehindLimit
    ) {
        this.pattern         = pattern;
        this.matchHandler    = matchHandler;
        this.lookBehindLimit = lookBehindLimit;
    }

    /**
     * Invokes the "match handler" for each match in the <var>in</var>. If there is a "partial match" at the
     * end of <var>in</var>, then the suffix is processed as part of following invocations.
     */
    @Override public void
    consume(CharSequence in) throws EX {

        this.buffer.append(in);

        Matcher m = this.pattern.matcher(this.buffer);
        m.useTransparentBounds(true);
        m.useAnchoringBounds(false);

        for (;;) {

            m.region(this.start, this.buffer.length());

            // Match at current position?
            boolean la = m.lookingAt();

            if (m.hitEnd()) {

                // E.g. "A.*B" => "AxxxBxx" => Not where the match will end
                // E.g. "Axxxxxx" => "Axxx" => Not sure if match
                break;
            }

            if (la) {

                // E.g. "A" => "Axxx"
                MatchResult matchResult = new MatchResult() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override public int    start(int group) { return m.start(group) + Searcher.this.offsetDelta; }
                    @Override public int    start()          { return m.start()      + Searcher.this.offsetDelta; }
                    @Override public int    groupCount()     { return m.groupCount();                             }
                    @Override public String group(int group) { return m.group(group);                             }
                    @Override public String group()          { return m.group();                                  }
                    @Override public int    end(int group)   { return m.end(group)   + Searcher.this.offsetDelta; }
                    @Override public int    end()            { return m.end()        + Searcher.this.offsetDelta; }
                };

                this.matchHandler.consume(matchResult);
                this.matchCount++;

                if (m.end() == m.start() && this.start < this.buffer.length()) {

                    // Special case: Zero-width match.
                    this.start++;
                } else {
                    this.start = m.end();
                }
            } else {

                // E.g. "A" => "Bxx"
                if (this.start == this.buffer.length()) break;
                this.start++;
            }
        }

        if (this.start > this.lookBehindLimit) {

            // Truncate the buffer to save memory.
            int delta = this.start - this.lookBehindLimit;
            this.buffer.delete(0, delta);
            this.offsetDelta += delta;
            this.start       =  this.lookBehindLimit;
        }

        if (this.buffer.capacity() > 10 * this.buffer.length()) this.buffer.trimToSize();
    }

    /**
     * @return The number of matches so far, i.e. the number of invocations of the <var>matchHandler</var>
     */
    public int
    matchCount() { return this.matchCount; }
}
