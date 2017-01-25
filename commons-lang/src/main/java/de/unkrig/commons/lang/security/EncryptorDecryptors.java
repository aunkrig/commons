
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import de.unkrig.commons.nullanalysis.Nullable;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Utility methods related to {@link EncryptorDecryptor}s.
 */
public final
class EncryptorDecryptors {

    // "If you use a block-chaining mode like CBC, you need to provide an IvParameterSpec to the Cipher as well."
    // Aha.
    // See : http://stackoverflow.com/questions/6669181/why-does-my-aes-encryption-throws-an-invalidkeyexception
    private static final byte[] IV = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private EncryptorDecryptors() {}

    /**
     * Loads a {@link SecretKey} from the <var>keyStoreFile</var>, or, if that file does not exist, generates a new
     * {@link SecretKey} and stores it in the <var>keyStoreFile</var>).
     *
     * @param keyStorePassword      See {@link KeyStore#load(InputStream, char[])}
     * @param keyProtectionPassword See {@link KeyStore#getKey(String, char[])
     */
    public static SecretKey
    adHocSecretKey(
        File             keyStoreFile,
        @Nullable char[] keyStorePassword,
        String           keyAlias,
        char[]           keyProtectionPassword
    ) throws GeneralSecurityException, IOException {

        // Notice: The "KeyStore.getDefault()" cannot store SecretKeys, so we must use "JCEKS".
        KeyStore ks = KeyStore.getInstance("JCEKS");

        boolean keystoreDirty = false;
        if (keyStoreFile.exists()) {

            // Load existing keystore file.
            InputStream is = new FileInputStream(keyStoreFile);
            try {

                ks.load(is, keyStorePassword);
                is.close();
            } catch (IOException ioe) {
                if (ioe.getCause() instanceof UnrecoverableKeyException) {

                    // Wrong key store password.
                    throw new AssertionError(ioe); // TODO Better handling required - query password interactively!?
                }
            } finally {
                try { is.close(); } catch (Exception e) {}
            }
        } else {

            // The keystore file does not yet exist; create an empty keystore.
            ks.load(null, keyStorePassword);
            keystoreDirty = true;
        }

        SecretKey secretKey = (SecretKey) ks.getKey(keyAlias, keyProtectionPassword);
        if (secretKey == null) {

            // Key does not exist in keystore; generate a new one and put it into the keystore.
            secretKey = KeyGenerator.getInstance("AES").generateKey();
            ks.setKeyEntry(keyAlias, secretKey, keyProtectionPassword, null);
            keystoreDirty = true;
        }

        // Store the keystore in the file, if necessary.
        if (keystoreDirty) {

            OutputStream os = new FileOutputStream(keyStoreFile);
            try {

                ks.store(os, keyStorePassword);
                os.close();
            } finally {
                try { os.close(); } catch (Exception e) {}
            }
        }

        return secretKey;
    }

    /**
     * Creates and returns aen {@link EncryptorDecryptor} which uses the given keys for encryption resp. decryption.
     */
    public static EncryptorDecryptor
    fromKeys(final Key encryptionKey, final Key decryptionKey) throws NoSuchAlgorithmException, NoSuchPaddingException {

        return new EncryptorDecryptor() {

            final Cipher encryptionCipher = Cipher.getInstance(encryptionKey.getAlgorithm());
            final Cipher decryptionCipher = Cipher.getInstance(decryptionKey.getAlgorithm());

            @Override public byte[]
            encrypt(byte[] unencrypted) {
                try {
                    synchronized (this.encryptionCipher) {
                        this.encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
                        return this.encryptionCipher.doFinal(unencrypted);
                    }
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                } finally {
                    Arrays.fill(unencrypted, (byte) 0);
                }
            }

            @Override public byte[]
            decrypt(byte[] encrypted) {
                try {
                    synchronized (this.decryptionCipher) {
                        this.decryptionCipher.init(Cipher.DECRYPT_MODE, decryptionKey);
                        return this.decryptionCipher.doFinal(encrypted);
                    }
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                } finally {
                    Arrays.fill(encrypted, (byte) 0);
                }
            }
        };
    }

    /**
     * Encodes the <var>subject</var> as UTF-8, encrypts the resulting bytes, and BASE64-encodes them.
     */
    public static String
    encrypt(EncryptorDecryptor ed, CharSequence subject) { return EncryptorDecryptors.encrypt(ed, null, subject); }

    public static String
    encrypt(EncryptorDecryptor ed, @Nullable byte[] salt, CharSequence subject) {

        DestroyableString ss = new DestroyableString(subject);
        try {

            byte[] unencryptedBytes = ss.getBytes("UTF-8");

            if (salt != null && salt.length > 0) {
                byte[] tmp = unencryptedBytes;
                unencryptedBytes = Arrays.copyOf(salt, salt.length + tmp.length);
                System.arraycopy(tmp, 0, unencryptedBytes, salt.length, tmp.length);
                Arrays.fill(tmp, (byte) 0);
            }

            byte[] encryptedBytes = ed.encrypt(unencryptedBytes);

            return EncryptorDecryptors.base64Encode(encryptedBytes);
        } finally {
            ss.destroy();
        }
    }

    /**
     * BASE64-decodes the <var>subject</var>, decrypts the resulting bytes, and decodes them as UTF-8.
     * <p>
     *   Closes the <var>subject</var>; the caller is responsible for closing the returned secure string.
     * </p>
     */
    public static DestroyableString
    decrypt(EncryptorDecryptor ed, DestroyableString subject) { return EncryptorDecryptors.decrypt(ed, null, subject); }

    public static DestroyableString
    decrypt(EncryptorDecryptor ed, @Nullable byte[] salt, DestroyableString subject) {

        try {
            String encryptedString = new String(subject.toCharArray());

            byte[] encryptedBytes = EncryptorDecryptors.base64Decode(encryptedString);

            byte[] decryptedBytes = ed.decrypt(encryptedBytes);

            if (salt != null && salt.length > 0) {

                if (
                    decryptedBytes.length < salt.length
                    || !EncryptorDecryptors.arrayEquals(decryptedBytes, 0, salt, 0, salt.length)
                ) throw new AssertionError("Salt mismatch");

                byte[] tmp = decryptedBytes;
                decryptedBytes = Arrays.copyOfRange(decryptedBytes, salt.length, decryptedBytes.length);
                Arrays.fill(tmp, (byte) 0);
            }

            return new DestroyableString(decryptedBytes, "UTF8");
        } finally {
            subject.destroy();
        }
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

    private static byte[]
    base64Decode(String subject) throws AssertionError {

        try {

            @SuppressWarnings("restriction") byte[] tmp = new BASE64Decoder().decodeBuffer(subject);
            return tmp;
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    /**
     * BASE64-encodes the <var>subject</var> byte array and fills it with zeros.
     * @param subject
     * @return
     */
    private static String
    base64Encode(byte[] subject) {

        try {

            @SuppressWarnings("restriction") String tmp = new BASE64Encoder().encode(subject);

            return tmp;
        } finally {
            Arrays.fill(subject, (byte) 0);
        }
    }
}
