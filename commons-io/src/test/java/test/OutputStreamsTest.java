
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

package test;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.OutputStreams;

public
class OutputStreamsTest {

    @Test public void
    testCompareOutput1() throws IOException {

        final boolean[] definitelyIdentical    = new boolean[1];
        final boolean[] definitelyNotIdentical = new boolean[1];

        OutputStream[] oss = OutputStreams.compareOutput(
            2,
            new Runnable() { @Override public void run() { definitelyIdentical[0]    = true; } },
            new Runnable() { @Override public void run() { definitelyNotIdentical[0] = true; } }
        );

        oss[0].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[0].close();
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[1].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[1].close();
        Assert.assertTrue(definitelyIdentical[0] && !definitelyNotIdentical[0]);
    }

    @Test public void
    testCompareOutput2() throws IOException {

        final boolean[] definitelyIdentical    = new boolean[1];
        final boolean[] definitelyNotIdentical = new boolean[1];

        OutputStream[] oss = OutputStreams.compareOutput(
            2,
            new Runnable() { @Override public void run() { definitelyIdentical[0]    = true; } },
            new Runnable() { @Override public void run() { definitelyNotIdentical[0] = true; } }
        );

        oss[0].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[1].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 99 });
        Assert.assertTrue(!definitelyIdentical[0]);
        oss[0].close();
        Assert.assertTrue(!definitelyIdentical[0]);
        oss[1].close();
        Assert.assertTrue(!definitelyIdentical[0] && definitelyNotIdentical[0]);
    }

    @Test public void
    testCompareOutput3() throws IOException {

        final boolean[] definitelyIdentical    = new boolean[1];
        final boolean[] definitelyNotIdentical = new boolean[1];

        OutputStream[] oss = OutputStreams.compareOutput(
            2,
            new Runnable() { @Override public void run() { definitelyIdentical[0]    = true; } },
            new Runnable() { @Override public void run() { definitelyNotIdentical[0] = true; } }
        );

        oss[0].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[1].write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        Assert.assertTrue(!definitelyIdentical[0] && !definitelyNotIdentical[0]);
        oss[0].close();
        Assert.assertTrue(!definitelyIdentical[0]);
        oss[1].close();
        Assert.assertTrue(!definitelyIdentical[0] && definitelyNotIdentical[0]);
    }

    // ===========================================

    private static File   file   = new File("foo");
    private static Random random = new Random();

    @Test public void
    testOverwriteFilesEqual() throws Exception {
        byte[] data = OutputStreamsTest.randomData(100);
        OutputStreamsTest.writeToFile(OutputStreamsTest.file, data);
        OutputStreamsTest.overwrite(OutputStreamsTest.file, data);
        OutputStreamsTest.assertFileContents(OutputStreamsTest.file, data);
    }

    @Test public void
    testOverwriteOldFileLonger() throws Exception {
        byte[] data = OutputStreamsTest.randomData(100), data2 = OutputStreamsTest.randomData(20);
        OutputStreamsTest.writeToFile(OutputStreamsTest.file, data, data2);
        OutputStreamsTest.overwrite(OutputStreamsTest.file, data);
        OutputStreamsTest.assertFileContents(OutputStreamsTest.file, data);
    }

    @Test public void
    testOverwriteOldFileShorter() throws Exception {
        byte[] data = OutputStreamsTest.randomData(100), data2 = OutputStreamsTest.randomData(20);
        OutputStreamsTest.writeToFile(OutputStreamsTest.file, data);
        OutputStreamsTest.overwrite(OutputStreamsTest.file, data, data2);
        OutputStreamsTest.assertFileContents(OutputStreamsTest.file, data, data2);
    }

    @Test public void
    testOverwriteFilesDiffer() throws Exception {
        byte[] data = OutputStreamsTest.randomData(100), data2 = OutputStreamsTest.randomData(20), data3 = OutputStreamsTest.randomData(30);
        OutputStreamsTest.writeToFile(OutputStreamsTest.file, data, data2);
        OutputStreamsTest.overwrite(OutputStreamsTest.file, data, data3);
        OutputStreamsTest.assertFileContents(OutputStreamsTest.file, data, data3);
    }

    @Test public void
    testOverwriteFilesDiffer2() throws Exception {
        byte[] data = OutputStreamsTest.randomData(100), data2 = OutputStreamsTest.randomData(20), data3 = OutputStreamsTest.randomData(30);
        OutputStreamsTest.writeToFile(OutputStreamsTest.file, OutputStreamsTest.cat(data, data2));
        OutputStreamsTest.overwrite(OutputStreamsTest.file, OutputStreamsTest.cat(data, data3));
        OutputStreamsTest.assertFileContents(OutputStreamsTest.file, data, data3);
    }

    // ====================================================

    private static byte[]
    cat(byte[] ba1, byte[] ba2) {

        int len1 = ba1.length, len2 = ba2.length;

        byte[] result = new byte[len1 + len2];
        System.arraycopy(ba1, 0, result, 0,    len1);
        System.arraycopy(ba2, 0, result, len1, len2);
        return result;
    }

    private static void
    overwrite(File file, byte[]... data) throws IOException {
        OutputStream os = OutputStreams.newOverwritingFileOutputStream(file);
        try {
            for (byte[] ba : data) os.write(ba);
        } finally {
            os.close();
        }
    }

    private static void
    writeToFile(File file, byte[]... data) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            for (byte[] ba : data) os.write(ba);
        } finally {
            os.close();
        }
    }

    private static void
    assertFileContents(File file, byte[]... expected) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        try {
            for (byte[] ba : expected) {
                byte[] buf = new byte[ba.length];
                try {
                    dis.readFully(buf);
                } catch (EOFException eofe) {
                    int x = 0;
                    for (byte[] ba2 : expected) x += ba2.length;
                    throw new AssertionError("File \"" + file + "\" shorter (" + file.length() + ") than expected (" + x + ")", eofe);
                }
                Assert.assertArrayEquals(ba, buf);
            }
        } finally {
            dis.close();
        }
    }

    private static byte[]
    randomData(int n) {
        byte[] result = new byte[n];
        OutputStreamsTest.random.nextBytes(result);
        return result;
    }
}
