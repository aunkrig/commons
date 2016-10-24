
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

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.net.http.HttpResponse.Status;

/**
 * A very simple servlett that answers all requests with a preconfigured response.
 */
public
class SimpleServlett extends AbstractServlett {

    private static final Logger LOGGER = Logger.getLogger(SimpleServlett.class.getName());

    private final Status status;
    private final String contentType;
    private final String body;

    public
    SimpleServlett(Status status, String contentType, String body) {
        this.status      = status;
        this.contentType = contentType;
        this.body        = body;
    }

    @Override public HttpResponse
    getOrPost(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse) {

        String expandedBody = this.body;
        expandedBody = expandedBody.replace("${path}",         httpRequest.getUri().getPath());
        expandedBody = expandedBody.replace("${statusCode}",   Integer.toString(this.status.getCode()));
        expandedBody = expandedBody.replace("${reasonPhrase}", this.status.getReasonPhrase());
        if (LOGGER.isLoggable(FINE)) LOGGER.fine("Expanded body is '" + expandedBody + "'");

        // Create the response.
        HttpResponse httpResponse = HttpResponse.response(this.status, expandedBody);
        httpResponse.addHeader("Content-Type", this.contentType);

        return httpResponse;
    }
}
