
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.lang.security.EncryptorDecryptor;
import de.unkrig.commons.lang.security.EncryptorDecryptors;
import de.unkrig.commons.lang.security.DestroyableString;

public class EncryptorDecryptorTest {

    private static final File   KEY_STORE_FILE          = new File(EncryptorDecryptorTest.class.getName() + "-keyStore");
    private static final char[] KEY_STORE_PASSWORD      = new char[] { 'x', 'y', 'z' };
    private static final String KEY_ALIAS               = "alias";
    private static final char[] KEY_PROTECTION_PASSWORD = new char[] { 'A', 'B', 'C' };

    @Before public void
    setUp() {
        if (EncryptorDecryptorTest.KEY_STORE_FILE.exists()) {
            Assert.assertTrue(EncryptorDecryptorTest.KEY_STORE_FILE.delete());
        }
    }

    @Test public void
    testByteArrays() throws GeneralSecurityException, IOException {

        EncryptorDecryptor ed = EncryptorDecryptors.keyStoreBased(
            EncryptorDecryptorTest.KEY_STORE_FILE,
            EncryptorDecryptorTest.KEY_STORE_PASSWORD,
            EncryptorDecryptorTest.KEY_ALIAS,
            EncryptorDecryptorTest.KEY_PROTECTION_PASSWORD
        );

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = ed.encrypt(Arrays.copyOf(original, original.length));

        byte[] decrypted = ed.decrypt(encrypted);

        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testTwoEncodesDecoders() throws GeneralSecurityException, IOException {

        EncryptorDecryptor ed1 = EncryptorDecryptors.keyStoreBased(
            EncryptorDecryptorTest.KEY_STORE_FILE,
            EncryptorDecryptorTest.KEY_STORE_PASSWORD,
            EncryptorDecryptorTest.KEY_ALIAS,
            EncryptorDecryptorTest.KEY_PROTECTION_PASSWORD
        );

        byte[] original = { 3, 99, 3, -23, 5, 99, 99 };

        byte[] encrypted = ed1.encrypt(Arrays.copyOf(original, original.length));

        EncryptorDecryptor ed2 = EncryptorDecryptors.keyStoreBased(
            EncryptorDecryptorTest.KEY_STORE_FILE,
            EncryptorDecryptorTest.KEY_STORE_PASSWORD,
            EncryptorDecryptorTest.KEY_ALIAS,
            EncryptorDecryptorTest.KEY_PROTECTION_PASSWORD
        );

        byte[] decrypted = ed2.decrypt(encrypted);

        Assert.assertArrayEquals(original, decrypted);
    }

    @Test public void
    testStrings() throws GeneralSecurityException, IOException {

        EncryptorDecryptor ed = EncryptorDecryptors.keyStoreBased(
            EncryptorDecryptorTest.KEY_STORE_FILE,
            EncryptorDecryptorTest.KEY_STORE_PASSWORD,
            EncryptorDecryptorTest.KEY_ALIAS,
            EncryptorDecryptorTest.KEY_PROTECTION_PASSWORD
        );

        String original = "The quick brown fox jumps over the lazy dog";

        String encrypted = EncryptorDecryptors.encrypt(ed, original);
        Assert.assertNotEquals(original, encrypted);
        DestroyableString decrypted = EncryptorDecryptors.decrypt(ed, new DestroyableString(encrypted));
        Assert.assertEquals(original, new String(decrypted.toCharArray()));
    }

    @Test public void
    testStringsWithSalt() throws GeneralSecurityException, IOException {

        EncryptorDecryptor ed = EncryptorDecryptors.keyStoreBased(
            EncryptorDecryptorTest.KEY_STORE_FILE,
            EncryptorDecryptorTest.KEY_STORE_PASSWORD,
            EncryptorDecryptorTest.KEY_ALIAS,
            EncryptorDecryptorTest.KEY_PROTECTION_PASSWORD
        );

        String original = "The quick brown fox jumps over the lazy dog";
        byte[] salt = { 1, 2, 3, 4 };

        String encrypted = EncryptorDecryptors.encrypt(ed, salt, original);
        Assert.assertNotEquals(original, encrypted);
        DestroyableString decrypted = EncryptorDecryptors.decrypt(ed, salt, new DestroyableString(encrypted));
        Assert.assertEquals(original, new String(decrypted.toCharArray()));
    }
}
