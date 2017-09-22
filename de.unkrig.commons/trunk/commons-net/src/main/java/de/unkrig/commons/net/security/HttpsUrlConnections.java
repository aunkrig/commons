
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

package de.unkrig.commons.net.security;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Utility functionality related to {@link HttpsURLConnection}s.
 */
public final
class HttpsUrlConnections {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private HttpsUrlConnections() {}

    /**
     * Disables validation of client certificates, server certificates, certificate issuers and hostnames for
     * {@link HttpsURLConnection}s.
     * <p>
     *   {@link SSLHandshakeException}s like this will no longer be thrown:
     * </p>
     * <p>
     *   javax.net.ssl.SSLHandshakeException:<br />
     *   sun.security.validator.ValidatorException: PKIX path building failed:<br />
     *   sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to
     *   requested target
     * </p>
     */
    public static void
    disableSslCertificateValidation() throws GeneralSecurityException {

        SSLContext sslc;
        {
            sslc = SSLContext.getInstance("TLS");

            TrustManager[] trustManagerArray = { new X509TrustManager() {

                @Override @NotNullByDefault(false) public void
                checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override @NotNullByDefault(false) public void
                checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override @NotNullByDefault(false) public X509Certificate[]
                getAcceptedIssuers() { return new X509Certificate[0]; }
            } };

            sslc.init(null, trustManagerArray, null);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory());

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            @Override @NotNullByDefault(false) public boolean
            verify(String hostname, SSLSession session) { return true; }
        });
    }

}
