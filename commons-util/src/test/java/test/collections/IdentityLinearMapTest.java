
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package test.collections;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.collections.IdentityLinearMap;

public
class IdentityLinearMapTest {

    @Test public void
    test() {
        Map<String, String> m = new IdentityLinearMap<String, String>();
        Assert.assertTrue(m.isEmpty());
        Assert.assertEquals(0, m.size());
        Assert.assertFalse(m.containsKey("xx"));

        Assert.assertNull(m.put("foo", "bar"));
        Assert.assertFalse(m.isEmpty());
        Assert.assertEquals(1, m.size());
        Assert.assertNull(m.get("xx"));
        Assert.assertSame("bar", m.get("foo"));
        Assert.assertNull(m.get(new String("foo")));
        Assert.assertFalse(m.containsKey("xx"));
        Assert.assertTrue(m.containsKey("foo"));
        Assert.assertFalse(m.containsKey(new String("foo")));
        Assert.assertFalse(m.containsValue("xx"));
        Assert.assertTrue(m.containsValue("bar"));
        Assert.assertFalse(m.containsValue(new String("bar")));

        Assert.assertNull(m.put("hello", "world"));
        Assert.assertFalse(m.isEmpty());
        Assert.assertEquals(2, m.size());
        Assert.assertTrue(m.containsValue("world"));

        Assert.assertSame("world", m.put("hello", "WORLD"));
        Assert.assertFalse(m.isEmpty());
        Assert.assertEquals(2, m.size());
        Assert.assertSame("WORLD", m.get("hello"));
        Assert.assertTrue(m.containsKey("hello"));
        Assert.assertTrue(m.containsValue("WORLD"));
        Assert.assertFalse(m.containsValue("world"));
    }
}
