
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of one HTTP response.
 */
public
class HttpResponse extends HttpMessage {

    private static final Logger LOGGER = Logger.getLogger(HttpResponse.class.getName());

    private static final Pattern STATUS_LINE_PATTERN = Pattern.compile("HTTP/(\\d+\\.\\d+) (\\d+) (.*)");

    private String       httpVersion;
    private final Status status;
    private String       reasonPhrase;

    /** Representation of the various HTTP response statuses. */
    public
    enum Status {

        // SUPPRESS CHECKSTYLE JavadocVariable:40
        CONTINUE                       (100, "Continue",                        false),
        SWITCHING_PROTOCOLS            (101, "Switching Protocols",             false),
        OK                             (200, "OK",                              true),
        CREATED                        (201, "Created",                         true),
        ACCEPTED                       (202, "Accepted",                        true),
        NON_AUTHORITATIVE_INFORMATION  (203, "Non-Authoritative Information",   true),
        NO_CONTENT                     (204, "No Content",                      false),
        RESET_CONTENT                  (205, "Reset Content",                   true),
        PARTIAL_CONTENT                (206, "Partial Content",                 true),
        MULTIPLE_CHOICES               (300, "Multiple Choices",                true),
        MOVED_PERMANENTLY              (301, "Moved Permanently",               true),
        FOUND                          (302, "Found",                           true),
        SEE_OTHER                      (303, "See Other",                       true),
        NOT_MODIFIED                   (304, "Not Modified",                    false),
        USE_PROXY                      (305, "Use Proxy",                       true),
        TEMPORARY_REDIRECT             (307, "Temporary Redirect",              true),
        BAD_REQUEST                    (400, "Bad Request",                     true),
        UNAUTHORIZED                   (401, "Unauthorized",                    true),
        PAYMENT_REQUIRED               (402, "Payment Required",                true),
        FORBIDDEN                      (403, "Forbidden",                       true),
        NOT_FOUND                      (404, "Not Found",                       true),
        METHOD_NOT_ALLOWED             (405, "Method Not Allowed",              true),
        NOT_ACCEPTABLE                 (406, "Not Acceptable",                  true),
        PROXY_AUTHENTICATION_REQUIRED  (407, "Proxy Authentication Required",   true),
        REQUEST_TIMEOUT                (408, "Request Time-out",                true),
        CONFLICT                       (409, "Conflict",                        true),
        GONE                           (410, "Gone",                            true),
        LENGTH_REQUIRED                (411, "Length Required",                 true),
        PRECONDITION_FAILED            (412, "Precondition Failed",             true),
        REQUEST_ENTITY_TOO_LARGE       (413, "Request Entity Too Large",        true),
        REQEST_URI_TOO_LONG            (414, "Request-URI Too Large",           true),
        UNSUPPORTED_MEDIA_TYPE         (415, "Unsupported Media Type",          true),
        REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable", true),
        EXPECTATION_FAILED             (417, "Expectation Failed",              true),
        INTERNAL_SERVER_ERROR          (500, "Internal Server Error",           true),
        NOT_IMPLEMENTED                (501, "Not Implemented",                 true),
        BAD_GATEWAY                    (502, "Bad Gateway",                     true),
        SERVICE_UNAVAILABLE            (503, "Service Unavailable",             true),
        GATEWAY_TIMEOUT                (504, "Gateway Time-out",                true),
        HTTP_VERSION_NOT_SUPPORTED     (505, "HTTP Version not supported",      true);

        private final int     code;
        private final String  reasonPhrase;
        private final boolean hasBody;

        private static final Map<Integer, Status> CODE_TO_STATUS = new HashMap<Integer, HttpResponse.Status>();
        static { for (Status status : Status.values()) Status.CODE_TO_STATUS.put(status.code, status); }

        Status(int code, String reasonPhrase, boolean hasBody) {
            this.code         = code;
            this.reasonPhrase = reasonPhrase;
            this.hasBody      = hasBody;
        }

        /** @return This HTTP response's code */
        public int
        getCode() { return this.code; }

        /** @return This HTTP response's reason phrase, i.e. the text in the status line after the response code */
        public String
        getReasonPhrase() { return this.reasonPhrase; }

        /** @return Whether this HTTP response has a body */
        public boolean
        hasBody() { return this.hasBody; }

        /**
         * @return {@code null} iff a status with that code does not exist
         */
        @Nullable public static Status
        fromCode(int code) {
            return Status.CODE_TO_STATUS.get(code);
        }

        public boolean
        isInformational() { return this.code >= 100 && this.code < 200; }

        @Override public String
        toString() { return this.code + " " + this.name(); }
    }

    /**
     * Constructor for outgoing responses.
     */
    public
    HttpResponse(InputStream bodyStream) {
        this(Status.OK, bodyStream);
    }

    /**
     * Constructor for outgoing responses.
     */
    public
    HttpResponse(Status status, String body) {
        super(status.hasBody());

        this.httpVersion  = "1.1";
        this.status       = status;
        this.reasonPhrase = status.getReasonPhrase();
        this.setBody(HttpMessage.body(body, this.getCharset()));
    }

    /**
     * Constructor for outgoing responses.
     */
    public
    HttpResponse(Status status, InputStream bodyStream) {
        super(status.hasBody());

        this.httpVersion  = "1.1";
        this.status       = status;
        this.reasonPhrase = status.getReasonPhrase();
        this.setBody(HttpMessage.body(bodyStream));
    }

    /**
     * Constructor for outgoing responses.
     */
    public
    HttpResponse(Status status, File contentsFile) throws FileNotFoundException {
        this(status, new FileInputStream(contentsFile));
    }

    /**
     * Constructor for outgoing responses.
     */
    public
    HttpResponse(Status status, Body body) {
        super(status.hasBody());

        this.httpVersion  = "1.1";
        this.status       = status;
        this.reasonPhrase = status.getReasonPhrase();
        this.setBody(body);
    }
    
    /**
     * Constructor for incoming responses.
     * <p>
     *   Notice that <var>in</var> will be read and closed when the body of this message is processed or disposed
     *   (see {@link Body}).
     * </p>
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    private
    HttpResponse(
        String      httpVersion,
        Status      status,
        String      reasonPhrase,
        InputStream in,
        boolean     isResponseToHEAD, // SUPPRESS CHECKSTYLE Abbreviation
        String      loggingPrefix
    ) throws IOException {
        super(
            in,                                    // in
            !"0.9".equals(httpVersion),            // hasHeaders
            !isResponseToHEAD && status.hasBody(), // hasBody
            loggingPrefix
        );
        this.httpVersion  = httpVersion;
        this.status       = status;
        this.reasonPhrase = reasonPhrase;
    }
    
    /**
     * Constructor for incoming responses.
     * <p>
     *   Notice that <var>in</var> will be read and closed when the body of this message is processed or disposed
     *   (see {@link HttpMessage.Body}).
     * </p>
     */
    public static HttpResponse
    read(
        InputStream in,
        String      httpVersion,
        boolean     isResponseToHEAD // SUPPRESS CHECKSTYLE Abbreviation
    ) throws IOException { return HttpResponse.read(in, httpVersion, isResponseToHEAD, ">>> "); }

    /**
     * Constructor for incoming responses.
     * <p>
     *   Notice that <var>in</var> will be read and closed when the body of this message is processed or disposed
     *   (see {@link HttpMessage.Body}).
     * </p>
     *
     * @param loggingPrefix E.g. {@code ">>> "}
     */
    public static HttpResponse
    read(
        InputStream in,
        String      httpVersion,
        boolean     isResponseToHEAD, // SUPPRESS CHECKSTYLE Abbreviation
        String      loggingPrefix
    ) throws IOException {

        Status status;
        String reasonPhrase;
        if ("0.9".equals(httpVersion)) {
            status       = Status.OK;
            reasonPhrase = status.getReasonPhrase();
        } else {

            // Read and parse status line.
            {
                String statusLine = HttpMessage.readLine(in);
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(loggingPrefix + statusLine);

                Matcher m = HttpResponse.STATUS_LINE_PATTERN.matcher(statusLine);
                if (!m.matches()) throw new InvalidHttpMessageException("Invalid status line");
                httpVersion = m.group(1);
                int statusCode;
                try {
                    statusCode = Integer.parseInt(m.group(2));
                } catch (NumberFormatException nfe) {
                 // SUPPRESS CHECKSTYLE AvoidHidingCause
                    throw new InvalidHttpMessageException("Invalid status code '" + m.group(2) + "'");
                }
                status = Status.fromCode(statusCode);
                if (status == null) throw new InvalidHttpMessageException("Invalid status code '" + statusCode + "'");
                reasonPhrase = m.group(3);
            }
        }

        return new HttpResponse(httpVersion, status, reasonPhrase, in, isResponseToHEAD, loggingPrefix);
    }
    
    /**
     * Writes this HTTP response to the given {@link OutputStream}.
     */
    public void
    write(OutputStream out) throws IOException { this.write(out, "<<< "); }

    /**
     * Writes this HTTP response to the given {@link OutputStream}.
     *
     * @param loggingPrefix E.g. {@code "<<< "}
     */
    public void
    write(OutputStream out, String loggingPrefix) throws IOException {

        String statusLine = "HTTP/" + this.httpVersion + ' ' + this.status.getCode() + ' ' + this.reasonPhrase;

        LOGGER.fine(loggingPrefix + statusLine);

        out.write((statusLine + "\r\n").getBytes("ASCII"));

        this.writeHeadersAndBody(loggingPrefix, out);
    }

    /**
     * @return Whether this response in "provisional" in the sense of <a
     * href="https://tools.ietf.org/html/rfc2616#section-10.1">RFC 2616: 10 Status Code Definitions: 10.1 Informational
     * 1xx</a>.
     */
    public boolean
    isProvisional() { return this.status.isInformational(); }

    /**
     * @return A response that redirects the HTTP client to a different location.
     */
    public static HttpResponse
    redirect(String targetURI) {
        HttpResponse httpResponse = new HttpResponse(
            Status.SEE_OTHER,
            "This page has temporarily moved to '" + targetURI + "'."
        );
        httpResponse.setHeader("Location", targetURI);
        return httpResponse;
    }

    /**
     * @return A response with a given status code and phrase.
     */
    public static HttpResponse
    response(Status status) {
        return new HttpResponse(status, status.reasonPhrase);
    }

    /**
     * @return A response with a given status code and body.
     */
    public static HttpResponse
    response(Status status, String body) {
        return new HttpResponse(status, body);
    }

    /**
     * @return A response with a given status code and body.
     */
    public static HttpResponse
    response(Status status, InputStream bodyStream) {
        return new HttpResponse(status, bodyStream);
    }

    /**
     * @return A response with a given status code and body.
     */
    public static HttpResponse
    response(Status status, Body body) {
        HttpResponse httpResponse = new HttpResponse(status, body);
        httpResponse.setHeader("Content-Type", "text/plain");
        return httpResponse;
    }

    public Status
    getStatus() { return this.status; }
}
