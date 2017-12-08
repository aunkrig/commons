
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

// SUPPRESS CHECKSTYLE Javadoc|LineLength:9999

package test;

import static test.EnumsTest.MyEnum.BLUE;
import static test.EnumsTest.MyEnum.GREEN;
import static test.EnumsTest.MyEnum.RED;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.Enums;

public
class EnumsTest {

    public enum MyEnum { RED, GREEN, BLUE }

    @Test public void test1() { Assert.assertEquals(EnumSet.of(RED, GREEN),       Enums.enumSetFromString("[RED,GREEN]",        MyEnum.class)); }
    @Test public void test2() { Assert.assertEquals(EnumSet.noneOf(MyEnum.class), Enums.enumSetFromString(" ",                  MyEnum.class)); }
    @Test public void test3() { Assert.assertEquals(EnumSet.noneOf(MyEnum.class), Enums.enumSetFromString("",                   MyEnum.class)); }
    @Test public void test4() { Assert.assertEquals(EnumSet.noneOf(MyEnum.class), Enums.enumSetFromString(" [ ] ",              MyEnum.class)); }
    @Test public void test5() { Assert.assertEquals(EnumSet.of(BLUE, GREEN),      Enums.enumSetFromString(" [ GREEN , BLUE ] ", MyEnum.class)); }

    @Test(expected = IllegalArgumentException.class) public void test6()  { Enums.enumSetFromString(" [ GREEN , BLUE ",   MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test7()  { Enums.enumSetFromString(" [ , BLUE ] ",       MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test8()  { Enums.enumSetFromString(" GREEN , BLUE ",     MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test9()  { Enums.enumSetFromString(" [ ] x ",            MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test10() { Enums.enumSetFromString(" [ GREEN ,, BLUE] ", MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test11() { Enums.enumSetFromString("[GR EEN]",           MyEnum.class); }
    @Test(expected = IllegalArgumentException.class) public void test12() { Enums.enumSetFromString("[GREEn]",            MyEnum.class); }
}
