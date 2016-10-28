
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

package de.unkrig.commons.text.parser;

import java.util.Arrays;

import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;

/**
 * The base class for implementing parsers. Typically, you would declare methods named 'parse...()' which invoke
 * each other and the 'peek...()', 'read...()' and 'peekRead...()' methods to parse a document.
 *
 * @param <TT> The enumerator representing the scanner's token types
 */
public
class AbstractParser<TT extends Enum<TT>> {

    /**
     * The source of tokens that are processed by this parser.
     */
    protected final ProducerWhichThrows<? extends Token<TT>, ? extends ScanException> scanner;

    /**
     * One token read-ahead.
     */
    @Nullable private Token<TT> current;

    /**
     * @param scanner Its {@code toString()} method returns a human-readable indication of the scanner location
     */
    public
    AbstractParser(ProducerWhichThrows<? extends Token<TT>, ? extends ScanException> scanner) {
        this.scanner = scanner;
    }

    // PEEK METHODS

    /**
     * Checks the next token, but does not consume it.
     *
     * @return The next token, or {@code null} if the scanner is at end-of-input
     */
    @Nullable public Token<TT>
    peek() throws ParseException {
        if (this.current == null) this.current = this.produceToken();
        return this.current;
    }

    /**
     * Checks the next token, but does not consume it.
     *
     * @return The next token's text, or {@code null} if the next token's type is not {@code tokenType}, or if the
     *         scanner is at end-of-input
     */
    @Nullable public String
    peek(TT tokenType) throws ParseException {
        this.peek();
        return this.current != null && this.current.type == tokenType ? this.current.text : null;
    }

    /**
     * Checks the next token, but does not consume it.
     *
     * @return Whether the next token's text equals {@code text}, or the scanner is not at end-of-input and
     *         <var>text</var> is {@code null}
     */
    public boolean
    peek(@Nullable String text) throws ParseException {

        this.peek();

        Token<TT> current = this.current;
        return current == null ? text == null : current.text.equals(text);
    }

    /**
     * Checks the next token, but does not consume it.
     * <p>
     *   An element of <var>tokenTypeOrText</var> matches iff:
     * </p>
     * <ul>
     *   <li>the element equals the next token's <b>type</b>, or</li>
     *   <li>the element equals the next token's <b>text</b>, or</li>
     *   <li>the element is {@code null} and the scanner is at end-of-input</li>
     * </ul>
     *
     * @return The index of the first element of <var>tokenTypeOrText</var> that matches, or {@code -1}
     */
    public int
    peek(Object... tokenTypeOrText) throws ParseException {
        Token<TT> c = this.peek();

        for (int i = 0; i < tokenTypeOrText.length; i++) {
            Object ttot = tokenTypeOrText[i];
            if (c == null ? ttot == null : c.type.equals(ttot) || c.text.equals(ttot)) return i;
        }
        return -1;
    }


    // PEEK READ METHODS

    /**
     * Checks the next token and consumes it if its type is {@code tokenType}.
     *
     * @return The text of the next token, or {@code null} if the next token's type is not {@code tokenType}, or if
     *         the scanner is at end-of-input
     */
    @Nullable public String
    peekRead(TT tokenType) throws ParseException {
        Token<TT> c = this.peek();
        if (c == null || c.type != tokenType) return null;

        final String result = c.text;
        this.current = null;
        return result;
    }

    /**
     * Checks the next token and consumes it if it matches.
     *
     * @return Whether the next token's text equals {@code text}, or the scanner is at end-of-input and <var>text</var>
     *         is {@code null}
     */
    public boolean
    peekRead(@Nullable String text) throws ParseException {

        Token<TT> c = this.peek();

        if (c == null) return text == null;

        if (!c.text.equals(text)) return false;

        this.current = null;
        return true;
    }

    /**
     * Checks the next token and consumes it if it matches.
     * <p>
     *   An element of <var>texts</var> matches iff:
     * </p>
     * <ul>
     *   <li>the element equals the next token's text, or</li>
     *   <li>the element is {@code null} and the scanner is at end-of-input</li>
     * </ul>
     *
     * @return The index of the first element of <var>texts</var> that matches, or {@code -1}
     */
    public int
    peekRead(String... texts) throws ParseException {
        Token<TT> c = this.peek();
        if (c == null) return -1;

        for (int i = 0; i < texts.length; i++) {
            if (c.text.equals(texts[i])) {
                this.current = null;
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks the next token and consumes it if its text equals the return value of {@link Object#toString()
     * toString()} of one of the {@code values}.
     *
     * @return The matched value, or {@code null} if none match, or the scanner is at end-of-input
     */
    @Nullable public <T extends Enum<T>> T
    peekReadEnum(T... values) throws ParseException {
        Token<TT> c = this.peek();
        if (c == null) return null;

        for (T value : values) {
            if (c.text.equals(value.toString())) {
                this.current = null;
                return value;
            }
        }

        return null;
    }

    /**
     * Checks the next token and consumes it if its type is one of the {@code tokenTypes}.
     *
     * @return The matched token, or {@code null} iff none match, or the scanner is at end-of-input
     */
    @Nullable public Token<TT>
    peekRead(TT... tokenTypes) throws ParseException {
        Token<TT> c = this.peek();
        if (c == null) return null;

        for (TT tokenType : tokenTypes) {
            if (c.type == tokenType) {
                this.current = null;
                return c;
            }
        }
        return null;
    }

    // READ METHODS

    /**
     * Consumes the next token.
     *
     * @return                The next token
     * @throws ParseException The scanner is at end-of-input
     */
    public Token<TT>
    read() throws ParseException {
        if (this.current != null) {
            final Token<TT> result = this.current;
            this.current = null;
            return result;
        }
        Token<TT> result = this.produceToken();
        if (result == null) throw new ParseException("Unexpected end of input");
        return result;
    }

    /**
     * Consumes the next token.
     *
     * @return                The next token's text
     * @throws ParseException The next token's type is not {@code tokenType}
     * @throws ParseException The scanner is at end-of-input
     */
    public String
    read(TT tokenType) throws ParseException {
        Token<TT> result = this.read();
        if (result.type != tokenType) {
            throw new ParseException("'" + tokenType + "' expected instead of '" + result + "'");
        }
        return result.text;
    }

    /**
     * Consumes the next token.
     *
     * @throws ParseException The next token's text does not equal {@code text}
     * @throws ParseException The scanner is at end-of-input
     * @see #eoi()
     */
    public void
    read(String text) throws ParseException {
        Token<TT> t = this.read();
        if (!t.text.equals(text)) throw new ParseException("'" + text + "' expected instead of '" + t + "'");
    }

    /**
     * Consumes the next token.
     * <p>
     *   An element of <var>tokenTypeOrText</var> matches iff:
     * </p>
     * <ul>
     *   <li>the element equals the next token's type, or</li>
     *   <li>the element equals the next token's text, or</li>
     *   <li>the element is {@code null} and the scanner is at end-of-input</li>
     * </ul>
     *
     * @return                The index of the first element of <var>tokenTypeOrText</var> that matches
     * @throws ParseException Neither the next token's type nor its text equals any of {@code tokenTypeOrText}
     * @throws ParseException The scanner is at end-of-input
     */
    public int
    read(Object... tokenTypeOrText) throws ParseException {
        Token<TT> c = this.peek();
        if (c == null) {
            throw new ParseException(
                "One of " + Arrays.toString(tokenTypeOrText) + " expected instead of end-of-input"
            );
        }

        for (int i = 0; i < tokenTypeOrText.length; i++) {
            Object ttot = tokenTypeOrText[i];
            if (c.type.equals(ttot) || c.text.equals(ttot)) {
                this.current = null;
                return i;
            }
        }
        throw new ParseException("One of " + Arrays.toString(tokenTypeOrText) + " expected instead of '" + c + "'");
    }

    /**
     * @throws ParseException Iff the scanner is <i>not</i> at end-of-input
     */
    public void
    eoi() throws ParseException {
        Token<TT> t = this.peek();
        if (t != null) throw new ParseException("Expected end-of-input instead of '" + t + "'");
    }

    @Nullable private Token<TT>
    produceToken() throws ParseException {
        try {
            return this.scanner.produce();
        } catch (ScanException se) {
            throw new ParseException(se);
        }
    }
}
