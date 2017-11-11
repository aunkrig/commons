
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.ObjectUtil;

public
class ObjectUtilTest {

    @Test public void
    testFromStringToShort() {
        Assert.assertEquals((Object) (short) 3, ObjectUtil.fromString("3", short.class));
    }

    @Test public void
    testFromStringToJavaLangShort() {
        Assert.assertEquals((Object) (short) 1, ObjectUtil.fromString("1", Short.class));
    }

    @Test public void
    testFromStringToDouble() {
        Assert.assertEquals((Double) 12.3, ObjectUtil.fromString("12.30", double.class));

        try {
            Assert.assertEquals((Double) 12.3, ObjectUtil.fromString("12.3A", double.class));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("Cannot convert \"12.3A\" to double", iae.getMessage());
        }
    }

    @Test public void
    testFromStringToString() {
        Assert.assertEquals("FOO", ObjectUtil.fromString("FOO", String.class));
    }

    @Test public void
    testFromStringToClass() {
        Assert.assertSame(ObjectUtilTest.class, ObjectUtil.fromString(ObjectUtilTest.class.getName(), Class.class));
    }

    @Test public void
    testFromStringToChar() {
        Assert.assertEquals((Object) 'A', ObjectUtil.fromString("A", char.class));
    }

    @Test public void
    testFromStringToCharacter() {

        Assert.assertEquals((Object) 'A', ObjectUtil.fromString("A", Character.class));

        try {
            Assert.assertEquals((Object) 'A', ObjectUtil.fromString("AA", Character.class));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("String too long", iae.getMessage());
        }
    }

    @Test public void
    testFromStringToPattern() {
        Assert.assertTrue(ObjectUtil.fromString(".*", Pattern.class).matcher("ABC").matches());
    }

    @Test public void
    testFromStringToCharset() {
        Assert.assertSame(Charset.forName("UTF-8"), ObjectUtil.fromString("UTF-8", Charset.class));
    }

    public static
    class Foo {
        public String bar;
        public Foo(String bar) { this.bar = bar; }
    }

    @Test public void
    testFromStringToObject() {
        Assert.assertSame("bar", ObjectUtil.fromString("bar", Foo.class).bar);
    }

    @Test public void
    testFromStringToIntArray() {
        Assert.assertArrayEquals(new int[] { 1, 2, 3 }, ObjectUtil.fromString("1,2   ,   3", int[].class));
    }

    @Test public void
    testFromStringToStringArray() {
        Assert.assertArrayEquals(
            new String[] { "A", "B", "C" },
            ObjectUtil.fromString("A  ,B,  C,  ", String[].class)
        );
    }

    /**
     * Tests the workaround for {@link List}s which is proposed {@link ObjectUtil#fromString(String, Class) here}.
     */
    @Test public void
    testFromStringToList() {

        // Notice that the order of the elements (RED, GREEN) must be IDENTICAL, because arrays are ordered.
        Assert.assertEquals(
            Arrays.asList("RED", "GREEN"),
            Arrays.asList(ObjectUtil.fromString("RED,  ,GREEN ,", String[].class))
        );
    }

    /**
     * Tests the workaround for {@link Set}s which is proposed {@link ObjectUtil#fromString(String, Class) here}.
     */
    @Test public void
    testFromStringToSet() {

        Assert.assertEquals(
            new HashSet<String>(Arrays.asList("RED", "GREEN")),
            new HashSet<String>(Arrays.asList(ObjectUtil.fromString("GREEN,  ,RED ,", String[].class)))
        );
    }

    public enum MyEnum { RED, GREEN, BLUE }

    @Test public void
    testFromStringToEnum() {
        Assert.assertSame(MyEnum.RED, ObjectUtil.fromString("RED", MyEnum.class));
    }

    @Test public void
    testFromStringToEnumArray() {

        // Notice that the order of the elements (RED, GREEN) must be IDENTICAL, because arrays are ordered.
        Assert.assertArrayEquals(
            new MyEnum[] { MyEnum.RED, MyEnum.GREEN },
            ObjectUtil.fromString("RED,  ,GREEN ,", MyEnum[].class)
        );
    }

    /**
     * Tests the workaround for {@link EnumSet}s which is proposed {@link ObjectUtil#fromString(String, Class) here}.
     */
    @Test public void
    testFromStringToEnumSet() {

        // Notice that the order of the elements (RED, GREEN) is different, which works because of the semantics of the
        // "set".
        Assert.assertEquals(
            EnumSet.of(MyEnum.RED, MyEnum.GREEN),
            EnumSet.copyOf(Arrays.asList(ObjectUtil.fromString("GREEN,  ,RED ,", MyEnum[].class)))
        );
    }
}
