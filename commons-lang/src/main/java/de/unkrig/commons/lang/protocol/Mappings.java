
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link Mapping}.
 */
public final
class Mappings {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Mappings() {}

    /**
     * Returns a proxy {@link Mapping} for a {@link Map}. This is very straightforward because the {@link Map} declares
     * all methods that the {@link Mapping} requires. (Actually the {@link Map} should <i>extend</i> {@link Mapping},
     * but doesn't.)
     */
    public static <K, V> Mapping<K, V>
    fromMap(final Map<K, V> map) {

        return new Mapping<K, V>() {
            @Override public boolean containsKey(@Nullable Object key) { return map.containsKey(key); }
            @Override public V       get(@Nullable Object key)         { return map.get(key);         }
            @Override public String  toString()                        { return map.toString();       }
        };
    }

    /**
     *
     * @param keysAndValues An alternating sequence of keys and values; even elements must have type {@code K}, odd
     *                      elements must have type {@code V} or {@link Producer Producer&lt;V>}
     * @return              A mapping of the keys and values
     */
    public static <K, V> Mapping<K, V>
    mapping(final Object... keysAndValues) {

        assert keysAndValues.length % 2 == 0 : Arrays.deepToString(keysAndValues);

        return new Mapping<K, V>() {

            @Override public boolean
            containsKey(@Nullable Object key) {

                for (int i = 0; i < keysAndValues.length; i += 2) {
                    if (ObjectUtil.equals(keysAndValues[i], key)) return true;
                }

                return false;
            }

            @Override @Nullable public V
            get(@Nullable Object key) {

                for (int i = 0; i < keysAndValues.length; i += 2) {
                    if (ObjectUtil.equals(keysAndValues[i], key)) {

                        Object value = keysAndValues[i + 1];

                        if (value instanceof Producer) {
                            @SuppressWarnings("unchecked") Producer<V> producer = (Producer<V>) value;
                            return producer.produce();
                        } else {
                            @SuppressWarnings("unchecked") V value2 = (V) value;
                            return value2;
                        }
                    }
                }

                return null;
            }

            @Override public String
            toString() { return Arrays.toString(keysAndValues); }
        };
    }

    /**
     * @return A {@link Mapping} with no mappings
     */
    @SuppressWarnings("unchecked") public static <K, V> Mapping<K, V>
    none() { return Mappings.NONE; }

    @SuppressWarnings("rawtypes") private static final Mapping NONE = new Mapping() {
        @Override public boolean          containsKey(@Nullable Object key) { return false;    }
        @Override @Nullable public Object get(@Nullable Object key)         { return null;     }
        @Override public String           toString()                        { return "(none)"; }
    };

    /**
     * Returns a mapping of property names to property values for the given <var>subject</var> object.
     * <p>
     *   The existence of a property "<code><i>propertyName</i></code>" is determined by the existence of one of the
     *   following, in this order:
     * </p>
     * <ul>
     *   <li>A public method "<code>get<i>PropertyName</i>()</code>"
     *   <li>A public method "<code>is<i>PropertyName</i>()</code>"
     *   <li>A public method "<code>has<i>PropertyName</i>()</code>"
     *   <li>A public method "<code><i>propertyName</i>()</code>"
     *   <li>A public field named "<code><i>propertyName</i></code>"
     * </ul>
     *
     * @return A mapping of property name to property value
     */
    public static Mapping<String, Object>
    propertiesOf(final Object subject) {

        final Class<? extends Object> clasS = subject.getClass();

        if (clasS.isArray()) {
            return Mappings.mapping("length", ((Object[]) subject).length);
        }

        return new Mapping<String, Object>() {

            @Override @Nullable public Object
            get(@Nullable Object key) {
                assert key instanceof String;
                Object result = this.get((String) key);
                return result == this ? null : result;
            }

            @Override public boolean
            containsKey(@Nullable Object key) {
                assert key instanceof String;
                return this.get((String) key) != this;
            }

            @Nullable private Object
            get(String propertyName) {

                // Try to invoke 'get' plus the property name.
                for (String getterMethodPrefix : new String[] { "get", "is", "has" }) {
                    try {
                        String getterMethodName = (
                            getterMethodPrefix
                            + Character.toUpperCase(propertyName.charAt(0))
                            + propertyName.substring(1)
                        );
                        Method m = clasS.getMethod(getterMethodName);
                        m.setAccessible(true); // To be able to access non-public classes.
                        return m.invoke(subject);
                    } catch (InvocationTargetException ite) {
                        throw new RuntimeException(ite.getTargetException()); // SUPPRESS CHECKSTYLE AvoidHidingCause
                    } catch (Exception e) {
                        ;
                    }
                }

                // For properties like 'is...', 'has...', 'can...', try to invoke the method with the property name.
                try {
                    Method m = clasS.getMethod(propertyName);
                    m.setAccessible(true); // To be able to access non-public classes.
                    return m.invoke(subject);
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException(ite.getTargetException()); // SUPPRESS CHECKSTYLE AvoidHidingCause
                } catch (Exception e) {
                    ;
                }

                try {
                    return clasS.getField(propertyName).get(subject);
                } catch (Exception e) {
                    ;
                }

                return this;
            }

            @Override public String
            toString() { return "Properties of \"" + subject.toString() + "\" (" + subject.getClass() + ")"; }
        };
    }

    /**
     * Invokes {@link Mapping#get(Object)} on the <var>mapping</var> and, if the result is not {@code null}, converts
     * it to the given <var>targetType</var>.
     *
     * @throws IllegalArgumentException The type of the value of the property is not assignable to {@code T}
     */
    @Nullable public static <K, T> T
    get(Mapping<? extends K, ?> mapping, Object key, Class<T> targetType) {

        Object value = mapping.get(key);

        if (value == null) return null;

        Class<? extends Object> actualType = value.getClass();

        try {
            if (
                targetType.isAssignableFrom(actualType)
                || (targetType.isPrimitive() && actualType.getDeclaredField("TYPE").get(null) == targetType)
            ) {
                @SuppressWarnings("unchecked") T tmp = (T) value;
                return tmp;
            }
        } catch (Exception e) {
            ;
        }

        throw new IllegalArgumentException(
            "Value '"
            + value
            + "' of key '"
            + key
            + "' of mapping '"
            + mapping
            + "' has unexpected type '"
            + actualType
            + "' - expected '"
            + targetType
            + "'"
        );
    }

    /**
     * @return                          The value to which the <var>mapping</var> maps the <var>key</var>
     * @throws IllegalArgumentException The <var>mapping</var> does not contain the given <var>key</var>
     * @throws IllegalArgumentException The <var>mapping</var> contains the <var>key</var>, but the mapped value is
     *                                  {@code null}
     */
    public static <K> Object
    getNonNull(Mapping<K, ?> mapping, String key) {

        Object value = mapping.get(key);

        if (value == null) {

            if (!mapping.containsKey(key)) {
                throw new IllegalArgumentException("Mapping '" + mapping + "' does not contain the key '" + key + "'");
            } else {
                throw new IllegalArgumentException("Value of key '" + key + "' of mapping '" + mapping + "' is <null>");
            }
        }

        return value;
    }

    /**
     * @return                          The value to which the <var>mapping</var> maps the <var>key</var>
     * @throws IllegalArgumentException The <var>mapping</var> does not contain the given <var>key</var>
     * @throws IllegalArgumentException The <var>mapping</var> contains the <var>key</var>, but the mapped value is
     *                                  {@code null}
     * @throws IllegalArgumentException The <var>mapping</var> contains the <var>key</var>, but the mapped value is not
     *                                  assignable to {@code T}
     */
    public static <K, T> T
    getNonNull(Mapping<K, ?> mapping, String key, Class<T> targetType) {

        T value = Mappings.get(mapping, key, targetType);

        if (value == null) {

            if (!mapping.containsKey(key)) {
                throw new IllegalArgumentException("Mapping '" + mapping + "' does not contain the key '" + key + "'");
            } else {
                throw new IllegalArgumentException("Value of key '" + key + "' of mapping '" + mapping + "' is <null>");
            }
        }

        return value;
    }

    /**
     * Returns a mapping that implements the "union" of two mappings.
     * <p>
     *   For the returned {@link Mapping}, the following conditions apply:
     * </p>
     * <ul>
     *   <li>
     *     A key is contained in the result iff it is contained in (at least) one of the operands.
     *   </li>
     *   <li>
     *     The value mapped to a key is the value mapped to the key in the <i>first</i> of the operands which contains
     *     the key, or {@code null} iff neither of <var>op1</var> and <var>op2</var> contain the key.
     *   </li>
     * </ul>
     */
    public static <K, V> Mapping<K, V>
    union(final Mapping<K, V> op1, final Mapping<K, V> op2) {

        if (op1 == Mappings.NONE) return op2;
        if (op2 == Mappings.NONE) return op1;

        return new Mapping<K, V>() {

            @Override public boolean
            containsKey(@Nullable Object key) { return op1.containsKey(key) || op2.containsKey(key); }

            @Override @Nullable public V
            get(@Nullable Object key) {
                V value = op1.get(key);
                return value != null || op1.containsKey(key) ? value : op2.get(key);
            }

            @Override public String
            toString() { return "(union of " + op1 + " and " + op2 + ")"; }
        };
    }

    /**
     * Equivalent with {@link #union(Mapping, Mapping)}, where the first argument is <var>in</var> and the second is
     * constructed from the given <var>keyValuePairs</var>.
     *
     * @param keysAndValues An alternating sequence of keys and values; even elements must have type {@code K}, odd
     *                      elements must have type {@code V} or {@link Producer Producer&lt;V>}
     */
    public static <K, V> Mapping<K, V>
    augment(Mapping<K, V> in, Object... keysAndValues) {
        return Mappings.union(in, Mappings.<K, V>mapping(keysAndValues));
    }

    /**
     * Equivalent with {@link #union(Mapping, Mapping)}, where the first argument is constructed from the given {@code
     * keyValuePairs} and the second argument is <var>in</var>.
     *
     * @param keysAndValues An alternating sequence of keys and values; even elements must have type {@code K}, odd
     *                      elements must have type {@code V} or {@link Producer Producer&lt;V>}
     */
    public static <K, V> Mapping<K, V>
    override(Mapping<K, V> in, Object... keysAndValues) {
        return Mappings.union(Mappings.<K, V>mapping(keysAndValues), in);
    }

    /**
     * @return A mapping which always returns <var>value</var>, independent from the <var>key</var>value
     */
    public static <K, V> Mapping<K, V>
    constant(final V constantValue) {

        return new Mapping<K, V>() {
            @Override public boolean     containsKey(@Nullable Object key) { return true;          }
            @Override @Nullable public V get(@Nullable Object key)         { return constantValue; }
        };
    }

    /**
     * @return A predicate that indicates whether a given key exists in the <var>delegate</var> mapping.
     */
    public static <K, V> Predicate<K>
    containsKeyPredicate(final Mapping<K, V> delegate) {
        return new Predicate<K>() {
            @Override public boolean evaluate(K key) { return delegate.containsKey(key); }
        };
    }

    /**
     * @return A predicate that indicates whether a given key exists in the <var>delegate</var> mapping.
     */
    public static <K, V, EX extends Throwable> PredicateWhichThrows<K, EX>
    containsKeyPredicateWhichThrows(final Mapping<K, V> delegate) {
        return new PredicateWhichThrows<K, EX>() {
            @Override public boolean evaluate(K key) { return delegate.containsKey(key); }
        };
    }
}
