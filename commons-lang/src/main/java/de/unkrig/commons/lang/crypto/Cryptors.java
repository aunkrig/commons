
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.lang.crypto;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * Utility methods related to {@link Cryptor}s.
 */
public final
class Cryptors {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Cryptors() {}

    /**
     * Creates an encryptor-decryptor pair for a given <var>secretKey</var>.
     */
    public static Cryptor
    fromSecretKey(SecretKey secretKey) {

        return Cryptors.from(
            Encryptors.fromKey(secretKey),
            Decryptors.fromKey(secretKey)
        );
    }

    /**
     * Combines the given <var>encryptor</var> and <var>decryptor</var> in one {@link Cryptor} object.
     */
    public static Cryptor
    from(final Encryptor encryptor, final Decryptor decryptor) {

        return new Cryptor() {

            @Override public byte[]
            encrypt(byte[] unencrypted) { return encryptor.encrypt(unencrypted); }

            @Override public byte[]
            decrypt(byte[] encrypted) throws WrongKeyException { return decryptor.decrypt(encrypted); }

            @Override public void
            destroy() throws DestroyFailedException {
                encryptor.destroy();
                decryptor.destroy();
            }

            @Override public boolean
            isDestroyed() { return encryptor.isDestroyed() && decryptor.isDestroyed(); }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that any change in the encrypted data is guaranteed to be detected and raised
     * as a {@link WrongKeyException}.
     *
     * @see Encryptors#addChecksum(Encryptor)
     * @see Decryptors#addChecksum(Decryptor)
     */
    public static Cryptor
    addChecksum(Cryptor delegate) {

        return Cryptors.from(
            Encryptors.addChecksum(delegate),
            Decryptors.addChecksum(delegate)
        );
    }
}
