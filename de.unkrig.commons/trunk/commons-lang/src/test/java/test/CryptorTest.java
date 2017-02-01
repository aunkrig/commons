
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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.crypto.Cryptor;
import de.unkrig.commons.lang.crypto.Cryptors;
import de.unkrig.commons.lang.crypto.Decryptor;
import de.unkrig.commons.lang.crypto.Decryptors;
import de.unkrig.commons.lang.crypto.Encryptor;
import de.unkrig.commons.lang.crypto.Encryptors;
import de.unkrig.commons.lang.crypto.SaltException;
import de.unkrig.commons.lang.crypto.WrongKeyException;

public class CryptorTest {

    private static final SecretKey SECRET_KEY;
    static {
        try {
            SECRET_KEY = KeyGenerator.getInstance("AES").generateKey();
        } catch (NoSuchAlgorithmException nsae) {
            throw new ExceptionInInitializerError(nsae);
        }
    }

    @Test public void
    testByteArrays() throws WrongKeyException {

        Cryptor c = Cryptors.fromSecretKey(CryptorTest.SECRET_KEY);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = c.encrypt(Arrays.copyOf(original, original.length));

        byte[] decrypted = c.decrypt(encrypted);

        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testAddChecksum() throws WrongKeyException {

        Cryptor c = Cryptors.fromSecretKey(CryptorTest.SECRET_KEY);

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
    testTwoEncodesDecoders() throws WrongKeyException {

        Encryptor e = Encryptors.fromKey(CryptorTest.SECRET_KEY);

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };
        byte[] encrypted = e.encrypt(Arrays.copyOf(original, original.length));

        Decryptor d = Decryptors.fromKey(CryptorTest.SECRET_KEY);

        byte[] decrypted = d.decrypt(encrypted);
        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testStrings() throws WrongKeyException {

        String original = "The quick brown fox jumps over the lazy dog";

        Cryptor c = Cryptors.fromSecretKey(CryptorTest.SECRET_KEY);

        String encrypted = Encryptors.encrypt(c, original.toCharArray());

        Assert.assertNotEquals(original, encrypted);

        char[] decrypted = Decryptors.decrypt(c, encrypted);
        Assert.assertEquals(original, new String(decrypted));
    }

    @Test public void
    testStringsWithSalt() throws WrongKeyException, SaltException {

        Cryptor c = Cryptors.fromSecretKey(CryptorTest.SECRET_KEY);

        String original = "The quick brown fox jumps over the lazy dog";
        byte[] salt = { 1, 2, 3, 4 };

        String encrypted = Encryptors.encrypt(c, salt, original.toCharArray());
        Assert.assertNotEquals(original, encrypted);

        char[] decrypted = Decryptors.decrypt(c, salt, encrypted);
        Assert.assertEquals(original, new String(decrypted));
    }

    @Test(expected = SaltException.class) public void
    testStringsWithWrongSalt() throws WrongKeyException, SaltException {

        Cryptor c = Cryptors.fromSecretKey(CryptorTest.SECRET_KEY);

        String original = "The quick brown fox jumps over the lazy dog";
        byte[] salt = { 1, 2, 3, 4 };

        String encrypted = Encryptors.encrypt(c, salt, original.toCharArray());
        Assert.assertNotEquals(original, encrypted);

        salt[0]++;
        Decryptors.decrypt(c, salt, encrypted);
    }
}
