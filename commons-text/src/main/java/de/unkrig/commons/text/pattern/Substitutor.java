
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, 2022, Arno Unkrig
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

import java.util.regex.Pattern;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.text.pattern.Finders.MatchResult2;

/**
 * Replaces pattern matches in a stream of strings ("chunks"). Matches are allowed across chunks; thus, this
 * transformer is stateful.
 * <p>
 *   As the substitutor <em>consumes</em> chunks, it <em>produces</em> a sequence of strings, and the concatenation of
 *   the chunks read equals the concatenation of the produced strings, except that all matches of the
 *   <var>pattern</var> are substituted with the replacements generated by the <var>matchReplacer</var>. (If the
 *   <var>matchReplacer</var> returns {@code null}, then the match is <em>not</em> replaced and is copied to the
 *   output as-is.)
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
 *   Substitutor#create(Pattern, FunctionWhichThrows, int)}.
 * </p>
 * <p>
 *   Also bear in mind that, specifically when using "greedy quantifiers", that it may happen quite easily that the
 *   entire input must be read into memory, only to find that there is no match. E.g. the pattern {@code "a.*b"}, as
 *   soon as it hits the letter "a", will load all the remaining text into memory, because there might still come
 *   (another) "b".
 * </p>
 *
 * @param <EX> The exception type that the "match replacer" (see {@link Substitutor#create(Pattern,
 *             FunctionWhichThrows)} is allowed to throw; use {@link NoException} if your match replacer does not throw
 *             any (checked) exceptions
 */
public final
class Substitutor<EX extends Throwable> implements TransformerWhichThrows<CharSequence, CharSequence, EX> {


    /**
     * The number of characters that can safely be used for look-behind, unless a different value is configured through
     * {@link #create(Pattern, FunctionWhichThrows, int)}.
     */
    public static final int DEFAULT_LOOKBEHIND_LIMIT = 10;

    private final ConsumerWhichThrows<CharSequence, EX> finder;

    private final StringBuilder buffer = new StringBuilder();

    /**
     * Number of substitutions executed so far.
     */
    private int substitutionCount = 0; // SUPPRESS CHECKSTYLE ExplicitInitialization

    private
    Substitutor(
        Pattern[]                                                                             patterns,
        final FunctionWhichThrows<? super MatchResult2, ? extends CharSequence, ? extends EX> matchReplacer,
        int                                                                                   lookBehindLimit
    ) {
        this.finder = Finders.patternFinder(
            patterns,                                     // patterns
            new ConsumerWhichThrows<MatchResult2, EX>() { // match

                @Override public void
                consume(MatchResult2 m) throws EX {
                    CharSequence replacement = matchReplacer.call(m);
                    if (replacement == null) {
                        Substitutor.this.buffer.append(m.group());
                    } else {
                        Substitutor.this.buffer.append(replacement);
                        Substitutor.this.substitutionCount++;
                    }
                }
            },
            new ConsumerWhichThrows<Character, EX>() {    // nonMatch

                @Override public void
                consume(Character c) {
                    Substitutor.this.buffer.append(c);
                }
            },
            lookBehindLimit
        );
    }

    /**
     * Equivalent with {@link #create(Pattern, FunctionWhichThrows, int) create}{@code (}<var>pattern</var>{@code ,}
     * <var>matchReplacer</var>{@code ,} {@link #DEFAULT_LOOKBEHIND_LIMIT}{@code )}.
     */
    public static <EX extends Throwable> Substitutor<EX>
    create(
        Pattern                                                                         pattern,
        FunctionWhichThrows<? super MatchResult2, ? extends CharSequence, ? extends EX> matchReplacer
    ) {
        return Substitutor.create(pattern, matchReplacer, Substitutor.DEFAULT_LOOKBEHIND_LIMIT);
    }

    /**
     * Equivalent with {@link #create(Pattern[], FunctionWhichThrows, int) create}<code>(new Pattern[] {
     * </code><var>pattern</var><code> }, </code> <var>matchReplacer</var>{@code , }<var>lookBehindLimit</var>{@code )}.
     */
    public static <EX extends Throwable> Substitutor<EX>
    create(
        Pattern                                                                               pattern,
        final FunctionWhichThrows<? super MatchResult2, ? extends CharSequence, ? extends EX> matchReplacer,
        int                                                                                   lookBehindLimit
    ) {
        return new Substitutor<EX>(new Pattern[] { pattern }, matchReplacer, lookBehindLimit);
    }

    /**
     * @return A {@link Substitutor} that substitutes all matches of the <var>patterns</var> through the
     *         <var>matchReplacer</var>
     * @see    Substitutor
     */
    public static <EX extends Throwable> Substitutor<EX>
    create(
        Pattern[]                                                                             patterns,
        final FunctionWhichThrows<? super MatchResult2, ? extends CharSequence, ? extends EX> matchReplacer,
        int                                                                                   lookBehindLimit
    ) {
        return new Substitutor<EX>(patterns, matchReplacer, lookBehindLimit);
    }

    /**
     * Substitutes all matches in the <var>subject</var> and returns it. If there is a "partial match" at the
     * end of the subject, then only a prefix of the result is returned, and the suffix is processed as part of
     * following invocations.
     */
    @Override public CharSequence
    transform(CharSequence in) throws EX {

        this.finder.consume(in);

        String result = this.buffer.toString();
        this.buffer.setLength(0);
        return result;
    }

    /**
     * @return The number of substitutions executed so far, i.e. the number of invocations of the
     *         <var>matchReplacer</var> that returned a non-{@code null} value
     */
    public int
    substitutionCount() { return this.substitutionCount; }
}
