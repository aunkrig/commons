
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;
import de.unkrig.commons.lang.protocol.Producer;

public
class StringUtilTest {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    @Test public void
    testNewNaiveIndexOf() {

        String infix = "ABCDEFGHIJKLMNOP";

        IndexOf io = StringUtil.naiveIndexOf(infix);

        Producer<String> rsp = StringUtilTest.randomSubjectProducer(infix);

        for (int j = 0; j < 100000; j++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            for (int offset = 0;; offset += subject.length()) {

                int o1 = subject.indexOf(infix, offset);
                int o2 = io.indexOf(subject, offset, subject.length());

                Assert.assertEquals(o1, o2);

                if (o1 == -1) break;
            }
        }
    }

    @Test public void
    testBoyerMooreHorspoolIndexOf1() {

        String infix = "ABCDEFGHIJKLMNOP";

        IndexOf io = StringUtil.boyerMooreHorspoolIndexOf(infix);

        Producer<String> rsp = StringUtilTest.randomSubjectProducer(infix);

        for (int j = 0; j < 10000; j++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            for (
                int offset = -10;
                offset < subject.length() + 10;
                offset += offset == 100 ? subject.length() - 200 : 1
            ) {

                int i1 = subject.indexOf(infix, offset);
                int i2 = io.indexOf(subject, offset);

                if (i1 != i2) {
                    Assert.assertEquals(
                        "j=" + j + " " + io + ".indexOf(" + PrettyPrinter.toString(subject) + ", " + offset + ")",
                        i1,
                        i2
                    );
                }

                int li1 = subject.lastIndexOf(infix, offset);
                int li2 = io.lastIndexOf(subject, offset);

                if (li1 != li2) {
                    Assert.assertEquals(
                        "j=" + j + " " + io + ".lastIndexOf(" + PrettyPrinter.toString(subject) + ", " + offset + ")",
                        li1,
                        li2
                    );
                }
            }
        }
    }

    @Test public void
    testBoyerMooreHorspoolIndexOf2() {

        Random r = new Random(9);

        for (int i = 0; i < 1000; i++) {

            String infix;
            {
                StringBuilder sb = new StringBuilder();
                for (int j = 3 + r.nextInt(3); j > 0; j--) {
                    sb.append((char) ('A' + r.nextInt(5)));
                }
                infix = sb.toString();
            }

            for (int j = 0; j < 100; j++) {

                String subject;
                {
                    StringBuilder sb = new StringBuilder();
                    for (int jj = 100 + r.nextInt(100); jj >= 0; jj--) sb.append((char) ('A' + r.nextInt(8)));
                    subject = sb.toString();
                }

                IndexOf io = StringUtil.boyerMooreHorspoolIndexOf(infix);

                for (
                    int offset = -10;
                    offset < subject.length() + 10;
                    offset += offset == 10 ? subject.length() - 20 : 1
                ) {

                    int i1 = subject.indexOf(infix, offset);
                    int i2 = io.indexOf(subject, offset);

                    if (i1 != i2) {
                        Assert.assertEquals(
                            "i=" + i + ", j=" + j + " " + io + ".indexOf(" + PrettyPrinter.toString(subject) + ", " + offset + ")", // SUPPRESS CHECKSTYLE LineLength
                            i1,
                            i2
                        );
                    }

                    int li1 = subject.lastIndexOf(infix, offset);
                    int li2 = io.lastIndexOf(subject, offset);

                    if (li1 != li2) {
                        Assert.assertEquals(
                            "i=" + i + ", j=" + j + " " + io + ".lastIndexOf(" + PrettyPrinter.toString(subject) + ", " + offset + ")", // SUPPRESS CHECKSTYLE LineLength
                            li1,
                            li2
                        );
                    }
                }
            }
        }
    }

    @Test public void
    testBoyerMooreHorspoolIndexOf3() {

        Random r = new Random(9);

        for (int i = 0; i < 1000; i++) {

            String infix;
            {
                StringBuilder sb = new StringBuilder();
                for (int j = 3 + r.nextInt(3); j > 0; j--) {
                    sb.append((char) ('A' + r.nextInt(5)));
                }
                infix = sb.toString();
            }

            for (int j = 0; j < 100; j++) {

                String subject;
                {
                    StringBuilder sb = new StringBuilder();
                    for (int jj = 100 + r.nextInt(100); jj >= 0; jj--) sb.append((char) ('A' + r.nextInt(8)));
                    subject = sb.toString();
                }

                IndexOf io = StringUtil.boyerMooreHorspoolIndexOf(infix);

                for (int offset = 0; offset < subject.length(); offset++) {

                    int o1 = subject.lastIndexOf(infix, offset);
                    int o2 = io.lastIndexOf(subject, offset);

                    if (o1 != o2) {
                        Assert.assertEquals(
                            "i=" + i + ", j=" + j + " " + io + ".lastIndexOf(" + PrettyPrinter.toString(subject) + ", " + offset + ")", // SUPPRESS CHECKSTYLE LineLength
                            o1,
                            o2
                        );
                    }
                }
            }
        }
    }

    @Test public void
    testBoyerMooreHorspoolIndexOf4() {

        String       infix   = "ABAB";
        final String subject = "      ABA AB ABAB  ";

        IndexOf io = StringUtil.boyerMooreHorspoolIndexOf(infix);

        final StringBuilder sb       = new StringBuilder();
        CharSequence        subject2 = new CharSequence() {

            @Override public CharSequence
            subSequence(int start, int end) {
                sb.append(" subSequence(").append(start).append(", ").append(end).append(")");
                return subject.subSequence(start, end);
            }

            @Override public int
            length() {
                sb.append(" length()");
                return subject.length();
            }

            @Override public char
            charAt(int index) {
                sb.append(" charAt(").append(index).append(')');
                return subject.charAt(index);
            }
        };

        sb.setLength(0);
        Assert.assertEquals(13, io.indexOf(subject2));
        Assert.assertEquals((
            ""
            + " length() length()"
            + " charAt(3) charAt(7) charAt(6) charAt(5) charAt(8)"
            + " charAt(9) charAt(13) charAt(14) charAt(13) charAt(12)"
            + " charAt(15) charAt(16) charAt(15) charAt(14) charAt(13)"
        ), sb.toString());

        sb.setLength(0);
        Assert.assertEquals(13, io.lastIndexOf(subject2));
        Assert.assertEquals((
            " length()"
            + " charAt(15) charAt(16) charAt(17) charAt(14) charAt(13)"
            + " charAt(14) charAt(15) charAt(16)"
        ), sb.toString());
    }

    @Test public void
    testBoyerMooreHorspoolIndexOfLimit1() {
        String  needle = "ABCDE";
        IndexOf io     = StringUtil.boyerMooreHorspoolIndexOf(needle);

        // Variations on the haystack.
        Assert.assertEquals(1,  io.indexOf("_ABCDE_**",   1,   1, 7));
        Assert.assertEquals(2,  io.indexOf("__ABCDE**",   2,   2, 7));
        Assert.assertEquals(3,  io.indexOf("___ABCD**",   3,   3, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   4,   4, 7));
        Assert.assertEquals(5,  io.indexOf("_____AB**",   5,   5, 7));
        Assert.assertEquals(6,  io.indexOf("______A**",   6,   6, 7));
        Assert.assertEquals(-1, io.indexOf("_______**",   7,   7, 7));

        // Variations on the limit.
        Assert.assertEquals(-1, io.indexOf("_ABC___**", -99,  99, 7));
        Assert.assertEquals(-1, io.indexOf("_ABC__***", -99,  99, 6));
        Assert.assertEquals(-1, io.indexOf("_ABC_****", -99,  99, 5));
        Assert.assertEquals(1,  io.indexOf("_ABC*****", -99,  99, 4));
        Assert.assertEquals(1,  io.indexOf("_AB******", -99,  99, 3));
        Assert.assertEquals(1,  io.indexOf("_A*******", -99,  99, 2));
        Assert.assertEquals(-1, io.indexOf("_********", -99,  99, 1));

        // Variations on the minIndex.
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,  99, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   0,  99, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   1,  99, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   2,  99, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   3,  99, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**",   4,  99, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**",   5,  99, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**",   6,  99, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**",   7,  99, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**",  99,  99, 7));

        // Variations on the maxIndex.
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99, -99, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99,  -1, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99,   0, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99,   1, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99,   2, 7));
        Assert.assertEquals(-1, io.indexOf("____ABC**", -99,   3, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   4, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   5, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   6, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   7, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   8, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,   9, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,  10, 7));
        Assert.assertEquals(4,  io.indexOf("____ABC**", -99,  99, 7));
    }

    @Test public void
    testBoyerMooreHorspoolIndexOfLimit2() {
        String  needle = "ABCABCDE";
        IndexOf io     = StringUtil.boyerMooreHorspoolIndexOf(needle);

        Assert.assertEquals(1,  io.indexOf("_ABCABCDE_**", 1, 1, 10));
        Assert.assertEquals(2,  io.indexOf("__ABCABCDE**", 2, 2, 10));
        Assert.assertEquals(3,  io.indexOf("___ABCABCD**", 3, 3, 10));
        Assert.assertEquals(4,  io.indexOf("____ABCABC**", 4, 4, 10));
        Assert.assertEquals(5,  io.indexOf("_____ABCAB**", 5, 5, 10));
        Assert.assertEquals(6,  io.indexOf("______ABCA**", 6, 6, 10));
        Assert.assertEquals(-1, io.indexOf("_______ABC**", 7, 7, 10));
        Assert.assertEquals(-1, io.indexOf("________AB**", 8, 8, 10));
        Assert.assertEquals(-1, io.indexOf("_________A**", 9, 9, 10));
        Assert.assertEquals(-1, io.indexOf("__________**", 7, 7, 10));
    }

    @Test public void
    testIndexOf1() throws Throwable {

        String infix = "ABC";

        for (IndexOf io : new IndexOf[] {
            StringUtil.naiveIndexOf(infix),
            StringUtil.boyerMooreHorspoolIndexOf(infix),
        }) {

            try {
                Assert.assertEquals(3,  io.indexOf("   ABC ABC   "));

                Assert.assertEquals(3,  io.indexOf("   ABC ABC   ", 1));
                Assert.assertEquals(3,  io.indexOf("   ABC ABC   ", 2));
                Assert.assertEquals(3,  io.indexOf("   ABC ABC   ", 3));
                Assert.assertEquals(7,  io.indexOf("   ABC ABC   ", 4));

                Assert.assertEquals(7,  io.indexOf("   ABC ABC   ", 4, 10));
                Assert.assertEquals(7,  io.indexOf("   ABC ABC   ", 4, 9));
                Assert.assertEquals(7,  io.indexOf("   ABC ABC   ", 4, 8));
                Assert.assertEquals(7,  io.indexOf("   ABC ABC   ", 4, 7));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 6));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 5));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 4));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 3));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 2));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 1));
                Assert.assertEquals(-1, io.indexOf("   ABC ABC   ", 4, 0));
            } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                throw ExceptionUtil.wrap(io.toString(), t);
            }
        }
    }

    @Test public void
    testIndexOf2() {
        Random random = new Random(123);

        String infix = "ABC";

        IndexOf[] ios = {
            StringUtil.naiveIndexOf(infix),
            StringUtil.boyerMooreHorspoolIndexOf(infix),
        };

        for (int j = 0; j < 100000; j++) {

            String subject;
            {
                StringBuilder sb = new StringBuilder();
                for (int k = 10 + random.nextInt(10); k > 0; k--) {
                    sb.append((char) ('A' + random.nextInt(4)));
                }
                subject = sb.toString();
            }

            for (int i = 0;; i++) {
                int index0 = ios[0].indexOf(subject, i);
                for (int k = 1; k < ios.length; k++) {
                    int indexK = ios[k].indexOf(subject, i);
                    Assert.assertEquals(
                        "Iteration #" + j + ": " + ios[k] + ".indexOf(" + PrettyPrinter.toString(subject) + ", " + i + ")", // SUPPRESS CHECKSTYLE LineLength
                        index0,
                        indexK
                    );
                }
                if (index0 == -1) break;
            }
        }
    }

    @Test public void
    testIndexOf3() {
        Random random = new Random(123);

        for (int i = 0; i < 100; i++) {

            String subject;
            {
                StringBuilder sb = new StringBuilder();
                for (int j = 100 + random.nextInt(10); j > 0; j--) {
                    sb.append((char) ('A' + random.nextInt(6)));
                }
                subject = sb.toString();
            }

            for (IndexOf io : new IndexOf[] {
                StringUtil.naiveIndexOf("ABCDE"),
                StringUtil.boyerMooreHorspoolIndexOf("ABCDE"),
            }) {
//                long start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    for (int k = 0;; k++) {
                        k = io.indexOf(subject, k);
                        if (k == -1) break;
                    }
                }
//                long end = System.nanoTime();
//                System.out.printf(Locale.US, "%-37s %,d ns%n", io + ".indexOf()", end - start);
            }
        }
    }

    @Test public void
    testLastIndexOf1() throws Throwable {

        String infix = "ABC";

        for (IndexOf io : new IndexOf[] {
            StringUtil.naiveIndexOf(infix),
            StringUtil.boyerMooreHorspoolIndexOf(infix),
        }) {

            try {
                Assert.assertEquals(7,  io.lastIndexOf("   ABC ABC   "));

                Assert.assertEquals(7,  io.lastIndexOf("   ABC ABC   ", 8));
                Assert.assertEquals(7,  io.lastIndexOf("   ABC ABC   ", 7));
                Assert.assertEquals(3,  io.lastIndexOf("   ABC ABC   ", 6));

                Assert.assertEquals(7,  io.lastIndexOf("   ABC ABC   ", 0, 8));
                Assert.assertEquals(7,  io.lastIndexOf("   ABC ABC   ", 0, 7));
                Assert.assertEquals(3,  io.lastIndexOf("   ABC ABC   ", 0, 6));

                Assert.assertEquals(3,  io.lastIndexOf("   ABC ABC   ", 3, 6));
                Assert.assertEquals(-1, io.lastIndexOf("   ABC ABC   ", 4, 6));
            } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                throw ExceptionUtil.wrap(io.toString(), t);
            }
        }
    }

    @Test public void
    testLastIndexOf2() {

        String infix = "ABC";

        IndexOf io1 = StringUtil.naiveIndexOf(infix);
        IndexOf io2 = StringUtil.boyerMooreHorspoolIndexOf(infix);

        Random random = new Random(123);

        for (int j = 0; j < 100000; j++) {
            StringBuilder sb = new StringBuilder();
            for (int k = 10 + random.nextInt(10); k > 0; k--) {
                sb.append((char) ('A' + random.nextInt(4)));
            }
            String subject = sb.toString();
            for (int i = Integer.MAX_VALUE;; i--) {
                int i1 = io1.lastIndexOf(subject, i);
                int i2 = io2.lastIndexOf(subject, i);
                Assert.assertEquals(
                    "Iteration #" + j + ", \"" + subject + "\".lastindexOf(\"" + infix + "\", " + i + ")",
                    i1,
                    i2
                );
                if (i1 == -1) break;
                i = i1;
            }
        }
    }

    @Test public void
    testLastIndexOf3() {
        Random random = new Random(123);

        for (int i = 0; i < 100; i++) {

            String subject;
            {
                StringBuilder sb = new StringBuilder();
                for (int j = 100 + random.nextInt(10); j > 0; j--) {
                    sb.append((char) ('A' + random.nextInt(6)));
                }
                subject = sb.toString();
            }

            for (IndexOf io : new IndexOf[] {
                StringUtil.naiveIndexOf("ABCDE"),
                StringUtil.boyerMooreHorspoolIndexOf("ABCDE"),
            }) {
//                long start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    for (int k = Integer.MAX_VALUE;; k--) {
                        k = io.lastIndexOf(subject, k);
                        if (k == -1) break;
                    }
                }
//                long end = System.nanoTime();
//                System.out.printf(Locale.US, "%-45s %,d ns%n", io + ".lastIndexOf()", end - start);
            }
        }
    }

    @Test public void
    testLastIndexOf4() {
        int lidx = StringUtil.boyerMooreHorspoolIndexOf("ABCDEFGHIJKLMNOP").lastIndexOf(
            "ABCDEFGLMNOPABCDEFGHIJCDEFGHIJKLMNOPABHIJKLMNOPAIJKLMNOPABCDEFGHIJKLMXXXXXXXXXXXXXXXXXXXXXXOPXXXXXXX"
            + "XXXXXXXXXXXXXXXXXABCDEFGHIJXXXXXXXXXXXXXXXXXXXGHIJKLMNOPABCDEFGHIJKEFGHIJKLMNOPABCDEFGHXXXXXXXXXXXXX"
            + "XXXXXXXXXXXXXXXXBCDEFGHIJKLMNOPXXXXXXXXXXXXXXXXXXXXAMNOPABCDEFGHIJHIJKLMNOPABCDEFGHIGHIJKLMNOPXXXXXX"
            + "XXXXXXXXXXXXXXXXXABCDEFGHIHIJKLMNOPABJKLMNOPABCDEFGHIJKLMXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXEFGHIJKLMNOP"
            + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXABXXXXXXXXXXXXXXXXXXXXXXXXXXMNOPXXXXXXXXABCDEFGXXXXXXXXXXXXXXXXXXXXXX"
            + "XXXXXXXEFGHIJKLMNOPXXXXXXXXXXXXXXXXXXXXXXXXXXXXABCDEFGHIJKEFGHIJKLMNOPABCDEFGXXXLMNOPXXXXXXXXXXXXXXX"
            + "XXXABCDEFGHIJKXXXXXXXXXXXXXXXXCDEFGHIJKLMNOPXABCDEFXIJKLMNOP"
        );
        Assert.assertEquals(-1, lidx);
    }

    private static Producer<String>
    randomSubjectProducer(final String infix) {

        return new Producer<String>() {

            Random r = new Random(123);

            @Override public String
            produce() {

                StringBuilder subject = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    for (int j = this.r.nextInt(64) - 32; j > 0; j--) subject.append('X');
                    subject.append(infix, 0, this.r.nextInt(infix.length() + 1));
                    for (int j = this.r.nextInt(64) - 32; j > 0; j--) subject.append('X');
                    subject.append(infix, this.r.nextInt(infix.length()), infix.length());
                }

                return subject.toString();
            }
        };
    }
}
