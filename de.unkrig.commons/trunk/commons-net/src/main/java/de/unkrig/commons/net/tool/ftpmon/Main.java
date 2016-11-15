
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

package de.unkrig.commons.net.tool.ftpmon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.net.ftp.DataConnectionProxy;
import de.unkrig.commons.net.ftp.FtpReverseProxy;
import de.unkrig.commons.util.logging.SimpleLogging;

/**
 * The FTPMON command line utility.
 */
public final
class Main {

    private Main() {}

    static { SimpleLogging.init(); }

    public static void // SUPPRESS CHECKSTYLE JavadocMethod
    main(String[] args) throws IOException {
        int i = 0;

        // Process global options.
        {
            while (i < args.length) {
                String arg = args[i];
                if (!arg.startsWith("-")) break;
                i++;
                if ("-help".equals(arg)) {
                    // CHECKSTYLE LineLength:OFF
                    System.out.println("Usage:");
                    System.out.println("  java " + Main.class.getName());
                    System.out.println("        [ <global-option> ... ]");
                    System.out.println("        ( [ <local-option> ... ]");
                    System.out.println("              <local-port> <remote-server-host-name> <remote-server-port> ) ...");
                    System.out.println("Valid <global-option>s are:");
                    System.out.println("  -help");
                    System.out.println("  -data-connection-port <port>");
                    System.out.println("                  Use exactly this passive port for data connection forwarding");
                    System.out.println("                  (allows for at most one data connection at a time).");
                    System.out.println("  -data-connection-port <first>-<last>");
                    System.out.println("                  Use the given range for the passive port for data connection");
                    System.out.println("                  forwarding.");
                    System.out.println("  -nowarn         Suppress all messages except errors");
                    System.out.println("  -quiet          Suppress normal output");
                    System.out.println("  -verbose        Log verbose messages");
                    System.out.println("  -debug          Log verbose and debug messages (repeat to get more output)");
                    System.out.println("  -log <level>:<logger>:<handler>:<formatter>:<format>");
                    System.out.println("                  Add logging at level FINE on logger 'de.unkrig' to STDERR");
                    System.out.println("                  using the FormatFormatter and SIMPLE format, or the given");
                    System.out.println("                  arguments which are all optional.");
                    System.out.println("Valid <local-option>s are:");
                    System.out.println("  -backlog <n>    The maximum queue length for incoming request to connect");
                    System.out.println("  -bind-address <address>");
                    System.out.println("                  Accept connect requests to only this address");
                    System.out.println("  -server-connection-timeout <ms>");
                    System.out.println("                  Timeout for creating connections to the remote server");
                    // CHECKSTYLE LineLength:ON
                    System.exit(0);
                } else
                if ("-data-connection-port".equals(arg)) {
                    Matcher m = Pattern.compile("(\\d+)(?:-(\\d+))?").matcher(args[i++]);
                    if (!m.matches()) {
                        System.err.println("Invalid argument to '-data-connection-port'; try '-help'");
                        System.exit(1);
                    }
                    int firstDataConnectionPort = Integer.parseInt(m.group(1));
                    int lastDataConnectionPort  = (
                        m.group(2) == null
                        ? firstDataConnectionPort
                        : Integer.parseInt(m.group(2))
                    );
                    DataConnectionProxy.setLocalPortRange(firstDataConnectionPort, lastDataConnectionPort);
                } else
                if ("-nowarn".equals(arg)) {
                    SimpleLogging.setNoWarn();
                } else
                if ("-quiet".equals(arg)) {
                    SimpleLogging.setQuiet();
                } else
                if ("-verbose".equals(arg)) {
                    SimpleLogging.setVerbose();
                } else
                if ("-debug".equals(arg)) {
                    SimpleLogging.setDebug();
                } else
                if ("-log".equals(arg)) {
                    SimpleLogging.configureLoggers(args[i++]);
                } else
                {
                    break;
                }
            }
        }

        while (i < args.length) {

            // Parse and process local options.
            int         backlog                 = 0;
            InetAddress bindAddress             = null;
            int         serverConnectionTimeout = 0;

            while (i < args.length) {
                String arg = args[i];
                if (!arg.startsWith("-")) break;
                i++;
                if ("-backlog".equals(arg)) {
                    backlog = Integer.parseInt(args[i++]);
                } else
                if ("-bind-address".equals(arg)) {
                    bindAddress = InetAddress.getByName(args[i++]);
                } else
                if ("-server-connection-timeout".equals(arg)) {
                    serverConnectionTimeout = Integer.parseInt(args[i++]);
                } else
                {
                    System.err.println("Invalid command line option '" + arg + "'; try '-help'");
                    System.exit(1);
                }
            }

            // Parse local port / remote server host name / remote server port.
            if (i + 3 > args.length) {
                System.err.println("Local port, remote server host name and/or remote server port missing; try '-help'"); // SUPPRESS CHECKSTYLE LineLength
                System.exit(1);
            }

            InetSocketAddress endpoint      = new InetSocketAddress(bindAddress, Integer.parseInt(args[i++]));
            InetSocketAddress serverAddress = new InetSocketAddress(args[i++], Integer.parseInt(args[i++]));

            ThreadUtil.runInBackground(new FtpReverseProxy(
                endpoint,
                backlog,
                serverAddress,
                serverConnectionTimeout
            ), null);
        }

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ie) {
            ;
        }
    }
}
