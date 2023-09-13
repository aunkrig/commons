
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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.net.ftp.FtpServer.CommandCode;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.collections.MapUtil;

/**
 * An FTP client.
 */
public
class FtpClient {

	// Example:
	// drwxr-x---  35 ftp      ftp          8192 Mar 11 22:14 ..
	private static final Pattern
	DIR_LINE_PATTERN = Pattern.compile("(..........) +\\d+ +(\\w+) +(\\w+) +(\\d+) (\\w\\w\\w) +(\\d+) +(?:(\\d+):(\\d+)|(\\d+)) +(.*)");

	private static final Map<String, Integer>
	MONTH_NAME_TO_INT = MapUtil.map(
		"Jan", 1,
		"Feb", 2,
		"Mar", 3,
		"Apr", 4,
		"May", 5,
		"Jun", 6,
		"Jul", 7,
		"Aug", 8,
		"Sep", 9,
		"Oct", 10,
		"Nov", 11,
		"Dec", 12
	);

	private static final Map<Integer, String>
	INT_TO_MONTH_NAME = MapUtil.map(
		1,  "Jan",
		2,  "Feb",
		3,  "Mar",
		4,  "Apr",
		5,  "May",
		6,  "Jun",
		7,  "Jul",
		8,  "Aug",
		9,  "Sep",
		10, "Oct",
		11, "Nov",
		12, "Dec"
	);

	public
    FtpClient(InputStream controlIn, OutputStream controlOut, InetAddress controlLocalAddress)
    throws IOException {
        this.controlIn           = LineUtil.lineProducerUtf8(controlIn);
        this.controlOut          = LineUtil.lineConsumerUtf8(controlOut);
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
        if (this.receiveReply(230, 331).statusCode == 331) {

            this.sendCommand(CommandCode.PASS, password);
            this.receiveReply(230, 202);
        }
    }
    
    /**
     * Gets the remote working directory.
     */
    public String
    getWorkingDirectory() throws IOException {
    	
    	this.sendCommand(CommandCode.PWD);
    	return this.receiveReply(250, 257).text;
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
     * Switches from ACTIVE to PASSIVE mode.
     */
    public void
    passive() throws IOException {

    	LOGGER.fine("Switching to passive mode");

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
     * Creates a new passive socket for an active data transfer on the given port.
     * 
     * @param port Pass 0 to pick an ephemeral port.
     */
    public void
    active(int port) throws IOException {

    	LOGGER.fine("Switching to active mode");

        {
            ServerSocket adcss = this.activeDataConnectionServerSocket;
            if (adcss != null) {
                adcss.close();
                this.activeDataConnectionServerSocket = null;
            }
        }
    	this.passiveDataRemoteSocketAddress = null;

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
     * Creates a remote resource, in {@link #active(int)} or {@link #passive()} data transfer mode.
     * The caller is responsible for closing the returned {@link OutputStream}.
     */
    public OutputStream
    store(String fileName) throws IOException {
    	
    	// Switch to BINARY mode.
    	this.sendCommand(CommandCode.TYPE, "I");
    	this.receiveReply(200);
    	
    	// Open the data connection for the retrieval.
    	final Socket dataSocket = this.dataConnection();
    	
    	this.sendCommand(CommandCode.STOR, fileName);
    	
    	// Return an InputStream which automagically closes the data connection.
    	return new FilterOutputStream(dataSocket.getOutputStream()) {
    		
    		@Override public void
    		close() throws IOException {
    			FtpClient.this.receiveReply(226, 250);
    			LOGGER.fine("File storage complete, closing data connection");
    			super.close();
    			dataSocket.close();
    		}
    	};
    }

    /**
     * Retrieves the content of a remote resource, in {@link #active(int)} or {@link #passive()} data transfer mode.
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
        this.receiveRawReply(150);

        // Return an InputStream which automagically closes the data connection.
        return new FilterInputStream(dataSocket.getInputStream()) {

            @Override public void
            close() throws IOException {
                LOGGER.fine("File retrieval complete, closing data connection");
                
                // Close data connection FIRST, THEN wait for 226 response.
                super.close();
                dataSocket.close();
                FtpClient.this.receiveReply(226);
            }
        };
        
    }

    /**
     * Like {@link #list(String)}, but, as an additional service, deserializes the received directory listing lines.
     */
    public ProducerWhichThrows<DirEntry, IOException>
    listEntries(@Nullable String name) throws IOException {
    	
    	final ProducerWhichThrows<String, IOException> lineProducer = this.list(name);
    	
    	return new ProducerWhichThrows<DirEntry, IOException>() {

			@Override @Nullable public DirEntry
			produce() throws IOException {
				
				String line = lineProducer.produce();
				if (line == null) return null;
				
				return FtpClient.deserializeDirEntry(line);
			}
		};
    }

    /**
     * Lists a given remote directory or file. The returned producer produces directory listing lines, and after these
     * {@code null}.
     * <p>
     *   Notice that the format of a listing is notoriously server-dependent.
     * </p>
     */
    public ProducerWhichThrows<String, IOException>
    list(@Nullable String name) throws IOException {
    	
    	LOGGER.fine(name == null ? "Listing cwd" : "Listing dir \"" + name + "\"");

    	// Switch to BINARY mode.
    	this.sendCommand(CommandCode.TYPE, "I");
    	this.receiveReply(200);
    	
    	// Open the data connection for the dir listing.
    	final Socket dataSocket = this.dataConnection();
    	
    	this.sendCommand(CommandCode.LIST, name);
    	this.receiveRawReply(150);
    	
    	BufferedReader br = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.US_ASCII));

    	return new ProducerWhichThrows<String, IOException>() {

    		int n;

    		@Override @Nullable public String
			produce() throws IOException {
				String line = br.readLine();
				if (line == null) {
					LOGGER.fine("Listing complete after " + this.n + " lines");
					br.close();
					dataSocket.close();
					FtpClient.this.receiveReply(226);
					return null;
				}
    			LOGGER.fine("Received listing line #" + this.n + ": " + line);
				this.n++;
				return line;
			}
		};
    }
    
    public ProducerWhichThrows<String, IOException>
    nlist(@Nullable String name) throws IOException {
    	
    	LOGGER.fine(name == null ? "Nlisting cwd" : "Nlisting dir \"" + name + "\"");
    	
    	// Switch to BINARY mode.
    	this.sendCommand(CommandCode.TYPE, "I");
    	this.receiveReply(200);
    	
    	// Open the data connection for the retrieval.
    	final Socket dataSocket = this.dataConnection();
    	
    	this.sendCommand(CommandCode.NLST, name);
    	
    	BufferedReader br = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.US_ASCII));
    	
    	return new ProducerWhichThrows<String, IOException>() {
    		
    		int n;

    		@Override @Nullable public String
    		produce() throws IOException {
    			String line = br.readLine();
    			if (line == null) {
    				LOGGER.fine("Nlisting complete after " + this.n + " liness");
    				br.close();
    				dataSocket.close();
    				FtpClient.this.receiveReply(226);
    				return null;
    			}
    			LOGGER.fine("Received nlisting line #" + this.n + ": " + line);
    			this.n++;
    			return line;
    		}
    	};
    }

	public void
	delete(String resourceName) throws IOException {
		this.sendCommand(CommandCode.DELE, resourceName);
		this.receiveReply(200);
	}

	public void
	rename(String from, String to) throws IOException {
		this.sendCommand(CommandCode.RNFR, from);
		this.receiveReply(300);
		this.sendCommand(CommandCode.RNTO, to);
		this.receiveReply(200);
	}

	public static String
	serializeDirEntry(DirEntry dirEntry) {

		String ndel;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(dirEntry.mtime);

		int currentYear = GregorianCalendar.from(ZonedDateTime.now()).get(GregorianCalendar.YEAR);

		// Line example:
		// drwxr-x---  35 ftp      ftp          8192 Mar 11 22:14 ..
		// drwxr-x---  35 ftp      ftp          8192 Mar 11  2011 foo.txt
		ndel = String.format(
			"%crwxr-x---   1 %-8s %-8s %8d %s %2d %5s %s",
			dirEntry.isDir ? 'd' : '-',
			dirEntry.group.substring(0, Math.min(dirEntry.group.length(), 8)),
			dirEntry.user.substring(0, Math.min(dirEntry.user.length(), 8)),
			dirEntry.length,
			FtpClient.INT_TO_MONTH_NAME.get(cal.get(GregorianCalendar.MONTH) + 1),
			cal.get(GregorianCalendar.DAY_OF_MONTH),
			(
				cal.get(GregorianCalendar.YEAR) == currentYear
				? String.format("%d:%02d", cal.get(GregorianCalendar.HOUR_OF_DAY), cal.get(GregorianCalendar.MINUTE))
				: cal.get(GregorianCalendar.YEAR)
			),
			dirEntry.name
		);
		return ndel;
	}

    public static DirEntry
    deserializeDirEntry(String line) throws IOException {

        LOGGER.fine("Deserializing dir entry line \"" + line + "\"");

    	Matcher m = DIR_LINE_PATTERN.matcher(line);
		if (!m.matches()) throw new IOException("Unrecognized listing line format \"" + line + "\"");

		String permissions = m.group(1);
		String user        = m.group(2);
		String group       = m.group(3);
		String length      = m.group(4);
		String monthName   = m.group(5);
		String dayOfMonth  = m.group(6);
		String hourOfDay   = m.group(7);
		String minute      = m.group(8);
		String year        = m.group(9);
		String fileName    = m.group(10);
		
		int currentYear = GregorianCalendar.from(ZonedDateTime.now()).get(GregorianCalendar.YEAR);
		return new DirEntry(
			permissions.charAt(0) == 'd', // isDir
			user,                         // user
			group,                        // group
			Long.parseLong(length),       // length
			new GregorianCalendar(        // mtime
				year == null ? currentYear : Integer.parseInt(year), // year
				MONTH_NAME_TO_INT.get(monthName) - 1,                // month
				Integer.parseInt(dayOfMonth),                        // dayOfMonth
				hourOfDay == null ? 0 : Integer.parseInt(hourOfDay), // hourOfDay
				minute == null ? 0 : Integer.parseInt(minute),       // minute
				0                                                    // second
			).getTime(),
			fileName                      // fileName
		);
    }

    public static
    class DirEntry {
    	public DirEntry(
			boolean isDir,
	    	String  user,
	    	String  group,
	    	long    length,
	    	Date    mtime,
	    	String  name
		) {
    		this.isDir  = isDir;
        	this.user   = user;
        	this.group  = group;
        	this.length = length;
        	this.mtime  = mtime;
        	this.name   = name;
    	}
    	public final boolean isDir;
    	public final String  user;
    	public final String  group;
    	public final long    length;
    	public final Date    mtime;
    	public final String  name;
    }

    public void
    site(String s) throws IOException {
        this.sendCommand(CommandCode.SITE, s);
        this.receiveReply(220, 250);
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
            return this.statusCode + this.text;
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
     * Establishes the data connection.
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
    sendCommand(CommandCode commandCode, @Nullable String argument) throws IOException {
    	
        LOGGER.fine(">>> " + commandCode + (
    		argument == null ? "" :
			commandCode == CommandCode.PASS ? " ***" :
			" " + argument
		));
        
        this.controlOut.consume(argument == null ? commandCode.toString() : commandCode + " " + argument);
    }

    /**
     * @param statusCode    The expected status code
     * @return              The text of the reply
     * @throws FtpException Unexpected status code received
     */
    private String
    receiveReply(int statusCode) throws IOException {
        Reply reply = this.receiveReply();
        if (reply.statusCode == statusCode) return reply.text;

        throw new FtpException(reply.statusCode, reply.text);
    }

    /**
     * @param statusCodes   The expected status codes
     * @return              One of the <var>statusCodes</var>
     * @throws FtpException Unexpected status code received
     */
    private Reply
    receiveReply(int... statusCodes) throws IOException {
        Reply reply = this.receiveReply();
        for (int statusCode : statusCodes) {
            if (reply.statusCode == statusCode) return reply;
        }

        throw new FtpException(reply.statusCode, reply.text);
    }

    /**
     * Receive replies until one has status code 200+.
     */
    private Reply
    receiveReply() throws IOException {
        for (;;) {

        	Reply reply = this.receiveRawReply();

            // Ignore 1XX replies.
            if (reply.statusCode >= 200) return reply;
        }
    }

    /**
     * Receive one reply (including 1xx status codes).
     *
     * @param statusCode    The expected status code
     * @return              The text of the reply
     * @throws FtpException Unexpected status code received
     */
    private String
    receiveRawReply(int statusCode) throws IOException {
        Reply reply = this.receiveRawReply();
        if (reply.statusCode == statusCode) return reply.text;

        throw new FtpException(reply.statusCode, reply.text);
    }

	private Reply
	receiveRawReply() throws IOException {

        // Read one reply line.
        String line = this.controlIn.produce();
        if (line == null) {
            LOGGER.fine("Socket end-of-input");
            throw new EOFException();
        }
        LOGGER.fine("<<< '" + line + "'");

        // Scan the reply line.
        Matcher matcher = Pattern.compile("(\\d\\d\\d)([ \\-])+(.*)").matcher(line);
        if (!matcher.matches()) {
            throw new IOException("Invalid reply '" + line + "' received");
        }
        int    statusCode = Integer.parseInt(matcher.group(1));
        String separator  = matcher.group(2);
        String statusText = matcher.group(3);

        // Handle multi-line reply.
		if ("-".equals(separator)) {
            for (;;) {
                line = this.controlIn.produce();
                if (line == null) {
                    LOGGER.fine("Socket end-of-input in the middle of a multi-line reply");
                    throw new IOException("Socket end-of-input in the middle of a multi-line reply");
                }
                LOGGER.fine("<<< '" + line + "'");
                if (line.startsWith(statusCode + " ")) {
                	statusText += "\n" + line.substring(4);
                	break;
                } else
            	if (line.startsWith(statusCode + "-")) {
            		statusText += "\n" + line.substring(4);
            	} else
                {
                	statusText += "\n" + line;
                }
            }
        }
		
        return new Reply(statusCode, statusText);
	}
}
