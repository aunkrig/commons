
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

package de.unkrig.commons.lang;

import de.unkrig.commons.lang.protocol.Predicate;

/**
 * Extensions for {@link Character}.
 */
public final
class Characters {

    private Characters() {}

    public static final Predicate<Character>
    POSIX_LOWER = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c >= 'a' && c <= 'z';
        }
    };

    public static final Predicate<Character>
    POSIX_UPPER = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c >= 'A' && c <= 'Z';
        }
    };

    public static final Predicate<Character>
    POSIX_ASCII = new Predicate<Character>() {
        @Override public boolean evaluate(Character subject) { return subject <= 0x7f; }
    };

    public static final Predicate<Character>
    POSIX_ALPHA = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }
    };

    public static final Predicate<Character>
    POSIX_DIGIT = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c >= '0' && c <= '9';
        }
    };

    public static final Predicate<Character>
    POSIX_ALNUM = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
        }
    };

    public static final Predicate<Character>
    POSIX_PUNCT = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return (
                (c >= '!' && c <= '/')    // !"#$%&'()*+,-./
                || (c >= ':' && c <= '@') // :;<=>?@
                || (c >= '[' && c <= '`') // [\]^_`
                || (c >= '{' && c <= '~') // {|}~
            );
        }
    };

    public static final Predicate<Character>
    POSIX_GRAPH = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) { return Characters.POSIX_ALNUM.evaluate(subject) || Characters.POSIX_PUNCT.evaluate(subject); }
    };

    public static final Predicate<Character>
    POSIX_PRINT = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            return subject == ' ' || Characters.POSIX_ALNUM.evaluate(subject) || Characters.POSIX_PUNCT.evaluate(subject);
        }
    };

    public static final Predicate<Character>
    POSIX_BLANK = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c == ' ' || c == '\t';
        }
    };

    public static final Predicate<Character>
    POSIX_CNTRL = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c <= 0x1f || c == 0x7f;
        }
    };

    public static final Predicate<Character>
    POSIX_XDIGIT = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        }
    };

    public static final Predicate<Character>
    POSIX_SPACE = new Predicate<Character>() {

        @Override public boolean
        evaluate(Character subject) {
            char c = subject;
            return c >= 'a' && c <= 'z';
        }
    };
}
