
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link Cryptor}s.
 */
public final
class Cryptors {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Cryptors() {}

    public static Cryptor
    fromSecretKey(SecretKey secretKey) {

        return Cryptors.from(
            Encryptors.fromKey(secretKey),
            Decryptors.fromKey(secretKey)
        );
    }

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
     * Loads a {@link SecretKey} from the <var>keyStoreFile</var>, or, if that file does not exist, generates a new
     * {@link SecretKey} and stores it in the <var>keyStoreFile</var>).
     *
     * @param keyStorePassword      See {@link KeyStore#load(InputStream, char[])}
     * @param keyProtectionPassword See {@link KeyStore#getKey(String, char[]); {@code null} to prompt the user
     * @return                      {@code null} indicates that the user cancelled the operation
     */
    @Nullable public static SecretKey
    adHocSecretKey(
        File             keyStoreFile,
        @Nullable char[] keyStorePassword,
        String           keyAlias,
        @Nullable char[] keyProtectionPassword
    ) throws GeneralSecurityException, IOException {

        // Notice: The "KeyStore.getDefault()" cannot store SecretKeys, so we must use "JCEKS".
        KeyStore ks = KeyStore.getInstance("JCEKS");

        SecretKey secretKey;
        boolean   keystoreDirty;
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

            if (keyProtectionPassword != null) {
                secretKey = (SecretKey) ks.getKey(keyAlias, keyProtectionPassword);
            } else {

                try {

                    // Try to get the key with an EMPTY protection password first.
                    secretKey = (SecretKey) ks.getKey(keyAlias, new char[0]);
                    keyProtectionPassword = new char[0];
                } catch (UnrecoverableKeyException uke) {

                    // The user now needs to enter the correct master password.
                    String label1 = (
                        ""
                        + "<html>"
                        + "Do you want to use the existing authentication store for user names and passwords?<br />"
                        + "<br />"
                        + "If yes, then enter the correct master password:"
                        + "</html>"
                    );
                    for (;;) {

                        keyProtectionPassword = Cryptors.askForPassword(
                            "Authentication store",
                            label1,
                            (
                                ""
                                + "<html>"
                                +    "<small>"
                                +       "If you lost the master password, you can always delete the keystore<br />"
                                +       "file \"" + keyStoreFile + "\"<br />"
                                +       "and start over with a new master password."
                                +    "</small>"
                                + "</html>"
                            )
                        );
                        if (keyProtectionPassword == null) return null;

                        try {
                            secretKey = (SecretKey) ks.getKey(keyAlias, keyProtectionPassword);
                            break;
                        } catch (UnrecoverableKeyException uke2) {
                            label1 = "That password is wrong; please enter the correct master password:";
                        }
                    }
                }
            }

            keystoreDirty = false;
        } else {

            // The keystore file does not yet exist; create an empty keystore.
            ks.load(null, keyStorePassword);
            secretKey = null;
            keystoreDirty = true;
        }

        if (secretKey == null) {

            // Key does not exist in keystore; generate a new one and put it into the keystore.
            secretKey = KeyGenerator.getInstance("AES").generateKey();

            if (keyProtectionPassword == null) {
                keyProtectionPassword = Cryptors.askForPassword(
                    "Authentication store",
                    (
                        ""
                        + "<html>"
                        + "Do you want to create an authentication store for user names and passwords?<br />"
                        + "<br />"
                        + "If yes, then define your master password here:"
                        + "</html>"
                    ),
                    (
                        ""
                        + "<html>"
                        +    "<small>"
                        +       "Choosing an empty or trivial master password makes it possible for an<br />"
                        +       "attacker to compromise stored user names and passwords."
                        +    "</small>"
                        + "</html>"
                    )
                );
                if (keyProtectionPassword == null) return null;
            }

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

    @Nullable private static char[]
    askForPassword(String title, String label1, @Nullable String label2) {

        JPasswordField passwordField = new JPasswordField();
        Cryptors.focussify(passwordField);

        if (JOptionPane.showOptionDialog(
            null,                         // parentComponent
            new Object[] {                // message
                new JLabel(label1),
                passwordField,
                label2 == null ? null : new JLabel(label2)
            },
            title,                        // title
            JOptionPane.YES_NO_OPTION,    // optionType
            JOptionPane.QUESTION_MESSAGE, // messageType
            null,                         // icon
            null,                         // options
            null                          // initialValue
        ) != JOptionPane.YES_OPTION) return null;

        return passwordField.getPassword();
    }

    public static Cryptor
    addChecksum(Cryptor delegate) {

        return Cryptors.from(
            Encryptors.addChecksum(delegate),
            Decryptors.addChecksum(delegate)
        );
    }

    private static void
    focussify(JComponent component) {

        // One terrible hack: One this particular machine, "focussify()" makes any dialog unusable, and I have no
        // reasonable way to debug the problem.
        try {
            if (InetAddress.getLocalHost().getHostName().endsWith("DRMF")) return;
        } catch (UnknownHostException e) {
            ;
        }

        // This is tricky... see
        //    http://tips4java.wordpress.com/2010/03/14/dialog-focus/
        component.addAncestorListener(new AncestorListener() {

            @Override public void
            ancestorAdded(@Nullable AncestorEvent event) {
                assert event != null;
                JComponent component = event.getComponent();
                component.requestFocusInWindow();

                component.removeAncestorListener(this);
            }

            @Override public void ancestorRemoved(@Nullable AncestorEvent event) {}
            @Override public void ancestorMoved(@Nullable AncestorEvent event)   {}
        });
    }
}
