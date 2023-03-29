
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

package de.unkrig.commons.net;

import static java.util.logging.Level.FINEST;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.io.HexOutputStream;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A very thin wrapper around {@link Socket#Socket(InetAddress, int)}.
 * <p>
 * Adds a data logging facility, and has <b>idempotent</b> {@link #getInputStream()} and {@link #getOutputStream()}
 * methods (as opposed to {@link Socket}).
 * <p>
 * Data logging is enabled by setting the level of logger {@code de.unkrig.commons.net.TcpClient} to {@link
 * Level#FINEST} or higher.
 *
 * @see #TcpClient(InetAddress, int)
 */
public final
class TcpClient implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(TcpClient.class.getName());

    private final Socket       socket;
    private final InputStream  fromServer;
    private final OutputStream toServer;

    /**
     * @see #TcpClient(InetAddress, int)
     */
    public
    TcpClient(String host, int port) throws IOException {
        this(InetAddress.getByName(host), port);
    }

    /**
     * Connects to the given <var>address</var> and <var>port</var>. Data can be sent to the remote server through the
     * {@link OutputStream} returned by {@link #getOutputStream()}, and data can be read from the remote server
     * through the {@link InputStream} returned by {@link #getInputStream()}.
     */
    public
    TcpClient(InetAddress address, int port) throws IOException {

        try {
            this.socket = new Socket(address, port);
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(address + ":" + port, ioe);
        }
        LOGGER.fine("Connected to " + this.socket.getRemoteSocketAddress());

        InputStream  in  = this.socket.getInputStream();
        OutputStream out = this.socket.getOutputStream();

        boolean logData = LOGGER.isLoggable(FINEST);
        this.fromServer = (
            logData
            ? InputStreams.wye(in, new HexOutputStream(LogUtil.logWriter(LOGGER, FINEST, "<From Server< ")))
            : in
        );
        this.toServer = (
            logData
            ? OutputStreams.tee(out, new HexOutputStream(LogUtil.logWriter(LOGGER, FINEST, ">To   Server> ")))
            : out
        );
    }

    /** @return The stream from the server */
    public InputStream
    getInputStream() {
        return this.fromServer;
    }

    /** @return The stream to the server */
    public OutputStream
    getOutputStream() {
        return this.toServer;
    }

    @Override public void
    close() throws IOException {

        LOGGER.fine("Closing connection to " + this.socket.getRemoteSocketAddress());

        this.socket.close();
    }

    /** @return The address of the server socket that this client is connected to */
    public SocketAddress
    getRemoteSocketAddress() {
        return this.socket.getRemoteSocketAddress();
    }
}
