
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

package de.unkrig.commons.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various enum-related utility methods.
 */
public final
class Enums {

    private Enums() {}

    /**
     * @return The ordinals of all members of the <var>enumSet</var>
     */
    public static Collection<Integer>
    ordinals(Collection<? extends Enum<?>> enumSet) {

        Collection<Integer> result = new ArrayList<Integer>();
        for (Enum<?> e : enumSet) result.add(e.ordinal());

        return result;
    }

    /**
     * Creates an {@link EnumSet} from the given string. The string must have one of the following formats:
     * <dl>
     *   <dt>(Empty)</dt>
     *   <dt><code>'[]'</code></dt>
     *   <dd>Empty enum set</dd>
     *
     *   <dt><code>enum-constant</code></dt>
     *   <dd>Enum set with one element</dd>
     *
     *   <dt><code>'[' enum-constant { ',' enum-constant } ']'</code></dt>
     *   <dd>Enum set with one or more elements</dd>
     * </dl>
     * <p>
     *   <code> enum-constant := java-identifier-start { java-identifier-part }</code>
     * </p>
     * @return A (mutable) {@link EnumSet}
     */
    public static <E extends Enum<E>> EnumSet<E>
    enumSetFromString(String s, Class<E> elementType) {

        int i = 0;

        i = Enums.skipWhitespace(s, i);

        if (i == s.length()) return EnumSet.noneOf(elementType); // Empty string => empty set

        if (s.charAt(i) != '[') return EnumSet.of(Enums.valueOf(s, elementType));
        i = Enums.skipWhitespace(s, i + 1);

        if (s.charAt(i) == ']') {

            // '[]' => empty set
            i = Enums.skipWhitespace(s, i + 1);
            if (i < s.length()) throw new IllegalArgumentException("Trailing garbage");
            return EnumSet.noneOf(elementType);
        }

        // Now let's parse
        //    enum-constant { ',' enum-constant } ']' end-of-input
        for (EnumSet<E> result = EnumSet.noneOf(elementType);;) {

            {
                int j = Enums.scanIdentifier(s, i);
                result.add(Enums.valueOf(s.substring(i, j), elementType));
                i = Enums.skipWhitespace(s, j);
            }

            if (i == s.length()) throw new IllegalArgumentException("Unexpected end-of-input");

            char c = s.charAt(i);

            if (c == ']') {
                i = Enums.skipWhitespace(s, i + 1);
                if (i < s.length()) throw new IllegalArgumentException("Trailing garbage");
                return result;
            }

            if (c != ',') throw new IllegalArgumentException("Unexpected character '" + c + "'");

            i = Enums.skipWhitespace(s, i + 1);
        }
    }

    /**
     * Verifies that the character at position <var>index</var> is a valid Java identifier start character, and
     * returns the position of the first non-Java identifier part character after the first char.
     *
     * @throws IllegalArgumentException <var>index</var> {@code ==} <var>s</var>{@code .length()}
     * @throws IllegalArgumentException The character at position <var>index</var> is not a valid Java identifier start
     *                                  character
     */
    private static int
    scanIdentifier(String s, int index) {

        if (index == s.length()) throw new IllegalArgumentException("Uxpected end-of-input");

        if (!Character.isJavaIdentifierStart(s.charAt(index))) {
            throw new IllegalArgumentException("Identifier expected");
        }

        for (index++; index < s.length() && Character.isJavaIdentifierPart(s.charAt(index)); index++);

        return index;
    }

    private static int skipWhitespace(String s, int index) {
        while (index < s.length() && Character.isWhitespace(s.charAt(index))) index++;
        return index;
    }

    /**
     * Returns the enum constant of the <var>enumType</var> with the specified <var>name</var>. The string must match
     * exactly the identifier used to declare the enum constant in this type. Extraneous whitespace characters are not
     * permitted.
     *
     * @return                          The enum constant with the specified name
     * @throws IllegalArgumentException The <var>enumType</var> has no constant with the specified name
     */
    public static <E extends Enum<E>> E
    valueOf(String name, Class<E> enumType) {

        for (E e : Enums.getEnumConstants(enumType)) {
            if (name.equals(e.name())) return e;
        }

        throw new IllegalArgumentException(name);
    }

    /**
     * Identical with {@link Class#getEnumConstants()}, but caches its result.
     */
    private static <E extends Enum<E>> E[]
    getEnumConstants(Class<E> enumType) {

        // Look up the cache.
        {
            Enum<?>[] cache = Enums.CACHE;
            if (cache != null && cache[0].getClass() == enumType) {

                @SuppressWarnings("unchecked") E[] result = (E[]) cache;

                return result;
            }
        }

        // Cache miss; call "Class.getEnumConstants()".
        E[] result = enumType.getEnumConstants();
        if (result.length > 0) Enums.CACHE = result; // Cannot cache zero-constant enums.

        return result;
    }
    @Nullable private static Enum<?>[] CACHE;
}
