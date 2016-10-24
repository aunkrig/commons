
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.lang;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.regex.Pattern;

import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@code java.lang.Object}-related utility methods.
 */
public final
class ObjectUtil {

    private static final Pattern WORD_SEPARATOR = Pattern.compile("[\\s,]+");

    private ObjectUtil() {}

    /**
     * The often-needed "equal" method that handles {@code null} references.
     * <p>
     *   Redeemed by {@code java.util.Objects.equals(Object, Object)} which is available since Java 1.7.
     * </p>
     *
     * @return Both arguments are {@code null}, or are {@link CharSequence}s with equal contents, or are equal
     */
    public static <T> boolean
    equals(@Nullable T o1, @Nullable T o2) {

        if (o1 == null) return o2 == null;
        if (o2 == null) return false;

        if (o1 instanceof CharSequence && o2 instanceof CharSequence) {
            return StringUtil.equals((CharSequence) o1, (CharSequence) o2);
        }

        return o1.equals(o2);
    }

    /**
     * @return {@code Null} iff {@code o == null}, otherwise {@code o.hashCode()}
     */
    public static int
    hashCode(@Nullable Object o) { return o == null ? 0 : o.hashCode(); }

    /**
     * @return Returns {@code value.toString()} or the {@code defaultValue} iff {@code value == null}
     */
    public static <T> String
    toString(@Nullable T value, String defaultValue) { return value != null ? value.toString() : defaultValue; }

    /**
     * @return <var>lhs</var>, or <var>rhs</var> iff {@code lhs == null}
     */
    public static <T> T
    or(@Nullable T lhs, T rhs) { return lhs == null ? rhs : lhs; }

    /**
     * Converts a string to an object of the given <var>targetType</var>.
     * <p>
     *   For primitive <var>targetType</var>s, the corresponding wrapper object is returned.
     * </p>
     * <p>
     *   Supports all primitive types (except {@code void}), their wrapper types, {@link String}, {@code char[]},
     *   {@link Charset}, {@link Class}, {@link Pattern}, enum types, and classes that have a single-string-parameter
     *   constructor.
     * </p>
     * <p>
     *   Supports (one-dimensional) arrays of these types, where the <var>text</var> is split into words at commas
     *   and/or whitespace, and each word converted to the component type.
     * </p>
     * <p>
     *   Does not support collections, including {@link EnumSet}s, because the element type cannot be determined
     *   through REFLECTION. Usage of an array is recommended as a workaround, as follows:
     * </p>
     * <pre>{@code
     *String  s   = "RED,GREEN";
     *Color[] tmp = ObjectUtil.fromString(s, Color[].class);
     *
     *List<Color>    list    = Arrays.asList(tmp);
     *EnumSet<Color> enumSet = EnumSet.copyOf(Arrays.asList(tmp));
     *Set<Color>     set     = new HashSet<String>(Arrays.asList(tmp));
     *}</pre>
     *
     * @throws NumberFormatException    The string could not be converted to the <var>targetType</var>
     * @throws IllegalArgumentException The string could not be converted to the <var>targetType</var>
     * @throws RuntimeException         The string could not be converted to the <var>targetType</var>
     */
    @SuppressWarnings("unchecked") public static <T> T
    fromString(String text, Class<T> targetType) {

        if (targetType.isArray()) {
            Class<?> componentType = targetType.getComponentType();
            String[] words         = ObjectUtil.WORD_SEPARATOR.split(text);
            Object   array         = Array.newInstance(componentType, words.length);
            for (int i = 0; i < words.length; i++) {
                Array.set(array, i, ObjectUtil.fromString(words[i], componentType));
            }
            return (T) array;
        }

        if (targetType == String.class) return (T) text;

        try {
            if (targetType == boolean.class || targetType == Boolean.class) return (T) Boolean.valueOf(text);
            if (targetType == byte.class    || targetType == Byte.class)    return (T) Byte.valueOf(text);
            if (targetType == short.class   || targetType == Short.class)   return (T) Short.valueOf(text);
            if (targetType == int.class     || targetType == Integer.class) return (T) Integer.valueOf(text);
            if (targetType == long.class    || targetType == Long.class)    return (T) Long.valueOf(text);
            if (targetType == float.class   || targetType == Float.class)   return (T) Float.valueOf(text);
            if (targetType == double.class  || targetType == Double.class)  return (T) Double.valueOf(text);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(
                "Cannot convert \""
                + text
                + "\" to "
                + targetType.getSimpleName().toLowerCase()
            );
        }

        if (targetType == char.class || targetType == Character.class) {
            if (text.length() == 0) throw new IllegalArgumentException("Empty string");
            if (text.length() > 1) throw new IllegalArgumentException("String too long");
            return (T) Character.valueOf(text.charAt(0));
        }

        if (targetType == char[].class) return (T) text.toCharArray();

        if (targetType == Charset.class) return (T) Charset.forName(text);

        if (targetType == Class.class) {
            try {
                return (T) Class.forName(text);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException(cnfe);
            } catch (NoClassDefFoundError ncdfe) {
                throw new IllegalArgumentException(ncdfe);
            }
        }

        if (targetType == Pattern.class) return (T) Pattern.compile(text);

        if (Enum.class.isAssignableFrom(targetType)) {
            try {
                @SuppressWarnings("rawtypes") Enum<?> constant = Enum.valueOf((Class<Enum>) targetType, text);
                return (T) constant;
            } catch (IllegalArgumentException iae) {
                @SuppressWarnings("rawtypes") EnumSet<?> allConstants = EnumSet.allOf((Class<Enum>) targetType);
                throw new IllegalArgumentException(
                    "Invalid constant \""
                    + text
                    + "\"; valid values are "
                    + allConstants
                );
            }
        }

        // For all other parameter types, use the single-string-parameter constructor.
        try {
            return targetType.getConstructor(String.class).newInstance(text);
        } catch (Exception e) {
            throw new IllegalArgumentException((
                "Instantiating "
                + targetType.getSimpleName()
                + ": "
                + e.getMessage()
            ), e);
        }
    }

    /**
     * This method returns {@code null} although it is declared {@link NotNull @NotNull}. This comes in handy to
     * initialize a field that is declared as {@code NotNull @NotNull}, but is filled reflectively, e.g. by
     * frameworks like JUNIT or SPRING.
     */
    @SuppressWarnings("null") public static <T> T
    almostNull() { return null; }
}
