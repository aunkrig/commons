
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

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@code java.lang.String}-related utility methods.
 */
public final
class StringUtil {

    private
    StringUtil() {}

    /**
     * Converts all <var>elements</var> to string and concatenates these, separated by the <var>glue</var>.
     */
    public static String
    join(Collection<? extends Object> elements, String glue) {
        if (elements.size() == 0) return "";
        if (elements.size() == 1) return String.valueOf(elements.iterator().next());

        Iterator<? extends Object> it = elements.iterator();
        StringBuilder              sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) sb.append(glue).append(it.next());
        return sb.toString();
    }

    /**
     * Naturally, {@link CharSequence} does not refine {@link Object#equals(Object)}. This method fills the gap.
     */
    public static boolean
    equals(CharSequence cs1, CharSequence cs2) {

        int l1 = cs1.length(), l2 = cs2.length();

        if (l1 != l2) return false;

        for (int i = 0; i < l1; i++) {
            if (cs1.charAt(i) != cs2.charAt(i)) return false;
        }

        return true;
    }

    /**
     * Naturally, {@link CharSequence} does not extend {@link Comparable}. This method fills the gap.
     */
    public static int
    compareTo(CharSequence cs1, CharSequence cs2) {

        int l1 = cs1.length(), l2 = cs1.length();

        int n = Math.min(l1,  l2);
        for (int i = 0; i < n; i++) {

            int diff = cs1.charAt(i) - cs2.charAt(i);
            if (diff != 0) return diff;
        }

        return l1 - l2;
    }

    /**
     * @return A string consisting of <var>n</var> times the character <var>c</var>
     */
    public static String
    repeat(int n, char c) {

        if (n <= 0) return "";

        char[] ca = new char[n];
        Arrays.fill(ca, c);
        return String.copyValueOf(ca);
    }

    /**
     * @return A string consisting of <var>n</var> times the string <var>s</var>
     */
    public static String
    repeat(int n, String s) {

        if (n <= 0) return "";
        if (n == 1) return s;

        int    len = s.length();
        char[] src = s.toCharArray();
        char[] dst = new char[n * len];
        for (int i = 0; i < n; i++) {
            System.arraycopy(src, 0, dst, i * len, len);
        }
        return String.copyValueOf(dst);
    }

    /**
     * @return Whether the given char sequences consists only of characters 0...32
     */
    public static boolean
    isBlank(CharSequence cs) {

        for (int i = cs.length() - 1; i >= 0; i--) {
            if (cs.charAt(i) > ' ') return false;
        }

        return true;
    }

    /**
     * A predicate that evaluates its subject with {@link #isBlank(CharSequence)}.
     */
    public static final Predicate<CharSequence>
    IS_BLANK = new Predicate<CharSequence>() {
        @Override public boolean evaluate(CharSequence subject) { return StringUtil.isBlank(subject); }
    };

    /** @return The string <var>s</var>, with the first letter converted to upper case */
    public static String
    firstLetterToUpperCase(String s) {

        if (s.isEmpty() || !Character.isLowerCase(s.charAt(0))) return s;

        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** @return The string <var>s</var>, with the first letter converted to lower case */
    public static String
    firstLetterToLowerCase(String s) {

        if (s.isEmpty() || !Character.isUpperCase(s.charAt(0))) return s;

        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    /**
     * Wraps a char sequence as an iterable.
     */
    public static Iterable<Character>
    asIterable(final CharSequence subject) {

        return new Iterable<Character>() {
            @Override public Iterator<Character> iterator() { return StringUtil.iterator(subject); }
        };
    }

    /**
     * Returns an iterator that produces the characters of the <var>subject</var>, from index 0 through index {@code
     * subject.length() - 1}.
     */
    public static Iterator<Character>
    iterator(final CharSequence subject) {

        return new Iterator<Character>() {

            int idx;

            @Override public void
            remove() { throw new NoSuchElementException("remove"); }

            @Override public Character
            next() {
                if (this.idx >= subject.length()) throw new ArrayIndexOutOfBoundsException();
                return subject.charAt(this.idx++);
            }

            @Override public boolean
            hasNext() { return this.idx < subject.length(); }
        };
    }

    /**
     * Returns an iterator that produces the characters of the <var>subject</var> in reverse order, from index {@code
     * subject.length() - 1} through index 0.
     */
    public static Iterator<Character>
    reverseIterator(final CharSequence subject) {

        return new Iterator<Character>() {

            int idx = subject.length();

            @Override public void
            remove() { throw new NoSuchElementException("remove"); }

            @Override public Character
            next() {
                if (this.idx <= 0) throw new ArrayIndexOutOfBoundsException();
                return subject.charAt(--this.idx);
            }

            @Override public boolean
            hasNext() { return this.idx > 0; }
        };
    }

    /**
     * @return The <var>s</var>, with all trailing {@code '\r'} and {@code '\n'} chopped off
     * @see    BufferedReader#readLine()
     */
    public static String
    lessTrailingLineSeparators(String s) {

        if (s.isEmpty()) return "";

        int i = s.length() - 1;

        // Examine the last character.
        {
            char c = s.charAt(i);
            if (c != '\r' && c != '\n') return s;
        }

        // Find the last non-line-separator character.
        for (i--; i >= 0; i--) {
            char c = s.charAt(i);
            if (c != '\r' && c != '\n') return s.substring(0, i + 1);
        }

        return "";
    }

    /**
     * @return Whether the <var>subject</var> contains any of the <var>characters</var>
     */
    public static boolean
    containsAny(String subject, String characters) {
        for (int i = characters.length() - 1; i >= 0; i--) {
            if (subject.indexOf(Character.codePointAt(characters, i)) != -1) return true;
        }
        return false;
    }

    /**
     * @see ObjectUtil#equals(Object, Object)
     */
    public static boolean
    equalsIgnoreCase(@Nullable String s1, @Nullable String s2) {
        return s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);
    }

    /**
     * @see #indexOf(CharSequence, int, int)
     */
    public
    interface IndexOf {

        /**
         * The equivalent of {@link String#indexOf(String)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(infix).indexOf(subject) == subject.indexOf(infix)
         * </pre>
         *
         * @return {@code 0} ... {@code subject.length() - infix.length()}, or {@code -1}
         */
        int indexOf(CharSequence subject);

        /**
         * The equivalent of {@link String#indexOf(String, int)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(infix).indexOf(subject, minIndex) == subject.indexOf(infix, minIndex)
         * </pre>
         *
         * @return {@code max(0, minIndex)} ... {@code subject.length() - infix.length()}, or {@code -1}
         */
        int indexOf(CharSequence subject, int minIndex);

        /**
         * Like {@link #indexOf(CharSequence, int)}, but the match terminates at index <var>maxIndex</var> (inclusive).
         *
         * @return {@code max(0, minIndex)} ... {@code min(maxIndex, subject.length() - infix.length())}, or {@code -1}
         */
        int indexOf(CharSequence subject, int minIndex, int maxIndex);

        /**
         * The equivalent of {@link String#lastIndexOf(String)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(infix).lastIndexOf(subject) == subject.lastIndexOf(infix)
         * </pre>
         *
         * @return {@code 0} ... {@code subject.length() - infix.length()}, or {@code -1}
         */
        int lastIndexOf(CharSequence subject);

        /**
         * The equivalent of {@link String#lastIndexOf(String, int)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(infix).lastIndexOf(subject, maxIndex) == subject.lastIndexOf(infix, maxIndex)
         * </pre>
         *
         * @return {@code 0} ... {@code min(maxIndex, subject.length() - infix.length())}, or {@code -1}
         */
        int lastIndexOf(CharSequence subject, int maxIndex);

        /**
         * Like {@link #lastIndexOf(CharSequence, int)}, but the match terminates at index <var>minIndex</var>
         * (inclusive).
         *
         * @return {@code max(0, minIndex)} ... {@code min(maxIndex, subject.length() - infix.length())}, or {@code -1}
         */
        int lastIndexOf(CharSequence subject, int minIndex, int maxIndex);

        /**
         * @return A textual representation of the infix and the search algorithm
         */
        @Override String toString();
    }

    /**
     * Runtime-optimized reimplementation of {@link String#indexOf(String)} and {@link String#lastIndexOf(String)}.
     * <p>
     *   This method returns an implementation that performs at least as well as the {@link String} methods by analyzing
     *   the <var>infix</var> (the string to search for).
     * </p>
     */
    public static IndexOf
    newIndexOf(final String infix) {

        if (infix.length() < 16) {
            return StringUtil.newNaiveIndexOf(infix);
        } else {
            return StringUtil.newKnuthMorrisPrattIndexOf(infix);
        }
    }

    private abstract static
    class AbstractIndexOf implements IndexOf {
        @Override public int indexOf(CharSequence subject)                   { return this.indexOf(subject, 0, Integer.MAX_VALUE);         } // SUPPRESS CHECKSTYLE LineLength:4
        @Override public int indexOf(CharSequence subject, int minIndex)     { return this.indexOf(subject, minIndex, Integer.MAX_VALUE); }
        @Override public int lastIndexOf(CharSequence subject)               { return this.lastIndexOf(subject, 0, Integer.MAX_VALUE);     }
        @Override public int lastIndexOf(CharSequence subject, int maxIndex) { return this.lastIndexOf(subject, 0, maxIndex);              }

        @Override public abstract String toString();
    }

    /**
     * @return A wrapper for {@link String#indexOf(String)} and {@link String#lastIndexOf(String)}, which implements
     *         a naive string search algorithm
     */
    public static IndexOf
    newNaiveIndexOf(final String infix) {

        return new AbstractIndexOf() {

            final int infixLength = infix.length();

            @Override public int
            indexOf(CharSequence subject, int minIndex, int maxIndex) {

                if (maxIndex >= subject.length() - this.infixLength) {
                    return subject.toString().indexOf(infix, minIndex);
                }

                return subject.toString().substring(0, maxIndex + this.infixLength).indexOf(infix, minIndex);
            }

            @Override public int
            lastIndexOf(CharSequence subject, int minIndex, int maxIndex) {

                if (minIndex <= 0) return subject.toString().lastIndexOf(infix, maxIndex);

                subject  =  subject.subSequence(minIndex, subject.length());
                maxIndex -= minIndex;

                int result = subject.toString().lastIndexOf(infix, maxIndex);
                return result == -1 ? -1 : result + minIndex;
            }

            @Override public String
            toString() { return "naive(" + PrettyPrinter.toString(infix) + ")"; }
        };
    }

    /**
     * Implementation of the Knuth-Morris-Pratt string search algorithm.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">The Knuth–Morris–Pratt
     *      algorithm</a>
     */
    public static IndexOf
    newKnuthMorrisPrattIndexOf(final String infix) {

        return new AbstractIndexOf() {

            final CharToIntMapping deltas      = this.computeDeltas(infix);
            final int              infixLength = infix.length();

            @Override public int
            indexOf(CharSequence subject, int minIndex, int maxIndex) {
                int subjectLength = subject.length();

                if (minIndex < 0) minIndex = 0;

                {
                    int limit = subjectLength - this.infixLength;
                    if (maxIndex > limit) maxIndex = limit;
                }

                if (minIndex > maxIndex) return -1;

                minIndex += this.infixLength - 1;
                for (;;) {

                    int delta = this.deltas.get(subject.charAt(minIndex));
                    if (delta == -1) {
                        if (minIndex >= maxIndex) break;
                        minIndex += this.infixLength;
                        continue;
                    }

                    minIndex -= delta;

                    if (minIndex > maxIndex) break;

                    for (int infixIndex = 0, subjectIndex = minIndex;; subjectIndex++, infixIndex++) {

                        if (infixIndex >= this.infixLength) return minIndex;

                        if (subject.charAt(subjectIndex) != infix.charAt(infixIndex)) break;
                    }

                    if (minIndex == maxIndex) break;

                    minIndex += this.infixLength;
                }

                return -1;
            }

            @Override public int
            lastIndexOf(CharSequence subject, int minIndex, int maxIndex) {
                int subjectLength = subject.length();

                if (minIndex < 0) minIndex = 0;

                {
                    int limit = subjectLength - this.infixLength;
                    if (maxIndex > limit) maxIndex = limit;
                }

                while (maxIndex >= minIndex) {

                    int delta = this.deltas.get(subject.charAt(maxIndex));
                    if (delta == -1) {
                        maxIndex -= this.infixLength;
                        continue;
                    }

                    maxIndex -= delta;

                    for (int infixIndex = 0, subjectIndex = maxIndex;; subjectIndex++, infixIndex++) {

                        if (infixIndex >= this.infixLength) return maxIndex;
                        if (subjectIndex >= subjectLength)  return -1;

                        if (subject.charAt(subjectIndex) != infix.charAt(infixIndex)) break;
                    }

                    maxIndex--;
                }

                return -1;
            }

            private CharToIntMapping
            computeDeltas(CharSequence keys) {
                int len = keys.length();

                char maxKey = 0;
                for (int i = 0; i < len; i++) {
                    char c = keys.charAt(i);
                    if (c > maxKey) maxKey = c;
                }

                CharToIntMapping result;
                if (maxKey < 256) {

                    // The key characters are relative small, so we can use a super-fast, array-based mapping.
                    result = StringUtil.arrayBasedCharToIntMapping(maxKey);
                } else {

                    result = StringUtil.hashMapCharToIntMapping();
                }

                for (int i = 0; i < len; i++) result.put(keys.charAt(i), i);

                return result;
            }

            @Override public String
            toString() { return "knuthMorrisPratt(\"" + infix + "\")"; }
        };
    }

    /**
     * Optimized version of a {@code Map<Character, Integer>}.
     */
    private
    interface CharToIntMapping {

        /**
         * @return The value that the <var>key</var> maps to, or {@code -1}
         */
        int get(char key);

        /**
         * Maps the given <var>key</var> to the given <var>value</var>, replacing any previously mapped value.
         */
        void put(char key, int value);
    }

    private static CharToIntMapping
    arrayBasedCharToIntMapping(final char maxKey) {

        return new CharToIntMapping() {

            final int[] deltas = new int[maxKey + 1];
            { Arrays.fill(this.deltas, -1); }

            @Override public int
            get(char c) {
                return c < this.deltas.length ? this.deltas[c] : -1;
            }

            @Override public void
            put(char key, int value) { this.deltas[key] = value; }
        };
    }

    private static CharToIntMapping
    hashMapCharToIntMapping() {

        return new CharToIntMapping() {

            final Map<Character, Integer> deltas = new HashMap<Character, Integer>();

            @Override public int
            get(char c) {
                Integer result = this.deltas.get(c);
                return result != null ? result : -1;
            }

            @Override public void
            put(char key, int value) { this.deltas.put(key, value); }
        };
    }

    /**
     * @return The input string, enclosed in double quotes, and special characters replaced with Java escapes
     */
    public static String
    asJavaLiteral(String s) {

        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');

        for (char c : s.toCharArray()) {
            int idx;
            if ((idx = "\r\n\b\t\\".indexOf(c)) != -1) {
                sb.append('\\').append("rnbt\\".charAt(idx));
            } else
            if (c < 32 || c == 127 || c > 255) {
                sb.append(String.format("\\u%04x", (int) c));
            } else
            {
                sb.append(c);
            }
        }

        return sb.append('"').toString();
    }
}
