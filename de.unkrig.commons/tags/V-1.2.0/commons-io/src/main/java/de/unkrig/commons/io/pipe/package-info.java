
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

// CheckStyle bug: '</dt>' and '</dd>' are reported as invalid tags.
// SUPPRESS CHECKSTYLE JavadocStyle:18
/**
 * A pipe is an object to which data can be written and from which data can be read; the data read equals the data
 * previously written.
 * <p>
 *   There are two typical use cases for a pipe:
 * </p>
 * <dl>
 *   <dt>Buffer</dt>
 *   <dd>
 *     One ore more threads write to and read from the pipe alternatingly and/or simultaneously. If the writers are
 *     'faster' than the readers, the pipe will fill up. If the readers ar faster, the pipe will become empty. An
 *     obvious implementation is a 'ring buffer'.
 *   </dd>
 *   <dt>Temporary storage</dt>
 *   <dd>
 *     First data is written to the pipe, then (completely) read back. This could be implemented by a 'linear buffer';
 *     however, since since 'ring buffer' has no significant overhead, there is no reason to implement the 'linear
 *     buffer'.
 *   </dd>
 * </dl>
 * <p>
 *   The {@link de.unkrig.commons.io.pipe.Pipe} interface describes a non-blocking version of a pipe: The {@link
 *   de.unkrig.commons.io.pipe.Pipe#write(byte[], int, int)} and {@link de.unkrig.commons.io.pipe.Pipe#read(byte[],
 *   int, int)} methods simply return zero iff the pipe is full resp. empty.
 * </p>
 * <p>
 *   {@link de.unkrig.commons.io.pipe.PipeUtil#asInputOutputStreams(Pipe)} converts a {@link
 *   de.unkrig.commons.io.pipe.Pipe} into a pair of output stream and input stream which block the calling thread while
 *   the pipe is full resp. empty.
 * </p>
 * <p>
 *   The methods of the {@link de.unkrig.commons.io.pipe.PipeFactory} create various implementations of the {@link
 *   de.unkrig.commons.io.pipe.Pipe} interface.
 * </p>
 */
@NotNullByDefault
package de.unkrig.commons.io.pipe;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

