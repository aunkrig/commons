
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.ByteFilter;
import de.unkrig.commons.io.ByteFilterOutputStream;
import de.unkrig.commons.nullanalysis.Nullable;

public
class ByteFilterOutputStreamTest {

    @Test public void
    test1() throws IOException {

        // This ByteFilter changes each '3' to a '99.
        ByteFilter<Void> bf = new ByteFilter<Void>() {

            @Override @Nullable public Void
            run(InputStream in, OutputStream out) throws IOException {
                for (;;) {
                    int b = in.read();
                    if (b == -1) break;
                    if (b == 3) b = 99;
                    out.write(b);
                }
                return null;
            }
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final boolean[] wasClosed = new boolean[1];

        OutputStream fos = new FilterOutputStream(baos) {
            @Override public void close() throws IOException { wasClosed[0] = true; super.close(); }
        };

        OutputStream os = new ByteFilterOutputStream(bf, fos);

        os.write(1);
        os.write(2);
        os.write(3);
        os.write(4);
        os.write(5);

        Assert.assertFalse(wasClosed[0]);
        os.close();
        Assert.assertTrue(wasClosed[0]);

        Assert.assertArrayEquals(new byte[] { 1, 2, 99, 4, 5 }, baos.toByteArray());
    }
}
