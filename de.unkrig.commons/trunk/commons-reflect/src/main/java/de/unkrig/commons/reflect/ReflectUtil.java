
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@code java.lang.reflect}-related utility methods.
 */
public final
class ReflectUtil {

    private
    ReflectUtil() {}

    /**
     * @return The types of the {@code values}
     */
    public static Class<?>[]
    getTypes(List<Object> values) {
        Class<?>[] types = new Class<?>[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value != null) types[i] = value.getClass();
        }
        return types;
    }

    /**
     * @return     The most specific applicable public method of the {@code targetType}
     * @throws NoSuchMethodException
     *             No <a
     *             href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.1">applicable</a>
     *             method exists
     * @throws NoSuchMethodException
     *             Two or more applicable methods exist, and none is <a
     *             href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.5">more specific</a>
     *             than all others
     */
    public static Method
    getMostSpecificMethod(Class<?> targetType, String methodName, Class<?>[] argumentTypes)
    throws NoSuchMethodException {

        Method mostSpecificMethod = null;
        for (Method method : targetType.getMethods()) {
            if (
                !method.getName().equals(methodName)
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers())
            ) continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!ReflectUtil.areApplicable(parameterTypes, argumentTypes)) continue;

            if (mostSpecificMethod == null) {
                mostSpecificMethod = method;
            } else
            if (ReflectUtil.isLessSpecific(mostSpecificMethod, method)) {
                mostSpecificMethod = method;
            } else
            if (ReflectUtil.isLessSpecific(method, mostSpecificMethod)) {
                ;
            } else
            {
                throw new NoSuchMethodException("Ambiguity between " + method + " and " + mostSpecificMethod);
            }
        }

        if (mostSpecificMethod == null) {
            throw new NoSuchMethodException(
                targetType
                + " has no method '"
                + methodName
                + "' applicable to argument types "
                + Arrays.toString(argumentTypes)
            );
        }

        return mostSpecificMethod;
    }

    /**
     * @return         The most specific applicable constructor of the {@code type}
     * @throws NoSuchMethodException
     *                 No <a
     *                 href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.1">applicable</a>
     *                 constructor exists
     * @throws NoSuchMethodException
     *                 Two or more applicable constructors exist, and none is <a
     *                 href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.5">more
     *                 specific</a> than all others
     */
    public static Constructor<?>
    getMostSpecificConstructor(Class<?> type, Class<?>[] argumentTypes) throws NoSuchMethodException {
        Constructor<?> mostSpecificConstructor = null;

        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            if (!ReflectUtil.areApplicable(parameterTypes, argumentTypes)) continue;

            if (mostSpecificConstructor == null) {
                mostSpecificConstructor = constructor;
            } else
            if (ReflectUtil.isLessSpecific(mostSpecificConstructor, constructor)) {
                mostSpecificConstructor = constructor;
            } else
            if (ReflectUtil.isLessSpecific(constructor, mostSpecificConstructor)) {
                ;
            } else
            {
                throw new NoSuchMethodException("Ambiguity between " + constructor + " and " + mostSpecificConstructor);
            }
        }

        if (mostSpecificConstructor == null) {
            throw new NoSuchMethodException(
                type
                + " has no constructor that is applicable to "
                + Arrays.toString(argumentTypes)
            );
        }

        return mostSpecificConstructor;
    }

    private static boolean
    isLessSpecific(Method method1, Method method2) {
        return (
            !ReflectUtil.isApplicable(method2.getDeclaringClass(), method1.getDeclaringClass())
            || !ReflectUtil.areApplicable(method2.getParameterTypes(), method1.getParameterTypes())
        );
    }

    private static boolean
    isLessSpecific(Constructor<?> constructor1, Constructor<?> constructor2) {
        return !ReflectUtil.areApplicable(constructor2.getParameterTypes(), constructor1.getParameterTypes());
    }

    private static boolean
    areApplicable(Class<?>[] parameterTypes, Class<?>[] argumentTypes) {
        if (parameterTypes.length != argumentTypes.length) return false;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!ReflectUtil.isApplicable(parameterTypes[i], argumentTypes[i])) return false;
        }
        return true;
    }

    /**
     * Similar to {@link Class#isAssignableFrom(Class)}, but also considers primitive types to be applicable from their
     * wrapper types, and including widening primitive conversion, as available through {@link Method#invoke(Object,
     * Object...)}.
     * <p>
     * Also, any {@code parameterType} is always applicable from a {@code null} {@code argumentType}.
     */
    private static boolean
    isApplicable(Class<?> parameterType, @Nullable Class<?> argumentType) {
        if (argumentType == null) return true;

        if (parameterType.isPrimitive()) {
            if (parameterType == boolean.class && (
                argumentType == Boolean.class
            )) return true;
            if (parameterType == byte.class && (
                argumentType == Byte.class
            )) return true;
            if (parameterType == short.class && (
                argumentType == Short.class || argumentType == Byte.class
            )) return true;
            if (parameterType == int.class && (
                argumentType == Integer.class || argumentType == Short.class || argumentType == Byte.class
            )) return true;
            if (parameterType == long.class && (
                argumentType == Long.class
                || argumentType == Integer.class
                || argumentType == Short.class
                || argumentType == Byte.class
            )) return true;
            if (parameterType == char.class && (
                argumentType == Character.class
            )) return true;
            if (parameterType == float.class && (
                argumentType == Float.class || argumentType == Double.class
            )) return true;
            if (parameterType == double.class && (
                argumentType == Double.class
            )) return true;
            return parameterType == argumentType;
        } else {
            return parameterType.isAssignableFrom(argumentType);
        }
    }
}
