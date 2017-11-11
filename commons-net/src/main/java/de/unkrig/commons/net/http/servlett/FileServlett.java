
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

package de.unkrig.commons.net.http.servlett;

import static java.util.logging.Level.FINE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.FixedLengthInputStream;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.net.http.HttpMessage;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.net.http.HttpResponse.Status;

/**
 * A {@link Servlett} that serves documents with {@link File}s.
 */
public abstract
class FileServlett extends AbstractServlett {

    private static final Logger LOGGER = Logger.getLogger(FileServlett.class.getName());

    /** Length limit for member names in a directory listing. */
    private static final int LENGTH_LIMIT = 40;

    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = new HashMap<String, String>();
    static {
        EXTENSION_TO_CONTENT_TYPE.put("gif",       "image/gif");
        EXTENSION_TO_CONTENT_TYPE.put("jpg",       "image/jpeg");
        EXTENSION_TO_CONTENT_TYPE.put("jpeg",      "image/jpeg");
        EXTENSION_TO_CONTENT_TYPE.put("css",       "text/css");
        EXTENSION_TO_CONTENT_TYPE.put("html",      "text/html");
        EXTENSION_TO_CONTENT_TYPE.put("java",      "text/plain");
        EXTENSION_TO_CONTENT_TYPE.put("txt",       "text/plain");
        EXTENSION_TO_CONTENT_TYPE.put("classpath", "text/xml");
        EXTENSION_TO_CONTENT_TYPE.put("launch",    "text/xml");
        EXTENSION_TO_CONTENT_TYPE.put("project",   "text/xml");
        EXTENSION_TO_CONTENT_TYPE.put("xml",       "text/xml");
    }

    /**
     * Translates an HTTP request (typically the URI of the HTTP request) into a {@link File}.
     *
     * @return The {@link File} corresponding with the HTTP request
     */
    protected abstract File
    getFile(HttpRequest httpRequest);

    @Override public HttpResponse
    get(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {

        File file = this.getFile(httpRequest);
        try {
            file = file.getCanonicalFile();
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(file.toString(), ioe);
        }

        if (LOGGER.isLoggable(FINE)) LOGGER.fine("Accessing file '" + file + "'");

        long from = 0, to = Long.MAX_VALUE;

        String s = httpRequest.getHeader("Range");
        if (s != null) {
            Matcher m = RANGES_SPECIFIER.matcher(s);
            if (m.matches()) {
                if (m.group(1) != null) from = Long.parseLong(m.group(1));
                if (m.group(2) != null) to   = Long.parseLong(m.group(2) + 1);
            }
        }

        // Attempt to read a file.
        String path = httpRequest.getUri().getPath();
        if (path.endsWith("/") && file.isDirectory()) {
            File indexFile = new File(file, "index.html");
            if (indexFile.isFile()) {
                return processFileRequest(indexFile, from, to);
            }

            // Generate a nice directory listing.
            return processDirectoryListing(file);
        }

        // Handle file.
        if (file.isFile()) {

            HttpResponse rsp = processFileRequest(file, from, to);
            return rsp;
        }

        // Handle directory.
        if (file.isDirectory()) {

            // Redirect "/dir" to "/dir/".
            return HttpResponse.redirect(path + "/");
        }

        // Give up.
        return HttpResponse.response(
            Status.NOT_FOUND,
            "Sorry - resource '" + path + "' does not exist on this server."
        );
    }
    private static final Pattern RANGES_SPECIFIER = Pattern.compile("bytes=(\\d+)?-(\\d+)?");

    @Override protected HttpResponse
    head(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {

        HttpResponse httpResponse = this.get(httpRequest, sendProvisionalResponse);

        httpResponse.setBody(HttpMessage.NO_BODY);

        return httpResponse;
    }

    @Override public HttpResponse
    put(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws IOException {

        File file = this.getFile(httpRequest);
        try {
            file = file.getCanonicalFile();
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(file.toString(), ioe);
        }

        if (LOGGER.isLoggable(FINE)) LOGGER.fine("Accessing file '" + file + "'");

        // Handle file.
        if (file.isFile() || !file.exists()) {

            OutputStream os = new FileOutputStream(file);
            try {
                httpRequest.removeBody().write(os);
                os.close();
            } finally {
                try { os.close(); } catch (Exception e) {}
            }

            return HttpResponse.response(Status.OK);
        }

        // Give up.
        return HttpResponse.response(
            Status.NOT_FOUND,
            "Sorry - resource '" + httpRequest.getUri().getPath() + "' does not exist on this server."
        );
    }

    /**
     * Generates an HTML listing for the given <var>directory</var>.
     */
    static HttpResponse
    processDirectoryListing(final File directory) {

        HttpResponse response = HttpResponse.response(
            Status.OK,
            HttpMessage.body(new ConsumerWhichThrows<OutputStream, IOException>() {

                @Override public void
                consume(OutputStream stream) throws IOException {
                    Writer w = new OutputStreamWriter(stream);
                    w.write(
                        "<html>\r\n"
                        + "  <head>\r\n"
                        + "    <title>Directory Listing</title>\r\n"
                        + "  </head>\r\n"
                        + "  <body>\r\n"
                        + "  <h2>Directory listing of '"
                        + directory.toString()
                        + "'</h2>\r\n"
                        + "  <pre><a href=\"../\">../</a>"
                    );
                    for (File f : directory.listFiles()) {
                        String name = f.getName();
                        String displayName, title;
                        if (name.length() > LENGTH_LIMIT) {
                            displayName = name.substring(0, LENGTH_LIMIT - 3) + "...";
                            title       = name;
                        } else {
                            displayName = name;
                            title       = null;
                        }
                        if (f.isDirectory()) name += '/';
                        new Formatter(w).format((
                            "\r\n"
                            + "<a href=\""
                            + name
                            + "\""
                            + (title == null ? "" : " title=\"" + title + "\"")
                            + ">"
                            + "%-"
                            + (LENGTH_LIMIT + 4)
                            + "s %s"
                        ), displayName + "</a>", new Date(f.lastModified()));
                    }
                    w.write(
                        "</pre>\r\n"
                        + "  </body>\r\n"
                        + "</html>\r\n"
                    );
                    w.flush();
                }
            })
        );
        response.setHeader("Content-Type", "text/html");
        response.setHeader("Transfer-Encoding", "chunked");
        return response;
    }

    /**
     * Serves the given <var>file</var> to the HTTP client.
     *
     * @param from Index of the first byte in the file; must be &gt;= 0
     * @param to   Index of the last byte in the file +plus on; must be &gt; <var>from</var>
     */
    static HttpResponse
    processFileRequest(File file, long from, long to) throws IOException {

        // Guess the content type from the file name extension.
        String contentType;
        {
            String extension;
            {
                String name = file.getName();
                int    idx  = name.lastIndexOf('/') + 1;
                idx       = name.indexOf('.', idx);
                extension = idx == -1 ? "" : name.substring(idx + 1);
            }
            contentType = EXTENSION_TO_CONTENT_TYPE.get(extension);
            if (contentType == null) contentType = "application/octet-stream";
        }

        long fileLength = file.length();

        if (to   > fileLength) to   = fileLength;
        if (from > to)         from = to;

        long contentLength = to - from;

        FileInputStream is = new FileInputStream(file);
        if (InputStreams.skip(is, from) != from) {
            throw new IOException("Cannot skip " + from + " bytes of \"" + file + "\"");
        }

        // Create the response.
        HttpResponse httpResponse = HttpResponse.response(
            Status.OK,
            new FixedLengthInputStream(is, contentLength)
        );
        httpResponse.addHeader("Content-Length", contentLength);
        if (contentLength != fileLength) {
            httpResponse.addHeader("Content-Range", from + "-" + (to - 1) + "/" + fileLength);
        }
        httpResponse.addHeader("Last-Modified", new Date(file.lastModified()));
        httpResponse.addHeader("Content-Type", contentType);

        return httpResponse;
    }
}
