
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.net.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.PredicateWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7235">RFC 7235</a>
 * @see #parse(String)
 */
public final
class AuthenticateHeaderParser {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private AuthenticateHeaderParser() {}

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-2.1">Section 2.1 of RFC 7235</a>
     */
    public static
    class Challenge {
        public final String authScheme;
        private Challenge(String authScheme) {
            this.authScheme = authScheme;
        }
    }

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-2.1">Section 2.1 of RFC 7235</a>
     */
    public static
    class Token68Challenge extends Challenge {
        public final String token68;
        private Token68Challenge(String authScheme, String token68) {
            super(authScheme);
            this.token68 = token68;
        }
    }

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-2.1">Section 2.1 of RFC 7235</a>
     */
    public static
    class ChallengeWithAuthParams extends Challenge {
        public final List<AuthParam> authParams;
        private ChallengeWithAuthParams(String authScheme, List<AuthParam> authParams) {
            super(authScheme);
            this.authParams = authParams;
        }
    }

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-2.1">Section 2.1 of RFC 7235</a>
     */
    public static
    class AuthParam {
        public final String name, value;
        private AuthParam(String name, String value) { this.name = name; this.value = value; }
    }

    /**
     * Parses the content of an {@code Authenticate} or {@code Proxy-Authenticate} HTTP Header.
     */
    public static List<Challenge>
    parse(String input) throws ParseException {

        StatefulScanner<TokenType, State> ss = new StatefulScanner<TokenType, State>(CHALLENGE_SCANNER);
        ss.setInput(input);

        AbstractParser<TokenType>
        p = new AbstractParser<TokenType>(
            ProducerUtil.filter(ss, new PredicateWhichThrows<Token<TokenType>, ScanException>() {

                @Override public boolean
                evaluate(Token<TokenType> subject) { return subject.type != TokenType.SPACE; }
            })
        );

        List<Challenge> result = new ArrayList<Challenge>();
        for (;;) {

            String authScheme = p.read(TokenType.TOKEN);

            if (p.peek() == null) {
                result.add(new Challenge(authScheme));
                return result;
            }

            String token68 = p.peekRead(TokenType.TOKEN68);
            if (token68 != null) {
                result.add(new Token68Challenge(authScheme, token68));
                if (p.peek() == null) return result;
                p.read(TokenType.COMMA);
            } else
            if (p.peekRead(",")) {
                result.add(new Challenge(authScheme));
                continue;
            } else
            {

                List<AuthParam> aps = new ArrayList<AuthParam>();
                for (;;) {
                    Token<TokenType> t = p.read();

                    if (t.type != TokenType.AUTH_PARAM) throw new ParseException(TokenType.AUTH_PARAM + " expected instead of " + t.type);

                    String[] captured = t.captured;
                    assert captured != null;

                    String name = captured[0], value;
                    if (captured[1] != null && captured[2] == null) {
                        value = captured[1];
                    } else
                    if (captured[1] == null && captured[2] != null) {
                        value = captured[2].replaceAll("\\\\(.)", "$1");
                    } else
                    {
                        throw new AssertionError(Arrays.toString(captured));
                    }
                    aps.add(new AuthParam(name, value));

                    if (p.peek() == null) {
                        result.add(new ChallengeWithAuthParams(authScheme, aps));
                        return result;
                    }

                    p.read(TokenType.COMMA);

                    if (p.peek(TokenType.AUTH_PARAM) == null) break;
                }
            }
        }
    }

    enum TokenType { TOKEN, TOKEN68, SPACE, AUTH_PARAM, COMMA }
    enum State { Challenge1, Challenge2 }
    private static final StatefulScanner<TokenType, State> CHALLENGE_SCANNER = new StatefulScanner<TokenType, State>(State.class);
    static {
        CHALLENGE_SCANNER.addRule(CHALLENGE_SCANNER.ANY_STATE, "\\s+", TokenType.SPACE, CHALLENGE_SCANNER.REMAIN);

        String token = "[A-Za-z0-9!#$%&'*+\\-.^_`|~]+";

        // token
        CHALLENGE_SCANNER.addRule(token, TokenType.TOKEN, State.Challenge1);

        // token68  Use negative lookahead to handle the similarity with "token '=' ( token | quoted-string )".
        CHALLENGE_SCANNER.addRule(State.Challenge1, "[A-Za-z0-9\\-._~+/]+=*+(?!\\s*[A-Za-z0-9!#$%&'*+\\-.^_`|~\"])", TokenType.TOKEN68);

        // auth-param
        CHALLENGE_SCANNER.addRule(CHALLENGE_SCANNER.ANY_STATE, "(" + token + ")\\s*+=\\s*+(?:(" + token + ")|\"((?:[^\"\\\\]|\\\\.)*)\")", TokenType.AUTH_PARAM);

        // comma
        CHALLENGE_SCANNER.addRule(CHALLENGE_SCANNER.ANY_STATE, ",", TokenType.COMMA);
    }
}

