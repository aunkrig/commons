
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.EnumSet;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;

/**
 * A scanner for the JAVA programming language.
 */
public final
class JavaScanner {

    private JavaScanner() {}

    // PUBLIC INTERFACE

    /**
     * Token types of the JAVA programming language.
     */
    public
    enum TokenType {

        /**
         * One or more characters that are "{@code [ \t\n\x0B\f\r]}".
         * <p>
         *   This token type is the only one that is not deterministic, e.g. two space could be scanned into <i>one</i>
         *   or into <i>two</i> space tokens.
         * </p>
         */
        SPACE,

        /**
         * Starts with {@code "//"} and always ends with a line break (unless it is the last token).
         */
        CXX_COMMENT,

        /**
         * Starts with {@code "/*"} and ends with <code>"&42;/"</code>.
         * <p>
         *   If the C comment spans multiple lines, the line breaks appear <b>exactly as in the input</b>, for
         *   example: <code>"/**\r * &#64;throws IOException\r\n &#42;/"</code>
         * </p>
         * <p>
         *   Notice that a Java scanner may represent multi-line C comments either as a {@link TokenType#C_COMMENT},
         *   token, or as a sequence of
         * </p>
         * <p>
         *   {@link #MULTI_LINE_C_COMMENT_BEGINNING}
         *   { {@link #MULTI_LINE_C_COMMENT_MIDDLE} }
         *   {@link #MULTI_LINE_C_COMMENT_END}
         * </p>
         */
        C_COMMENT,

        /**
         * Starts with {@code "/*"} and ends with a line break.
         * <p>
         *   The line break appear <b>exactly as in the input</b>, for
         *   example: {@code "/**\r"}
         * </p>
         * <p>
         *   Notice that a Java scanner may represent multi-line C comments either as a {@link TokenType#C_COMMENT},
         *   token, or as a sequence of
         * </p>
         * <p>
         *   {@link #MULTI_LINE_C_COMMENT_BEGINNING}
         *   { {@link #MULTI_LINE_C_COMMENT_MIDDLE} }
         *   {@link #MULTI_LINE_C_COMMENT_END}
         * </p>
         */
        MULTI_LINE_C_COMMENT_BEGINNING,

        /**
         * Ends with a line break.
         * <p>
         *   The line break appear <b>exactly as in the input</b>, for
         *   example: {@code " * &#64;throws IOException\n"}
         * </p>
         * <p>
         *   Notice that a Java scanner may represent multi-line C comments either as a {@link TokenType#C_COMMENT},
         *   token, or as a sequence of
         * </p>
         * <p>
         *   {@link #MULTI_LINE_C_COMMENT_BEGINNING}
         *   { {@link #MULTI_LINE_C_COMMENT_MIDDLE} }
         *   {@link #MULTI_LINE_C_COMMENT_END}
         * </p>
         */
        MULTI_LINE_C_COMMENT_MIDDLE,

        /**
         * Ends with <code>"&#42;/"</code>.
         * <p>
         *   Notice that a Java scanner may represent multi-line C comments either as a {@link TokenType#C_COMMENT},
         *   token, or as a sequence of
         * </p>
         * <p>
         *   {@link #MULTI_LINE_C_COMMENT_BEGINNING}
         *   { {@link #MULTI_LINE_C_COMMENT_MIDDLE} }
         *   {@link #MULTI_LINE_C_COMMENT_END}
         * </p>
         */
        MULTI_LINE_C_COMMENT_END,

        /**
         * One of the Java keywords ({@code "abstract"}, {@code "assert"} and so forth).
         */
        KEYWORD,

        /**
         * A Java identifier.
         */
        IDENTIFIER,

        /**
         * One of the Java "separators": {@code ( ) { } [ ] ; . ,}
         */
        SEPARATOR,

        /**
         * One of the Java "operators":
         * <pre>
         *   >>>=
         *   &lt;&lt;= >>= >>>
         *   += -= *= /= &amp;= |= ^= %=
         *   == &lt;= >= != &amp;&amp; || ++ -- &lt;&lt; >>
         *   = > &lt; ! ~ ? :
         *   + - * / &amp; | ^ %
         *   &#64;
         * </pre>
         */
        OPERATOR,

        /**
         * A Java string literal <b>as it appears in the input</b>, i.e. including the leading and the trailing double
         * quote and all escape sequences.
         */
        STRING_LITERAL,

        /**
         * A Java character literal <b>as it appears in the input</b>, i.e. including the leading and the trailing
         * single quote and any escape sequence.
         */
        CHARACTER_LITERAL,

        /**
         * A Java integer literal <b>as it appears in the input</b>, i.e. a decimal, hexadecimal or octal constant,
         * optionally followed by {@code 'l'} or {@code 'L'}.
         */
        INTEGER_LITERAL,

        /**
         * A Java floating point literal <b>as it appears in the input</b>, i.e. digits, decimal point and/or
         * exponent, optionally followed by {@code 'd'} or {@code 'D'}.
         */
        FLOATING_POINT_LITERAL,
    }

    /**
     * Returns a Java scanner that also produces SPACE and COMMENT tokens. Multi-line C-style comments are returned as
     * two or more tokens, as follows:
     * <pre>
     *   {@link TokenType#MULTI_LINE_C_COMMENT_BEGINNING MULTI_LINE_C_COMMENT_BEGINNING} { {@link
     *   TokenType#MULTI_LINE_C_COMMENT_MIDDLE MULTI_LINE_C_COMMENT_MIDDLE} } {@link TokenType#MULTI_LINE_C_COMMENT_END
     *   MULTI_LINE_C_COMMENT_END}
     * </pre>
     */
    public static StringScanner<TokenType>
    rawStringScanner() {
        StatefulScanner<TokenType, State> scanner = new StatefulScanner<TokenType, State>(State.class);

        // Recognize whitespace.
        scanner.addRule("\\s+", TokenType.SPACE);

        // Recognize C++-style comments.
        scanner.addRule(
            "//.*(?:\r\n|\r|\n)?", // Because '|' is not greedy, be sure to check CRLF BEFORE CR!
            TokenType.CXX_COMMENT
        );

        // Recognize single-line C-style comments.
        scanner.addRule("/\\*.*?\\*/", TokenType.C_COMMENT);

        // Recognize multi-line C-style comments.
        scanner.addRule(
            "(?s)/\\*.*",                             // regex
            TokenType.MULTI_LINE_C_COMMENT_BEGINNING, // tokenType
            State.IN_MULTI_LINE_C_COMMENT             // nextState
        );
        scanner.addRule(
            State.IN_MULTI_LINE_C_COMMENT,     // state
            ".*?\\*/",                         // regex
            TokenType.MULTI_LINE_C_COMMENT_END // tokenType
        );
        scanner.addRule(
            State.IN_MULTI_LINE_C_COMMENT,         // state
            "(?s).+",                              // regex
            TokenType.MULTI_LINE_C_COMMENT_MIDDLE, // tokenType
            State.IN_MULTI_LINE_C_COMMENT          // nextState
        );

        // Recognize Java keywords.
        scanner.addRule((
            "(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum"
            + "|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new"
            + "|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw"
            + "|throws|transient|try|void|volatile|while)(?![\\p{L}\\p{Nd}_$])"
        ), TokenType.KEYWORD);

        // Recognize Java identifiers.
        // See:
        //   http://en.wikipedia.org/wiki/Mapping_of_Unicode_characters#General_Category
        //   http://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#isJavaIdentifierStart%28char%29
        //   http://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#isJavaIdentifierPart%28char%29
        scanner.addRule((
            "[\\p{L}\\p{Nl}\\p{Sc}\\p{Pc}]"
            + "[\\p{L}\\p{Nl}\\p{Sc}\\p{Pc}\\p{Nd}\\p{Mn}\\p{Mc}\\x00-\\x08\\x0E-\\x1B\\x7F-\\x9F]*"
        ), TokenType.IDENTIFIER);

        // Recognize Java floating point literals.
        // Notice: Must be added BEFORE the rule for separator "." because of the ambiguity of ".9".
        scanner.addRule("\\d+\\.\\d*(?:[eE][+\\-]?\\d+)?[fFdD]?", TokenType.FLOATING_POINT_LITERAL);  // 9.
        scanner.addRule("\\.\\d+(?:[eE][+\\-]?\\d+)?[fFdD]?",     TokenType.FLOATING_POINT_LITERAL);  // .9
        scanner.addRule("\\d+[eE][+\\-]?\\d+[fFdD]?",             TokenType.FLOATING_POINT_LITERAL);  // 9e1
        scanner.addRule("\\d+([eE][+\\-]?\\d+)?[fFdD]",           TokenType.FLOATING_POINT_LITERAL);  // 9f

        // Recognize Java integer literals.
        scanner.addRule("(?:[1-9]\\d*|0x\\p{XDigit}+|0[0-7]*)(L|l)?", TokenType.INTEGER_LITERAL);

        // Recognize Java character literals.
        scanner.addRule(
            "'(?:\\\\[btnfr\"'\\\\]|\\\\u\\p{XDigit}{4}|\\\\[0-7]|\\\\[0-7][0-7]|\\\\[0-3][0-7][0-7]|[^'])'",
            TokenType.CHARACTER_LITERAL
        );

        // Recognize Java string literals.
        scanner.addRule(
            "\\\"(?:\\\\[btnfr\"'\\\\]|\\\\u\\p{XDigit}{4}|\\\\[0-7]|\\\\[0-7][0-7]|\\\\[0-3][0-7][0-7]|[^\"])*+\\\"",
            TokenType.STRING_LITERAL
        );

        // Recognize Java separators.
        scanner.addRule("\\(|\\)|\\{|\\}|\\[|]|;|\\.|,", TokenType.SEPARATOR); // ( ) { } [ ] ; . ,

        // Recognize Java operators.
        // Notice: Obviously '|' implements a first-match search (and not a greedy search, as one may expect).
        //         Thus longer operators need to appear before the shorter ones.
        scanner.addRule((
            ">>>="                                     // >>>=
            + "|<<=|>>=|>>>"                           // <<= >>= >>>
            + "|\\+=|-=|\\*=|/=|&=|\\|=|\\^=|%="       // += -= *= /= &= |= ^= %=
            + "|==|<=|>=|!=|&&|\\|\\||\\+\\+|--|<<|>>" // == <= >= != && || ++ -- << >>
            + "|=|>|<|!|~|\\?|:"                       // = > < ! ~ ? :
            + "|\\+|-|\\*|/|&|\\||\\^|%"               // + - * / & | ^ %
            + "|@"                                     // @
        ), TokenType.OPERATOR);

        return scanner;
    }

    /**
     * @return A {@link StringScanner} that swallows SPACE and COMMENT tokens
     */
    public static StringScanner<TokenType>
    stringScanner() {

        return ScannerUtil.filter(JavaScanner.rawStringScanner(), new Predicate<Token<TokenType>>() {

            @Override public boolean
            evaluate(@Nullable Token<TokenType> token) {
                return token == null || !JavaScanner.IGNORABLES.contains(token.type);
            }
        });
    }
    private static final EnumSet<TokenType> IGNORABLES = EnumSet.of(
        TokenType.C_COMMENT,
        TokenType.MULTI_LINE_C_COMMENT_BEGINNING,
        TokenType.MULTI_LINE_C_COMMENT_MIDDLE,
        TokenType.MULTI_LINE_C_COMMENT_END,
        TokenType.CXX_COMMENT,
        TokenType.SPACE
    );

    /**
     * Creates and returns a token producer that combines {@link TokenType#MULTI_LINE_C_COMMENT_BEGINNING},
     * {@link TokenType#MULTI_LINE_C_COMMENT_MIDDLE} and {@link TokenType#MULTI_LINE_C_COMMENT_END} tokens that the
     * <var>delegate</var> produces into a single {@link TokenType#C_COMMENT}.
     */
    public static ProducerWhichThrows<Token<TokenType>, ScanException>
    combineMultiLineCComments(final ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> delegate) {

        return new ProducerWhichThrows<Token<TokenType>, ScanException>() {

            @Override @Nullable public Token<TokenType>
            produce() throws ScanException {

                Token<TokenType> t = delegate.produce();
                if (t == null || t.type != TokenType.MULTI_LINE_C_COMMENT_BEGINNING) return t;

                final StringBuilder commentText = new StringBuilder(t.text);
                for (;;) {

                    t = delegate.produce();

                    if (t == null) throw new ScanException("Input ends in the middle of a multi-line C comment");

                    commentText.append(t.text);

                    if (t.type == TokenType.MULTI_LINE_C_COMMENT_END) {
                        return new Token<JavaScanner.TokenType>(TokenType.C_COMMENT, commentText.toString());
                    }

                    assert t.type == TokenType.MULTI_LINE_C_COMMENT_MIDDLE;
                }
            }
        };
    }

    /**
     * Creates and returns a token producer that merges sequences of two or more {@link TokenType#SPACE} tokens into
     * one.
     */
    public static ProducerWhichThrows<Token<TokenType>, ScanException>
    compressSpaces(final ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> delegate) {

        return new ProducerWhichThrows<Token<TokenType>, ScanException>() {

            @Nullable Token<TokenType> lookahead;

            @Override @Nullable public Token<TokenType>
            produce() throws ScanException {

                Token<TokenType> t;
                if (this.lookahead != null) {
                    t              = this.lookahead;
                    this.lookahead = null;
                } else {
                    t = delegate.produce();
                }
                if (t == null || t.type != TokenType.SPACE) return t;

                String s = t.text;
                for (;;) {

                    t = delegate.produce();

                    if (t == null) return new Token<TokenType>(TokenType.SPACE, s);

                    if (t.type != TokenType.SPACE) {
                        this.lookahead = t;
                        return new Token<TokenType>(TokenType.SPACE, s);
                    }

                    s += t.text;
                }
            }
        };
    }

    /**
     * A {@link FilterReader} that recognizes "unicode escapes" (backslash, 'u' and four hex digits), and decodes
     * them on-the-fly.
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.3">The Java Language
     *      Specification, Section 3.3, "Unicode Escapes"</a>
     */
    public static Reader
    unicodeEscapesDecodingReader(Reader delegate) {

        return new FilterReader(delegate) {

            int  state;
            char lookahead;

            @Override public int
            read() throws IOException {

                switch (this.state) {

                case 0:
                    int c = this.in.read();
                    if (c == -1) return -1;
                    if (c != '\\') return c;

                    // We just read a backslash - let's see what's next.

                    c = this.in.read();
                    if (c == -1) return '\\';

                    if (c != 'u') {
                        this.lookahead = (char) c;
                        this.state     = 1;
                        return '\\';
                    }

                    // We just read a backslash and a unicode marker ('u') - let's see what's next.
                    int d = Character.digit((char) this.in.read(), 16);
                    if (d >= 0) {
                        int codepoint = d;

                        d = Character.digit((char) this.in.read(), 16);
                        if (d >= 0) {
                            codepoint = (codepoint << 4) + d;

                            d = Character.digit((char) this.in.read(), 16);
                            if (d >= 0) {
                                codepoint = (codepoint << 4) + d;

                                d = Character.digit((char) this.in.read(), 16);
                                if (d >= 0) {
                                    return (char) (codepoint << 4) + d;
                                }
                            }
                        }
                    }
                    throw new IOException("Invalid unicode escape");

                case 1:
                    this.state = 0;
                    return this.lookahead;

                default:
                    throw new IllegalStateException(Integer.toString(this.state));
                }
            }

            @Override public int
            read(@Nullable char[] buf, int off, int len) throws IOException {
                assert buf != null;

                if (len == 0) return 0;

                int c = this.read();
                if (c == -1) return -1;

                buf[off] = (char) c;

                for (int i = 1; i < len; i++) {
                    c = this.read();
                    if (c == -1) return i;
                    buf[off + i] = (char) c;
                }

                return len;
            }

            @Override public long
            skip(long n) throws IOException {

                for (int i = 0; i < n; i++) {
                    if (this.read() == -1) return i;
                }

                return n;
            }

            @Override public boolean
            ready() throws IOException { return this.state == 1 || this.in.ready(); }

            @Override public boolean
            markSupported() { return false; }

            @Override
            public void
            mark(int readAheadLimit) { throw new UnsupportedOperationException("mark"); }

            @Override
            public void
            reset() { throw new UnsupportedOperationException("reset"); }

            @Override
            public void
            close() throws IOException {
                this.in.close();
                this.state = 0;
            }
        };
    }

    // IMPLEMENTATION

    private
    enum State {
        IN_MULTI_LINE_C_COMMENT,
    }
}
