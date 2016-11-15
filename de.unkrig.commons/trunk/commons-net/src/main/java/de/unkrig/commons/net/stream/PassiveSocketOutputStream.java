
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

package de.unkrig.commons.net.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.RunnableUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * An {@link OutputStream} that accepts connections from remote clients and sends the data to all connected clients.
 * This includes the case where <i>no</i> client is connected, and the data is thus silently discarded.
 */
@NotNullByDefault(false) public
class PassiveSocketOutputStream extends OutputStream {

    // STATE

    private final ServerSocket              serverSocket;
    private final Map<Socket, OutputStream> connections = new HashMap<Socket, OutputStream>();

    // CONSTANTS

    /**
     * How often the server socket is checked for connection requests, in milliseconds.
     */
    private static final long ACCEPT_INTERVAL = 1000L;

    /**
     * How long the server waits for the client to respond to the server's ACCEPT.
     */
    private static final int ACCEPT_TIMEOUT = 1;

    /**
     * Socket write timeout.
     */
    private static final int SO_TIMEOUT = 1000;

    /**
     * Checks for and accepts incoming connection requests from clients every now and then.
     */
    private final Runnable connectionAcceptor = RunnableUtil.sparingRunnable(
        new Runnable() {

            @Override public void
            run() { PassiveSocketOutputStream.this.acceptConnections(); }
        },
        ProducerUtil.every(ACCEPT_INTERVAL)
    );
    private static final int BACKLOG = 50;

    public
    PassiveSocketOutputStream(int port) throws IOException {
        this.serverSocket = new ServerSocket(port, BACKLOG);
        this.serverSocket.setSoTimeout(1);
        this.acceptConnections();
    }

    public
    PassiveSocketOutputStream(InetSocketAddress localAddress) throws IOException {
        this.serverSocket = new ServerSocket(localAddress.getPort(), BACKLOG, localAddress.getAddress());
        this.serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
        this.acceptConnections();
    }

    @Override public void
    write(int b) {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override public synchronized void
    write(byte[] b, int off, int len) {

        // Check for pending connection requests.
        this.connectionAcceptor.run();

        // Send the data to all connected clients.
        for (Entry<Socket, OutputStream> entry : this.connections.entrySet()) {
            Socket       socket       = entry.getKey();
            OutputStream outputStream = entry.getValue();
            try {
                outputStream.write(b, off, len);
            } catch (IOException e) {
                try { socket.close(); } catch (IOException ioe) {}
                this.connections.remove(socket);
            }
        }
    }

    private void
    acceptConnections() {
        for (;;) {
            Socket socket;
            try {
                socket = this.serverSocket.accept();
                socket.setReuseAddress(true);
                socket.setSoTimeout(SO_TIMEOUT);
                OutputStream outputStream = socket.getOutputStream();
                this.connections.put(socket, outputStream);
            } catch (IOException e) {
                return;
            }
        }
    }
}
