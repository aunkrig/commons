
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

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Logger;

import de.unkrig.commons.io.FileBufferedChannel;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.Multiplexer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil.Produmer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.TcpServer;
import de.unkrig.commons.net.http.HttpResponse.Status;
import de.unkrig.commons.net.http.servlett.Servlett;
import de.unkrig.commons.util.collections.IterableUtil;

/**
 * A container for {@link Servlett}s that handle requests that are received from a client connection.
 */
public
class HttpClientConnectionHandler implements TcpServer.ConnectionHandler, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(HttpClientConnectionHandler.class.getName());

    private Iterable<Servlett>          servletts;
    private final Collection<Stoppable> stoppables = Collections.synchronizedCollection(new HashSet<Stoppable>());

    public
    HttpClientConnectionHandler() {
        this.servletts = Collections.emptyList();
    }

    public
    HttpClientConnectionHandler(Servlett servlett) {
        this.servletts = Collections.singletonList(servlett);
    }

    public
    HttpClientConnectionHandler(Iterable<Servlett> servletts) {
        this.servletts = servletts;
    }

    /**
     * @return The {@link Servlett}s currently registered with this {@link HttpClientConnectionHandler}
     */
    public Iterable<Servlett>
    getServletts() {
        return IterableUtil.unmodifiableIterable(this.servletts);
    }

    /**
     * Clears the set of currently registerd {@link Servlett}s and registers the given {@code servlett}.
     */
    public void
    setServlett(Servlett servlett) {
        this.servletts = Collections.singletonList(servlett);
    }

    /**
     * Clears the set of currently registerd {@link Servlett}s and registers the given {@code servletts}.
     */
    public void
    setServletts(Iterable<Servlett> servletts) {
        this.servletts = servletts;
    }

    /**
     * This one is called from the {@link TcpServer}. Override if you want to process the connection before
     * request processing begins.
     *
     * @param localSocketAddress  Could be examined by implementations that override this method
     * @param remoteSocketAddress Could be examined by implementations that override this method
     * @param stoppable           Stopping this will break the connection
     */
    @Override public void
    handleConnection(
        InputStream       in,
        OutputStream      out,
        InetSocketAddress localSocketAddress,
        InetSocketAddress remoteSocketAddress,
        Stoppable         stoppable
    ) throws IOException, InvalidHttpMessageException {
        this.processRequests(in, out, stoppable);
    }

    /**
     * This one is called from the {@link NioHttpServer}. Override if you want to process the connection before
     * request processing begins.
     *
     * @param localSocketAddress  Could be examined by implementations that override this method
     * @param remoteSocketAddress Could be examined by implementations that override this method
     * @param stoppable           Stopping this will break the connection
     */
    public void
    handleConnection(
        ReadableByteChannel in,
        WritableByteChannel out,
        InetSocketAddress   localSocketAddress,
        InetSocketAddress   remoteSocketAddress,
        Multiplexer         multiplexer,
        Stoppable           stoppable
    ) throws IOException {
        this.processRequests(in, out, multiplexer, stoppable);
    }

    /**
     * Processes HTTP requests from the client until the connection breaks.
     *
     * @param stoppable Stopping this will break the connection
     */
    protected void
    processRequests(InputStream in, OutputStream out, Stoppable stoppable)
    throws IOException, InvalidHttpMessageException {
        try {

            // Measure request and response sizes for logging.
            Produmer<Long, Long> requestSize = null, responseSize = null;
            if (LOGGER.isLoggable(FINE)) {
                requestSize  = ConsumerUtil.store();
                responseSize = ConsumerUtil.store();

                out = IoUtil.tee(out, IoUtil.lengthWritten(ConsumerUtil.cumulate(responseSize, 0)));
                in  = IoUtil.wye(in, IoUtil.lengthWritten(ConsumerUtil.cumulate(requestSize, 0)));
            }

            this.stoppables.add(stoppable);
            LOGGER.fine("Reading request from client");
            HttpRequest request = HttpRequest.read(in);

            HttpResponse httpResponse;
            HANDLE:
            {
                for (Servlett servlett : this.servletts) {

                    final OutputStream finalOut = out;

                    final boolean[] hadProvisionalResponses = new boolean[1];
                    httpResponse = servlett.handleRequest(
                        request,
                        new ConsumerWhichThrows<HttpResponse, IOException>() {

                            @Override public void
                            consume(HttpResponse provisionalResponse) throws IOException {
                                assert provisionalResponse.isProvisional();
                                provisionalResponse.write(finalOut);
                                hadProvisionalResponses[0] = true;
                            }
                        }
                    );

                    if (httpResponse != null) break HANDLE;

                    assert !hadProvisionalResponses[0] : (
                        "Servlett \"" + servlett + "\" sent provisional responses, but returned NULL"
                    );
                }

                httpResponse = HttpResponse.response(
                    Status.INTERNAL_SERVER_ERROR,
                    "None of " + this.servletts + " handled the request"
                );
            }

            assert httpResponse != null; // Redundant.

            LOGGER.fine("Sending response to client");
            httpResponse.write(out);

            request.removeBody().dispose();

            if (requestSize != null && responseSize != null) {
                LOGGER.fine(
                    request.getMethod()
                    + " "
                    + request.getUri()
                    + " => "
                    + httpResponse.getStatus()
                    + " ("
                    + requestSize.produce()
                    + "/"
                    + responseSize.produce()
                    + " bytes)"
                );
            }
        } finally {
            this.stoppables.remove(stoppable);
            if (this.stoppables.isEmpty()) {
                for (Servlett servlett : this.servletts) {
                    try { servlett.close(); } catch (IOException ioe) {}
                }
            }
        }
    }

    private void
    processRequests(
        final ReadableByteChannel in,
        final WritableByteChannel out,
        final Multiplexer         multiplexer,
        Stoppable                 stoppable
    ) throws IOException {

        this.stoppables.add(stoppable);

        final FileBufferedChannel fbc = new FileBufferedChannel(multiplexer, (SelectableChannel) out);

        ConsumerWhichThrows<HttpRequest, IOException> requestConsumer = (
            new ConsumerWhichThrows<HttpRequest, IOException>() {

                @Override public void
                consume(HttpRequest request) throws IOException {

                    HttpResponse response;
                    HANDLE:
                    {
                        for (Servlett servlett : HttpClientConnectionHandler.this.servletts) {

                            final boolean[] hadProvisionalResponses = new boolean[1];
                            response = servlett.handleRequest(
                                request,
                                new ConsumerWhichThrows<HttpResponse, IOException>() {

                                    @Override public void
                                    consume(HttpResponse provisionalResponse) throws IOException {
                                        assert provisionalResponse.isProvisional();
                                        provisionalResponse.write(Channels.newOutputStream(fbc));
                                        hadProvisionalResponses[0] = true;
                                    }
                                }
                            );
                            if (response != null) break HANDLE;

                            assert !hadProvisionalResponses[0] : (
                                "Servlett \"" + servlett + "\" sent provisional responses, but returned NULL"
                            );
                        }

                        response = HttpResponse.response(
                            Status.INTERNAL_SERVER_ERROR,
                            "None of " + HttpClientConnectionHandler.this.servletts + " handled the request"
                        );
                    }

                    request.removeBody().dispose();

                    LOGGER.fine("Sending response to client");
                    assert response != null;
                    response.write(Channels.newOutputStream(fbc));
                }
            }
        );

        HttpRequest.read(in, multiplexer, requestConsumer);
    }

    @Override public void
    stop() {
        for (Stoppable stoppable : this.stoppables) stoppable.stop();
    }
}
