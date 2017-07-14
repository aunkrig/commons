
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

import java.io.IOException;
import java.io.Writer;

import javax.swing.text.Segment;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Utility functionality related to {@link Writer}s.
 */
public final
class Writers {

    private Writers() {}

    /**
     * A writer that ignores any data written to it.
     */
    public static final Writer DISCARD = new Writer() {
        @Override public void                            write(int c)                                 {}
        @NotNullByDefault(false) @Override public void   write(String str, int off, int len)          {}
        @NotNullByDefault(false) @Override public Writer append(CharSequence csq)                     { return this; }
        @NotNullByDefault(false) @Override public Writer append(CharSequence csq, int start, int end) { return this; }
        @NotNullByDefault(false) @Override public void   write(char[] cbuf, int off, int len)         {}
        @Override public void                            flush()                                      {}
        @Override public void                            close()                                      {}
    };

    /**
     * Wraps an {@link Appendable} as a {@link Writer} (if necessary).
     */
    @NotNullByDefault(false) public static Writer
    fromAppendable(final Appendable delegate) {
        if (delegate instanceof Writer) return (Writer) delegate;
        return new Writer() {

            // SUPPRESS CHECKSTYLE LineLength:8
            @Override public void   write(int c)                                 throws IOException { delegate.append((char) c);                          }
            @Override public void   write(char[] cbuf)                           throws IOException { delegate.append(new Segment(cbuf, 0, cbuf.length)); }
            @Override public void   write(char[] cbuf, int off, int len)         throws IOException { delegate.append(new Segment(cbuf, off, len));       }
            @Override public void   write(String str)                            throws IOException { delegate.append(str);                               }
            @Override public void   write(String str, int off, int len)          throws IOException { delegate.append(str, off, len);                     }
            @Override public Writer append(CharSequence csq)                     throws IOException { delegate.append(csq);                  return this; }
            @Override public Writer append(CharSequence csq, int start, int end) throws IOException { delegate.append(csq, start, end);      return this; }
            @Override public Writer append(char c)                               throws IOException { delegate.append(c);                    return this; }
            @Override public void   flush()                                                         {}
            @Override public void   close()                                                         {}
        };
    }
}
