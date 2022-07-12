
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
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
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
     * @return A string consisting of <var>n</var> times the character sequence <var>chars</var>
     */
    public static String
    repeat(int n, char[] chars) {

        if (n == 0) return "";
        int len = chars.length;
        if (len == 0) return "";
        if (len == 1) return StringUtil.repeat(n, chars[0]);

        char[] ca = new char[n * len];
        for (int i = 0, o = 0; i < n; i++) {
            for (int j = 0; j < len; j++) ca[o++] = chars[j];
        }
        return String.copyValueOf(ca);
    }

    /**
     * @return A string consisting of <var>n</var> times the <var>cs</var>
     */
    public static String
    repeat(int n, CharSequence cs) {

        if (n <= 0) return "";
        if (n == 1) return cs.toString();

        int    len = cs.length();
        char[] src = CharSequences.toCharArray(cs);
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
         *   StringUtil.newIndexOf(needle).indexOf(haystack) == haystack.indexOf(needle)
         * </pre>
         *
         * @return {@code 0} ... {@code haystack.length() - needle.length()}, or {@code -1}
         */
        int indexOf(CharSequence haystack);

        /**
         * The equivalent of {@link String#indexOf(String, int)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(needle).indexOf(haystack, minIndex) == haystack.indexOf(needle, minIndex)
         * </pre>
         *
         * @return {@code max(0, minIndex)} ... {@code haystack.length() - needle.length()}, or {@code -1}
         */
        int indexOf(CharSequence haystack, int minIndex);

        /**
         * Like {@link #indexOf(CharSequence, int)}, but the match terminates at index <var>maxIndex</var> (inclusive).
         *
         * @return {@code max(0, minIndex)} ... {@code min(maxIndex, haystack.length() - needle.length())}, or {@code
         *         -1}
         */
        int indexOf(CharSequence haystack, int minIndex, int maxIndex);

        /**
         * Like {@link #indexOf(CharSequence, int, int)}, but never accesses <var>haystack</var> chars at position
         * <var>limit</var> or greater.
         * Instead, if there are <em>partial matches</em> at positions <var>limit</var> {@code -}
         * <var>needle</var>{@code .length - 1} ... <var>limit</var> {@code - 1}, then the <em>first</em> of these is
         * returned.
         * <p>
         *   Examples:
         * </p>
         * <table border="1">
         *   <tr>
         *     <th>needle</th>
         *     <th>haystack</th>
         *     <th>minIndex</th>
         *     <th>maxIndex</th>
         *     <th>limit</th>
         *     <th>result</th>
         *   </tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code _ABCDE_**}</td><td>&lt;=1</td><td>&gt;=1</td><td>7</td><td>1</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code __ABCDE**}</td><td>&lt;=2</td><td>&gt;=2</td><td>7</td><td>2</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code ___ABCD**}</td><td>&lt;=3</td><td>&gt;=3</td><td>7</td><td>3</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code ____ABC**}</td><td>&lt;=4</td><td>&gt;=4</td><td>7</td><td>4</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code _____AB**}</td><td>&lt;=5</td><td>&gt;=5</td><td>7</td><td>5</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code ______A**}</td><td>&lt;=6</td><td>&gt;=6</td><td>7</td><td>6</td></tr>
         *   <tr><td>{@code ABCDE}</td><td>{@code _______**}</td><td>&lt;=7</td><td>&gt;=7</td><td>7</td><td>7</td></tr>
         * </table>
         * <p>
         *   ("{@code _}": Any character but "{@code ABCDE}"; "{@code *}": Any character.)
         * </p>
         *
         * @param limit 0...<var>haystack</var>{@code .length}
         */
        int indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit);

        /**
         * The equivalent of {@link String#lastIndexOf(String)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(needle).lastIndexOf(haystack) == haystack.lastIndexOf(needle)
         * </pre>
         *
         * @return {@code 0} ... {@code haystack.length() - needle.length()}, or {@code -1}
         */
        int lastIndexOf(CharSequence haystack);

        /**
         * The equivalent of {@link String#lastIndexOf(String, int)}.
         * <p>
         *   For the return value, the following condition holds true:
         * </p>
         * <pre>
         *   StringUtil.newIndexOf(needle).lastIndexOf(haystack, maxIndex) == haystack.lastIndexOf(needle, maxIndex)
         * </pre>
         *
         * @return {@code 0} ... {@code min(maxIndex, haystack.length() - needle.length())}, or {@code -1}
         */
        int lastIndexOf(CharSequence haystack, int maxIndex);

        /**
         * Like {@link #lastIndexOf(CharSequence, int)}, but the match terminates at index <var>minIndex</var>
         * (inclusive).
         *
         * @return {@code max(0, minIndex)} ... {@code min(maxIndex, haystack.length() - needle.length())}, or {@code
         *         -1}
         */
        int lastIndexOf(CharSequence haystack, int minIndex, int maxIndex);

        /**
         * @return A textual representation of the needle and the search algorithm
         */
        @Override String toString();
    }

    /**
     * @see #indexOf(CharSequence, BitSet, int, int, int)
     */
    public
    interface MultiNeedleIndexOf {

        /**
         * Equivalent with {@link #startsWith(CharSequence, BitSet, int, int) startsWith}{@code (haystack, new BitSet(),
         * 0, haystack.length())}.
         */
        boolean startsWith(CharSequence haystack);

        /**
         * Equivalent with {@link #startsWith(CharSequence, BitSet, int, int) startsWith}{@code (haystack,
         * matchingNeedleIndices, 0, haystack.length())}.
         */
        boolean startsWith(CharSequence haystack, BitSet matchingNeedleIndices);

        /**
         * Equivalent with {@link #startsWith(CharSequence, BitSet, int, int) startsWith}{@code (haystack,
         * matchingNeedleIndices, offset, haystack.length())}.
         */
        boolean startsWith(CharSequence haystack, BitSet matchingNeedleIndices, int offset);

        /**
         * Checks whether one or more of the needles matches the <var>haystack</var> at position <var>offset</var>. If
         * so, then the indices of the matching needles are set in the <var>matchingNeedleIndices</var> bit set.
         *
         * @param limit Never look beyond this limit in the <var>haystack</var>
         */
        boolean startsWith(CharSequence haystack, BitSet matchingNeedleIndices, int offset, int limit);

        /**
         * Equivalent with {@link #indexOf(CharSequence, BitSet, int, int, int) indexOf}{@code (haystack, new BitSet(),
         * 0, haystack.length(), haystack.length())}.
         */
        int indexOf(CharSequence haystack);

        /**
         * Equivalent with {@link #indexOf(CharSequence, BitSet, int, int, int) indexOf}{@code (haystack,
         * matchingNeedleIndices, 0, haystack.length(), haystack.length())}.
         */
        int indexOf(CharSequence haystack, BitSet matchingNeedleIndices);

        /**
         * Equivalent with {@link #indexOf(CharSequence, BitSet, int, int, int) indexOf}{@code (haystack,
         * matchingNeedleIndices, minIndex, haystack.length(), haystack.length())}.
         */
        int indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex);

        /**
         * Equivalent with {@link #indexOf(CharSequence, BitSet, int, int, int) indexOf}{@code (haystack,
         * matchingNeedleIndices, minIndex, maxIndex, haystack.length())}.
         */
        int indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex, int maxIndex);

        /**
         * Finds the first match of any of the needles in the <var>haystack</var>, starting at position
         * <var>minIndex</var>, and stopping at position <var>maxIndex</var> (inclusive). If there is such a match,
         * then it sets the indices of the matching needles in the <var>matchingNeedleIndices</var> bit set, and
         * returns the position of the match.
         *
         * @param limit Never look beyond this limit in the <var>haystack</var>
         * @return      -1 iff there is no match under the given conditions
         */
        int indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex, int maxIndex, int limit);
    }

    abstract static
    class AbstractMultiNeedleIndexOf implements MultiNeedleIndexOf {

        // Default implementations of "startsWith()".

        @Override public boolean
        startsWith(CharSequence haystack) {
            return this.startsWith(haystack, new BitSet() /*matchingNeedleIndices*/, 0 /*offset*/, haystack.length() /*limit*/);
        }

        @Override public boolean
        startsWith(CharSequence haystack, BitSet matchingNeedleIndices) {
            return this.startsWith(haystack, matchingNeedleIndices, 0 /*offset*/, haystack.length() /*limit*/);
        }

        @Override public boolean startsWith(CharSequence haystack, BitSet matchingNeedleIndices, int offset) {
            return this.startsWith(haystack, matchingNeedleIndices, offset, haystack.length() /*limit*/);
        }

        // Default implementations of "indexOf()".

        @Override public int
        indexOf(CharSequence haystack) {
            return this.indexOf(haystack, new BitSet() /*matchingNeedleIndices*/, 0 /*minIndex*/, haystack.length() /*maxIndex*/, haystack.length() /*limit*/);
        }
        @Override public int
        indexOf(CharSequence haystack, BitSet matchingNeedleIndices) {
            return this.indexOf(haystack, matchingNeedleIndices, 0 /*minIndex*/, haystack.length() /*maxIndex*/, haystack.length() /*limit*/);
        }
        @Override public int
            indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex) {
            return this.indexOf(haystack, matchingNeedleIndices, minIndex, haystack.length() /*maxIndex*/, haystack.length() /*limit*/);
        }
        @Override public int
        indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex, int maxIndex) {
            return this.indexOf(haystack, matchingNeedleIndices, minIndex, maxIndex, haystack.length() /*limit*/);
        }

        @Override
        public abstract String toString();
    }

    /**
     * Runtime-optimized reimplementation of {@link String#indexOf(String)} and {@link String#lastIndexOf(String)}.
     * <p>
     *   This method returns an implementation that performs at least as well as the {@link String} methods by
     *   analyzing the <var>needle</var> (the string to search for).
     * </p>
     *
     * @param needle The string to search for
     */
    public static IndexOf
    indexOf(char[] needle) { return StringUtil.indexOf(CharSequences.from(needle)); }

    /**
     * Runtime-optimized reimplementation of {@link String#indexOf(String)} and {@link String#lastIndexOf(String)}.
     * <p>
     *   This method returns an implementation that performs at least as well as the {@link String} methods by
     *   analyzing the <var>needle</var> (the string to search for).
     * </p>
     *
     * @param needle The string to search for
     */
    public static IndexOf
    indexOf(final CharSequence needle) {

        if (needle.length() < 16) {
            return StringUtil.naiveIndexOf(needle);
        } else {
            return StringUtil.boyerMooreHorspoolIndexOf(needle);
        }
    }

    private abstract static
    class AbstractIndexOf implements IndexOf {
        @Override public int indexOf(CharSequence haystack)                   { return this.indexOf(haystack, 0, Integer.MAX_VALUE);        } // SUPPRESS CHECKSTYLE LineLength|Align:3
        @Override public int indexOf(CharSequence haystack, int minIndex)     { return this.indexOf(haystack, minIndex, Integer.MAX_VALUE); }
        @Override public int lastIndexOf(CharSequence haystack)               { return this.lastIndexOf(haystack, 0, Integer.MAX_VALUE);    }
        @Override public int lastIndexOf(CharSequence haystack, int maxIndex) { return this.lastIndexOf(haystack, 0, maxIndex);             }

        @Override public int indexOf(CharSequence haystack, int minIndex, int maxIndex)            { throw new UnsupportedOperationException(); } // SUPPRESS CHECKSTYLE LineLength:2
        @Override public int indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit) { throw new UnsupportedOperationException(); }
        @Override public int lastIndexOf(CharSequence haystack, int minIndex, int maxIndex)        { throw new UnsupportedOperationException(); }

        @Override public abstract String toString();
    }

    /**
     * @return A wrapper for {@link String#indexOf(String)} and {@link String#lastIndexOf(String)}, which implements
     *         a naive string search algorithm
     */
    public static IndexOf
    naiveIndexOf(final CharSequence needle) {

        final String needle2 = needle.toString();

        return new AbstractIndexOf() {

            final int needleLength = needle.length();

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex) {

                if (maxIndex >= haystack.length() - this.needleLength) {
                    return haystack.toString().indexOf(needle2, minIndex);
                }

                return haystack.toString().substring(0, maxIndex + this.needleLength).indexOf(needle2, minIndex);
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit) {
                throw new UnsupportedOperationException();
            }

            @Override public int
            lastIndexOf(CharSequence haystack, int minIndex, int maxIndex) {

                if (minIndex <= 0) return haystack.toString().lastIndexOf(needle2, maxIndex);

                haystack =  haystack.subSequence(minIndex, haystack.length());
                maxIndex -= minIndex;

                int result = haystack.toString().lastIndexOf(needle2, maxIndex);
                return result == -1 ? -1 : result + minIndex;
            }

            @Override public String
            toString() { return "naive(" + PrettyPrinter.toJavaStringLiteral(needle) + ")"; }
        };
    }

    /**
     * Implementation of the Boyer-Moore-Horspool string search algorithm.
     *
     * @param needle The string to search for
     * @see          <a href="https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore%E2%80%93Horspool_algorithm">The
     *               Boyer–Moore–Horspool algorithm</a>
     */
    public static IndexOf
    boyerMooreHorspoolIndexOf(final CharSequence needle) {

        return new AbstractIndexOf() {

            final int needleLength = needle.length();

            /**
             * The "Safe-skip" table for {@link #indexOf()}.
             */
            final int[] safeSkip1 = new int[256];

            /**
             * The "Safe-skip" table for {@link #lastIndexOf(CharSequence)}.
             */
            final int[] safeSkip2 = new int[256];

            {
                Arrays.fill(this.safeSkip1, this.needleLength);
                Arrays.fill(this.safeSkip2, this.needleLength);
                int nl1 = this.needleLength - 1;
                for (int i = 0; i < this.needleLength; i++) {
                    int ss  = nl1 - i;
                    int ss2 = ss == 0 ? 1 : ss;
                    this.safeSkip1[0xff & needle.charAt(i)]  = ss2;
                    this.safeSkip2[0xff & needle.charAt(ss)] = ss2;
                }
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex) {

                final int  nl1      = this.needleLength - 1;
                final char lastChar = needle.charAt(nl1);

                final int limit = maxIndex >= haystack.length() - nl1 ? haystack.length() - 1 : maxIndex + nl1;
                for (int o = minIndex <= 0 ? nl1 : minIndex + nl1; o <= limit;) {

                    char c = haystack.charAt(o);
                    if (c != lastChar) {
                        o += this.safeSkip1[0xff & c];
                        continue;
                    }

                    if (nl1 == 0) return o;

                    int o2 = o - 1;
                    for (int ni = nl1 - 1;; ni--, o2--) {
                        if (haystack.charAt(o2) != needle.charAt(ni)) break;
                        if (ni == 0) return o2;
                    }

                    o++;
                }

                return -1;
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit) {

                if (maxIndex <= limit - this.needleLength) return this.indexOf(haystack, minIndex, maxIndex);

                int result = this.indexOf(haystack, minIndex, limit - this.needleLength);
                if (result != -1) return result;

                int nl1 = this.needleLength - 1;

                int minResult = Integer.MIN_VALUE, maxResult = Integer.MAX_VALUE;
                for (int o = limit - 1; o > limit - this.needleLength; o--) {
                    char c = haystack.charAt(o);

                    int ss1 = this.safeSkip1[0xff & c];
                    if (ss1 == this.needleLength) {
                        return -1;
                    }
                    int minOffset = o + ss1 - nl1;
                    if (minOffset > minResult) minResult = minOffset;

                    int maxOffset;
                    if (c == needle.charAt(0)) {
                        maxOffset = o;
                    } else {
                        int ss2 = this.safeSkip2[0xff & c];
                        maxOffset = o - ss2;
                    }
                    if (maxOffset < maxResult) maxResult = maxOffset;

                    if (maxResult == minResult) break;
                }
                if (minResult < minIndex) minResult = minIndex;
                if (maxResult > maxIndex) maxResult = maxIndex;

                for (int i = minResult; i <= maxResult; i++) {
                    M: {
                        for (int j = 0, i2 = i; j < needle.length(); j++, i2++) {
                            if (i2 == limit) return i;
                            if (needle.charAt(j) != haystack.charAt(i2)) break M;
                        }
                        return i;
                    }
                }

                return -1;
            }

            @Override public int
            lastIndexOf(CharSequence haystack, int minIndex, int maxIndex) {

                final int  nl        = this.needleLength;
                final char firstChar = needle.charAt(0);

                final int limit = haystack.length() - nl;
                for (int o = maxIndex <= limit ? maxIndex : limit; o >= minIndex;) {

                    char c = haystack.charAt(o);
                    if (c != firstChar) {
                        o -= this.safeSkip2[0xff & c];
                        continue;
                    }

                    if (nl == 1) return o;

                    int o2 = o + 1;
                    for (int ii = 1;; ii++, o2++) {
                        if (ii >= nl) return o;
                        if (haystack.charAt(o2) != needle.charAt(ii)) break;
                    }

                    o--;
                }

                return -1;
            }

            @Override public String
            toString() { return "boyerMooreHorspool(" + PrettyPrinter.toJavaStringLiteral(needle) + ")"; }
        };
    }

    /**
     * Creates a highly optimized {@link IndexOf} object that searches for "multivalent" <var>needle</var>.
     * Multivalent means that a character in the haystack equals <em>any of</em> {@code needle[offset]}.
     * <p>
     *   E.g. a case-insensitive search for {@code "abc"} in a {@code haystack} string could be implemented like
     *   this:
     * </p>
     * <pre>
     *   int idx = StringUtil.indexOf(new char[][] { { 'a', 'A' }, { 'b', 'B' }, { 'c', 'C' } }).indexOf(haystack);
     * </pre>
     */
    public static IndexOf
    indexOf(char[][] needle) {
        int len = needle.length;

        UNIVALENT_NEEDLE: {
            char[] univalentNeedle = new char[len];
            for (int i = 0; i < len; i++) {
                if (needle[i].length != 1) break UNIVALENT_NEEDLE;
                univalentNeedle[i] = needle[i][0];
            }

            return StringUtil.indexOf(univalentNeedle);
        }

        return (
            len < 3
            ? StringUtil.naiveIndexOf(needle)
            : StringUtil.boyerMooreHorspoolIndexOf(needle)
        );
    }

    private static IndexOf
    naiveIndexOf(final char[][] needle) {

        return new AbstractIndexOf() {

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex) {

                if (minIndex < 0) minIndex = 0;
                if (maxIndex > haystack.length() - needle.length) maxIndex = haystack.length() - needle.length;

                CHARS: for (; minIndex <= maxIndex; minIndex++) {

                    int o = minIndex;
                    for (char[] n : needle) {
                        char c = haystack.charAt(o++);
                        NC: {
                            for (char nc : n) {
                                if (c == nc) break NC;
                            }
                            continue CHARS;
                        }
                    }

                    return minIndex;
                }

                return -1;
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit) {
                throw new UnsupportedOperationException();
            }

            @Override public int
            lastIndexOf(CharSequence haystack, int minIndex, int maxIndex) {

                if (minIndex < 0) minIndex = 0;
                if (maxIndex > haystack.length() - needle.length) maxIndex = haystack.length() - needle.length;

                CHARS: for (; maxIndex >= minIndex; maxIndex++) {

                    int o = maxIndex;
                    for (char[] n : needle) {
                        char c = haystack.charAt(o++);
                        NC: {
                            for (char nc : n) {
                                if (c == nc) break NC;
                            }
                            continue CHARS;
                        }
                    }

                    return maxIndex;
                }

                return -1;
            }

            @Override public String
            toString() { return "naiveIndexOf(" + PrettyPrinter.toJavaArrayInitializer(needle) + ")"; }
        };
    }

    /**
     * Implementation of the Boyer-Moore-Horspool string search algorithm for multiple needles.
     */
    public static MultiNeedleIndexOf
    boyerMooreHorspoolIndexOf(CharSequence[] needles) {

        return new AbstractMultiNeedleIndexOf() {

            private int shortestNeedleLength;
            {
                this.shortestNeedleLength = Integer.MAX_VALUE;
                for (CharSequence needle : needles) {
                    int nl = needle.length();
                    if (nl < this.shortestNeedleLength) this.shortestNeedleLength = nl;
                }
            }

            /**
             * Element value >= 1: Pivot offset can safely be incremented by this value
             * Element value <= 0: The first possible match is at (pivot offset) - |value|
             */
            private final int[] safeSkipTable = new int[256];
            {
                for (int i = 0; i < 256; i++) this.safeSkipTable[i] = this.shortestNeedleLength - 1;

                for (CharSequence needle : needles) {
                    for (int offset = 0; offset < this.shortestNeedleLength; offset++) {
                        char c = needle.charAt(offset);
                        if (offset > -this.safeSkipTable[255 & c]) {
                            this.safeSkipTable[255 & c] = -offset;
                        }
                    }
                }
            }

            @Override public boolean
            startsWith(CharSequence haystack, BitSet matchingNeedleIndices, int offset, int limit) {

                boolean result = false;

                NEXT_NEEDLE: for (int needleIndex = 0; needleIndex < needles.length; needleIndex++) {
                    CharSequence needle = needles[needleIndex];
                    if (offset + needle.length() > limit) continue;
                    NEXT_OFFSET: for (int offset2 = 0; offset2 < needle.length(); offset2++) {
                        if (needle.charAt(offset2) != haystack.charAt(offset + offset2)) continue NEXT_NEEDLE;
                    }
                    matchingNeedleIndices.set(needleIndex);
                    result = true;
                }

                return result;
            }

            @Override public int
            indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex, int maxIndex, int limit) {

                assert limit <= haystack.length();

                if (minIndex + this.shortestNeedleLength > limit) return -1;

                int pivot;
                for (pivot = minIndex + this.shortestNeedleLength - 1;;) {

                    // Step one: Compute the smallest possible match offset, using the Boyer-Moore-Horspool algorithm.
                    int firstPossibleMatchOffset;
                    for (;;) {

                        if (pivot >= limit) return -1;

                        int x = this.safeSkipTable[255 & haystack.charAt(pivot)];
                        if (x >= 1) {
                            pivot += x;
                        } else {
                            firstPossibleMatchOffset = pivot + x;
                            break;
                        }
                    }

                    if (firstPossibleMatchOffset > maxIndex) return -1;

                    // Step two: Check if any of the needle matches at that position. If so, return the first match.
                    if (this.startsWith(haystack, matchingNeedleIndices, firstPossibleMatchOffset, limit)) {
                        return firstPossibleMatchOffset;
                    }

                    // Step 3: Avance the pivot offset.
                    pivot += this.shortestNeedleLength;
                }
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("boyerMooreHorspool(");
                if (needles.length > 0) {
                    for (int ni = 0;;) {
                        sb.append(needles[ni]);
                        if (++ni >= needles.length) break;
                        sb.append('|');
                    }
                }
                sb.append(')');
                return sb.toString();
            }
        };
    }

    /**
     * Implementation of the Boyer-Moore-Horspool string search algorithm.
     * <p>
     *   Notice that the {@link #indexOf(char[][])} method performs some extra optimizations, e.g. when the needle
     *   is actually univalent (all of {@code needle[n].length} are 1), or when the needle is very short.
     * </p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore%E2%80%93Horspool_algorithm">The
     *      Boyer–Moore–Horspool algorithm</a>
     */
    public static IndexOf
    boyerMooreHorspoolIndexOf(final char[][] needle) {

        boolean univalent = true;
        for (int i = needle.length - 1; i >= 0; i--) {

            char[] n = StringUtil.removeDuplicates(needle[i]);

            if (n.length != 1) univalent = false;

            needle[i] = n;
        }

        if (univalent) {
            char[] univalentNeedle = new char[needle.length];
            for (int i = needle.length - 1; i >= 0; i--) univalentNeedle[i] = needle[i][0];
            return StringUtil.boyerMooreHorspoolIndexOf(String.valueOf(univalentNeedle));
        }

        return new AbstractIndexOf() {

            final int needleLength = needle.length;

            /**
             * The "Safe-skip" table for {@link #indexOf()}.
             */
            final int[] safeSkip1 = new int[256];

            /**
             * The "Safe-skip" table for {@link #lastIndexOf(CharSequence)}.
             */
            final int[] safeSkip2 = new int[256];

            {
                Arrays.fill(this.safeSkip1, this.needleLength);
                Arrays.fill(this.safeSkip2, this.needleLength);
                int nl1 = this.needleLength - 1;
                for (int i = 0; i < this.needleLength; i++) {
                    for (char c : needle[i]) this.safeSkip1[0xff & c] = nl1 - i;
                }
                for (int i = nl1; i >= 0; i--) {
                    for (char c : needle[i]) this.safeSkip2[0xff & c] = i;
                }
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex) {

                final int    nl1       = this.needleLength - 1;
                final char[] lastChars = needle[nl1];

                final int limit = maxIndex >= haystack.length() - nl1 ? haystack.length() - 1 : maxIndex + nl1;
                for (int o = minIndex <= 0 ? nl1 : minIndex + nl1; o <= limit;) {

                    char c = haystack.charAt(o);
                    LC:
                    switch (lastChars.length) {

                    case 1:
                        if (c == lastChars[0]) break;
                        o += this.safeSkip1[0xff & c];
                        continue;

                    case 2:
                        if (c == lastChars[0] || c == lastChars[1]) break;
                        o += this.safeSkip1[0xff & c];
                        continue;

                    case 3:
                        if (c == lastChars[0] || c == lastChars[1] || c == lastChars[2]) break;
                        o += this.safeSkip1[0xff & c];
                        continue;

                    default:
                        for (char lc : lastChars) {
                            if (lc == c) break LC;
                        }
                        o += this.safeSkip1[0xff & c];
                        continue;
                    }

                    if (nl1 == 0) return o;

                    int o2 = o - 1;
                    for (int ni = nl1 - 1;; ni--, o2--) {

                        c = haystack.charAt(o2);
                        NC: {
                            for (char nc : needle[ni]) {
                                if (c == nc) break NC;
                            }
                            break;
                        }
                        if (ni == 0) return o2;
                    }

                    o++;
                }

                return -1;
            }

            @Override public int
            indexOf(CharSequence haystack, int minIndex, int maxIndex, int limit) {
                throw new UnsupportedOperationException();
            }

            @Override public int
            lastIndexOf(CharSequence haystack, int minIndex, int maxIndex) {

                final int    nl         = this.needleLength;
                final char[] firstChars = needle[0];

                final int limit = haystack.length() - nl;
                for (int o = maxIndex <= limit ? maxIndex : limit; o >= minIndex;) {

                    char c = haystack.charAt(o);
                    FC:
                    switch (firstChars.length) {

                    case 1:
                        if (c == firstChars[0]) break;
                        o += this.safeSkip2[0xff & c];
                        continue;

                    case 2:
                        if (c == firstChars[0] || c == firstChars[1]) break;
                        o += this.safeSkip2[0xff & c];
                        continue;

                    case 3:
                        if (c == firstChars[0] || c == firstChars[1] || c == firstChars[2]) break;
                        o += this.safeSkip2[0xff & c];
                        continue;

                    default:
                        for (char fc : firstChars) {
                            if (fc == c) break FC;
                        }
                        o += this.safeSkip2[0xff & c];
                        continue;
                    }

                    if (nl == 1) return o;

                    int o2 = o + 1;
                    for (int ni = 1;; ni++, o2++) {

                        if (ni >= nl) return o;

                        c = haystack.charAt(o2);
                        NC: {
                            for (char nc : needle[ni]) {
                                if (c == nc) break NC;
                            }
                            break;
                        }
                    }

                    o--;
                }

                return -1;
            }

            @Override public String
            toString() { return "boyerMooreHorspool(" + PrettyPrinter.toJavaArrayInitializer(needle) + ")"; }
        };
    }

    /**
     * @param needle {@code [needleIndex][offset][multivalence]}
     */
    public static MultiNeedleIndexOf
    boyerMooreHorspoolIndexOf(final char[][][] needles) {

        // Check whether all needles are univalent, i.e. needles[needleIndex][offset].length == 1
        OPTIMIZE_UNIVALENT: {
            for (char[][] needle : needles) {
                for (int i = needle.length - 1; i >= 0; i--) {

                    char[] n = StringUtil.removeDuplicates(needle[i]);
                    needle[i] = n;

                    if (n.length != 1) break OPTIMIZE_UNIVALENT;
                }
            }

            // Because all needles are univalent, we can delegate to the (probably faster)
            // "boyerMoorHorspoolIndexOf(CharSequence[])".
            CharSequence[] univalentNeedles = new CharSequence[needles.length];
            for (int i = needles.length - 1; i >= 0; i--) {
                char[][] needle = needles[i];

                char[] un = new char[needle.length];
                for (int j = needle.length - 1; j >= 0; j--) un[j] = needle[j][0];
                univalentNeedles[i] = String.valueOf(un);
            }
            return StringUtil.boyerMooreHorspoolIndexOf(univalentNeedles);
        }

        return new AbstractMultiNeedleIndexOf() {

            private int shortestNeedleLength;
            {
                this.shortestNeedleLength = Integer.MAX_VALUE;
                for (char[][] needle : needles) {
                    int nl = needle.length;
                    if (nl < this.shortestNeedleLength) this.shortestNeedleLength = nl;
                }
            }

            /**
             * Element value >= 1: Pivot offset can safely be incremented by this value
             * Element value <= 0: The first possible match is at (pivot offset) - |value|
             */
            private final int[] safeSkipTable = new int[256];
            {
                for (int i = 0; i < 256; i++) this.safeSkipTable[i] = this.shortestNeedleLength - 1;

                for (char[][] needle : needles) {
                    for (int offset = 0; offset < this.shortestNeedleLength; offset++) {
                        for (char c : needle[offset]) {
                            if (offset > -this.safeSkipTable[255 & c]) {
                                this.safeSkipTable[255 & c] = -offset;
                            }
                        }
                    }
                }
            }

            @Override public boolean
            startsWith(CharSequence haystack, BitSet matchingNeedleIndices, int offset, int limit) {

                boolean result = false;

                NEXT_NEEDLE: for (int needleIndex = 0; needleIndex < needles.length; needleIndex++) {
                    char[][] needle = needles[needleIndex];
                    if (offset + needle.length > limit) continue;
                    NEXT_OFFSET: for (int offset2 = 0; offset2 < needle.length; offset2++) {
                        char haystackChar = haystack.charAt(offset + offset2);
                        for (char c : needle[offset2]) {
                            if (c == haystackChar) continue NEXT_OFFSET;
                        }
                        continue NEXT_NEEDLE;
                    }
                    matchingNeedleIndices.set(needleIndex);
                    result = true;
                }

                return result;
            }

            @Override public int
            indexOf(CharSequence haystack, BitSet matchingNeedleIndices, int minIndex, int maxIndex, int limit) {

                assert limit <= haystack.length();

                if (minIndex + this.shortestNeedleLength > limit) return -1;

                int pivot;
                for (pivot = minIndex + this.shortestNeedleLength - 1;;) {

                    // Step one: Compute the smallest possible match offset, using the Boyer-Moore-Horspool algorithm.
                    int firstPossibleMatchOffset;
                    for (;;) {

                        if (pivot >= limit) return -1;

                        int x = this.safeSkipTable[255 & haystack.charAt(pivot)];
                        if (x >= 1) {
                            pivot += x;
                        } else {
                            firstPossibleMatchOffset = pivot + x;
                            break;
                        }
                    }

                    if (firstPossibleMatchOffset > maxIndex) return -1;

                    // Step two: Check if any of the needle matches at that position. If so, return the first match.
                    if (this.startsWith(haystack, matchingNeedleIndices, firstPossibleMatchOffset, limit)) {
                        return firstPossibleMatchOffset;
                    }

                    // Step 3: Avance the pivot offset.
                    pivot += this.shortestNeedleLength;
                }
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("boyerMooreHorspool(");
                if (needles.length > 0) {
                    for (int ni = 0;;) {
                        for (char[] o : needles[ni]) sb.append('[').append(o).append(']');
                        if (++ni >= needles.length) break;
                        sb.append('|');
                    }
                }
                sb.append(')');
                return sb.toString();
            }
        };
    }

    // Duplicated from "ArrayUtil".
    private static char[][]
    mirror(char[][] subject) {

        int height = subject.length;
        if (height == 0) return new char[0][];

        int width = subject[0].length;

        char[][] result = new char[width][];

        result[0] = new char[height];
        for (int j = 0; j < height; j++) {
            if (subject[j].length != width) throw new IllegalArgumentException();
            result[0][j] = subject[j][0];
        }

        for (int i = 1; i < width; i++) {
            result[i] = new char[height];
            for (int j = 0; j < height; j++) {
                result[i][j] = subject[j][i];
            }
        }

        return result;
    }

    private static char[]
    removeDuplicates(char[] subject) {

        for (int j = subject.length - 1; j >= 0; j--) {
            for (int k = j - 1; k >= 0; k--) {
                if (subject[k] == subject[j]) {
                    char[] tmp = new char[subject.length - 1];
                    System.arraycopy(subject, 0, tmp, 0, k);
                    System.arraycopy(subject, k + 1, tmp, k, subject.length - 1 - k);
                    j--;
                    k--;
                    subject = tmp;
                }
            }
        }

        return subject;
    }
}
