
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

package test.pipe;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Producer;

/**
 * Utility methods related to JUNIT and {@code java.io}.
 */
public final
class AssertIo {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private AssertIo() {}

    /**
     * @return An output stream which asserts that the data written to it equals the {@code expected} byte sequence
     */
    public static OutputStream
    assertEqualData(final Producer<? extends Byte> expected) {

        return new OutputStream() {

            int count;

            @Override public void
            write(int b) {
                Byte product = expected.produce();
                assert product != null;
                if ((0xff & product) != (0xff & b)) {
                    assertEquals(this.count + " bytes matched so far", 0xff & product, 0xff & b);
                }
                this.count++;
            }
        };
    }
}
