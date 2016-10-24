
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package test;

import static de.unkrig.commons.lang.ExceptionUtil.throwUndeclared;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.Test;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF

@SuppressWarnings({ "static-method", "unused" }) public
class ExceptionUtilTest {

    @Test public void
    testThrowUndeclared1() {
        try {
            this.meth1();
        } catch (Exception e) {
            assertSame(ClassNotFoundException.class, e.getClass());
        }
    }

    @Test public void
    testThrowUndeclared2() {
        try {
            this.meth3();
        } catch (Exception e) {
            assertSame(ClassNotFoundException.class, e.getClass());
        }
    }

    private void meth1() { this.meth2(); }
    private void meth2() { throwUndeclared(new ClassNotFoundException()); }

    private void meth3() throws IOException { this.meth4(); }
    private void meth4() throws IOException { throwUndeclared(new ClassNotFoundException()); }
}
