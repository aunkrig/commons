
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

package de.unkrig.commons.lang.protocol;

/**
 * Utility functionality related to {@link Transformer Transformer&lt;String>}s.
 */
public final
class StringTransformers {

    private StringTransformers() {}

    /**
     * @see String#toUpperCase()
     */
    public static final Transformer<CharSequence, String>
    TO_UPPER_CASE = new Transformer<CharSequence, String>() {

        @Override public String
        transform(CharSequence in) { return in.toString().toUpperCase(); }
    };

    /**
     * @see String#toLowerCase()
     */
    public static final Transformer<CharSequence, String>
    TO_LOWER_CASE = new Transformer<CharSequence, String>() {

        @Override public String
        transform(CharSequence in) { return in.toString().toLowerCase(); }
    };

    /**
     * @see String#replace(char, char)
     */
    public static Transformer<CharSequence, String>
    replaceChar(final char oldChar, final char newChar) {

        return new Transformer<CharSequence, String>() {

            @Override public String
            transform(CharSequence in) { return in.toString().replace(oldChar, newChar); }
        };
    }

    /**
     * Transforms any string to {@code ""} (the empty string).
     */
    @SuppressWarnings("unchecked")
    public static <EX extends Throwable> TransformerWhichThrows<CharSequence, CharSequence, EX>
    toEmpty() { return StringTransformers.TO_EMPTY; }

    /**
     * Transforms any string to {@code ""} (the empty string).
     */
    @SuppressWarnings("rawtypes") public static final Transformer
    TO_EMPTY = new Transformer() {

        @Override public Object
        transform(Object in) { return ""; }
    };
}
