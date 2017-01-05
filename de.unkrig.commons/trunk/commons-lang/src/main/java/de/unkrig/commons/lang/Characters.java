
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
 * Extensions for the JRE's {@link Character} class.
 */
public final
class Characters {

    private Characters() {}

    /**
     * Evaluates whether a given code point lies in the POSIX character class "lower" ({@code [a-z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_LOWER = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return c >= 'a' && c <= 'z';
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "upper" ({@code [A-Z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_UPPER = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return c >= 'A' && c <= 'Z';
        }
    };

    /**
     * Evaluates whether a given code point is in the ASCII range (0-127).
     */
    public static final Predicate<Integer>
    IS_POSIX_ASCII = new Predicate<Integer>() {
        @Override public boolean evaluate(Integer subject) { return subject <= 0x7f; }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "alpha" ({@code [A-Za-z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_ALPHA = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "digit" ({@code [0-9]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_DIGIT = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return c >= '0' && c <= '9';
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "alnum" ({@code [A-Za-z0-9]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_ALNUM = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "punct" (one of <code>! " # $ % &amp;
     * ' ( ) * + , - . / : ; &lt; = > ? @ [ \ ] ^ _ ` { | } ~</code>).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_PUNCT = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return (
                (c >= '!' && c <= '/')    // !"#$%&'()*+,-./ (33...47)
                || (c >= ':' && c <= '@') // :;<=>?@         (58...64)
                || (c >= '[' && c <= '`') // [\]^_`          (91...96)
                || (c >= '{' && c <= '~') // {|}~            (123...126)
            );
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "graph"; the union of classes "alpha",
     * "digit", and "punct".
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_GRAPH = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            return Characters.IS_POSIX_ALNUM.evaluate(subject) || Characters.IS_POSIX_PUNCT.evaluate(subject);
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "print"; the union of classes "alpha",
     * "digit" and "punct", and the SPACE character.
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_PRINT = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            return (
                subject == ' '
                || Characters.IS_POSIX_ALNUM.evaluate(subject)
                || Characters.IS_POSIX_PUNCT.evaluate(subject)
            );
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "blank"; which consists of the SPACE
     * character and the TAB character.
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_BLANK = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return c == ' ' || c == '\t';
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "cntrl" ({@code [\0-\x1f\x7f]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_CNTRL = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return c <= 0x1f || c == 0x7f;
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "xdigit" ({@code [0-9a-fA-F]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_XDIGIT = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "space" (consisting of the tab, newline,
     * vertical-tab, form-feed, carriage-return and space characters).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_SPACE = new Predicate<Integer>() {

        @Override public boolean
        evaluate(Integer subject) {
            int c = subject;
            return (
                (c >= 9 && c <= 13) // 0x09=tab, 0x0a=newline, 0x0b=vertical-tab, 0x0c=form-feed, 0x0d=carriage-return
                || c == ' '         // 0x20=space
            );
        }
    };

    /** A predicate for {@link Character#isLowerCase(int)}. */
    public static final Predicate<Integer>
    IS_LOWER_CASE = new Predicate<Integer>() {
        @Override public boolean evaluate(Integer subject) { return Character.isLowerCase(subject); }
    };

    /** A predicate for {@link Character#isUpperCase(int)}. */
    public static final Predicate<Integer>
    IS_UPPER_CASE = new Predicate<Integer>() {
        @Override public boolean evaluate(Integer subject) { return Character.isUpperCase(subject); }
    };

    /** A predicate for {@link Character#isWhitespace(int)}. */
    public static final Predicate<Integer>
    IS_WHITESPACE = new Predicate<Integer>() {
        @Override public boolean evaluate(Integer subject) { return Character.isWhitespace(subject); }
    };

    /** A predicate for {@link Character#isMirrored(int)}. */
    public static final Predicate<Integer>
    IS_MIRRORED = new Predicate<Integer>() {
        @Override public boolean evaluate(Integer subject) { return Character.isMirrored(subject); }
    };
}
