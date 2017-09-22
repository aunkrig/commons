
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

package de.unkrig.commons.text.expression;

import static de.unkrig.commons.text.expression.Scanner.TokenType.CHARACTER_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.C_COMMENT;
import static de.unkrig.commons.text.expression.Scanner.TokenType.END_OF_IGNORABLES;
import static de.unkrig.commons.text.expression.Scanner.TokenType.FLOATING_POINT_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.IDENTIFIER;
import static de.unkrig.commons.text.expression.Scanner.TokenType.INTEGER_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.INVALID_CHARACTER;
import static de.unkrig.commons.text.expression.Scanner.TokenType.KEYWORD;
import static de.unkrig.commons.text.expression.Scanner.TokenType.OPERATOR;
import static de.unkrig.commons.text.expression.Scanner.TokenType.SPACE;
import static de.unkrig.commons.text.expression.Scanner.TokenType.STRING_LITERAL;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.ScannerUtil;
import de.unkrig.commons.text.scanner.StatelessScanner;
import de.unkrig.commons.text.scanner.StringScanner;

/**
 * The scanner for the {@link ExpressionEvaluator}.
 */
public final
class Scanner {

    private Scanner() {}

    /**
     * Token types for the {@link ExpressionEvaluator} scanner.
     */
    public
    enum TokenType {

        // SUPPRESS CHECKSTYLE JavadocVariable:11
        SPACE, C_COMMENT,

        // Dummy enum constant which separates ignorable (above) from non-ignorable token types (below).
        END_OF_IGNORABLES,

        KEYWORD,
        IDENTIFIER,
        OPERATOR,
        CHARACTER_LITERAL, STRING_LITERAL, INTEGER_LITERAL, FLOATING_POINT_LITERAL,

        INVALID_CHARACTER,
    }

    /**
     * @return A {@link StringScanner} for the given {@link TokenType}
     */
    public static StringScanner<TokenType>
    stringScanner() {
        final StatelessScanner<TokenType> scanner = new StatelessScanner<TokenType>();

        scanner.addRule("\\s+", SPACE);
        scanner.addRule("/\\*.*?\\*/", C_COMMENT);

        scanner.addRule(
            "(?:true|false|null|instanceof|new|boolean|byte|short|int|long|float|double|char)(?![\\p{L}\\p{Nd}_$])",
            KEYWORD
        );

        scanner.addRule(
            "[\\p{L}\\p{Sc}\\p{Pc}\\p{Nl}][\\p{L}\\p{Sc}\\p{Pc}\\p{Nl}\\p{Nd}\\p{Mn}\\p{Mc}]*",
            IDENTIFIER
        );

        scanner.addRule((
            "<<|>>>|>>|"                                    // << >>> >>
            + "&&|\\?|:|==|=\\*|=~|!=|<=|<|>=|\\|\\|"       // && ? : == =* =~ != <= < >= ||
            + "|>|!|\\+|-|\\*|/|%|\\(|\\)|\\.|,|\\[|\\]|~|" // > ! + - * / % ( ) . , [ ] ~
            + "&|\\||\\^"                                   // & | ^
        ), OPERATOR);

        scanner.addRule("\\d+\\.\\d*(?:[eE][+\\-]?\\d+)?[fFdD]?", FLOATING_POINT_LITERAL);  // 9.
        scanner.addRule("\\.\\d+(?:[eE][+\\-]?\\d+)?[fFdD]?",     FLOATING_POINT_LITERAL);  // .9
        scanner.addRule("\\d+[eE][+\\-]?\\d+[fFdD]?",             FLOATING_POINT_LITERAL);  // 9e1
        scanner.addRule("\\d+([eE][+\\-]?\\d+)?[fFdD]",           FLOATING_POINT_LITERAL);  // 9f
        scanner.addRule("0[Xx](?:[0-9a-fA-F]+)(L|l)?",                  INTEGER_LITERAL);
        scanner.addRule("(?:0|[1-9]\\d*|0x\\p{XDigit}+|0[0-7]+)(L|l)?", INTEGER_LITERAL);
        scanner.addRule(
            "'(?:\\\\[btnfr\"'\\\\]|\\\\[0-3][0-7][0-7]|\\\\[0-7][0-7]|\\\\[0-7]|[^\\\\'])'",
            CHARACTER_LITERAL
        );
        scanner.addRule(
            "\"(?:\\\\[btnfr\"'\\\\]|\\\\[0-3][0-7][0-7]|\\\\[0-7][0-7]|\\\\[0-7]|[^\\\\\"])*\"",
            STRING_LITERAL
        );
        scanner.addRule(
            "'(?:\\\\[btnfr\"'\\\\]|\\\\[0-3][0-7][0-7]|\\\\[0-7][0-7]|\\\\[0-7]|[^\\\\'])*'",
            STRING_LITERAL
        );

        // In order to avoid ScanExceptions, which would break "ExpressionEvaluator.parsePart()":
        scanner.addRule(".", INVALID_CHARACTER);

        return ScannerUtil.filter(scanner, new Predicate<Token<TokenType>>() {

            @Override public boolean
            evaluate(@Nullable Token<TokenType> token) {
                return token == null || token.type.ordinal() > END_OF_IGNORABLES.ordinal();
            }
        });
    }

    /**
     * @return E.g. a single quote if the {@code text} is single quote, backslash, single quote, single quote
     */
    public static Character
    decodeCharacterLiteral(String text) throws ScanException {
        return Character.valueOf(Scanner.unescape(text, new int[] { 1 }));
    }

    /**
     * @return A {@link Double} or a {@link Float}
     */
    public static Object
    decodeFloatingPointLiteral(String text) {
        int  len1  = text.length() - 1;
        char lc    = text.charAt(len1);
        return (
            lc == 'd' || lc == 'D' ? Double.parseDouble(text.substring(0, len1)) :
            lc == 'f' || lc == 'F' ? Float.parseFloat(text.substring(0, len1)) :
            Double.parseDouble(text)
        );
    }

    /**
     * @return A {@link Long} or an {@link Integer}
     */
    public static Object
    decodeIntegerLiteral(String text) throws ScanException {

        boolean isNegative, isHex, isLong;
        int     from, to;
        {
            if (text.charAt(0) == '-') {
                isNegative = true;
                from       = 1;
            } else {
                isNegative = false;
                from       = 0;
            }
            if (from + 3 < text.length() && text.charAt(from) == '0' && (text.charAt(from + 1) | 0x20) == 'x') {
                isHex = true;
                from  += 2;
            } else {
                isHex = false;
            }

            to = text.length();
            if ((text.charAt(to - 1) | 0x20) == 'l') {
                to--;
                isLong = true;
            } else {
                isLong = false;
            }
        }

        if (isHex) {

            // 'String.decode()' and consorts only accept values from -0x80000000 to +0x7fffffff, but hex literals
            // range from -0xffffffff to +0xffffffff.
            int i = from;
            for (; i < to && text.charAt(i) == '0'; i++);
            if (isLong) {
                if (to > i + 16) throw new ScanException("Integer literal '" + text + "' out of range");
                long result = 0;
                for (; i < to; i++) {
                    result = 16L * result + Character.digit(text.charAt(i), 16);
                }
                return isNegative ? -result : result;
            } else {
                if (to > i + 8) throw new ScanException("Integer literal '" + text + "' out of range");
                int result = 0;
                for (; i < to; i++) {
                    result = 16 * result + Character.digit(text.charAt(i), 16);
                }
                return isNegative ? -result : result;
            }
        } else {
            try {
                int radix = text.charAt(0) == '=' ? 8 : 10;
                return (
                    isLong
                    ? (Object) Long.valueOf(text.substring(0, to), radix)
                    : (Object) Integer.valueOf(text, radix)
                );
            } catch (NumberFormatException nfe) {
                // SUPPRESS CHECKSTYLE AvoidHidingCause
                throw new ScanException("Integer literal '" + text + "' out of range");
            }
        }
    }

    /**
     * Removes the double quotes and the escape sequences from the given string literal.
     */
    public static String
    decodeStringLiteral(String text) throws ScanException {
        int           len1 = text.length() - 1;
        StringBuilder sb   = new StringBuilder();
        for (int[] off = { 1 }; off[0] < len1;) sb.append(Scanner.unescape(text, off));
        return sb.toString();
    }

    /**
     * Unescapes the character at the given {@code offset} in the {@code text}. On return, the {@code offset} points
     * to the next character in the {@code text}.
     */
    private static char
    unescape(String text, int[] offset) throws ScanException {
        char c = text.charAt(offset[0]++);

        if (c != '\\') return c; // Unescaped character.

        c = text.charAt(offset[0]++);

        {
            int idx = "btnfr\"'\\".indexOf(c);
            if (idx != -1) return "\b\t\n\f\r\"'\\".charAt(idx);
        }

        // '\0'?
        if (c == '0') return '\0';

        // Octal escape sequence?
        if (c >= '1' && c <= '7') {
            char result = (char) (c - '0'); // SUPPRESS CHECKSTYLE UsageDistance

            // One-digit octal escape sequence (\0...\7)?
            c = text.charAt(offset[0]);
            if (c < '0' || c > '7') return result;

            // Two-digit octal escape sequence (\40...\77)?
            offset[0]++;
            result = (char) (8 * result + (c - '0'));
            if (result >= 32) return result;

            // Two-digit octal escape sequence (\00...\37)?
            c = text.charAt(offset[0]);
            if (c < '0' || c > '7') return result;

            // Three-digit octal escape sequence (\000...\377).
            offset[0]++;
            return (char) (8 * result + (c - '0'));
        }

        throw new ScanException("Invalid escape sequence '\\" + c + "'");
    }
}
