
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

package de.unkrig.commons.net.tool.httpd;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.TcpServer;
import de.unkrig.commons.net.http.HttpClientConnectionHandler;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.servlett.FileServlett;

/**
 * A VERY simple HTTP server that serves contents from a "document root" directory.
 */
public
class Httpd implements RunnableWhichThrows<IOException>, Stoppable {

    private final TcpServer tcpServer;

    /**
     * Accepts connections on the <var>endpoint</var>; responds each received request with the contents of the file
     * designated by the <var>documentRootDirectoryNamePattern</var>/<var>path</var>, where the placeholder
     * "<code>{path}</code>" is substituted with the "path" component of the requested URI.
     */
    public
    Httpd(InetSocketAddress endpoint, final String documentRootDirectoryNamePattern) throws IOException {

        this.tcpServer = new TcpServer(
            endpoint,
            0,
            new HttpClientConnectionHandler(new FileServlett() {

                @Override protected File
                getFile(HttpRequest httpRequest) {
                    return new File(documentRootDirectoryNamePattern.replace("{path}", httpRequest.getUri().getPath()));
                }
            })
        );
    }

    /**
     * Accepts secure (HTTPS) connections on the <var>endpoint</var>; responds each received request with the contents
     * of the file designated by the <var>documentRootDirectoryNamePattern</var>/<var>path</var>, where the placeholder
     * "<code>{path}</code>" is substituted with the "path" component of the requested URI.
     */
    public
    Httpd(InetSocketAddress endpoint, SSLContext sslContext, final String documentRootDirectoryNamePattern)
    throws IOException {

        this.tcpServer = new TcpServer(
            endpoint,
            0,
            sslContext,
            new HttpClientConnectionHandler(new FileServlett() {

                @Override protected File
                getFile(HttpRequest httpRequest) {
                    return new File(documentRootDirectoryNamePattern.replace("{path}", httpRequest.getUri().getPath()));
                }
            })
        );
    }

    @Override public void
    run() throws IOException {
        this.tcpServer.run();
    }

    /**
     * Will stop the accepting connections from the server socket, close the server socket, and cause {@link #run()} to
     * return as quickly as possible.
     */
    @Override public void
    stop() {
        this.tcpServer.stop();
    }

    /**
     * The address and the <em>actual</em> port that this {@link Httpd} is bound to.
     *
     * @see #Httpd(InetSocketAddress, String)
     * @see TcpServer#getEndpointAddress()
     */
    public InetSocketAddress
    getEndpointAddress() {
        return this.tcpServer.getEndpointAddress();
    }
}
