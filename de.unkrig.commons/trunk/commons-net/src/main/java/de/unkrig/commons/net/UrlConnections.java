
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.commons.net;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link URLConnection}.
 */
public final
class UrlConnections {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final Logger LOGGER = Logger.getLogger(UrlConnections.class.getName());

    /**
     * The charset that applies if the HTTP "Content-Type:" header is missing or has no "charset" parameter.
     * <p>
     *  See <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>, sections "3.4.1 Missing Charset" and "3.7.1
     *  Canonicalization and Text Defaults".
     *</p>
     */
    public static final Charset HTTP_DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    private
    UrlConnections() {}

    /**
     * The "follow redirects" feature of {@link HttpURLConnection} doesn't support "cross-protocol" redirects, e.g.
     * from "{@code http://foo}" to "{@code https://foo}". This methods implements them.
     *
     * @return <var>httpConn</var>, or a new {@link URLConnection} iff the connection was cross-protocol-redirected
     */
    public static URLConnection
    followRedirects2(HttpURLConnection httpConn) throws IOException {

        LOGGER.log(Level.FINE, "followRedirects2({0})", httpConn);

        for (int attempt = 0;; attempt++) {

            if (attempt == 10) {
                throw new IOException(
                    "Giving up after "
                    + attempt
                    + " REDIRECTs (last location was '"
                    + httpConn.getURL()
                    + "')"
                );
            }

            try {
                final Set<Entry<String, List<String>>> requestProperties = httpConn.getRequestProperties().entrySet();

                int responseCode = httpConn.getResponseCode();

                // 2xx?
                if (responseCode < 300) return httpConn;

                // >= 400?
                if (responseCode >= 400) {
                    throw toException(httpConn, null);
                }

                String location = httpConn.getHeaderField("Location");
                if (location == null) {
                    throw new IOException("Response with code " + responseCode + " lacks the 'Location:' header field");
                }

                URL url;
                try {
                    url = new URL(httpConn.getURL(), location);
                } catch (MalformedURLException mue) {
                    throw new IOException("Invalid redirection location '" + location + "'", mue);
                }

                URLConnection conn2 = url.openConnection();

                // Special handling for response "303 See other".
                if (responseCode == 303) return conn2;

                // Copy all connection properties to the new connection.
                conn2.setAllowUserInteraction(httpConn.getAllowUserInteraction());
                conn2.setConnectTimeout(httpConn.getConnectTimeout());
                conn2.setDefaultUseCaches(httpConn.getDefaultUseCaches());
                conn2.setDoInput(httpConn.getDoInput());
                conn2.setDoOutput(httpConn.getDoOutput());
                conn2.setIfModifiedSince(httpConn.getIfModifiedSince());
                conn2.setReadTimeout(httpConn.getReadTimeout());
                conn2.setUseCaches(httpConn.getUseCaches());
                for (Entry<String, List<String>> e : requestProperties) {
                    for (String v : e.getValue()) {
                        conn2.addRequestProperty(e.getKey(), v);
                    }
                }

                if (!(conn2 instanceof HttpURLConnection)) return conn2;

                HttpURLConnection httpConn2 = (HttpURLConnection) conn2;

                // Copy all HTTP connection properties to the new connection.
                {
                    int chunkLength = getChunkLength(httpConn);
                    if (chunkLength != -1) {
                        httpConn2.setChunkedStreamingMode(chunkLength);
                    }
                }
                {
                    int fixedContentLength = getFixedContentLength(httpConn);
                    if (fixedContentLength != -1) {
                        httpConn2.setFixedLengthStreamingMode(fixedContentLength);
                    }
                }
                httpConn2.setInstanceFollowRedirects(httpConn.getInstanceFollowRedirects());
                httpConn2.setRequestMethod(httpConn.getRequestMethod());

                httpConn = httpConn2;
            } catch (Exception e) {

                LOGGER.log(Level.FINE, null, e);

                throw toException(httpConn, e);
            }
        }
    }

    /**
     * @return An {@link IOException} that contains detailed information about the request, the response,
     *         and the response's "error stream"
     */
    public static IOException
    toException(HttpURLConnection httpConn, @Nullable Throwable cause) throws IOException {

        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND && cause == null) {
            return new FileNotFoundException(httpConn.getURL().toString());
        }

        StringBuilder sb = new StringBuilder();

        sb.append(httpConn.getRequestMethod()).append(" ").append(httpConn.getURL()).append('\n');

        sb.append(responseCode).append(" ").append(httpConn.getResponseMessage()).append('\n');

        InputStream errorStream = httpConn.getErrorStream();
        if (errorStream != null) {
            sb.append(IoUtil.readAll(new InputStreamReader(errorStream, Charset.forName("ISO-8859-1"))));
        }

        return new IOException(sb.toString(), cause);
    }

    /**
     * The missing counterpart for {@link HttpURLConnection#setFixedLengthStreamingMode(int)}.
     * 
     * @return The fixed content-length when using fixed-length streaming mode; {@code -1} means fixed-length streaming
     *         mode is disabled for output
     */
    private static int
    getFixedContentLength(HttpURLConnection httpConn) {

        try {

            return (Integer) HttpURLConnection.class.getDeclaredField("fixedContentLength").get(httpConn);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * The missing counterpart for {@link HttpURLConnection#setChunkedStreamingMode(int)}.
     * 
     * @return The chunk-length when using chunked encoding streaming mode for output; {@code -1} means chunked
     *         encoding is disabled for output
     */
    private static int
    getChunkLength(HttpURLConnection httpConn) {

        try {

            return (Integer) HttpURLConnection.class.getDeclaredField("chunkLength").get(httpConn);
        } catch (Exception e) {
            return -1;
        }
    }
}
