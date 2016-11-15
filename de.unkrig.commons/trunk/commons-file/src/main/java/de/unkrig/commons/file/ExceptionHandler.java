
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

package de.unkrig.commons.file;

/**
 * A helper class that is useful when exceptions should not be caught, but handled.
 *
 * @param <E> The exception type that is handled
 */
public abstract
class ExceptionHandler<E extends Exception> {

    @SuppressWarnings("rawtypes") private static final ExceptionHandler DEFAULT = new ExceptionHandler() {
        @Override public void handle(String path, Exception        e)  throws Exception { throw e;  }
        @Override public void handle(String path, RuntimeException re)                  { throw re; }
    };

    /**
     * @return A simple {@link ExceptionHandler} that merely rethrows the exceptions it receives
     */
    @SuppressWarnings("unchecked") public static <E extends Exception> ExceptionHandler<E>
    defaultHandler() { return ExceptionHandler.DEFAULT; }

    /**
     * Handles the given <var>exception</var>, e.g. by printing, logging and/or rethrowing it.
     *
     * @param path Describes the resource being processed
     */
    public abstract void
    handle(String path, E exception) throws E;

    /**
     * Handles the given <var>runtimeException</var>, e.g. by printing, logging and/or rethrowing it.
     *
     * @param path Describes the resource being processed
     */
    public abstract void
    handle(String path, RuntimeException runtimeException);
}
