
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2018, Arno Unkrig
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.io.LineUtil.LineAndColumnTracker;
import de.unkrig.commons.io.Readers;

public
class ReadersTest {

    @Test public void
    testOnFirstChar() throws IOException {

        final int[] count = new int[1];
        Reader      r     = Readers.onFirstChar(
            new StringReader("ABC"),
            new Runnable() { @Override public void run() { count[0]++; } }
        );

        Assert.assertEquals(0, count[0]);
        Assert.assertEquals('A', r.read());
        Assert.assertEquals(1, count[0]);
        Assert.assertEquals('B', r.read());
        Assert.assertEquals(1, count[0]);
        Assert.assertEquals('C', r.read());
        Assert.assertEquals(1, count[0]);
        Assert.assertEquals(-1, r.read());
        Assert.assertEquals(1, count[0]);
    }

    @Test public void
    testLineAndColumnTracking() throws IOException {

        LineAndColumnTracker tracker = LineUtil.lineAndColumnTracker();
        Reader               r       = Readers.trackLineAndColumn(new StringReader("A\rB\r\nC\nD\u0085E\tF"), tracker);

        Assert.assertEquals(1, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('A', r.read());
        Assert.assertEquals(1, tracker.getLineNumber());
        Assert.assertEquals(2, tracker.getColumnNumber());
        Assert.assertEquals('\r', r.read());
        Assert.assertEquals(2, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('B', r.read());
        Assert.assertEquals(2, tracker.getLineNumber());
        Assert.assertEquals(2, tracker.getColumnNumber());
        Assert.assertEquals('\r', r.read());
        Assert.assertEquals(3, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('\n', r.read());
        Assert.assertEquals(3, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('C', r.read());
        Assert.assertEquals(3, tracker.getLineNumber());
        Assert.assertEquals(2, tracker.getColumnNumber());
        Assert.assertEquals('\n', r.read());
        Assert.assertEquals(4, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('D', r.read());
        Assert.assertEquals(4, tracker.getLineNumber());
        Assert.assertEquals(2, tracker.getColumnNumber());
        Assert.assertEquals('\u0085', r.read());
        Assert.assertEquals(5, tracker.getLineNumber());
        Assert.assertEquals(1, tracker.getColumnNumber());
        Assert.assertEquals('E', r.read());
        Assert.assertEquals(5, tracker.getLineNumber());
        Assert.assertEquals(2, tracker.getColumnNumber());
        Assert.assertEquals('\t', r.read());
        Assert.assertEquals(5, tracker.getLineNumber());
        Assert.assertEquals(9, tracker.getColumnNumber());
        Assert.assertEquals('F', r.read());
        Assert.assertEquals(5, tracker.getLineNumber());
        Assert.assertEquals(10, tracker.getColumnNumber());
        Assert.assertEquals(-1, r.read());
        Assert.assertEquals(5, tracker.getLineNumber());
        Assert.assertEquals(10, tracker.getColumnNumber());
    }
}
