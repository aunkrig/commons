
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

package test;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.OutputStreams;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF
// CHECKSTYLE JavadocVariable:OFF

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
}
