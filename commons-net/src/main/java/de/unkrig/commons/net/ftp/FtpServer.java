
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

package de.unkrig.commons.net.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.TcpServer;
import de.unkrig.commons.net.ftp.ftplett.Ftplett;

/**
 * See <a href="http://tools.ietf.org/html/rfc959">RFC 959</a>: "FILE TRANSFER PROTOCOL (FTP)"
 */
public
class FtpServer implements RunnableWhichThrows<IOException>, Stoppable {

    enum CommandCode {
        // SUPPRESS CHECKSTYLE JavadocVariable
        USER, PASS, NOOP, RETR, CWD, PWD, QUIT, PORT, LIST, NLST, SYST, TYPE, PASV, MODE, STOR, DELE, RNFR, RNTO, MDTM,
        SITE,
    }

    private TcpServer tcpServer;

    /**
     * This {@link FtpServer} will accept connections and run a new {@link FtpSession} with the given
     * <var>Ftplett</var>.
     *
     * @param endpoint The {@link InetAddress} and local port the server will bind to; {@link
     *                 InetSocketAddress#InetSocketAddress(int)} will accept connections on any/all local addresses;
     *                 port number zero will pick an ephemeral port
     */
    public
    FtpServer(InetSocketAddress endpoint, int backlog, final Ftplett ftplett) throws IOException {

        this.tcpServer = new TcpServer(endpoint, backlog, new TcpServer.ConnectionHandler() {

            @Override public void
            handleConnection(
                final InputStream        in,
                final OutputStream       out,
                InetSocketAddress        localSocketAddress,
                final InetSocketAddress  remoteSocketAddress,
                final Stoppable          stoppable
            ) throws IOException {
                new FtpSession(in, out, localSocketAddress, remoteSocketAddress.getAddress(), ftplett).run();
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
}
