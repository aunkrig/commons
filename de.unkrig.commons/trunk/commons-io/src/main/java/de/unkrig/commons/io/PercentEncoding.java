
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.io;

/**
 * Implements <a href="http://en.wikipedia.org/wiki/Percent-encoding">Percent-Encoding</a>.
 */
public final
class PercentEncoding {

    private
    PercentEncoding() {}

    private static final byte[] CHAR_PROPS = new byte[256];
    private static final byte   RESERVED   = 1;
    private static final byte   UNRESERVED = 2;

    private static final byte[] RESERVED_CHARS;
    static {
        String s = "!*'();:@&=+$,/?#[]";
        RESERVED_CHARS = new byte[s.length()];
        for (int i = 0; i < PercentEncoding.RESERVED_CHARS.length; i++) {
            byte c = (byte) s.charAt(i);
            PercentEncoding.RESERVED_CHARS[i] = c;
            PercentEncoding.CHAR_PROPS[c]     |= PercentEncoding.RESERVED;
        }
    }

    private static final byte[] UNRESERVED_CHARS;
    static {
        String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~";
        UNRESERVED_CHARS = new byte[s.length()];
        for (int i = 0; i < PercentEncoding.UNRESERVED_CHARS.length; i++) {
            byte c = (byte) s.charAt(i);
            PercentEncoding.UNRESERVED_CHARS[i] = c;
            PercentEncoding.CHAR_PROPS[c]       |= PercentEncoding.UNRESERVED;
        }
    }

    /**
     * @return Whether the given character is unreserved.
     */
    public static boolean
    isUnreserved(int b) {
        return PercentEncoding.CHAR_PROPS[0xff & b] == PercentEncoding.UNRESERVED;
    }
}
