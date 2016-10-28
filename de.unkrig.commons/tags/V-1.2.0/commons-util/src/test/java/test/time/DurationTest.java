
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

package test.time;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.time.Duration;

// CHECKSTYLE Javadoc:OFF
// CHECKSTYLE R_PAREN__METH_INVOCATION:OFF

public
class DurationTest {

    @Test public void
    test() {
        Assert.assertEquals("1ms",                new Duration("1ms"                  ).toString());
        Assert.assertEquals("90ms",               new Duration("0.09s"                ).toString());
        Assert.assertEquals("1.3s",               new Duration("1300ms"               ).toString());
        Assert.assertEquals("1.35s",              new Duration("1350ms"               ).toString());
        Assert.assertEquals("3s",                 new Duration("3.0001s"              ).toString());
        Assert.assertEquals("0:02:00",            new Duration("120s"                 ).toString());
        Assert.assertEquals("5d 17:03:59.999",    new Duration("137:3:59.9999359"     ).toString());
        Assert.assertEquals("52w 0d 0:00:00",     new Duration("52w"                  ).toString());
        Assert.assertEquals("52w 1d 5:49:12",     new Duration("31556952s"            ).toString()); // Gregorian year
        Assert.assertEquals("52w 1d 5:48:45.261", new Duration("365d 5h 48min 45.261s").toString()); // Tropical year
    }
}
