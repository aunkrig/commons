
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

package de.unkrig.commons.net.http.servlett;

import java.io.IOException;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.net.http.HttpResponse.Status;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Abstract base implementation of {@link Servlett}.
 */
public
class AbstractServlett implements Servlett {

    /**
     * Forwards the HTTP requests it receives to one of
     * <ul>
     *   <li>{@link #get(HttpRequest, ConsumerWhichThrows)}</li>
     *   <li>{@link #head(HttpRequest, ConsumerWhichThrows)}</li>
     *   <li>{@link #post(HttpRequest, ConsumerWhichThrows)}</li>
     *   <li>{@link #put(HttpRequest, ConsumerWhichThrows)}</li>
     * </ul>
     * , depending on the HTTP method.
     */
    @Override @Nullable public HttpResponse
    handleRequest(HttpRequest request, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {

        HttpResponse response;
        switch (request.getMethod()) {

        case GET:
            response = this.get(request, sendProvisionalResponse);
            break;

        case HEAD:
            response = this.head(request, sendProvisionalResponse);
            break;

        case POST:
            response = this.post(request, sendProvisionalResponse);
            break;

        case PUT:
            response = this.put(request, sendProvisionalResponse);
            break;

        default:
            throw new IllegalStateException(request.toString());
        }

        if (response.isProvisional()) {
            throw new IllegalStateException("Request handler method returned a provisional response: " + response);
        }

        return response;
    }

    /**
     * Handles one HTTP GET request. The default implementation forwards the request to {@link
     * #getOrPost(HttpRequest, ConsumerWhichThrows)}.
     *
     * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
     * @see Servlett#handleRequest(HttpRequest, ConsumerWhichThrows)
     */
    protected HttpResponse
    get(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {
        return this.getOrPost(httpRequest, sendProvisionalResponse);
    }

    /**
     * Handles one HTTP HEAD request. The default implementation returns a BAD_REQUEST response.
     *
     * @throws IOException
     * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
     * @see Servlett#handleRequest(HttpRequest, ConsumerWhichThrows)
     */
    protected HttpResponse
    head(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {
        return HttpResponse.response(Status.BAD_REQUEST, "Method '" + httpRequest.getMethod() + "' not implemented");
    }

    /**
     * Handles one HTTP POST request. The default implementation forwards the request to {@link
     * #getOrPost(HttpRequest, ConsumerWhichThrows)}.
     *
     * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
     * @see Servlett#handleRequest(HttpRequest, ConsumerWhichThrows)
     */
    protected HttpResponse
    post(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {
        return this.getOrPost(httpRequest, sendProvisionalResponse);
    }

    /**
     * Handles one HTTP PUT request. The default implementation returns a BAD_REQUEST response.
     *
     * @throws IOException
     * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
     * @see Servlett#handleRequest(HttpRequest, ConsumerWhichThrows)
     */
    protected HttpResponse
    put(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {
        return HttpResponse.response(Status.BAD_REQUEST, "Method '" + httpRequest.getMethod() + "' not implemented");
    }

    /**
     * Is invoked by the default implementation of {@link #get(HttpRequest, ConsumerWhichThrows)} and {@link
     * #post(HttpRequest, ConsumerWhichThrows)}. The default implementation returns a BAD_REQUEST response.
     *
     * @throws IOException
     * @see #handleRequest(HttpRequest, ConsumerWhichThrows)
     * @see Servlett#handleRequest(HttpRequest, ConsumerWhichThrows)
     */
    protected HttpResponse
    getOrPost(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {
        return HttpResponse.response(Status.BAD_REQUEST, "Method '" + httpRequest.getMethod() + "' not implemented");
    }

    /**
     * This default implementation does simply <b>nothing</b>.
     *
     * @throws IOException
     */
    @Override public void
    close() throws IOException {
        ;
    }
}
