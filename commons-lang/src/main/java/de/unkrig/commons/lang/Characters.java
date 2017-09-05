
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

import java.lang.Character.UnicodeBlock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.unkrig.commons.lang.OptionalMethods.MethodWrapper1;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Extensions for the JRE's {@link Character} class.
 */
public final
class Characters {

    private Characters() {}

    private abstract static
    class IntegerPredicate implements Predicate<Integer> {

        private final String toString;

        IntegerPredicate(String toString) { this.toString = toString; }

        @Override public String
        toString() { return this.toString; }
    }

    /**
     * Evaluates whether a given code point lies in the POSIX character class "lower" ({@code [a-z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_LOWER = Characters.rangePredicate("posixLower", 'a', 'z');

    /**
     * Evaluates whether a given code point lies in the POSIX character class "upper" ({@code [A-Z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_UPPER = Characters.rangePredicate("posixUpper", 'A', 'Z');

    /**
     * Evaluates whether a given code point is in the ASCII range (0-127).
     */
    public static final Predicate<Integer>
    IS_POSIX_ASCII = Characters.rangePredicate("posixAscii", 0, 0x7f);

    /**
     * Evaluates whether a given code point lies in the POSIX character class "alpha" ({@code [A-Za-z]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_ALPHA = new IntegerPredicate("posixAlpha") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z');
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "digit" ({@code [0-9]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_DIGIT = Characters.rangePredicate("posixDigit", '0', '9');

    /**
     * Evaluates whether a given code point lies in the POSIX character class "alnum" ({@code [A-Za-z0-9]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_ALNUM = new IntegerPredicate("posixAlnum") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') || (cp >= '0' && cp <= '9');
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
    IS_POSIX_PUNCT = new IntegerPredicate("posixPunct") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (
                (cp >= '!' && cp <= '/')    // !"#$%&'()*+,-./ (33...47)
                || (cp >= ':' && cp <= '@') // :;<=>?@         (58...64)
                || (cp >= '[' && cp <= '`') // [\]^_`          (91...96)
                || (cp >= '{' && cp <= '~') // {|}~            (123...126)
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
    IS_POSIX_GRAPH = new IntegerPredicate("posixGraph") {

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
    IS_POSIX_PRINT = new IntegerPredicate("posixPrint") {

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
    IS_POSIX_BLANK = new IntegerPredicate("posixBlank") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return cp == ' ' || cp == '\t';
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "cntrl" ({@code [\0-\x1f\x7f]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_CNTRL = new IntegerPredicate("posixCntrl") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return cp <= 0x1f || cp == 0x7f;
        }
    };

    /**
     * Evaluates whether a given code point lies in the POSIX character class "xdigit" ({@code [0-9a-fA-F]}).
     *
     * @see <a href="http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap07.html#tag_07_03_01">The Open
     *      Group Base Specifications Issue 7, section 7.3.1: LC_CTYPE</a>
     */
    public static final Predicate<Integer>
    IS_POSIX_XDIGIT = new IntegerPredicate("posixXdigit") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (cp >= '0' && cp <= '9') || (cp >= 'a' && cp <= 'f') || (cp >= 'A' && cp <= 'F');
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
    IS_POSIX_SPACE = new IntegerPredicate("posixSpace") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (
                (cp >= 9 && cp <= 13) // 0x09=tab, 0x0a=newline, 0x0b=vertical-tab, 0x0c=form-feed, 0x0d=carriage-return
                || cp == ' '         // 0x20=space
            );
        }
    };

    @Nullable public static Predicate<Integer>
    javaCharacterClassFromName(String name) {
        return (
            "javaAlphabetic".equals(name)             ? Characters.IS_UNICODE_ALPHA            :
            "javaIdeographic".equals(name)            ? Characters.IS_UNICODE_IDEOGRAPHIC      :
            "javaLetter".equals(name)                 ? Characters.IS_UNICODE_LETTER           :
            "javaLowerCase".equals(name)              ? Characters.IS_UNICODE_LOWER            :
            "javaUpperCase".equals(name)              ? Characters.IS_UNICODE_UPPER            :
            "javaTitleCase".equals(name)              ? Characters.IS_UNICODE_TITLE            :
            "javaWhitespace".equals(name)             ? Characters.IS_WHITESPACE               :
            "javaMirrored".equals(name)               ? Characters.IS_MIRRORED                 :
            "javaDigit".equals(name)                  ? Characters.IS_UNICODE_DIGIT            :
            "javaLetterOrDigit".equals(name)          ? Characters.IS_LETTER_OR_DIGIT          :
            "javaDefined".equals(name)                ? Characters.IS_DEFINED                  :
            "javaJavaIdentifierStart".equals(name)    ? Characters.IS_JAVA_IDENTIFIER_START    :
            "javaJavaIdentifierPart".equals(name)     ? Characters.IS_JAVA_IDENTIFIER_PART     :
            "javaIdentifierIgnorable".equals(name)    ? Characters.IS_IDENTIFIER_IGNORABLE     :
            "javaUnicodeIdentifierStart".equals(name) ? Characters.IS_UNICODE_IDENTIFIER_START :
            "javaUnicodeIdentifierPart".equals(name)  ? Characters.IS_UNICODE_IDENTIFIER_PART  :
            "javaSpaceChar".equals(name)              ? Characters.IS_SPACE_CHAR               :
            "javaISOControl".equals(name)             ? Characters.IS_ISO_CONTROL              :
            null
        );
    }

    /** A predicate for {@link Character#isISOControl(int)}. */
    public static final Predicate<Integer>
    IS_ISO_CONTROL = new IntegerPredicate("javaISOControl") {
        @Override public boolean evaluate(Integer subject) { return Character.isISOControl(subject); }
    };

    /** A predicate for {@link Character#isSpaceChar(int)}. */
    public static final Predicate<Integer>
    IS_SPACE_CHAR = new IntegerPredicate("javaSpaceChar") {
        @Override public boolean evaluate(Integer subject) { return Character.isSpaceChar(subject); }
    };

    /** A predicate for {@link Character#isDefined(int)}. */
    public static final Predicate<Integer>
    IS_DEFINED = new IntegerPredicate("javaDefined") {
        @Override public boolean evaluate(Integer subject) { return Character.isDefined(subject); }
    };

    /** A predicate for {@link Character#isJavaIdentifierStart(int)}. */
    public static final Predicate<Integer>
    IS_JAVA_IDENTIFIER_START = new IntegerPredicate("isJavaIdentifierStart") {
        @Override public boolean evaluate(Integer subject) { return Character.isJavaIdentifierStart(subject); }
    };

    /** A predicate for {@link Character#isJavaIdentifierPart(int)}. */
    public static final Predicate<Integer>
    IS_JAVA_IDENTIFIER_PART = new IntegerPredicate("isJavaIdentifierPart") {
        @Override public boolean evaluate(Integer subject) { return Character.isJavaIdentifierPart(subject); }
    };

    /** A predicate for {@link Character#isIdentifierIgnorable(int)}. */
    public static final Predicate<Integer>
    IS_IDENTIFIER_IGNORABLE = new IntegerPredicate("isIdentifierIgnorable") {
        @Override public boolean evaluate(Integer subject) { return Character.isIdentifierIgnorable(subject); }
    };

    /** A predicate for {@link Character#isUnicodeIdentifierStart(int)}. */
    public static final Predicate<Integer>
    IS_UNICODE_IDENTIFIER_START = new IntegerPredicate("isUnicodeIdentifierStart") {
        @Override public boolean evaluate(Integer subject) { return Character.isUnicodeIdentifierStart(subject); }
    };

    /** A predicate for {@link Character#isUnicodeIdentifierPart(int)}. */
    public static final Predicate<Integer>
    IS_UNICODE_IDENTIFIER_PART = new IntegerPredicate("isUnicodeIdentifierPart") {
        @Override public boolean evaluate(Integer subject) { return Character.isUnicodeIdentifierPart(subject); }
    };

    /** A predicate for {@link Character#isLetterOrDigit(int)}. */
    public static final Predicate<Integer>
    IS_LETTER_OR_DIGIT = new IntegerPredicate("javaDefined") {
        @Override public boolean evaluate(Integer subject) { return Character.isLetterOrDigit(subject); }
    };

    /** A predicate for {@link Character#isWhitespace(int)}. */
    public static final Predicate<Integer>
    IS_WHITESPACE = new IntegerPredicate("javaWhitespace") {
        @Override public boolean evaluate(Integer subject) { return Character.isWhitespace(subject); }
    };

    /** A predicate for {@link Characters#isHorizontalWhitespace(int)}. */
    public static final Predicate<Integer>
    IS_HORIZONTAL_WHITESPACE = new IntegerPredicate("horizontalWhitespace") {
        @Override public boolean evaluate(Integer subject) { return Characters.isHorizontalWhitespace(subject); }
    };

    public static boolean
    isHorizontalWhitespace(int codePoint) {
        return (
            " \t\u00A0\u1680\u180e\u202f\u205f\u3000".indexOf(codePoint) != -1
            || (codePoint >= '\u2000' && codePoint <= '\u200a')
        );
    }

    /** A predicate for {@link Character#isMirrored(int)}. */
    public static final Predicate<Integer>
    IS_MIRRORED = new IntegerPredicate("mirrored") {
        @Override public boolean evaluate(Integer subject) { return Character.isMirrored(subject); }
    };

    /**  A word character: [a-zA-Z_0-9] */
    public static final Predicate<Integer>
    IS_WORD = new IntegerPredicate("word") {
        @Override public boolean evaluate(Integer subject) { return Characters.isWordCharacter(subject); }
    };

    /**  A word character: [a-zA-Z_0-9] */
    public static boolean
    isWordCharacter(int codePoint) {
        return (
            (codePoint >= 'a' && codePoint <= 'z')
            || (codePoint >= 'A' && codePoint <= 'Z')
            || (codePoint >= '0' && codePoint <= '9')
            || codePoint == '_'
        );
    }

    // =============================== UNICODE CATEGORIES ===============================

    // SUPPRESS CHECKSTYLE LineLength|JavadocVariable:31
    public static final Predicate<Integer> IS_UNICODE_UNASSIGNED                = Characters.unicodeGeneralCategoryPredicate("unicodeUnassigned",              Character.UNASSIGNED);
    public static final Predicate<Integer> IS_UNICODE_LETTER                    = new IntegerPredicate("unicodeLetter") { @Override public boolean evaluate(Integer subject) { return Character.isLetter(subject);    } };
    public static final Predicate<Integer> IS_UNICODE_UPPER                     = new IntegerPredicate("unicodeUpper")  { @Override public boolean evaluate(Integer subject) { return Character.isUpperCase(subject); } };
    public static final Predicate<Integer> IS_UNICODE_LOWER                     = new IntegerPredicate("unicodeLower")  { @Override public boolean evaluate(Integer subject) { return Character.isLowerCase(subject); } };
    public static final Predicate<Integer> IS_UNICODE_TITLE                     = new IntegerPredicate("unicodeTitle")  { @Override public boolean evaluate(Integer subject) { return Character.isTitleCase(subject); } };
    public static final Predicate<Integer> IS_UNICODE_MODIFIER_LETTER           = Characters.unicodeGeneralCategoryPredicate("unicodeModifier",                Character.MODIFIER_LETTER);
    public static final Predicate<Integer> IS_UNICODE_OTHER_LETTER              = Characters.unicodeGeneralCategoryPredicate("unicodeOther",                   Character.OTHER_LETTER);
    public static final Predicate<Integer> IS_UNICODE_NON_SPACING_MARK          = Characters.unicodeGeneralCategoryPredicate("unicodeNonSpacingNark",          Character.NON_SPACING_MARK);
    public static final Predicate<Integer> IS_UNICODE_ENCLOSING_MARK            = Characters.unicodeGeneralCategoryPredicate("unicodeEnclosingMark",           Character.ENCLOSING_MARK);
    public static final Predicate<Integer> IS_UNICODE_COMBINING_SPACING_MARK    = Characters.unicodeGeneralCategoryPredicate("unicodeCombiningSpacingMark",    Character.COMBINING_SPACING_MARK);
    public static final Predicate<Integer> IS_UNICODE_DECIMAL_DIGIT_NUMBER      = Characters.unicodeGeneralCategoryPredicate("unicodeDecimalDigitNumber",      Character.DECIMAL_DIGIT_NUMBER);
    public static final Predicate<Integer> IS_UNICODE_LETTER_NUMBER             = Characters.unicodeGeneralCategoryPredicate("unicodeLetterNumber",            Character.LETTER_NUMBER);
    public static final Predicate<Integer> IS_UNICODE_OTHER_NUMBER              = Characters.unicodeGeneralCategoryPredicate("unicodeOtherNumber",             Character.OTHER_NUMBER);
    public static final Predicate<Integer> IS_UNICODE_SPACE_SEPARATOR           = Characters.unicodeGeneralCategoryPredicate("unicodeSpaceSeparator",          Character.SPACE_SEPARATOR);
    public static final Predicate<Integer> IS_UNICODE_LINE_SEPARATOR            = Characters.unicodeGeneralCategoryPredicate("unicodeLineSeparator",           Character.LINE_SEPARATOR);
    public static final Predicate<Integer> IS_UNICODE_PARAGRAPH_SEPARATOR       = Characters.unicodeGeneralCategoryPredicate("unicodeParagraphSeparator",      Character.PARAGRAPH_SEPARATOR);
    public static final Predicate<Integer> IS_UNICODE_CONTROL                   = Characters.unicodeGeneralCategoryPredicate("unicodeControl",                 Character.CONTROL);
    public static final Predicate<Integer> IS_UNICODE_FORMAT                    = Characters.unicodeGeneralCategoryPredicate("unicodeFormat",                  Character.FORMAT);
    public static final Predicate<Integer> IS_UNICODE_PRIVATE_USE               = Characters.unicodeGeneralCategoryPredicate("unicodePrivateUse",              Character.PRIVATE_USE);
    public static final Predicate<Integer> IS_UNICODE_SURROGATE                 = Characters.unicodeGeneralCategoryPredicate("unicodeSurrogate",               Character.SURROGATE);
    public static final Predicate<Integer> IS_UNICODE_DASH_PUNCTUATION          = Characters.unicodeGeneralCategoryPredicate("unicodeDashPunctuation",         Character.DASH_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_START_PUNCTUATION         = Characters.unicodeGeneralCategoryPredicate("unicodeStartPunctuation",        Character.START_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_END_PUNCTUATION           = Characters.unicodeGeneralCategoryPredicate("unicodeEndPunctuation",          Character.END_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_CONNECTOR_PUNCTUATION     = Characters.unicodeGeneralCategoryPredicate("unicodeConnectorPunctuation",    Character.CONNECTOR_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_OTHER_PUNCTUATION         = Characters.unicodeGeneralCategoryPredicate("unicodeOtherPunctuation",        Character.OTHER_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_MATH_SYMBOL               = Characters.unicodeGeneralCategoryPredicate("unicodeMATH_Symbol",             Character.MATH_SYMBOL);
    public static final Predicate<Integer> IS_UNICODE_CURRENCY_SYMBOL           = Characters.unicodeGeneralCategoryPredicate("unicodeCurrencySymbol",          Character.CURRENCY_SYMBOL);
    public static final Predicate<Integer> IS_UNICODE_MODIFIER_SYMBOL           = Characters.unicodeGeneralCategoryPredicate("unicodeModifierSymbol",          Character.MODIFIER_SYMBOL);
    public static final Predicate<Integer> IS_UNICODE_OTHER_SYMBOL              = Characters.unicodeGeneralCategoryPredicate("unicodeOtherSymbol",             Character.OTHER_SYMBOL);
    public static final Predicate<Integer> IS_UNICODE_INITIAL_QUOTE_PUNCTUATION = Characters.unicodeGeneralCategoryPredicate("unicodeInitialQuotePunctuation", Character.INITIAL_QUOTE_PUNCTUATION);
    public static final Predicate<Integer> IS_UNICODE_FINAL_QUOTE_PUNCTUATION   = Characters.unicodeGeneralCategoryPredicate("unicodeFinalQuotePunctuation",   Character.FINAL_QUOTE_PUNCTUATION);

    public static final Predicate<Integer>
    IS_UNICODE_ALPHA = new IntegerPredicate("unicodeAlpha") {

        @SuppressWarnings("null") @Override public boolean
        evaluate(Integer subject) { return Characters.CHARACTER_IS_ALPHABETIC.invoke(null, subject); }
    };
    private static final MethodWrapper1<Character, Boolean, Integer, RuntimeException>
    CHARACTER_IS_ALPHABETIC = OptionalMethods.get1(
        Character.class,       // declaringClass
        "isAlphabetic",        // methodName
        int.class,             // parameterType
        RuntimeException.class // checkedException
    );


    public static final Predicate<Integer>
    IS_UNICODE_IDEOGRAPHIC = new IntegerPredicate("unicodeIdeographic") {


        @SuppressWarnings("null") @Override public boolean
        evaluate(Integer subject) { return Characters.CHARACTER_IS_IDEOGRAPHIC.invoke(null, subject); }
    };
    private static final MethodWrapper1<Character, Boolean, Integer, RuntimeException>
    CHARACTER_IS_IDEOGRAPHIC = OptionalMethods.get1(
        Character.class,       // declaringClass
        "isIdeographic",       // methodName
        int.class,             // parameterType
        RuntimeException.class // checkedException
    );

    public static final Predicate<Integer>
    IS_UNICODE_WHITE_SPACE = new IntegerPredicate("unicodeWhiteSpace") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;

            int type = Character.getType(cp);
            return (
                type == Character.SPACE_SEPARATOR
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR
                || (cp >= 0x9 && cp <= 0xd)
                || (cp == 0x85)
            );
        }

        @Override public String
        toString() { return "unicodeWhiteSpace"; }
    };

    public static final Predicate<Integer>
    IS_UNICODE_CNTRL = Characters.unicodeGeneralCategoryPredicate("unicodeCntrl", Character.CONTROL);

    public static final Predicate<Integer>
    IS_UNICODE_PUNCT = new IntegerPredicate("unicodePunct") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;

            // See "UnicodeProp.PUNCTUATION"
            int type = Character.getType(cp);
            return (
                type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
            );
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_HEX_DIGIT = new IntegerPredicate("unicodeHexDigit") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;

            // See "UnicodeProp.HEX_DIGIT"
            return (
                Character.isDigit(cp)
                || (cp >= '0'    && cp <= '9')
                || (cp >= 'A'    && cp <= 'F')
                || (cp >= 'a'    && cp <= 'f')
                || (cp >= 0xFF10 && cp <= 0xFF19)
                || (cp >= 0xFF21 && cp <= 0xFF26)
                || (cp >= 0xFF41 && cp <= 0xFF46)
            );
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_ASSIGNED = new IntegerPredicate("unicodeAssigned") {

        @Override public boolean
        evaluate(Integer subject) { return Character.getType(subject) != Character.UNASSIGNED; }
    };

    public static final Predicate<Integer>
    IS_UNICODE_NONCHARACTER = new IntegerPredicate("unicodeNoncharacter") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return (cp & 0xfffe) == 0xfffe || (cp >= 0xfdd0 && cp <= 0xfdef);
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_DIGIT = new IntegerPredicate("unicodeDigit") {
        @Override public boolean evaluate(Integer subject) { return Character.isDigit(subject); }
    };

    public static final Predicate<Integer>
    IS_UNICODE_ALNUM = new IntegerPredicate("unicodeAlnum") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return Characters.IS_UNICODE_ALPHA.evaluate(cp) || Characters.IS_UNICODE_DIGIT.evaluate(cp);
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_BLANK = new IntegerPredicate("unicodeBlank") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return Character.getType(cp) == Character.SPACE_SEPARATOR || cp == 0x9;
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_GRAPH = new IntegerPredicate("unicodeGraph") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;

            // See "UnicodeProp.GRAPH"
            int type = Character.getType(cp);
            return (
                type != Character.SPACE_SEPARATOR
                && type != Character.LINE_SEPARATOR
                && type != Character.PARAGRAPH_SEPARATOR
                && type != Character.CONTROL
                && type != Character.SURROGATE
                && type != Character.UNASSIGNED
            );
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_PRINT = new IntegerPredicate("unicodePrint") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;

            // See "UnicodeProp.PRINT"
            return (
                (Characters.IS_UNICODE_GRAPH.evaluate(cp) || Characters.IS_UNICODE_BLANK.evaluate(cp))
                && !Characters.IS_UNICODE_CNTRL.evaluate(cp)
            );
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_WORD = new IntegerPredicate("unicodeWord") {
        @Override public boolean evaluate(Integer subject) { return Characters.isUnicodeWord(subject); }
    };

    public static final Predicate<Integer>
    IS_UNICODE_JOIN_CONTROL = new IntegerPredicate("unicodeJoinControl") {

        @Override public boolean
        evaluate(Integer subject) {
            int cp = subject;
            return cp == 0x200C || cp == 0x200D;
        }
    };

    public static final Predicate<Integer>
    IS_UNICODE_MARK = Characters.unicodeGeneralCategoryPredicate(
        "unicodeMark",
        Character.NON_SPACING_MARK,
        Character.ENCLOSING_MARK,
        Character.COMBINING_SPACING_MARK
    );

    public static final Predicate<Integer>
    IS_UNICODE_NUMBER = Characters.unicodeGeneralCategoryPredicate(
        "unicodeNumber",
        Character.DECIMAL_DIGIT_NUMBER,
        Character.LETTER_NUMBER,
        Character.OTHER_NUMBER
    );

    public static final Predicate<Integer>
    IS_UNICODE_SEPARATOR = Characters.unicodeGeneralCategoryPredicate(
        "unicodeSeparator",
        Character.SPACE_SEPARATOR,
        Character.LINE_SEPARATOR,
        Character.PARAGRAPH_SEPARATOR
    );

    public static final Predicate<Integer>
    IS_UNICODE_SPECIAL = Characters.unicodeGeneralCategoryPredicate(
        "unicodeSpecial",
        Character.CONTROL,
        Character.FORMAT,
        Character.PRIVATE_USE,
        Character.SURROGATE
    );

    public static final Predicate<Integer>
    IS_UNICODE_SYMBOL = Characters.unicodeGeneralCategoryPredicate(
        "unicodeSymbol",
        Character.MATH_SYMBOL,
        Character.CURRENCY_SYMBOL,
        Character.MODIFIER_SYMBOL,
        Character.OTHER_SYMBOL
    );

    public static final Predicate<Integer>
    IS_UNICODE_UPPER_LOWER_TITLE = Characters.unicodeGeneralCategoryPredicate(
        "unicodeUpperLowerTitle",
        Character.UPPERCASE_LETTER,
        Character.LOWERCASE_LETTER,
        Character.TITLECASE_LETTER
    );

    public static final Predicate<Integer>
    IS_UNICODE_ALPHA2 = Characters.unicodeGeneralCategoryPredicate(
        "unicodeAlpha2",
        Character.UPPERCASE_LETTER,
        Character.LOWERCASE_LETTER,
        Character.TITLECASE_LETTER,
        Character.MODIFIER_LETTER,
        Character.OTHER_LETTER,
        Character.DECIMAL_DIGIT_NUMBER
    );

    public static final Predicate<Integer>
    IS_UNICODE_LATIN1 = Characters.rangePredicate("unicodeAlpha2", 0, 0xff);

    protected static boolean
    isUnicodeWord(Integer subject) {

        if (
            Characters.IS_UNICODE_ALPHA.evaluate(subject)
            || Characters.IS_UNICODE_JOIN_CONTROL.evaluate(subject)
        ) return true;

        int type = Character.getType(subject);
        return (
            type == Character.NON_SPACING_MARK
            || type == Character.ENCLOSING_MARK
            || type == Character.COMBINING_SPACING_MARK
            || type == Character.DECIMAL_DIGIT_NUMBER
            || type == Character.CONNECTOR_PUNCTUATION
        );
    }

    /**
     * @return {@code null} iff the named category is unknown
     */
    @Nullable public static Predicate<Integer>
    unicodeCategoryFromName(String name) {
        return Characters.UNICODE_CATEGORIES.get(name.toUpperCase(Locale.US));
    }
    private static final Map<String /*name*/, Predicate<Integer>> UNICODE_CATEGORIES;
    static {
        Map<String /*name*/, Predicate<Integer>> m = new HashMap<String, Predicate<Integer>>();

        m.put("CN", Characters.IS_UNICODE_UNASSIGNED);
        m.put("LU", Characters.IS_UNICODE_UPPER);
        m.put("LL", Characters.IS_UNICODE_LOWER);
        m.put("LT", Characters.IS_UNICODE_TITLE);
        m.put("LM", Characters.IS_UNICODE_MODIFIER_LETTER);
        m.put("LO", Characters.IS_UNICODE_OTHER_LETTER);
        m.put("MN", Characters.IS_UNICODE_NON_SPACING_MARK);
        m.put("ME", Characters.IS_UNICODE_ENCLOSING_MARK);
        m.put("MC", Characters.IS_UNICODE_COMBINING_SPACING_MARK);
        m.put("ND", Characters.IS_UNICODE_DECIMAL_DIGIT_NUMBER);
        m.put("NL", Characters.IS_UNICODE_LETTER_NUMBER);
        m.put("NO", Characters.IS_UNICODE_OTHER_NUMBER);
        m.put("ZS", Characters.IS_UNICODE_SPACE_SEPARATOR);
        m.put("ZL", Characters.IS_UNICODE_LINE_SEPARATOR);
        m.put("ZP", Characters.IS_UNICODE_PARAGRAPH_SEPARATOR);
        m.put("CC", Characters.IS_UNICODE_CONTROL);
        m.put("CF", Characters.IS_UNICODE_FORMAT);
        m.put("CO", Characters.IS_UNICODE_PRIVATE_USE);
        m.put("CS", Characters.IS_UNICODE_SURROGATE);
        m.put("PD", Characters.IS_UNICODE_DASH_PUNCTUATION);
        m.put("PS", Characters.IS_UNICODE_START_PUNCTUATION);
        m.put("PE", Characters.IS_UNICODE_END_PUNCTUATION);
        m.put("PC", Characters.IS_UNICODE_CONNECTOR_PUNCTUATION);
        m.put("PO", Characters.IS_UNICODE_OTHER_PUNCTUATION);
        m.put("SM", Characters.IS_UNICODE_MATH_SYMBOL);
        m.put("SC", Characters.IS_UNICODE_CURRENCY_SYMBOL);
        m.put("SK", Characters.IS_UNICODE_MODIFIER_SYMBOL);
        m.put("SO", Characters.IS_UNICODE_OTHER_SYMBOL);
        m.put("PI", Characters.IS_UNICODE_INITIAL_QUOTE_PUNCTUATION);
        m.put("PF", Characters.IS_UNICODE_FINAL_QUOTE_PUNCTUATION);
        m.put("L",  Characters.IS_UNICODE_LETTER);
        m.put("M",  Characters.IS_UNICODE_MARK);
        m.put("N",  Characters.IS_UNICODE_NUMBER);
        m.put("Z",  Characters.IS_UNICODE_SEPARATOR);
        m.put("C",  Characters.IS_UNICODE_SPECIAL);
        m.put("P",  Characters.IS_UNICODE_PUNCT);
        m.put("S",  Characters.IS_UNICODE_SYMBOL);
        m.put("LC", Characters.IS_UNICODE_UPPER_LOWER_TITLE);
        m.put("LD", Characters.IS_UNICODE_ALPHA2);
        m.put("L1", Characters.IS_UNICODE_LATIN1);

        m.put("ALPHABETIC",              Characters.IS_UNICODE_ALPHA);
        m.put("LETTER",                  Characters.IS_UNICODE_LETTER);
        m.put("IDEOGRAPHIC",             Characters.IS_UNICODE_IDEOGRAPHIC);
        m.put("LOWERCASE",               Characters.IS_UNICODE_LOWER);
        m.put("UPPERCASE",               Characters.IS_UNICODE_UPPER);
        m.put("TITLECASE",               Characters.IS_UNICODE_TITLE);
        m.put("WHITE_SPACE",             Characters.IS_UNICODE_WHITE_SPACE);
        m.put("CONTROL",                 Characters.IS_UNICODE_CNTRL);
        m.put("PUNCTUATION",             Characters.IS_UNICODE_PUNCT);
        m.put("HEX_DIGIT",               Characters.IS_UNICODE_HEX_DIGIT);
        m.put("ASSIGNED",                Characters.IS_UNICODE_ASSIGNED);
        m.put("NONCHARACTER_CODE_POINT", Characters.IS_UNICODE_NONCHARACTER);
        m.put("DIGIT",                   Characters.IS_UNICODE_DIGIT);
        m.put("ALNUM",                   Characters.IS_UNICODE_ALNUM);
        m.put("BLANK",                   Characters.IS_UNICODE_BLANK);
        m.put("GRAPH",                   Characters.IS_UNICODE_GRAPH);
        m.put("PRINT",                   Characters.IS_UNICODE_PRINT);
        m.put("WORD",                    Characters.IS_UNICODE_WORD);
        m.put("JOIN_CONTROL",            Characters.IS_UNICODE_JOIN_CONTROL);

        // Aliases.
        m.put("WHITESPACE",            m.get("WHITE_SPACE"));
        m.put("HEXDIGIT",              m.get("HEX_DIGIT"));
        m.put("NONCHARACTERCODEPOINT", m.get("NONCHARACTER_CODE_POINT"));
        m.put("JOINCONTROL",           m.get("JOIN_CONTROL"));

        UNICODE_CATEGORIES = Collections.unmodifiableMap(m);
    }

    @Nullable public static Predicate<Integer>
    unicodeBinaryPropertyFromName(String name) {
        return Characters.UNICODE_PROPERTIES.get(name.toUpperCase(Locale.US));
    }
    private static final Map<String /*name*/, Predicate<Integer>> UNICODE_PROPERTIES;
    static {
        Map<String /*name*/, Predicate<Integer>> m = new HashMap<String, Predicate<Integer>>();

        m.put("ALPHABETIC",              Characters.IS_UNICODE_ALPHA);
        m.put("IDEOGRAPHIC",             Characters.IS_UNICODE_IDEOGRAPHIC);
        m.put("LETTER",                  Characters.IS_UNICODE_LETTER);
        m.put("LOWERCASE",               Characters.IS_UNICODE_LOWER);
        m.put("UPPERCASE",               Characters.IS_UNICODE_UPPER);
        m.put("TITLECASE",               Characters.IS_UNICODE_TITLE);
        m.put("PUNCTUATION",             Characters.IS_UNICODE_PUNCT);
        m.put("CONTROL",                 Characters.IS_UNICODE_CNTRL);
        m.put("WHITE_SPACE",             Characters.IS_UNICODE_WHITE_SPACE);
        m.put("DIGIT",                   Characters.IS_UNICODE_DIGIT);
        m.put("HEX_DIGIT",               Characters.IS_UNICODE_HEX_DIGIT);
        m.put("JOIN_CONTROL",            Characters.IS_UNICODE_JOIN_CONTROL);
        m.put("NONCHARACTER_CODE_POINT", Characters.IS_UNICODE_NONCHARACTER);
        m.put("ASSIGNED",                Characters.IS_UNICODE_ASSIGNED);

        UNICODE_PROPERTIES = Collections.unmodifiableMap(m);
    }

//    @Nullable public static Predicate<Integer>
//    unicodePredefinedCharacterClassFromName(String name) {
//        return Characters.UNICODE_PREDEFINIED_CHARACTER_CLASSES.get(name.toUpperCase(Locale.US));
//    }
//    private static final Map<String /*name*/, Predicate<Integer>> UNICODE_PREDEFINIED_CHARACTER_CLASSES;
//    static {
//        Map<String /*name*/, Predicate<Integer>> m = new HashMap<String, Predicate<Integer>>();
//
//        m.put("ALNUM",                   Characters.IS_UNICODE_ALNUM);
//        m.put("BLANK",                   Characters.IS_UNICODE_BLANK);
//        m.put("GRAPH",                   Characters.IS_UNICODE_GRAPH);
//        m.put("PRINT",                   Characters.IS_UNICODE_PRINT);
//        m.put("WORD",                    Characters.IS_UNICODE_WORD);
//
//        // Aliases.
//        m.put("WHITESPACE",            m.get("WHITE_SPACE"));
//        m.put("HEXDIGIT",              m.get("HEX_DIGIT"));
//        m.put("NONCHARACTERCODEPOINT", m.get("NONCHARACTER_CODE_POINT"));
//        m.put("JOINCONTROL",           m.get("JOIN_CONTROL"));
//
//        UNICODE_PREDEFINIED_CHARACTER_CLASSES = Collections.unmodifiableMap(m);
//    }

    /**
     * @return A wrapper predicate around {@link Character#getType(int)}
     */
    private static IntegerPredicate
    unicodeGeneralCategoryPredicate(String toString, final byte generalCategory) {

        return new IntegerPredicate(toString) {
            @Override public boolean evaluate(Integer subject) { return Character.getType(subject) == generalCategory; }
        };
    }

    /**
     * @return A wrapper predicate around {@link Character#getType(int)}
     */
    private static IntegerPredicate
    unicodeGeneralCategoryPredicate(String toString, final byte gc1, final byte gc2, final byte... gc3) {

        int mask = 1 << gc1 | 1 << gc2;
        for (byte gc : gc3) mask |= 1 << gc;

        final int finalMask = mask;
        return new IntegerPredicate(toString) {

            @Override public boolean
            evaluate(Integer subject) { return (finalMask & 1 << Character.getType(subject)) != 0; }
        };
    }

    private static IntegerPredicate
    rangePredicate(String toString, final int minCp, final int maxCp) {

        return new IntegerPredicate(toString) {

            @Override public boolean
            evaluate(Integer subject) {
                int cp = subject;
                return cp >= minCp && cp <= maxCp;
            }
        };
    }

    @Nullable public static Predicate<Integer>
    unicodePredefinedCharacterClassFromName(String name) {
        return Characters.UNICODE_PREDEFINED_CHARACTER_CLASSES.get(name.toUpperCase(Locale.US));
    }
    private static final Map<String /*name*/, Predicate<Integer>> UNICODE_PREDEFINED_CHARACTER_CLASSES;
    static {
        Map<String /*name*/, Predicate<Integer>> m = new HashMap<String, Predicate<Integer>>();

        m.put("LOWER",  Characters.IS_UNICODE_LOWER);
        m.put("UPPER",  Characters.IS_UNICODE_UPPER);
        m.put("ASCII",  Characters.IS_POSIX_ASCII);
        m.put("ALPHA",  Characters.IS_UNICODE_ALPHA);
        m.put("DIGIT",  Characters.IS_UNICODE_DIGIT);
        m.put("ALNUM",  Characters.IS_UNICODE_ALNUM);
        m.put("PUNCT",  Characters.IS_UNICODE_PUNCT);
        m.put("GRAPH",  Characters.IS_UNICODE_GRAPH);
        m.put("PRINT",  Characters.IS_UNICODE_PRINT);
        m.put("BLANK",  Characters.IS_UNICODE_BLANK);
        m.put("CNTRL",  Characters.IS_UNICODE_CNTRL);
        m.put("XDIGIT", Characters.IS_UNICODE_HEX_DIGIT);
        m.put("SPACE",  Characters.IS_UNICODE_WHITE_SPACE);

        UNICODE_PREDEFINED_CHARACTER_CLASSES = Collections.unmodifiableMap(m);
    }

    @Nullable public static Predicate<Integer>
    posixCharacterClassFromName(String name) {
        return Characters.POSIX_CHARACTER_CLASSES.get(name.toUpperCase(Locale.US));
    }
    private static final Map<String /*name*/, Predicate<Integer>> POSIX_CHARACTER_CLASSES;
    static {
        Map<String /*name*/, Predicate<Integer>> m = new HashMap<String, Predicate<Integer>>();

        m.put("LOWER",  Characters.IS_POSIX_LOWER);
        m.put("UPPER",  Characters.IS_POSIX_UPPER);
        m.put("ASCII",  Characters.IS_POSIX_ASCII);
        m.put("ALPHA",  Characters.IS_POSIX_ALPHA);
        m.put("DIGIT",  Characters.IS_POSIX_DIGIT);
        m.put("ALNUM",  Characters.IS_POSIX_ALNUM);
        m.put("PUNCT",  Characters.IS_POSIX_PUNCT);
        m.put("GRAPH",  Characters.IS_POSIX_GRAPH);
        m.put("PRINT",  Characters.IS_POSIX_PRINT);
        m.put("BLANK",  Characters.IS_POSIX_BLANK);
        m.put("CNTRL",  Characters.IS_POSIX_CNTRL);
        m.put("XDIGIT", Characters.IS_POSIX_XDIGIT);
        m.put("SPACE",  Characters.IS_POSIX_SPACE);

        POSIX_CHARACTER_CLASSES = Collections.unmodifiableMap(m);
    }

    @Nullable public static Predicate<Integer>
    unicodeBlockFromName(String name) {

        final UnicodeBlock block;
        try {
            block = Character.UnicodeBlock.forName(name);
        } catch (IllegalArgumentException iae) {
            return null;
        }

        return new Predicate<Integer>() {
            @Override public boolean evaluate(Integer subject) { return Character.UnicodeBlock.of(subject) == block; }
            @Override public String  toString()                { return "inUnicodeBlock(" + block + ")";             }
        };
    }

    static final MethodWrapper1<?, Object, String, RuntimeException>
    UNICODE_SCRIPT_FOR_NAME = OptionalMethods.get1(
        null,                                // classLoader
        "java.lang.Character$UnicodeScript", // declaringClassName
        "forName",                           // methodName
        String.class,                        // parameterType
        null                                 // checkedException
    );
    static final MethodWrapper1<?, Object, Integer, RuntimeException>
    UNICODE_SCRIPT_OF = OptionalMethods.get1(
        null,                                // classLoader
        "java.lang.Character$UnicodeScript", // declaringClassName
        "of",                                // methodName
        int.class,                           // parameterType
        null                                 // checkedException
    );
    static final boolean
    UNICODE_SCRIPT_AVAILABLE = (
        Characters.UNICODE_SCRIPT_FOR_NAME.isAvailable()
        && Characters.UNICODE_SCRIPT_OF.isAvailable()
    );

    /**
     * @return Whether this JRE supports unicode scripts (because it is 1.7 or later)
     */
    public static boolean
    unicodeScriptAvailable() { return Characters.UNICODE_SCRIPT_AVAILABLE; }

    /**
     * @return                               A predicate that tests if a given code point is in the named script;
     *                                       {@code null} iff a UNICODE script with the <var>name</name> is unknown
     * @throws UnsupportedOperationException This JRE does not support unicode scripts (because it is pre-1.7)
     * @see                                  #unicodeScriptAvailable()
     */
    @Nullable public static Predicate<Integer>
    unicodeScriptPredicate(String name) {

        final Object unicodeScript1;
        try {
            unicodeScript1 = Characters.UNICODE_SCRIPT_FOR_NAME.invoke(null, name);
        } catch (IllegalArgumentException iae) {

            // Script name is unknown.
            return null;
        }

        return new Predicate<Integer>() {

            @Override public boolean
            evaluate(Integer subject) {
                Object unicodeScript2 = Characters.UNICODE_SCRIPT_OF.invoke(null, subject);
                return unicodeScript1 == unicodeScript2;
            }

            @Override public String
            toString() { return "unicodeScript(" + unicodeScript1 + ")"; }
        };
    }

    /**
     * @return The set of codepoints that are regarded as case-insensitively "equal", including the <var>cp</var>, e.g.
     *         <code>{ 'a', 'A' }</code>, or {@code null} iff no other codepoints are case-insensitively equal with
     *         <var>cp</var>
     */
    @Nullable public static String
    caseInsensitivelyEqualCharacters(int cp) {

        {
            String s = Characters.SPECIAL_CASES.get(cp);
            if (s != null) return s;
        }

        int lc = Character.toLowerCase(cp);
        int uc = Character.toUpperCase(cp);
        int tc = Character.toTitleCase(cp);

        if (lc == uc) {
            if (uc == tc) return null;                          // xxx
            return new String(new int[] { lc, tc }, 0, 2);      // xxy
        } else
        if (lc == tc) {
            return new String(new int[] { lc, uc }, 0, 2);      // xyx
        } else
        if (uc == tc) {
            return new String(new int[] { lc, uc }, 0, 2);      // xyy
        } else
        {
            return new String(new int[] { lc, uc, tc }, 0, 3);  // xyz

        }
    }
    private static final Map<Integer, String> SPECIAL_CASES;
    static {
        Map<Integer, String> m = new HashMap<Integer, String>();
        for (String s : new String[] {
            "I"      + "i"      + "\u0130" + "\u0131",
            "K"      + "k"      + "\u212a",
            "S"      + "s"      + "\u017f",
            "µ"      + "\u039c" + "\u03bc",
            "Å"      + "å"      + "\u212b",
            "\u0345" + "\u0399" + "\u03b9" + "\u1fbe",
            "\u0392" + "\u03b2" + "\u03d0",
            "\u0395" + "\u03b5" + "\u03f5",
            "\u0398" + "\u03b8" + "\u03d1" + "\u03f4",
            "\u039a" + "\u03ba" + "\u03f0",
            "\u03a0" + "\u03c0" + "\u03d6",
            "\u03a1" + "\u03c1" + "\u03f1",
            "\u03a3" + "\u03c2" + "\u03c3",
            "\u03a6" + "\u03c6" + "\u03d5",
            "\u03a9" + "\u03c9" + "\u2126",
            "\u1e60" + "\u1e61" + "\u1e9b",
        }) {
            for (int c : s.toCharArray()) m.put(c, s);
        }
        SPECIAL_CASES = m;
    }
}
