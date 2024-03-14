
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

package de.unkrig.commons.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Helpers for convenient handling of methods and classes that may or may be loadable at runtime, e.g. those which
 * appeared in a later JRE version.
 * <p>
 *   Example:
 * </p>
 * <p>
 *   The JRE class "{@code java.lang.Character}" has a method "{@code boolean isAlphabetic(int)}", but only since Java
 *   7, so you cannot invoke this method from your code if you compile it with JDK 6 (so that it is still runnable
 *   in a JRE 6). To make the method available for your code, proceed as follows:
 * </p>
 * <pre>
 * class MyClass {
 *
 *     // ...
 *
 *     private static final MethodWrapper1&lt;Character, Boolean, Integer, RuntimeException&gt;
 *     CHARACTER__IS_ALPHABETIC = OptionalMethods.get1(
 *         Character.class,       // declaringClass
 *         "isAlphabetic",        // methodName
 *         int.class,             // parameterType
 *         RuntimeException.class // checkedException
 *     );
 *
 *     // ...
 *
 *     public void m(int cp) {
 *
 *         // Will throw an UnsupportedOperationException if this code runs in a JRE 6.
 *         if (CHARACTER__IS_ALPHABETIC.invoke(null, cp)) {
 *             // ...
 *         }
 *
 *         // Alternatively, it is possible to check whether the wrapped method is available:
 *         if (CHARACTER__IS_ALPHABETIC.isAvailable() &amp;&amp; CHARACTER__IS_ALPHABETIC.invoke(null, cp)) {
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 * <p>
 */
public final
class OptionalMethods {

    private OptionalMethods() {}

    // ================================== METHOD WITH ZERO PARAMETERS ==================================

    /**
     * Wrapper for a method with zero parameters.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <EX> The exception that the method may throw (use {@code RuntimeException} iff the method does not
     *             declare any checked exceptions)
     */
    public
    interface MethodWrapper0<DC, R, EX extends Throwable> {

        /**
         * @return Whether the wrapped method exists
         */
        boolean isAvailable();

        /**
         * Invokes the wrapped method, or, iff that method does not exist, takes an alternate action.
         *
         * @param target Ignored iff the wrapped method is STATIC
         */
        @Nullable R
        invoke(@Nullable DC target) throws EX;
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <R> MethodWrapper0<Object, R, RuntimeException>
    get0(
        @Nullable String      message,
        @Nullable ClassLoader declaringClassLoader,
        final String          declaringClassName,
        final String          methodName
    ) {
        return OptionalMethods.get0(
            message,
            declaringClassLoader,
            declaringClassName,
            methodName,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    @SuppressWarnings("unchecked") public static <R, EX extends Throwable> MethodWrapper0<Object, R, EX>
    get0(
        @Nullable final String    message,
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        try {
            return (MethodWrapper0<Object, R, EX>) OptionalMethods.get0(
                message,
                declaringClassLoader.loadClass(declaringClassName),
                methodName,
                checkedException
            );
        } catch (ClassNotFoundException e) {
            return OptionalMethods.missingMethod0(message, declaringClassName, methodName);
        }
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the method
     * @param <R>     The return type of the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R> MethodWrapper0<DC, R, RuntimeException>
    get0(@Nullable String message, Class<DC> declaringClass, final String methodName) {
        return OptionalMethods.get0(
            message,
            declaringClass,
            methodName,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the method
     * @param <R>     The return type of the method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R, EX extends Throwable> MethodWrapper0<DC, R, EX>
    get0(
        @Nullable final String    message,
        final Class<DC>           declaringClass,
        final String              methodName,
        @Nullable final Class<EX> checkedException
    ) {

        try {
            final Method method = declaringClass.getMethod(methodName);

            return new MethodWrapper0<DC, R, EX>() {

                @Override public boolean
                isAvailable() { return true; }

                @Override @Nullable public R
                invoke(@Nullable DC target) throws EX {
                    return OptionalMethods.invoke(method, target, checkedException);
                }
            };
        } catch (NoSuchMethodException e) {
            return OptionalMethods.missingMethod0(message, declaringClass.getName(), methodName);
        }
    }

    // ================================== METHOD WITH ONE PARAMETER ==================================

    /**
     * Wrapper for a method with one parameter.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P>  The type of the single parameter of the wrapped method
     * @param <EX> The exception that the method may throw (use {@code RuntimeException} iff the method does not
     *             declare any checked exceptions)
     */
    public
    interface MethodWrapper1<DC, R, P, EX extends Throwable> {

        /**
         * @return Whether the wrapped method exists
         */
        boolean isAvailable();

        /**
         * Invokes the wrapped method, or, iff that method does not exist, takes an alternate action.
         *
         * @param target Ignored iff the wrapped method is STATIC
         */
        @Nullable R
        invoke(@Nullable DC target, @Nullable P argument) throws EX;
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the wrapped method
     * @param <P>     The type of the single parameter of the wrapped method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <R, P> MethodWrapper1<?, R, P, RuntimeException>
    get1(
        @Nullable String      message,
        @Nullable ClassLoader declaringClassLoader,
        String                declaringClassName,
        String                methodName,
        Class<P>              parameterType
    ) {
        return OptionalMethods.get1(
            message,
            declaringClassLoader,
            declaringClassName,
            methodName,
            parameterType,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the wrapped method
     * @param <P>     The type of the single parameter of the wrapped method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <R, P, EX extends Throwable> MethodWrapper1<?, R, P, EX>
    get1(
        @Nullable final String    message,
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        final Class<P>            parameterType,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        try {
            return OptionalMethods.get1(
                message,
                declaringClassLoader.loadClass(declaringClassName),
                methodName,
                parameterType,
                checkedException
            );
        } catch (ClassNotFoundException cnfe) {
            return OptionalMethods.missingMethod1(message, declaringClassName, methodName, parameterType);
        }
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the wrapped method
     * @param <R>     The return type of the wrapped method
     * @param <P>     The type of the single parameter of the wrapped method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R, P> MethodWrapper1<DC, R, P, RuntimeException>
    get1(@Nullable String message, Class<DC> declaringClass, String methodName, Class<P> parameterType) {
        return OptionalMethods.get1(
            message,
            declaringClass,
            methodName,
            parameterType,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the wrapped method
     * @param <R>     The return type of the wrapped method
     * @param <P>     The type of the single parameter of the wrapped method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R, P, EX extends Throwable> MethodWrapper1<DC, R, P, EX>
    get1(
        @Nullable final String    message,
        final Class<DC>           declaringClass,
        final String              methodName,
        final Class<P>            parameterType,
        @Nullable final Class<EX> checkedException
    ) {

        try {
            final Method method = declaringClass.getMethod(methodName, parameterType);

            return new MethodWrapper1<DC, R, P, EX>() {

                @Override public boolean
                isAvailable() { return true; }

                @Override @Nullable public R
                invoke(@Nullable DC target, @Nullable P argument) throws EX {
                    return OptionalMethods.invoke(method, target, checkedException, argument);
                }
            };
        } catch (NoSuchMethodException e) {
            return OptionalMethods.<DC, R, P, EX>missingMethod1(
                message,
                declaringClass.getName(),
                methodName,
                parameterType
            );
        }
    }

    // ================================== METHOD WITH TWO PARAMETERS ==================================

    /**
     * Wrapper for a method with two parameters.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P1> The type of first parameter of the wrapped method
     * @param <P2> The type of second parameter of the wrapped method
     * @param <EX> The exception that the method may throw (use {@code RuntimeException} iff the method does not
     *             declare any checked exceptions)
     */
    public
    interface MethodWrapper2<DC, R, P1, P2, EX extends Throwable> {

        /**
         * @return Whether the wrapped method exists
         */
        boolean isAvailable();

        /**
         * Invokes the wrapped method, or, iff that method does not exist, takes an alternate action.
         *
         * @param target Ignored iff the wrapped method is STATIC
         */
        @Nullable R
        invoke(@Nullable DC target, @Nullable P1 argument1, @Nullable P2 argument2) throws EX;
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the wrapped method
     * @param <P1>    The type of the first parameter of the wrapped method
     * @param <P2>    The type of the second parameter of the wrapped method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <R, P1, P2> MethodWrapper2<?, R, P1, P2, RuntimeException>
    get2(
        @Nullable String      message,
        @Nullable ClassLoader declaringClassLoader,
        final String          declaringClassName,
        final String          methodName,
        Class<P1>             parameterType1,
        Class<P2>             parameterType2
    ) {
        return OptionalMethods.get2(
            message,
            declaringClassLoader,
            declaringClassName,
            methodName,
            parameterType1,
            parameterType2,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <R>     The return type of the wrapped method
     * @param <P1>    The type of the first parameter of the wrapped method
     * @param <P2>    The type of the second parameter of the wrapped method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <R, P1, P2, EX extends Throwable> MethodWrapper2<?, R, P1, P2, EX>
    get2(
        @Nullable final String    message,
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        Class<P1>                 parameterType1,
        Class<P2>                 parameterType2,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        try {
            return OptionalMethods.get2(
                message,
                declaringClassLoader.loadClass(declaringClassName),
                methodName,
                parameterType1,
                parameterType2,
                checkedException
            );
        } catch (ClassNotFoundException e) {
            return OptionalMethods.missingMethod2(
                message,
                declaringClassName,
                methodName,
                parameterType1,
                parameterType2
            );
        }
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the wrapped method
     * @param <R>     The return type of the wrapped method
     * @param <P1>    The type of the first parameter of the wrapped method
     * @param <P2>    The type of the second parameter of the wrapped method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R, P1, P2> MethodWrapper2<DC, R, P1, P2, RuntimeException>
    get2(
        @Nullable final String message,
        final Class<DC>        declaringClass,
        final String           methodName,
        Class<P1>              parameterType1,
        Class<P2>              parameterType2
    ) {
        return OptionalMethods.get2(
            message,
            declaringClass,
            methodName,
            parameterType1,
            parameterType2,
            null // checkedException
        );
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC>    The class that declares the wrapped method
     * @param <R>     The return type of the wrapped method
     * @param <P1>    The type of the first parameter of the wrapped method
     * @param <P2>    The type of the second parameter of the wrapped method
     * @param <EX>    The (single) checked exception declared for the method
     * @param message The message for the {@link UnsupportedOperationException}; if {@code null}, then the method
     *                signature is used
     */
    public static <DC, R, P1, P2, EX extends Throwable> MethodWrapper2<DC, R, P1, P2, EX>
    get2(
        @Nullable final String    message,
        final Class<DC>           declaringClass,
        final String              methodName,
        Class<P1>                 parameterType1,
        Class<P2>                 parameterType2,
        @Nullable final Class<EX> checkedException
    ) {

        Method method;
        try {
            method = declaringClass.getMethod(methodName, parameterType1, parameterType2);
        } catch (NoSuchMethodException nsme) {
            try {
                method = declaringClass.getDeclaredMethod(methodName, parameterType1, parameterType2);
                if (!Modifier.isPublic(method.getModifiers())) method.setAccessible(true);
            } catch (NoSuchMethodException nsme2) {
                return OptionalMethods.missingMethod2(
                    message,
                    declaringClass.getName(),
                    methodName,
                    parameterType1,
                    parameterType2
                );
            }
        }

        final Method finalMethod = method;
        return new MethodWrapper2<DC, R, P1, P2, EX>() {

            @Override public boolean
            isAvailable() { return true; }

            @Override @Nullable public R
            invoke(@Nullable DC target, @Nullable P1 argument1, @Nullable P2 argument2) throws EX {
                return OptionalMethods.invoke(finalMethod, target, checkedException, argument1, argument2);
            }
        };
    }

    private static <DC, R, EX extends Throwable> MethodWrapper0<DC, R, EX>
    missingMethod0(
        @Nullable final String explicitMessage,
        final String           declaringClassName,
        final String           methodName
    ) {

        return new MethodWrapper0<DC, R, EX>() {

            @Override public boolean
            isAvailable() { return false; }

            @Override @Nullable public R
            invoke(@Nullable Object target) {
                throw new UnsupportedOperationException(
                    OptionalMethods.cookMessage(explicitMessage, declaringClassName, methodName)
                );
            }
        };
    }

    private static <DC, R, P, EX extends Throwable> MethodWrapper1<DC, R, P, EX>
    missingMethod1(
        @Nullable final String explicitMessage,
        final String           declaringClassName,
        final String           methodName,
        final Class<P>         parameterType
    ) {

        return new MethodWrapper1<DC, R, P, EX>() {

            @Override public boolean
            isAvailable() { return false; }

            @Override @Nullable public R
            invoke(@Nullable DC target, @Nullable P argument) {
                throw new UnsupportedOperationException(
                    OptionalMethods.cookMessage(explicitMessage, declaringClassName, methodName, parameterType)
                );
            }
        };
    }

    private static <DC, R, P1, P2, EX extends Throwable> MethodWrapper2<DC, R, P1, P2, EX>
    missingMethod2(
        @Nullable final String explicitMessage,
        final String           declaringClassName,
        final String           methodName,
        final Class<P1>        parameterType1,
        final Class<P2>        parameterType2
    ) {

        return new MethodWrapper2<DC, R, P1, P2, EX>() {

            @Override public boolean
            isAvailable() { return false; }

            @Override @Nullable public R
            invoke(@Nullable DC target, @Nullable P1 argument1, @Nullable P2 argument2) {
                throw new UnsupportedOperationException(
                    OptionalMethods.cookMessage(
                        explicitMessage,
                        declaringClassName,
                        methodName,
                        parameterType1,
                        parameterType2
                    )
                );
            }
        };
    }

    private static String
    cookMessage(
        @Nullable String explicitMessage,
        String           declaringClassName,
        String           methodName,
        Class<?>...      parameterTypes
    ) {

        if (explicitMessage != null) return explicitMessage;

        StringBuilder sb = new StringBuilder();
        sb.append(declaringClassName).append('.').append(methodName).append('(');
        if (parameterTypes.length >= 1) {
            sb.append(parameterTypes[0]);
            for (int i = 1; i < parameterTypes.length; i++) sb.append(", ").append(parameterTypes[i]);
        }
        return sb.append(')').toString();
    }

    @SuppressWarnings("unchecked") private static <DC, R, EX extends Throwable> R
    invoke(final Method method, @Nullable DC target, @Nullable Class<EX> checkedException, Object... arguments)
    throws EX {

        try {
            return (R) method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if (te instanceof RuntimeException) throw (RuntimeException) te;
            if (te instanceof Error)            throw (Error) te;
            assert checkedException != null : "Caught undeclared checked exception " + te;
            assert checkedException.isAssignableFrom(te.getClass());
            throw (EX) te;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
