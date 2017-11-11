
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

// SUPPRESS CHECKSTYLE LineLength|Whitespace|Javadoc:999

package test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.TimeTable;

public
class TimeTableTest {

    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Date       D1;
    static {
        try {
            D1 = TimeTableTest.DF.parse("2000-12-31 23:59:59");
        } catch (java.text.ParseException pe) {
            throw new ExceptionInInitializerError(pe);
        }
    }

    @Test public void
    testOnce() throws java.text.ParseException, de.unkrig.commons.text.parser.ParseException {
        Assert.assertEquals(TimeTableTest.DF.parse("2400-02-29 00:00:00"), TimeTable.parse("*/100-02-29"        ).next(TimeTableTest.D1)); // One-time
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:00:00"), TimeTable.parse("2001-01-01 00:00:00").next(TimeTableTest.D1)); // One-time
        Assert.assertEquals(TimeTable.MAX_DATE,                            TimeTable.parse("2000-12-31 23:59:59").next(TimeTableTest.D1)); // One-time, in the past
        Assert.assertEquals(TimeTableTest.DF.parse("2012-12-31 23:59:00"), TimeTable.parse("2012-12-31 23:59"   ).next(TimeTableTest.D1)); // One-time
        Assert.assertEquals(TimeTableTest.DF.parse("2012-12-31 00:00:00"), TimeTable.parse("2012-12-31"         ).next(TimeTableTest.D1)); // One-time
    }

    @Test public void
    testPeriodic() throws java.text.ParseException, de.unkrig.commons.text.parser.ParseException {
        Assert.assertEquals(TimeTableTest.DF.parse("2001-12-31 23:59:59"), TimeTable.parse("*-12-31 23:59:59"   ).next(TimeTableTest.D1)); // Once per year
        Assert.assertEquals(TimeTableTest.DF.parse("2001-12-31 23:59:00"), TimeTable.parse("*-12-31 23:59"      ).next(TimeTableTest.D1)); // Once per year
        Assert.assertEquals(TimeTableTest.DF.parse("2001-12-31 00:00:00"), TimeTable.parse("*-12-31"            ).next(TimeTableTest.D1)); // Once per year
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-31 23:59:59"), TimeTable.parse("*-*-31 23:59:59"    ).next(TimeTableTest.D1)); // Once per month
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-31 23:59:00"), TimeTable.parse("*-*-31 23:59"       ).next(TimeTableTest.D1)); // Once per month
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-31 00:00:00"), TimeTable.parse("*-*-31"             ).next(TimeTableTest.D1)); // Once per month
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-03 23:59:59"), TimeTable.parse("Wed 23:59:59"       ).next(TimeTableTest.D1)); // Once per week
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-03 23:59:00"), TimeTable.parse("Wed 23:59"          ).next(TimeTableTest.D1)); // Once per week
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-03 00:00:00"), TimeTable.parse("Wed"                ).next(TimeTableTest.D1)); // Once per week
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 23:59:59"), TimeTable.parse("23:59:59"           ).next(TimeTableTest.D1)); // Once per day
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 23:59:00"), TimeTable.parse("23:59"              ).next(TimeTableTest.D1)); // Once per day
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:59:59"), TimeTable.parse("*:59:59"            ).next(TimeTableTest.D1)); // Once per hour
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:59:00"), TimeTable.parse("*:59"               ).next(TimeTableTest.D1)); // Once per hour
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:00:59"), TimeTable.parse("*:*:59"             ).next(TimeTableTest.D1)); // Once per minute
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:00:00"), TimeTable.parse("*:*"                ).next(TimeTableTest.D1)); // Once per minute
    }

    @Test public void
    testIntegerSequenceAndRange() throws java.text.ParseException, de.unkrig.commons.text.parser.ParseException {
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-10 00:00:00"), TimeTable.parse("*-*-(8-14) Wed"     ).next(TimeTableTest.D1)); // Second wed of month
        TimeTableTest.assertTimeTableParseException(                                       "*-*-8-14"                                   ); // Unparenthesized day range
        Assert.assertEquals(TimeTableTest.DF.parse("2001-08-14 00:00:00"), TimeTable.parse("*-8-14"             ).next(TimeTableTest.D1)); // Looks like an integer range, but isn't
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 03:07:08"), TimeTable.parse("3-7:7:8"            ).next(TimeTableTest.D1)); // Unparenthesized hour range
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-01 00:07:08"), TimeTable.parse("7-3:7:8"            ).next(TimeTableTest.D1)); // Reverse hour range
        Assert.assertEquals(TimeTableTest.DF.parse("2001-01-10 00:00:00"), TimeTable.parse("*-*-(6,8,10,12) Wed").next(TimeTableTest.D1)); // Second wed of month
    }

    private static void
    assertTimeTableParseException(String s) {
        try {
            TimeTable.parse(s);
            Assert.fail("ParseException expected");
        } catch (de.unkrig.commons.text.parser.ParseException e) {
            ;
        }
    }

    @Test public void
    testToString() throws de.unkrig.commons.text.parser.ParseException {
        Assert.assertEquals("*-*-(8-14) Wed", TimeTable.parse("*-*-(8-14) Wed").toString());
        Assert.assertEquals("*:*/3", TimeTable.parse("*:*/3").toString());
    }
}
