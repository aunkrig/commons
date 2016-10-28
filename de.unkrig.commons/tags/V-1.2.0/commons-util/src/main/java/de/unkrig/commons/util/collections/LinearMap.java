
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

package de.unkrig.commons.util.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link Map} that performs very well for a SMALL number of entries. {@code null} keys and {@code null} values are
 * supported.
 *
 * @param <K> The type of the map keys
 * @param <V> The type of the map values
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) public
class LinearMap<K, V> extends AbstractMap<K, V> {

    /**
     * {@code keys[n]} maps to {@code values[n]}.
     * <p>
     * {@code keys[this.size...]} and {@code values[this.size...]} are always {@code null}.
     */
    private Object[] keys, values;

    /** @see #keys */
    private int size;

    public
    LinearMap() {
        this.keys   = new Object[4];
        this.values = new Object[4];
    }

    public
    LinearMap(int initialCapacity) {
        this.keys   = new Object[initialCapacity];
        this.values = new Object[initialCapacity];
    }

    @Override public int
    size() { return this.size; }

    @Override public boolean
    isEmpty() { return this.size == 0; }

    @Override public boolean
    containsKey(@Nullable Object key) {
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.keysEqual(this.keys[i], key)) return true;
        }
        return false;
    }

    @Override public boolean
    containsValue(@Nullable Object value) {
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.valuesEqual(this.values[i], value)) return true;
        }
        return false;
    }

    @Override @Nullable public V
    get(@Nullable Object key) {
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.keysEqual(this.keys[i], key)) return (V) this.values[i];
        }
        return null;
    }

    @Override @Nullable public V
    put(@Nullable K key, @Nullable V value) {
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.keysEqual(this.keys[i], key)) {
                final Object oldValue = this.values[i];
                this.values[i] = value;
                return (V) oldValue;
            }
        }
        int n = this.size;
        if (n == this.keys.length) {
            Object[] tmpKeys = new Object[n << 1];
            System.arraycopy(this.keys, 0, tmpKeys, 0, n);
            this.keys = tmpKeys;
            Object[] tmpValues = new Object[n << 1];
            System.arraycopy(this.values, 0, tmpValues, 0, n);
            this.values = tmpValues;
        }
        this.keys[n]   = key;
        this.values[n] = value;
        this.size      = n + 1;
        return null;
    }

    @Override @Nullable public V
    remove(@Nullable Object key) {
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.keysEqual(this.keys[i], key)) {

                final Object result = this.values[i];

                int n = this.size - 1;
                for (; i < n; i++) {
                    this.keys[i]   = this.keys[i + 1];
                    this.values[i] = this.values[i + 1];
                }
                this.keys[n]   = null;
                this.values[n] = null;
                this.size      = n;
                return (V) result;
            }
        }
        return null;
    }

    @Override public void
    clear() {
        for (int i = this.size - 1; i >= 0; i--) {
            this.keys[i]   = null;
            this.values[i] = null;
        }
        this.size = 0;
    }

    @Override public Set<Entry<K, V>>
    entrySet() {

        Set<Map.Entry<K, V>> es = this.entrySet;
        if (es == null) {
            this.entrySet = (es = new EntrySet());
        }
        return es;
    }
    @Nullable private Set<Entry<K, V>> entrySet;

    @NotNullByDefault(false) final
    class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override public int
        size() { return LinearMap.this.size; }

        @Override public boolean
        isEmpty() { return LinearMap.this.size == 0; }

        @Override public boolean
        contains(Object object) {
            if (!(object instanceof Entry)) return false;
            Entry entry = (Entry) object;
            for (int i = LinearMap.this.size - 1; i >= 0; i--) {
                if (
                    LinearMap.this.keysEqual(LinearMap.this.keys[i], entry.getKey())
                    && LinearMap.this.valuesEqual(LinearMap.this.values[i], entry.getValue())
                ) return true;
            }
            return false;
        }

        @Override public Iterator<Entry<K, V>>
        iterator() {
            return new Iterator<Entry<K, V>>() {

                private int pos = LinearMap.this.size;

                @Override public boolean
                hasNext() { return this.pos > 0; }

                @Override public Entry<K, V>
                next() {
                    if (this.pos == 0) throw new NoSuchElementException();
                    return EntrySet.this.newEntry(--this.pos);
                }

                @Override public void
                remove() {
                    if (this.pos == LinearMap.this.size) throw new IllegalStateException();
                    int n = LinearMap.this.size - 1;
                    for (int i = this.pos; i < n; i++) {
                        LinearMap.this.keys[i]   = LinearMap.this.keys[i + 1];
                        LinearMap.this.values[i] = LinearMap.this.values[i + 1];
                    }
                    LinearMap.this.keys[n]   = null;
                    LinearMap.this.values[n] = null;
                    LinearMap.this.size      = n;
                }
            };
        }

        @Override public Object[]
        toArray() {
            int      n      = LinearMap.this.size;
            Object[] result = new Object[n];
            for (int i = n - 1; i >= 0; i--) result[i] = this.newEntry(i);
            return result;
        }

        @Override public <T> T[]
        toArray(T[] result) {
            int n = LinearMap.this.size;
            if (result.length < n) {
                result = (T[]) java.lang.reflect.Array.newInstance(result.getClass().getComponentType(), n);
            }
            for (int i = n - 1; i >= 0; i--) result[i] = (T) this.newEntry(i);
            return result;
        }

        @Override public boolean
        add(Entry<K, V> entry) { return LinearMap.this.put(entry.getKey(), entry.getValue()) != null; }

        @Override public boolean
        remove(Object key) { return LinearMap.this.remove(key) != null; }

        @Override public void
        clear() { LinearMap.this.clear(); }

        private Entry<K, V>
        newEntry(final int idx) {
            return new Entry<K, V>() {

                @Override public K
                getKey() { return (K) LinearMap.this.keys[idx]; }

                @Override public V
                getValue() { return (V) LinearMap.this.values[idx]; }

                @Override public V
                setValue(Object newValue) {
                    final Object result = LinearMap.this.values[idx];
                    LinearMap.this.values[idx] = newValue;
                    return (V) result;
                }
            };
        }
    }

    /**
     * @return Whether the two keys are equal in the sense of this map; can be overridden e.g. for an 'identity map'
     * @see  IdentityHashMap
     */
    protected boolean
    keysEqual(@Nullable Object key1, @Nullable Object key2) {
        return key1 == null ? key2 == null : key1.equals(key2);
    }

    /**
     * @return Whether the two values are equal in the sense of this map
     */
    protected boolean
    valuesEqual(@Nullable Object value1, @Nullable Object value2) {
        return value1 == null ? value2 == null : value1.equals(value2);
    }
}
