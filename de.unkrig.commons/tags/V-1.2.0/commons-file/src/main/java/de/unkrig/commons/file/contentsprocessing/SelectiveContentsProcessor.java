
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

package de.unkrig.commons.file.contentsprocessing;

import java.io.IOException;
import java.io.InputStream;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @param <T> The type returned by {@link #process(String, InputStream, long, long, ProducerWhichThrows)}
 * @see       #process(String, InputStream, long, long, ProducerWhichThrows)
 */
public
class SelectiveContentsProcessor<T> implements ContentsProcessor<T> {

    private final Predicate<? super String> pathPredicate;
    private final ContentsProcessor<T>      trueDelegate;
    private final ContentsProcessor<T>      falseDelegate;

    public
    SelectiveContentsProcessor(
        Predicate<? super String> pathPredicate,
        ContentsProcessor<T>      trueDelegate,
        ContentsProcessor<T>      falseDelegate
    ) {
        this.pathPredicate = pathPredicate;
        this.trueDelegate  = trueDelegate;
        this.falseDelegate = falseDelegate;
    }

    /**
     * If the {@code pathPredicate} evaluates to {@code true} for the {@code path}, then the {@code trueDelegate} is
     * called, otherwise the {@code falseDelegate}.
     */
    @Override @Nullable public T
    process(
        String                                                            path,
        InputStream                                                       is,
        long                                                              size,
        long                                                              crc32,
        ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
    ) throws IOException {

        if (this.pathPredicate.evaluate(path)) {
            return this.trueDelegate.process(path, is, size, crc32, opener);
        } else {
            return this.falseDelegate.process(path, is, size, crc32, opener);
        }
    }
}
