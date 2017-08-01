
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

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;

// CHECKSTYLE Javadoc:OFF

public
class StringUtilTest {

    @Test public void
    testIndexOf() {

        String infix = "ABCDEFGHIJKLMNOP";
        Random r     = new Random(123);

        // Compose a LONG subject string.
        StringBuilder subject = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            for (int j = r.nextInt(64) - 32; j > 0; j--) subject.append('X');
            subject.append(infix, 0, r.nextInt(infix.length() + 1));
            for (int j = r.nextInt(64) - 32; j > 0; j--) subject.append('X');
            subject.append(infix, r.nextInt(infix.length()), infix.length());
        }

        IndexOf io = StringUtil.newIndexOf(infix);

        for (int offset = 0;; offset += subject.length()) {

            int o1 = subject.indexOf(infix, offset);
            int o2 = io.indexOf(subject, offset, subject.length());

            Assert.assertEquals(o1, o2);

            if (o1 == -1) break;
        }
    }
}
