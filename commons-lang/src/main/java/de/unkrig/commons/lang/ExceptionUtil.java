
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

import java.lang.reflect.Field;
import java.util.Iterator;

import org.xml.sax.SAXParseException;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Exception}-related utility methods.
 */
public final
class ExceptionUtil {

    private
    ExceptionUtil() {}

    @Nullable private static final Field
    THROWABLE_DETAIL_MESSAGE_FIELD = ExceptionUtil.getDeclaredField(Throwable.class, "detailMessage");

    @Nullable private static Field
    getDeclaredField(Class<Throwable> declaringClass, String fieldName) {

        Field f;
        try {
            f = declaringClass.getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        if (!f.isAccessible()) {
            try {
                f.setAccessible(true);
            } catch (RuntimeException rte) {
                if (!"java.lang.reflect.InaccessibleObjectException".contentEquals(rte.getClass().getName())) throw rte;
                System.err.println((
                    "Warning: " + ExceptionUtil.class.getName() + ": "
                    + "This JVM forbids the use of \"java.lang.reflect.Field.setAccessible()\"; "
                    + "\"--add-opens java.base/java.lang=ALL-UNNAMED\" should solve that"
                ));
                return null;
            }
        }

        return f;
    }

    /**
     * Wraps a given 'cause' in another throwable of the same type, with a detail message composed from {@code
     * prefix}, a colon, a space, and the cause.
     * <p>
     *   This is useful for adding context information to a throwable, e.g. which file is currently being processed,
     *   the current line number, etc.
     * </p>
     *
     * @param prefix The text to prepend to the cause throwable's detail message
     * @param cause  The throwable to wrap
     * @return       The wrapping throwable
     */
    @SuppressWarnings("unchecked") public static <T extends Throwable> T
    wrap(@Nullable String prefix, T cause) {
        Class<? extends Throwable> causeClass = cause.getClass();

        // Determine the new detail message and the root cause.
        String message;
        {
            String causeMessage;

            // Some classes override "getMessage()", so it is better to get the message from the
            // "Throwable.detailMessage" field.
            Field tdmf = ExceptionUtil.THROWABLE_DETAIL_MESSAGE_FIELD;
            if (tdmf != null) {
                try {
                    causeMessage = (String) tdmf.get(cause);
                } catch (Exception e) {
                    causeMessage = cause.getMessage();
                }
            } else {
                causeMessage = cause.getMessage();
            }
            message = prefix == null ? causeMessage : causeMessage == null ? prefix : prefix + ": " + causeMessage;
        }

        // Try "new TargetThrowable(String message, Throwable cause)". 95% of all throwables should have such a
        // constructor.
        T wrapper;

        WRAP:
        {
            try {
                wrapper = (T) causeClass.getConstructor(String.class, Throwable.class).newInstance(message, cause);
                break WRAP;
            } catch (Exception e) {
                ;
            }

            // Try "new TargetThrowable(String message)", plus "initCause(Throwable cause)".
            try {
                wrapper = (T) causeClass.getConstructor(String.class).newInstance(message);
                wrapper.initCause(cause);
                break WRAP;
            } catch (Exception e) {
                ;
            }

            // Try "new TargetThrowable(Object message)", plus "initCause(Throwable cause)".
            try {
                wrapper = (T) causeClass.getConstructor(Object.class).newInstance(message);
                wrapper.initCause(cause);
                break WRAP;
            } catch (Exception e3) {
                ;
            }

            // Special handling for SAXParseException.
            if (cause instanceof SAXParseException) {
                SAXParseException spe = (SAXParseException) cause;
                wrapper = (T) new SAXParseException(
                    message,
                    spe.getPublicId(),
                    spe.getSystemId(),
                    spe.getLineNumber(),
                    spe.getColumnNumber()
                );
                wrapper.initCause(cause);
                break WRAP;
            }

            // Special handling for JUNIT's "org.junit.ComparisonFailure" error.
            if (cause.getClass().getName().equals("org.junit.ComparisonFailure")) {

                try {
                    wrapper = (T) causeClass.getConstructor(String.class, String.class, String.class).newInstance(
                        message,                                           // message
                        causeClass.getMethod("getExpected").invoke(cause), // expected
                        causeClass.getMethod("getActual").invoke(cause)    // actual
                    );
                    wrapper.initCause(cause);
                    break WRAP;
                } catch (Exception e) {
                    ;
                }
            }

            // Don't know how to wrap the target throwable - give up.
            return cause;
        }

        // Eliminate the top frames up to and including this "wrap()" method.
        StackTraceElement[] st = wrapper.getStackTrace();
        for (int i = 0;; i++) {
            if ("wrap".equals(st[i].getMethodName())) {
                i++;
                StackTraceElement[] st2 = new StackTraceElement[st.length - i];
                System.arraycopy(st, i, st2, 0, st2.length);
                wrapper.setStackTrace(st2);
                break;
            }
        }

        return wrapper;
    }

    /**
     * Wraps a given 'cause' in another throwable of the given wrapper class type, with a detail message composed
     * from <var>prefix</var>, a colon, a space, and the cause.
     *
     * @param prefix       The text to prepend to the cause throwable's detail message
     * @param cause        The throwable to wrap
     * @param wrapperClass The type of the wrapping throwable
     * @return             The wrapping throwable
     */
    public static <T extends Throwable> T
    wrap(@Nullable String prefix, Throwable cause, Class<T> wrapperClass) {

        // Compose the new detail message.
        StringBuilder sb = new StringBuilder();
        if (prefix != null) sb.append(prefix).append(": ");

        sb.append(cause.getClass().getName());

        String causeMessage = cause.getMessage();
        if (causeMessage != null) {
            sb.append(": ").append(causeMessage);
        } else {
            for (Throwable t = cause.getCause(); t != null; t = t.getCause()) {
                sb.append(": ").append(t.getClass().getName());
                String tMessage = t.getMessage();
                if (tMessage != null) {
                    sb.append(": ").append(tMessage);
                    break;
                }
            }
        }
        String message = sb.toString();

        // Try "new Wrapper(String message, Throwable cause)".
        try {
            return wrapperClass.getConstructor(String.class, Throwable.class).newInstance(message, cause);
        } catch (Exception e) {
            ;
        }

        // Try "new Wrapper(String message)", plus "initCause(Throwable cause)".
        try {
            T wrapper = wrapperClass.getConstructor(String.class).newInstance(message);
            wrapper.initCause(cause);
            return wrapper;
        } catch (Exception e) {
            ;
        }

        // Try "new Wrapper(Object message)", plus "initCause(Throwable cause)". (This is mainly for the
        // AssertionError, which, strange enough, has no "AssertionError(String, Throwable)" constructor.)
        try {
            T wrapper = wrapperClass.getConstructor(Object.class).newInstance(message);
            wrapper.initCause(cause);
            return wrapper;
        } catch (Exception e) {
            ;
        }

        // Try "new Wrapper()", plus "initCause(Throwable cause)".
        try {
            T wrapper = wrapperClass.newInstance();
            wrapper.initCause(cause);
            return wrapper;
        } catch (Exception e) {
            ;
        }

        if (wrapperClass.isAssignableFrom(cause.getClass())) {
            @SuppressWarnings("unchecked") T result = (T) cause;
            return result;
        }

        AssertionError
        ae = new AssertionError("Exception class '" + wrapperClass.getName() + "' has no suitable constructor");

        ae.initCause(cause);
        throw ae;
    }

    /**
     * Throws the given {@link Exception}, although it does not declare any exceptions.
     * <p>
     *   This feature exploits the fact that the per-method declaration of thrown exceptions (the THROWS clause) is a
     *   <i>compiler feature</i> and not a <i>runtime feature</i>, and can be circumvented with some trickery.
     * </p>
     * <p>
     *   This is useful e.g. for implementations of {@link Iterator}, {@link Runnable} and other "service classes"
     *   that want to throw checked exceptions. (Wrapping these in {@link RuntimeException}s is
     *   sometimes not an option.)
     * </p>
     * <p>
     *   Notice that the only way to catch such undeclared checked exceptions is "<tt>try { ... } catch (Exception e)
     *   { ... }</tt>".
     * </p>
     * <p>
     *   Notice that this method breaks Java's concept of <a
     *   href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-11.html#jls-11.2">exception checking</a> entirely.
     *   It also renders the exception-type-parametrized interfaces {@code *WhichThrows} (e.g. {@link
     *   de.unkrig.commons.lang.protocol.RunnableWhichThrows}) useless. One should use one or the other, but never both.
     * </p>
     * <p>
     *   Usage example:
     * </p>
     * <pre>
     * import static de.unkrig.commons.lang.ExceptionUtil.throwUndeclared;
     *
     * class MyIterator&lt;E&gt; implements Iterator&lt;E&gt; {
     *
     *     public E next() {
     *         // ...
     *         <b>throwUndeclared</b>(new IOException());
     *     }
     *
     *     // ...
     * }</pre>
     *
     * @param e The exception to be thrown
     */
    public static void
    throwUndeclared(Exception e) {
        ExceptionUtil.<RuntimeException>throwUndeclared2(e);
    }

    @SuppressWarnings("unchecked") private static <EX extends Exception> void
    throwUndeclared2(Exception e) throws EX {

        // The trick is that because generic methods are implemented by ERASURE, the following CAST is removed during
        // compilation, and hence ANY exception can be thrown:
        throw (EX) e;
    }

    /**
     * Identical with "{@code throw throwable}", but has a return type {@code T}, so it can be used in an expression.
     */
    public static <T, EX extends Throwable> T
    throW(EX throwable) throws EX { throw throwable; }

    /**
     * Identical with "{@code throw new AssertionError(object)}", but has a return type {@code T}, so it can be used
     * in an expression.
     */
    public static <T> T
    throwAssertionError(Object object) { throw new AssertionError(object); }
}
