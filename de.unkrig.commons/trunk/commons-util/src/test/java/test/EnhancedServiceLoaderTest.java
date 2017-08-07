
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

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.EnhancedServiceLoader;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF
//CHECKSTYLE JavadocVariable:OFF

public
class EnhancedServiceLoaderTest {

    public
    interface MyService {
        int getValue();
    }

    public static final
    class MyService1 implements MyService {
        public static MyService INSTANCE = new MyService1(); // SUPPRESS CHECKSTYLE StaticVariableName
        private                 MyService1() {}
        @Override public int    getValue()   { return 1; }
    }

    public static final
    class MyService2 implements MyService {

        private                 MyService2()  {}
        public static MyService getInstance() { return new MyService2(); }
        @Override public int    getValue()    { return 2; }
    }

    public static
    class MyService3 implements MyService {
        @Override public int getValue() { return 3; }
    }

    @Test public void
    test1() {
        Iterator<MyService> it = EnhancedServiceLoader.DEFAULT.load(MyService.class).iterator();

        {
            MyService instance = it.next();
            Assert.assertSame(MyService1.class, instance.getClass());
            Assert.assertEquals(1, instance.getValue());
        }

        {
            MyService instance = it.next();
            Assert.assertSame(MyService2.class, instance.getClass());
            Assert.assertEquals(2, instance.getValue());
        }

        {
            MyService instance = it.next();
            Assert.assertSame(MyService3.class, instance.getClass());
            Assert.assertEquals(3, instance.getValue());
        }

        Assert.assertFalse(it.hasNext());
    }
}
