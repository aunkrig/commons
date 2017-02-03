
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.net.ftp.FtpServer.CommandCode;
import de.unkrig.commons.net.ftp.ftplett.Ftplett;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of one FTP session, i.e. control connection and its state.
 */
public
class FtpSession implements RunnableWhichThrows<IOException> {

    public
    FtpSession(
        InputStream       controlIn,
        OutputStream      controlOut,
        InetSocketAddress controlLocalSocketAddress,
        InetAddress       controlRemoteAddress,
        Ftplett           ftplett
    ) {
        this.controlIn                     = LineUtil.lineProducerISO8859_1(controlIn);
        this.controlOut                    = LineUtil.lineConsumerISO8859_1(controlOut);
        this.interfacE                     = controlLocalSocketAddress.getAddress();
        this.activeDataRemoteSocketAddress = new InetSocketAddress(
            controlRemoteAddress,
            controlLocalSocketAddress.getPort() - 1
        );
        this.ftplett                       = ftplett;
    }

    @Override public void
    run() throws IOException {

        this.sendReply("220 de.unkrig.commons.net.ftp.FtpServer");

        int state = 0;
        try {
            for (;;) {
                final Command c = this.receiveCommand();
                if (c == null) return;
                final String argument = c.argument;

                if (state < 1 && c.code == CommandCode.PASS) {
                    this.sendReply("503 Login with USER first.");
                    continue;
                }
                if (state < 2 && c.code != CommandCode.USER && c.code != CommandCode.PASS) {
                    this.sendReply("530 Please login with USER and PASS.");
                    continue;
                }
                switch (c.code) {

                case SYST:
                    this.sendReply("215 " + System.getProperty("os.name"));
                    break;

                case USER:
                    if (c.argument == null) {
                        this.sendReply("501 'user': Invalid number of parameters.");
                        break;
                    }
                    state = 1;
                    this.sendReply("331 Password required for " + c.argument + ".");
                    break;

                case PASS:
                    state = 2;
                    this.sendReply("230 User logged in.");
                    break;

                case CWD:
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    if (this.ftplett.changeWorkingDirectory(argument)) {
                        this.sendReply("250 cwd command successful.");
                    } else {
                        this.sendReply("550 Requested action not taken");
                    }
                    break;

                case PWD:
                    this.sendReply("257 \"" + this.ftplett.getWorkingDirectory() + "\" is current directory.");
                    break;

                case NOOP:
                    this.sendReply("200 noop command successful.");
                    break;

                case MODE:
                    if (c.argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    if ("S".equals(c.argument)) {
                        this.transmissionMode = TransmissionMode.STREAM;
                        this.sendReply("200 Stream transmission mode selected");
                    } else
                    if ("B".equals(c.argument)) {
                        this.transmissionMode = TransmissionMode.BLOCK;
                        this.sendReply("200 Block transmission mode selected");
                    } else
                    if ("C".equals(c.argument)) {
                        this.transmissionMode = TransmissionMode.COMPRESSED;
                        this.sendReply("200 Compressed transmission mode selected");
                    } else
                    {
                        this.sendReply("501 Invalid transmission mode '" + c.argument + "'");
                    }
                    break;

                case RETR:
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    final InputStream is = this.ftplett.retrieve(argument);
                    if (is == null) {
                        this.sendReply("550 The system cannot find the file specified.");
                        break;
                    }
                    try {
                        this.dataConnection(new ConsumerWhichThrows<Socket, IOException>() {

                            @Override public void
                            consume(Socket dataSocket) throws IOException {
                                long count = IoUtil.copy(is, dataSocket.getOutputStream());
                                LOGGER.fine(count + " bytes sent");
                            }
                        });
                    } finally {
                        try { is.close(); } catch (Exception e) {}
                    }
                    break;

                case LIST: // http://tools.ietf.org/html/rfc959#page-32
                    this.dataConnection(new ConsumerWhichThrows<Socket, IOException>() {

                        @Override public void
                        consume(Socket dataSocket) throws IOException {
                            final Writer w = new OutputStreamWriter(
                                dataSocket.getOutputStream(),
                                Charset.forName("ISO-8859-1")
                            );
                            ConsumerWhichThrows<String, IOException> lineConsumer = (
                                new ConsumerWhichThrows<String, IOException>() {

                                    @Override public void
                                    consume(String line) throws IOException {
                                        w.write(line + "\r\n");
                                    }
                                }
                            );
                            if (!FtpSession.this.ftplett.list(argument, lineConsumer)) {
                                FtpSession.this.sendReply("550 '" + c.argument + "' does not exist");
                            }
                            w.flush();
                        }
                    });
                    break;

                case NLST: // http://tools.ietf.org/html/rfc959#page-33
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    this.dataConnection(new ConsumerWhichThrows<Socket, IOException>() {

                        @Override public void
                        consume(Socket dataSocket) throws IOException {
                            final Writer w = new OutputStreamWriter(
                                dataSocket.getOutputStream(),
                                Charset.forName("ISO-8859-1")
                            );
                            ConsumerWhichThrows<String, IOException> lineConsumer = (
                                new ConsumerWhichThrows<String, IOException>() {

                                    @Override public void
                                    consume(String line) throws IOException {
                                        w.write(line + "\r\n");
                                    }
                                }
                            );
                            if (!FtpSession.this.ftplett.nameList(argument, lineConsumer)) {
                                FtpSession.this.sendReply("550 '" + c.argument + "' does not exist");
                            }
                            w.flush();
                        }
                    });
                    break;

                case PORT:
                    if (c.argument == null) {
                        this.sendReply("501 'port': Invalid number of parameters.");
                        break;
                    }
                    Matcher matcher = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)").matcher(c.argument);
                    if (!matcher.matches()) {
                        this.sendReply("501 Server cannot accept argument.");
                        break;
                    }
                    this.activeDataRemoteSocketAddress = new InetSocketAddress(
                        InetAddress.getByAddress(new byte[] {
                            (byte) Integer.parseInt(matcher.group(1)),
                            (byte) Integer.parseInt(matcher.group(2)),
                            (byte) Integer.parseInt(matcher.group(3)),
                            (byte) Integer.parseInt(matcher.group(4)),
                        }),
                        256 * Integer.parseInt(matcher.group(5)) + Integer.parseInt(matcher.group(6))
                    );
                    this.passiveDataTransfer = false;
                    LOGGER.fine("Client address for data connection: " + this.activeDataRemoteSocketAddress);
                    this.sendReply("200 port command successful.");
                    break;

                case QUIT:
                    return;

                case TYPE:
                    this.sendReply("200 type command successful.");
                    break;

                case PASV:
                    {
                        ServerSocket pdcss = this.passiveDataConnectionServerSocket;
                        if (pdcss == null) {
                            pdcss = (this.passiveDataConnectionServerSocket = new ServerSocket(0, 1));
                        }
                        InetSocketAddress localSocketAddress = (InetSocketAddress) pdcss.getLocalSocketAddress();
                        byte[]            address            = this.interfacE.getAddress();
                        int               port               = localSocketAddress.getPort();
                        this.sendReply(
                            "227 Entering Passive Mode ("
                            + (0xff & address[0]) + ","
                            + (0xff & address[1]) + ","
                            + (0xff & address[2]) + ","
                            + (0xff & address[3]) + ","
                            + (0xff & port >> 8) + "," + (0xff & port)
                            + ")."
                        );
                        this.passiveDataTransfer = true;
                    }
                    break;

                case STOR:
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    final OutputStream os = this.ftplett.store(argument);
                    if (os == null) {
                        this.sendReply("550 The system cannot create the file specified.");
                        break;
                    }
                    try {
                        this.dataConnection(new ConsumerWhichThrows<Socket, IOException>() {

                            @Override public void
                            consume(Socket dataSocket) throws IOException {
                                long count = IoUtil.copy(dataSocket.getInputStream(), os);
                                LOGGER.fine(count + " bytes received");
                            }
                        });
                    } finally {
                        try { os.close(); } catch (Exception e) {}
                    }
                    break;

                case DELE:
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    if (!this.ftplett.delete(argument)) {
                        this.sendReply("550 The system cannot delete the file specified.");
                        break;
                    }
                    this.sendReply("250 File deleted");
                    break;

                case RNFR:
                    if (c.argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    this.renameFrom = c.argument;
                    this.sendReply("350 Requested file action pending further information.");
                    break;

                case RNTO:
                    {
                        if (argument == null) {
                            this.sendReply("501 Invalid number of parameters.");
                            break;
                        }
                        String rf = this.renameFrom;
                        assert rf != null;
                        if (!this.ftplett.rename(rf, argument)) {
                            this.sendReply("553 The system cannot rename the file specified.");
                            break;
                        }
                        this.sendReply("250 File renamed");
                    }
                    break;

                case MDTM:
                    if (argument == null) {
                        this.sendReply("501 Invalid number of parameters.");
                        break;
                    }
                    {
                        Date modificationTime = this.ftplett.getModificationTime(argument);
                        if (modificationTime == null) {
                            this.sendReply(
                                "553 The system cannot determine the modification time of the file specified."
                            );
                            break;
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                        this.sendReply("213 " + sdf.format(modificationTime));
                    }
                    break;
                }
            }
        } finally {
            ServerSocket pdcss = this.passiveDataConnectionServerSocket;
            if (pdcss != null) {
                try { pdcss.close(); } catch (Exception e) {}
            }
        }
    }

    // CONFIGURATION

    private static final Logger LOGGER = Logger.getLogger(FtpSession.class.getName());

    /** See <a href="http://tools.ietf.org/html/rfc959#page-20">RFC959, section 3.4 "TRANSMISSION MODES"</a> */
    private enum TransmissionMode { STREAM, BLOCK, COMPRESSED }

    private final ProducerWhichThrows<String, IOException> controlIn;
    private final ConsumerWhichThrows<String, IOException> controlOut;
    private final InetAddress                              interfacE;
    private final Ftplett                                  ftplett;

    // STATE

    private boolean                     passiveDataTransfer;
    @Nullable private InetSocketAddress activeDataRemoteSocketAddress;
    @Nullable private ServerSocket      passiveDataConnectionServerSocket;
    private TransmissionMode            transmissionMode = TransmissionMode.STREAM;
    @Nullable private String            renameFrom; // For RNFR / RNTO


    // IMPLEMENTATION

    /**
     * Establishes the data connection, calls the <var>action</var>, then closes the data connection.
     */
    private void
    dataConnection(ConsumerWhichThrows<Socket, IOException> action) throws IOException {

        // Establish the data connection.
        Socket dataSocket;
        if (this.passiveDataTransfer) {
            ServerSocket pdcss = this.passiveDataConnectionServerSocket;
            assert pdcss != null;
            LOGGER.fine("Accepting data connection on '" + pdcss.getLocalSocketAddress() + "'");
            dataSocket = pdcss.accept();
        } else {
            InetSocketAddress adrsa = this.activeDataRemoteSocketAddress;
            assert adrsa != null;
            LOGGER.fine("Creating data connection to '" + adrsa + "'");
            dataSocket = new Socket(adrsa.getAddress(), adrsa.getPort());
        }

        if (this.transmissionMode != TransmissionMode.STREAM) { // TODO Transmission modes other than STREAM
            throw new IOException("Transmission mode '" + this.transmissionMode + "' NYI");
        }

        this.sendReply("125 Data connection created; Transfer starting.");
        try {

            // Execute the action on the data connection.
            action.consume(dataSocket);

            // Close the data connection.
            LOGGER.fine("Closing data connection");
            dataSocket.close();
        } finally {
            try { dataSocket.close(); } catch (Exception e) {}
        }
        this.sendReply("226 Transfer complete.");
    }

    class Command {

        /** The command's code. */
        CommandCode code;

        /** The command's optional argument. */
        @Nullable String argument;

        Command(CommandCode code, @Nullable String argument) {
            this.code     = code;
            this.argument = argument;
        }
    }

    /**
     * Wait for a valid command.
     *
     * @return {@code null} if the control connection signals end-of-input
     */
    @Nullable private Command
    receiveCommand() throws IOException {
        for (;;) {
            String line = this.controlIn.produce();
            if (line == null) {
                LOGGER.fine("Socket end-of-input");
                return null;
            }
            LOGGER.fine("<<< " + line);
            int    idx = line.indexOf(' ');
            String code, argument;
            if (idx == -1) {
                code     = line;
                argument = null;
            } else {
                code     = line.substring(0, idx);
                argument = line.substring(idx + 1);
            }
            try {
                return new Command(CommandCode.valueOf(code.toUpperCase()), argument);
            } catch (IllegalArgumentException iae) {
                this.sendReply("500 '" + code + "': command not understood.");
            }
        }
    }

    /**
     * Send a given reply.
     */
    private void
    sendReply(String line) throws IOException {
        LOGGER.fine(">>> " + line);
        this.controlOut.consume(line);
    }
}
