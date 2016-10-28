
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

package de.unkrig.commons.net.tool.nullserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.net.TcpServer;
import de.unkrig.commons.util.logging.SimpleLogging;

/** The NULLSERVER commandline utility. */
public final
class Main {

    private Main() {}

    static { SimpleLogging.init(); }

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void // SUPPRESS CHECKSTYLE JavadocMethod
    main(String[] args) throws IOException {
        int i = 0;

        // Process command line options.
        while (i < args.length) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            i++;
            if ("-help".equals(arg)) {
                System.out.println("A server that accepts connections from clients and logs the data it receives");
                System.out.println("from these connections.");
                System.out.println();
                System.out.println("Usage:");
                System.out.println("  java [ <option> ... ] " + Main.class.getName() + " <port>");
                System.out.println("Valid <global-option>s are:");
                System.out.println("  -help");
                System.out.println("  -out <file>     Redirect the output to the given file");
                System.out.println("  -nowarn         Suppress all messages except errors");
                System.out.println("  -quiet          Suppress normal output");
                System.out.println("  -verbose        Log verbose messages");
                System.out.println("  -debug          Log verbose and debug messages (repeat to get more output)");
                System.out.println("  -log <level>:<logger>:<handler>:<formatter>:<format>");
                System.out.println("                  Add logging at level FINE on logger 'de.unkrig' to STDERR");
                System.out.println("                  using the FormatFormatter and SIMPLE format, or the given");
                System.out.println("                  arguments which are all optional.");
                System.exit(0);
            } else
            if ("-out".equals(arg)) {
                SimpleLogging.setOut(new File(args[i++]));
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
                System.err.println("Invalid command line option '" + arg + "'; try '-help'");
            }
        }

        if (i + 1 != args.length) {
            System.err.println("Local port missing; try '-help'");
            System.exit(1);
        }
        int port = Integer.parseInt(args[i++]);

        new TcpServer(new InetSocketAddress(port), 0, new TcpServer.ConnectionHandler() {

            @Override public void
            handleConnection(
                InputStream       in,
                OutputStream      out,
                InetSocketAddress localSocketAddress,
                InetSocketAddress remoteSocketAddress,
                Stoppable         stoppable
            ) throws IOException {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
                for (;;) {
                    String line = br.readLine();
                    if (line == null) break;
                    LOGGER.fine(line);
                }
            }
        }).run();
    }
}
