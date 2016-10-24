
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

package de.unkrig.commons.net.tool.tcpmon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.ReverseProxy;
import de.unkrig.commons.net.ReverseProxy.ProxyConnectionHandler;
import de.unkrig.commons.util.logging.SimpleLogging;

/** The TCPMON command line utility. */
public final
class Main {

    private Main() {}

    static { SimpleLogging.init(); }

    public static void // SUPPRESS CHECKSTYLE JavadocMethod
    main(String[] args) throws IOException {
        int i = 0;

        // Process global options.
        for (; i < args.length;) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            i++;
            if ("-help".equals(arg)) {
                System.out.println("A server that accepts connections from clients, creates another connection to a");
                System.out.println("remote server for each accepted connection, and forwards all data from the");
                System.out.println("client to the server and back.");
                System.out.println();
                System.out.println("Usage:");
                System.out.println("  java " + Main.class.getName() + "");
                System.out.println("        [ <global-option> ... ]");
                System.out.println("        ( [ <local-option> ... ]");
                System.out.println("              <local-port> <remote-server-host-name> <remote-server-port> ) ...");
                System.out.println("Valid <global-option>s are:");
                System.out.println("  -help");
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
                System.exit(0);
            }
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
                i--;
                break;
            }
        }

        while (i < args.length) {

            // Process local options.
            int         backlog                 = 0;
            InetAddress bindAddress             = null;
            int         serverConnectionTimeout = 0;
            for (; i < args.length;) {
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

            // Process local port, server host name and server port.
            if (i + 3 > args.length) {
                System.err.println("Local port, server host name and/or server port missing; try '-help'.");
                System.exit(1);
            }

            InetSocketAddress endpoint      = new InetSocketAddress(bindAddress, Integer.parseInt(args[i++]));
            InetSocketAddress serverAddress = InetSocketAddress.createUnresolved(
                args[i++],
                Integer.parseInt(args[i++])
            );

            ThreadUtil.runInBackground(new ReverseProxy(
                endpoint,
                backlog,
                serverAddress,
                Proxy.NO_PROXY,
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
                        ThreadUtil.parallel(
                            IoUtil.copyRunnable(clientIn, serverOut),
                            IoUtil.copyRunnable(serverIn, clientOut),
                            stoppable
                        );
                    }
                }
            ), null);
        }

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ie) {
            ;
        }
    }
}
