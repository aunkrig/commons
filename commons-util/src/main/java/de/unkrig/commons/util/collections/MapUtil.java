
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

package de.unkrig.commons.util.collections;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various utility methods for map processing.
 */
public final
class MapUtil {

    private
    MapUtil() {}

    /**
     * Returns an unmodifiable map, mapping the given key-value pairs. {@code null} keys and {@code null} values are
     * supported.
     *
     * @param keyValuePairs                   An alternating sequence of keys and values
     * @throws ArrayIndexOutOfBoundsException The length of {@code keyValuePairs} is odd
     * @throws IllegalArgumentException       Two of the keys are equal
     */
    @SuppressWarnings("unchecked") public static <K, V> Map<K, V>
    map(Object... keyValuePairs) {

        int n = keyValuePairs.length;
        if ((n & 1) == 1) throw new ArrayIndexOutOfBoundsException(n);

        if (n == 0) return Collections.emptyMap();
        if (n == 2) return Collections.singletonMap((K) keyValuePairs[0], (V) keyValuePairs[1]);

        Map<K, V> result = (
            n <= 8
            ? new LinearMap<K, V>(n / 2)
            : new HashMap<K, V>(n)
        );

        for (int i = 0; i < n;) {
            if (result.put((K) keyValuePairs[i++], (V) keyValuePairs[i++]) != null) {
                throw new IllegalArgumentException("Duplicate key '" + keyValuePairs[i - 2]);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an unmodifiable map, mapping the given key-value pairs. {@code null} keys and {@code null} values are
     * supported.
     *
     * @throws ArrayIndexOutOfBoundsException The length of {@code keyValuePairs} is odd
     * @throws IllegalArgumentException       Two of the keys are equal
     */
    public static <K, V> Map<K, V>
    map(K[] keys, V[] values) {

        int n = keys.length;
        assert n == values.length;

        if (n == 0) return Collections.emptyMap();
        if (n == 1) return Collections.singletonMap(keys[0], values[0]);

        Map<K, V> result = (
            n <= 4
            ? new LinearMap<K, V>(n)
            : new HashMap<K, V>(2 * n)
        );
        for (int i = 0; i < n; i++) {
            if (result.put(keys[i], values[i]) != null) {
                throw new IllegalArgumentException("Duplicate key '" + keys[i]);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates, fills and returns a {@link HashMap}. The initial capacity of the {@link HashMap} is chosen to be
     * optimal for the number of <var>entries</var>.
     *
     * @return                          A {@link HashMap} containing all <var>entries/<var>
     * @throws IllegalArgumentException Two <var>entries</var> have equal keys
     */
    // @SafeVarags <= Only available in Java 7
    public static <K, V> HashMap<K, V>
    hashMapOf(Map.Entry<? extends K, ? extends V>... entries) {
        return MapUtil.putAll(new HashMap<K, V>((4 * entries.length) / 3), entries);
    }

    /**
     * @param initialCapacity           See the documentation of {@link HashMap}
     * @return                          A {@link HashMap} containing all <var>entries/<var>
     * @throws IllegalArgumentException Two <var>entries</var> have equal keys
     */
    // @SafeVarags <= Only available in Java 7
    public static <K, V> HashMap<K, V>
    hashMapOf(int initialCapacity, Map.Entry<? extends K, ? extends V>... entries) {
        return MapUtil.putAll(new HashMap<K, V>(initialCapacity), entries);
    }

    /**
     * @return                          A {@link TreeMap} containing all <var>entries/<var>
     * @throws IllegalArgumentException Two <var>entries</var> have equal keys
     */
    // @SafeVarags <= Only available in Java 7
    public static <K, V> TreeMap<K, V>
    treeMapOf(Map.Entry<? extends K, ? extends V>... entries) { return MapUtil.putAll(new TreeMap<K, V>(), entries); }

    /**
     * Puts all <var>entries</var> into the <var>subject</var>.
     *
     * @return                          The <var>subject</var>
     * @throws IllegalArgumentException Two <var>entries</var> have equal keys
     */
    // @SafeVarags <= Only available in Java 7
    public static <K, V, M extends Map<K, V>> M
    putAll(M subject, Map.Entry<? extends K, ? extends V>... entries) {

        int size = subject.size();
        for (Map.Entry<? extends K, ? extends V> e : entries) {
            K key   = e.getKey();
            V value = e.getValue();

            V previousValue = subject.put(key, value);

            // To avoid the overhead of an additional "containsKey()" call, we check the map size instead, which is
            // (supposedly) faster.
            if (subject.size() <= size) {
                throw new IllegalArgumentException(
                    "Duplicate key \""
                    + key
                    + "\" (offending values are \""
                    + previousValue
                    + "\" and \""
                    + value
                    + "\")"
                );
            }

            size++;
        }

        return subject;
    }

    /**
     * Puts all <var>entries</var> into the <var>subject</var>.
     *
     * @return                          The <var>subject</var>
     * @throws IllegalArgumentException Two <var>entries</var> have equal keys, and <var>allowDuplicateKeys</var> is
     *                                  {@code false}
     */
    // @SafeVarags <= Only available in Java 7
    public static <K, V, M extends Map<K, V>> M
    putAll(M subject, boolean allowDuplicateKeys, Map.Entry<? extends K, ? extends V>... entries) {

        if (allowDuplicateKeys) {
            for (Map.Entry<? extends K, ? extends V> e : entries) subject.put(e.getKey(), e.getValue());
        } else {
            MapUtil.putAll(subject, entries);
        }

        return subject;
    }

    /**
     * @return                                   A {@link java.util.Map.Entry} with the given <var>key</var> and the
     *                                           <var>initialValue</var>
     * @see java.util.Map.Entry#setValue(Object)
     */
    public static <K, V> Map.Entry<K, V>
    entry(final K key, final V initialValue) {

        return new Map.Entry<K, V>() {

            @Nullable V value = initialValue;

            @Override @Nullable public K
            getKey() { return key; }

            @Override @Nullable public V
            getValue() { return this.value; }

            @Override @Nullable public V
            setValue(@Nullable V value) {
                V result = this.value;
                this.value = value;
                return result;
            }

            @Override public String
            toString() { return key + " => " + this.value; }
        };
    }

    /**
     * Desperately missing from {@code java.util.Collections}.
     */
    @SuppressWarnings("rawtypes") public static final SortedMap
    EMPTY_SORTED_MAP = new EmptySortedMap();

    /**
     * Desperately missing from {@code java.util.Collections}.
     */
    @SuppressWarnings("unchecked") public static <K, V> SortedMap<K, V>
    emptySortedMap() { return MapUtil.EMPTY_SORTED_MAP; }

    @NotNullByDefault(false) @SuppressWarnings("rawtypes") private static
    class EmptySortedMap extends AbstractMap implements SortedMap, Serializable {

        private static final long serialVersionUID = 1;

        @Override public Comparator comparator()                         { return null; }
        @Override public SortedMap  subMap(Object fromKey, Object toKey) { return MapUtil.EMPTY_SORTED_MAP; }
        @Override public SortedMap  headMap(Object toKey)                { return MapUtil.EMPTY_SORTED_MAP; }
        @Override public SortedMap  tailMap(Object fromKey)              { return MapUtil.EMPTY_SORTED_MAP; }
        @Override public Object     firstKey()                           { throw new NoSuchElementException(); }
        @Override public Object     lastKey()                            { throw new NoSuchElementException(); }
        @Override public int        size()                               { return 0; }
        @Override public boolean    isEmpty()                            { return true; }
        @Override public boolean    containsKey(Object key)              { return false; }
        @Override public boolean    containsValue(Object value)          { return false; }
        @Override public Object     get(Object key)                      { return null; }
        @Override public Set        keySet()                             { return Collections.EMPTY_SET; }
        @Override public Collection values()                             { return Collections.EMPTY_SET; }
        @Override public Set        entrySet()                           { return Collections.EMPTY_SET; }
        @Override public boolean    equals(Object o)                     { return (o instanceof SortedMap) && ((SortedMap) o).size() == 0; } // SUPPRESS CHECKSTYLE LineLength
        @Override public int        hashCode()                           { return 0; }
    }

    /**
     * Returns a {@link Map} that is composed of <var>map1</var> and <var>map2</var>, where <var>map1</var> takes
     * precedence over <<var>map2</var>.
     * <p>
     *   All modifying operations are directed to <var>map1</var>; <var>map2</var> is never modified.
     * </p>
     */
    @NotNullByDefault(false) public static <K, V> Map<K, V>
    combine(final Map<K, V> map1, final Map<? extends K, ? extends V> map2) {
        return new Map<K, V>() {

            // SUPPRESS CHECKSTYLE LineLength:5
            @Override public void                 clear()            { throw new UnsupportedOperationException("clear");    }
            @Override public Set<Map.Entry<K, V>> entrySet()         { throw new UnsupportedOperationException("entrySet"); }
            @Override public Set<K>               keySet()           { throw new UnsupportedOperationException("keySet");   }
            @Override public V                    remove(Object key) { throw new UnsupportedOperationException("remove");   }
            @Override public Collection<V>        values()           { throw new UnsupportedOperationException("values");   }

            @Override public boolean
            containsKey(Object key) { return map1.containsKey(key) || map2.containsKey(key); }

            @Override public boolean
            containsValue(Object value) { return map1.containsValue(value) || map2.containsValue(value); }

            @Override public V
            get(Object key) {
                V value = map1.get(key);
                return value != null || map1.containsKey(key) ? value : map2.get(key);
            }

            @Override public boolean
            isEmpty() { return map1.isEmpty() && map2.isEmpty(); }

            @Override public V
            put(K key, V value) { return map1.put(key, value); }

            @Override public void
            putAll(Map<? extends K, ? extends V> map) { map1.putAll(map); }

            @Override public int
            size() {
                int result = map1.size();
                for (K key : map2.keySet()) {
                    if (!map1.containsKey(key)) result++;
                }
                return result;
            }
        };
    }

    /**
     * Returns a {@link Map} that is composed of the <var>delegate</var> and one extra entry. If the <var>delegate</var>
     * contains the <var>extraKey</var>, then the value from the <var>delegate</var> takes precedence.
     */
    @NotNullByDefault(false) public static <K, V> Map<K, V>
    augment(final Map<K, V> delegate, final K extraKey, final V extraValue) {

        return new Map<K, V>() {

            // SUPPRESS CHECKSTYLE LineLength:7
            @Override public void                 clear()                                   { throw new UnsupportedOperationException("clear");    }
            @Override public Set<Map.Entry<K, V>> entrySet()                                { throw new UnsupportedOperationException("entrySet"); }
            @Override public Set<K>               keySet()                                  { throw new UnsupportedOperationException("keySet");   }
            @Override public V                    remove(Object key)                        { throw new UnsupportedOperationException("remove");   }
            @Override public Collection<V>        values()                                  { throw new UnsupportedOperationException("values");   }
            @Override public V                    put(K key, V value)                       { throw new UnsupportedOperationException("put");      }
            @Override public void                 putAll(Map<? extends K, ? extends V> map) { throw new UnsupportedOperationException("putAll");   }

            @Override public boolean
            containsKey(Object key) { return MapUtil.equal(key, extraKey) || delegate.containsKey(key); }

            @Override public boolean
            containsValue(Object value) {
                return delegate.containsValue(value) || (
                    !delegate.containsKey(extraKey)
                    && ObjectUtil.equals(value, extraValue)
                );
            }

            @Override public boolean
            isEmpty() { return false; }

            @Override public int
            size() { return delegate.containsKey(extraKey) ? delegate.size() : delegate.size() + 1; }

            @Override public V
            get(Object key) {
                return (
                    delegate.containsKey(key) ? delegate.get(key) :
                    ObjectUtil.equals(key, extraKey) ? extraValue :
                    null
                );
            }
        };
    }

    /**
     * Returns a {@link Map} that is composed of the <var>delegate</var> and one extra entry. If the <var>delegate</var>
     * contains the <var>extraKey</var>, then the <var>extraValue</var> takes precedence.
     */
    @NotNullByDefault(false) public static <K, V> Map<K, V>
    override(final Map<K, V> delegate, final K extraKey, final V extraValue) {

        return new Map<K, V>() {

            // SUPPRESS CHECKSTYLE LineLength:7
            @Override public void                 clear()                                   { throw new UnsupportedOperationException("clear");    }
            @Override public Set<Map.Entry<K, V>> entrySet()                                { throw new UnsupportedOperationException("entrySet"); }
            @Override public Set<K>               keySet()                                  { throw new UnsupportedOperationException("keySet");   }
            @Override public V                    remove(Object key)                        { throw new UnsupportedOperationException("remove");   }
            @Override public Collection<V>        values()                                  { throw new UnsupportedOperationException("values");   }
            @Override public V                    put(K key, V value)                       { throw new UnsupportedOperationException("put");      }
            @Override public void                 putAll(Map<? extends K, ? extends V> map) { throw new UnsupportedOperationException("putAll");   }

            @Override public boolean
            containsKey(Object key) { return MapUtil.equal(key, extraKey) || delegate.containsKey(key); }

            @Override public boolean
            containsValue(Object value) {
                if (ObjectUtil.equals(value, extraValue)) return true;
                for (Map.Entry<K, V> e : delegate.entrySet()) {
                    if (ObjectUtil.equals(e.getValue(), value) && !ObjectUtil.equals(e.getKey(), extraKey)) return true;
                }
                return false;
            }

            @Override public boolean
            isEmpty() { return false; }

            @Override public int
            size() { return delegate.containsKey(extraKey) ? delegate.size() : delegate.size() + 1; }

            @Override public V
            get(Object key) { return ObjectUtil.equals(key, extraKey) ? extraValue : delegate.get(key); }
        };
    }

    /** @deprecated Moved to {@link ObjectUtil#equals(Object, Object)} */
    @Deprecated public static boolean
    equal(@Nullable Object o1, @Nullable Object o2) { return ObjectUtil.equals(o1, o2); }

    /**
     * @deprecated Use {@link #map(Object...)} instead
     */
    @Deprecated @SuppressWarnings("unchecked") public static <K, V> Map<K, V>
    fromMappings(Object... keysAndValues) {
        final Map<K, V> m = new HashMap<K, V>();
        for (int i = 0; i < keysAndValues.length;) {
            m.put((K) keysAndValues[i++], (V) keysAndValues[i++]);
        }
        return Collections.unmodifiableMap(m);
    }
}
