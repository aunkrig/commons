
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

package de.unkrig.commons.net;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.lang.protocol.StoppableUtil;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A server that accepts connections from clients on a local port/bind address, and, for each accepted connection,
 * establishes another connection to a remote port/address, and then invokes a {@link ProxyConnectionHandler}.
 *
 * @see #ReverseProxy(InetSocketAddress, int, InetSocketAddress, Proxy, int, ProxyConnectionHandler)
 */
public
class ReverseProxy implements RunnableWhichThrows<IOException>, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(ReverseProxy.class.getName());

    /**
     * @see ProxyConnectionHandler#handleConnection(InputStream, OutputStream, InputStream, OutputStream,
     *      InetSocketAddress, InetSocketAddress, InetSocketAddress, InetSocketAddress, Stoppable)
     */
    public
    interface ProxyConnectionHandler {

        // ECLIPSE 4.2.1 has problems with @NotNullByDefault and this interface; the only way to NOT produce a warning
        // is to add @NotNullByDefault(false) and then @NotNull to each parameter.

        /**
         * This method is invoked when the {@link ReverseProxy} has accepted a connection from a client and created
         * the connection to the remote server.
         *
         * @param clientIn                  Stream from the client
         * @param clientOut                 Stream to the client
         * @param serverIn                  Stream from the server
         * @param serverOut                 Stream to the server
         * @param clientLocalSocketAddress  Local address of the connection to the client
         * @param clientRemoteSocketAddress Remote address of the connection to the client
         * @param serverLocalSocketAddress  Local address of the connection to the server
         * @param serverRemoteSocketAddress Remote address of the connection to the server
         * @param stoppable                 Stopping this shuts the reverse proxy down
         */
        @NotNullByDefault(false) void
        handleConnection(
            @NotNull InputStream       clientIn,
            @NotNull OutputStream      clientOut,
            @NotNull InputStream       serverIn,
            @NotNull OutputStream      serverOut,
            @NotNull InetSocketAddress clientLocalSocketAddress,
            @NotNull InetSocketAddress clientRemoteSocketAddress,
            @NotNull InetSocketAddress serverLocalSocketAddress,
            @NotNull InetSocketAddress serverRemoteSocketAddress,
            @NotNull Stoppable         stoppable
        ) throws IOException;
    }

    private final TcpServer server;

    /**
     * @param endpoint                The local TCP port (see {@link ServerSocket#ServerSocket(int)} and the local
     *                                interface this reverse proxy will bind to, see {@link
     *                                ServerSocket#ServerSocket(int, int, InetAddress)}
     * @param backlog                 The listen backlog (see {@link ServerSocket#ServerSocket(int, int)}
     * @param serverAddress           Address of the remote server to connect to
     * @param serverConnectionProxy   Used to create connections to the remote server; see {@link Socket#Socket(Proxy)}
     * @param serverConnectionTimeout See {@link Socket#connect(java.net.SocketAddress, int)}
     */
    public
    ReverseProxy(
        InetSocketAddress            endpoint,
        int                          backlog,
        final InetSocketAddress      serverAddress,
        final Proxy                  serverConnectionProxy,
        final int                    serverConnectionTimeout,
        final ProxyConnectionHandler proxyConnectionHandler
    ) throws IOException {
        this.server = new TcpServer(endpoint, backlog, new TcpServer.ConnectionHandler() {

            @Override public void
            handleConnection(
                InputStream       clientIn,
                OutputStream      clientOut,
                InetSocketAddress clientLocalSocketAddress,
                InetSocketAddress clientRemoteSocketAddress,
                final Stoppable   stoppable
            ) throws IOException {
                final Socket serverConnection = new Socket(serverConnectionProxy);

                if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Connecting with {0}", serverAddress);
                {
                    long t = System.currentTimeMillis();
                    try {
                        serverConnection.connect(serverAddress, serverConnectionTimeout);
                    } catch (SocketTimeoutException ste) {
                        LOGGER.log(
                            WARNING,
                            "Connecting with {0} timed out after {1} ms",
                            new Object[] { serverAddress, System.currentTimeMillis() - t }
                        );
                        return;
                    } catch (IOException ioe) {
                        throw ExceptionUtil.wrap("Connecting with " + serverAddress, ioe);
                    }
                }
                try {

                    InetSocketAddress serverLocalSocketAddress = (
                        (InetSocketAddress) serverConnection.getLocalSocketAddress()
                    );
                    InetSocketAddress serverRemoteSocketAddress = (
                        (InetSocketAddress) serverConnection.getRemoteSocketAddress()
                    );

                    if (LOGGER.isLoggable(FINE)) {
                        LOGGER.log(
                            FINE,
                            "Connected {0} => {1}",
                            new Object[] { serverLocalSocketAddress, serverRemoteSocketAddress }
                        );
                    }

                    proxyConnectionHandler.handleConnection(
                        clientIn,
                        clientOut,
                        serverConnection.getInputStream(),
                        serverConnection.getOutputStream(),
                        clientLocalSocketAddress,
                        clientRemoteSocketAddress,
                        serverLocalSocketAddress,
                        serverRemoteSocketAddress,
                        StoppableUtil.toStoppable(serverConnection)
                    );
                } finally {
                    try { serverConnection.close(); } catch (Exception e) {}
                }
            }
        });
    }

    /** @return The local address of the passive socket of the reverse proxy */
    public InetSocketAddress
    getEndpointAddress() {
        return this.server.getEndpointAddress();
    }

    @Override public void
    run() throws IOException {
        this.server.run();
    }

    @Override public void
    stop() {
        this.server.stop();
    }
}
