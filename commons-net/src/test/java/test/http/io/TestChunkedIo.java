
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package test.http.io;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.Test;

import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.net.http.io.ChunkedInputStream;
import de.unkrig.commons.net.http.io.ChunkedOutputStream;

public
class TestChunkedIo {

    @Test public void
    test() throws IOException {
        Random random = new Random(999L);

        byte[] bytes;
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < 1234567; i++) {
                baos.write(random.nextInt(256));
            }
            bytes = baos.toByteArray();
        }

        InputStream cis;
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChunkedOutputStream   cos  = new ChunkedOutputStream(baos);
            for (int off = 0; off < bytes.length;) {
                int n = 12 + random.nextInt(8000);
                if (off + n > bytes.length) n = bytes.length - off;
                cos.write(bytes, off, n);
                off += n;
            }
            cos.close();
            cis = new ChunkedInputStream(new ByteArrayInputStream(baos.toByteArray()));
        }

        assertArrayEquals(bytes, InputStreams.readAll(cis));
    }
}
