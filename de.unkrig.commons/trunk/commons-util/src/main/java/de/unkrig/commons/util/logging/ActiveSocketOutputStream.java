
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

package de.unkrig.commons.util.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.RunnableUtil;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An {@link OutputStream} that connects to a remote server and sends all data to that server. If the connection
 * cannot be established or breaks, the data is silently discarded, but reconnection attempts are made every now and
 * then.
 */
@NotNullByDefault(false) public
class ActiveSocketOutputStream extends OutputStream {

    @NotNull private final InetSocketAddress  remoteAddress;
    @Nullable private final InetSocketAddress localAddress;

    // STATE

    private Socket       socket;             // null if unconnected or closed
    private OutputStream socketOutputStream; // null if unconnected or closed

    /**
     * Attempts to connect to currently unconnected servers every now and then. NULL if closed.
     */
    private Runnable reconnector = RunnableUtil.sparingRunnable(
        new Runnable() {

            @Override public void
            run() { ActiveSocketOutputStream.this.reconnect(); }
        },
        ProducerUtil.every(RECONNECT_INTERVAL)
    );

    // CONSTANTS

    /**
     * How often reconnects are attempted to a previously unreachable client, in milliseconds.
     */
    private static final long RECONNECT_INTERVAL = 10000L;

    /**
     * Socket connect timeout.
     */
    private static final int CONNECT_TIMEOUT = 1000;

    /**
     * Socket write timeout.
     */
    private static final int SO_TIMEOUT = 1000;

    public
    ActiveSocketOutputStream(@NotNull InetAddress addr, int port) {
        this(new InetSocketAddress(addr, port));
    }

    public
    ActiveSocketOutputStream(@NotNull String hostname, int port) {
        this(new InetSocketAddress(hostname, port));
    }

    public
    ActiveSocketOutputStream(@NotNull InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.localAddress  = null;
        this.reconnect();
    }

    /**
     * @param remoteAddress See {@link Socket#connect(java.net.SocketAddress)}
     * @param localAddress  See {@link Socket#bind(java.net.SocketAddress)}
     */
    public
    ActiveSocketOutputStream(@NotNull InetSocketAddress remoteAddress, @Nullable InetSocketAddress localAddress) {
        this.remoteAddress = remoteAddress;
        this.localAddress  = localAddress;
        this.reconnect();
    }

    @Override public void
    write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override public synchronized void
    write(byte[] b, int off, int len) throws IOException {
        if (this.reconnector == null) throw new IOException("closed");

        // Attempt to connect if currently unconnected.
        this.reconnector.run();

        // Write the data to the connection.
        if (this.socketOutputStream != null) {
            try {
                this.socketOutputStream.write(b, off, len);
            } catch (IOException e) {

                // Writing the data failed; close the socket silently.
                try { this.socket.close(); } catch (IOException ioe) {}

                this.socketOutputStream = null;
                this.socket             = null;
            }
        }
    }

    @Override public void
    close() throws IOException {
        if (this.socket != null) this.socket.close();

        this.socketOutputStream = null;
        this.socket             = null;
        this.reconnector        = null;
    }

    private void
    reconnect() {
        if (this.socket == null) {
            Socket       socket;
            OutputStream socketOutputStream;
            try {
                socket = new Socket();
                socket.setReuseAddress(true);
                socket.setSoTimeout(SO_TIMEOUT);
                if (this.localAddress != null) socket.bind(this.localAddress);
                socket.connect(
                    new InetSocketAddress(this.remoteAddress.getAddress(), this.remoteAddress.getPort()),
                    CONNECT_TIMEOUT
                );
                socketOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                return;
            }

            this.socket             = socket;
            this.socketOutputStream = socketOutputStream;
        }
    }
}
