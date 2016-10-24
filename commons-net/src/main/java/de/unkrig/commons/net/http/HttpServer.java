
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.TcpServer;

/**
 * Implementation of an HTTP server.
 *
 * @see #HttpServer(InetSocketAddress, HttpClientConnectionHandler)
 * @deprecated                                                      Use a {@link TcpServer} and pass it an {@link
 *                                                                  HttpClientConnectionHandler}
 */
@Deprecated public final
class HttpServer implements RunnableWhichThrows<IOException>, Stoppable {

    private final TcpServer tcpServer;

    /**
     * This {@link HttpServer} will accept connections on the {@code endpoint} and will handle each with the
     * <var>httpClientConnectionHandler</var>.
     *
     * @param endpoint The {@link InetAddress} and local port the server will bind to; {@link
     *                 InetSocketAddress#InetSocketAddress(int)} will accept connections on any/all local addresses;
     *                 port number zero will pick an ephemeral port
     */
    public
    HttpServer(
        InetSocketAddress                 endpoint,
        final HttpClientConnectionHandler httpClientConnectionHandler
    ) throws IOException {
        this.tcpServer = new TcpServer(endpoint, 0, httpClientConnectionHandler);
    }

    /**
     * This {@link HttpServer} will accept secure (HTTPS) connections on the {@code endpoint} and will handle each
     * with the <var>httpClientConnectionHandler</var>.
     *
     * @param endpoint The {@link InetAddress} and local port the server will bind to; {@link
     *                 InetSocketAddress#InetSocketAddress(int)} will accept connections on any/all local addresses;
     *                 port number zero will pick an ephemeral port
     */
    public
    HttpServer(
        InetSocketAddress                 endpoint,
        SSLContext                        sslContext,
        final HttpClientConnectionHandler httpClientConnectionHandler
    ) throws IOException {
        this.tcpServer = new TcpServer(endpoint, 0, sslContext, httpClientConnectionHandler);
    }

    @Override public void
    run() throws IOException {
        this.tcpServer.run();
    }

    @Override public void
    stop() {
        this.tcpServer.stop();
    }

    /**
     * @return The endpoint address of this HTTP server
     * @see    TcpServer#getEndpointAddress()
     */
    public InetSocketAddress
    getEndpointAddress() {
        return this.tcpServer.getEndpointAddress();
    }
}
