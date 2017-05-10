
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

package de.unkrig.commons.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.net.TcpServer;
import de.unkrig.commons.net.http.servlett.AbstractServlett;

/**
 * Implementation of a simple HTTP proxy. Creates a connection to a remote host/port each time an HTTP client connects.
 */
public final
class HttpProxy implements RunnableWhichThrows<IOException>, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(HttpProxy.class.getName());

    private final TcpServer tcpServer;

    private AtomicInteger requestCount = new AtomicInteger();

    private static final TransformerWhichThrows<? super HttpRequest, HttpRequest, IOException>
    REQUEST_IDENTITY = TransformerUtil.asTransformerWhichThrows(TransformerUtil.<HttpRequest, HttpRequest>identity());

    private static final TransformerWhichThrows<? super HttpResponse, HttpResponse, IOException>
    RESPONSE_IDENTITY = TransformerUtil.asTransformerWhichThrows(
        TransformerUtil.<HttpResponse, HttpResponse>identity()
    );

    /**
     * @param endpoint      The local endpoint (interface) to bind to
     * @param remoteAddress The address of the remote server to connect to
     */
    public
    HttpProxy(InetSocketAddress endpoint, InetSocketAddress remoteAddress) throws IOException {
        this(
            endpoint,
            remoteAddress.getHostName(),
            remoteAddress.getPort(),
            HttpProxy.REQUEST_IDENTITY,  // requestTransformer
            HttpProxy.RESPONSE_IDENTITY  // responseTransformer
        );
    }

    /**
     * @param endpoint   The local endpoint (interface) to bind to
     * @param remoteHost The name of the remote server to connect to
     * @param remotePort The port on the remote server
     */
    public
    HttpProxy(InetSocketAddress endpoint, String remoteHost, int remotePort) throws IOException {
        this(endpoint, remoteHost, remotePort, HttpProxy.REQUEST_IDENTITY, HttpProxy.RESPONSE_IDENTITY);
    }

    /**
     * @param endpoint            The local endpoint (interface) to bind to
     * @param remoteAddress       The address of the remote server to connect to
     * @param requestTransformer  Could modify requests as they are forwarded from the client to the server
     * @param responseTransformer Could modify responses as they are forwarded from the server to the client
     */
    public
    HttpProxy(
        InetSocketAddress                                                             endpoint,
        final InetSocketAddress                                                       remoteAddress,
        final TransformerWhichThrows<? super HttpRequest, HttpRequest, IOException>   requestTransformer,
        final TransformerWhichThrows<? super HttpResponse, HttpResponse, IOException> responseTransformer
    ) throws IOException {
        this(endpoint, remoteAddress.getHostName(), remoteAddress.getPort(), requestTransformer, responseTransformer);
    }

    /**
     * @param endpoint            The local endpoint (interface) to bind to
     * @param remoteHost          The name of the remote server to connect to
     * @param remotePort          The port on the remote server
     * @param requestTransformer  Could modify requests as they are forwarded from the client to the server
     * @param responseTransformer Could modify responses as they are forwarded from the server to the client
     */
    public
    HttpProxy(
        InetSocketAddress                                                             endpoint,
        final String                                                                  remoteHost,
        final int                                                                     remotePort,
        final TransformerWhichThrows<? super HttpRequest, HttpRequest, IOException>   requestTransformer,
        final TransformerWhichThrows<? super HttpResponse, HttpResponse, IOException> responseTransformer
    ) throws IOException {
        this.tcpServer = new TcpServer(endpoint, 0, new HttpClientConnectionHandler(">>> ", "<<< ") {

            @Override public void
            handleConnection(
                InputStream       in,
                OutputStream      out,
                InetSocketAddress localSocketAddess,
                InetSocketAddress remoteSocketAddress,
                Stoppable         stoppable
            ) throws IOException, InvalidHttpMessageException {

                // Connect to remote host.
                final HttpClient httpClient = new HttpClient(remoteHost, remotePort);

                this.setServlett(new AbstractServlett() {

                    @Override public HttpResponse
                    getOrPost(
                        HttpRequest                                    request,
                        ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse
                    ) throws IOException {
                        final int requestNumber = HttpProxy.this.requestCount.incrementAndGet();

                        // Patch the 'Host' request header.
                        request.setHeader("Host", remoteHost);

                        // Forward the request.
                        HttpProxy.LOGGER.fine("Forwarding request #" + requestNumber + ":");
                        HttpResponse httpResponse = httpClient.call(
                            requestTransformer.transform(request),
                            "<<< ",
                            ">>> "
                        );

                        // Forward the response.
                        HttpProxy.LOGGER.fine("Forwarding response #" + requestNumber + ":");
                        return responseTransformer.transform(httpResponse);
                    }


                    @Override public void
                    close() throws IOException {
                        httpClient.close();
                    }
                });
                for (;;) {
                    this.processRequests(in, out, stoppable);
                }
            }
        });
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
     * @return The endpoint address of the local HTTP server.
     */
    public InetSocketAddress
    getEndpointAddress() {
        return this.tcpServer.getEndpointAddress();
    }
}
