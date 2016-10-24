
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

package de.unkrig.commons.text.json;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScannerUtil;
import de.unkrig.commons.text.scanner.StatefulScanner;
import de.unkrig.commons.text.scanner.StringScanner;

/**
 * A JASON scanner; scans tokens as defined on <a href="http://json.org/">json.org</a>.
 */
public final
class JsonScanner {

    private JsonScanner() {}

    /** Representation of the type of a token. */
    public
    enum TokenType {

        // CHECKSTYLE JavadocVariable:OFF
        SPACE,

        CXX_COMMENT,
        SINGLE_LINE_C_COMMENT,
        MULTI_LINE_C_COMMENT_BEGINNING, MULTI_LINE_C_COMMENT_MIDDLE, MULTI_LINE_C_COMMENT_END,

        END_OF_IGNORABLES,

        OPERATOR, NUMBER, KEYWORD,

        DOUBLE_QUOTE, STRING_UNICODE_ESCAPE, STRING_ESCAPE, STRING_CHARS,
        // CHECKSTYLE JavadocVariable:ON

    }

    private
    enum State { IN_MULTI_LINE_C_COMMENT, IN_STRING }

    /**
     * Returns a Java scanner that also produces SPACE and COMMENT tokens.
     */
    public static StringScanner<TokenType>
    rawStringScanner() {
        StatefulScanner<TokenType, State> scanner = new StatefulScanner<TokenType, State>(State.class);

        scanner.addRule("\\s+", TokenType.SPACE);

        scanner.addRule("//.*(?:\r|\r\n|\n)?", TokenType.CXX_COMMENT);
        scanner.addRule("/\\*.*?\\*/",         TokenType.SINGLE_LINE_C_COMMENT);

        // Multi-line C-style comments require special treatment, i.e. a special scanner state
        // 'IN_MULTI_LINE_C_COMMENT'.
        scanner.addRule("/\\*.*", TokenType.MULTI_LINE_C_COMMENT_BEGINNING, State.IN_MULTI_LINE_C_COMMENT);
        {
            scanner.addRule(State.IN_MULTI_LINE_C_COMMENT, ".*?\\*/", TokenType.MULTI_LINE_C_COMMENT_END);
            scanner.addRule(
                State.IN_MULTI_LINE_C_COMMENT,
                ".*",
                TokenType.MULTI_LINE_C_COMMENT_MIDDLE,
                State.IN_MULTI_LINE_C_COMMENT
            );
        }

        scanner.addRule("\\p{Alpha}+", TokenType.KEYWORD);

        scanner.addRule("-?[0-9.][0-9.Ee]*", TokenType.NUMBER);

        // Strings require special treatment, i.e. a special scanner state 'IN_STRING'.
        scanner.addRule("\"", TokenType.DOUBLE_QUOTE, State.IN_STRING);
        {
            scanner.addRule(
                State.IN_STRING,
                "\\\\u\\p{XDigit}\\p{XDigit}\\p{XDigit}\\p{XDigit}",
                TokenType.STRING_UNICODE_ESCAPE,
                State.IN_STRING
            );
            scanner.addRule(State.IN_STRING, "\\\\.",                TokenType.STRING_ESCAPE, State.IN_STRING);
            scanner.addRule(State.IN_STRING, "[^\\p{Cntrl}\"\\\\]+", TokenType.STRING_CHARS, State.IN_STRING);
            scanner.addRule(State.IN_STRING, "\"",                   TokenType.DOUBLE_QUOTE);
        }

        scanner.addRule("[\\{\\}\\[\\]:,]", TokenType.OPERATOR);

        return scanner;
    }

    /**
     * @return A scanner that swallows SPACE and COMMENT tokens
     */
    public static StringScanner<TokenType>
    stringScanner() {

        return ScannerUtil.filter(JsonScanner.rawStringScanner(), new Predicate<Token<TokenType>>() {

            @Override public boolean
            evaluate(@Nullable Token<TokenType> token) {
                return token == null || token.type.ordinal() > TokenType.END_OF_IGNORABLES.ordinal();
            }
        });
    }
}
