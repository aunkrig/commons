
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

package de.unkrig.commons.lang;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Formatter;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @see #toString(Object)
 */
public final
class PrettyPrinter {

    private
    PrettyPrinter() {}

    /**
     * If an array is larger than this threshold ({@value #ARRAY_ELLIPSIS}), then it is printed as
     * <pre>{ <var>elem-0</var>, <var>elem-1</var>, <var>elem-2</var>, ... }</pre>
     */
    public static final int ARRAY_ELLIPSIS = 10;

    /**
     * If a char array is larger than this threshold ({@value #CHAR_ARRAY_ELLIPSIS}), then it is printed as
     * <pre>'abc'...</pre>
      */
    public static final int CHAR_ARRAY_ELLIPSIS = 20;

    /**
     * If a char sequence is longer than this threshold ({@value #CHAR_SEQUENCE_ELLIPSIS}), then it is printed as
     * <pre>"abc"...</pre>
     */
    public static final int CHAR_SEQUENCE_ELLIPSIS = 100;

    /**
     * If the length of the output exceeds this threshold ({@value #TOTAL_LENGTH_ELLIPSIS}), then all remaining array
     * elements are printed as
     * <pre>"abc"...</pre>
     */
    public static final int TOTAL_LENGTH_ELLIPSIS = 1024;

    /**
     * An improved version of {@link String#valueOf(Object)}.
     * <p>
     *   Objects are converted to strings in a Java-like format:
     * </p>
     * <dl>
     *   <dt>Character:</dt>
     *   <dd>{@code '\n'}</dd>
     *   <dt>Byte, Short, Long, Float, Double:</dt>
     *   <dd>{@code 3B 3S 3L 3F 3D}</dd>
     *   <dt>String:</dt>
     *   <dd>{@code "abc\n"}<br/>{@code "abcdef"... (123 chars)}</dd>
     *   <dt>Other {@link CharSequence}:</dt>
     *   <dd>{@code StringBuilder "abc\n"}</dd>
     *   <dt>Char array:</dt>
     *   <dd>{@code char[4] 'abc\n'}<br/>{@code char[99] 'abcdef'...}</dd>
     *   <dt>Other array:</dt>
     *   <dd><code>int[3] { 1, 2, 3 }</code><br/><code>short[199] { 1, 2, 3, 4, 5, 6, ... }</code></dd>
     *   <dt>Nested array:</dt>
     *   <dd><code>Object[3][] { [self], null, Object[2] { "abc", [parent] } }</code></dd>
     *   <dt>Any other object:</dt>
     *   <dd>The string returned by {@link Object#toString()}</dd>
     * </dl>
     * Large objects are abbreviated with an ellipsis as shown above:
     * <dl>
     *   <dt>Char array:</dt>
     *   <dd>{@value #CHAR_ARRAY_ELLIPSIS} chars</dd>
     *   <dt>Other array:</dt>
     *   <dd>{@value #ARRAY_ELLIPSIS} elements</dd>
     *   <dt>CharSequence:</dt>
     *   <dd>{@value #CHAR_SEQUENCE_ELLIPSIS} chars</dd>
     * </dl>
     * As the result string exceeds {@value #TOTAL_LENGTH_ELLIPSIS} characters, all remaining arrays and
     * {@link CharSequence}s are abbreviated, so the result string will effectively not be much longer than {@value
     * #TOTAL_LENGTH_ELLIPSIS} characters.
     */
    @Nullable public static String
    toString(@Nullable Object o) {

        if (o == null) return "null";

        StringBuilder sb = new StringBuilder();
        return PrettyPrinter.append(o, sb).toString();
    }

    private static StringBuilder
    append(@Nullable Object o, StringBuilder sb) {

        if (o == null) return sb.append("null");

        Class<? extends Object> clasS = o.getClass();

        if (clasS.isArray()) return PrettyPrinter.appendArray(o, null, sb);

        if (o instanceof CharSequence) return PrettyPrinter.append((CharSequence) o, sb);

        if (clasS == Character.class) return PrettyPrinter.append(((Character) o).charValue(), sb);

        if (clasS == Byte.class)   return sb.append(((Byte)  o).toString()).append('B');
        if (clasS == Short.class)  return sb.append(((Short) o).toString()).append('S');
        if (clasS == Long.class)   return sb.append(((Long)  o).toString()).append('L');
        if (clasS == Float.class)  return sb.append(new DecimalFormat().format(((Float)  o).floatValue())).append('F');
        if (clasS == Double.class) return sb.append(new DecimalFormat().format(((Double) o).doubleValue())).append('D');

        return sb.append(o);
    }

    /**
     * @param o An array of primitive or reference
     */
    private static StringBuilder
    appendArray(Object o, @Nullable Object[][] dejaVu, StringBuilder sb) {
        int length = Array.getLength(o);

        {
            String canonicalName = PrettyPrinter.getPrettyClassName(o);
            int    idx           = canonicalName.indexOf(']');
            if (idx == -1) throw new IllegalArgumentException(canonicalName);
            sb.append(canonicalName.substring(0, idx)).append(length).append(canonicalName.substring(idx));
        }

        if (length == 0) return sb;

        if (o.getClass().getComponentType() == Character.TYPE) {
            sb.append(" '");
            final char[] ca = (char[]) o;
            for (int i = 0; i < length; i++) {

                if (i >= PrettyPrinter.CHAR_ARRAY_ELLIPSIS || sb.length() >= PrettyPrinter.TOTAL_LENGTH_ELLIPSIS) {
                    return sb.append("'...");
                }

                PrettyPrinter.appendChar(ca[i], sb);
            }
            return sb.append('\'');
        }

        sb.append(" { ");
        for (int i = 0;;) {
            Object element = Array.get(o, i);

            ELEMENT:
            if (element instanceof Object[]) {
                if (element == o) {
                    sb.append("[self]");
                } else
                if (dejaVu == null) {
                    PrettyPrinter.appendArray(element, new Object[][] { (Object[]) o }, sb);
                } else {
                    for (Object[] other : dejaVu) {
                        if (element == other) {
                            sb.append("[parent]");
                            break ELEMENT;
                        }
                    }
                    Object[][] tmp = new Object[dejaVu.length + 1][];
                    System.arraycopy(dejaVu, 0, tmp, 0, dejaVu.length);
                    tmp[dejaVu.length] = (Object[]) o;
                    PrettyPrinter.appendArray(element, tmp, sb);
                }
            } else {
                PrettyPrinter.append(element, sb);
            }

            if (++i == length) break;

            if (i >= PrettyPrinter.ARRAY_ELLIPSIS || sb.length() >= PrettyPrinter.TOTAL_LENGTH_ELLIPSIS) {
                return sb.append(", ... }");
            }

            sb.append(", ");
        }
        return sb.append(" }");
    }

    /**
     * @return The fully qualified canonical name of the class of {@code o}, or, if that class is in a subpackage of
     * 'java', its simple class name.
     */
    private static String
    getPrettyClassName(Object o) {
        String result = o.getClass().getCanonicalName();
        if (result.startsWith("java.")) result = result.substring(result.lastIndexOf('.') + 1);
        return result;
    }

    /**
     * Converts a char sequence into a JAVA string literal like {@code "Hello!\n"}.
     */
    private static StringBuilder
    append(CharSequence cs, StringBuilder sb) {

        if (cs instanceof String) {
            sb.append('"');
        } else {
            sb.append(PrettyPrinter.getPrettyClassName(cs)).append(" \"");
        }

        int length = cs.length();
        for (int i = 0; i < length; i++) {
            if (i >= PrettyPrinter.CHAR_SEQUENCE_ELLIPSIS || sb.length() >= PrettyPrinter.TOTAL_LENGTH_ELLIPSIS) {
                return sb.append("\"... (").append(length).append(" chars)");
            }
            PrettyPrinter.appendChar(cs.charAt(i), sb);
        }
        return sb.append('"');
    }

    /**
     * Converts a character into a JAVA character literal like {@code '\n'}.
     */
    private static StringBuilder
    append(char c, StringBuilder sb) {
        sb.append('\'');
        return PrettyPrinter.appendChar(c, sb).append('\'');
    }

    @SuppressWarnings("resource") private static StringBuilder
    appendChar(char c, StringBuilder sb) {

        {
            int idx = "\b\t\n\f\r\"\'\\".indexOf(c);
            if (idx != -1) return sb.append('\\').append("btnfr\"'\\".charAt(idx));
        }

        if (c < ' ') return sb.append('\\').append(Integer.toOctalString(c));

        if (c <= 255) return sb.append(c);

        new Formatter(sb).format("\\u%04x", (int) c);

        return sb;
    }
}
