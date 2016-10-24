
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.text.scanner;

import de.unkrig.commons.lang.AssertionUtil;

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

        public
        Token(TT type, String text) {
            assert type != null;
            assert text != null;

            this.type = type;
            this.text = text;
        }

        @Override public String
        toString() {
            return this.text;
        }
    }

    @Override public AbstractScanner<TT>
    setInput(CharSequence cs) {
        this.cs                  = cs;
        this.offset              = 0;
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

    @Override public String
    toString() {
        return "'" + this.cs + "' at offset " + this.previousTokenOffset;
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
     * The position within {@link #cs} of the previously scanned token.
     */
    protected int previousTokenOffset = -1;
}
