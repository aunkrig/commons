
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * The {@code char[]} counterparts for {@link String#getBytes(Charset)} and {@link String#String(byte[], Charset)}.
 */
public final
class SecureCharsets {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private SecureCharsets() {}

    /**
     * Decodes the <var>ba</var> and fills it with zeros. Leaves no traces of the data in the heap, except for the
     * returned char array.
     */
    public static char[]
    secureDecode(byte[] ba, Charset cs) {

        if (ba.length == 0) return new char[0];

        try {

            // Set up the charset encoder.
            CharsetDecoder cd = (
                cs
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
            );

            // Allocate a char array for the decoded output.
            char[] ca = new char[ba.length * (int) Math.ceil(cd.maxCharsPerByte())];

            // Wrap input and output arrays in ByteBuffers resp. CharBuffers.
            ByteBuffer bb = ByteBuffer.wrap(ba);
            CharBuffer cb = CharBuffer.wrap(ca);

            // Now go for it!
            try {
                cd.reset();

                CoderResult cr = cd.decode(bb, cb, true);
                if (!cr.isUnderflow()) cr.throwException();

                cr = cd.flush(cb);
                if (!cr.isUnderflow()) cr.throwException();
            } catch (CharacterCodingException cce) {
                throw new AssertionError(cce);
            }

            if (cb.position() != ca.length) {
                char[] tmp = ca;
                ca = Arrays.copyOf(ca, cb.position());
                Arrays.fill(tmp, '\0');
            }

            return ca;
        } finally {
            Arrays.fill(ba, (byte) 0);
        }
    }

    /**
     * Encodes the <var>ca</var> and fills it with zeros. Leaves no traces of the data in the heap, except for the
     * returned byte array.
     */
    public static byte[]
    secureEncode(char[] ca, Charset charset) {

        CharsetEncoder ce = (
            charset
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        );

        byte[] ba = new byte[ca.length * (int) Math.ceil(ce.maxBytesPerChar())];

        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca);

        ce.reset();

        try {

            CoderResult cr = ce.encode(cb, bb, true);
            if (!cr.isUnderflow()) cr.throwException();

            cr = ce.flush(bb);
            if (!cr.isUnderflow()) cr.throwException();
        } catch (CharacterCodingException cce) {
            throw new AssertionError(cce);
        }

        if (bb.position() != ba.length) {
            byte[] tmp = ba;
            ba = Arrays.copyOf(ba, bb.position());
            Arrays.fill(tmp, (byte) 0);
        }

        return ba;
    }
}
