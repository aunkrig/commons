
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.security.DestroyableProperties;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link PasswordAuthenticationStore}s.
 */
public final
class PasswordAuthenticationStores {

    private PasswordAuthenticationStores() {}

    /**
     * Creates and returns a {@link PasswordAuthenticationStore} which uses a given {@link DestroyableProperties} object for
     * persistent storage.
     */
    public static PasswordAuthenticationStore
    propertiesPasswordAuthenticationStore(final DestroyableProperties delegate) {

        return new PasswordAuthenticationStore() {

            @Override @Nullable public String
            getUserName(String key) {
                char[] ca = delegate.getProperty(key + ".userName");
                return ca == null ? null : new String(ca);
            }

            @Override @Nullable public char[]
            getPassword(String key, String userName) { return delegate.getProperty(key + ".password"); }


            @Override public void
            put(String key, String userName) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                DestroyableProperties sps = delegate;

                sps.setProperty(userNamePropertyName, userName.toCharArray());
                sps.removeProperty(passwordPropertyName);

                sps.store();
            }

            @Override public void
            put(String key, String userName, char[] password) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                DestroyableProperties sps = delegate;

                sps.setProperty(userNamePropertyName, userName.toCharArray());
                sps.setProperty(passwordPropertyName, password); // <= Clears "password" array.

                sps.store();
            }

            @Override public void
            remove(String key) throws IOException {

                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";

                DestroyableProperties sps = delegate;

                sps.removeProperty(userNamePropertyName);
                sps.removeProperty(passwordPropertyName);

                sps.store();
            }

            @Override public void
            destroy() throws DestroyFailedException { delegate.destroy(); }

            @Override public boolean isDestroyed() { return delegate.isDestroyed(); }
        };
    }

    /**
     * Creates and returns a {@link PasswordAuthenticationStore} that forwards all operations to the <var>delegate</var>,
     * except that it encrypts and decrypts passwords on-the-fly. Uses the user name as the "salt" for encryption/
     * decryption.
     */
    public static PasswordAuthenticationStore
    encryptPasswords(SecretKey secretKey, final PasswordAuthenticationStore delegate) {

        final Cryptor c = Cryptors.addChecksum(Cryptors.fromSecretKey(secretKey));

        return new PasswordAuthenticationStore() {

            @Override @Nullable public String
            getUserName(String key) { return delegate.getUserName(key); }

            @Override @Nullable public char[]
            getPassword(String key, String userName) {
                char[] password = delegate.getPassword(key, userName);
                try {
                    return password == null ? null : Decryptors.decrypt(
                        c,                   // decryptor
                        MD5.of(userName),    // salt
                        new String(password) // subject
                    );
                } catch (WrongKeyException wke) {
                    return null;
                } catch (SaltException e) {
                    return null;
                }
            }

            @Override public void
            put(String key, String userName) throws IOException { delegate.put(key, userName); }

            @Override public void
            put(String key, String userName, char[] password) throws IOException {
                delegate.put(key, userName, Encryptors.encrypt(c, MD5.of(userName), password).toCharArray());
            }

            @Override public void
            remove(String key) throws IOException { delegate.remove(key); }

            @Override public void
            destroy() throws DestroyFailedException {
                c.destroy();
                delegate.isDestroyed();
            }

            @Override public boolean
            isDestroyed() { return c.isDestroyed() && delegate.isDestroyed(); }
        };
    }

    /**
     * Creates and returns a {@link DestroyableProperties} object which uses a properties file as its persistent store.
     */
    public static DestroyableProperties
    propertiesFileDestroyableProperties(final File propertiesFile, final String comments) throws IOException {

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

        return new DestroyableProperties() {

            private boolean dirty;
            private boolean destroyed;

            @Override public synchronized void
            store() throws IOException {
                if (this.destroyed) throw new IllegalStateException();

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

                if (origFile.exists()) PasswordAuthenticationStores.delete(origFile);

                if (propertiesFile.exists()) PasswordAuthenticationStores.rename(propertiesFile, origFile);

                PasswordAuthenticationStores.rename(newFile, propertiesFile);

                this.dirty = false;
            }

            @Override public int
            size() {
                if (this.destroyed) throw new IllegalStateException();

                return properties.size();
            }

            @Override public synchronized void
            setProperty(String key, char[] value) {
                if (this.destroyed) throw new IllegalStateException();

                Object previous = properties.setProperty(key, new String(value));
                this.dirty |= !ObjectUtil.equals(value, previous);
            }

            @Override public void
            removeProperty(String name) {
                if (this.destroyed) throw new IllegalStateException();

                Object previous = properties.remove(name);
                this.dirty |= previous != null;
            }

            @Override public void
            putAll(Map<? extends String, ? extends CharSequence> t) {
                if (this.destroyed) throw new IllegalStateException();

                for (Entry<? extends String, ? extends CharSequence> e : t.entrySet()) {
                    String       key   = e.getKey();
                    CharSequence value = e.getValue();

                    properties.put(key, value.toString());
                }
            }

            @Override public void
            put(String name, CharSequence value) {
                if (this.destroyed) throw new IllegalStateException();

                properties.put(name, value.toString());
            }

            @Override public Set<String>
            propertyNames() {
                if (this.destroyed) throw new IllegalStateException();

                return properties.stringPropertyNames();
            }

            @Override public boolean
            isEmpty() {
                if (this.destroyed) throw new IllegalStateException();

                return properties.isEmpty();
            }

            @Override @Nullable public char[]
            getProperty(String key) {
                if (this.destroyed) throw new IllegalStateException();

                String result = properties.getProperty(key);
                return result == null ? null : result.toCharArray();
            }

            @Override public boolean
            containsName(String name) {
                if (this.destroyed) throw new IllegalStateException();

                return properties.containsKey(name);
            }

            @Override public void
            clear() {
                if (this.destroyed) throw new IllegalStateException();

                properties.clear();
            }

            @Override public void
            destroy() { this.destroyed = true; }

            @Override public boolean
            isDestroyed() { return this.destroyed; }
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
}
