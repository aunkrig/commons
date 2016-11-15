
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

package de.unkrig.commons.net.ftp;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.unkrig.commons.io.HexOutputStream;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.ReverseProxy;
import de.unkrig.commons.net.ReverseProxy.ProxyConnectionHandler;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A proxy for the FTP data connection.
 */
public
class DataConnectionProxy {

    private static final Logger LOGGER = Logger.getLogger(DataConnectionProxy.class.getName());

    @Nullable private ReverseProxy reverseProxy;

    private static int                     firstDataConnectionPort;
    private static int                     lastDataConnectionPort;
    @Nullable private static AtomicInteger nextDataConnectionPort;

    /**
     * Defines the port that will be used for the server socket for all data connection proxies.
     * <p>
     *   If {@code first} and {@code last} are zero (which is the default), then an "ephemeral port" is picked for each
     *   data connection proxy (see {@link InetSocketAddress#InetSocketAddress(int)}).
     * </p>
     * <p>
     *   Otherwise, if {@code first} and {@code last} are equal, then exactly that port is used for the server socket
     *   of each data connection proxy (this limits the number of data connections to one at any given time).
     * </p>
     * <p>
     *   Otherwise, {@code first} is used for the server socket of the first data connection proxy, then {@code
     *   first + 1} (or {@code first - 1}, if {@code last < first}), and so on up to and including {@code last},
     *   then {@code first} again, and so on. If the port is already in use, then the next port is tried, until a
     *   "free" port is found, and a {@code BindException} is only thrown if <i>all</i> ports are already in use.
     * </p>
     * <p>
     *   Must not be invoked after the first call to {@link #start(InetAddress, InetSocketAddress)}.
     * </p>
     */
    public static void
    setLocalPortRange(int first, int last) {
        if (nextDataConnectionPort != null) {
            throw new IllegalStateException(
                "'setDataConnectionLocalPortRange()' must not be invoked after the first call to 'start()'"
            );
        }

        DataConnectionProxy.firstDataConnectionPort = first;
        DataConnectionProxy.lastDataConnectionPort  = last;
    }

    /**
     * Finds a free port on the given local interface and creates a server socket.
     *
     * @param bindAddress   The local interface to bind this server to
     * @param remoteAddress The remote address and port to connect to
     * @return              The actual address and port to which the data connection proxy was bound
     * @see #setLocalPortRange(int, int)
     */
    public InetSocketAddress
    start(InetAddress bindAddress, InetSocketAddress remoteAddress) throws IOException {
        this.stop();

        AtomicInteger port = nextDataConnectionPort;
        if (port == null) {
            DataConnectionProxy.nextDataConnectionPort = (
                port = new AtomicInteger(DataConnectionProxy.firstDataConnectionPort)
            );
        }

        int firstTriedPort = -1;
        for (;;) {
            int port2 = (
                port.compareAndSet(lastDataConnectionPort, firstDataConnectionPort) ? firstDataConnectionPort :
                firstDataConnectionPort < lastDataConnectionPort ? port.getAndIncrement() :
                port.getAndDecrement()
            );
            if (firstTriedPort == -1) firstTriedPort = port2;
            try {
                InetSocketAddress endpoint = new InetSocketAddress(bindAddress, port2);
                ReverseProxy      rp       = (this.reverseProxy = new ReverseProxy(
                    endpoint,                      // endpoint
                    0,                             // backlog
                    remoteAddress,                 // serverAddress
                    Proxy.NO_PROXY,                // serverConnectionProxy
                    20000,                         // serverConnectionTimeout
                    new ProxyConnectionHandler() { // proxyConnectionHandler

                        @Override public void
                        handleConnection(
                            InputStream       clientIn,
                            OutputStream      clientOut,
                            InputStream       serverIn,
                            OutputStream      serverOut,
                            InetSocketAddress clientLocalSocketAddress,
                            InetSocketAddress clientRemoteSocketAddress,
                            InetSocketAddress serverLocalSocketAddress,
                            InetSocketAddress serverRemoteSocketAddress,
                            Stoppable         stoppable
                        ) throws IOException {
                            ThreadUtil.parallel(
                                IoUtil.copyRunnable(clientIn, IoUtil.tee(
                                    serverOut,
                                    new HexOutputStream(LogUtil.logWriter(LOGGER, FINER, "> "))
                                )),
                                IoUtil.copyRunnable(serverIn, IoUtil.tee(
                                    clientOut,
                                    new HexOutputStream(LogUtil.logWriter(LOGGER, FINER, "< "))
                                )),
                                stoppable
                            );
                        }
                    }
                ));
                ThreadUtil.runInBackground(rp, Thread.currentThread().getName());
                return rp.getEndpointAddress();
            } catch (BindException be) {
                if (
                    !be.getMessage().startsWith("Address already in use")
                    || port2 != firstTriedPort
                ) throw be;
            }
            if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Port {0} is in use; trying next", port2);
        }
    }

    /**
     * Stops this FTP data connection proxy.
     */
    void
    stop() {
        ReverseProxy rp = this.reverseProxy;
        if (rp != null) rp.stop();
    }
}
