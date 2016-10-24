
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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.text.json.Json.Value;
import de.unkrig.commons.text.json.JsonScanner.TokenType;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.ScannerUtil;

/**
 * Parser for a 'parametrized value' of an HTTP header like:
 * <pre>
 * Content-Type: text/plain; charset=ASCII
 * </pre>
 * 'text/plain' is the so-called 'token', 'char=ASCII' is a so-called 'parameter'.
 */
public
class JsonParser extends AbstractParser<TokenType> {

    public
    JsonParser(ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> scanner) { super(scanner); }

    public
    JsonParser(Reader r) { super(ScannerUtil.toDocumentScanner(JsonScanner.stringScanner(), r)); }

    /**
     * <pre>
     * object := '{' [ member { ',' member } ] '}'
     *
     * member := string ':' value
     * </pre>
     */
    public Json.ObjecT
    parseObject() throws IOException, ParseException {
        this.read("{");
        List<Json.Member> svps = new ArrayList<Json.Member>();
        if (!this.peekRead("}")) {
            for (;;) {
                this.read("\"");
                final Json.StrinG string = this.parseStringRest();
                this.read(":");
                Json.Value value = this.parseValue();
                svps.add(new Json.Member(string, value));
                if (this.peekRead("}")) break;
                this.read(",");
            }
        }
        return new Json.ObjecT(svps);
    }

    /**
     * <pre>
     * value :=
     *   string
     *   | number
     *   | object
     *   | array
     *   | 'true'
     *   | 'false'
     *   | 'null'
     * </pre>
     */
    public Json.Value
    parseValue() throws IOException, ParseException {

        if (this.peek("{")) return this.parseObject();

        if (this.peek("[")) return this.parseArray();

        Token<TokenType> t = this.read();
        switch (t.type) {

        case DOUBLE_QUOTE:
            return this.parseStringRest();

        case NUMBER:
            return new Json.NumbeR(t.text);

        case KEYWORD:
            if ("true".equals(t.text)) return new Json.True();
            if ("false".equals(t.text)) return new Json.False();
            if ("null".equals(t.text)) return new Json.Null();
            throw new ParseException("Invalid keyword '" + t.text + "'");

        default:
            throw new ParseException("Unexpected token '" + t.text + "'");
        }
    }

    private Json.StrinG
    parseStringRest() throws ParseException {
        StringBuilder sb = new StringBuilder();

        for (;;) {
            Token<TokenType> t    = this.read();
            String           text = t.text;
            switch (t.type) {

            case STRING_CHARS:
                sb.append(text);
                break;

            case STRING_ESCAPE:
                {
                    char c   = text.charAt(1);
                    int  idx = "bfnrt".indexOf(c);
                    if (idx != -1) c = "\b\f\n\r\t".charAt(idx);
                    sb.append(c);
                }
                break;

            case STRING_UNICODE_ESCAPE:
                sb.append((char) (
                    Character.digit(text.charAt(5), 16)
                    + (Character.digit(text.charAt(4), 16) << 4)
                    + (Character.digit(text.charAt(3), 16) << 8)
                    + (Character.digit(text.charAt(2), 16) << 12)
                ));
                break;

            case DOUBLE_QUOTE:
                return new Json.StrinG(sb.toString());

            default:
                throw new IllegalStateException();
            }
        }
    }

    /**
     * <pre>
     * array := '[' [ value { ',' value } ] ']'
     * </pre>
     */
    public Json.Value
    parseArray() throws IOException, ParseException {
        this.read("[");
        if (this.peekRead("]")) {
            List<Value> noValues = Collections.<Json.Value>emptyList();
            return new Json.Array(noValues);
        }
        List<Json.Value> l = new ArrayList<Json.Value>();
        for (;;) {
            Json.Value value = this.parseValue();
            l.add(value);
            if (this.peekRead("]")) break;
            this.read(",");
        }
        return new Json.Array(l);
    }
}
