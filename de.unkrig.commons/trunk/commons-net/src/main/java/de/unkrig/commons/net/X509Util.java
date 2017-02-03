
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

package de.unkrig.commons.net;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Various utility methods related to X509.
 */
public final
class X509Util {

    private
    X509Util() {}

    /**
     * @return An {@link X509KeyManager} that forwards all method calls to a delegate, except that {@link
     * X509KeyManager#chooseServerAlias(String, Principal[], Socket)} returns <var>serverAlias</var>.
     */
    @NotNullByDefault(false) public static X509KeyManager
    selectServerAlias(final X509KeyManager delegate, final String serverAlias) {
        return new X509KeyManager() {

            @Override public String
            chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                return delegate.chooseClientAlias(keyType, issuers, socket);
            }

            @Override public String[]
            getClientAliases(String keyType, Principal[] issues) {
                return delegate.getClientAliases(keyType, issues);
            }

            @Override public String[]
            getServerAliases(String keyType, Principal[] issues) {
                return delegate.getServerAliases(keyType, issues);
            }

            @Override public String
            chooseServerAlias(String keyType, Principal[] issues, Socket socket) {
                return serverAlias;
            }

            @Override public X509Certificate[]
            getCertificateChain(String alias) {
                return delegate.getCertificateChain(alias);
            }

            @Override public PrivateKey
            getPrivateKey(String alias) {
                return delegate.getPrivateKey(alias);
            }
        };
    }
}
