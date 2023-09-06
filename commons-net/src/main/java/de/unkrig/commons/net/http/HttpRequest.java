
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.Multiplexer;
import de.unkrig.commons.io.PercentEncodingInputStream;
import de.unkrig.commons.io.PercentEncodingOutputStream;
import de.unkrig.commons.io.Readers;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of an HTTP request.
 */
public
class HttpRequest extends HttpMessage {

    private static final Logger LOGGER = Logger.getLogger(HttpRequest.class.getName());

    private static final Pattern REQUEST_LINE_PATTERN = (
        Pattern.compile("(\\p{Alpha}+) ([^ ]+)(?: HTTP/(\\d+\\.\\d+))?")
    );

    private Method method;
    private String httpVersion;

    /**
     * Representation of the various HTTP methods.
     */
    public
    enum Method {
        GET(HasBody.FALSE),
        POST(HasBody.TRUE),
        HEAD(HasBody.FALSE),
        PUT(HasBody.TRUE),
        CONNECT(HasBody.TRUE),
        OPTIONS(HasBody.IF_CONTENT_LENGTH_OR_TRANSFER_ENCODING),
        ;

        private HasBody hasBody;

        Method(HasBody hasBody) { this.hasBody = hasBody; }

        public HasBody hasBody() { return this.hasBody; }
    }

    /**
     * Parses and returns one HTTP request from the given {@link InputStream}.
     */
    public static HttpRequest
    read(InputStream in) throws IOException, InvalidHttpMessageException { return HttpRequest.read(in, ">>> "); }

    /**
     * Parses and returns one HTTP request from the given {@link InputStream}.
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    public static HttpRequest
    read(InputStream in, String loggingPrefix) throws IOException, InvalidHttpMessageException {

        // Read and parse first request line.
        Method method;
        String httpVersion;
        URI    uri;
        {
            String requestLine = HttpMessage.readLine(in);
            LOGGER.fine(loggingPrefix + requestLine);

            Matcher matcher = REQUEST_LINE_PATTERN.matcher(requestLine);
            if (!matcher.matches()) {
                LOGGER.warning("Invalid request line '" + requestLine + "'");
                throw new IOException("Invalid request line");
            }

            method = Method.valueOf(matcher.group(1));

            try {
                uri = (
                    method == Method.CONNECT
                    ? new URI(null, matcher.group(2), null, null, null) :
                    new URI(matcher.group(2))
                );
            } catch (URISyntaxException use) {
                throw new InvalidHttpMessageException(use);
            }

            httpVersion = matcher.group(3);
            if (httpVersion == null) httpVersion = "0.9";
        }

        return new HttpRequest(method, uri, httpVersion, in, loggingPrefix);
    }

    public
    HttpRequest(Method method, URI uri, String httpVersion, InputStream in) throws IOException {
        this(method, uri, httpVersion, in, ">>> ");
    }

    /**
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    public
    HttpRequest(Method method, URI uri, String httpVersion, InputStream in, String loggingPrefix) throws IOException {
        super(in, true, method.hasBody(), loggingPrefix);
        this.method        = method;
        this.httpVersion   = httpVersion;
        this.uri           = uri;
        this.uriQueryValid = true;
        this.parameterList = null;
        this.parameterMap  = null;

        if (method == Method.CONNECT) this.setAttemptUnstreaming(false);
    }

    public
    HttpRequest(Method method, URI uri, String httpVersion) {
        super(method.hasBody() == HasBody.TRUE);
        this.method        = method;
        this.httpVersion   = httpVersion;
        this.uri           = uri;
        this.uriQueryValid = true;
        this.parameterList = null;
        this.parameterMap  = null;

        if (method == Method.CONNECT) this.setAttemptUnstreaming(false);
    }

    /**
     * @return The HTTP request's {@link Method}
     */
    public Method
    getMethod() { return this.method; }

    /** @return This HTTP request's HTTP version, as given in the request line */
    public String
    getHttpVersion() { return this.httpVersion; }

    /** Query component is valid iff {@link #uriQueryValid}. */
    private URI     uri;
    private boolean uriQueryValid;

    /** {@code null} indicates it needs to be updated from the URI. */
    @Nullable private List<Map.Entry<String, String>> parameterList = new ArrayList<Map.Entry<String, String>>();

    /** {@code null} indicates it needs to be updated from the URI. */
    @Nullable private Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();

    /** @return The URI of this HTTP request */
    public URI
    getUri() {
        if (!this.uriQueryValid) {
            if (this.parameterList != null) this.updateUriFromParameterList();
            this.uriQueryValid = true;
        }
        return this.uri;
    }

    /** Changes the URI of this HTTP request. */
    public final void
    setUri(URI uri) {
        this.uri           = uri;
        this.uriQueryValid = true;
        this.parameterList = null;
        this.parameterMap  = null;
    }

    /**
     * @return The parameters of this request, in order, as they exist in the body (POST, PUT) or in the query string
     *         (all other HTTP methods)
     */
    public List<Map.Entry<String, String>>
    getParameterList() throws IOException {

        if (this.parameterList == null) {
            this.updateParameterListFromQueryOrBody();
            this.parameterMap = null;
        }

        return Collections.unmodifiableList(this.parameterList);
    }

    /**
     * Changes this HTTP request's parameters.
     */
    public void
    setParameterList(Iterable<Map.Entry<String, String>> parameters) {

        List<Entry<String, String>> pl = this.parameterList;
        if (pl == null) {
            pl = (this.parameterList = new ArrayList<Map.Entry<String, String>>());
        } else {
            pl.clear();
        }
        for (Entry<String, String> e : parameters) {
            pl.add(HttpRequest.entry(e.getKey(), e.getValue()));
        }
        this.uriQueryValid = false;
        this.parameterMap  = null;
    }

    /** @return The values of all parameters with the given <var>name</var> */
    @Nullable public String[]
    getParameter(String name) throws IOException {

        this.getParameterMap();

        List<String> l = this.getParameterMap().get(name);
        return l == null ? null : l.toArray(new String[l.size()]);
    }

    /** Adds another parameter. */
    public void
    addParameter(String name, String value) throws IOException {
        this.addParameter(name, new String[] { value });
    }

    /** Adds a multi-value parameter. */
    public void
    addParameter(String name, String[] values) throws IOException {

        // Modify parameterList.
        {
            List<Entry<String, String>> pl = this.getParameterList();
            for (String value : values) pl.add(HttpRequest.entry(name, value));
        }

        // Modify parameterMap.
        {
            Map<String, List<String>> pm = this.getParameterMap();
            List<String>              l  = pm.get(name);
            if (l == null) {
                l = new ArrayList<String>();
                pm.put(name, l);
            }
            for (String value : values) l.add(value);
        }

        // Invalidate uri.
        if (this.method != Method.POST && this.method != Method.PUT) this.uriQueryValid = false;
    }

    /** Removes all parameters with the given name and adds another parameter. */
    public void
    setParameter(String name, String value) throws IOException {
        this.setParameter(name, new String[] { value });
    }

    /** Removes all parameters with the given name and adds another multi-value parameter. */
    public void
    setParameter(String name, String[] values) throws IOException {

        this.getParameterMap();

        // Modify the parameterMap.
        {
            List<String> l = this.getParameterMap().get(name);
            if (l == null) {
                this.addParameter(name, values);
                return;
            }
            l.clear();
            for (String value : values) l.add(value);
        }

        // Modify the parameterList.
        {
            List<Entry<String, String>> pl = this.getParameterList();
            for (Iterator<Entry<String, String>> it = pl.iterator(); it.hasNext();) {
                if (it.next().getKey().equals(name)) it.remove();
            }
            for (String value : values) pl.add(HttpRequest.entry(name, value));
        }

        // Invalidate uri.
        if (this.method != Method.POST && this.method != Method.PUT) this.uriQueryValid = false;
    }

    private void
    updateParameterListFromQueryOrBody() throws IOException {

        String query;
        if (this.method == Method.POST || this.method == Method.PUT) {
            query = Readers.readAll(new InputStreamReader(
                this.removeBody().inputStream(),
                this.getCharset()
            ));
        } else {
            assert this.uriQueryValid;
            query = this.uri.getQuery();
        }

        List<Entry<String, String>> pl = (this.parameterList = new ArrayList<Map.Entry<String, String>>());
        pl.addAll(HttpRequest.decodeParameters(query));
    }

    private void
    updateUriFromParameterList() {
        List<Entry<String, String>> pl = this.parameterList;
        assert pl != null;
        try {
            this.uri = new URI(
                this.uri.getScheme(),
                this.uri.getUserInfo(),
                this.uri.getHost(),
                this.uri.getPort(),
                this.uri.getPath(),
                HttpRequest.encodeParameters(pl),
                this.uri.getFragment()
            );
        } catch (URISyntaxException use) {
            if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Updating URI", use);
        }
    }

    private Map<String, List<String>>
    getParameterMap() throws IOException {
        Map<String, List<String>> pm = this.parameterMap;
        if (pm != null) return pm;

        pm = (this.parameterMap = new HashMap<String, List<String>>());

        for (Entry<String, String> e : this.getParameterList()) {

            String key   = e.getKey();
            String value = e.getValue();

            List<String> l = pm.get(key);
            if (l == null) {
                l = new ArrayList<String>();
                pm.put(key, l);
            }
            l.add(value);
        }
        return pm;
    }

    /**
     *  Transforms a {@code List<Entry<String, String>>} into {@code "a=b&c=d"}.
     *
     *  @return {@code null} iff the <var>parameterList</var> is empty
     */
    @Nullable private static String
    encodeParameters(List<Entry<String, String>> parameterList) {
        Iterator<Entry<String, String>> it = parameterList.iterator();
        if (!it.hasNext()) return null;

        ByteArrayOutputStream baos;
        try {
            baos = new ByteArrayOutputStream();
            PercentEncodingOutputStream peos = new PercentEncodingOutputStream(baos);
            for (;;) {
                Entry<String, String> e = it.next();
                new OutputStreamWriter(peos, StandardCharsets.UTF_8).write(e.getKey());
                peos.writeUnencoded('=');
                new OutputStreamWriter(peos, StandardCharsets.UTF_8).write(e.getValue());
                if (!it.hasNext()) break;
                peos.writeUnencoded('&');
            }
        } catch (IOException ioe) {
            if (LOGGER.isLoggable(FINE)) LOGGER.log(FINE, "Decoding parameters", ioe);
            return null;
        }

        @SuppressWarnings("deprecation") String result = new String(baos.toByteArray(), 0);
        return result;
    }

    @SuppressWarnings("deprecation") private static List<Map.Entry<String, String>>
    decodeParameters(@Nullable String s) throws IOException {

        if (s == null) return Collections.emptyList();

        byte[] bytes;
        {
            int len = s.length();
            bytes = new byte[len];
            s.getBytes(0, len, bytes, 0); // <= Deprecated, but exactly what we want.
        }
        return HttpRequest.decodeParameters(bytes);
    }

    private static List<Map.Entry<String, String>>
    decodeParameters(byte[] bytes) throws IOException {

        int len = bytes.length;

        List<Map.Entry<String, String>> result = new ArrayList<Map.Entry<String, String>>();
        for (int off = 0; off < len;) {

            // Search first '=' or '&'.
            int to;
            for (to = off; to < len && bytes[to] != '=' && bytes[to] != '&'; to++);

            // Read parameter key.
            String key = HttpRequest.percentDecode(bytes, off, to - off);

            if (to == len) {

                // Last parameter, without a value.
                result.add(HttpRequest.entry(key, ""));
                break;
            } else
            if (bytes[to] == '&') {

                // Parameter without a value is treated like "empty value".
                result.add(HttpRequest.entry(key, ""));
                off = to + 1;
            } else
            {

                // Find next '&'.
                off = to + 1;
                for (to = off; to < len && bytes[to] != '&'; to++);

                // Read parameter value.
                String value = HttpRequest.percentDecode(bytes, off, to - off);
                result.add(HttpRequest.entry(key, value));

                if (to == len) break;

                // Skip '&' after parameter value.
                off = to + 1;
            }
        }

        return result;
    }

    @NotNullByDefault(false) private static Entry<String, String>
    entry(final String key, final String value) {
        return new Map.Entry<String, String>() {

            @Override public String
            getKey() { return key; }

            @Override public String
            getValue() { return value; }

            @Override public String
            setValue(String value) { throw new UnsupportedOperationException("setValue"); }
        };
    }

    private static String
    percentDecode(byte[] bytes, int off, int len) throws IOException {

        PercentEncodingInputStream is = new PercentEncodingInputStream(new ByteArrayInputStream(bytes, off, len));

        try {

            // According to https://tools.ietf.org/html/rfc3986#section-2.5, "... the data should first be encoded as
            // octets according to the UTF-8 character encoding".
            return Readers.readAll(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (MalformedInputException mie) {

            // Bytes could not be UTF-8-decoded, so fall back to ISO 8859-1.
            return Readers.readAll(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
        }
    }

    /** Changes the HTTP method of this request. */
    public void
    setMethod(Method method) { this.method = method; }

    /** Changes the HTTP version of this request. */
    public void
    setHttpVersion(String httpVersion) { this.httpVersion = httpVersion; }

    /**
     * Writes this HTTP request to the given {@link OutputStream}.
     */
    public void
    write(OutputStream out) throws IOException { this.writeHeadersAndBody("<<< ", out); }

    /**
     * Writes this HTTP request to the given {@link OutputStream}.
     *
     * @param loggingPrefix E.g. {@code "<<< "}
     */
    public void
    write(OutputStream out, String loggingPrefix) throws IOException {

        {
            String requestLine = (
                this.method
                + " "
                + (this.method == Method.CONNECT ? this.uri.getAuthority() : this.uri)
            );
            if (!"0.9".equals(this.httpVersion)) requestLine += " HTTP/" + this.httpVersion;
            LOGGER.fine(loggingPrefix + requestLine);

            Writer w = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
            w.write(requestLine + "\r\n");
            w.flush();
        }

        this.writeHeadersAndBody(loggingPrefix, out);
    }

    /**
     * Reads one HTTP request from <var>in</var> through the <var>multiplexer</var> and passes it to the
     * <var>requestConsumer</var>.
     */
    public static void
    read(
        final ReadableByteChannel                           in,
        final Multiplexer                                   multiplexer,
        final ConsumerWhichThrows<HttpRequest, IOException> requestConsumer
    ) throws IOException { HttpRequest.read(in, multiplexer, requestConsumer, ">>> "); }

    /**
     * Reads one HTTP request from <var>in</var> through the <var>multiplexer</var> and passes it to the
     * <var>requestConsumer</var>.
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    public static void
    read(
        final ReadableByteChannel                           in,
        final Multiplexer                                   multiplexer,
        final ConsumerWhichThrows<HttpRequest, IOException> requestConsumer,
        final String                                        loggingPrefix
    ) throws IOException {

        ConsumerWhichThrows<String, IOException> requestLineConsumer = new ConsumerWhichThrows<String, IOException>() {

            @Override public void
            consume(String requestLine) throws IOException {

                final HttpRequest.Method method;
                final URI                uri;
                final String             httpVersion;
                {
                    Matcher matcher = REQUEST_LINE_PATTERN.matcher(requestLine);
                    if (!matcher.matches()) {
                        LOGGER.warning("Invalid request line '" + requestLine + "'");
                        throw new InvalidHttpMessageException("Invalid request line");
                    }

                    method = Method.valueOf(matcher.group(1));

                    try {
                        uri = new URI(matcher.group(2));
                    } catch (URISyntaxException use) {
                        throw new InvalidHttpMessageException(use);
                    }

                    httpVersion = matcher.group(3) == null ? "0.9" : matcher.group(3);
                }

                HttpMessage.readHeaders(in, multiplexer, new ConsumerWhichThrows<List<MessageHeader>, IOException>() {

                    @Override public void
                    consume(List<MessageHeader> headers) throws IOException {
                        final HttpRequest httpRequest = new HttpRequest(method, uri, httpVersion);
                        if (method == Method.POST || method == Method.PUT) {
                            httpRequest.readBody(in, multiplexer, new RunnableWhichThrows<IOException>() {

                                @Override public void
                                run() throws IOException {
                                    requestConsumer.consume(httpRequest);
                                }
                            }, loggingPrefix);
                        } else {
                            requestConsumer.consume(httpRequest);
                        }
                    }
                }, loggingPrefix);
            }
        };

        HttpMessage.readLine(in, multiplexer, requestLineConsumer);
    }
}
