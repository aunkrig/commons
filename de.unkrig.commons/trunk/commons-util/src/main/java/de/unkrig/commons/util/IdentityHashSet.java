
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

package de.unkrig.commons.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * The combination of the {@link IdentityHashMap} and the {@link Set}.
 *
 * @param <E> The element type
 */
public
class IdentityHashSet<E> extends AbstractSet<E> {

    private transient IdentityHashMap<E, Object> map;

    private static final Object PRESENT = new Object();

    public
    IdentityHashSet() { this.map = new IdentityHashMap<E, Object>(); }

    public
    IdentityHashSet(Collection<? extends E> c) {
        this.map = new IdentityHashMap<E, Object>(Math.max((int) (c.size() / .75f) + 1, 16));
        this.addAll(c);
    }

    public
    IdentityHashSet(int expectedMaxSize) { this.map = new IdentityHashMap<E, Object>(expectedMaxSize); }

    @Override public Iterator<E>
    iterator() { return this.map.keySet().iterator(); }

    @Override public int
    size() { return this.map.size(); }

    @Override public boolean
    isEmpty() { return this.map.isEmpty(); }

    @Override public boolean
    contains(@Nullable Object o) { return this.map.containsKey(o); }

    @Override public boolean
    add(@Nullable E e) { return this.map.put(e, IdentityHashSet.PRESENT) == null; }

    @Override public boolean
    remove(@Nullable Object o) { return this.map.remove(o) == IdentityHashSet.PRESENT; }

    @Override public void
    clear() { this.map.clear(); }

    @Override public Object
    clone() {
        try {
            @SuppressWarnings("unchecked") IdentityHashSet<E>
            newSet = (IdentityHashSet<E>) super.clone();

            @SuppressWarnings("unchecked") IdentityHashMap<E, Object>
            map = (IdentityHashMap<E, Object>) this.map.clone();

            newSet.map = map;

            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
