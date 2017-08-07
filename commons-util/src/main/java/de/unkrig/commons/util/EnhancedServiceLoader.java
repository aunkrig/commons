
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

package de.unkrig.commons.util;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceConfigurationError;

import de.unkrig.commons.lang.ExceptionUtil;

/**
 * An enhanced version of the {@link java.util.ServiceLoader} class.
 * <p>
 *   While the strategy for searching impementations is identical, the following enhancments exist:
 * </p>
 * <ul>
 *   <li>
 *     The {@link #load(Class)} method caches the instances for each service. (Use {@link #reload(Class)} to get
 *     <em>uncached</em> services.)
 *   </li>
 *   <li>
 *     Service instances are not necessarily created by {@link Class#newInstance()} (unflexible), but, alternatively,
 *     by configuring a (static) class field or a (static, zero-parameter) method that provide the service instance.
 *   </li>
 *   <li>
 *     Instead of the (idiotic?) concept of {@link java.util.ServiceLoader} <em>implementing</em> the {@code
 *     Iterable<S>}, {@link EnhancedServiceLoader#load(Class)} <em>returns</em> an {@code Iterable<S>}.
 *   </li>
 *   <li>
 *     {@link #DEFAULT} is a singelton that loads services through the system class loader.
 *   </li>
 * </ul>
 */
public final
class EnhancedServiceLoader {

    /**
     * An instance that loads services through the system class loader.
     */
    public static final EnhancedServiceLoader DEFAULT = new EnhancedServiceLoader(ClassLoader.getSystemClassLoader());

    /**
     * Initializes an instance that will load services through a given class loader.
     */
    public
    EnhancedServiceLoader(ClassLoader loader) { this.loader = loader; }

    /**
     * @return All available implementations of the <var>service</var>, cached
     */
    public <S> Iterable<S>
    load(Class<S> service) throws ServiceConfigurationError {

        synchronized (this.cache) {

            @SuppressWarnings("unchecked") Iterable<S>
            result = (Iterable<S>) this.cache.get(service);

            if (result != null) return result;

            return this.reload(service);
        }
    }

    /**
     * @return                           All available implementations of the <var>service</var>, not cached
     * @throws ServiceConfigurationError Some of the services are badly configured
     */
    public <S> Iterable<S>
    reload(Class<S> service) throws ServiceConfigurationError {

        Iterable<S> result;
        try {
            result = this.load2(service);
        } catch (ServiceConfigurationError sce) {
            throw ExceptionUtil.wrap("Service " + service, sce);
        } catch (Exception e) {
            throw ExceptionUtil.wrap("Service " + service, e, ServiceConfigurationError.class);
        }

        synchronized (this.cache) {
            this.cache.put(service, result);
        }

        return result;
    }

    // ===================================================

    private final ClassLoader                loader;
    private final Map<Class<?>, Iterable<?>> cache = new HashMap<Class<?>, Iterable<?>>();

    /**
     * @return           All implementations of the given <var>service</var> that are available through the {@link
     *                   #loader}
     * @see              ClassLoader#getResources(String)
     * @throws Exception All kinds of exceptional conditions that may occur while instantiating services
     */
    private <S> Iterable<S>
    load2(Class<S> service) throws Exception {

        List<S> result = new ArrayList<S>();

        // Scan all resources under "META-INF/services/" for implementations.
        for (
            Enumeration<URL> en = this.loader.getResources("META-INF/services/" + service.getName());
            en.hasMoreElements();
        ) {
            URL url = en.nextElement();

            // Load the resource as a properties file.
            Properties properties = new Properties();
            {
                InputStream is = url.openStream();
                try {
                    properties.load(is);
                    is.close();
                } finally {
                    try { is.close(); } catch (Exception e) {}
                }
            }

            // For compatibility with the java.util.ServiceLoader all properties with an empty value represent the
            // names of eligible classes.
            List<String> classNames = new ArrayList<String>();
            for (Entry<Object, Object> e : properties.entrySet()) {
                String key   = (String) e.getKey();
                String value = (String) e.getValue();

                if (value.isEmpty()) classNames.add(key);
            }

            // Sort the class names alphabetically, so that results will be deterministic.
            Collections.sort(classNames);

            for (String className : classNames) {

                // Get the implementing object instance.
                Object instance = EnhancedServiceLoader.getInstance(
                    this.loader.loadClass(className),
                    properties,
                    className + "."
                );

                // Verify that the actual type implements the service.
                Class<? extends Object> actualType = instance.getClass();
                if (!service.isAssignableFrom(actualType)) {
                    throw new ServiceConfigurationError(actualType + " does not extend or implement the service");
                }

                @SuppressWarnings("unchecked") S instance2 = (S) instance;

                result.add(instance2);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns an object based on the parameters. Notice that the returned object is not necessarily a subclass of
     * the <var>clasS</var>.
     */
    private static <S> Object
    getInstance(Class<?> clasS, Properties properties, String keyPrefix) throws Exception {

        // Is there a ".instanceField" property? If so, use the value of the named static field.
        String instanceFieldName = properties.getProperty(keyPrefix + "instanceField");
        if (instanceFieldName != null) {
            return clasS.getField(instanceFieldName).get(null);
        }

        // Is there a ".instanceMethod" property? If so, invoke the static zero-parameter method and use its return
        // value.
        String instanceMethodName = properties.getProperty(keyPrefix + "instanceMethod");
        if (instanceMethodName != null) {
            Method method = clasS.getMethod(instanceMethodName);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException(method + " is not static");
            }
            return method.invoke(null);
        }

        // Instantiate by calling the zero-parameter constructor.
        return clasS.newInstance();
    }
}
