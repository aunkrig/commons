
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

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import de.unkrig.commons.lang.CharSequences;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link CharSequence} that can be {@link #close()}d, which reliably removes its characters from the heap.
 * After a {@link SecureString} has been {@link #close()}d, all {@link CharSequence} methods throw an {@link
 * IllegalStateException}.
 * <p>
 *   Notice that the {@link #toString()} method returns either {@code "****"} or {@code "ERASED"}.
 * </p>
 */
public
class SecureString implements CharSequence, Closeable {

    @Nullable private char[] contents;

    public
    SecureString(String that) {

        int    len = that.length();
        char[] ca  = (this.contents = new char[len]);

        for (int i = 0; i < len; i++) ca[i] = that.charAt(i);
    }

    /**
     * A new secure string which is a copy of <var>that</var>
     */
    public
    SecureString(CharSequence that) {

        int    len = that.length();
        char[] ca  = (this.contents = new char[len]);

        for (int i = 0; i < len; i++) ca[i] = that.charAt(i);
    }

    /**
     * A new secure string which takes ownership over the character array
     */
    public
    SecureString(char[] ca) { this.contents = ca; }

    /**
     * Decodes the <var>ba</var> and fills it with zeros.
     */
    public
    SecureString(byte[] ba, String charsetName) {
        this.contents = SecureString.secureDecode(ba, Charset.forName(charsetName));
   }

    /**
     * Decodes the <var>ba</var> and fills it with zeros.
     */
    public
    SecureString(byte[] ba, Charset cs) { this.contents = SecureString.secureDecode(ba, cs); }

    /**
     * Decodes the <var>ba</var> and fills it with zeros. Leaves no traces of the data in the heap, except for the
     * returned char array.
     */
    private static char[]
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
     * @return A new secure string which is a copy of {@code this}
     */
    @Nullable public SecureString
    copy() { return new SecureString(this); }

    /**
     * @return A new secure string which is a copy of <var>that</var>, or {@code null} iff <var>that</var> {@code ==
     *         null}
     */
    @Nullable public static SecureString
    from(@Nullable CharSequence that) { return that == null ? null : new SecureString(that); }

    /**
     * @return A new secure string which takes ownership of <var>that</var>, or {@code null} iff <var>that</var> {@code
     *         == null}
     */
    @Nullable public static SecureString
    from(@Nullable char[] that) { return that == null ? null : new SecureString(that); }

    @Override public int
    length() {
        char[] ca = this.contents;
        if (ca == null) throw new IllegalStateException();
        return ca.length;
    }

    @Override public char
    charAt(int index) {

        char[] ca = this.contents;
        if (ca == null) throw new IllegalStateException();

        return ca[index];
    }


    @Override public void
    close() {

        char[] ca = this.contents;
        if (ca == null) return;

        this.contents = null;

        Arrays.fill(ca, '\0');
    }

    public char[]
    toCharArray() {

        char[] ca = this.contents;
        if (ca == null) throw new IllegalStateException();

        return Arrays.copyOf(ca, ca.length);
    }

    @Override public CharSequence
    subSequence(int start, int end) {

        if (this.contents == null) throw new IllegalStateException();

        return CharSequences.subSequence(this, start, end);
    }

    @Override public int
    hashCode() {

        char[] ca = this.contents;
        if (ca == null) throw new IllegalStateException();

        int h = 0;
        for (int i = 0; i < ca.length; i++) h = 31 * h + ca[i];

        return h;
    }

    @Override public boolean
    equals(@Nullable Object obj) {

        if (this == obj) return true;
        if (!(obj instanceof SecureString)) return false;
        SecureString that = (SecureString) obj;

        char[] ca1 = this.contents;
        if (ca1 == null) throw new IllegalStateException();
        char[] ca2 = that.contents;
        if (ca2 == null) throw new IllegalStateException();

        int len = ca1.length;
        if (ca2.length != len) return false;

        for (int i = 0; i < len; i++) if (ca1[i] != ca2[i]) return false;
        return true;
    }

    @Override
    protected void finalize() { this.close(); }

    /**
     * @return Either {@code "****"} or {@code "ERASED"}.
     */
    @Override public String
    toString() { return this.contents == null ? "ERASED" : "****"; }
}
