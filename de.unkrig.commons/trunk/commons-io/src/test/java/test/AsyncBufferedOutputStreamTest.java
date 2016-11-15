
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

package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.AsyncBufferedOutputStream;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF

public
class AsyncBufferedOutputStreamTest {

//    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final int N = 3333333;

    @Test public void
    test() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        System.gc();
//        System.out.println(RUNTIME.totalMemory() - RUNTIME.freeMemory());
        AsyncBufferedOutputStream abos = new AsyncBufferedOutputStream(baos, ByteBuffer.allocateDirect(1000000), false);
//        System.gc();
//        System.out.println(RUNTIME.totalMemory() - RUNTIME.freeMemory());

//        int[] sizes = new int[N];
        for (int i = 0; i < AsyncBufferedOutputStreamTest.N; i++) {
            abos.write(i);
            int size = baos.size();
//            sizes[i] = size;
            Assert.assertTrue(size <= i + 1);
        }
        abos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        for (int i = 0; i < AsyncBufferedOutputStreamTest.N; i++) {
            Assert.assertEquals(0xff & i, bais.read());
        }
        Assert.assertEquals(-1, bais.read());
//        for (int i = 0; i < N; i++) {
//            System.out.println(i + ": " + sizes[i]);
//        }
    }
}
