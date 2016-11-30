
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

package de.unkrig.commons.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link FilterReader} that transforms the char stream through a {@link CharFilter}. Any {@link IOException}
 * and {@link RuntimeException} that {@link CharFilter#run} throws is caught and rethrown by the {@link #read}
 * methods of this object.
 */
public
class CharFilterReader extends FilterReader {

    private final Thread               worker;
    @Nullable private IOException      charFilterIOException;
    @Nullable private RuntimeException charFilterRuntimeException;

    /**
     * @see CharFilterReader
     */
    public
    CharFilterReader(final Reader in, final CharFilter<?> charFilter) {
        super(new PipedReader());

        final PipedWriter pw;
        try {
            pw = new PipedWriter((PipedReader) this.in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.worker = new Thread("CharFilterReader") {

            @Override public void
            run() {
                try {
                    charFilter.run(in, pw);
                } catch (IOException ioe) {
                    CharFilterReader.this.charFilterIOException = ioe;
                } catch (RuntimeException re) {
                    CharFilterReader.this.charFilterRuntimeException = re;
                } finally {

                    // This will signal "end-of-input" to the read side.
                    try { pw.close(); } catch (IOException ioe) {}
                }
            }
        };
        this.worker.start();
    }

    @Override public int
    read() throws IOException {
        char[] buffer = new char[1];
        for (;;) {
            int n = this.read(buffer, 0, 1);
            if (n == -1) return -1;
            if (n == 1) return 0xff & buffer[0];
            if (n != 0) throw new IllegalStateException();
        }
    }

    @NotNullByDefault(false) @Override public int
    read(char[] b, int off, int len) throws IOException {

        final int n = this.in.read(b, off, len);

        if (this.charFilterIOException != null) {

            // Wrap because the charFilterIOException comes from a different thread, and we want to document the call
            // stack of THIS thread as well.
            throw ExceptionUtil.wrap("CharFilterReader", this.charFilterIOException);
        }

        if (this.charFilterRuntimeException != null) {

            // Wrap because the charFilterRuntimeException comes from a different thread, and we want to document the
            // call stack of THIS thread as well.
            throw ExceptionUtil.wrap("CharFilterReader", this.charFilterRuntimeException);
        }

        return n;
    }

    @Override public void
    close() throws IOException {
        try {

            // This will put the pipe into the "broken" state, and the worker thread is very likely to terminate
            // quickly.
            this.in.close();
        } finally {
            for (;;) {
                try {
                    this.worker.join();
                    break;
                } catch (InterruptedException ie) {
                    ;
                }
            }
        }

        if (this.charFilterIOException != null) {

            // Wrap because the charFilterIOException comes from a different thread, and we want to document the call
            // stack of THIS thread as well.
            throw ExceptionUtil.wrap("CharFilterReader", this.charFilterIOException);
        }

        if (this.charFilterRuntimeException != null) {

            // Wrap because the charFilterRuntimeException comes from a different thread, and we want to document the
            // call stack of THIS thread as well.
            throw ExceptionUtil.wrap("CharFilterReader", this.charFilterRuntimeException);
        }
    }
}
