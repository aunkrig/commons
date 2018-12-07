
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;

@SuppressWarnings("static-method") public final
class AssertionUtilTest {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    static { AssertionUtilTest.class.getClassLoader().setDefaultAssertionStatus(false); }

    /**
     * Verifies that enabling assertions is only permitted from <em>class initializers</em> (and not from instance
     * initializers, methods and constructors).
     */
    @Test(expected = AssertionError.class) public void
    test1() {
        AssertionUtil.enableAssertionsForThisClass();
    }

    /**
     * Verifies that the assertions of member classes and interfaces are independent from the enclosing class's or
     * interface's assertions.
     */
    @Test(expected = AssertionError.class) public void
    test3() {
        MemberClassWithAssertionsEnabled.meth();
    }
    @Test public void
    test4() {
        MemberClassWithAssertionsNotEnabled.meth();
    }
    static
    class MemberClassWithAssertionsEnabled {
        static { AssertionUtil.enableAssertionsForThisClass(); }
        public static void meth() { assert false; }
    }
    static
    class MemberClassWithAssertionsNotEnabled {
        public static void meth() { assert false; }
    }

    /**
     * Verifies that assertions are enabled fot <em>this</em> object.
     */
    @Test(expected = AssertionError.class) public void
    test5() {
        assert false;
    }

    @Test public void
    test6() {
        Assert.assertFalse(ClassWithAssertionsNotEnabled.isAssertionsEnabled());
    }

    @Test public void
    test7() {
        Assert.assertTrue(ClassWithAssertionsEnabled.isAssertionsEnabled());
    }
}
