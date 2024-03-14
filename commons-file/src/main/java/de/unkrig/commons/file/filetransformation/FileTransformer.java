
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

package de.unkrig.commons.file.filetransformation;

import java.io.File;
import java.io.IOException;

/**
 * @see #transform(String, File, File, Mode)
 */
public
interface FileTransformer {

    /**
     * @see FileTransformer#transform(String, File, File, Mode)
     * @see #TRANSFORM
     * @see #CHECK
     * @see #CHECK_AND_TRANSFORM
     */
    enum Mode {

        /** Execute the operation without previously checking if it actually changes any files. */
        TRANSFORM,

        /**
         * Execute the operation, but do not create or modify any files, and throw {@link #NOT_IDENTICAL} iff the
         * operation does not produce an identical result.
         */
        CHECK,

        /**
         * Before executing the actual transformation, verify that it will actually modify any files.
         * <p>
         *   Since checking whether a transformation would actually change any files is typically much cheaper than
         *   the executing the actual transformation, this mode may be more efficient than {@link #TRANSFORM},
         *   particularly if you expect few or no modifications.
         * </p>
         */
        CHECK_AND_TRANSFORM,
    }

    /**
     * Thrown by {@link #transform(String, File, File, Mode) transform}{@code (String, File, File, }{@link
     * Mode#CHECK}{@code )}; indicates that when that method would be invoked with <var>mode</var> {@link
     * Mode#TRANSFORM}, its output would <em>not</em> be identical with its input. That can only be the case for an
     * "in-place" transformation.
     */
    RuntimeException
    NOT_IDENTICAL = new RuntimeException("Mode CHECK detected non-identical contents") {

        private static final long serialVersionUID = 1L;

        @Override public Throwable
        fillInStackTrace() {

            // Save the cost of filling in the stack trace because we will never need it: This exception should
            // ALWAYS be caught and processed specially.
            return this;
        }
    };

    /**
     * A {@link Runnable} that simply throws {@link #NOT_IDENTICAL}.
     */
    Runnable
    THROW_NOT_IDENTICAL = new Runnable() {

        @Override public void
        run() {
            throw FileTransformer.NOT_IDENTICAL;
        }
    };

    /**
     * Creates the file <var>out</var>, based on the file <var>in</var>. Iff {@code in.equals(out)} ('in-place
     * transformation'), then the original file remains unchanged, is modified, or replaced with a new file.
     * <p>
     *   The precise contract is as follows:
     * </p>
     * <ul>
     *   <li>
     *     <b>If {@code mode == Mode.CHECK}</b>, then this method merely <i>checks</i> whether the content of the file
     *     would change, and, if so, complete normally, otherwise it would throw {@link #NOT_IDENTICAL}.
     *   </li>
     *   <li>
     *     <b>Otherwise, if {@code !}<var>in</var>{@code .equals(}<var>out</var>{@code )} ("out-of-place
     *     transformation")</b>, <var>out</var> is created, based on <var>in</var>.
     *     (Parameter <var>mode</var> is ignored.)
     *     (If this method throws an exception, then it must not leave a file <var>out</var> behind.)
     *   </li>
     *   <li>
     *     <b>Otherwise</b>, the file is left unchanged, is modified, or is replaced with a new file. If {@code mode ==
     *     Mode.CHECK_AND_TRANSFORM}, then the method attempts to avoid unnecessary i/o and processing by first checking
     *     whether the file requires any modifications before applying them. (If this method throws an exception, then
     *     it must revert the file to its original state as far as is reasonably possible.)
     *   </li>
     * </ul>
     *
     * @param  path             A text designating the input file; typically, but not necessarily identical with {@link
     *                          File#getPath() in.getPath()}
     * @param  in               The input file to read
     * @param  out              The output file to create; irrelevant iff {@code mode ==} {@link Mode#CHECK}
     * @throws RuntimeException {@link #NOT_IDENTICAL} iff {@code mode == }{@link Mode#CHECK} and the output produced
     *                          by {@link #transform(String, File, File, Mode)} would not be identical with the input
     * @see #NOT_IDENTICAL
     * @see Mode#TRANSFORM
     * @see Mode#CHECK
     * @see Mode#CHECK_AND_TRANSFORM
     */
    void
    transform(String path, File in, File out, Mode mode) throws IOException;
}
