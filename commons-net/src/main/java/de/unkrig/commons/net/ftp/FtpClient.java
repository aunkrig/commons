
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

package de.unkrig.commons.net.ftp;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.net.ftp.FtpServer.CommandCode;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An FTP client.
 */
public
class FtpClient {

    public
    FtpClient(InputStream controlIn, OutputStream controlOut, InetAddress controlLocalAddress)
    throws IOException {
        this.controlIn           = LineUtil.lineProducerISO8859_1(controlIn);
        this.controlOut          = LineUtil.lineConsumerISO8859_1(controlOut);
        this.controlLocalAddress = controlLocalAddress;

        this.receiveReply();
    }

    /** @see ServerSocket#setSoTimeout(int) */
    public void
    setDataConnectionAcceptTimeout(int dataConnectionAcceptTimeout) {
        this.dataConnectionAcceptTimeout = dataConnectionAcceptTimeout;
    }

    /**
     * Logs in to the remote FTP server.
     */
    public void
    login(String user, String password) throws IOException {

        this.sendCommand(CommandCode.USER, user);
        if (this.receiveReply(230, 331) == 331) {

            this.sendCommand(CommandCode.PASS, password);
            this.receiveReply(230, 202);
        }
    }

    /**
     * Changes the remote working directory.
     */
    public void
    changeWorkingDirectory(String directory) throws IOException {

        this.sendCommand(CommandCode.CWD, directory);
        this.receiveReply(250);
    }

    /**
     * Switches fro ACTIVE to PASSIVE mode.
     */
    public void
    passive() throws IOException {
        ServerSocket adcss = this.activeDataConnectionServerSocket;
        if (adcss != null) {
            adcss.close();
            this.activeDataConnectionServerSocket = null;
        }

        this.sendCommand(CommandCode.PASV);
        String  response = this.receiveReply(227);
        Matcher matcher  = Pattern.compile(".*?(\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+).*").matcher(response);
        if (!matcher.matches()) throw new IOException("Invalid response '" + response + "' to PASV received");
        this.passiveDataRemoteSocketAddress = new InetSocketAddress(
            InetAddress.getByAddress(new byte[] {
                (byte) Integer.parseInt(matcher.group(1)),
                (byte) Integer.parseInt(matcher.group(2)),
                (byte) Integer.parseInt(matcher.group(3)),
                (byte) Integer.parseInt(matcher.group(4)),
            }),
            256 * Integer.parseInt(matcher.group(5)) + Integer.parseInt(matcher.group(6))
        );

        this.dataTransferMode = DataTransferMode.PASSIVE;
    }

    /**
     * @param port Pass 0 to pick an ephemeral port.
     */
    public void
    active(int port) throws IOException {
        {
            ServerSocket adcss = this.activeDataConnectionServerSocket;
            if (adcss != null) {
                adcss.close();
                this.activeDataConnectionServerSocket = null;
            }
        }

        ServerSocket adcss = (
            this.activeDataConnectionServerSocket = new ServerSocket(port, 1, this.controlLocalAddress)
        );
        adcss.setSoTimeout(this.dataConnectionAcceptTimeout);

        byte[] address = this.controlLocalAddress.getAddress();
        port = adcss.getLocalPort();

        this.sendCommand(CommandCode.PORT, (
            (0xff & address[0]) + ","
            + (0xff & address[1]) + ","
            + (0xff & address[2]) + ","
            + (0xff & address[3]) + ","
            + (0xff & port >> 8) + ","
            + (0xff & port)
        ));
        this.receiveReply(200);

        this.dataTransferMode = DataTransferMode.ACTIVE;
    }

    /**
     * The caller is responsible for closing the returned {@link InputStream}.
     */
    public InputStream
    retrieve(String fileName) throws IOException {

        // Switch to BINARY mode.
        this.sendCommand(CommandCode.TYPE, "I");
        this.receiveReply(200);

        // Open the data connection for the retrieval.
        final Socket dataSocket = this.dataConnection();

        this.sendCommand(CommandCode.RETR, fileName);

        // Return an InputStream which automagically closes the data connection.
        return new FilterInputStream(dataSocket.getInputStream()) {

            @Override public void
            close() throws IOException {
                FtpClient.this.receiveReply(226, 250);
                LOGGER.fine("File retrieval complete, closing data connection");
                super.close();
                dataSocket.close();
            }
        };
    }

    /**
     * Releases all resources associated with the connection to the FTP server, specifically an open data
     * connection.
     */
    public void
    dispose() {
        ServerSocket adcss = this.activeDataConnectionServerSocket;
        if (adcss != null) {
            LOGGER.fine("Closing active data connection server socket");
            try { adcss.close(); } catch (IOException e) {}
            this.activeDataConnectionServerSocket = null;
        }
    }

    // IMPLEMENTATION

    private static final Logger LOGGER = Logger.getLogger(FtpClient.class.getName());

    private enum DataTransferMode { ACTIVE, PASSIVE }

    private static final
    class Reply {

        final int    statusCode;
        final String text;

        Reply(int statusCode, String text) {
            this.statusCode = statusCode;
            this.text       = text;
        }

        @Override public String
        toString() {
            return this.statusCode + " " + this.text;
        }
    }

    private final ProducerWhichThrows<String, IOException> controlIn;
    private final ConsumerWhichThrows<String, IOException> controlOut;
    private final InetAddress                              controlLocalAddress;
    private int                                            dataConnectionAcceptTimeout = 20000;

    private DataTransferMode                               dataTransferMode = DataTransferMode.ACTIVE;
    @Nullable private InetSocketAddress                    passiveDataRemoteSocketAddress;
    @Nullable private ServerSocket                         activeDataConnectionServerSocket;

    /**
     * Establishes the data connection, calls the {@code action}, then closes the data connection.
     */
    private Socket
    dataConnection() throws IOException {

        // Establish the data connection.
        switch (this.dataTransferMode) {

        case ACTIVE:
            if (this.activeDataConnectionServerSocket == null) {
                this.active(0);
            }
            {
                ServerSocket adcss = this.activeDataConnectionServerSocket;
                assert adcss != null;
                LOGGER.fine("Accepting data connection on '" + adcss.getLocalSocketAddress() + "'");
                return adcss.accept();
            }

        case PASSIVE:
            {
                InetSocketAddress pdrsa = this.passiveDataRemoteSocketAddress;
                assert pdrsa != null;
                LOGGER.fine("Creating data connection to '" + pdrsa + "'");
                return new Socket(pdrsa.getAddress(), pdrsa.getPort());
            }

        default:
            throw new IllegalStateException();
        }
    }

    private void
    sendCommand(CommandCode commandCode) throws IOException {
        String line = commandCode.toString();
        LOGGER.fine(">>> " + line);
        this.controlOut.consume(line);
    }

    private void
    sendCommand(CommandCode commandCode, String argument) throws IOException {
        String line = commandCode + " " + argument;
        LOGGER.fine(">>> " + line);
        this.controlOut.consume(line);
    }

    /**
     * @return The text of the reply
     */
    private String
    receiveReply(int statusCode) throws IOException {
        Reply reply = this.receiveReply();
        if (reply.statusCode == statusCode) return reply.text;

        throw new IOException("Expected reply '" + reply + "'");
    }

    /**
     * @return One of the {@code statusCodes}
     */
    private int
    receiveReply(int... statusCodes) throws IOException {
        Reply reply = this.receiveReply();
        for (int statusCode : statusCodes) {
            if (reply.statusCode == statusCode) return statusCode;
        }

        throw new IOException(
            "Expected reply with on of status codes "
            + Arrays.toString(statusCodes)
            + " instead of '"
            + reply
            + "'"
        );
    }

    private Reply
    receiveReply() throws IOException {
        for (;;) {

            // Read one reply line.
            String line = this.controlIn.produce();
            if (line == null) {
                LOGGER.fine("Socket end-of-input");
                throw new EOFException();
            }
            LOGGER.fine("Received reply '" + line + "'");

            // Scan the reply line.
            Matcher matcher = Pattern.compile("(\\d\\d\\d)([ \\-])+(.*)").matcher(line);
            if (!matcher.matches()) {
                throw new IOException("Invalid reply '" + line + "' received");
            }
            int    statusCode = Integer.parseInt(matcher.group(1));
            String statusText = matcher.group(3);

            // Handle multi-line reply.
            if ("-".equals(matcher.group(2))) {
                for (;;) {
                    line = this.controlIn.produce();
                    if (line == null) {
                        LOGGER.fine("Socket end-of-input in the middle of a multi-line reply");
                        throw new IOException("Socket end-of-input in the middle of a multi-line reply");
                    }
                    if (line.startsWith(matcher.group(1))) break;
                }
            }

            // Ignore 1XX replies.
            if (statusCode >= 200) {

                // Return the reply.
                return new Reply(statusCode, statusText);
            }
        }
    }
}
