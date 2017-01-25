
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

import javax.security.auth.Destroyable;

/**
 * An interface that implements both {@link Encryptor} and {@link Decryptor}. ("{@code EncryptorAndDecryptor}" was
 * considered a too long name for this interface.)
 * <p>
 *   Additionally, this interface promises that the ancryptor and decryptor are "right" for each other, i.e.
 * </p>
 * <blockquote>
 *   {@code Arrays.equals(}<var>ba</var>{@code ,} <var>cryptor</var>{@code .decrypt(}<var>cryptor</var>{@code
 *   .encrypt(}<var>ba</var>{@code )))}
 * </blockquote>
 * <p>
 *   is {@code true} for any byte array <var>ba</var> and {@link Cryptor} <var>cryptor</var>.
 *   Also, it is (more or less) difficult for an attacker to decrypt any encrypted data without the <var>ed</var>.
 * </p>
 *
 * @see Encryptors#encrypt(Cryptor, CharSequence)      For encrypting <em>strings</em> rather than byte arrays
 * @see Decryptors#decrypt(Cryptor, DestroyableString) For decrypting <em>strings</em> rather than byte arrays
 */
public
interface Cryptor extends Encryptor, Decryptor, Destroyable {
}
