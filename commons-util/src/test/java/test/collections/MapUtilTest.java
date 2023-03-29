
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

package test.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.protocol.Function;
import de.unkrig.commons.util.collections.MapUtil;
import de.unkrig.commons.util.collections.Sets;

public
class MapUtilTest {

    @Test public void
    testAugment() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("foo", "bar");

        {
            Map<String, String> m2 = MapUtil.augment(m, "alpha", "beta");
            Assert.assertEquals(2, m2.size());
            Assert.assertEquals("bar", m2.get("foo"));
            Assert.assertEquals("beta", m2.get("alpha"));

            Assert.assertTrue(m2.containsKey("foo"));
            Assert.assertTrue(m2.containsKey("alpha"));
            Assert.assertFalse(m2.containsKey(null));
            Assert.assertFalse(m2.containsKey("gamma"));

            Assert.assertTrue(m2.containsValue("bar"));
            Assert.assertTrue(m2.containsValue("beta"));
            Assert.assertFalse(m2.containsValue("gamma"));
            Assert.assertFalse(m2.containsValue(null));
        }

        {
            Map<String, String> m2 = MapUtil.augment(m, "foo", "beta");
            Assert.assertEquals(1, m2.size());
            Assert.assertEquals("bar", m2.get("foo"));

            Assert.assertTrue(m2.containsKey("foo"));
            Assert.assertFalse(m2.containsKey(null));
            Assert.assertFalse(m2.containsKey("gamma"));

            Assert.assertTrue(m2.containsValue("bar"));
            Assert.assertFalse(m2.containsValue("beta"));
            Assert.assertFalse(m2.containsValue("gamma"));
            Assert.assertFalse(m2.containsValue(null));
        }
    }

    @Test public void
    testOverride() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("foo", "bar");

        {
            Map<String, String> m2 = MapUtil.override(m, "alpha", "beta");
            Assert.assertEquals(2, m2.size());
            Assert.assertEquals("bar", m2.get("foo"));
            Assert.assertEquals("beta", m2.get("alpha"));

            Assert.assertTrue(m2.containsKey("foo"));
            Assert.assertTrue(m2.containsKey("alpha"));
            Assert.assertFalse(m2.containsKey(null));
            Assert.assertFalse(m2.containsKey("gamma"));

            Assert.assertTrue(m2.containsValue("bar"));
            Assert.assertTrue(m2.containsValue("beta"));
            Assert.assertFalse(m2.containsValue("gamma"));
            Assert.assertFalse(m2.containsValue(null));
        }

        {
            Map<String, String> m2 = MapUtil.override(m, "foo", "beta");
            Assert.assertEquals(1, m2.size());
            Assert.assertEquals("beta", m2.get("foo"));

            Assert.assertTrue(m2.containsKey("foo"));
            Assert.assertFalse(m2.containsKey(null));
            Assert.assertFalse(m2.containsKey("gamma"));

            Assert.assertFalse(m2.containsValue("bar"));
            Assert.assertTrue(m2.containsValue("beta"));
            Assert.assertFalse(m2.containsValue("gamma"));
            Assert.assertFalse(m2.containsValue(null));
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

        Assert.assertEquals(3, m3.size());

        Assert.assertTrue(m3.containsKey("alpha"));
        Assert.assertTrue(m3.containsKey("gamma"));
        Assert.assertTrue(m3.containsKey("epsilon"));
        Assert.assertFalse(m3.containsKey("foo"));
        Assert.assertFalse(m3.containsKey(null));

        Assert.assertTrue(m3.containsKey("alpha"));
        Assert.assertTrue(m3.containsKey("gamma"));
        Assert.assertTrue(m3.containsKey("epsilon"));
        Assert.assertFalse(m3.containsKey("foo"));
        Assert.assertFalse(m3.containsKey(null));

        Assert.assertEquals("beta", m3.get("alpha"));
        Assert.assertEquals("delta", m3.get("gamma"));
        Assert.assertEquals("zeta", m3.get("epsilon"));
        Assert.assertEquals(null, m3.get("foo"));
        Assert.assertEquals(null, m3.get(null));
    }

    @Test public void
    testFromMappings() {

        @SuppressWarnings("deprecation") Map<String, String> m = MapUtil.<String, String>fromMappings(
            "alpha",   "beta",          // SUPPRESS CHECKSTYLE WrapMethod:3
            "gamma",   "delta",
            "gamma",   "DELTA",
            "epsilon", "zeta"
        );

        Assert.assertEquals(3, m.size());

        Assert.assertTrue(m.containsKey("alpha"));
        Assert.assertTrue(m.containsKey("gamma"));
        Assert.assertTrue(m.containsKey("epsilon"));
        Assert.assertFalse(m.containsKey("foo"));
        Assert.assertFalse(m.containsKey(null));

        Assert.assertEquals("beta", m.get("alpha"));
        Assert.assertEquals("DELTA", m.get("gamma"));
        Assert.assertEquals("zeta", m.get("epsilon"));
        Assert.assertEquals(null, m.get("foo"));
        Assert.assertEquals(null, m.get(null));
    }

    @Test public void
    testMap1() {

        try {
            MapUtil.<String, String>map(
                "alpha",   "beta",           // SUPPRESS CHECKSTYLE WrapMethod:3
                "gamma",   "delta",
                "gamma",   "DELTA",
                "epsilon", "zeta"
            );
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Duplicate key"));
        }
    }

    @Test public void
    testMap() {

        Map<String, String> m = MapUtil.<String, String>map(
            "alpha",   "beta",    // SUPPRESS CHECKSTYLE WrapMethod:2
            "gamma",   "DELTA",
            "epsilon", "zeta"
        );

        Assert.assertEquals(3, m.size());

        Assert.assertTrue(m.containsKey("alpha"));
        Assert.assertTrue(m.containsKey("gamma"));
        Assert.assertTrue(m.containsKey("epsilon"));
        Assert.assertFalse(m.containsKey("foo"));
        Assert.assertFalse(m.containsKey(null));

        Assert.assertEquals("beta", m.get("alpha"));
        Assert.assertEquals("DELTA", m.get("gamma"));
        Assert.assertEquals("zeta", m.get("epsilon"));
        Assert.assertEquals(null, m.get("foo"));
        Assert.assertEquals(null, m.get(null));
    }

    @Test public void
    testHashMapOf1() {

        // In Java, this warning can be avoided by adding @SafeVarargs to "MapUtil.hashMapOf()".
        @SuppressWarnings("unchecked") HashMap<String, String>
        m = MapUtil.hashMapOf(MapUtil.entry("alpha", "beta"), MapUtil.entry("gamma", "delta"));

        Assert.assertEquals(2, m.size());

        Assert.assertTrue(m.containsKey("alpha"));
        Assert.assertTrue(m.containsKey("gamma"));
        Assert.assertFalse(m.containsKey("foo"));
        Assert.assertFalse(m.containsKey(null));

        Assert.assertEquals("beta", m.get("alpha"));
        Assert.assertEquals("delta", m.get("gamma"));
        Assert.assertEquals(null, m.get("foo"));
        Assert.assertEquals(null, m.get(null));
    }

    @Test @SuppressWarnings("unchecked") public void
    testHashMapOf2() {

        try {
            MapUtil.hashMapOf(MapUtil.entry("alpha", "beta"), MapUtil.entry("alpha", "beta"));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            ;
        }
    }

    @Test public void
    testLazyMap() {
        List<String>              out = new ArrayList<>();
        Function<Integer, String> vg1 = MapUtilTest.func("alpha", out);
        Function<Integer, String> vg2 = MapUtilTest.func("beta", out);
        Function<Integer, String> vg3 = MapUtilTest.func("gamma", out);

        Map<Integer, Function<? super Integer, ? extends String>> valueGetters = new TreeMap<>();
        valueGetters.put(1, vg1);
        valueGetters.put(2, vg2);
        valueGetters.put(3, vg3);
        Map<Integer, String> lm = MapUtil.lazyMap(valueGetters, (Integer) 77);
        Assert.assertEquals(Arrays.asList(), out);

        // get()
        Assert.assertEquals("77beta", lm.get(2));
        Assert.assertEquals(Arrays.asList("77beta"), out);

        // containsKey()
        Assert.assertTrue(lm.containsKey(3));
        Assert.assertEquals(Arrays.asList("77beta"), out);

        // containsValue(nonexisting key)
        out.clear();
        Assert.assertFalse(lm.containsValue("xxx"));
        Assert.assertEquals(Arrays.asList("77alpha", "77beta", "77gamma"), out);

        // containsValue(existing key)
        out.clear();
        Assert.assertTrue(lm.containsValue("77beta"));
        Assert.assertEquals(Arrays.asList("77alpha", "77beta"), out);

        // keySet()
        out.clear();
        Assert.assertEquals(Sets.of(1, 2, 3), lm.keySet());
        Assert.assertTrue(out.isEmpty());

        // values().contains(existing value)
        out.clear();
        {
            Collection<String> values = lm.values();
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertTrue(values.contains("77beta"));
            Assert.assertEquals(Arrays.asList("77alpha", "77beta"), out);
        }

        // values().contains(nonexisting value)
        out.clear();
        {
            Collection<String> values = lm.values();
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertFalse(values.contains("xxx"));
            Assert.assertEquals(Arrays.asList("77alpha", "77beta", "77gamma"), out);
        }

        // values().iterator().hasNext(), .next()
        out.clear();
        {
            Collection<String> values = lm.values();
            Assert.assertEquals(Arrays.asList(), out);
            Iterator<String> it = values.iterator();
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertEquals("77alpha", it.next());
            Assert.assertEquals(Arrays.asList("77alpha"), out);
        }

        // entrySet().iterator().hasNext(), .next().getKey(), .getValue()
        out.clear();
        {
            Collection<Entry<Integer, String>> entries = lm.entrySet();
            Assert.assertEquals(Arrays.asList(), out);
            Iterator<Entry<Integer, String>> it = entries.iterator();
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals(Arrays.asList(), out);
            Entry<Integer, String> e = it.next();
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertEquals(1, e.getKey().intValue());
            Assert.assertEquals(Arrays.asList(), out);
            Assert.assertEquals("77alpha", e.getValue());
            Assert.assertEquals(Arrays.asList("77alpha"), out);
        }
    }

    private static Function<Integer, String>
    func(String s, Collection<String> out) {
        return in -> {
            String result = in + s;
            out.add(result);
            return result;
        };
    }
}
