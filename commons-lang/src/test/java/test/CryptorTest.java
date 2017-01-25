
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

package test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.lang.crypto.Cryptor;
import de.unkrig.commons.lang.crypto.Cryptors;
import de.unkrig.commons.lang.crypto.Decryptor;
import de.unkrig.commons.lang.crypto.Decryptors;
import de.unkrig.commons.lang.crypto.Encryptor;
import de.unkrig.commons.lang.crypto.Encryptors;
import de.unkrig.commons.lang.crypto.SaltException;
import de.unkrig.commons.lang.crypto.WrongKeyException;
import de.unkrig.commons.lang.security.DestroyableString;

public class CryptorTest {

    private static final File   KEY_STORE_FILE          = new File(CryptorTest.class.getName() + "-keyStore");
    private static final char[] KEY_STORE_PASSWORD      = new char[] { 'x', 'y', 'z' };
    private static final String KEY_ALIAS               = "alias";
    private static final char[] KEY_PROTECTION_PASSWORD = new char[] { 'A', 'B', 'C' };

    @Before public void
    setUp() {
        if (CryptorTest.KEY_STORE_FILE.exists()) {
            Assert.assertTrue(CryptorTest.KEY_STORE_FILE.delete());
        }
    }

    @Test public void
    testByteArrays() throws WrongKeyException, GeneralSecurityException, IOException {

        SecretKey secretKey = Cryptors.adHocSecretKey(
            CryptorTest.KEY_STORE_FILE,
            CryptorTest.KEY_STORE_PASSWORD,
            CryptorTest.KEY_ALIAS,
            CryptorTest.KEY_PROTECTION_PASSWORD
        );

        Cryptor c = Cryptors.fromSecretKey(secretKey);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = c.encrypt(Arrays.copyOf(original, original.length));

        byte[] decrypted = c.decrypt(encrypted);

        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testAddChecksum() throws WrongKeyException, GeneralSecurityException, IOException {

        SecretKey secretKey = Cryptors.adHocSecretKey(
            CryptorTest.KEY_STORE_FILE,
            CryptorTest.KEY_STORE_PASSWORD,
            CryptorTest.KEY_ALIAS,
            CryptorTest.KEY_PROTECTION_PASSWORD
        );

        Cryptor c = Cryptors.fromSecretKey(secretKey);

        c = Cryptors.addChecksum(c);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = c.encrypt(Arrays.copyOf(original, original.length));

        byte[] decrypted = c.decrypt(encrypted);

        Assert.assertArrayEquals(original, decrypted);
    }

    @Test(expected = WrongKeyException.class) public void
    testWrongSecretKey() throws GeneralSecurityException, WrongKeyException {

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

        SecretKey secretKey1 = keyGenerator.generateKey();
        SecretKey secretKey2 = keyGenerator.generateKey();

        Encryptor e = Encryptors.fromKey(secretKey1);
        Decryptor d = Decryptors.fromKey(secretKey2);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = e.encrypt(Arrays.copyOf(original, original.length));

        d.decrypt(encrypted);
    }

    @Test public void
    testTwoEncodesDecoders() throws GeneralSecurityException, IOException, WrongKeyException {

        SecretKey secretKey = Cryptors.adHocSecretKey(
            CryptorTest.KEY_STORE_FILE,
            CryptorTest.KEY_STORE_PASSWORD,
            CryptorTest.KEY_ALIAS,
            CryptorTest.KEY_PROTECTION_PASSWORD
        );

        Encryptor e = Encryptors.fromKey(secretKey);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };
        byte[] encrypted = e.encrypt(Arrays.copyOf(original, original.length));

        Decryptor d = Decryptors.fromKey(secretKey);

        byte[] decrypted = d.decrypt(encrypted);
        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testStrings() throws GeneralSecurityException, IOException, WrongKeyException {

        SecretKey secretKey = Cryptors.adHocSecretKey(
            CryptorTest.KEY_STORE_FILE,
            CryptorTest.KEY_STORE_PASSWORD,
            CryptorTest.KEY_ALIAS,
            CryptorTest.KEY_PROTECTION_PASSWORD
        );

        String original = "The quick brown fox jumps over the lazy dog";

        Cryptor c = Cryptors.fromSecretKey(secretKey);

        String encrypted = Encryptors.encrypt(c, original);

        Assert.assertNotEquals(original, encrypted);

        DestroyableString decrypted = Decryptors.decrypt(c, new DestroyableString(encrypted));
        Assert.assertEquals(original, new String(decrypted.toCharArray()));
    }

    @Test public void
    testStringsWithSalt() throws GeneralSecurityException, IOException, WrongKeyException, SaltException {

        SecretKey secretKey = Cryptors.adHocSecretKey(
            CryptorTest.KEY_STORE_FILE,
            CryptorTest.KEY_STORE_PASSWORD,
            CryptorTest.KEY_ALIAS,
            CryptorTest.KEY_PROTECTION_PASSWORD
        );

        Cryptor c = Cryptors.fromSecretKey(secretKey);

        String original = "The quick brown fox jumps over the lazy dog";
        byte[] salt = { 1, 2, 3, 4 };

        String encrypted = Encryptors.encrypt(c, salt, original);

        Assert.assertNotEquals(original, encrypted);

        DestroyableString decrypted = Decryptors.decrypt(c, salt, new DestroyableString(encrypted));

        Assert.assertEquals(original, new String(decrypted.toCharArray()));
    }
}
