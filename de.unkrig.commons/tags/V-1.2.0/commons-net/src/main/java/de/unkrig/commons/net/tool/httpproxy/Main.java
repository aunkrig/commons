
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

package de.unkrig.commons.net.tool.httpproxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.net.http.HttpMessage;
import de.unkrig.commons.net.http.HttpMessage.Body;
import de.unkrig.commons.net.http.HttpProxy;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.util.logging.SimpleLogging;

/** The HTTPPROXY command line utility. */
public final
class Main {

    private Main() {}

    static { SimpleLogging.init(); }

    interface Modifier {

        /** Modifies the given HTTP message. */
        void
        modify(HttpMessage message) throws IOException;
    }

    static
    class Substitution {

        private final Pattern pattern;
        private final String  replacement;
        private final boolean global;

        Substitution(Pattern pattern, String replacement, boolean global) {
            this.pattern     = pattern;
            this.replacement = replacement;
            this.global      = global;
        }

        Substitution(String sedCommand) {
            Matcher m = Pattern.compile("s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/([gi]*)").matcher(sedCommand);
            if (!m.matches()) throw new RuntimeException("Invalid SED command '" + sedCommand + "'");
            String flags = m.group(3);
            this.pattern     = Pattern.compile(m.group(1), flags.indexOf('i') != -1 ? Pattern.CASE_INSENSITIVE : 0);
            this.replacement = m.group(2);
            this.global      = flags.indexOf('g') != -1;
        }

        /**
         * Replaces all matches of the pattern within the {@code subject} with the replacement
         *
         * @return The modified {@code subject}
         */
        public String
        apply(String subject) {
            Matcher m = this.pattern.matcher(subject);
            return this.global ? m.replaceAll(this.replacement) : m.replaceFirst(this.replacement);
        }
    }

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
                    System.out.println("        ( [ <local-option> ... ] <local-port> <remote-server-host-name> <remote-server-port> ) ...");
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
                    System.out.println("  -bind-address <address>");
                    System.out.println("                  Accept connect requests to only this address");
                    System.out.println("  <message-modification-option>");
                    System.out.println("                  is applied to all requests and responses");
                    System.out.println("Valid <message-modification-option>s are:");
                    System.out.println("  -request        Apply the following options only to REQUESTS");
                    System.out.println("  -response       Apply the following options only to RESPONSES");
                    System.out.println("  -e s/<regex>/<replacement>/<flags>");
                    System.out.println("                  Replace the first occurrence of <regex> within the body");
                    System.out.println("                  with <replacement>.");
                    System.out.println("                  Valid flags are:");
                    System.out.println("                    'i': Case-insensitive match");
                    System.out.println("                    'g': Replace ALL occurrences");
                    System.out.println("  -add-header <name> <value>");
                    System.out.println("                  Add a header with the given <name> and <value>");
                    System.out.println("  -set-header <name> <value>");
                    System.out.println("                  Change the value of the first header with the given <name>,");
                    System.out.println("                  and remove all other headers withe the given <name>");
                    System.out.println("  -remove-header <name>");
                    System.out.println("                  Remove all headers with the given <name>");
                    System.out.println("  -modify-header <name> s/<regex>/<replacement>/<flags>");
                    System.out.println("                  Modify the value of all headers with the given <name>");
                    // CHECKSTYLE LineLength:ON
                    System.exit(0);
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
                    i--;
                    break;
                }
            }
        }

        while (i < args.length) {

            // Parse and process local options.
            InetAddress          bindAddress           = null;
            boolean              applicableToRequests  = true, applicableToResponses = true;
            final List<Modifier> requestBodyModifiers  = new ArrayList<Modifier>();
            final List<Modifier> responseBodyModifiers = new ArrayList<Modifier>();

            while (i < args.length) {
                String arg = args[i];
                if (!arg.startsWith("-")) break;
                i++;
                if ("-bind-address".equals(arg)) {
                    bindAddress = InetAddress.getByName(args[i++]);
                } else
                if ("-request".equals(arg)) {
                    applicableToRequests  = true;
                    applicableToResponses = false;
                } else
                if ("-response".equals(arg)) {
                    applicableToRequests  = false;
                    applicableToResponses = true;
                } else
                if ("-e".equals(arg)) {
                    final Substitution substitution = new Substitution(args[i++]);

                    Modifier modifier = new Modifier() {

                        @Override public void
                        modify(HttpMessage message) throws IOException {
                            Charset charSet = message.getCharset();

                            Body body = message.removeBody();
                            if (body != HttpMessage.NO_BODY) {
                                message.setBody(HttpMessage.body(
                                    substitution.apply(body.string(charSet)),
                                    charSet
                                ));
                            }
                        }
                    };

                    if (applicableToRequests) requestBodyModifiers.add(modifier);
                    if (applicableToResponses) responseBodyModifiers.add(modifier);
                } else
                if ("-add-header".equals(arg)) {
                    final String name  = args[i++];
                    final String value = args[i++];

                    Modifier modifier = new Modifier() {

                        @Override public void
                        modify(HttpMessage message) { message.addHeader(name, value); }
                    };

                    if (applicableToRequests) requestBodyModifiers.add(modifier);
                    if (applicableToResponses) responseBodyModifiers.add(modifier);
                } else
                if ("-set-header".equals(arg)) {
                    final String name  = args[i++];
                    final String value = args[i++];

                    Modifier modifier = new Modifier() {

                        @Override public void
                        modify(HttpMessage message) { message.setHeader(name, value); }
                    };

                    if (applicableToRequests) requestBodyModifiers.add(modifier);
                    if (applicableToResponses) responseBodyModifiers.add(modifier);
                } else
                if ("-remove-header".equals(arg)) {
                    final String name = args[i++];

                    Modifier modifier = new Modifier() {

                        @Override public void
                        modify(HttpMessage message) { message.removeHeader(name); }
                    };

                    if (applicableToRequests) requestBodyModifiers.add(modifier);
                    if (applicableToResponses) responseBodyModifiers.add(modifier);
                } else
                if ("-modify-header".equals(arg)) {
                    final String       name         = args[i++];
                    final Substitution substitution = new Substitution(args[i++]);

                    Modifier modifier = new Modifier() {

                        @Override public void
                        modify(HttpMessage message) {
                            String value = message.getHeader(name);
                            if (value != null) {
                                message.setHeader(name, substitution.apply(value));
                            }
                        }
                    };

                    if (applicableToRequests) requestBodyModifiers.add(modifier);
                    if (applicableToResponses) responseBodyModifiers.add(modifier);
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

            HttpProxy httpProxy = new HttpProxy(
                endpoint,
                serverAddress,
                new TransformerWhichThrows<HttpRequest, HttpRequest, IOException>() {

                    @Override public HttpRequest
                    transform(HttpRequest in) throws IOException {
                        applyBodySubstitutions(in, requestBodyModifiers);
                        return in;
                    }
                },
                new TransformerWhichThrows<HttpResponse, HttpResponse, IOException>() {

                    @Override public HttpResponse
                    transform(HttpResponse in) throws IOException {
                        applyBodySubstitutions(in, responseBodyModifiers);
                        return in;
                    }
                }
            );
            ThreadUtil.runInBackground(httpProxy, null);
        }

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            ;
        }
    }

    private static void
    applyBodySubstitutions(HttpMessage message, List<Modifier> modifiers) throws IOException {

        for (Modifier modifier : modifiers) {
            modifier.modify(message);
        }
    }
}
