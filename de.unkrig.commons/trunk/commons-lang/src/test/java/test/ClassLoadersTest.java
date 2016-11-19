
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

package test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.ClassLoaders;

public
class ClassLoadersTest {

    private static final byte[]
    CLASS_FILE_MAGIC_BYTES = new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe };

    @Test public void
    testGetAllSubresources() throws Exception {

        URL[] resources = ClassLoaders.getAllSubresources(null, "test/", true);
        for (URL url : resources) {
            if (url.toString().endsWith(
                "/commons-lang/target/test-classes/" + ClassLoadersTest.class.getName().replace('.', '/') + ".class"
            )) {
                ClassLoadersTest.assertContentsBeginsWith(url, ClassLoadersTest.CLASS_FILE_MAGIC_BYTES);
                return;
            }
        }

        Assert.fail(Arrays.toString(resources));
    }

    @Test public void
    testGetSubresources() throws Exception {

        Map<String, URL> m = ClassLoaders.getSubresources(null, "test/", true);

        String resourceName = this.getClass().getName().replace('.', '/') + ".class";
        URL url = m.get(resourceName);
        Assert.assertNotNull(m + ": " + resourceName, url);
        ClassLoadersTest.assertContentsBeginsWith(url, ClassLoadersTest.CLASS_FILE_MAGIC_BYTES);
    }

    private static void
    assertContentsBeginsWith(URL url, byte[] expected) throws IOException {
        InputStream is = url.openStream();
        try {
            byte[] ba = new byte[4];
            is.read(ba);
            Assert.assertArrayEquals(expected, ba);
            is.close();
            return;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }
}
