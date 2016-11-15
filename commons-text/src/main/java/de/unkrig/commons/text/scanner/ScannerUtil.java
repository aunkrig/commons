
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;

/**
 * Various scanner-related utility methods.
 */
public final
class ScannerUtil {

    private ScannerUtil() {}

    /**
     * Converts the {@link StringScanner}, which reads from a {@link CharSequence} into a {@link DocumentScanner},
     * which reads from a {@link Reader} and thinks in "line numbers" and "column numbers").
     *
     * <p>{@link IOException}s thrown by the {@link Reader} are wrapped in {@link ScanException}s.
     */
    public static <TT extends Enum<TT>> DocumentScanner<TT>
    toDocumentScanner(final StringScanner<TT> stringScanner, final Reader reader) {

        final ProducerWhichThrows<? extends String, ? extends IOException>
        lineProducer = LineUtil.readLineWithSeparator(reader);

        return new DocumentScanner<TT>() {

            private int lineNumber;

            @Override @Nullable public Token<TT>
            produce() throws ScanException {
                for (;;) {
                    Token<TT> token = stringScanner.produce();
                    if (token != null) return token;
                    String line;
                    try {
                        line = lineProducer.produce();
                    } catch (IOException ioe) {
                        throw new ScanException(ioe);
                    }
                    if (line == null) return null;
                    this.lineNumber++;
                    stringScanner.setInput(line);
                }
            }

            @Override public int
            getPreviousTokenLineNumber() {
                return this.lineNumber;
            }

            @Override public int
            getPreviousTokenColumnNumber() {
                return stringScanner.getPreviousTokenOffset() + 1;
            }

            @Override public String
            toString() {
                return "Line " + this.getPreviousTokenLineNumber() + ", column " + this.getPreviousTokenColumnNumber();
            }
        };
    }

    /**
     * Uses a given {@link StringScanner} to scan the contents of a given file.
     *
     * @param charset E.g. {@link Charset#forName(String)} or {@link Charset#defaultCharset()}
     * @see           #toDocumentScanner(StringScanner, Reader)
     */
    public static <TT extends Enum<TT>> ProducerWhichThrows<Token<TT>, ScanException>
    scanner(final StringScanner<TT> stringScanner, File file, Charset charset) throws FileNotFoundException {

        return ScannerUtil.augmentScanningLocation(ScannerUtil.toDocumentScanner(
            stringScanner,
            new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))
        ), file.toString());
    }

    /**
     * @return A {@link Producer} who's {@link #toString()} method prepends the given {@code prefix}, a colon and a
     *         space to the string returned by the {@code delegate}'s {@link #toString()} method
     */
    public static <T, EX extends Throwable> ProducerWhichThrows<T, EX>
    augmentScanningLocation(
        final ProducerWhichThrows<? extends T, ? extends EX> delegate,
        @Nullable final String                               prefix
    ) {

        return new ProducerWhichThrows<T, EX>() {

            @Override @Nullable public T
            produce() throws EX { return delegate.produce(); }

            @Override @Nullable public String
            toString() {
                return prefix == null ? delegate.toString() : prefix + ": " + delegate.toString();
            }
        };
    }

    /**
     * @return A {@link StringScanner} which produces tokens through a {@code delegate}, but only those for which
     *         the {@code predicate} returns {@code true}
     */
    public static <TT extends Enum<TT>> StringScanner<TT>
    filter(final StringScanner<TT> delegate, final Predicate<? super Token<TT>> predicate) {

        return new StringScanner<TT>() {

            @Override @Nullable public Token<TT>
            produce() throws ScanException {
                for (;;) {
                    Token<TT> token = delegate.produce();
                    if (token == null) return null;
                    if (predicate.evaluate(token)) return token;
                }
            }

            @Override public StringScanner<TT>
            setInput(CharSequence cs) {
                delegate.setInput(cs);
                return this;
            }

            @Override public int
            getOffset() {
                return delegate.getOffset();
            }

            @Override public int
            getPreviousTokenOffset() {
                return delegate.getPreviousTokenOffset();
            }

            @Override @Nullable public String
            toString() { return delegate.toString(); }
        };

    }

    /**
     * If set, {@code "\023"} escapes to {@code '\0', '2', '3'}, otherwise, if {@value #UNESCAPE_OCTAL}, it escapes to
     * {@code '#'}, otherwise, it is an invalid escape sequence.
     *
     * @see #unescape(String, int)
     */
    public static final int UNESCAPE_NUL = 1;

    /**
     * If set, {@code \"} escapes to a double quote, otherwise it is an invalid escape sequence.
     *
     * @see #unescape(String, int)
     */
    public static final int UNESCAPE_DOUBLE_QUOTE = 2;

    /**
     * If set, {@code \'} escapes to a single quote, otherwise it is an invalid escape sequence.
     *
     * @see #unescape(String, int)
     */
    public static final int UNESCAPE_SINGLE_QUOTE = 4;

    /**
     * If set, <code>"&#92;u<i>xxxx</i>"</code> escapes to the unicode character '<i>xxxx</i>', otherwise it is an
     * invalid escape sequence.
     *
     * @see #unescape(String, int)
     */
    public static final int UNESCAPE_UNICODE = 8;

    /**
     * If set, {@code "\123"} escapes to "S", otherwise it is an invalid escape sequence (but see {@link
     * #UNESCAPE_NUL}).
     *
     * @see #unescape(String, int)
     */
    public static final int UNESCAPE_OCTAL = 16;

    /**
     * @return               The input string, unescaped according to the rules defined in JLS7
     * @throws ScanException {@code s} contains control characters
     * @throws ScanException <code>\&#92;</code> is not followed by exactly four hex digits
     * @throws ScanException A backslash is not followed by an allowed character
     * @throws ScanException {@code s} ends in the middle of an escape sequence
     * @see #UNESCAPE_NUL
     * @see #UNESCAPE_DOUBLE_QUOTE
     * @see #UNESCAPE_SINGLE_QUOTE
     * @see #UNESCAPE_UNICODE
     * @see #UNESCAPE_OCTAL
     */
    public static String
    unescape(String s, int options) throws ScanException {
        if (s.indexOf('\\') == -1) return s;

        StringBuilder sb = new StringBuilder(s.length() + 3);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isISOControl(c)) throw new ScanException("Character " + (int) c + " not allowed in string");

            if (c != '\\') {
                sb.append(c);
                continue;
            }

            try {
                c = s.charAt(++i);

                int idx;

                if (c == '0' && (options & ScannerUtil.UNESCAPE_NUL) != 0) {
                    sb.append('\0');
                } else

                if (c == '"' && (options & ScannerUtil.UNESCAPE_DOUBLE_QUOTE) != 0) {
                    sb.append('"');
                } else

                if (c == '\'' && (options & ScannerUtil.UNESCAPE_SINGLE_QUOTE) != 0) {
                    sb.append('\'');
                } else

                if ((idx = "\\/bfnrt".indexOf(c)) != -1) {
                    sb.append("\\/\b\f\n\r\t".charAt(idx));
                } else

                if (c == 'u' && (options & ScannerUtil.UNESCAPE_UNICODE) != 0) {
                    int h = 0;
                    for (int j = 0; j < 4; j++) {
                        c = s.charAt(++i);
                        int nibble = Character.digit(c, 16);
                        if (nibble == -1) throw new ScanException("'" + c + "' is not a hex digit");
                        h = (h << 4) + nibble;
                    }
                    sb.append((char) h);
                } else

                if (c >= '0' && c <= '7' && (options & ScannerUtil.UNESCAPE_OCTAL) != 0) {
                    int o = Character.digit(c, 8);
                    if (i < s.length() - 1) {
                        int octel = Character.digit(s.charAt(i + 1), 8);
                        if (octel != -1) {
                            o = (o << 3) + octel;
                            i++;
                            if (o <= 31 && i < s.length() - 1) {
                                octel = Character.digit(s.charAt(i + 1), 8);
                                if (octel != -1) {
                                    o = (o << 3) + octel;
                                    i++;
                                }
                            }
                        }
                    }
                    sb.append((char) o);
                } else

                {
                    throw new ScanException("Invalid character '" + c + "' after backslash");
                }

            } catch (IndexOutOfBoundsException ioobe) {
                throw new ScanException("Truncated escape sequence"); // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
        }

        return sb.toString();
    }
}
