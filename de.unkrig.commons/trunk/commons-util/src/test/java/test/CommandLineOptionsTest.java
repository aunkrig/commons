
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

import static de.unkrig.commons.util.annotation.CommandLineOption.Cardinality.MANDATORY;
import static de.unkrig.commons.util.annotation.CommandLineOption.Cardinality.ONCE_OR_MORE;
import static de.unkrig.commons.util.annotation.CommandLineOption.Cardinality.OPTIONAL;
import static de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality.EXACTLY_ONE;
import static de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality.ONE_OR_MORE;
import static de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality.ZERO_OR_ONE;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.CommandLineOptionException;
import de.unkrig.commons.util.CommandLineOptionException.ArgumentConversionFailed;
import de.unkrig.commons.util.CommandLineOptionException.ConflictingOptions;
import de.unkrig.commons.util.CommandLineOptionException.DuplicateOption;
import de.unkrig.commons.util.CommandLineOptionException.RequiredOptionGroupMissing;
import de.unkrig.commons.util.CommandLineOptionException.RequiredOptionMissing;
import de.unkrig.commons.util.CommandLineOptions;
import de.unkrig.commons.util.annotation.CommandLineOption;
import de.unkrig.commons.util.annotation.CommandLineOptionGroup;

public
class CommandLineOptionsTest {

    public static
    class Foo1 {
        public int             x1, x2, x5;
        @Nullable public int[] x3;

        @CommandLineOption public void                          setBar1(int x)               { this.x1 = x; }
        @CommandLineOption public void                          addBar2(int a, int b, int c) { this.x2 = c; }
        @CommandLineOption(name = "bar3") public void           bar4(int a, int b, int... c) { this.x3 = c; }
        @CommandLineOption(cardinality = OPTIONAL) public void  bar5(int x)                  { this.x5 = x; }
    }

    public static
    class Foo2 {
        public int x6;

        @CommandLineOption(cardinality = MANDATORY) public void bar6(int x) { this.x6 = x; }
    }

    public static
    class Foo3 {
        public int x7;

        @CommandLineOption(cardinality = ONCE_OR_MORE) public void bar7(int x) { this.x7 = x; }
    }

    @CommandLineOptionGroup(cardinality = ZERO_OR_ONE)
    interface ZeroOrOne {}

    public static
    class Foo4 {
        public int x1, x2;

        @CommandLineOption(group = ZeroOrOne.class) public void bar1(int x) { this.x1 = x; }
        @CommandLineOption(group = ZeroOrOne.class) public void bar2(int x) { this.x2 = x; }
    }

    @CommandLineOptionGroup(cardinality = EXACTLY_ONE)
    interface ExactlyOne {}

    public static
    class Foo5 {
        public int x1, x2;

        @CommandLineOption(group = ExactlyOne.class) public void bar1(int x) { this.x1 = x; }
        @CommandLineOption(group = ExactlyOne.class) public void bar2(int x) { this.x2 = x; }
    }

    @CommandLineOptionGroup(cardinality = ONE_OR_MORE)
    interface OneOrMore {}

    public static
    class Foo6 {
        public int x1, x2;

        @CommandLineOption(group = OneOrMore.class) public void bar1(int x) { this.x1 = x; }
        @CommandLineOption(group = OneOrMore.class) public void bar2(int x) { this.x2 = x; }
    }

    @Test public void
    test1() throws CommandLineOptionException {
        Foo1 foo1 = new Foo1();
        Assert.assertArrayEquals(
            new String[] { "xy.txt" },
            CommandLineOptions.parse(new String[] { "-bar1", "7", "xy.txt" }, foo1)
        );
        Assert.assertEquals(7, foo1.x1);
    }

    @Test public void
    test2() throws CommandLineOptionException {
        Foo1 foo1 = new Foo1();
        Assert.assertArrayEquals(new String[0], CommandLineOptions.parse(new String[] {
            "-bar1", "11",
            "-bar2", "22", "222", "2222",
            "-bar3", "33", "333", "3333", "33333",
        }, foo1));
        Assert.assertEquals(11,   foo1.x1);
        Assert.assertEquals(2222, foo1.x2);
        Assert.assertArrayEquals(new int[] { 3333, 33333 }, foo1.x3);
    }

    @Test public void
    test3() throws CommandLineOptionException {
        try {
            Assert.assertArrayEquals(
                new String[0],
                CommandLineOptions.parse(new String[] { "-bar1", "11", "-bar2", "22", "222", }, new Foo1())
            );
            Assert.fail("Exception");
        } catch (CommandLineOptionException.OptionArgumentMissing oam) {
            Assert.assertEquals("Argument #3 for command line option \"-bar2\" is missing", oam.getMessage());
            Assert.assertEquals(oam.getOptionName(), "-bar2");
            Assert.assertEquals(oam.getArgumentIndex(), 2);
        }
    }

    @Test public void
    test4() throws CommandLineOptionException {
        try {
            Assert.assertArrayEquals(
                new String[0],
                CommandLineOptions.parse(new String[] { "-bar1", "11", "-bar2", "22", "222", "-ff", }, new Foo1())
            );
            Assert.fail("Exception");
        } catch (ArgumentConversionFailed acf) {
            Assert.assertEquals(
                "Converting option argument \"-ff\" to \"int\": Cannot convert \"-ff\" to int",
                acf.getMessage()
            );
            Assert.assertEquals("-ff", acf.getArgument());
            Assert.assertEquals(int.class, acf.getTargetType());
        }
    }

    @Test public void
    testOptionalOption0() throws CommandLineOptionException {

        Foo1 foo1 = new Foo1();
        Assert.assertArrayEquals(
            new String[0],
            CommandLineOptions.parse(new String[0], foo1)
        );
        Assert.assertEquals(foo1.x5, 0);
    }

    @Test public void
    testOptionalOption1() throws CommandLineOptionException {

        Foo1 foo1 = new Foo1();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar5", "99" }, foo1).length);
        Assert.assertEquals(foo1.x5, 99);
    }

    @Test public void
    testOptionalOption2() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[] { "-bar5", "99", "-bar5", "88" }, new Foo1());
            Assert.fail();
        } catch (DuplicateOption dO) {
            Assert.assertEquals("Option \"-bar5\" must appear at most once", dO.getMessage());
            Assert.assertEquals("-bar5", dO.getOptionName());
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar5", "--bar5")),
                new HashSet<String>(Arrays.asList(dO.getOptionNames()))
            );
        }
    }

    @Test public void
    testMandatoryOption0() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[0], new Foo2());
            Assert.fail();
        } catch (RequiredOptionMissing rom) {
            Assert.assertEquals(
                "Exactly one of command line options \"-bar6\" and \"--bar6\" must be given",
                rom.getMessage()
            );
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar6", "--bar6")),
                new HashSet<String>(Arrays.asList(rom.getOptionNames()))
            );
        }
    }

    @Test public void
    testMandatoryOption1() throws CommandLineOptionException {

        Foo2 foo2 = new Foo2();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar6", "99" }, foo2).length);
        Assert.assertEquals(foo2.x6, 99);
    }

    @Test public void
    testMandatoryOption2() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[] { "-bar6", "99", "-bar6", "88" }, new Foo2());
            Assert.fail();
        } catch (DuplicateOption dO) {
            Assert.assertEquals("Option \"-bar6\" must appear at most once", dO.getMessage());
            Assert.assertEquals("-bar6", dO.getOptionName());
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar6", "--bar6")),
                new HashSet<String>(Arrays.asList(dO.getOptionNames()))
            );
        }
    }

    @Test public void
    testOnceOrMoreOption0() throws CommandLineOptionException {

        try {
            Assert.assertEquals(0, CommandLineOptions.parse(new String[0], new Foo3()).length);
            Assert.fail();
        } catch (RequiredOptionMissing rom) {
            Assert.assertEquals(
                "Exactly one of command line options \"-bar7\" and \"--bar7\" must be given",
                rom.getMessage()
            );
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar7", "--bar7")),
                new HashSet<String>(Arrays.asList(rom.getOptionNames()))
            );
        }
    }

    @Test public void
    testOnceOrMoreOption1() throws CommandLineOptionException {

        Foo3 foo3 = new Foo3();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar7", "99" }, foo3).length);
        Assert.assertEquals(foo3.x7, 99);
    }

    @Test public void
    testOnceOrMoreOption2() throws CommandLineOptionException {

        Foo3 foo3 = new Foo3();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar7", "99", "-bar7", "88" }, foo3).length);
        Assert.assertEquals(foo3.x7, 88);
    }

    @Test public void
    testOptionGroupZeroOrOne0() throws CommandLineOptionException {

        Assert.assertEquals(0, CommandLineOptions.parse(new String[0], new Foo4()).length);
    }

    @Test public void
    testOptionGroupZeroOrOne1() throws CommandLineOptionException {

        Foo4 foo4 = new Foo4();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar1", "99" }, foo4).length);
        Assert.assertEquals(foo4.x1, 99);
    }

    @Test public void
    testOptionGroupZeroOrOne2() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[] { "-bar1", "99", "-bar2", "88" }, new Foo4());
            Assert.fail();
        } catch (ConflictingOptions dO) {
            Assert.assertEquals("Option \"-bar2\" is exclusive with a preceding option", dO.getMessage());
            Assert.assertEquals("-bar2", dO.getOptionName());
        }
    }

    @Test public void
    testOptionGroupExactlyOne0() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[0], new Foo5());
            Assert.fail();
        } catch (RequiredOptionGroupMissing rogm) {
            Assert.assertEquals(
                "Exactly one of \"-bar1\", \"--bar1\", \"-bar2\" and \"--bar2\" must be specified",
                rogm.getMessage()
            );
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar1", "--bar1", "-bar2", "--bar2")),
                new HashSet<String>(Arrays.asList(rogm.getOptionNames()))
            );
        }
    }

    @Test public void
    testOptionGroupExactlyOne1() throws CommandLineOptionException {

        Foo5 foo5 = new Foo5();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar1", "99" }, foo5).length);
        Assert.assertEquals(foo5.x1, 99);
    }

    @Test public void
    testOptionGroupExactlyOne2() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[] { "-bar1", "99", "-bar2", "88" }, new Foo5());
            Assert.fail();
        } catch (ConflictingOptions dO) {
            Assert.assertEquals("Option \"-bar2\" is exclusive with a preceding option", dO.getMessage());
            Assert.assertEquals("-bar2", dO.getOptionName());
        }
    }

    @Test public void
    testOptionGroupOneOrMore0() throws CommandLineOptionException {

        try {
            CommandLineOptions.parse(new String[0], new Foo6());
            Assert.fail();
        } catch (RequiredOptionGroupMissing rogm) {
            Assert.assertEquals(
                "One or more of \"-bar1\", \"--bar1\", \"-bar2\" and \"--bar2\" must be specified",
                rogm.getMessage()
            );
            Assert.assertEquals(
                new HashSet<String>(Arrays.asList("-bar1", "--bar1", "-bar2", "--bar2")),
                new HashSet<String>(Arrays.asList(rogm.getOptionNames()))
            );
        }
    }

    @Test public void
    testOptionGroupOneOrMore1() throws CommandLineOptionException {

        Foo6 foo6 = new Foo6();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar1", "99" }, foo6).length);
        Assert.assertEquals(foo6.x1, 99);
    }

    @Test public void
    testOptionGroupOneOrMore2() throws CommandLineOptionException {

        Foo6 foo6 = new Foo6();
        Assert.assertEquals(0, CommandLineOptions.parse(new String[] { "-bar1", "99", "-bar2", "88" }, foo6).length);
        Assert.assertEquals(foo6.x1, 99);
        Assert.assertEquals(foo6.x2, 88);
    }

    @Test public void
    testMultiGroups() throws CommandLineOptionException {
        CommandLineOptions.parse(new String[] { "--clo1" },            new Foo10());
        CommandLineOptions.parse(new String[] { "--clo1", "--clo01" }, new Foo10());
        CommandLineOptions.parse(new String[] { "--combo" },           new Foo10());
        for (Object[] os : new Object[][] {
            { RequiredOptionGroupMissing.class, new String[] {}                                 },
            { RequiredOptionGroupMissing.class, new String[] { "--clo01"                      } },
            { ConflictingOptions.class,         new String[] { "--clo1", "--combo"            } },
            { ConflictingOptions.class,         new String[] { "--clo1", "--clo01", "--combo" } },
            { ConflictingOptions.class,         new String[] { "--clo01", "--combo"           } },
        }) {
            Class<?> expectedException = (Class<?>) os[0];
            String[] args              = (String[]) os[1];
            try {
                CommandLineOptions.parse(args, new Foo10());
                Assert.fail(Arrays.toString(args));
            } catch (Exception e) {
                Class<? extends Exception> actualException = e.getClass();
                Assert.assertEquals(Arrays.toString(args), expectedException, actualException);
            }
        }
    }
    public static
    class Foo10 {
        @CommandLineOption(group = ExactlyOne.class) public void                      clo1()  {}
        @CommandLineOption(group = ZeroOrOne.class) public void                       clo01() {}
        @CommandLineOption(group = { ExactlyOne.class, ZeroOrOne.class }) public void combo() {}
    }
}
