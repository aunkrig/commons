
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

package de.unkrig.commons.lang.protocol;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A map that computes a value only when {@link #get(Object)} is invoked.
 * Consequently, in contrast with {@link Map}, it has no specific {@link Map#size() size()}, and thus no {@link
 * Map#entrySet() entrySet()}, {@link Map#keySet() keySet()} and {@link Map#values() values()} set.
 * Also the modifying operations ({@link Map#put(Object, Object) put}, {@link Map#putAll(Map) putAll}, {@link
 * Map#remove(Object) remove} and {@link Map#clear() clear}) are all missing, because a {@link Mapping} is not changed
 * by "putting" key-value-pairs into it.
 * <p>
 *   Actually {@link Map} should extend {@link Mapping}, but it doesn't - thus there are the {@link Mapping#asMap()}
 *   and {@link Mappings#fromMap(Map)} helper methods.
 * </p>
 * <p>
 *   The relationship between {@link Mapping} and {@link Map} is very much like that between {@link Predicate} and
 *   {@link Collection}.
 * </p>
 *
 * <h3>IMPORTANT NOTICE:</h3>
 * <p>
 *   When using this type in a variable, parameter or field declaration, <b>never</b> write:
 * </p>
 * <pre>Mapping&lt;<i>key-type</i>, <i>value-type</i>&gt;</pre>
 * <p>
 *   , but always:
 * </p>
 * <pre>Mapping&lt;? super <i>key-type</i>, ? extends <i>value-type</i>&gt;</pre>
 * <p>
 *   That way many type conversions are possible  which otherwise aren't.
 * </p>
 *
 * @param <K> The 'key' type
 * @param <V> The 'value' type
 */
public abstract
class Mapping<K, V> {

    /** @see Map#containsKey(Object) */
    public abstract boolean
    containsKey(@Nullable Object key);

    /** @see Map#get(Object) */
    @Nullable public abstract V
    get(@Nullable Object key);

    /**
     * Returns a proxy {@link Map} for a {@link Mapping} where all methods declared by {@link Map} but not by
     * {@link Mapping} throw an {@link UnsupportedOperationException}.
     */
    @NotNullByDefault(false) public final Map<K, V>
    asMap() {

        return new Map<K, V>() {

            @Override public boolean containsKey(Object key) { return Mapping.this.containsKey(key); }
            @Override public V       get(Object key)         { return Mapping.this.get(key);         }

            // SUPPRESS CHECKSTYLE LineLength:16
            @Override public int              size()                                  { throw new UnsupportedOperationException("size");          }
            @Override public boolean          isEmpty()                               { throw new UnsupportedOperationException("isEmpty");       }
            @Override public boolean          containsValue(Object value)             { throw new UnsupportedOperationException("containsValue"); }
            @Override public V                put(K key, V value)                     { throw new UnsupportedOperationException("put");           }
            @Override public V                remove(Object key)                      { throw new UnsupportedOperationException("remove");        }
            @Override public void             putAll(Map<? extends K, ? extends V> t) { throw new UnsupportedOperationException("putAll");        }
            @Override public void             clear()                                 { throw new UnsupportedOperationException("clear");         }
            @Override public Set<K>           keySet()                                { throw new UnsupportedOperationException("keySet");        }
            @Override public Collection<V>    values()                                { throw new UnsupportedOperationException("values");        }
            @Override public Set<Entry<K, V>> entrySet()                              { throw new UnsupportedOperationException("entrySet");      }

            @Override public boolean equals(Object o) { return o == this;                     }
            @Override public int     hashCode()       { return System.identityHashCode(this); }
            @Override public String  toString()       { return "Mapping";                     }

            @Override protected Object clone() throws CloneNotSupportedException { throw new CloneNotSupportedException("Mapping"); }
        };
    }
}
