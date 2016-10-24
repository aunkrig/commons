
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various assertion-related utility methods.
 */
public final
class AssertionUtil {

    private
    AssertionUtil() {}

    /**
     * Enables assertions for the given {@code clasS} and the local and anonymous classes that it encloses, regardless
     * of the "enable assertions" command line options that the JVM was started with.
     * <p>
     *   Enabling assertions programmatically makes them much more useful, because unfortunately assertions are
     *   disabled by default and hardly any runtime environment provides a simple way to enable them.
     * </p>
     * <p>
     *   Notice that the assertion status of a top-level type or member type is shared with the local and anonymous
     *   class that its methods declare, but <em>not</em> with the member types it declares. In other words: This
     *   method should be called for local classes and anonymous classes.
     * </ul>
     * <p>
     *   {@link #enableAssertionsForThisClass()} is an even more elegant way to enable assertions for classes as they
     *   are loaded and initialized.
     * </p>
     */
    public static void
    enableAssertionsFor(Class<?> clasS) {
        try {

            // Top-level and member types declare a field "static final boolean $assertionsDisabled" iff there are
            // assertions in the code.
            Field assertionsDisabledField = clasS.getDeclaredField("$assertionsDisabled");
            assertionsDisabledField.setAccessible(true);

            // Remove the "finality" from the "$assertionsDisabled" field.
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(assertionsDisabledField, assertionsDisabledField.getModifiers() & ~Modifier.FINAL);

            // Set the "$assertionsDisabled" field to false.
            assertionsDisabledField.setBoolean(null, false);
        } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
            // Generally one should never catch "Throwable", because "Error"s are typically so severe that they
            // should not be caught and processed. However here some very low-level reflection errors could be
            // thrown and we absolutely want to ignore them.
            ;
        }
    }

    /**
     * Enables assertions for the class that invokes this method, regardless of the "enable assertions" command line
     * options that the JVM was started with.
     * <pre>
     *     public class MyClass {
     *
     *         static { AssertionUtil.enableAssertionsForThisClass(); }
     *
     *         // ...
     *     }
     * </pre>
     * <p>
     *   Adding such static initializers to all classes makes assertions much more helpful, because unfortunately
     *   assertions are disabled by default and hardly any runtime environment provides a simple way to enable them.
     * </p>
     * <p>
     *   Must only be called from the static initializer of a class.
     * </p>
     * <p>
     *   Should not be called from local and anonymous classes, because these share the assertion state with the
     *   enclosing (non-local, non-anonymous) type.
     * </p>
     */
    public static void
    enableAssertionsForThisClass() {

        StackTraceElement sf = new Throwable().getStackTrace()[1];

        String subjectClassName  = sf.getClassName();
        String subjectMethodName = sf.getMethodName();

        if (!"<clinit>".equals(subjectMethodName)) {
            throw new AssertionError(
                "'AssertionUtil.enableAssertionsForThisClass()' must only be called from a class initializer, "
                + "not from '"
                + sf
                + "'"
            );
        }

        Class<?> subjectClass;
        try {
            subjectClass = Class.forName(subjectClassName);
        } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
            return;
        }

        AssertionUtil.enableAssertionsFor(subjectClass);
    }

    /**
     * Verifies that the {@code subject} is not {@code null}.
     *
     * @return The {@code subject}
     * @throws NullPointerException
     */
    public static <T> T
    notNull(@Nullable T subject) {
        if (subject == null) throw new NullPointerException();
        return subject;
    }

    /**
     * Verifies that the {@code subject} is not {@code null}.
     *
     * @return The {@code subject}
     * @throws NullPointerException with the given {@code message} iff the {@code subject} is {@code null}
     */
    public static <T> T
    notNull(@Nullable T subject, String message) {
        if (subject == null) throw new NullPointerException(message);
        return subject;
    }

    /**
     * Identical with '{@code throw new AssertionError()}', but returns an {@link Object} so it can be used in an
     * expression.
     */
    public static Object
    fail() { throw new AssertionError(); }

    /**
     * Identical with '{@code throw new AssertionError(message)}', but returns an {@link Object} so it can be used in
     * an expression.
     */
    public static <T> T
    fail(String message) { throw new AssertionError(message); }

    /**
     * Identical with '{@code throw new AssertionError(cause)}', but returns an {@link Object} so it can be used in
     * an expression.
     */
    public static Object
    fail(Throwable cause) {
        AssertionError ae = new AssertionError(String.valueOf(cause));
        ae.initCause(cause);
        throw ae;
    }

    /**
     * Identical with '{@code throw new AssertionError(message, cause)}', but returns an {@link Object} so it can be
     * used in an expression.
     */
    public static Object
    fail(String message, Throwable cause) {
        AssertionError ae = new AssertionError(message);
        ae.initCause(cause);
        throw ae;
    }
}
