
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.file.contentstransformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.unkrig.commons.io.ByteFilterInputStream;
import de.unkrig.commons.lang.protocol.Predicate;

/**
 * A {@link ContentsTransformer} that delegates contents transformation to one of two delegates, depending on whether
 * the {@code name} argument matches a string {@link Predicate} or not.
 */
public
class SelectiveContentsTransformer implements ContentsTransformer {

    private final Predicate<? super String> namePredicate;
    private final ContentsTransformer       transformer;
    private final ContentsTransformer       delegate;

    /**
     * If the {@code namePredicate} does not match the node's name, then the {@code delegate} is called.
     * Otherwise the {@code delegate} is called, and its output is piped through the {@code transformer}.
     * <p>
     *   Notice that the preformance is particularly good if the {@code transformer} and/or the {@code delegate} is
     *   {@link ContentsTransformations#COPY}.
     * </p>
     */
    public
    SelectiveContentsTransformer(
        Predicate<? super String> namePredicate,
        ContentsTransformer       transformer,
        ContentsTransformer       delegate
    ) {
        this.namePredicate = namePredicate;
        this.transformer   = transformer;
        this.delegate      = delegate;
    }

    /**
     * If the {@code namePredicate} does not match the {@code name}, then the {@code delegate} is called with
     * arguments {@code is} and {@code os}.
     * Otherwise the {@code delegate} is called with argument {@code is}, and its output is piped through the {@code
     * transformer} before it is written to {@code os}.
     */
    @Override public void
    transform(final String name, InputStream is, OutputStream os) throws IOException {

        // Check the name predicate.
        if (!this.namePredicate.evaluate(name)) {
            this.delegate.transform(name, is, os);
            return;
        }

        // Avoid the creation of an unnecessary ByteFilterInputStream if the delegate is
        // 'ContentsTransformerUtil.copy()'.
        if (this.delegate == ContentsTransformations.COPY) {
            this.transformer.transform(name, is, os);
            return;
        }

        // Avoid the creation of an unnecessary ByteFilterInputStream if the transformer is
        // 'ContentsTransformerUtil.copy()'.
        if (this.transformer == ContentsTransformations.COPY) {
            this.delegate.transform(name, is, os);
            return;
        }

        this.delegate.transform(
            name,
            new ByteFilterInputStream(is, new ContentsTransformerByteFilter(this.transformer, name)),
            os
        );
    }

    @Override public String
    toString() {
        return (
            "("
            + this.namePredicate
            + " ? "
            + this.delegate
            + " => "
            + this.transformer
            + " : "
            + this.delegate
            + ")"
        );
    }
}
