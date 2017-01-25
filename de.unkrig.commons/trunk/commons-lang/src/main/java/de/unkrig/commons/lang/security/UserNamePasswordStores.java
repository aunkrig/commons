
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
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link UserNamePasswordStore}s.
 */
public final
class UserNamePasswordStores {

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

            @Override @Nullable public DestroyableString
            getPassword(String key, String userName) {  return delegate.getProperty(key + ".password"); }


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
    encryptPasswords(
        File                        keyStoreFile,
        char[]                      keyStorePassword,
        String                      keyAlias,
        char[]                      keyProtectionPassword,
        final UserNamePasswordStore delegate
    ) throws GeneralSecurityException, IOException {

        final EncryptorDecryptor
        ed = EncryptorDecryptors.keyStoreBased(
            keyStoreFile,
            keyStorePassword,
            keyAlias,
            keyProtectionPassword
        );

        return new UserNamePasswordStore() {

            @Override @Nullable public String
            getUserName(String key) { return delegate.getUserName(key); }

            @Override @Nullable public DestroyableString
            getPassword(String key, String userName) {
                DestroyableString password = delegate.getPassword(key, userName);
                return password == null ? null : EncryptorDecryptors.decrypt(
                    ed,                                     // ed
                    UserNamePasswordStores.md5Of(userName), // salt
                    password                                // subject
                );
            }

            @Override public void
            put(String key, String userName) throws IOException { delegate.put(key, userName); }

            @Override public void
            put(String key, String userName, CharSequence password) throws IOException {
                delegate.put(key, userName, EncryptorDecryptors.encrypt(ed, UserNamePasswordStores.md5Of(userName), password));
            }

            @Override public void
            remove(String key) throws IOException { delegate.remove(key);}
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

            @Override @Nullable public DestroyableString
            getProperty(String key) {
                String result = properties.getProperty(key);
                return result == null ? null : new DestroyableString(result);
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
    toString(@Nullable DestroyableString subject) { return subject == null ? null : new String(subject.toCharArray()); }

    @Nullable private static String
    toString(@Nullable CharSequence subject) {
        return (
            subject == null                 ? null                                               :
            subject instanceof DestroyableString ? new String(((DestroyableString) subject).toCharArray()) :
            subject.toString()
        );
    }

    private static byte[]
    md5Of(String subject) {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        }

        return md.digest(subject.getBytes(Charset.forName("UTF-8")));
    }
}
