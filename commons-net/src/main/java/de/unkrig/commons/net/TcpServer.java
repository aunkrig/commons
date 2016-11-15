
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
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import de.unkrig.commons.io.HexOutputStream;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.lang.protocol.StoppableUtil;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A runnable that accepts TCP connections on a given interface/port and passes them to a {@link ConnectionHandler}.
 * Supports SSL.
 *
 * <p>Notice that this implementation does not use {@code java.nio.channels}, i.e. connections are handled
 * <i>synchronously</i> and each blocks one thread. In other words, it does not scale well for thousands of client
 * connections; consider using {@link NioTcpServer} for that use case.
 */
public
class TcpServer implements RunnableWhichThrows<IOException>, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

    private final ServerSocket      serverSocket;
    private final ConnectionHandler clientConnectionHandler;

    private static final AtomicInteger CONNECTION_COUNT = new AtomicInteger();

    /**
     * Represents a TCP connection to a remote peer (e.g. a server or a client).
     *
     * @see #handleConnection(InputStream, OutputStream, InetSocketAddress, InetSocketAddress, Stoppable)
     */
    public
    interface ConnectionHandler {

        /**
         * The input stream is readable; consume and process one request from it.
         *
         * @param stoppable Stopping this will break the connection
         */
        void
        handleConnection(
            InputStream       in,
            OutputStream      out,
            InetSocketAddress localSocketAddress,
            InetSocketAddress remoteSocketAddress,
            Stoppable         stoppable
        ) throws Exception;
    }

    /**
     * Creates a (non-secure) server with the specified endpoint and listen backlog.
     * <p>
     *   The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept
     *   connect requests to one of its addresses. If address of the {@code endpoint} is the wildcard address, it will
     *   default accepting connections on any/all local addresses.
     * </p>
     * <p>
     *   The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemeral port
     *  will be picked for the server socket.
     *</p>
     * <p>
     *   The {@code backlog} argument must be a positive value greater than 0. If the value passed if equal or less
     *   than 0, then the default value will be assumed.
     * </p>
     *
     * @param endpoint The {@link InetAddress} and local port the server will bind to; {@link
     *                 InetSocketAddress#InetSocketAddress(int)} will accept connections on any/all local addresses;
     *                 port number zero will pick an ephemeral port
     * @param backlog  The listen backlog
     */
    public
    TcpServer(InetSocketAddress endpoint, int backlog, ConnectionHandler clientConnectionHandler) throws IOException {
        this(TcpServer.serverSocket(endpoint, backlog), clientConnectionHandler);
    }

    /**
     * Creates a secure server with the specified endpoint and listen backlog.
     * <p>
     * The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept connect
     * requests to one of its addresses. If address of the {@code endpoint} is the wildcard address, it will default
     * accepting connections on any/all local addresses.
     * <p>
     * The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemoral port
     * will be picked for the server socket.
     * <p>
     * The {@code backlog} argument must be a positive value greater than 0. If the value passed if equal or less than
     * 0, then the default value will be assumed.
     *
     * @param endpoint The local port and {@link InetAddress} the server will bind to. If {@code null}, then the
     *                 system will pick up an ephemeral port and a valid local address to bind the socket.
     * @param backlog  The listen backlog
     */
    public
    TcpServer(
        InetSocketAddress endpoint,
        int               backlog,
        SSLContext        sslContext,
        ConnectionHandler clientConnectionHandler
    ) throws IOException {
        this(TcpServer.secureServerSocket(endpoint, backlog, sslContext), clientConnectionHandler);
    }

    private
    TcpServer(ServerSocket serverSocket, ConnectionHandler clientConnectionHandler) {

        this.serverSocket            = serverSocket;
        this.clientConnectionHandler = clientConnectionHandler;

        //this.serverSocket.setReuseAddress(true);
        //this.serverSocket.setSoTimeout(20 * 1000); // Causes periodic exceptions.

        if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "{0} created", this.serverSocket);
    }

    /**
     * Create a server socket with the specified endpoint and listen backlog.
     * <p>
     *   The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept
     *   connect requests to one of its addresses. If address of the {@code endpoint} is the
     *   {@link InetAddress#anyLocalAddress() wildcard address}, it will default accepting connections on any/all local
     *   addresses.
     * <p>
     *   The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemoral port
     *   will be picked for the server socket.
     * </p>
     * <p>
     *   The {@code backlog} argument must be a positive value greater than 0. If the value passed if equal or less
     *   than 0, then the default value will be assumed.
     * </p>
     *
     * @param endpoint The {@link InetAddress} and local port the server will bind to; {@link
     *                 InetSocketAddress#InetSocketAddress(int)} will accept connections on any/all local addresses;
     *                 port number zero will pick an ephemeral port
     * @param backlog  The listen backlog
     */
    private static ServerSocket
    serverSocket(InetSocketAddress endpoint, int backlog) throws IOException {

        LOGGER.log(FINE, "Creating server on {0}", endpoint);

        try {
            return new ServerSocket(endpoint.getPort(), backlog, endpoint.getAddress());
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap("Creating server socket for endpoint " + endpoint, ioe);
        }
    }

    /**
     * Creates a secure server socket with the specified endpoint and listen backlog.
     * <p>
     * The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept connect
     * requests to one of its addresses. If the address of the {@code endpoint} is the wildcard address, it will
     * default accepting connections on any/all local addresses.
     * <p>
     * The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemoral port
     * will be picked for the server socket.
     * <p>
     * The {@code backlog} argument must be a positive value greater than 0. If the value passed if equal or less than
     * 0, then the default value will be assumed.
     *
     * @param endpoint The local port and {@link InetAddress} the server will bind to. If {@code null}, then the
     *                 system will pick up an ephemeral port and a valid local address to bind the socket.
     * @param backlog  The listen backlog
     */
    private static SSLServerSocket
    secureServerSocket(
        InetSocketAddress endpoint,
        int               backlog,
        SSLContext        sslContext
    ) throws IOException {

        LOGGER.log(FINE, "Creating secure server socket on {0}", endpoint);

        SSLServerSocket sss = (SSLServerSocket) (
            sslContext
            .getServerSocketFactory()
            .createServerSocket(endpoint.getPort(), backlog, endpoint.getAddress())
        );

        disableProblematicCipherSuites(sss);

        return sss;
    }

    /**
     * Taken from
     * http://stackoverflow.com/questions/30523324/how-to-config-local-jetty-ssl-to-avoid-weak-phermeral-dh-key-error.
     */
    private static void
    disableProblematicCipherSuites(SSLServerSocket sss) {

        // Effectively, to solve the FIREFOX problem, it is enough to disable "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", but
        // who knows...
        {
            Set<String> enabledCipherSuites = new HashSet<String>(Arrays.asList(sss.getEnabledCipherSuites()));
            enabledCipherSuites.remove("SSL_RSA_WITH_DES_CBC_SHA");
            enabledCipherSuites.remove("SSL_DHE_RSA_WITH_DES_CBC_SHA");
            enabledCipherSuites.remove("SSL_DHE_DSS_WITH_DES_CBC_SHA");
            enabledCipherSuites.remove("SSL_RSA_EXPORT_WITH_RC4_40_MD5");
            enabledCipherSuites.remove("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA");
            enabledCipherSuites.remove("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA");
            // Disable cipher suites with Diffie-Hellman key exchange to prevent Logjam attack
            // and avoid the ssl_error_weak_server_ephemeral_dh_key error in recent browsers.
            enabledCipherSuites.remove("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA");
            enabledCipherSuites.remove("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA");
            enabledCipherSuites.remove("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
            enabledCipherSuites.remove("TLS_DHE_DSS_WITH_AES_256_CBC_SHA256");
            enabledCipherSuites.remove("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
            enabledCipherSuites.remove("TLS_DHE_DSS_WITH_AES_256_CBC_SHA");
            enabledCipherSuites.remove("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256");
            enabledCipherSuites.remove("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256");
            enabledCipherSuites.remove("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");    // <= This is the important one!
            enabledCipherSuites.remove("TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
            sss.setEnabledCipherSuites(enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]));
        }
    }

    /**
     * Returns the address and port of the <i>actual</i> endpoint, which may differ from the endpoint given to
     * {@link #TcpServer(InetSocketAddress, int, ConnectionHandler)}
     */
    public InetSocketAddress
    getEndpointAddress() {
        return (InetSocketAddress) this.serverSocket.getLocalSocketAddress();
    }

    @Override public void
    run() throws IOException {

        // Accept connection requests from clients until the end of time.
        for (;;) {
            if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Accepting connections on {0}", this.serverSocket);

            final Socket clientSocket;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (SocketTimeoutException ste) {
                if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "{0}: {1}", new Object[] { this.serverSocket, ste });
                continue;
            } catch (SocketException se) {
                if (
                    "socket closed".equals(se.getMessage())
                    || "Socket closed".equals(se.getMessage())
                    || "Socket is closed".equals(se.getMessage())
                ) {
                    if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "{0}: {1}", new Object[] { this.serverSocket, se });
                    return;
                }
                throw se;
            }

            try {

                //clientConnection.setSoTimeout(300 * 1000); // Causes timeouts when the FTP connection is idle.

                final int connectionNumber = CONNECTION_COUNT.incrementAndGet();

                if (LOGGER.isLoggable(FINE)) LOGGER.log(
                    FINE,
                    "Client connection #{0} accepted: {1}",
                    new Object[] { connectionNumber, clientSocket }
                );

                final InputStream clientIn;
                {
                    InputStream in = clientSocket.getInputStream();
                    if (LOGGER.isLoggable(FINEST)) {
                        in = IoUtil.wye(
                            in,
                            new HexOutputStream(LogUtil.logWriter(LOGGER, FINEST, "<From Client< "))
                        );
                    }
                    clientIn = in;
                }

                final OutputStream clientOut;
                {
                    OutputStream out = clientSocket.getOutputStream();
                    if (LOGGER.isLoggable(FINEST)) {
                        out = IoUtil.tee(
                            out,
                            new HexOutputStream(LogUtil.logWriter(LOGGER, FINEST, ">To   Client> "))
                        );
                    }
                    clientOut = out;
                }

                final InetSocketAddress clientLocalSocketAddress = (
                    (InetSocketAddress) clientSocket.getLocalSocketAddress()
                );
                final InetSocketAddress clientRemoteSocketAddress = (
                    (InetSocketAddress) clientSocket.getRemoteSocketAddress()
                );

                ThreadUtil.runInBackground(
                    new Runnable() {

                        @Override public void
                        run() {
                            try {
                                try {
                                    TcpServer.this.clientConnectionHandler.handleConnection(
                                        clientIn,
                                        clientOut,
                                        clientLocalSocketAddress,
                                        clientRemoteSocketAddress,
                                        StoppableUtil.toStoppable(clientSocket)
                                    );
                                } catch (javax.net.ssl.SSLHandshakeException she) {
                                    if ("Received fatal alert: bad_certificate".equals(she.getMessage())) {
                                        if (LOGGER.isLoggable(FINER)) LOGGER.log(FINER, "Client does not like our server certificate", she); // SUPPRESS CHECKSTYLE LineLength
                                        return;
                                    }
                                    throw she;
                                } catch (SocketException se) {
                                    String m = se.getMessage();
                                    if (
                                        "socket closed".equals(m)
                                        || m.contains("Connection reset")
                                        || "Broken pipe".equals(m)
                                        || "Software caused connection abort: recv failed".equals(m)
                                        || "Software caused connection abort: socket write error".equals(m)
                                    ) {
                                        LOGGER.log(FINEST, "Connection closed by client", se);
                                        return;
                                    }
                                    throw se;
                                } catch (EOFException eofe) {
                                    LOGGER.log(FINEST, "Connection closed by client", eofe);
                                    return;
                                }
                            } catch (Exception e) {
                                LOGGER.log(WARNING, "Exception caught from client connection handler", e);
                            } finally {
                                try { clientSocket.close(); } catch (Exception e) {}
                                LOGGER.fine("Client connection closed");
                            }
                        }
                    },
                    "Connection #" + connectionNumber
                );
            } catch (IOException ioe) {
                try { clientSocket.close(); } catch (Exception e2) {}
                throw ioe;
            } catch (RuntimeException re) {
                try { clientSocket.close(); } catch (Exception e2) {}
                throw re;
            }
        }
    }

    @Override public void
    stop() {
        try { this.serverSocket.close(); } catch (Exception e) {}
    }
}
