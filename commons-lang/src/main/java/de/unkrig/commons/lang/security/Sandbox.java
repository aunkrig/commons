
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

package de.unkrig.commons.lang.security;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * This class establishes a security manager that confines the permissions for code executed through specific classes,
 * which may be specified by class, class name and/or class loader.
 * <p>
 *   To 'execute through a class' means that the execution stack includes the class. E.g., if a method of class {@code
 *   A} invokes a method of class {@code B}, which then invokes a method of class {@code C}, and all three classes were
 *   previously {@link #confine(Class, Permissions) confined}, then for all actions that are executed by class {@code
 *   C} the <i>intersection</i> of the three {@link Permissions} apply.
 * </p>
 * <p>
 *   Once the permissions for a class, class name or class loader are confined, they cannot be changed; this prevents
 *   any attempts (e.g. of a confined class itself) to release the confinement.
 * </p>
 * <p>
 *   Code example:
 * </p>
 * <pre>
 *  Runnable unprivileged = new Runnable() {
 *      public void run() {
 *          System.getProperty("user.dir");
 *      }
 *  };
 *
 *  // Run without confinement.
 *  unprivileged.run(); // Works fine.
 *
 *  // Set the most strict permissions.
 *  Sandbox.confine(unprivileged.getClass(), new Permissions());
 *  unprivileged.run(); // Throws a SecurityException.
 *
 *  // Attempt to change the permissions.
 *  {
 *      Permissions permissions = new Permissions();
 *      permissions.add(new AllPermission());
 *      Sandbox.confine(unprivileged.getClass(), permissions); // Throws a SecurityException.
 *  }
 *  unprivileged.run();
 * </pre>
 *
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/environment/security.html">ORACLE: Java Essentials: The Security Manager</a>
 */
public final
class Sandbox {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Sandbox() {}

    private static final Map<Class<?>, AccessControlContext>
    CONFINED_CLASSES = Collections.synchronizedMap(new WeakHashMap<Class<?>, AccessControlContext>());

    private static final Map<String, AccessControlContext>
    CONFINED_CLASS_NAMES = new ConcurrentHashMap<String, AccessControlContext>();

    private static final Map<ClassLoader, AccessControlContext>
    CONFINED_CLASS_LOADERS = Collections.synchronizedMap(new WeakHashMap<ClassLoader, AccessControlContext>());

    static {

        // Install our custom security manager.
        if (System.getSecurityManager() != null) {
            throw new ExceptionInInitializerError("There's already a security manager set");
        }
        System.setSecurityManager(new SecurityManager() {

            @Override public void
            checkPermission(@Nullable Permission perm) {
                assert perm != null;

                for (Class<?> clasS : this.getClassContext()) {

                    // Check if an ACC was set for the class.
                    {
                        AccessControlContext acc = Sandbox.CONFINED_CLASSES.get(clasS);
                        if (acc != null) acc.checkPermission(perm);
                    }

                    // Check if an ACC was set for the class name.
                    {
                        AccessControlContext acc = Sandbox.CONFINED_CLASS_NAMES.get(clasS.getName());
                        if (acc != null) acc.checkPermission(perm);
                    }

                    // Check if an ACC was set for the class loader.
                    {
                        AccessControlContext acc = Sandbox.CONFINED_CLASS_LOADERS.get(clasS.getClassLoader());
                        if (acc != null) acc.checkPermission(perm);
                    }
                }
            }
        });
    }

    // --------------------------

    /**
     * All future actions that are executed through the given <var>clasS</var> will be checked against the given {@code
     * accessControlContext}.
     *
     * @throws SecurityException Permissions are already confined for the <var>clasS</var>
     */
    public static void
    confine(Class<?> clasS, AccessControlContext accessControlContext) {

        if (Sandbox.CONFINED_CLASSES.containsKey(clasS)) {
            throw new SecurityException("Attempt to change the access control context for '" + clasS + "'");
        }

        Sandbox.CONFINED_CLASSES.put(clasS, accessControlContext);
    }

    /**
     * All future actions that are executed through the given <var>clasS</var> will be checked against the given {@code
     * protectionDomain}.
     *
     * @throws SecurityException Permissions are already confined for the <var>clasS</var>
     */
    public static void
    confine(Class<?> clasS, ProtectionDomain protectionDomain) {
        Sandbox.confine(clasS, new AccessControlContext(new ProtectionDomain[] { protectionDomain }));
    }

    /**
     * All future actions that are executed through the given <var>clasS</var> will be checked against the given {@code
     * permissions}.
     *
     * @throws SecurityException Permissions are already confined for the <var>clasS</var>
     */
    public static void
    confine(Class<?> clasS, Permissions permissions) {
        Sandbox.confine(clasS, new ProtectionDomain(null, permissions));
    }

    // --------------------------

    /**
     * All future actions that are executed through the named class will be checked against the given {@code
     * accessControlContext}.
     *
     * @throws SecurityException Permissions are already confined for the <var>className</var>
     */
    public static void
    confine(String className, AccessControlContext accessControlContext) {

        if (Sandbox.CONFINED_CLASS_NAMES.containsKey(className)) {
            throw new SecurityException("Attempt to change the access control context for '" + className + "'");
        }

        Sandbox.CONFINED_CLASS_NAMES.put(className, accessControlContext);
    }

    /**
     * All future actions that are executed through the named class will be checked against the given {@code
     * protectionDomain}.
     *
     * @throws SecurityException Permissions are already confined for the <var>className</var>
     */
    public static void
    confine(String className, ProtectionDomain protectionDomain) {
        Sandbox.confine(className, new AccessControlContext(new ProtectionDomain[] { protectionDomain }));
    }

    /**
     * All future actions that are executed through the named class will be checked against the given {@code
     * permissions}.
     *
     * @throws SecurityException Permissions are already confined for the <var>className</var>
     */
    public static void
    confine(String className, Permissions permissions) {
        Sandbox.confine(className, new ProtectionDomain(null, permissions));
    }

    // --------------------------

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>accessControlContext</var>.
     *
     * @throws SecurityException Permissions are already confined for the <var>classLoader</var>
     */
    public static void
    confine(ClassLoader classLoader, AccessControlContext accessControlContext) {

        if (Sandbox.CONFINED_CLASS_LOADERS.containsKey(classLoader)) {
            throw new SecurityException("Attempt to change the access control context for '" + classLoader + "'");
        }

        Sandbox.CONFINED_CLASS_LOADERS.put(classLoader, accessControlContext);
    }

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>protectionDomain</var>.
     *
     * @throws SecurityException Permissions are already confined for the <var>classLoader</var>
     */
    public static void
    confine(ClassLoader classLoader, ProtectionDomain protectionDomain) {
        Sandbox.confine(classLoader, new AccessControlContext(new ProtectionDomain[] { protectionDomain }));
    }

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>permissions</var>.
     *
     * @throws SecurityException Permissions are already confined for the <var>classLoader</var>
     */
    public static void
    confine(ClassLoader classLoader, Permissions permissions) {
        Sandbox.confine(classLoader, new ProtectionDomain(null, permissions));
    }
}
