
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.net.http.HttpRequest;
import de.unkrig.commons.net.http.HttpResponse;
import de.unkrig.commons.net.http.HttpResponse.Status;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.Base64;

/**
 * A {@link Servlett} that processes the AUTHENTICATION information in the HTTp request, and passes control to the
 * next servlett iff authentication is successfully completed.
 *
 * @param <EX> See {@link #HttpAuthenticationServlett(String, UserNamePasswordPredicate)}
 */
public
class HttpAuthenticationServlett<EX extends Exception> extends AbstractServlett {

    /**
     * @see HttpAuthenticationServlett#HttpAuthenticationServlett(String, UserNamePasswordPredicate)
     */
    public
    interface UserNamePasswordPredicate<EX extends Throwable> {

        /**
         * @return Whether the given tuple of {@link HttpRequest}, <var>userName</var> and <var>password</var> is
         *         acceptable or not
         */
        boolean evaluate(HttpRequest httpRequest, String userName, char[] password) throws EX;
    }

    private final String                        realm;
    private final UserNamePasswordPredicate<EX> userNamePasswordPredicate;

    /**
     * @param userNamePasswordPredicate Determines whether any given tuple of {@link HttpRequest}, user name and
     *                                  password is allowed or not
     */
    public
    HttpAuthenticationServlett(String realm, UserNamePasswordPredicate<EX> userNamePasswordPredicate) {
        this.realm                     = realm;
        this.userNamePasswordPredicate = userNamePasswordPredicate;
    }

    @Override @Nullable public HttpResponse
    handleRequest(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse) {
        try {
            return this.handleRequest2(httpRequest, sendProvisionalResponse);
        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return HttpResponse.response(Status.INTERNAL_SERVER_ERROR, sw.toString());
        }
    }

    @Nullable private HttpResponse
    handleRequest2(HttpRequest httpRequest, ConsumerWhichThrows<HttpResponse, IOException> sendProvisionalResponse)
    throws EX {

        // Get the "Authorization:" header.
        String s = httpRequest.getHeader("Authorization");
        if (s == null) {

            // Authorization missing.
            HttpResponse httpResponse = HttpResponse.response(Status.UNAUTHORIZED);
            httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + this.realm + "\"");
            return httpResponse;
        }

        // Parse the "Authorization:" header.
        String userName;
        char[] password;
        {
            if (!s.startsWith("Basic ")) {
                return HttpResponse.response(Status.BAD_REQUEST, "Unexpected authentication scheme");
            }

            String userPass;
            try {
                userPass = new String(Base64.base64ToByteArray(s.substring(6)));
            } catch (Exception e) {
                return HttpResponse.response(Status.BAD_REQUEST, "BASE64 encoding error: " + e);
            }

            int idx = userPass.indexOf(':');
            if (idx == -1) return HttpResponse.response(Status.BAD_REQUEST, "Basic-credentials lack a colon");
            userName = userPass.substring(0, idx);
            password = userPass.substring(idx + 1).toCharArray();
        }

        boolean authenticated = this.userNamePasswordPredicate.evaluate(httpRequest, userName, password);

        if (!authenticated) {

            // Authentication failed.
            HttpResponse httpResponse = HttpResponse.response(Status.UNAUTHORIZED, "Invalid user name or password");
            httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + this.realm + "\"");
            return httpResponse;
        }

        // Authentication successful; return NULL to pass control to the next servlett.
        return null;
    }
}
