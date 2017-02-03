
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import de.unkrig.commons.net.X509Util;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility functionality in the context of JVM "key stores".
 */
public final
class KeyStores {

    private KeyStores() {}

    /**
     * Loads and returns the given <var>keyStoreFile</var> (or the JVM's default keystore file).
     */
    public static KeyStore
    loadKeyStore(@Nullable File keyStoreFile, @Nullable char[] keyStorePassword)
    throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        InputStream is = new FileInputStream(
            keyStoreFile != null
            ? keyStoreFile
            : new File(System.getProperty("java.home") + "/lib/security/cacerts")
        );
        try {
            keyStore.load(is, keyStorePassword);
        } finally {
            is.close();
        }

        return keyStore;
    }

    /**
     * Creates and returns an {@link SSLContext} from the <var>keyStore</var> and the <var>serverAlias</var>.
     */
    public static SSLContext
    getSslContext(KeyStore keyStore, char[] keyStorePassword, @Nullable String serverAlias)
    throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {

        KeyManager[] keyManagers;
        {
            KeyManagerFactory
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(keyStore, keyStorePassword);

            keyManagers = keyManagerFactory.getKeyManagers();
        }

        TrustManager[] trustManagers;
        {
            TrustManagerFactory
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);

            trustManagers = trustManagerFactory.getTrustManagers();
        }

        if (serverAlias != null) {
            keyManagers = new KeyManager[] {
                X509Util.selectServerAlias((X509KeyManager) keyManagers[0], serverAlias)
            };
        }

        SSLContext sslContext = SSLContext.getInstance("SSL"); // Both "TLS" and "SSL" work.
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }
}
