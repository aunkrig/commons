
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

package test.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import de.unkrig.commons.util.collections.IdentityLinearMap;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class IdentityLinearMapTest {

    @Test public void
    test() {
        Map<String, String> m = new IdentityLinearMap<String, String>();
        assertTrue(m.isEmpty());
        assertEquals(0, m.size());
        assertFalse(m.containsKey("xx"));

        assertNull(m.put("foo", "bar"));
        assertFalse(m.isEmpty());
        assertEquals(1, m.size());
        assertNull(m.get("xx"));
        assertSame("bar", m.get("foo"));
        assertNull(m.get(new String("foo")));
        assertFalse(m.containsKey("xx"));
        assertTrue(m.containsKey("foo"));
        assertFalse(m.containsKey(new String("foo")));
        assertFalse(m.containsValue("xx"));
        assertTrue(m.containsValue("bar"));
        assertFalse(m.containsValue(new String("bar")));

        assertNull(m.put("hello", "world"));
        assertFalse(m.isEmpty());
        assertEquals(2, m.size());
        assertTrue(m.containsValue("world"));

        assertSame("world", m.put("hello", "WORLD"));
        assertFalse(m.isEmpty());
        assertEquals(2, m.size());
        assertSame("WORLD", m.get("hello"));
        assertTrue(m.containsKey("hello"));
        assertTrue(m.containsValue("WORLD"));
        assertFalse(m.containsValue("world"));
    }
}
