
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

package de.unkrig.commons.file.filetransformation;

import java.io.File;
import java.io.IOException;

import de.unkrig.commons.lang.protocol.Predicate;

/**
 * A {@link FileTransformer} that feeds the file to the {@code trueDelegate} iff the {@code path} matches a given
 * {@link Predicate}, and otherwise to the {@code falseDelegate}.
 */
public
class SelectiveFileTransformer implements FileTransformer {

    private final Predicate<? super String> pathPredicate;
    private final FileTransformer           trueDelegate;
    private final FileTransformer           falseDelegate;

    public
    SelectiveFileTransformer(
        Predicate<? super String> pathPredicate,
        FileTransformer           trueDelegate,
        FileTransformer           falseDelegate
    ) {
        this.pathPredicate = pathPredicate;
        this.trueDelegate  = trueDelegate;
        this.falseDelegate = falseDelegate;
    }

    @Override public void
    transform(String path, File in, File out, Mode mode) throws IOException {
        if (this.pathPredicate.evaluate(path)) {
            this.trueDelegate.transform(path, in, out, mode);
        } else {
            this.falseDelegate.transform(path, in, out, mode);
        }
    }

    @Override public String
    toString() { return this.pathPredicate + " ? " + this.trueDelegate + " : " + this.falseDelegate; }
}
