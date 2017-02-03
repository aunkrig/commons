
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

package de.unkrig.commons.file.contentstransformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.unkrig.commons.io.ByteFilterInputStream;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;

/**
 * A {@link ContentsTransformer} that delegates contents transformation to one of two delegates, depending on whether
 * the <var>path</var> argument matches a string {@link Predicate} or not.
 */
public
class SelectiveContentsTransformer implements ContentsTransformer {

    private final Predicate<? super String> pathPredicate;
    private final ContentsTransformer       transformer;
    private final ContentsTransformer       delegate;

    /**
     * Equivalent with {@link SelectiveContentsTransformer#SelectiveContentsTransformer(Predicate, ContentsTransformer,
     * ContentsTransformer)}, but conducts certain optimizations.
     * <ul>
     *   <li>When <var>pathPredicate</var> is {@link PredicateUtil#always()}</li>
     *   <li>When <var>pathPredicate</var> is {@link PredicateUtil#never()}</li>
     *   <li>When <var>transformer</var> is {@link ContentsTransformations#COPY}</li>
     *   <li>When <var>delegate</var> is {@link ContentsTransformations#COPY}</li>
     * </ul>
     */
    public static ContentsTransformer
    create(
        Predicate<? super String> pathPredicate,
        ContentsTransformer       transformer,
        ContentsTransformer       delegate
    ) {

        if (pathPredicate == PredicateUtil.always()) return ContentsTransformations.chain(transformer, delegate);

        if (pathPredicate == PredicateUtil.never()) return delegate;

        if (transformer == ContentsTransformations.COPY) return delegate;

        return new SelectiveContentsTransformer(pathPredicate, transformer, delegate);
    }

    /**
     * If the <var>pathPredicate</var> does not match the node's path, then the <var>delegate</var> is called.
     * Otherwise the <var>delegate</var> is called, and its output is piped through the <var>transformer</var>.
     * <p>
     *   Notice that the preformance is particularly good if the <var>transformer</var> and/or the <var>delegate</var>
     *   is {@link ContentsTransformations#COPY}.
     * </p>
     */
    public
    SelectiveContentsTransformer(
        Predicate<? super String> pathPredicate,
        ContentsTransformer       transformer,
        ContentsTransformer       delegate
    ) {
        this.pathPredicate = pathPredicate;
        this.transformer   = transformer;
        this.delegate      = delegate;
    }

    /**
     * If the <var>pathPredicate</var> does not match the <var>path</var>, then the <var>delegate</var> is called with
     * arguments <var>is</var> and <var>os</var>.
     * Otherwise the <var>delegate</var> is called with argument <var>is</var>, and its output is piped through the
     * {@code transformer} before it is written to <var>os</var>.
     */
    @Override public void
    transform(final String path, InputStream is, OutputStream os) throws IOException {

        // Check the path predicate.
        if (!this.pathPredicate.evaluate(path)) {
            this.delegate.transform(path, is, os);
            return;
        }

        // Avoid the creation of an unnecessary ByteFilterInputStream if the delegate is
        // 'ContentsTransformerUtil.copy()'.
        if (this.delegate == ContentsTransformations.COPY) {
            this.transformer.transform(path, is, os);
            return;
        }

        // Avoid the creation of an unnecessary ByteFilterInputStream if the transformer is
        // 'ContentsTransformerUtil.copy()'.
        if (this.transformer == ContentsTransformations.COPY) {
            this.delegate.transform(path, is, os);
            return;
        }

        this.delegate.transform(
            path,
            new ByteFilterInputStream(is, new ContentsTransformerByteFilter(this.transformer, path)),
            os
        );
    }

    @Override public String
    toString() {
        return (
            "("
            + this.pathPredicate
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
