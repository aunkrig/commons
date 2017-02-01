
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.IoUtil.WritingRunnable;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF
// CHECKSTYLE JavadocVariable:OFF

public
class IoUtilTest {

    @Test public void
    testParallelSimple() throws IOException {
        StringWriter sw = new StringWriter();

        WritingRunnable[] writingRunnables = {
            new WritingRunnable() {

                @Override public void
                run(Writer w) throws Exception {
                    PrintWriter pw = new PrintWriter(w);
                    Thread.sleep(100L);
                    pw.println("WR1 ONE");
                    Thread.sleep(200L);
                    pw.println("WR1 TWO");
                    Thread.sleep(200L);
                    pw.println("WR1 THREE");
                }
            },
            new WritingRunnable() {

                @Override public void
                run(Writer w) throws Exception {
                    PrintWriter pw = new PrintWriter(w);
                    Thread.sleep(200L);
                    pw.println("WR2 ONE");
                    Thread.sleep(200L);
                    pw.println("WR2 TWO");
                    Thread.sleep(200L);
                    pw.println("WR2 THREE");
                }
            },
        };

        IoUtil.parallel(writingRunnables, sw);

        BufferedReader br    = new BufferedReader(new StringReader(sw.toString()));
        List<String>   lines = new ArrayList<String>();
        for (;;) {
            String line = br.readLine();
            if (line == null) break;
            lines.add(line);
        }

        Assert.assertEquals(Arrays.asList(new String[] {
            "WR1 ONE",
            "WR1 TWO",
            "WR1 THREE",
            "WR2 ONE",
            "WR2 TWO",
            "WR2 THREE",
        }), lines);
    }

    static final int NUMBER_OF_RUNNABLES = 2000;
    static final int NUMBER_OF_LINES     = 200;

    @Test public void
    testParallelMassive() throws IOException {

        StringWriter sw = new StringWriter();

        WritingRunnable[] writingRunnables = new WritingRunnable[IoUtilTest.NUMBER_OF_RUNNABLES];
        for (int i = 0; i < writingRunnables.length; i++) {
            final int ii = i;
            writingRunnables[i] = new WritingRunnable() {

                @Override public void
                run(Writer w) {
                    PrintWriter pw = new PrintWriter(w, true);
                    for (int j = 0; j < IoUtilTest.NUMBER_OF_LINES; j++) {
                        pw.println(ii + ":" + j);
//                        Thread.sleep(10L);
                    }
                }
            };
        }

        IoUtil.parallel(writingRunnables, sw);

        BufferedReader br    = new BufferedReader(new StringReader(sw.toString()));
        List<String>   lines = new ArrayList<String>();
        for (;;) {
            String line = br.readLine();
            if (line == null) break;
            lines.add(line);
        }

        List<String> expected = new ArrayList<String>();
        for (int i = 0; i < IoUtilTest.NUMBER_OF_RUNNABLES; i++) {
            for (int j = 0; j < IoUtilTest.NUMBER_OF_LINES; j++) {
                expected.add(i + ":" + j);
            }
        }
        Assert.assertEquals(expected, lines);
    }

    @Test public void
    findOnPathTest() throws IOException {
        Assert.assertEquals(
            new File(System.getProperty("user.dir")).toURI() + "target/test-classes/test/IoUtilTest.class",
            String.valueOf(IoUtil.findOnPath(
                new File[] { new File("target/test-classes") },
                this.getClass().getName().replace('.', '/') + ".class"
            ))
        );

        URL loc = IoUtil.findOnPath(
            new File[] { new File("../commons-lang/target/commons-lang-1.2.8-SNAPSHOT.jar") },
            "de/unkrig/commons/lang/AssertionUtil.class"
        );
        Assert.assertEquals(
            "jar:" + new File(System.getProperty("user.dir")).toURI() + "../commons-lang/target/commons-lang-1.2.9-SNAPSHOT.jar!/de/unkrig/commons/lang/AssertionUtil.class",
            String.valueOf(loc)
        );
        if (loc != null) loc.openStream().close();
    }
}
