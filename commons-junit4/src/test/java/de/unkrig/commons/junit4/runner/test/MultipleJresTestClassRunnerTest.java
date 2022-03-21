
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2019, Arno Unkrig
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

package de.unkrig.commons.junit4.runner.test;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.unkrig.commons.junit4.AssertRegex;
import de.unkrig.commons.junit4.runner.JavaHomes;
import de.unkrig.commons.junit4.runner.MultipleJresTestClassRunner;

@Ignore
@RunWith(MultipleJresTestClassRunner.class)
@JavaHomes({
    "c:/Program Files/Java/jdk1.6.0_43",
    "c:/Program Files/Java/jdk1.7.0_17",
    "c:/Program Files/Java/jdk1.8.0_192",
    "c:/Program Files/Java/jdk-9.0.4",
    "c:/Program Files/Java/jdk-10.0.2",
    "c:/Program Files/Java/jdk-11.0.1",
})
public
class MultipleJresTestClassRunnerTest {

    @Test public void
    test1() {
        AssertRegex.assertMatches("1\\.6|1\\.7|1\\.8|9|10|11", System.getProperty("java.specification.version"));
    }
}
