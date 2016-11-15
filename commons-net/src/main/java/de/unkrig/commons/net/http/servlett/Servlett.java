
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

package de.unkrig.commons.net.http.servlett;

import java.io.Closeable;
import java.io.IOException;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An entity that handles HTTP requests, similar to an JEE servlet.
 * <p>
 *   The lifecycle of a {@link Servlett} is as follows:
 * </p>
 * <ol>
 *   <li>
 *     An instance that implements the {@link Servlett} interface is created (and typically used until the server
 *     terminates or reloads its configuration)
 *   </li>
 *   <li>
 *   </li>
 * </ol>
 *
 * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
 */
public
interface Servlett extends Closeable {

    /**
     * Processes one HTTP request. Notice that the caller is responsible for disposing the returned response's body.
     *
     * @param request                 The request to process
     * @param sendProvisionalResponse Consumes any provisional responses, see
     *                                <a href="https://tools.ietf.org/html/rfc2616#section-10.1">RFC 2616: 10 Status
     *                                Code Definitions: 10.1 Informational 1xx</a>
     * @return                        The final (non-provisional) response that completes the processing of the
     *                                <var>request</var>; or {@code null} iff this handler cannot handle the
     *                                <var>request</var>
     */
    @Nullable HttpResponse
    handleRequest(HttpRequest request, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException;
}
