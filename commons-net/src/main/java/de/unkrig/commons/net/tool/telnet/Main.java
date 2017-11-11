
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

package de.unkrig.commons.net.tool.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.util.logging.SimpleLogging;

/** The TELNET command line utility. */
public final
class Main {

    private Main() {}

    static { SimpleLogging.init(); }

    public static void // SUPPRESS CHECKSTYLE JavadocMethod
    main(String[] args) throws IOException {
        int i = 0;

        // Process command line options.
        while (i < args.length) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            i++;
            if ("-help".equals(arg)) {

                System.out.println("Creates a TCP connection to a server and forwards STDIN/STDOUT to that connection."); // SUPPRESS CHECKSTYLE LineLength
                System.out.println();
                System.out.println("Usage:");
                System.out.println("  java " + Main.class.getName() + " -help");
                System.out.println("  java " + Main.class.getName() + " <server-host-name> <server-port> )");

                System.exit(0);
            } else
            {
                System.err.println("Unknown command line option '" + arg + "'; try '-help'.");
                System.exit(1);
            }
        }

        // Get host name and port.
        if (i + 2 != args.length) {
            System.err.println("Server host name and/or server port missing; try '-help'");
            System.exit(1);
        }
        String hostName = args[i++];
        int    port     = Integer.parseInt(args[i++]);

        // Connect with the server.
        final Socket       socket;
        final OutputStream socketOutputStream;
        final InputStream  socketInputStream;
        {
            try {
                socket = new Socket(hostName, port);
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap("Connecting to '" + hostName + "':" + port, ioe);
            }
            socketOutputStream = socket.getOutputStream();
            socketInputStream  = socket.getInputStream();
        }

        // Forward data from the socket to the console and vice versa.
        ThreadUtil.runInBackground(IoUtil.copyRunnable(socketInputStream, System.out), null);
        ThreadUtil.runInForeground(IoUtil.copyRunnable(System.in, socketOutputStream));

        socket.close();
    }
}
