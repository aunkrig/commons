
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

package de.unkrig.commons.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An entity that allows REGEX-based analysis of the products of a delegate {@link ProducerWhichThrows
 * ProducerWhichThrows&lt;String>}.
 * <p>
 *   A "{@code null}" product is interpreted as the "end-of-input" condition.
 * </p>
 *
 * @param <EX> The exception type that the delegate {@link ProducerWhichThrows} may throw
 */
public
class StringStream<EX extends Throwable> {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final String NONE = new String("NONE");

    private final ProducerWhichThrows<? extends String, ? extends EX> producer;

    @Nullable private String  current = StringStream.NONE;
    @Nullable private Matcher matcher;
    @Nullable private String  unexpectedElementExceptionMessagePrefix;

    /**
     * Indication that a string is not as expected.
     */
    public static // SUPPRESS CHECKSTYLE CauseParameterInException
    class UnexpectedElementException extends Exception {

        private static final long serialVersionUID = 6447492090824323597L;

        public
        UnexpectedElementException(String message) { super(message); }
    }

    /** @see StringStream */
    public
    StringStream(ProducerWhichThrows<? extends String, ? extends EX> producer) { this.producer = producer; }

    /** @see StringStream */
    public
    StringStream(
        ProducerWhichThrows<? extends String, ? extends EX> producer,
        @Nullable String                                    unexpectedElementExceptionMessagePrefix
    ) {
        this.producer                                = producer;
        this.unexpectedElementExceptionMessagePrefix = unexpectedElementExceptionMessagePrefix;
    }

    /**
     * @return The next string, without consuming it
     */
    public boolean
    atEnd() throws EX { return this.next() == null; }

    /**
     * @return                            The next string, without consuming it
     * @throws UnexpectedElementException The next string was {@code null}
     */
    public String
    peek() throws EX, UnexpectedElementException {

        String result = this.next();

        if (result == null) throw new UnexpectedElementException(this.prefixMessage("Unexpected end-of-input"));

        return result;
    }

    /**
     * @return Whether the next string equals <var>expected</var>
     */
    public boolean
    peek(String expected) throws EX {

        return expected.equals(this.next());
    }

    /**
     * @return Whether the next string matches the <var>pattern</var>
     */
    public boolean
    peek(Pattern pattern) throws EX {

        String next = this.next();
        return next != null && (this.matcher = pattern.matcher(next)).matches();
    }

    /**
     * Consumes the next string iff it equals the <var>expected</var> string.
     *
     * @return Whether the next string equals the <var>expected</var> string
     */
    public boolean
    peekRead(@Nullable String expected) throws EX {

        if (!ObjectUtil.equals(this.next(), expected)) return false;

        this.consume();
        return true;
    }

    /**
     * Consumes the next string iff it matches the given <var>pattern</var>.
     *
     * @return Whether the next string matches the <var>pattern</var>
     * @see #group(int)
     */
    public boolean
    peekRead(@Nullable Pattern pattern) throws EX {

        String next = this.next();
        if (next == null) return pattern == null;
        if (pattern == null) return false;

        if (!(this.matcher = pattern.matcher(next)).matches()) return false;

        this.consume();
        return true;
    }

    /**
     * Consumes the next string iff it equals one of the <var>expected</var> strings.
     *
     * @return The index of the equl string, or -1
     */
    public int
    peekRead(String... expected) throws EX {
        String next = this.next();
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].equals(next)) {
                this.consume();
                return i;
            }
        }
        return -1;
    }

    /**
     * Verifies that the next string is not {@code null}, and consumes it.
     *
     * @throws UnexpectedElementException The next string is {@code null}
     */
    public String
    read() throws UnexpectedElementException, EX {

        String next = this.next();
        if (next == null) throw new UnexpectedElementException(this.prefixMessage("Unexpected end-of-input"));

        this.consume();

        return next;
    }

    /**
     * Verifies that the next string equals the given <var>expected</var> string, and consumes it.
     *
     * @throws UnexpectedElementException The next string does equal the <var>expected</var> string
     */
    public void
    read(String expected) throws UnexpectedElementException, EX {

        String next = this.next();
        if (next == null) throw new UnexpectedElementException(this.prefixMessage("Unexpected end-of-input"));

        if (!expected.equals(next)) {
            throw new UnexpectedElementException(
                this.prefixMessage("Expected '" + expected + "' instead of '" + next + "'")
            );
        }
        this.consume();
    }

    /**
     * Verifies that the next string matches the given <var>pattern</var>.
     *
     * @throws UnexpectedElementException The next string does match the <var>pattern</var>
     */
    public void
    read(Pattern pattern) throws EX, UnexpectedElementException {
        String next = this.next();
        if (next == null) throw new UnexpectedElementException(this.prefixMessage("Unexpected end-of-input"));

        Matcher m = (this.matcher = pattern.matcher(next));
        if (!m.matches()) {
            throw new UnexpectedElementException(
                this.prefixMessage("Expected '" + pattern + "' instead of '" + next + "'")
            );
        }

        this.consume();
    }

    /**
     * Consumes and returns all remaining elements.
     */
    public String[]
    readRest() throws EX {
        List<String> l = new ArrayList<String>();
        for (;;) {
            String next = this.next();
            if (next == null) break;
            l.add(next);
            this.consume();
        }
        return l.toArray(new String[l.size()]);
    }

    /**
     * @return                 The (possibly empty) subsequence captured by the group during the previous {@link
     *                         #read(Pattern)}, {@link #peek(Pattern)} or {@link #peekRead(Pattern)} operation, or
     *                         {@code null} if the group failed to match part of the input
     * @see Matcher#group(int)
     */
    @Nullable public String
    group(int group) {
        Matcher m = this.matcher;
        assert m != null;
        return m.group(group);
    }

    /** @return The next string on the stream, without consuming it */
    @Nullable private String
    next() throws EX {
        if (this.current == StringStream.NONE) this.current = this.producer.produce();
        return this.current;
    }

    private void
    consume() {
        assert this.current != StringStream.NONE;
        this.current = StringStream.NONE;
    }

    private String
    prefixMessage(String message) {
        return (
            this.unexpectedElementExceptionMessagePrefix == null
            ? message
            : this.unexpectedElementExceptionMessagePrefix + ": " + message
        );
    }
}
