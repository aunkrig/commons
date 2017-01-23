
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

import java.util.Arrays;

import de.unkrig.commons.lang.CharSequences;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link CharSequence} that can be {@link #erase()}d, which reliably removes its characters from the heap.
 * After a {@link SecureString} has been {@link #erase()}d, all {@link CharSequence} methods throw an {@link
 * IllegalStateException}.
 * <p>
 *   Notice that the {@link #toString()} method returns either {@code "****"} or {@code "ERASED"}.
 * </p>
 */
public
class SecureString implements CharSequence {

    @Nullable private char[] contents;

    public
    SecureString(CharSequence that) {

        int    len = that.length();
        char[] ca  = (this.contents = new char[len]);

        for (int i = 0; i < len; i++) ca[i] = that.charAt(i);
    }

    public
    SecureString(char[] ca) { this.contents = ca; }

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

    public void
    erase() {

        char[] ca = this.contents;
        if (ca == null) return;

        Arrays.fill(ca, '\0');
        this.contents = null;
    }

    public char[]
    extract() {

        char[] ca = this.contents;
        if (ca == null) throw new IllegalStateException();

        this.contents = null;

        return ca;
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
    protected void finalize() { this.erase(); }

    /**
     * @return Either {@code "****"} or {@code "ERASED"}.
     */
    @Override public String
    toString() { return this.contents == null ? "ERASED" : "****"; }
}
