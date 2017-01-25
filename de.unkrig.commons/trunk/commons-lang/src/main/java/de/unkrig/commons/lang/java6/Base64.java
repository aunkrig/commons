
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

package de.unkrig.commons.lang.java6;

import java.io.IOException;
import java.util.Arrays;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.security.Cryptor;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Utility methods related to {@link Cryptor}s.
 */
public final
class Base64 {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Base64() {}

    /**
     * BASE64-encodes the <var>subject</var> byte array and fills it with zeros.
     */
    public static String
    encode(byte[] subject) {

        try {

            @SuppressWarnings("restriction") String tmp = new BASE64Encoder().encode(subject);

            return tmp;
        } finally {
            Arrays.fill(subject, (byte) 0);
        }
    }

    /**
     * BASE64-decodes the <var>subject</var>.
     */
    public static byte[]
    decode(String subject) throws AssertionError {

        try {

            @SuppressWarnings("restriction") byte[] tmp = new BASE64Decoder().decodeBuffer(subject);
            return tmp;
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }
}
