
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

package de.unkrig.commons.lang.security;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.java6.Base64;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link Encryptor}s.
 */
public final
class Encryptors {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Encryptors() {}

    /**
     * Creates and returns an {@link Encryptor} which uses the given key for encryptionWhen this method returns, the
     * key may safely be destroyed.
     */
    public static Encryptor
    fromKey(final Key key) {

        String algorithm = key.getAlgorithm();

        final Cipher cipher;
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (GeneralSecurityException gse) {
            throw new AssertionError(gse);
        }

        return new Encryptor() {

            private boolean destroyed;

            @Override public byte[]
            encrypt(byte[] unencrypted) {
                if (this.destroyed) throw new IllegalStateException();

                try {

                    synchronized (cipher) {
                        cipher.init(Cipher.ENCRYPT_MODE, key);
                        return cipher.doFinal(unencrypted);
                    }
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                } finally {
                    Arrays.fill(unencrypted, (byte) 0);
                }
            }

            @Override public void
            destroy() throws DestroyFailedException {
                if (cipher instanceof Destroyable) ((Destroyable) cipher).destroy();
                this.destroyed = true;
            }

            @Override public boolean
            isDestroyed() { return this.destroyed; }
        };
    }

    /**
     * Wraps the <var>delegate</var> such that any change in the encrypted data is guaranteed to be detected and raised
     * as a {@link WrongKeyException}.
     */
    public static Encryptor
    addChecksum(final Encryptor delegate) {

        return new Encryptor() {

            @Override public byte[]
            encrypt(byte[] unencrypted) {

                byte[] md5 = MD5.of(unencrypted);
                assert md5.length == 16;

                byte[] tmp = unencrypted;
                unencrypted = Arrays.copyOf(unencrypted, unencrypted.length + 16);
                System.arraycopy(md5, 0, unencrypted, unencrypted.length - 16, 16);
                Arrays.fill(tmp, (byte) 0);

                return delegate.encrypt(unencrypted);
            }

            @Override public void
            destroy() throws DestroyFailedException { delegate.destroy(); }

            @Override public boolean
            isDestroyed() { return delegate.isDestroyed(); }
        };
    }

    /**
     * Encodes the <var>subject</var> as UTF-8, encrypts the resulting bytes, and BASE64-encodes them.
     */
    public static String
    encrypt(Encryptor encryptor, CharSequence subject) { return Encryptors.encrypt(encryptor, null, subject); }

    /**
     * Encodes the <var>subject</var> as UTF-8, (optionally) prepends it with the salt, encrypts the resulting bytes,
     * and BASE64-encodes them.
     */
    public static String
    encrypt(Encryptor encryptor, @Nullable byte[] salt, CharSequence subject) {

        DestroyableString ss = new DestroyableString(subject);
        try {

            byte[] unencryptedBytes = ss.getBytes("UTF-8");

            if (salt != null && salt.length > 0) {
                byte[] tmp = unencryptedBytes;
                unencryptedBytes = Arrays.copyOf(salt, salt.length + tmp.length);
                System.arraycopy(tmp, 0, unencryptedBytes, salt.length, tmp.length);
                Arrays.fill(tmp, (byte) 0);
            }

            byte[] encryptedBytes = encryptor.encrypt(unencryptedBytes);

            return Base64.encode(encryptedBytes);
        } finally {
            ss.destroy();
        }
    }
}
