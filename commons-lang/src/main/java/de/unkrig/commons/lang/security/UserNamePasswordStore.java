
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

import java.io.IOException;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Stores username/password pairs, persistently or not.
 */
public
interface UserNamePasswordStore {

    /**
     * @return The least recently {@link #put(String, String, SecureString) put} <var>userName</var> for the
     *         <var>key</var>, or {@code null} if a user name for the given <var>key</var> is not in this store
     */
    @Nullable String
    getUserName(String key);

    /**
     * @return The least recently {@link #put(String, String, SecureString) put} <var>password</var> for the
     *         <var>key</var>, or {@code null} if a password for the given <var>key</var> is not in this store
     */
    @Nullable SecureString
    getPassword(String key);

    /**
     * Updates the user name and password for the given <var>key</var>. Depending on whether this store is
     * persistent or not, the new values will not or will be forgotten when the JVM exits.
     *
     * @param userName {@code null} to remove the user name for the given <var>key</var> from this store
     * @param password {@code null} to remove the password for the given <var>key</var> from this store
     */
    void
    put(String key, @Nullable String userName, @Nullable SecureString password) throws IOException;
}
