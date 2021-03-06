
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * {@code java.util.collection}-related utility methods.
 */
public final
class CollectionUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private
    CollectionUtil() {}

    /**
     * Removes and returns the first element of the given collection. This is also known as the 'shift' operation.
     *
     * @return {@code null} iff the collection is empty
     */
    @Nullable public static <T> T
    removeFirstFrom(Collection<T> subject) {
        Iterator<T> it = subject.iterator();
        if (!it.hasNext()) return null;
        T result = it.next();
        it.remove();
        return result;
    }

    // A number of methods that moved to "MapUtil" (because they're MAP-related, not COLLECTION-related).

    /** @deprecated Has moved to {@link MapUtil#map(Object...)} */
    @Deprecated public static <K, V> Map<K, V>
    map(Object... keyValuePairs) { return MapUtil.map(keyValuePairs); }

    /** @deprecated Has moved to {@link MapUtil#map(Object[], Object[])} */
    @Deprecated public static <K, V> Map<K, V>
    map(K[] keys, V[] values) { return MapUtil.map(keys, values); }

    /** @deprecated Has moved to {@link MapUtil#hashMapOf(java.util.Map.Entry...)} */
    @Deprecated public static <K, V> HashMap<K, V>
    hashMapOf(Map.Entry<K, V>... entries) { return MapUtil.hashMapOf(entries); }

    /** @deprecated Has moved to {@link MapUtil#hashMapOf(int, java.util.Map.Entry...)} */
    @Deprecated public static <K, V> HashMap<K, V>
    hashMapOf(int initialCapacity, Map.Entry<K, V>... entries) { return MapUtil.hashMapOf(initialCapacity, entries); }

    /** @deprecated Has moved to {@link MapUtil#treeMapOf(java.util.Map.Entry...)} */
    @Deprecated public static <K, V> TreeMap<K, V>
    treeMapOf(Map.Entry<K, V>... entries) { return MapUtil.treeMapOf(entries); }

    /** @deprecated Has moved to {@link MapUtil#putAll(Map, java.util.Map.Entry...)} */
    @Deprecated public static <K, V, M extends Map<K, V>> M
    putAll(M subject, Map.Entry<K, V>... entries) { return MapUtil.putAll(subject, entries); }

    /** @deprecated Has moved to {@link MapUtil#putAll(Map, boolean, java.util.Map.Entry...)} */
    @Deprecated public static <K, V, M extends Map<K, V>> M
    putAll(M subject, boolean allowDuplicateKeys, Map.Entry<K, V>... entries) {
        return MapUtil.putAll(subject, allowDuplicateKeys, entries);
    }

    /**
     * @deprecated Has moved to {@link MapUtil#entry(Object, Object)}
     */
    @Deprecated public static <K, V> Map.Entry<K, V>
    entry(final K key, final V initialValue) { return MapUtil.entry(key, initialValue); }

    /**
     * @deprecated Has moved to {@link MapUtil#EMPTY_SORTED_MAP}
     */
    @Deprecated @SuppressWarnings("rawtypes") public static final SortedMap
    EMPTY_SORTED_MAP = MapUtil.EMPTY_SORTED_MAP;

    /**
     * @deprecated Has moved to {@link MapUtil#emptySortedMap()}
     */
    @Deprecated @SuppressWarnings("unchecked") public static <K, V> SortedMap<K, V>
    emptySortedMap() { return MapUtil.EMPTY_SORTED_MAP; }

    /**
     * @deprecated Has moved to {@link Sets#EMPTY_SORTED_SET}
     */
    @Deprecated @SuppressWarnings("rawtypes") public static final SortedSet
    EMPTY_SORTED_SET = Sets.EMPTY_SORTED_SET;

    /**
     * @deprecated Has moved to {@link Sets#emptySortedSet()}
     */
    @Deprecated public static <T> SortedSet<T>
    emptySortedSet() { return Sets.emptySortedSet(); }

    /**
     * @deprecated Moved to {@link IteratorUtil#AT_END}
     */
    @Deprecated @SuppressWarnings("rawtypes") public static final Iterator
    AT_END = IteratorUtil.AT_END;

    /**
     * Returns an unmodifiable collection containing the elements of the given <var>collection</var>, but sorted by
     * their "natural ordering".
     */
    @SuppressWarnings("unchecked") public static <T> Collection<T>
    sorted(Collection<T> collection) {

        Object[] a = collection.toArray();

        Arrays.sort(a);

        return (Collection<T>) Arrays.asList(a);
    }

    /**
     * @return                      A {@code char[]} that contains all elements of the <var>charSet</var>
     * @throws NullPointerException The <var>charSet</var> contained a {@code null} element
     */
    public static char[]
    toCharArray(Set<Character> charSet) {

        char[] result = new char[charSet.size()];

        int i = 0;
        for (Character c : charSet) result[i++] = c;

        return result;
    }

    /**
     * @return                      An {@code int[]} that contains all elements of the <var>intSet</var>
     * @throws NullPointerException The <var>intSet</var> contained a {@code null} element
     */
    public static int[]
    toIntArray(Set<Integer> intSet) {

        int[] result = new int[intSet.size()];

        int i = 0;
        for (int v : intSet) result[i++] = v;

        return result;
    }
}
