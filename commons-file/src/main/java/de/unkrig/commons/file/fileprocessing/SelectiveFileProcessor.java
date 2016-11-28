
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

package de.unkrig.commons.file.fileprocessing;

import java.io.File;
import java.io.IOException;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Passes to a delegate only the files who's path matches a given predicate.
 *
 * @param <T> Type returned by {@link #process(String, File)}
 */
public
class SelectiveFileProcessor<T> implements FileProcessor<T> {

    private final Predicate<? super String> pathPredicate;
    private final FileProcessor<T>          delegate1, delegate2;

    public
    SelectiveFileProcessor(
        Predicate<? super String> pathPredicate,
        FileProcessor<T>          delegate1,
        FileProcessor<T>          delegate2
    ) {
        this.pathPredicate = pathPredicate;
        this.delegate1     = delegate1;
        this.delegate2     = delegate2;
    }

    @Override @Nullable public T
    process(String path, File file) throws IOException, InterruptedException {

        if (this.pathPredicate.evaluate(path)) {
            return this.delegate1.process(path, file);
        } else {
            return this.delegate2.process(path, file);
        }
    }

    @Override public String
    toString() { return this.pathPredicate + " ? (" + this.delegate1 + ") : (" + this.delegate2 + ")"; }
}
