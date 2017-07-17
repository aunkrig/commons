
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility functionality related to {@link InputStream}s.
 */
public final
class InputStreams {

    private InputStreams() {}

    /** An {@link InputStream} that produces exactly 0 bytes. */
    public static final InputStream
    EMPTY = new InputStream() {
        @Override public int read()                                       { return -1; }
        @Override public int read(@Nullable byte[] buf, int off, int len) { return -1; }
    };

    /**
     * An input stream that reads an endless stream of zeros.
     */
    public static final InputStream
    ZERO = InputStreams.constantInputStream((byte) 0);

    /**
     * Creates and returns a {@link FilterInputStream} that duplicates all bytes read through it and writes them to
     * an {@link OutputStream}.
     * <p>
     *   The {@link OutputStream} is flushed on end-of-input and calls to {@link InputStream#available()}.
     * </p>
     */
    public static InputStream
    wye(InputStream in, final OutputStream out) {

        return new FilterInputStream(in) {

            @Override public int
            read() throws IOException {
                int b = super.read();
                if (b == -1) {
                    out.flush();
                } else {
                    out.write(b);
                }
                return b;
            }

            @Override public int
            read(@Nullable byte[] b, int off, int len) throws IOException {
                int count = super.read(b, off, len);
                if (count > 0) out.write(b, off, count);
                if (count == -1) out.flush();
                return count;
            }


            @Override public int
            available() throws IOException {
                out.flush();
                return this.in.available();
            }
        };
    }

    /**
     * Equivalent with {@code readAll(is, false)}.
     */
    public static byte[]
    readAll(InputStream is) throws IOException { return InputStreams.readAll(is, false); }

    /**
     * @param closeInputStream Whether to close the inputStream (also if an {@link IOException} is thrown)
     * @return                 All bytes that the given {@link InputStream} produces
     */
    public static byte[]
	readAll(InputStream is, boolean closeInputStream) throws IOException {

    	ByteArrayOutputStream baos = new ByteArrayOutputStream();

    	IoUtil.copy(
			is,               // inputStream
			closeInputStream, // closeInputStream
			baos,             // outputStream
			false             // closeOutputStream
		);

    	return baos.toByteArray();
    }

    /**
     * @return All bytes that the given {@link InputStream} produces, decoded into a string
     */
    public static String
    readAll(InputStream inputStream, Charset charset, boolean closeInputStream) throws IOException {

        StringWriter sw = new StringWriter();
        IoUtil.copy(
            new InputStreamReader(inputStream, charset),
            closeInputStream,
            sw,
            false
        );
        return sw.toString();
    }

    /**
     * Skips <var>n</var> bytes on the <var>inputStream</var>. Notice that {@link InputStream#skip(long)} sometimes
     * skips less than the requested number of bytes for no good reason, e.g. {@link BufferedInputStream#skip(long)}
     * is known for that. <em>This</em> method tries "harder" to skip exactly the requested number of bytes.
     *
     * @return                     The number of bytes that were skipped
     * @see InputStream#skip(long)
     */
    public static long
    skip(InputStream inputStream, long n) throws IOException {

        long result = 0;
        while (result < n) {
            long skipped = inputStream.skip(n - result);
            if (skipped == 0) return result;
            result += skipped;
        }

        return result;
    }

    /**
     * Skips all remaining data on the <var>inputStream</var>.
     *
     * @return                     The number of bytes that were skipped
     * @see InputStream#skip(long)
     */
    public static long
    skipAll(InputStream inputStream) throws IOException {

        long result = 0;
        for (;;) {
            long skipped = inputStream.skip(Long.MAX_VALUE);
            if (skipped == 0) return result;
            result += skipped;
        }
    }

    /**
     * @return An input stream that reads an endless stream of bytes of value <var>b</var>
     */
    public static InputStream
    constantInputStream(final byte b) {
        return new InputStream() {

            @Override public int
            read() { return 0; }

            @Override public int
            read(@Nullable byte[] buf, int off, int len) { Arrays.fill(buf,  off, len, b); return len; }
        };
    }

    /**
     * @return An {@link InputStream} which ignores all invocations of {@link InputStream#close()}
     */
    public static InputStream
    unclosable(InputStream delegate) {

        return new FilterInputStream(delegate) { @Override public void close() {} };
    }

    /**
     * @return An input stream which reads the data produced by the <var>delegate</var> byte producer; {@code null}
     *         products are returned as 'end-of-input'
     */
    public static InputStream
    byteProducerInputStream(final ProducerWhichThrows<? extends Byte, ? extends IOException> delegate) {

        return new InputStream() {

            @Override public int
            read() throws IOException {
                Byte b = delegate.produce();
                return b != null ? 0xff & b : -1;
            }
        };
    }

    /**
     * @return An input stream which reads the data produced by the <var>delegate</var> byte producer; {@code null}
     *         products are returned as 'end-of-input'
     */
    public static InputStream
    byteProducerInputStream(final Producer<? extends Byte> delegate) {

        return InputStreams.byteProducerInputStream(ProducerUtil.<Byte, IOException>asProducerWhichThrows(delegate));
    }

    /**
     * @return An input stream which reads and endless stream of random bytes
     */
    public static InputStream
    randomInputStream(final long seed) {

        return InputStreams.byteProducerInputStream(ProducerUtil.randomByteProducer(seed));
    }

    /**
     * @return An {@link InputStream} which first closes the <var>delegate</var>, and then attempts to delete the
     *         <var>file</var>
     */
    protected static InputStream
    deleteOnClose(InputStream delegate, final File file) {

        return new FilterInputStream(delegate) {

            @Override public void
            close() throws IOException {
                super.close();
                file.delete();
            }
        };
    }

    /**
     * Creates and returns an {@link InputStream} that reads from the <var>delegate</var> and invokes the
     * <var>runnable</var> on the <em>first</em> end-of-input condition.
     */
    public static InputStream
    onEndOfInput(InputStream delegate, final Runnable runnable) {

        return new FilterInputStream(delegate) {

            boolean hadEndOfInput;

            @Override public int
            read() throws IOException {

                int b = super.read();

                if (b == -1 && !this.hadEndOfInput) {
                    this.hadEndOfInput = true;
                    runnable.run();
                }

                return b;
            }

            @Override public int
            read(@Nullable byte[] b, int off, int len) throws IOException {

                int count = super.read(b, off, len);

                if (count == -1 && !this.hadEndOfInput) {
                    this.hadEndOfInput = true;
                    runnable.run();
                }

                return count;
            }
        };
    }

    /**
     * @return An {@link InputStream} for which {@link InputStream#read(byte[], int, int)} returns at most 1
     */
    public static InputStream
    singlingFilterInputStream(InputStream delegate) {

        return new FilterInputStream(delegate) {

            @NotNullByDefault(false) @Override public int
            read(byte[] cbuf, int off, int len) throws IOException {
                return this.in.read(cbuf, off, len <= 0 ? 0 : 1);
            }
        };
    }


}
