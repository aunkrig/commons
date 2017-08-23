
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

package de.unkrig.commons.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Extends a {@link Peekerator Peekerator&lt;String>} with various methods that check for string equality or pattern
 * matching.
 * <p>
 *   Notice that {@code null} is a perfectly valid element value, and all methods handle {@code null} values in a
 *   defined manner.
 * </p>
 *
 * @see de.unkrig.commons.text.StringStream
 * @see de.unkrig.commons.text.parser.AbstractParser
 */
public
class StringPeekerator implements Peekerator<String> {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private final Peekerator<String> delegate;

    public
    StringPeekerator(Peekerator<String> delegate) { this.delegate = delegate; }

    /**
     * Equivalent with {@code new StringPeekerator(Peekerators.from(delegate))}.
     */
    public
    StringPeekerator(Iterator<String> delegate) { this.delegate = Peekerators.from(delegate); }

    /**
     * Equivalent with {@code new StringPeekerator(Peekerators.from(delegate))}.
     */
    public
    StringPeekerator(ListIterator<String> delegate) { this.delegate = Peekerators.from(delegate); }

    @Override public boolean
    hasNext() { return this.delegate.hasNext(); }

    @Override public void
    remove() { this.delegate.remove(); }

    @Override public String
    peek() { return this.delegate.peek(); }

    /**
     * Checks whether the next element equals the <var>expected</var> string, but does consusme it.
     *
     * @see ObjectUtil#equals(Object)
     */
    public boolean
    peek(@Nullable String expected) { return ObjectUtil.equals(expected, this.delegate.peek()); }

    /**
     * Checks whether the next element equals (ignoring case) the <var>expected</var> string, but does not consume it.
     *
     * @see StringUtil#equalsIgnoreCase(String, String)
     */
    public boolean
    peekIgnoreCase(String expected) { return StringUtil.equalsIgnoreCase(expected, this.delegate.peek()); }

    /**
     * Matches the next element against the given <var>pattern</var>, and returns the successful matcher.
     *
     * @return {@code null} iff the next element is {@code null}, or if it does not match the <var>pattern</var>
     */
    @Nullable public Matcher
    peek(Pattern pattern) {

        String actual = this.delegate.peek();
        if (actual == null) return null;

        Matcher m = pattern.matcher(actual);
        return m.matches() ? m : null;
    }

    @Override @Nullable public String
    next() { return this.delegate.next(); }

    /**
     * Consumes the next element iff it equals the <var>expected</var> string.
     *
     * @return                                Whether the next element equals the <var>expected</var> string
     * @see ObjectUtil#equals(Object, Object)
     */
    public boolean
    nextIfEquals(@Nullable String expected) {
        boolean result = ObjectUtil.equals(this.delegate.peek(), expected);
        if (result) this.delegate.next();
        return result;
    }

    /**
     * Consumes the next element iff it equals (ignoring case) the <var>expected</var> string.
     *
     * @return                                          Whether the next element equals the <var>expected</var> string
     * @see StringUtil#equalsIgnoreCase(String, String)
     */
    public boolean
    nextIfEqualsIgnoreCase(@Nullable String expected) {
        boolean result = StringUtil.equalsIgnoreCase(this.delegate.peek(), expected);
        if (result) this.delegate.next();
        return result;
    }

    /**
     * Consumes the next element iff it equals any of the <var>expected</var> strings.
     *
     * @return                        The index of the first equal string, or -1
     * @see ObjectUtil#equals(Object)
     */
    public int
    nextIfEquals(String... expected) {

        String actual = this.delegate.peek();

        for (int i = 0; i < expected.length; i++) {
            if (ObjectUtil.equals(expected[i], actual)) {
                this.delegate.next();
                return i;
            }
        }

        return -1;
    }

    /**
     * Consumes the next element iff it equals (ignoring case) any of the <var>expected</var> strings.
     *
     * @return                                          The index of the first equal string, or -1
     * @see StringUtil#equalsIgnoreCase(String, String)
     */
    public int
    nextIfEqualsIgnoreCase(String... expected) {

        String actual = this.delegate.peek();

        for (int i = 0; i < expected.length; i++) {
            if (StringUtil.equalsIgnoreCase(expected[i], actual)) {
                this.delegate.next();
                return i;
            }
        }

        return -1;
    }

    /**
     * Consumes the next element iff it matches the given <var>pattern</var>.
     *
     * @return {@code null} if the next element is {@code null}, or does not match the given <var>pattern</var>
     */
    @Nullable public Matcher
    nextIfMatches(Pattern pattern) {

        String actual = this.delegate.peek();
        if (actual == null) return null;

        Matcher m = pattern.matcher(actual);
        if (!m.matches()) return null;

        this.delegate.next();
        return m;
    }

    /**
     * Verifies that the next element equals the <var>expected</var> string, and consumes it.
     *
     * @throws NoSuchElementException         This peekerator has no more elements
     * @throws NoSuchElementException         The next element does equal the <var>expected</var> string
     * @see ObjectUtil#equals(Object, Object)
     */
    public void
    nextEquals(String expected) {

        String actual = this.delegate.next();

        if (!ObjectUtil.equals(actual, expected)) {
            throw new NoSuchElementException("Expected '" + expected + "' instead of '" + actual + "'");
        }
    }

    /**
     * Verifies that the next element equals (ignoring case) the <var>expected</var> string, and consumes it.
     *
     * @throws NoSuchElementException                   This peekerator has no more elements
     * @throws NoSuchElementException                   The next element does equal the <var>expected</var> string
     * @see StringUtil#equalsIgnoreCase(String, String)
     */
    public void
    nextEqualsIgnoreCase(String expected) {

        String actual = this.delegate.next();

        if (!StringUtil.equalsIgnoreCase(actual, expected)) {
            throw new NoSuchElementException("Expected '" + expected + "' instead of '" + actual + "'");
        }
    }

    /**
     * Verifies that the next element equals any of the <var>expected</var> strings, and consumes it.
     *
     * @return                                The index of the first string that equals the next element
     * @throws NoSuchElementException         This peekerator has no more elements
     * @throws NoSuchElementException         The next element does equal any of the <var>expected</var> strings
     * @see ObjectUtil#equals(Object, Object)
     */
    public int
    nextEquals(String... expected) {

        String actual = this.delegate.next();

        for (int i = 0; i < expected.length; i++) {
            if (ObjectUtil.equals(actual, expected[i])) return i;
        }

        throw new NoSuchElementException(
            "Expected one of "
            + Arrays.toString(expected)
            + " instead of \""
            + actual
            + "\""
        );
    }

    /**
     * Verifies that the next element equals (ignoring case) any of the the <var>expected</var> string, and consumes it.
     *
     * @return                                          The index of the first string that equals the next element
     * @throws NoSuchElementException                   This peekerator has no more elements
     * @throws NoSuchElementException                   The next element does equal any of the <var>expected</var>
     *                                                  strings
     * @see StringUtil#equalsIgnoreCase(String, String)
     */
    public int
    nextEqualsIgnoreCase(String... expected) {

        String actual = this.delegate.next();

        for (int i = 0; i < expected.length; i++) {
            if (StringUtil.equalsIgnoreCase(actual, expected[i])) return i;
        }

        throw new NoSuchElementException(
            "Expected one of "
            + Arrays.toString(expected)
            + " instead of \""
            + actual
            + "\""
        );
    }

    /**
     * Verifies that the next element matches the given <var>pattern</var>.
     *
     * @throws NoSuchElementException This peekerator has no more elements
     * @throws NoSuchElementException The next element is {@code null}
     * @throws NoSuchElementException The next element does match the <var>pattern</var>
     */
    public void
    nextMatches(Pattern pattern) {

        String actual = this.delegate.next();

        if (actual == null) throw new NoSuchElementException("null");

        if (!pattern.matcher(actual).matches()) {
            throw new NoSuchElementException("Expected '" + pattern + "' instead of '" + actual + "'");
        }
    }

    /**
     * Consumes and returns all remaining elements.
     */
    public String[]
    rest() {

        List<String> result = new ArrayList<String>();
        while (this.delegate.hasNext()) result.add(this.delegate.next());

        return result.toArray(new String[result.size()]);
    }
}
