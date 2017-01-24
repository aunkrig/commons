
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Utility methods related to {@link UserNamePasswordStore}s.
 */
public final
class UserNamePasswordStores {

    private static final File KEYSTORE_FILE = new File(System.getProperty("user.home"), ".antology_setAuthenticator_keystore");

    // "If you use a block-chaining mode like CBC, you need to provide an IvParameterSpec to the Cipher as well."
    // http://stackoverflow.com/questions/6669181/why-does-my-aes-encryption-throws-an-invalidkeyexception
    private static final byte[] IV = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private UserNamePasswordStores() {}

    /**
     * Creates and returns a {@link UserNamePasswordStore} which uses a given {@link SecureProperties} object for
     * persistent storage.
     */
    public static UserNamePasswordStore
    propertiesUserNamePasswordStore(final SecureProperties delegate) {

        return new UserNamePasswordStore() {

            @Override @Nullable public String
            getUserName(String key) { return UserNamePasswordStores.toString(delegate.getProperty(key + ".userName")); }

            @Override @Nullable public SecureString
            getPassword(String key) {  return delegate.getProperty(key + ".password"); }


            @Override public void
            put(String key, String userName) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                SecureProperties sps = delegate;

                sps.setProperty(userNamePropertyName, userName);
                sps.removeProperty(passwordPropertyName);

                sps.store();
            }

            @Override public void
            put(String key, String userName, CharSequence password) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                SecureProperties sps = delegate;

                sps.setProperty(userNamePropertyName, userName);
                sps.setProperty(passwordPropertyName, password);

                sps.store();
            }

            @Override public void
            remove(String key) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                SecureProperties sps = delegate;

                sps.removeProperty(userNamePropertyName);
                sps.removeProperty(passwordPropertyName);

                sps.store();
            }
        };
    }

    /**
     * Creates and returns a {@link UserNamePasswordStore} that forwards all operations to the <var>delegate</var>,
     * except that it encrypts and decrypts passwords on-the-fly.
     */
    public static UserNamePasswordStore
    encryptPasswords(final UserNamePasswordStore delegate) throws GeneralSecurityException, IOException {

        char[] keystorePassword   = new char[0];
        String alias              = "setAuthenticatorKey";

        KeyStore ks = KeyStore.getInstance("JCEKS");

        boolean keystoreDirty = false;
        if (UserNamePasswordStores.KEYSTORE_FILE.exists()) {

            // Load existing keystore file.
            InputStream is = new FileInputStream(UserNamePasswordStores.KEYSTORE_FILE);
            try {

                ks.load(is, keystorePassword);
                is.close();
            } finally {
                try { is.close(); } catch (Exception e) {}
            }
        } else {

            // Keystore file does not yet exist; create an empty keystore.
            ks.load(null, keystorePassword);
            keystoreDirty = true;
        }

        SecretKey secretKey = (SecretKey) ks.getKey(alias, keystorePassword);
        if (secretKey == null) {

            // Key does not exist in keystore; generate a new one and put it into the keystore.
            secretKey = KeyGenerator.getInstance("AES").generateKey();
//            ks.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), keyStoreProtection);
            ks.setKeyEntry(alias, secretKey, keystorePassword, null);
            keystoreDirty = true;
        }

        // Store the keystore in the file, if necessary.
        if (keystoreDirty) {

            OutputStream os = new FileOutputStream(UserNamePasswordStores.KEYSTORE_FILE);
            try {

                ks.store(os, keystorePassword);
                os.close();
            } finally {
                try { os.close(); } catch (Exception e) {}
            }
        }

        final Cipher cipher;
        {
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            } catch (GeneralSecurityException gse) {
                throw new ExceptionInInitializerError(gse);
            }
        }

        final SecretKey finalSecretKey = secretKey;

        return new UserNamePasswordStore() {

            @Override @Nullable public String
            getUserName(String key) { return delegate.getUserName(key); }

            @Override @Nullable public SecureString
            getPassword(String key) { return this.decrypt(delegate.getPassword(key)); }

            @Override public void
            put(String key, String userName) throws IOException { delegate.put(key, userName); }

            @Override public void
            put(String key, String userName, CharSequence password) throws IOException {
                delegate.put(key, userName, this.encrypt(password));
            }

            @Override public void
            remove(String key) throws IOException { delegate.remove(key);}

            // =========================

            private CharSequence
            encrypt(CharSequence subject) {
                try {
                    byte[] utf8Bytes = new String(new SecureString(subject).toCharArray()).getBytes("UTF-8");

                    byte[] encryptedBytes;
                    synchronized (cipher) {
                        cipher.init(Cipher.ENCRYPT_MODE, finalSecretKey, new IvParameterSpec(UserNamePasswordStores.IV));
                        encryptedBytes = cipher.doFinal(utf8Bytes);
                    }

                    @SuppressWarnings("restriction") String encryptedString = new BASE64Encoder().encode(encryptedBytes);
                    return encryptedString;
                } catch (UnsupportedEncodingException uee) {
                    throw new AssertionError(uee);
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                }
            }

            /**
             * Closes the <var>password</var>; the caller is responsible for closing the returned secure string.
             */
            @Nullable private SecureString
            decrypt(@Nullable SecureString encryptedPassword) {

                if (encryptedPassword == null) return null;

                String encryptedString = new String(encryptedPassword.toCharArray());

                try {

                    @SuppressWarnings("restriction") byte[]
                    encryptedBytes = new BASE64Decoder().decodeBuffer(encryptedString);

                    byte[] utf8Bytes;
                    synchronized (cipher) {
                        cipher.init(Cipher.ENCRYPT_MODE, finalSecretKey, new IvParameterSpec(UserNamePasswordStores.IV));
                        utf8Bytes = cipher.doFinal(encryptedBytes);
                    }

                    return new SecureString(utf8Bytes, "UTF8");
                } catch (GeneralSecurityException gse) {
                    throw new AssertionError(gse);
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        };
    }

    /**
     * Creates and returns a {@link SecureProperties} object which uses a properties file as its persistent store.
     */
    public static SecureProperties
    propertiesFileSecureProperties(final File propertiesFile, final String comments) throws IOException {

        final Properties properties = new Properties();

        propertiesFile.createNewFile();

        InputStream is = new FileInputStream(propertiesFile);
        try {

            properties.load(is);
        } catch (FileNotFoundException fnfe) {
            ;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }

        return new SecureProperties() {

            private boolean dirty;

            @Override public synchronized void
            store() throws IOException {

                if (!this.dirty) return;

                File newFile = new File(propertiesFile.getParentFile(), "." + propertiesFile.getName() + ",new");

                OutputStream os = new FileOutputStream(newFile);
                try {

                    properties.store(os, comments);
                    os.close();
                } catch (IOException ioe) {
                    try { os.close();       } catch (Exception e) {}
                    try { newFile.delete(); } catch (Exception e) {}
                    throw ExceptionUtil.wrap("Creating temporary properties file", ioe);
                } finally {
                    try { os.close(); } catch (Exception e) {}
                }

                File origFile = new File(propertiesFile.getParentFile(), "." + propertiesFile.getName() + ",orig");

                if (origFile.exists()) UserNamePasswordStores.delete(origFile);

                if (propertiesFile.exists()) UserNamePasswordStores.rename(propertiesFile, origFile);

                UserNamePasswordStores.rename(newFile, propertiesFile);

                this.dirty = false;
            }

            @Override public int
            size() { return properties.size(); }

            @Override public synchronized void
            setProperty(String key, CharSequence value) {
                Object previous = properties.setProperty(key, UserNamePasswordStores.toString(value));
                this.dirty |= !ObjectUtil.equals(value, previous);
            }

            @Override public void
            removeProperty(String name) {
                Object previous = properties.remove(name);
                this.dirty |= previous != null;
            }

            @Override public void
            putAll(Map<? extends String, ? extends CharSequence> t) {
                for (Entry<? extends String, ? extends CharSequence> e : t.entrySet()) {
                    String       key   = e.getKey();
                    CharSequence value = e.getValue();

                    properties.put(key, value.toString());
                }
            }

            @Override public void
            put(String name, CharSequence value) { properties.put(name, value.toString()); }

            @Override public Set<String>
            propertyNames() { return properties.stringPropertyNames(); }

            @Override public boolean
            isEmpty() { return properties.isEmpty(); }

            @Override @Nullable public SecureString
            getProperty(String key) {
                String result = properties.getProperty(key);
                return result == null ? null : new SecureString(result);
            }

            @Override public boolean
            containsName(String name) { return properties.containsKey(name); }

            @Override public void
            clear() { properties.clear(); }
        };
    }

    private static void
    rename(final File oldFile, File newFile) throws IOException {

        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Unable to rename \"" + oldFile + "\" to \"" + newFile + "\"");
        }
    }

    private static void
    delete(File file) throws IOException {
        if (!file.delete()) throw new IOException("Unable to delete \"" + file + "\"");
    }

    @Nullable private static String
    toString(@Nullable SecureString subject) { return subject == null ? null : new String(subject.toCharArray()); }

    @Nullable private static String
    toString(@Nullable CharSequence subject) {
        return (
            subject == null                 ? null                                               :
            subject instanceof SecureString ? new String(((SecureString) subject).toCharArray()) :
            subject.toString()
        );
    }
}
