
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

package de.unkrig.commons.net.http;

import static java.util.logging.Level.FINE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.unkrig.commons.io.FixedLengthInputStream;
import de.unkrig.commons.io.FixedLengthOutputStream;
import de.unkrig.commons.io.HexOutputStream;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.Multiplexer;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.Readers;
import de.unkrig.commons.io.WriterOutputStream;
import de.unkrig.commons.io.XMLFormatterWriter;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil.Produmer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.net.http.io.ChunkedInputStream;
import de.unkrig.commons.net.http.io.ChunkedOutputStream;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * Representation of an HTTP request or response.
 */
public
class HttpMessage {

    private static final Logger LOGGER = Logger.getLogger(HttpMessage.class.getName());

    public
    enum HasBody { FALSE, TRUE, IF_CONTENT_LENGTH_OR_TRANSFER_ENCODING }

    private static final Pattern
    HEADER_PATTERN = Pattern.compile("([ -~&&[^()<>@,;:\\\\/\\[\\]?={} \\t]]+)\\s*:\\s*(.*?)\\s*");

    private static final DateFormat[] HEADER_DATE_FORMATS = {

        // RFC 822, updated by RFC 1123 (preferred format):
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH),

        // RFC 850, obsoleted by RFC 1036:
        new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss 'GMT'", Locale.ENGLISH),

        // ANSI C's asctime() format:
        new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH),
    };
    static { for (DateFormat df : HEADER_DATE_FORMATS) df.setTimeZone(TimeZone.getTimeZone("UTC")); }

    private final List<MessageHeader> headers = new ArrayList<MessageHeader>();

    /**
     * The life cycle of a {@link Body} is as follows:
     * <ol>
     *   <li>
     *     Create the object by calling one of the following methods:
     *     <ul>
     *       <li> {@link HttpMessage#body(String, Charset)}
     *       <li> {@link HttpMessage#body(InputStream)}
     *       <li> {@link HttpMessage#body(File)}
     *       <li> {@link HttpMessage#body(ConsumerWhichThrows)}
     *     </ul>
     *   </li>
     *   <li>
     *     Call exactly one of the following methods:
     *     <ul>
     *       <li>{@link #string(Charset)}
     *       <li>{@link #inputStream()}
     *       <li>{@link #write(OutputStream)}
     *       <li>{@link #dispose()}
     *     </ul>
     *   </li>
     *   <li>Call {@link #dispose()} as many times as you want
     * </ol>
     * Otherwise, a resource leak will occur.
     */
    public
    interface Body {

        /**
         * @return   The contents of the body of an {@link HttpMessage} as a string
         * @see Body
         */
        String
        string(Charset charset) throws IOException;

        /**
         * The caller is responsible for closing the returned {@link InputStream}.
         *
         * @return   Produces the contents of the body of an {@link HttpMessage} as a stream of bytes
         * @see Body
         */
        InputStream
        inputStream() throws IOException;

        /**
         * Writes the contents of the body to the given <var>stream</var>.
         *
         * @see Body
         */
        void
        write(OutputStream stream) throws IOException;

        /**
         * Releases any resources associated with this object.
         *
         * @see Body
         */
        void
        dispose();
    }

    /**
     * Representation of a non-existent HTTP request/response body.
     */
    public static final Body NO_BODY = new Body() {

        @Override public String
        string(Charset charset) { throw new UnsupportedOperationException("NO_BODY"); }

        @Override public InputStream
        inputStream() { throw new UnsupportedOperationException("NO_BODY"); }

        @Override public void
        write(OutputStream stream) { throw new UnsupportedOperationException("NO_BODY"); }

        @Override public void
        dispose() {}

        @Override public String
        toString() { return "(none)"; }
    };

    /**
     * Representation of an empty HTTP request/response body.
     */
    public static final Body EMPTY_BODY = new Body() {

        @Override public String
        string(Charset charset) { return ""; }

        @Override public InputStream
        inputStream() { return InputStreams.EMPTY; }

        @Override public void
        write(OutputStream stream) {}

        @Override public void
        dispose() {}
    };

    /**
     * {@code null} iff this message does not have a body.
     */
    private Body    body = NO_BODY;
    private boolean attemptUnstreaming;

    /**
     * Constructor for outgoing messages.
     */
    protected
    HttpMessage(boolean hasBody) { this.body = hasBody ? EMPTY_BODY : NO_BODY; }

    public void
    setAttemptUnstreaming(boolean attemptUnstreaming) { this.attemptUnstreaming = attemptUnstreaming; }

    /**
     * Constructor for incoming messages.
     * <p>
     *   Notice that <var>in</var> will be read and closed when the body of this message is processed or disposed
     *   (see {@link Body}).
     * </p>
     */
    protected
    HttpMessage(InputStream in, boolean hasHeaders, HasBody hasBody) throws IOException {
        this(in, hasHeaders, hasBody, ">>> ");
    }

    /**
     * Constructor for incoming messages.
     * <p>
     *   Notice that <var>in</var> will be read and closed when the body of this message is processed or disposed
     *   (see {@link Body}).
     * </p>
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    protected
    HttpMessage(InputStream in, boolean hasHeaders, HasBody hasBody, final String loggingPrefix) throws IOException {

        // Read the headers.
        if (hasHeaders) {
            String line = HttpMessage.readLine(in);
            while (line.length() > 0) {
                String headerLine = line;
                for (;;) {
                    line = HttpMessage.readLine(in);
                    if (line.length() == 0 || " \t".indexOf(line.charAt(0)) == -1) break;
                    headerLine += "\r\n" + line;
                }
                LOGGER.fine(loggingPrefix + headerLine);
                Matcher matcher = HEADER_PATTERN.matcher(headerLine);
                if (!matcher.matches()) throw new IOException("Invalid HTTP header line '" + headerLine + "'");
                this.headers.add(new MessageHeader(matcher.group(1), matcher.group(2)));
            }
        }

        // Read the body.
        if (
            hasBody == HasBody.TRUE
            || (
                hasBody == HasBody.IF_CONTENT_LENGTH_OR_TRANSFER_ENCODING
                && (this.getHeader("Content-Length") != null || this.getHeader("Transfer-Encoding") != null)
            )
        ) {

            final Produmer<Long, Long> rawByteCount     = ConsumerUtil.store(0L);
            final Produmer<Long, Long> decodedByteCount = ConsumerUtil.store(0L);

            // Determine the raw message body size.
            if (LOGGER.isLoggable(FINE)) {
                in = InputStreams.wye(in, OutputStreams.lengthWritten(ConsumerUtil.cumulate(rawByteCount, 0)));
            }

            // Insert a logging Wye-Reader if logging is enabled.
            if (LOGGER.isLoggable(FINE)) {

                LOGGER.fine(loggingPrefix + "Reading message body");
                Writer lw = LogUtil.logWriter(LOGGER, FINE, loggingPrefix);
                in = InputStreams.wye(in, (
                    this.contentTypeIsXmlish()
                    ? new WriterOutputStream(new XMLFormatterWriter(lw))
                    : new HexOutputStream(lw)
                ));
            }

            // Process "Content-Length" and "Transfer-Encoding" headers.
            {
                long cl = this.getLongHeader("Content-Length");
                if (cl != -1) {
                    in = new FixedLengthInputStream(in, cl);
                } else {
                    String tes = this.getHeader("Transfer-Encoding");
                    if (tes != null) {
                        if (!"chunked".equalsIgnoreCase(tes)) {
                            throw new IOException("Message with unsupported transfer encoding '" + tes + "' received");
                        }
                        LOGGER.fine(loggingPrefix + "Reading message with chunked contents");
                        in = new ChunkedInputStream(in);
                    } else
                    {
                        LOGGER.fine(loggingPrefix + "Reading message with streaming contents");
                        ;
                    }
                }
            }

            // Process "Content-Encoding" header.
            if ("gzip".equalsIgnoreCase(this.getHeader("Content-Encoding"))) {
                in = new GZIPInputStream(in);
            }

            // Track the decoded message body size.
            if (LOGGER.isLoggable(FINE)) {
                in = InputStreams.wye(in, OutputStreams.lengthWritten(ConsumerUtil.cumulate(decodedByteCount, 0)));
            }

            // Report on the raw and on the decoded message body size.
            if (LOGGER.isLoggable(FINE)) {
                in = InputStreams.onEndOfInput(in, new Runnable() {

                    @Override public void
                    run() {
                        LOGGER.fine(
                            loggingPrefix
                            + "Message body size was "
                            + NumberFormat.getInstance(Locale.US).format(rawByteCount.produce())
                            + " (raw) "
                            + NumberFormat.getInstance(Locale.US).format(decodedByteCount.produce())
                            + " (decoded)"
                        );
                    }
                });
            }

            this.setBody(HttpMessage.body(in));
        }
    }

    private boolean
    contentTypeIsXmlish() {

        String contentType = this.getHeader("Content-Type");
        if (contentType == null) return false;

        ParametrizedHeaderValue phv = new ParametrizedHeaderValue(contentType);

        // There are MANY xml-like content types, e.g. "text/xml", "application/atomsvc+xml".
        return phv.getToken().toLowerCase().contains("xml");
    }

    /**
     * Appends another HTTP header.
     */
    public void
    addHeader(String name, String value) {
        this.headers.add(new MessageHeader(name, value));
    }

    /**
     * Appends another HTTP header.
     */
    public void
    addHeader(String name, int value) {
        this.addHeader(name, Integer.toString(value));
    }

    /**
     * Appends another HTTP header.
     */
    public void
    addHeader(String name, long value) {
        this.addHeader(name, Long.toString(value));
    }

    /**
     * Appends another HTTP header.
     */
    public void
    addHeader(String name, Date value) {
        this.addHeader(name, HEADER_DATE_FORMATS[0].format(value));
    }

    /**
     * Changes the value of the first header with the given <var>name</var>, or adds a new header.
     */
    public void
    setHeader(String name, String value) {
        for (MessageHeader header : this.headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                header.setValue(value);
                return;
            }
        }
        this.headers.add(new MessageHeader(name, value));
    }

    /**
     * Changes the value of the first header with the given <var>name</var>.
     */
    public void
    setHeader(String name, int value) {
        this.setHeader(name, Integer.toString(value));
    }

    /**
     * Changes the value of the first header with the given <var>name</var>.
     */
    public void
    setHeader(String name, long value) {
        this.setHeader(name, Long.toString(value));
    }

    /**
     * Changes the value of the first header with the given <var>name</var>.
     */
    public void
    setHeader(String name, Date value) {
        this.setHeader(name, HEADER_DATE_FORMATS[0].format(value));
    }

    /**
     * Remove all headers with the given <var>name</var>.
     */
    public void
    removeHeader(String name) {
        for (Iterator<MessageHeader> it = this.headers.iterator(); it.hasNext();) {
            MessageHeader h = it.next();
            if (h.getName().equalsIgnoreCase(name)) it.remove();
        }
    }

    /**
     * @return the value of the first message header with that <var>name</var>, or {@code null}
     */
    @Nullable public final String
    getHeader(String name) {
        for (MessageHeader mh : this.headers) {
            if (mh.getName().equalsIgnoreCase(name)) return mh.getValue();
        }
        return null;
    }

    /**
     * @return {@code -1} iff a header with this name does not exist
     */
    public int
    getIntHeader(String name) throws IOException {
        String s = this.getHeader(name);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                // SUPPRESS CHECKSTYLE AvoidHidingCause
                throw new IOException("'" + name  + "' message header has invalid value '" + s + "'");
            }
        }
        return -1;
    }

    /**
     * @return {@code -1L} iff a header with this name does not exist
     */
    public final long
    getLongHeader(String name) throws IOException {
        String s = this.getHeader(name);
        if (s != null) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException nfe) {
                // SUPPRESS CHECKSTYLE HidingCause
                throw new IOException("'" + name  + "' message header has invalid value '" + s + "'");
            }
        }
        return -1L;
    }

    /**
     * @return {@code null} iff a header with this name does not exist
     */
    @Nullable public Date
    getDateHeader(String name) throws IOException {
        String s = this.getHeader(name);
        if (s == null) return null;

        for (DateFormat df : HEADER_DATE_FORMATS) {
            try { return df.parse(s); } catch (ParseException pe) {}
        }
        throw new IOException("Cannot parse date header '" + name + ": " + s + "'");
    }

    /**
     * @return the values of the message headers with that <var>name</var>, or an empty array
     */
    public String[]
    getHeaders(String name) {
        List<String> values = new ArrayList<String>();
        for (MessageHeader mh : this.headers) {
            if (mh.getName().equalsIgnoreCase(name)) values.add(mh.getValue());
        }
        return values.toArray(new String[values.size()]);
    }

    /**
     * The returned list is backed by the {@link HttpMessage}!
     */
    public List<MessageHeader>
    getHeaders() {
        return this.headers;
    }

    /**
     * Removes the body from this {@link HttpMessage} for analysis or modification. It can later be re-attached to
     * the same (or a different) {@link HttpMessage} through {@link #setBody(Body)}.
     */
    public Body
    removeBody() {
        Body result = this.body;
        this.body = NO_BODY;
        return result;
    }

    /**
     * @see Body
     */
    public static Body
    body(final String text, final Charset charset) {

        return new Body() {

            @Nullable String text2 = text;

            @Override public String
            string(Charset charset) {
                String result = this.text2;
                if (result == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                this.text2 = null;
                return result;
            }

            @Override public InputStream
            inputStream() throws IOException {
                if (this.text2 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new OutputStreamWriter(baos, charset).write(this.text2);
                this.text2 = null;
                return new ByteArrayInputStream(baos.toByteArray());
            }

            @Override public void
            write(OutputStream stream) throws IOException {
                if (this.text2 == null) {
                    throw new IllegalStateException("Body has been read before");
                }

                {
                    OutputStreamWriter w = new OutputStreamWriter(stream, charset);
                    w.write(this.text2);
                    w.flush();
                }

                this.text2 = null;
            }

            @Override public void
            dispose() {
                this.text2 = null;
            }
        };
    }

    /**
     * The {@link InputStream} will be closed by {@link Body#string(Charset)}, {@link Body#inputStream()}, {@link
     * Body#write(OutputStream)}, {@link Body#dispose()} and {@link Body#dispose()}.
     *
     * @see Body
     */
    public static Body
    body(final InputStream in) {

        return new Body() {

            /**
             * {@code Null} means that that one of {@link Body#string(Charset)}, {@link Body#inputStream()}, {@link
             * Body#write(OutputStream)}, {@link Body#dispose()} or {@link Body#dispose()} has been called before.
             */
            @Nullable InputStream in2 = in;

            @Override public String
            string(Charset charset) throws IOException {
                InputStream in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                final String result = Readers.readAll(new InputStreamReader(in3, charset));
                try { in3.close(); } catch (Exception e) {}
                this.in2 = null;
                return result;
            }

            @Override public InputStream
            inputStream() {
                InputStream in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                this.in2 = null;
                return in3;
            }

            @Override public void
            write(OutputStream stream) throws IOException {
                InputStream in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                IoUtil.copy(in3, stream);
                try { in3.close(); } catch (Exception e) {}
                this.in2 = null;
            }

            @Override public void
            dispose() {
                InputStream in3 = this.in2;
                if (in3 != null) {
                    try { InputStreams.skipAll(in3); } catch (Exception e) {}
                    try { in3.close();               } catch (Exception e) {}
                }
                this.in2 = null;
            }
        };
    }

    /**
     * For the returned objects, {@link Body#string(Charset)}, {@link Body#inputStream()} and {@link
     * Body#write(OutputStream)} will invoke <var>in</var> to produce an {@link InputStream} which will be read and
     * eventually closed.
     *
     * @see Body
     */
    public static Body
    body(final ProducerWhichThrows<InputStream, IOException> in) {

        return new Body() {

            /**
             * {@code Null} means that that one of {@link Body#string(Charset)}, {@link Body#inputStream()}, {@link
             * Body#write(OutputStream)}, {@link Body#dispose()} or {@link Body#dispose()} has been called before.
             */
            @Nullable ProducerWhichThrows<InputStream, IOException> in2 = in;
            @Nullable InputStream                                   is;

            @Override public String
            string(Charset charset) throws IOException {

                ProducerWhichThrows<InputStream, IOException> in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }

                InputStream is = (this.is = in3.produce());
                assert is != null;

                final String result = Readers.readAll(new InputStreamReader(is, charset), true);

                this.in2 = null;
                return result;
            }

            @Override public InputStream
            inputStream() throws IOException {

                ProducerWhichThrows<InputStream, IOException> in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }

                InputStream is = (this.is = in3.produce());
                assert is != null;

                this.in2 = null;
                return is;
            }

            @Override public void
            write(OutputStream stream) throws IOException {

                ProducerWhichThrows<InputStream, IOException> in3 = this.in2;
                if (in3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }

                InputStream is = (this.is = in3.produce());
                assert is != null;

                IoUtil.copy(is, true, stream, false);

                this.in2 = null;
            }

            @Override public void
            dispose() {
                InputStream is = this.is;
                if (is != null) {
                    try { InputStreams.skipAll(is); } catch (Exception e) {}
                    try { is.close();               } catch (Exception e) {}
                }
                this.is = null;
            }
        };
    }

    /**
     * @see Body
     */
    public static Body
    body(final File file) throws FileNotFoundException {
        return HttpMessage.body(new FileInputStream(file));
    }

    /**
     * @param writer Consumes exactly one {@link OutputStream}, and writes the body data to it
     * @see Body
     */
    public static Body
    body(final ConsumerWhichThrows<OutputStream, IOException> writer) {
        return new Body() {

            @Nullable ConsumerWhichThrows<OutputStream, IOException> writer2 = writer;

            @Override public String
            string(Charset charset) throws IOException {
                ConsumerWhichThrows<OutputStream, IOException> w3 = this.writer2;
                if (w3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                w3.consume(baos);
                this.writer2 = null;
                return Readers.readAll(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), charset));
            }

            @Override public InputStream
            inputStream() throws IOException {
                ConsumerWhichThrows<OutputStream, IOException> w3 = this.writer2;
                if (w3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                w3.consume(baos);
                this.writer2 = null;
                return new ByteArrayInputStream(baos.toByteArray());
            }

            @Override public void
            write(OutputStream stream) throws IOException {
                ConsumerWhichThrows<OutputStream, IOException> w3 = this.writer2;
                if (w3 == null) {
                    throw new IllegalStateException("Body has been read before");
                }
                w3.consume(stream);
                this.writer2 = null;
            }

            @Override public void
            dispose() {
                this.writer2 = null;
            }
        };
    }

    /**
     * Disposes the current body of this message and adopts the given {@link Body} object as the new body.
     */
    public void
    setBody(Body body) {
        this.body.dispose();
        this.body = body;
    }

    /**
     * Determines the charset from the "Content-Type" header.
     */
    public Charset
    getCharset() {
        String ct = this.getHeader("Content-Type");
        if (ct != null) {
            ParametrizedHeaderValue phv   = new ParametrizedHeaderValue(ct);
            String                  token = phv.getToken();
            if (token.startsWith("text/") || "application/x-www-form-urlencoded".equalsIgnoreCase(token)) {
                String charsetName = phv.getParameter("charset");
                if (charsetName != null) {
                    try {
                        return Charset.forName(charsetName);
                    } catch (IllegalCharsetNameException ncsne) {
                        ;
                    }
                }
            }
        }
        return DEFAULT_CHARSET;
    }
    private static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Writes this message's headers and body to the given {@link OutputStream}. Also closes the {@link OutputStream}
     * iff there is neither a "Content-Length:" header, nor a "Transfer-Encoding: chunked" header, nor the message
     * is very small.
     */
    protected void
    writeHeadersAndBody(final OutputStream out) throws IOException { this.writeHeadersAndBody("<<< ", out); }

    /**
     * Writes this message's headers and body to the given {@link OutputStream}. Also closes the {@link OutputStream}
     * iff there is neither a "Content-Length:" header, nor a "Transfer-Encoding: chunked" header, nor the message
     * is very small.
     *
     * @param loggingPrefix E.g. {@code "<<< "}
     */
    protected void
    writeHeadersAndBody(final String loggingPrefix, final OutputStream out) throws IOException {

        if (this.body == NO_BODY) {
            this.writeHeaders(loggingPrefix, out);
            return;
        }

        // Check for "chunked" transfer encoding.
        {
            String tes = this.getHeader("Transfer-Encoding");
            if (tes != null) {
                if (!"chunked".equalsIgnoreCase(tes)) {
                    throw new IOException("Message with unsupported transfer encoding '" + tes + "' received");
                }
                LOGGER.fine(loggingPrefix + "Writing message with chunked contents");

                // Chunked transfer encoding.
                this.writeHeaders(loggingPrefix, out);
                OutputStream cos = new ChunkedOutputStream(OutputStreams.unclosable(out));
                this.writeBody(loggingPrefix, cos);
                cos.close();
                return;
            }
        }

        // Check for a "Content-Length" header.
        {
            long contentLength = this.getLongHeader("Content-Length");
            if (contentLength >= 0L) {

                // Content length known.
                this.writeHeaders(loggingPrefix, out);
                FixedLengthOutputStream flos = new FixedLengthOutputStream(
                    OutputStreams.unclosable(out),
                    contentLength
                );
                this.writeBody(loggingPrefix, flos);
                flos.close();
                return;
            }
        }

        if (!this.attemptUnstreaming) {
            this.writeHeaders(loggingPrefix, out);
            this.writeBody(loggingPrefix, out);
            return;
        }

        // The message has neither a header "Transfer-Encoding: chunked" nor a "Content-Length" header, so the length
        // of the message is determined by closing the connection. Since this is terribly inefficient, an attempt is
        // made to measure the length of the body if it is small.
        final byte[] buffer = new byte[4000];
        final int[]  count  = new int[1];
        this.writeBody(loggingPrefix, new OutputStream() {

            @Override public void
            write(int b) throws IOException {
                this.write(new byte[] { (byte) b }, 0, 1);
            }

            @NotNullByDefault(false) @Override public void
            write(byte[] b, int off, int len) throws IOException {
                if (count[0] == -1) {
                    out.write(b, off, len);
                } else
                if (count[0] + len > buffer.length) {
                    HttpMessage.this.writeHeaders(loggingPrefix, out);
                    out.write(buffer, 0, count[0]);
                    count[0] = -1;
                    out.write(b, off, len);
                } else
                {
                    System.arraycopy(b, off, buffer, count[0], len);
                    count[0] += len;
                }
            }
        });
        if (count[0] == -1L) {

            // Unstreaming failed; nothing left to be done.
            out.close();
            return;
        }

        this.setHeader("Content-Length", count[0]);
        this.writeHeaders(loggingPrefix, out);
        out.write(buffer, 0, count[0]);
    }

    /**
     * Writes the body of this message <em>synchronously</em> to the given {@link OutputStream}.
     *
     * @param loggingPrefix E.g. {@code "<<< "}
     */
    private void
    writeBody(String loggingPrefix, OutputStream out) throws IOException {

        // Check "Content-Encoding: gzip"
        GZIPOutputStream finishable = null;
        if ("gzip".equalsIgnoreCase(this.getHeader("Content-Encoding"))) {
            LOGGER.fine(loggingPrefix + "GZIP-encoded contents");
            out = (finishable = new GZIPOutputStream(out));
        }

        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine(loggingPrefix + "Writing message body:");
            Writer lw = LogUtil.logWriter(LOGGER, FINE, loggingPrefix);
            out = OutputStreams.tee(out, (
                this.contentTypeIsXmlish()
                ? new WriterOutputStream(new XMLFormatterWriter(lw))
                : new HexOutputStream(lw)
            ));
        }

        this.body.write(out);

        if (finishable != null) finishable.finish();
        out.flush();
    }

    /**
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    private void
    writeHeaders(String loggingPrefix, OutputStream out) throws IOException {

        Writer w = new OutputStreamWriter(out, Charset.forName("ASCII"));

        // Headers and blank line.
        for (MessageHeader header : this.getHeaders()) {
            LOGGER.fine(loggingPrefix + header.getName() + ": " + header.getValue());
            w.write(header.getName() + ": " + header.getValue() + "\r\n");
        }

        // Headers and blank line.
        w.write("\r\n");

        w.flush();
    }

    /**
     * @return the line read, excluding the trailing CRLF
     *
     * @throws EOFException
     */
    public static String
    readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int c = in.read();
            if (c == -1) throw new EOFException();
            if (c == '\r') {
                c = in.read();
                if (c != '\n') throw new IOException("LF instead of " + c + " expected after CR");
                in.available();
                return new String(baos.toByteArray(), "ISO-8859-1");
            }
            baos.write(c);
        }
    }

    /**
     * Reads one HTTP request from <var>in</var> through the <var>multiplexer</var> and passes it to the
     * <var>requestConsumer</var>.
     */
    public static void
    readLine(
        final ReadableByteChannel                      in,
        final Multiplexer                              multiplexer,
        final ConsumerWhichThrows<String, IOException> lineConsumer
    ) throws IOException {

        RunnableWhichThrows<IOException> lineParser = new RunnableWhichThrows<IOException>() {

            final ByteBuffer            buffer = ByteBuffer.allocate(1);
            final ByteArrayOutputStream line = new ByteArrayOutputStream();
            int                         state;

            @Override public void
            run() throws IOException {
                this.buffer.rewind();
                in.read(this.buffer);

                byte b = this.buffer.get(0);
                switch (this.state) {

                case 0: // Start.
                    if (b == '\r') {
                        this.state = 1;
                        break;
                    }
                    this.line.write(b);
                    break;

                case 1: // After CR
                    if (b != '\n') {
                        throw new InvalidHttpMessageException(
                            "HTTP header line: CR is not followed by LF, but '" + (0xff & b) + "'"
                        );
                    }
                    lineConsumer.consume(new String(this.line.toByteArray(), "ISO-8859-1"));
                    return;
                }
                multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, this);
            }
        };

        multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, lineParser);
    }

    /**
     * Reads HTTP headers up to and including the terminating empty line.
     */
    public static void
    readHeaders(
        final ReadableByteChannel                                   in,
        final Multiplexer                                           multiplexer,
        final ConsumerWhichThrows<List<MessageHeader>, IOException> consumer
    ) throws IOException { HttpMessage.readHeaders(in, multiplexer, consumer, ">>> "); }

    /**
     * Reads HTTP headers up to and including the terminating empty line.
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    public static void
    readHeaders(
        final ReadableByteChannel                                   in,
        final Multiplexer                                           multiplexer,
        final ConsumerWhichThrows<List<MessageHeader>, IOException> consumer,
        final String                                                loggingPrefix
    ) throws IOException {
        HttpMessage.readLine(in, multiplexer, new ConsumerWhichThrows<String, IOException>() {

            @Nullable String          headerLine;
            final List<MessageHeader> headers = new ArrayList<MessageHeader>();

            @Override public void
            consume(String line) throws IOException {
                if (" \t".indexOf(line.charAt(0)) != -1) {
                    if (this.headerLine == null) {
                        throw new InvalidHttpMessageException("Unexpected leading continuation line '" + line + "'");
                    }
                    this.headerLine += "\r\n" + line;
                    HttpMessage.readLine(in, multiplexer, this);
                    return;
                }

                LOGGER.fine(loggingPrefix + this.headerLine);
                Matcher matcher = HEADER_PATTERN.matcher(this.headerLine);
                if (!matcher.matches()) {
                    throw new InvalidHttpMessageException("Invalid HTTP header line '" + this.headerLine + "'");
                }
                this.headers.add(new MessageHeader(matcher.group(1), matcher.group(2)));

                if (line.length() == 0) {
                    consumer.consume(this.headers);
                    return;
                }

                this.headerLine = line;
                HttpMessage.readLine(in, multiplexer, this);
            }
        });
    }

    /**
     * Reads the body contents of this message into a buffer (depending on the 'Content-Length' and 'Transfer-Encoding'
     * headers).
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    protected void
    readBody(
        ReadableByteChannel                    in,
        Multiplexer                            multiplexer,
        final RunnableWhichThrows<IOException> finished,
        final String                           loggingPrefix
    ) throws IOException {

        final ByteArrayOutputStream            buffer   = new ByteArrayOutputStream();
        final RunnableWhichThrows<IOException> runnable = new RunnableWhichThrows<IOException>() {

            @Override public void
            run() throws IOException {
                InputStream in = new ByteArrayInputStream(buffer.toByteArray());

                // Process "Content-Encoding" header.
                if ("gzip".equalsIgnoreCase(HttpMessage.this.getHeader("Content-Encoding"))) {
                    LOGGER.fine(loggingPrefix + "GZIP-encoded content");
                    in = new GZIPInputStream(in);
                }

                if (LOGGER.isLoggable(FINE)) {

                    LOGGER.fine(loggingPrefix + "Reading message body");
                    Writer lw = LogUtil.logWriter(LOGGER, FINE, loggingPrefix);
                    in = InputStreams.wye(in, (
                        HttpMessage.this.contentTypeIsXmlish()
                        ? new WriterOutputStream(new XMLFormatterWriter(lw))
                        : new HexOutputStream(lw)
                    ));
                }
                HttpMessage.this.setBody(HttpMessage.body(in));
                finished.run();
            }
        };

        // Read the body contents.
        {
            long cl = this.getLongHeader("Content-Length");
            if (cl != -1) {
                HttpMessage.read(in, multiplexer, cl, buffer, runnable);
            } else {
                String tes = this.getHeader("Transfer-Encoding");
                if (tes != null) {
                    if (!"chunked".equalsIgnoreCase(tes)) {
                        throw new IOException("Message with unsupported transfer encoding '" + tes + "' received");
                    }
                    LOGGER.fine(loggingPrefix + "Reading chunked contents");
                    HttpMessage.readChunked(in, multiplexer, buffer, runnable);
                } else
                {
                    LOGGER.fine(loggingPrefix + "Reading streaming contents");
                    HttpMessage.read(in, multiplexer, buffer, runnable);
                }
            }
        }
    }

    /**
     * Reads a chunked message body from <var>in</var> into the <var>buffer</var> and runs <var>finished</var>.
     */
    private static void
    readChunked(
        final ReadableByteChannel              in,
        final Multiplexer                      multiplexer,
        final OutputStream                     buffer,
        final RunnableWhichThrows<IOException> finished
    ) throws IOException {

        new RunnableWhichThrows<IOException>() {

            @Override public void
            run() throws IOException {
                final RunnableWhichThrows<IOException> chunkReader = this;

                HttpMessage.readLine(in, multiplexer, new ConsumerWhichThrows<String, IOException>() {

                    @Override public void
                    consume(String line) throws IOException {

                        // Ignore the blank line between chunks.
                        if (line.length() == 0) {
                            HttpMessage.readLine(in, multiplexer, this);
                            return;
                        }

                        // Strip the chunk extension.
                        {
                            int idx = line.indexOf(';');
                            if (idx != -1) line = line.substring(0, idx);
                        }

                        // Parse and validate the chunk size.
                        long available;
                        try {
                            available = Long.parseLong(line, 16);
                        } catch (NumberFormatException nfe) {
                            // SUPPRESS CHECKSTYLE HidingCause
                            throw new IOException("Invalid chunk size field '" + line + "'");
                        }
                        if (available < 0) throw new IOException("Negative chunk size field '" + line + "'");

                        // Last chunk?
                        if (available == 0L) {
                            finished.run();
                            return;
                        }

                        // Read chunk contents and then the next chunk header.
                        HttpMessage.read(in, multiplexer, available, buffer, chunkReader);
                    }
                });
            }
        }.run();
    }

    /**
     * Reads exactly bytes from <var>in</var> until end-of-input into the <var>buffer</var> and then runs
     * <var>finished</var>.
     */
    private static void
    read(
        final ReadableByteChannel              in,
        final Multiplexer                      multiplexer,
        final OutputStream                     os,
        final RunnableWhichThrows<IOException> finished
    ) throws IOException {

        multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, new RunnableWhichThrows<IOException>() {

            final ByteBuffer buffer = ByteBuffer.allocate(8192);

            @Override public void
            run() throws IOException {
                this.buffer.rewind();
                int r = in.read(this.buffer);
                if (r == -1) {
                    finished.run();
                    return;
                }
                os.write(this.buffer.array(), 0, r);

                multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, this);
            }
        });
    }

    /**
     * Reads exactly <var>n</var> bytes from <var>in</var> into the <var>buffer</var> and then runs
     * <var>finished</var>.
     */
    private static void
    read(
        final ReadableByteChannel              in,
        final Multiplexer                      multiplexer,
        final long                             n,
        final OutputStream                     os,
        final RunnableWhichThrows<IOException> finished
    ) throws IOException {

        if (n == 0L) {
            finished.run();
            return;
        }

        multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, new RunnableWhichThrows<IOException>() {

            int              count = (int) n;
            final ByteBuffer buffer = ByteBuffer.allocate(8192);

            @Override public void
            run() throws IOException {
                this.buffer.rewind();
                this.buffer.limit(Math.min(this.count, this.buffer.capacity()));
                int r = in.read(this.buffer);
                if (r == -1) throw new EOFException();
                os.write(this.buffer.array(), 0, r);
                this.count -= r;
                if (this.count == 0) {
                    finished.run();
                    return;
                }

                multiplexer.register((SelectableChannel) in, SelectionKey.OP_READ, this);
            }
        });
    }
}
