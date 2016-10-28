
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

package de.unkrig.commons.net.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import de.unkrig.commons.net.TcpClient;
import de.unkrig.commons.net.http.HttpRequest.Method;

/**
 * Implementation of an HTTP client.
 *
 * @see #HttpClient(InetAddress, int)
 */
public final
class HttpClient implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(HttpClient.class.getName());

    private final TcpClient tcpClient;

    /**
     * @see #HttpClient(InetAddress, int)
     */
    public
    HttpClient(String host, int port) throws IOException {
        this(InetAddress.getByName(host), port);
    }

    /**
     * Connects to the given {@code address} and {@code port}.
     */
    public
    HttpClient(InetAddress address, int port) throws IOException {
        this.tcpClient = new TcpClient(address, port);
    }

    /**
     * Sends the given {@code httpRequest} to the server, waits for the response from the server.
     *
     * @return The parsed response from the server
     */
    public HttpResponse
    call(HttpRequest httpRequest) throws IOException {

        httpRequest.write(this.tcpClient.getOutputStream());

        return HttpResponse.read(
            this.tcpClient.getInputStream(),       // in
            httpRequest.getHttpVersion(),          // httpVersion
            httpRequest.getMethod() == Method.HEAD // isResponseToHEAD
        );
    }

    @Override public void
    close() throws IOException {

        LOGGER.fine("Disposing connection to " + this.tcpClient.getRemoteSocketAddress());

        this.tcpClient.close();
    }
}
