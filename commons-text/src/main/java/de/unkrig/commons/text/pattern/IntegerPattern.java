
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

package de.unkrig.commons.text.pattern;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.StatelessScanner;

/**
 * Matches integer subjects against a pattern.
 *
 * @see #newPattern(CharSequence)
 */
public abstract
class IntegerPattern {

    private IntegerPattern() {}

    private enum TokenType { INTEGER, OPERATOR }

    /**
     * Parses a pattern like "-3,7-10,12-40,50-".
     */
    public static Predicate<Integer>
    newPattern(CharSequence pattern) throws ParseException {

        final StatelessScanner<TokenType> scanner2 = new StatelessScanner<TokenType>();
        scanner2.addRule("\\d+", TokenType.INTEGER);
        scanner2.addRule("[,\\-]", TokenType.OPERATOR);
        scanner2.setInput(pattern);

        // SUPPRESS CHECKSTYLE TypeJavadoc
        class Parser extends AbstractParser<TokenType> {
            Parser() { super(scanner2); }

            /**
             * <pre>
             *   alternatives :=
             *     range [ ',' alternatives ]
             * </pre>
             */
            Predicate<Integer>
            parseAlternatives() throws ParseException {

                Predicate<Integer> lhs = this.parseRange();

                return this.peekRead(",") ? PredicateUtil.or(lhs, this.parseAlternatives()) : lhs;
            }

            /**
             * <pre>
             *   range :=
             *     '-' int
             *     | int [ '-' int ]
             * </pre>
             */
            private Predicate<Integer>
            parseRange() throws ParseException {

                if (this.peekRead("-")) return PredicateUtil.between(Integer.MIN_VALUE, this.readInt());

                return PredicateUtil.between(
                    this.readInt(),
                    this.peekRead("-") ? this.readInt() : Integer.MAX_VALUE
                );
            }

            /**
             * <pre>
             *   int :=
             *     INTEGER
             * </pre>
             */
            private int
            readInt() throws ParseException {
                return Integer.parseInt(this.read(TokenType.INTEGER));
            }
        }

        Parser parser = new Parser();

        final Predicate<Integer> result = parser.parseAlternatives();
        parser.eoi();

        return result;
    }
}
