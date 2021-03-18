
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
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;

import de.unkrig.commons.io.LineUtil.LineAndColumnTracker;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility functionality related to {@link Reader}s.
 */
public final
class Readers {

    private Readers() {}

    /**
     * A reader that always signals "end-of-input".
     */
    public static final Reader
    EMPTY_READER = new Reader() {
        @Override public int  read()                                       { return -1; }
        @Override public int  read(@Nullable char[] buf, int off, int len) { return -1; }
        @Override public void close()                                      { ;          }
    };

    public static int
    readFully(Reader in, char[] buffer, int offset, int len) throws IOException {
    	int result = 0;
    	while (len > 0) {
    		int n = in.read(buffer, offset, len);
    		if (n == -1) return result == 0 ? -1 : result;
    		offset += n;
    		len    -= n;
    		result += n;
    	}
    	return result;
    }

    /**
     * @return All characters that the given {@link Reader} produces
     */
    public static String
    readAll(Reader reader) throws IOException {
        return Readers.readAll(reader, false);
    }

    /**
     * @param closeReader Whether the <var>reader</var> should be closed before the method returns
     * @return            All characters that the given {@link Reader} produces
     */
    public static String
    readAll(Reader reader, boolean closeReader) throws IOException {

        char[]        buf = new char[4096];
        StringBuilder sb  = new StringBuilder();

        try {

            for (;;) {
                int n = reader.read(buf);
                if (n == -1) break;
                sb.append(buf, 0, n);
            }

            if (closeReader) reader.close();

            return sb.toString();
        } finally {
            if (closeReader) {
                try { reader.close(); } catch (Exception e) {}
            }
        }
    }

    /**
     * Wraps the given {@link CharSequence} in a {@link Reader} - much more efficient than "{@code new
     * StringReader(cs.toString)}".
     */
    public static Reader
    asReader(final CharSequence cs) {
        return new Reader() {

            int pos;

            @Override public int
            read() { return this.pos >= cs.length() ? -1 : cs.charAt(this.pos++); }

            @Override public int
            read(@Nullable char[] cbuf, int off, int len) {
                assert cbuf != null;

                if (len <= 0) return 0;

                if (this.pos >= cs.length()) return -1;

                int end = cs.length();
                if (this.pos + len > end) {
                    len = end - this.pos;
                } else {
                    end = this.pos + len;
                }
                for (int i = this.pos; i < end; cbuf[off++] = cs.charAt(i++));
                return len;
            }

            @Override public void
            close() {}
        };
    }

    /**
     * @return A {@link Reader} for which {@link Reader#read(char[], int, int)} returns at most 1
     */
    public static Reader
    singlingFilterReader(Reader delegate) {

        return new FilterReader(delegate) {

            @NotNullByDefault(false) @Override public int
            read(char[] cbuf, int off, int len) throws IOException {
                return this.in.read(cbuf, off, len <= 0 ? 0 : 1);
            }

            @NotNullByDefault(false) @Override public int
            read(CharBuffer target) throws IOException {

                if (target.remaining() == 0) return 0;

                int c = this.read();
                if (c == -1) return -1;

                target.put((char) c);
                return 1;
            }
        };
    }

    /**
     * @return {@link FilterReader} that runs the <var>runnable</var> right before the first character is read
     */
    public static Reader
    onFirstChar(Reader in, final Runnable runnable) {

        return new FilterReader(in) {

            private boolean hadChars;

            @Override public int
            read() throws IOException {
                this.aboutToRead();
                return super.read();
            }

            @Override @NotNullByDefault(false) public int
            read(char[] cbuf, int off, int len) throws IOException {
                this.aboutToRead();
                return super.read(cbuf, off, len);
            }

            @Override public long
            skip(long n) throws IOException {
                this.aboutToRead();
                return super.skip(n);
            }

            private void
            aboutToRead() {
                if (!this.hadChars) {
                    runnable.run();
                    this.hadChars = true;
                }
            }
        };
    }

    /**
     * @return A {@link FilterReader} that tracks line and column numbers while characters are being read
     * @see    LineUtil#lineAndColumnTracker()
     */
    public static Reader
    trackLineAndColumn(Reader in, final LineAndColumnTracker tracker) {

        return new FilterReader(in) {

            @Override public int
            read() throws IOException {
                int c = super.read();
                if (c >= 0) tracker.consume((char) c);
                return c;
            }

            @Override @NotNullByDefault(false) public int
            read(char[] cbuf, final int off, int len) throws IOException {
                if (len <= 0) return 0;
                int c = this.read();
                if (c < 0) return -1;
                cbuf[off] = (char) c;
                return 1;
            }

            @Override public long
            skip(final long n) throws IOException {
                if (n <= 0) return 0;
                int c = this.read();
                if (c < 0) return 0;
                return 1;
            }

            @Override public boolean markSupported()                             { return false;            }
            @Override public void    mark(int readAheadLimit) throws IOException { throw new IOException(); }
            @Override public void    reset() throws IOException                  { throw new IOException(); }
        };
    }

    /**
     * Creates and returns a {@link Reader} that duplicates all chars read from <var>in</var> and writes them to
     * <var>out</var>.
     * <p>
     *   <var>Out</var> is never flushed automatically; if you want to have that, you must flush <var>out</var>
     *   yourself.
     * </p>
     * <p>
     *   {@link Reader#close()} closes <var>in</var> but does <em>not</em> flush nor close <var>out</var>.
     * </p>
     */
    public static Reader
    wye(Reader in, final Writer out) {

        return new FilterReader(in) {

            @Override public int
            read() throws IOException {
                int c = super.read();
                if (c >= 0) out.write(c);
                return c;
            }

            @Override public int
            read(@Nullable char[] cbuf, int off, int len) throws IOException {
                int count = super.read(cbuf, off, len);
                if (count > 0) out.write(cbuf, off, count);
                return count;
            }
        };
    }
}
