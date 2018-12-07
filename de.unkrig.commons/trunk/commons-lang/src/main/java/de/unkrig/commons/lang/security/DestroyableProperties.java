
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.lang.security;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Destroyable;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A (partial) reimplementation of {@link java.util.Properties java.util.Properties}, but with the property values
 * being {@link CharSequence}s instead of {@link String}s.
 * <p>
 *   {@link #destroy()} makes a best-effort attempt to destroy any sensitive data stored in property values.
 * </p>
 */
public
interface DestroyableProperties extends Destroyable {

    /**
     * @see Properties#setProperty(String, String)
     */
    void setProperty(String key, char[] value);

    /**
     * @see Properties#getProperty(String)
     */
    @Nullable char[] getProperty(String key);

    /**
     * @see Properties#getProperty(String, String)
     */
    Set<String> propertyNames();

    /**
     * @see Properties#size()
     */
    int size();

    /**
     * @see Properties#isEmpty()
     */
    boolean isEmpty();

    /**
     * @see Properties#containsKey(Object)
     */
    boolean containsName(String name);

    /**
     * @see Properties#put(Object, Object)
     */
    void put(String name, CharSequence value);

    /**
     * @see Properties#remove(Object)
     */
    void removeProperty(String name);

    /**
     * @see Properties#putAll(Map)
     */
    void putAll(Map<? extends String, ? extends CharSequence> t);

    /**
     * @see Properties#clear()
     */
    void clear();

    /**
     * Stores these properties somewhere.
     */
    void store() throws IOException;
}
