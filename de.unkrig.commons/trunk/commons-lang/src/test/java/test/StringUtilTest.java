
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

package test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;
import de.unkrig.commons.lang.protocol.Producer;

// CHECKSTYLE Javadoc:OFF

public
class StringUtilTest {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    @Test public void
    testNewNaiveIndexOf() {

        String infix = "ABCDEFGHIJKLMNOP";

        IndexOf io = StringUtil.newNaiveIndexOf(infix);

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
    testNewKnuthMorrisPrattIndexOf() {

        String infix = "ABCDEFGHIJKLMNOP";

        IndexOf io = StringUtil.newKnuthMorrisPrattIndexOf(infix);

        Producer<String> rsp = StringUtilTest.randomSubjectProducer(infix);

        for (int j = 0; j < 100000; j++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            for (int offset = 0;; offset += subject.length()) {

                int o1 = subject.indexOf(infix, offset);
                int o2 = io.indexOf(subject, offset, subject.length());

                if (o1 != o2) {
                    o2 = io.indexOf(subject, offset, subject.length());
                    System.currentTimeMillis();
                }
                Assert.assertEquals("Subject=\"" + subject + "\"", o1, o2);

                if (o1 == -1) break;
            }
        }
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
