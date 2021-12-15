
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
     * Enables assertions for the given <var>clasS</var> and the local and anonymous classes that it encloses,
     * regardless of the "enable assertions" command line options that the JVM was started with.
     * <p>
     *   Enabling assertions programmatically makes them much more useful, because unfortunately assertions are
     *   disabled by default and hardly any runtime environment provides a simple way to enable them.
     * </p>
     * <p>
     *   Notice that the assertion status of a top-level class or member class is shared with the local and anonymous
     *   class that its methods declare, but <em>not</em> with the member classes it declares. In other words: This
     *   method should not only be called for top-level class declarations, but also for each and every <em>member</em>
     *   class declaration.
     * </p>
     * <p>
     *   (Notice that assertions cannot be used in interfaces, because interfaces cannot declare non-abstract
     *   methods, nor initializers.)
     * </p>
     * <p>
     *   {@link #enableAssertionsForThisClass()} is an even more elegant way to enable assertions for classes
     *   as they are loaded and initialized.
     * </p>
     * <h3>Notice:</h3>
     * <p>
     *   Some JREs (e.g. adopt_openjdk-11.0.11.9-hotspot) print ugly messages to STDERR when this method is executed
     * </p>
     * <pre>
     *   WARNING: An illegal reflective access operation has occurred
     *   WARNING: Illegal reflective access by de.unkrig.commons.lang.AssertionUtil (file:/C:/dev/mavenrepo/de/unkrig/zz/zz-find/1.3.10-SNAPSHOT/zz-find-1.3.10-SNAPSHOT-jar-with-dependencies.jar) to field java.lang.reflect.Field.modifiers
     *   WARNING: Please consider reporting this to the maintainers of de.unkrig.commons.lang.AssertionUtil
     *   WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
     *   WARNING: All illegal access operations will be denied in a future release
     * </pre>
     * <p>
     *   , while most other JREs (e.g. adopt_openjdk-16.0.1.9-hotspot jdk-13.0.2+8 adopt_openjdk-14.0.2_12-hotspot
     *   adopt_openjdk-8.0.292.10-hotspot jdk-17.0.1+12) don't.
     *   That can be avoided by running the 11+ JREs with {@code --add-opens java.base/java.lang.reflect=ALL-UNNAMED}.
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
     * Enables assertions for the class that invokes this method, and for all all the local and anonymous classes that
     * its methods declare, regardless of the "enable assertions" command line options that the JVM was started with.
     * <pre>
     *     public class MyClass {
     *
     *         static { AssertionUtil.enableAssertionsForThisClass(); }
     *
     *         // ...
     *     }
     * </pre>
     *
     * @see #enableAssertionsFor(Class)
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
     * Verifies that the <var>subject</var> is not {@code null}.
     *
     * @return The <var>subject</var>
     * @throws NullPointerException
     */
    public static <T> T
    notNull(@Nullable T subject) {
        if (subject == null) throw new NullPointerException();
        return subject;
    }

    /**
     * Verifies that the <var>subject</var> is not {@code null}.
     *
     * @return The <var>subject</var>
     * @throws NullPointerException with the given <var>message</var> iff the <var>subject</var> is {@code null}
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
