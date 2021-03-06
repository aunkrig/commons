
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

package de.unkrig.commons.file.contentsprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @param <T> The type returned by {@link #process(String, InputStream, Date, long, long, ProducerWhichThrows)}
 * @see       #process(String, InputStream, Date, long, long, ProducerWhichThrows)
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
     * If the <var>pathPredicate</var> evaluates to {@code true} for the <var>path</var>, then the
     * <var>trueDelegate</var> is called, otherwise the <var>falseDelegate</var>.
     */
    @Override @Nullable public T
    process(
        String                                                            path,
        InputStream                                                       is,
        @Nullable Date                                                    lastModifiedDate,
        long                                                              size,
        long                                                              crc32,
        ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
    ) throws IOException {

        if (this.pathPredicate.evaluate(path)) {
            return this.trueDelegate.process(path, is, lastModifiedDate, size, crc32, opener);
        } else {
            return this.falseDelegate.process(path, is, lastModifiedDate, size, crc32, opener);
        }
    }
}
