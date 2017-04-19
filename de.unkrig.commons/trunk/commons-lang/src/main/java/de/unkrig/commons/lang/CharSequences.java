
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

package de.unkrig.commons.lang;

/**
 * Utility methods related to {@link CharSequence}s.
 */
public
class CharSequences {

    /**
     * Creates and returns a sub-{@link CharSequence} of the given <var>delegate</var> {@link CharSequence}.
     */
    public static CharSequence
    subSequence(final CharSequence delegate, final int start, final int end) {

        if (start == 0 && end == delegate.length()) return delegate;

        return new CharSequence() {

            @Override public CharSequence
            subSequence(int start2, int end2) {
                return CharSequences.subSequence(delegate, start + start2, start + end2);
            }

            @Override public int
            length() { return end - start; }

            @Override public char
            charAt(int index) { return delegate.charAt(start + index); }
        };
    }

    /**
     * Wraps a character array as a {@link CharSequence}.
     */
    public static CharSequence
    from(final char[] buf) {

        return new CharSequence() {

            @Override public CharSequence
            subSequence(int start, int end) { return CharSequences.subSequence(this, start, end); }

            @Override public int
            length() { return buf.length; }

            @Override public char
            charAt(int index) { return buf[index]; }
        };
    }

    /**
     * Wraps a character array as a {@link CharSequence}.
     */
    public static CharSequence
    from(final char[] buf, final int off, final int len) {

        // Fail fast:
        if (off < 0)                throw new IndexOutOfBoundsException(off + "<0");
        if (off > buf.length)       throw new IndexOutOfBoundsException(off + ">" + buf.length);
        if (len < 0)                throw new IndexOutOfBoundsException(len + "<0");
        if (off + len > buf.length) throw new IndexOutOfBoundsException((off + len) + ">" + buf.length);

        return new CharSequence() {

            @Override public CharSequence
            subSequence(int start, int end) { return CharSequences.subSequence(this, off + start, off + end); }

            @Override public int
            length() { return len; }

            @Override public char
            charAt(int index) { return buf[off + index]; }
        };
    }
}
