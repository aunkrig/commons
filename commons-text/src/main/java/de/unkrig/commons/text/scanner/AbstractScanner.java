
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.text.scanner;

import java.util.Collection;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;

/**
 * A scanner that produces {@link Token}s.
 *
 * @param <TT> Enumerates the scanner-specific token types
 * @see        #produce()
 */
public abstract
class AbstractScanner<TT extends Enum<TT>> implements StringScanner<TT> {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * Representation of a scanned token.
     *
     * @param <TT>
     */
    public static
    class Token<TT extends Enum<TT>> {

        /**
         * The type of this token.
         */
        public final TT type;

        /**
         * The text of this token, exactly as read from the document.
         */
        public final String text;

        /**
         * The input subsequences captured by the rule's pattern match. The length equals the number of capturing
         * groups, and the first capturing group is stored at position zero.
         * <p>
         *   Notice that multiple rules may generate the same token type, while the rules' patterns have different
         *   capturing groups.
         * </p>
         * <p>
         *   An array of length zero means that the matched pattern has no capturing groups.
         * </p>
         *
         * @see Pattern
         */
        public final String[] captured;

        private static final String[] DEFAULT_CAPTURED = new String[0];

        public
        Token(TT type, String text) {
            assert type != null;
            assert text != null;

            this.type     = type;
            this.text     = text;
            this.captured = Token.DEFAULT_CAPTURED;
        }

        public
        Token(TT type, String text, String[] captured) {
            assert type != null;
            assert text != null;

            this.type     = type;
            this.text     = text;
            this.captured = captured;
        }

        @Override public String
        toString() {
            return this.text;
        }
    }

    @Override public AbstractScanner<TT>
    setInput(CharSequence cs) { return this.setInput(cs, 0, cs.length()); }

    @Override public AbstractScanner<TT>
    setInput(CharSequence cs, int start, int end) {
        this.cs                  = cs;
        this.offset              = start;
        this.end                 = end;
        this.previousTokenOffset = -1;
        return this;
    }

    @Override public int
    getOffset() {
        return this.offset;
    }

    @Override public int
    getPreviousTokenOffset() {
        return this.previousTokenOffset;
    }

    /**
     * Creates and returns a producer which skips tokens of the <var>suppressedTokenType</var>.
     */
    public ProducerWhichThrows<Token<TT>, ScanException>
    suppress(final TT suppressedTokenType) {
        return ProducerUtil.filter(this, new Predicate<Token<TT>>() {
            @Override public boolean evaluate(Token<TT> token) { return token.type != suppressedTokenType; }
        });
    }

    /**
     * Creates and returns a producer which skips tokens of the <var>suppressedTokenTypes</var>.
     */
    public ProducerWhichThrows<Token<TT>, ScanException>
    suppress(final Collection<TT> suppressedTokenTypes) {
        return ProducerUtil.filter(this, new Predicate<Token<TT>>() {
            @Override public boolean evaluate(Token<TT> token) { return !suppressedTokenTypes.contains(token.type); }
        });
    }

    @Override public String
    toString() {
        return "\"" + this.cs + "\" at offset " + this.previousTokenOffset;
    }

    // STATE

    /**
     * The string currently being scanned; typically one line from an input document.
     */
    protected CharSequence cs = "";

    /**
     * The position within {@link #cs} of the next token to be scanned.
     */
    protected int offset;

    /**
     * The position of the the first character after the scannable region.
     */
    protected int end;

    /**
     * The position within {@link #cs} of the previously scanned token.
     */
    protected int previousTokenOffset = -1;
}
