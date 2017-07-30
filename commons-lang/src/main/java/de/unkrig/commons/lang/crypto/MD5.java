
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.unkrig.commons.lang.AssertionUtil;

public final
class MD5 {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private MD5() {}

    /**
     * @return Exactly 16 bytes
     */
    public static byte[]
    of(String subject) { return MD5.of(subject.getBytes(Charset.forName("UTF-8"))); }

    /**
     * @return Exactly 16 bytes
     */
    public static byte[]
    of(byte[] subject) { return MD5.of(subject, 0, subject.length); }

    /**
     * @return Exactly 16 bytes
     */
    public static byte[]
    of(byte[] subject, int offset, int length) {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        }

        md.update(subject, offset, length);

        byte[] result = md.digest();
        assert result.length == 16;

        return result;
    }

    /**
     * @return The MD5 digest (exactly 16 bytes) of all bytes produced by the <var>inputStream</var>
     */
    public static byte[]
    of(InputStream inputStream) throws IOException {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        }

        byte[] ba = new byte[4096];
        for (;;) {
            int n = inputStream.read(ba);
            if (n == -1) break;
            md.update(ba, 0, n);
        }

        byte[] result = md.digest();
        assert result.length == 16;

        return result;
    }
}
