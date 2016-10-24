
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

import static de.unkrig.commons.util.collections.MapUtil.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.util.collections.MapUtil;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class MapUtilTest {

    @Test public void
    testAugment() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("foo", "bar");

        {
            Map<String, String> m2 = MapUtil.augment(m, "alpha", "beta");
            assertEquals(2, m2.size());
            assertEquals("bar", m2.get("foo"));
            assertEquals("beta", m2.get("alpha"));

            assertTrue(m2.containsKey("foo"));
            assertTrue(m2.containsKey("alpha"));
            assertFalse(m2.containsKey(null));
            assertFalse(m2.containsKey("gamma"));

            assertTrue(m2.containsValue("bar"));
            assertTrue(m2.containsValue("beta"));
            assertFalse(m2.containsValue("gamma"));
            assertFalse(m2.containsValue(null));
        }

        {
            Map<String, String> m2 = MapUtil.augment(m, "foo", "beta");
            assertEquals(1, m2.size());
            assertEquals("bar", m2.get("foo"));

            assertTrue(m2.containsKey("foo"));
            assertFalse(m2.containsKey(null));
            assertFalse(m2.containsKey("gamma"));

            assertTrue(m2.containsValue("bar"));
            assertFalse(m2.containsValue("beta"));
            assertFalse(m2.containsValue("gamma"));
            assertFalse(m2.containsValue(null));
        }
    }

    @Test public void
    testOverride() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("foo", "bar");

        {
            Map<String, String> m2 = MapUtil.override(m, "alpha", "beta");
            assertEquals(2, m2.size());
            assertEquals("bar", m2.get("foo"));
            assertEquals("beta", m2.get("alpha"));

            assertTrue(m2.containsKey("foo"));
            assertTrue(m2.containsKey("alpha"));
            assertFalse(m2.containsKey(null));
            assertFalse(m2.containsKey("gamma"));

            assertTrue(m2.containsValue("bar"));
            assertTrue(m2.containsValue("beta"));
            assertFalse(m2.containsValue("gamma"));
            assertFalse(m2.containsValue(null));
        }

        {
            Map<String, String> m2 = MapUtil.override(m, "foo", "beta");
            assertEquals(1, m2.size());
            assertEquals("beta", m2.get("foo"));

            assertTrue(m2.containsKey("foo"));
            assertFalse(m2.containsKey(null));
            assertFalse(m2.containsKey("gamma"));

            assertFalse(m2.containsValue("bar"));
            assertTrue(m2.containsValue("beta"));
            assertFalse(m2.containsValue("gamma"));
            assertFalse(m2.containsValue(null));
        }
    }

    @Test public void
    testCombine() {

        Map<String, String> m1 = new HashMap<String, String>();
        m1.put("alpha", "beta");
        m1.put("gamma", "delta");

        Map<String, String> m2 = new HashMap<String, String>();
        m2.put("gamma", "DELTA");
        m2.put("epsilon", "zeta");

        Map<String, String> m3 = MapUtil.combine(m1, m2);

        assertEquals(3, m3.size());

        assertTrue(m3.containsKey("alpha"));
        assertTrue(m3.containsKey("gamma"));
        assertTrue(m3.containsKey("epsilon"));
        assertFalse(m3.containsKey("foo"));
        assertFalse(m3.containsKey(null));

        assertTrue(m3.containsKey("alpha"));
        assertTrue(m3.containsKey("gamma"));
        assertTrue(m3.containsKey("epsilon"));
        assertFalse(m3.containsKey("foo"));
        assertFalse(m3.containsKey(null));

        assertEquals("beta", m3.get("alpha"));
        assertEquals("delta", m3.get("gamma"));
        assertEquals("zeta", m3.get("epsilon"));
        assertEquals(null, m3.get("foo"));
        assertEquals(null, m3.get(null));
    }

    @Test public void
    testFromMappings() {

        @SuppressWarnings("deprecation") Map<String, String> m = MapUtil.<String, String>fromMappings(
            "alpha",   "beta",
            "gamma",   "delta",
            "gamma",   "DELTA",
            "epsilon", "zeta"
        );

        assertEquals(3, m.size());

        assertTrue(m.containsKey("alpha"));
        assertTrue(m.containsKey("gamma"));
        assertTrue(m.containsKey("epsilon"));
        assertFalse(m.containsKey("foo"));
        assertFalse(m.containsKey(null));

        assertEquals("beta", m.get("alpha"));
        assertEquals("DELTA", m.get("gamma"));
        assertEquals("zeta", m.get("epsilon"));
        assertEquals(null, m.get("foo"));
        assertEquals(null, m.get(null));
    }

    @Test public void
    testMap1() {

        try {
            MapUtil.<String, String>map(
                "alpha",   "beta",
                "gamma",   "delta",
                "gamma",   "DELTA",
                "epsilon", "zeta"
            );
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("Duplicate key"));
        }
    }

    @Test public void
    testMap() {

        Map<String, String> m = MapUtil.<String, String>map(
            "alpha",   "beta",
            "gamma",   "DELTA",
            "epsilon", "zeta"
        );

        assertEquals(3, m.size());

        assertTrue(m.containsKey("alpha"));
        assertTrue(m.containsKey("gamma"));
        assertTrue(m.containsKey("epsilon"));
        assertFalse(m.containsKey("foo"));
        assertFalse(m.containsKey(null));

        assertEquals("beta", m.get("alpha"));
        assertEquals("DELTA", m.get("gamma"));
        assertEquals("zeta", m.get("epsilon"));
        assertEquals(null, m.get("foo"));
        assertEquals(null, m.get(null));
    }

    @Test public void
    testHashMapOf1() {

        // In Java, this warning can be avoided by adding @SafeVarargs to "MapUtil.hashMapOf()".
        @SuppressWarnings("unchecked") HashMap<String, String>
        m = MapUtil.hashMapOf(entry("alpha", "beta"), MapUtil.entry("gamma", "delta"));

        assertEquals(2, m.size());

        assertTrue(m.containsKey("alpha"));
        assertTrue(m.containsKey("gamma"));
        assertFalse(m.containsKey("foo"));
        assertFalse(m.containsKey(null));

        assertEquals("beta", m.get("alpha"));
        assertEquals("delta", m.get("gamma"));
        assertEquals(null, m.get("foo"));
        assertEquals(null, m.get(null));
    }

    @Test @SuppressWarnings("unchecked") public void
    testHashMapOf2() {

        try {
            MapUtil.hashMapOf(entry("alpha", "beta"), entry("alpha", "beta"));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            ;
        }
    }
}
