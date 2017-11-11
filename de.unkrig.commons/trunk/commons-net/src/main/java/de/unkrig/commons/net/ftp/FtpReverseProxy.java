
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.ReverseProxy;
import de.unkrig.commons.net.ReverseProxy.ProxyConnectionHandler;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A reverse FTP proxy.
 */
public
class FtpReverseProxy implements RunnableWhichThrows<IOException> {

    private static final Logger LOGGER = Logger.getLogger(FtpReverseProxy.class.getName());

    private final ReverseProxy reverseProxy;

    /**
     * @see ReverseProxy
     */
    public
    FtpReverseProxy(
        InetSocketAddress endpoint,
        int               backlog,
        InetSocketAddress serverAddress,
        int               serverConnectionTimeout
    ) throws IOException {
        this.reverseProxy = new ReverseProxy(
            endpoint,
            backlog,
            serverAddress,
            Proxy.NO_PROXY,  // FTP does not support proxies.
            serverConnectionTimeout,
            new ProxyConnectionHandler() {

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
                    Connection client = FtpReverseProxy.connection(
                        LineUtil.lineProducerISO8859_1(clientIn),
                        ConsumerUtil.tee(
                            LineUtil.lineConsumerISO8859_1(clientOut),
                            ConsumerUtil.<String, IOException>widen2(
                                LogUtil.logConsumer(FtpReverseProxy.LOGGER, Level.FINE, "<<< ")
                            )
                        )
                    );
                    Connection server = FtpReverseProxy.connection(
                        LineUtil.lineProducerISO8859_1(serverIn),
                        ConsumerUtil.tee(
                            LineUtil.lineConsumerISO8859_1(serverOut),
                            ConsumerUtil.<String, IOException>widen2(
                                LogUtil.logConsumer(FtpReverseProxy.LOGGER, Level.FINE, ">>> ")
                            )
                        )
                    );

                    final DataConnectionProxy dataConnectionProxy = new DataConnectionProxy();

                    // Command / reply processing loop.
                    for (;;) {

                        // Forward replies.
                        for (;;) {

                            // Read one reply.
                            String reply = this.readReply(server);
                            FtpReverseProxy.LOGGER.log(Level.FINER, "Reply ''{0}'' received", reply);

                            if (reply == null) {
                                FtpReverseProxy.LOGGER.fine("Connection closed by remote server");
                                break;
                            }

                            Matcher m;
                            if ((m = FtpReverseProxy.REPLY_227.matcher(reply)).matches()) {

                                // Patch reply 227 (reply to PASV command).
                                InetAddress address = InetAddress.getByAddress(new byte[] {
                                    (byte) Integer.parseInt(m.group(1)),
                                    (byte) Integer.parseInt(m.group(2)),
                                    (byte) Integer.parseInt(m.group(3)),
                                    (byte) Integer.parseInt(m.group(4)),
                                });
                                int port = (Integer.parseInt(m.group(5)) << 8) + Integer.parseInt(m.group(6));

                                InetSocketAddress endpoint = dataConnectionProxy.start(
                                    clientLocalSocketAddress.getAddress(),
                                    new InetSocketAddress(address, port)
                                );
                                reply = "227 Entering Passive Mode (" + FtpReverseProxy.commafy(endpoint) + ")";
                            } else
                            if ((m = FtpReverseProxy.REPLY_229.matcher(reply)).matches()) {

                                // Patch reply 229 (reply to EPSV command).
                                int               port     = Integer.parseInt(m.group(1));
                                InetSocketAddress endpoint = dataConnectionProxy.start(
                                    clientLocalSocketAddress.getAddress(),
                                    new InetSocketAddress(serverRemoteSocketAddress.getAddress(), port)
                                );
                                reply = "229 Entering Extended Passive Mode (|||" + endpoint.getPort() + "|)";
                            }

                            // Send the reply to the client.
                            client.writeLine(reply);
                            if (!reply.startsWith("1")) break;
                        }

                        // Forward one command.
                        {

                            // Read one command from the client.
                            String command = client.readLine();
                            FtpReverseProxy.LOGGER.log(Level.FINER, "Command ''{0}'' received", command);
                            if (command == null) break;

                            Matcher m;
                            if ((m = FtpReverseProxy.COMMAND_PORT.matcher(command)).matches()) {

                                // Patch the PORT command.
                                InetAddress address = InetAddress.getByAddress(new byte[] {
                                    (byte) Integer.parseInt(m.group(1)),
                                    (byte) Integer.parseInt(m.group(2)),
                                    (byte) Integer.parseInt(m.group(3)),
                                    (byte) Integer.parseInt(m.group(4)),
                                });
                                int port = (Integer.parseInt(m.group(5)) << 8) + Integer.parseInt(m.group(6));

                                InetSocketAddress endpoint = dataConnectionProxy.start(
                                    serverLocalSocketAddress.getAddress(),
                                    new InetSocketAddress(address, port)
                                );
                                command = "PORT " + FtpReverseProxy.commafy(endpoint);
                            } else
                            if ((m = FtpReverseProxy.COMMAND_EPRT.matcher(command)).matches()) {

                                // Patch the EPRT command.
                                String      protocol = m.group(1);
                                InetAddress address  = InetAddress.getByName(m.group(2));
                                int         port     = Integer.parseInt(m.group(3));

                                InetSocketAddress endpoint = dataConnectionProxy.start(
                                    serverLocalSocketAddress.getAddress(),
                                    new InetSocketAddress(address, port)
                                );

                                // Replacing EPRT commands with PORT commands is generally a good idea, see
                                //   http://svn.apache.org/viewvc/commons/proper/net/tags/NET_3_1/src/main/java/org/
                                //   apache/commons/net/ftp/FTPClient.java?revision=1242980&view=markup
                                // , lines 697 and following. However, since this is a reverse proxy and not a
                                // "protocol fixer", this feature is disabled by default.
                                if (
                                    "1".equals(protocol)
                                    && Boolean.getBoolean(FtpReverseProxy.class.getName() + ".replaceEprtWithPort")
                                ) {
                                    command = "PORT " + FtpReverseProxy.commafy(endpoint);
                                } else {
                                    command = (
                                        "EPRT |"
                                        + (endpoint.getAddress().getAddress().length == 4 ? 1 : 2)
                                        + "|"
                                        + endpoint.getAddress().getHostAddress()
                                        + "|"
                                        + endpoint.getPort()
                                        + "|"
                                    );
                                }
                            }

                            // Send the command to the server.
                            server.writeLine(command);
                        }
                    }
                }

                /**
                 * Parses an FTP reply, as described in <a
                 * href="http://tools.ietf.org/html/rfc959#page-36">RFC 959, Section 4.2 FTP REPLIES</a>.
                 */
                @Nullable private String
                readReply(Connection server) throws IOException {
                    String reply = server.readLine();
                    if (reply == null) return null;

                    if (FtpReverseProxy.REPLY.matcher(reply).matches()) {
                        return reply;
                    } else
                    if (FtpReverseProxy.BRACKETED_REPLY.matcher(reply).matches()) {
                        for (;;) {
                            String s = server.readLine();
                            if (s == null) {
                                throw new IOException("Incomplete bracketed reply");
                            }
                            reply += "\r\n" + s;
                            if (FtpReverseProxy.REPLY.matcher(s).matches()) break;
                        }
                        return reply;
                    } else
                    {
                        throw new IOException("Invalid reply '" + reply + "' received");
                    }
                }
            }
        );
    }
    // SUPPRESS CHECKSTYLE LineLength|JavadocVariable:6
    static final Pattern REPLY           = Pattern.compile("(\\d\\d\\d) (.*)");
    static final Pattern BRACKETED_REPLY = Pattern.compile("(\\d\\d\\d)-(.*)");
    static final Pattern REPLY_227       = Pattern.compile("227 .*\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\).*");
    static final Pattern REPLY_229       = Pattern.compile("229 .*\\(\\|\\|\\|(\\d+)\\|\\).*");
    static final Pattern COMMAND_PORT    = Pattern.compile("PORT (\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)", Pattern.CASE_INSENSITIVE);
    static final Pattern COMMAND_EPRT    = Pattern.compile("EPRT \\|([12])\\|([^\\|]+)\\|(\\d+)\\|", Pattern.CASE_INSENSITIVE);

    /**
     * Converts an (IPv4) {@link InetSocketAddress} into the 'a1,a2,a3,a4,p1,p2' format used by FTP.
     */
    static String
    commafy(InetSocketAddress socketAddress) {
        byte[] address = socketAddress.getAddress().getAddress();
        return (
            (0xff & address[0])
            + ","
            + (0xff & address[1])
            + ","
            + (0xff & address[2])
            + ","
            + (0xff & address[3])
            + ","
            + (0xff & (socketAddress.getPort() >> 8))
            + ","
            + (0xff & socketAddress.getPort())
        );
    }

    @Override public void
    run() throws IOException {
        this.reverseProxy.run();
    }

    /**
     * An entity that lines can be written to and read from.
     */
    interface Connection {

        /** Reads one line from this connection. */
        @Nullable String
        readLine() throws IOException;

        /** Writes one line to this connection. */
        void
        writeLine(String line) throws IOException;
    }

    /**
     * @return A {@link Connection} that delegates to the producer/consumer pair
     */
    static Connection
    connection(
        final ProducerWhichThrows<String, IOException> producer,
        final ConsumerWhichThrows<String, IOException> consumer
    ) {
        return new Connection() {

            @Override @Nullable public String
            readLine() throws IOException { return producer.produce(); }

            @Override public void
            writeLine(String line) throws IOException { consumer.consume(line); }
        };
    }
}
