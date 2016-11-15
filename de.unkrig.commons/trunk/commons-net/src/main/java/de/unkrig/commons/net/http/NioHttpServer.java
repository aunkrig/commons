
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
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import de.unkrig.commons.io.Multiplexer;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.NioTcpServer;
import de.unkrig.commons.net.http.servlett.Servlett;

/**
 * Implementation of an HTTP server.
 *
 * @see #NioHttpServer(InetSocketAddress, HttpClientConnectionHandler)
 */
public final
class NioHttpServer implements Stoppable {


    private final NioTcpServer nioTcpServer;

    /**
     * Accepts connections on the {@code endpoint}; obtains a new {@link Servlett} from the {@code
     * httpClientConnectionHandlerFactory} for each accepted connection, and then calls {@link
     * Servlett#handleRequest(HttpRequest, de.unkrig.commons.lang.protocol.ConsumerWhichThrows)} for each received
     * request.
     */
    public
    NioHttpServer(
        InetSocketAddress                 endpoint,
        final HttpClientConnectionHandler httpClientConnectionHandler
    ) throws IOException {
        this.nioTcpServer = new NioTcpServer();
        this.nioTcpServer.addServer(endpoint, 0, new NioTcpServer.ConnectionHandler() {

            @Override public void
            handleConnection(
                ReadableByteChannel in,
                WritableByteChannel out,
                InetSocketAddress   localSocketAddress,
                InetSocketAddress   remoteSocketAddress,
                Multiplexer         multiplexer,
                Stoppable           stoppable
            ) {

//                httpClientConnectionHandler.handleConnection(
//                    in,
//                    out,
//                    localSocketAddress,
//                    remoteSocketAddress,
//                    stoppable
//                );
            }
        });
    }

    /**
     * @see NioTcpServer#start(int)
     */
    public void
    start(int threadCount) {
        this.nioTcpServer.start(threadCount);
    }

    @Override public void
    stop() {
        this.nioTcpServer.stop();
    }
}
