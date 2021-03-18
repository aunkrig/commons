
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

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.security.SecureCharsets;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link Decryptor}s.
 */
public final
class Decryptors {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Decryptors() {}

    /**
     * Creates and returns an {@link Decryptor} which uses the given keys for encryption and decryption. When
     * this method returns, the two keys may safely be destroyed.
     */
    public static Decryptor
    fromKey(final Key key) {

        String algorithm = key.getAlgorithm();

        final Cipher cipher;
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (GeneralSecurityException gse) {
            throw new AssertionError(gse);
        }

        return new Decryptor() {

            private boolean destroyed;

            @Override public byte[]
            decrypt(byte[] encrypted) throws WrongKeyException {
                if (this.destroyed) throw new IllegalStateException();

                try {
                    synchronized (cipher) {
                        cipher.init(Cipher.DECRYPT_MODE, key);

                        try {
                            return cipher.doFinal(encrypted);
                        } catch (BadPaddingException bpe) {

                            // A wrong key often causes a BadPaddingException.
                            throw new WrongKeyException();
                        }
                    }
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                } finally {
                    Arrays.fill(encrypted, (byte) 0);
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
    public static Decryptor
    addChecksum(final Decryptor delegate) {

        return new Decryptor() {

            @Override public byte[]
            decrypt(byte[] encrypted) throws WrongKeyException {

                byte[] decrypted = delegate.decrypt(encrypted);

                // Verify the checksum at the end of the decrypted data.
                if (decrypted.length < 16) {
                    Arrays.fill(decrypted, (byte) 0);
                    throw new WrongKeyException();
                }

                byte[] md5 = MD5.of(decrypted, 0, decrypted.length - 16);
                assert md5.length == 16;

                if (!Decryptors.arrayEquals(decrypted, decrypted.length - 16, md5, 0, 16)) {
                    Arrays.fill(decrypted, (byte) 0);
                    throw new WrongKeyException();
                }

                byte[] tmp = decrypted;
                decrypted = Arrays.copyOf(decrypted, decrypted.length - 16);
                Arrays.fill(tmp, (byte) 0);

                return decrypted;
            }

            @Override public void
            destroy() throws DestroyFailedException { delegate.destroy(); }

            @Override public boolean
            isDestroyed() { return delegate.isDestroyed(); }
        };
    }

    /**
     * BASE64-decodes the <var>subject</var>, decrypts the resulting bytes, and decodes them as UTF-8.
     * <p>
     *   Fills the <var>subject</var> with zeros.
     * </p>
     *
     * @throws WrongKeyException The key is wrong
     */
    public static char[]
    decrypt(Decryptor decryptor, String subject) throws WrongKeyException {

        try {
            return Decryptors.decrypt(decryptor, null, subject);
        } catch (SaltException se) {

            // Should not occur because there is no salt.
            throw new AssertionError(se);
        }
    }

    /**
     * BASE64-decodes the <var>subject</var>, decrypts the resulting bytes, (optionally) verifies and then strips the
     * <var>salt</var> prefix, and decodes the bytes as UTF-8.
     *
     * @throws WrongKeyException The key is wrong
     * @throws SaltException     After decryption, the salt verification failed
     */
    public static char[]
    decrypt(Decryptor decryptor, @Nullable byte[] salt, String subject) throws WrongKeyException, SaltException {

        byte[] encryptedBytes = Base64.getDecoder().decode(subject);

        byte[] decryptedBytes = decryptor.decrypt(encryptedBytes);

        if (salt != null && salt.length > 0) {

            if (
                decryptedBytes.length < salt.length
                || !Decryptors.arrayEquals(decryptedBytes, 0, salt, 0, salt.length)
            ) throw new SaltException();

            byte[] tmp = decryptedBytes;
            decryptedBytes = Arrays.copyOfRange(decryptedBytes, salt.length, decryptedBytes.length);
            Arrays.fill(tmp, (byte) 0);
        }

        return SecureCharsets.secureDecode(decryptedBytes, Charset.forName("UTF8"));
    }

    private static boolean
    arrayEquals(byte[] ba1, int ba1pos, byte[] ba2, int ba2pos, int length) {

        if (ba1pos + length > ba1.length) throw new IllegalArgumentException();
        if (ba2pos + length > ba2.length) throw new IllegalArgumentException();

        for (int i = 0; i < length; i++) {
            if (ba1[ba1pos + i] != ba2[ba2pos + i]) return false;
        }
        return true;
    }
}
