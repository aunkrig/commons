
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.commons.text;

import java.util.ArrayList;
import java.util.List;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * The methods of this class (in combination of the methods of the {@link Phrase} interface convert between various
 * notations.
 */
public final
class Notations {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Notations() {}

    /**
     * Representation of the result of notation parsing.
     *
     * @see Notations#fromCamelCase(String)
     * @see Notations#fromHyphenated(String)
     * @see Notations#fromUnderscored(String)
     */
    public
    interface Phrase {

        /**
         * @return A phrase which consists of the <var>words</var> and the words of {@code this} phrase
         */
        Phrase prepend(String... words);

        /**
         * @return A phrase which consists of the words of {@code this} phrase and the <var>words</var>
         */
        Phrase append(String... words);

        /**
         * Converts the first letter of each word to upper case, the rest of each word to lower case, and concatenates
         * them.
         */
        String toUpperCamelCase();

        /**
         * Converts the first word to lower case, converts the first letter of each following word to upper case, the
         * rest of the word to lower case, and concatenates the words.
         */
        String toLowerCamelCase();

        /**
         * Converts the words to upper case and concatenates them with {@code "-"} as the separator.
         */
        String toUpperCaseHyphenated();

        /**
         * Converts the words to lower case and concatenates them with {@code "-"} as the separator.
         */
        String toLowerCaseHyphenated();

        /**
         * Converts the words to upper case and concatenates them with {@code "_"} as the separator.
         */
        String toUpperCaseUnderscored();

        /**
         * Converts the words to lower case and concatenates them with {@code "_"} as the separator.
         */
        String toLowerCaseUnderscored();
    }

    /**
     *
     * @copyright (C) 2015, SWM Services GmbH
     */
    private static
    class PhraseImpl implements Phrase {

        final String[] words;

        PhraseImpl(String[] words) { this.words = words; }

        PhraseImpl(List<String> words) { this.words = words.toArray(new String[words.size()]); }

        PhraseImpl(String word) { this.words = new String[] { word }; }

        @Override public Phrase
        prepend(String... words) {
            String[] tmp = new String[words.length + this.words.length];
            System.arraycopy(words,      0, tmp, 0,            words.length);
            System.arraycopy(this.words, 0, tmp, words.length, this.words.length);
            return new PhraseImpl(tmp);
        }

        @Override public Phrase
        append(String... words) {
            String[] tmp = new String[this.words.length + words.length];
            System.arraycopy(this.words, 0, tmp, 0,            this.words.length);
            System.arraycopy(words,      0, tmp, words.length, words.length);
            return new PhraseImpl(tmp);
        }

        @Override public String
        toUpperCamelCase() {
            StringBuilder sb = new StringBuilder();
            for (String word : this.words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }

        @Override public String
        toLowerCamelCase() {
            if (this.words.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            sb.append(this.words[0].toLowerCase());
            for (int i = 1; i < this.words.length; i++) {
                String word = this.words[i];
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }

        @Override public String
        toUpperCaseHyphenated() {
            int n = this.words.length;
            if (n == 0) return "";
            if (n == 1) return this.words[0].toUpperCase();
            StringBuilder sb = new StringBuilder();
            sb.append(this.words[0].toUpperCase()).append('-').append(this.words[1].toUpperCase());
            for (int i = 2; i < this.words.length; i++) {
                sb.append('-').append(this.words[i].toUpperCase());
            }
            return sb.toString();
        }

        @Override public String
        toLowerCaseHyphenated() {
            int n = this.words.length;
            if (n == 0) return "";
            if (n == 1) return this.words[0].toLowerCase();
            StringBuilder sb = new StringBuilder();
            sb.append(this.words[0].toLowerCase()).append('-').append(this.words[1].toLowerCase());
            for (int i = 2; i < this.words.length; i++) {
                sb.append('-').append(this.words[i].toLowerCase());
            }
            return sb.toString();
        }

        @Override public String
        toUpperCaseUnderscored() {
            int n = this.words.length;
            if (n == 0) return "";
            if (n == 1) return this.words[0].toUpperCase();
            StringBuilder sb = new StringBuilder();
            sb.append(this.words[0].toUpperCase()).append('_').append(this.words[1].toUpperCase());
            for (int i = 2; i < this.words.length; i++) {
                sb.append('_').append(this.words[i].toUpperCase());
            }
            return sb.toString();
        }

        @Override public String
        toLowerCaseUnderscored() {
            int n = this.words.length;
            if (n == 0) return "";
            if (n == 1) return this.words[0].toLowerCase();
            StringBuilder sb = new StringBuilder();
            sb.append(this.words[0].toLowerCase()).append('_').append(this.words[1].toLowerCase());
            for (int i = 2; i < this.words.length; i++) {
                sb.append('_').append(this.words[i].toLowerCase());
            }
            return sb.toString();
        }
    }

    /**
     * Parses a camel-case string: Each upper-case letter indicates the beginning of a new word.
     */
    public static Phrase
    fromCamelCase(String s) {
        int len = s.length();

        int i = 1;
        for (; i < len; i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                List<String> l = new ArrayList<String>(4);
                l.add(s.substring(0, i));
                for (int to = i + 1;; to++) {
                    if (to == len) {
                        l.add(s.substring(i));
                        return new PhraseImpl(l);
                    }
                    if (Character.isUpperCase(s.charAt(to))) {
                        l.add(s.substring(i, to));
                        i = to;
                    }
                }
            }
        }

        // String contains no upper-case letters.
        return new PhraseImpl(s);
    }

    /**
     * Double hyphens enclose the word {@code ""}.
     * A leading hyphen maps the leading word {@code ""}.
     * A trailing hyphen maps the trailing word {@code ""}.
     */
    public static Phrase
    fromHyphenated(String s) {
        int idx = s.indexOf('-');
        if (idx == -1) return new PhraseImpl(s);

        List<String> l = new ArrayList<String>(4);
        l.add(s.substring(0, idx));
        for (int to = s.indexOf('-', idx + 1);;) {
            if (to == -1) {
                l.add(s.substring(idx + 1));
                return new PhraseImpl(l);
            }
            l.add(s.substring(idx + 1, to));
            idx = to;
        }
    }

    /**
     * Double underscores enclose the word {@code ""}.
     * A leading underscore maps the leading word {@code ""}.
     * A trailing underscore maps the trailing word {@code ""}.
     */
    public static Phrase
    fromUnderscored(String s) {
        int idx = s.indexOf('_');
        if (idx == -1) return new PhraseImpl(s);

        List<String> l = new ArrayList<String>(4);
        l.add(s.substring(0, idx));
        for (int to = s.indexOf('_', idx + 1);;) {
            if (to == -1) {
                l.add(s.substring(idx + 1));
                return new PhraseImpl(l);
            }
            l.add(s.substring(idx + 1, to));
            idx = to;
        }
    }
}
