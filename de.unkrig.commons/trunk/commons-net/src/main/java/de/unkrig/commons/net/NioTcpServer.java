
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.unkrig.commons.io.Multiplexer;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.lang.protocol.StoppableUtil;

/**
 * A runnable that accepts TCP connections on a given interface/port and passes them to a {@link ConnectionHandler}.
 * Does not supprt SSL (yet).
 *
 * <p>This implementation, opposed to {@link TcpServer}, does use {@code java.nio.channels}, i.e. connections are
 * handled <i>asynchronously</i> and do not block one thread each. In other words, it scales well for thousands of
 * client connections.
 */
public
class NioTcpServer implements Stoppable {

    private static final Logger LOGGER = Logger.getLogger(NioTcpServer.class.getName());

    private static final AtomicInteger CONNECTION_COUNT = new AtomicInteger();

    private final Executor executor = new ThreadPoolExecutor(
        10,                                   // corePoolSize
        100,                                  // maximumPoolSize
        10L,                                  // keepAliveTime
        TimeUnit.SECONDS,                     // unit
        new ArrayBlockingQueue<Runnable>(100) // workQueue
    );

    private final Multiplexer multiplexer;
    {
        try {
            this.multiplexer = new Multiplexer();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Create a server socket with the specified endpoint and listen backlog.
     * <p>
     * The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept connect
     * requests to one of its addresses. If address of the {@code endpoint} is the wildcard address, it will default
     * accepting connections on any/all local addresses.
     * <p>
     * The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemoral port
     * will be picked for the server socket.
     * <p>
     * The backlog argument must be a positive value greater than 0. If the value passed if equal or less than 0, then
     * the default value will be assumed.
     *
     * @param endpoint The local port and {@link InetAddress} the server will bind to. If {@code null}, then the
     *                 system will pick up an ephemeral port and a valid local address to bind the socket.
     * @param backlog  The listen backlog
     */
    private static ServerSocketChannel
    serverSocketChannel(InetSocketAddress endpoint, int backlog) throws IOException {

        LOGGER.log(FINE, "Creating server on {0}", endpoint);

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(endpoint, backlog);
        ssc.configureBlocking(false);
        return ssc;
    }

    /**
     * Represents a TCP connection to a remote peer (e.g. a server or a client).
     *
     * @see #handleConnection(ReadableByteChannel, WritableByteChannel, InetSocketAddress, InetSocketAddress,
     *      Multiplexer, Stoppable)
     */
    public
    interface ConnectionHandler {

        /**
         * A connection was accepted; use the {@code multiplexer} to wait until {@code in}, {@code out} or some
         * other {@link SelectableChannel} becomes readable or writable.
         *
         * @param stoppable Stopping this will break the connection
         */
        void
        handleConnection(
            ReadableByteChannel in,
            WritableByteChannel out,
            InetSocketAddress   localSocketAddress,
            InetSocketAddress   remoteSocketAddress,
            Multiplexer         multiplexer,
            Stoppable           stoppable
        ) throws Exception;
    }

    /**
     * Create a server with the specified endpoint and listen backlog.
     * <p>
     * The address of the {@code endpoint} can be used on a multi-homed host for a server that will only accept connect
     * requests to one of its addresses. If address of the {@code endpoint} is the wildcard address, it will default
     * accepting connections on any/all local addresses.
     * <p>
     * The port of the {@code endpoint} must be between 0 and 65535, inclusive. If it is zero, then an ephemoral port
     * will be picked for the server socket.
     * <p>
     * The backlog argument must be a positive value greater than 0. If the value passed if equal or less than 0, then
     * the default value will be assumed.
     *
     * @param endpoint The local port and {@link InetAddress} the server will bind to. If {@code null}, then the
     *                 system will pick up an ephemeral port and a valid local address to bind the socket.
     * @param backlog  The listen backlog
     * @return         The address and port of the <i>actual</i> endpoint, which may differ from the <i>given</i>
     *                 endpoint.
     */
    public InetSocketAddress
    addServer(
        InetSocketAddress       endpoint,
        int                     backlog,
        final ConnectionHandler clientConnectionHandler
    ) throws IOException {

        final ServerSocketChannel serverSocketChannel = serverSocketChannel(endpoint, backlog);

        // Register the serverSocketChannel for OP_ACCEPT.
        if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Accepting connections on {0}", serverSocketChannel);
        this.multiplexer.register(serverSocketChannel, SelectionKey.OP_ACCEPT, new RunnableWhichThrows<IOException>() {

            @Override public void
            run() throws IOException {

                // Accept the connection request.
                final SocketChannel clientSocketChannel = serverSocketChannel.accept();
                if (clientSocketChannel == null) return;

                clientSocketChannel.configureBlocking(false);
                final int connectionNumber = CONNECTION_COUNT.incrementAndGet();
                if (LOGGER.isLoggable(FINE)) LOGGER.log(
                    FINE,
                    "Client connection #{0} accepted: {1}",
                    new Object[] { connectionNumber, clientSocketChannel }
                );

                clientSocketChannel.configureBlocking(true);

                try {

                    Socket socket = clientSocketChannel.socket();
                    clientConnectionHandler.handleConnection(
                        clientSocketChannel,                                 // in
                        clientSocketChannel,                                 // out
                        (InetSocketAddress) socket.getLocalSocketAddress(),  // localSocketAddress
                        (InetSocketAddress) socket.getRemoteSocketAddress(), // remoteSocketAddress
                        NioTcpServer.this.multiplexer,                       // multiplexer
                        StoppableUtil.toStoppable(clientSocketChannel)       // stoppable
                    );
                    if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Connection {0} handled", clientSocketChannel);

                    clientSocketChannel.close();
                } catch (EOFException eofe) {
                    if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Connection {0} closed by client", clientSocketChannel); // SUPPRESS CHECKSTYLE LineLength
                    try { clientSocketChannel.close(); } catch (Exception e1) {}
                } catch (Exception e) {

                    // The client connection handler threw an exception - close the client connection.
                    if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, clientSocketChannel.toString(), e);
                    try { clientSocketChannel.close(); } catch (Exception e1) {}
                }
            }
        });

        return (InetSocketAddress) serverSocketChannel.socket().getLocalSocketAddress();
    }

    /**
     * Starts this server.
     *
     * @param multiplexerThreadCount Number of threads accepting connections. Notice that <i>accepted connections</i>
     *                               are handled by <i>additional</i> threads, so a value of 1 is appropriate in many
     *                               cases. 
     */
    public void
    start(int multiplexerThreadCount) {

        assert multiplexerThreadCount >= 1;

        for (int i = 0; i < multiplexerThreadCount; i++) {
            this.executor.execute(new Runnable() {

                @Override public void
                run() {
                    try {
                        NioTcpServer.this.multiplexer.run();
                    } catch (ClosedChannelException cce) {
                        if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Terminating (ChannelClosedException)");
                        return;
                    } catch (IOException ioe) {
                        if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Terminating", ioe);
                        return;
                    } catch (RuntimeException re) {
                        if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Terminating", re);
                        return;
                    }
                }
            });
        }
    }

    @Override public void
    stop() {
        this.multiplexer.stop();
    }
}
