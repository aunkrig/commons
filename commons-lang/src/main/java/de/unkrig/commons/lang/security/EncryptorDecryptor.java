
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

/**
 * An API that encrypts and decrypts byte arrays.
 * <p>
 *   The promise is that
 * </p>
 * <blockquote>
 *   {@code Arrays.equals(}<var>ba</var>{@code ,} <var>ed</var>{@code .decrypt(}<var>ed</var>{@code
 *   .encrypt(}<var>ba</var>{@code )))}
 * </blockquote>
 * <p>
 *   is {@code true} for any byte array <var>ba</var> and {@link EncryptorDecryptor} <var>ed</var>.
 *   Also, it is (more or less) difficult for an attacker to decrypt any encrypted data without the <var>ed</var>.
 * </p>
 *
 * @see EncryptorDecryptors#encrypt(EncryptorDecryptor, CharSequence)      For encrypting <em>strings</em> rather than
 *                                                                         byte arrays
 * @see EncryptorDecryptors#decrypt(EncryptorDecryptor, DestroyableString) For decrypting <em>strings</em> rather than
 *                                                                         byte arrays
 */
public
interface EncryptorDecryptor {

    /**
     * Encrypts the <var>unencrypted</var> byte array and fills it with zeros.
     *
     * @return The encrypted data
     */
    byte[] encrypt(byte[] unencrypted);

    /**
     * Decrypts the <var>encrypted</var> byte array and fills it with zeros.
     *
     * @return The decrypted data
     */
    byte[] decrypt(byte[] encrypted);
}
