
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
 *     private static final MethodWrapper1<Character, Boolean, Integer, RuntimeException>
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
 *         if (CHARACTER__IS_ALPHABETIC.isAvailable() && CHARACTER__IS_ALPHABETIC.invoke(null, cp)) {
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
     * @param <DC> The class that declares the method
     * @param <R>  The return type of the method
     */
    public static <R> MethodWrapper0<?, R, RuntimeException>
    get0(
        @Nullable ClassLoader declaringClassLoader,
        final String          declaringClassName,
        final String          methodName
    ) {
        return OptionalMethods.get0(
            declaringClassLoader,
            declaringClassName,
            methodName,
            RuntimeException.class
        );
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the method
     * @param <R>  The return type of the method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <R, EX extends Throwable> MethodWrapper0<?, R, EX>
    get0(
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        Class<?> declaringClass;
        try {
            declaringClass = declaringClassLoader.loadClass(declaringClassName);
        } catch (ClassNotFoundException e) {

            return new MethodWrapper0<Object, R, EX>() {

                @Override public boolean
                isAvailable() { return false; }

                @Override @Nullable public R
                invoke(@Nullable Object target) {
                    throw new UnsupportedOperationException(declaringClassName);
                }
            };
        }

        return OptionalMethods.get0(declaringClass, methodName, checkedException);
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the method
     * @param <R>  The return type of the method
     */
    public static <DC, R> MethodWrapper0<DC, R, RuntimeException>
    get0(final Class<DC> declaringClass, final String methodName) {
        return OptionalMethods.get0(declaringClass, methodName, RuntimeException.class);
    }

    /**
     * Returns a wrapper for a zero-parameter method of the </var>declaringClass</var>, based on the
     * <var>methodName</var>. If that method does not exist, a wrapper is returned that will throw an {@link
     * UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the method
     * @param <R>  The return type of the method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <DC, R, EX extends Throwable> MethodWrapper0<DC, R, EX>
    get0(
        final Class<DC>           declaringClass,
        final String              methodName,
        @Nullable final Class<EX> checkedException
    ) {

        Method method;
        try {
            method = declaringClass.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            method = null;
        }

        final Method finalMethod = method;
        return new MethodWrapper0<DC, R, EX>() {

            @Override public boolean
            isAvailable() { return finalMethod != null; }

            @Override @Nullable public @SuppressWarnings("unchecked") R
            invoke(@Nullable DC target) throws EX {

                if (finalMethod == null) {
                    throw new UnsupportedOperationException(
                        declaringClass.getName()
                        + "."
                        + methodName
                        + "()"
                    );
                }

                try {
                    return (R) finalMethod.invoke(target);
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
        };
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

        boolean isAvailable();

        /**
         * Invokes the wrapped method, or, iff that method does not exist, takes an alternate action.
         *
         * @param target Ignored iff the wrapped method is STATIC
         */
        @Nullable R
        invoke(@Nullable DC target, @Nullable P parameter) throws EX;
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P>  The type of the single parameter of the wrapped method
     */
    public static <R, P> MethodWrapper1<?, R, P, RuntimeException>
    get1(
        @Nullable ClassLoader declaringClassLoader,
        final String          declaringClassName,
        final String          methodName,
        Class<P>              parameterType
    ) {
        return OptionalMethods.get1(
            declaringClassLoader,
            declaringClassName,
            methodName,
            parameterType,
            RuntimeException.class
        );
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P>  The type of the single parameter of the wrapped method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <R, P, EX extends Throwable> MethodWrapper1<?, R, P, EX>
    get1(
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        Class<P>                  parameterType,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        Class<?> declaringClass;
        try {
            declaringClass = declaringClassLoader.loadClass(declaringClassName);
        } catch (ClassNotFoundException e) {

            return new MethodWrapper1<Object, R, P, EX>() {

                @Override public boolean
                isAvailable() { return false; }

                @Override @Nullable public R
                invoke(@Nullable Object target, @Nullable P parameter) {
                    throw new UnsupportedOperationException(declaringClassName);
                }
            };
        }

        return OptionalMethods.get1(declaringClass, methodName, parameterType, checkedException);
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P>  The type of the single parameter of the wrapped method
     */
    public static <DC, R, P> MethodWrapper1<DC, R, P, RuntimeException>
    get1(final Class<DC> declaringClass, final String methodName, Class<P> parameterType) {
        return OptionalMethods.get1(declaringClass, methodName, parameterType, RuntimeException.class);
    }

    /**
     * Returns a wrapper for a single-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var> and <var>parameterType</var>. If that method does not exist, a wrapper is returned that
     * will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P>  The type of the single parameter of the wrapped method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <DC, R, P, EX extends Throwable> MethodWrapper1<DC, R, P, EX>
    get1(
        final Class<DC>           declaringClass,
        final String              methodName,
        Class<P>                  parameterType,
        @Nullable final Class<EX> checkedException
    ) {

        Method method;
        try {
            method = declaringClass.getMethod(methodName, parameterType);
        } catch (NoSuchMethodException e) {
            method = null;
        }

        final Method finalMethod = method;
        return new MethodWrapper1<DC, R, P, EX>() {

            @Override public boolean
            isAvailable() { return finalMethod != null; }

            @Override @Nullable public @SuppressWarnings("unchecked") R
            invoke(@Nullable DC target, @Nullable P parameter) throws EX {

                if (finalMethod == null) {
                    throw new UnsupportedOperationException(
                        declaringClass.getName()
                        + "."
                        + methodName
                        + "("
                        + parameter
                        + ")"
                    );
                }

                try {
                    return (R) finalMethod.invoke(target, parameter);
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
        };
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

        boolean isAvailable();

        /**
         * Invokes the wrapped method, or, iff that method does not exist, takes an alternate action.
         *
         * @param target Ignored iff the wrapped method is STATIC
         */
        @Nullable R
        invoke(@Nullable DC target, @Nullable P1 parameter1, @Nullable P2 parameter2) throws EX;
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P1> The type of the first parameter of the wrapped method
     * @param <P2> The type of the second parameter of the wrapped method
     */
    public static <R, P1, P2> MethodWrapper2<?, R, P1, P2, RuntimeException>
    get2(
        @Nullable ClassLoader declaringClassLoader,
        final String          declaringClassName,
        final String          methodName,
        Class<P1>             parameterType1,
        Class<P2>             parameterType2
    ) {
        return OptionalMethods.get2(
            declaringClassLoader,
            declaringClassName,
            methodName,
            parameterType1,
            parameterType2,
            RuntimeException.class
        );
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P1> The type of the first parameter of the wrapped method
     * @param <P1> The type of the second parameter of the wrapped method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <R, P1, P2, EX extends Throwable> MethodWrapper2<?, R, P1, P2, EX>
    get2(
        @Nullable ClassLoader     declaringClassLoader,
        final String              declaringClassName,
        final String              methodName,
        Class<P1>                 parameterType1,
        Class<P2>                 parameterType2,
        @Nullable final Class<EX> checkedException
    ) {

        if (declaringClassLoader == null) declaringClassLoader = Thread.currentThread().getContextClassLoader();
        assert declaringClassLoader != null;

        Class<?> declaringClass;
        try {
            declaringClass = declaringClassLoader.loadClass(declaringClassName);
        } catch (ClassNotFoundException e) {

            return new MethodWrapper2<Object, R, P1, P2, EX>() {

                @Override public boolean
                isAvailable() { return false; }

                @Override @Nullable public R
                invoke(@Nullable Object target, @Nullable P1 parameter1, @Nullable P2 parameter2) {
                    throw new UnsupportedOperationException(declaringClassName);
                }
            };
        }

        return OptionalMethods.get2(declaringClass, methodName, parameterType1, parameterType2, checkedException);
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P1> The type of the first parameter of the wrapped method
     * @param <P2> The type of the second parameter of the wrapped method
     */
    public static <DC, R, P1, P2> MethodWrapper2<DC, R, P1, P2, RuntimeException>
    get2(final Class<DC> declaringClass, final String methodName, Class<P1> parameterType1, Class<P2> parameterType2) {
        return OptionalMethods.get2(declaringClass, methodName, parameterType1, parameterType2, RuntimeException.class);
    }

    /**
     * Returns a wrapper for a two-parameter method of the </var>declaringClass</var>, based on
     * <var>methodName</var>, <var>parameterType1</var> and <var>parameterType2</var>. If that method does not exist,
     * a wrapper is returned that will throw an {@link UnsupportedOperationException} when invoked.
     *
     * @param <DC> The class that declares the wrapped method
     * @param <R>  The return type of the wrapped method
     * @param <P1> The type of the first parameter of the wrapped method
     * @param <P2> The type of the second parameter of the wrapped method
     * @param <EX> The (single) checked exception declared for the method
     */
    public static <DC, R, P1, P2, EX extends Throwable> MethodWrapper2<DC, R, P1, P2, EX>
    get2(
        final Class<DC>           declaringClass,
        final String              methodName,
        Class<P1>                 parameterType1,
        Class<P2>                 parameterType2,
        @Nullable final Class<EX> checkedException
    ) {

        Method method;
        try {
            method = declaringClass.getMethod(methodName, parameterType1, parameterType2);
        } catch (NoSuchMethodException e) {
            method = null;
        }

        final Method finalMethod = method;
        return new MethodWrapper2<DC, R, P1, P2, EX>() {

            @Override public boolean
            isAvailable() { return finalMethod != null; }

            @Override @Nullable public @SuppressWarnings("unchecked") R
            invoke(@Nullable DC target, @Nullable P1 parameter1, @Nullable P2 parameter2) throws EX {

                if (finalMethod == null) {
                    throw new UnsupportedOperationException(
                        declaringClass.getName()
                        + "."
                        + methodName
                        + "("
                        + parameter1
                        + ", "
                        + parameter2
                        + ")"
                    );
                }

                try {
                    return (R) finalMethod.invoke(target, parameter1, parameter2);
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
        };
    }
}
