
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

package de.unkrig.commons.lang;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.unkrig.commons.nullanalysis.Nullable;

public final
class ClassLoaders {

    private
    ClassLoaders() {}

    /**
     * Returns the locations of all resources "under" a given directory name.
     * <p>
     *   Iff the <var>name</var> does not end with a slash, then calling this method is equivalent with calling
     *   {@link ClassLoader#getResources(String)}.
     * </p>
     * <p>
     *   Otherwise, if the <var>name</var> <em>does</em> end with a slash, then this method returns the locations of
     *   all resources who's names <em>begin</em> with the given <var>name</var>. Iff <var>includeDirectories</var> is
     *   {@code true}, then <var>name</var>, and all the subdirectories underneath, are also included in the result
     *   set.
     * </p>
     * <p>
     *   Notice that it is not (reliably) possible to determine the <var>names</var> of the retrieved resources; to
     *   get these, use {@link #getSubresources(ClassLoader, String, boolean)}.
     * </p>
     *
     * @param classLoader The class loader to use; {@code null} means use the system class loader
     * @param name        No leading slash
     */
    public static URL[]
    getAllSubresources(@Nullable ClassLoader classLoader, String name, boolean includeDirectories) throws IOException {

        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        assert classLoader != null;

        List<URL> result = new ArrayList<URL>();
        for (URL r : Collections.list(classLoader.getResources(name))) {
            result.addAll(ClassLoaders.getSubresourcesOf(r, name, includeDirectories).values());
        }

        return result.toArray(new URL[result.size()]);
    }

    /**
     * Returns a name-to-URL mapping of all resources "under" a given directory name.
     * <p>
     *   Iff the <var>name</var> does not end with a slash, then calling this method is equivalent with calling
     *   {@link ClassLoader#getResource(String)}.
     * </p>
     * <p>
     *   Otherwise, if the <var>name</var> <em>does</em> end with a slash, then this method returns a name-to-URL
     *   mapping of all resources who's names <em>begin</em> with the given <var>name</var>. Iff
     *   <var>includeDirectories</var> is {@code true}, then <var>name</var>, and all the subdirectories underneath,
     *   are also included in the result set; their names all ending with a slash.
     * </p>
     * <p>
     *   If multiple resources have the <var>name</var>, then the resources are retrieved from the <var>first</var>
     *   occurrence.
     * </p>
     *
     * @param classLoader The class loader to use; {@code null} means use the system class loader
     * @param name        No leading slash
     * @return            Keys ending with a slash map to "directory resources", the other keys map to "content
     *                    resources"
     */
    public static Map<String, URL>
    getSubresources(@Nullable ClassLoader classLoader, String name, boolean includeDirectories) throws IOException {

        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        assert classLoader != null;

        URL r = classLoader.getResource(name);
        if (r == null) return Collections.emptyMap();

        return ClassLoaders.getSubresourcesOf(r, name, includeDirectories);
    }

    /**
     * Returns a name-to-URL mapping of all resources "under" a given root resource.
     * <p>
     *   If the <var>root</var> designates a "content resource" (as opposed to a "directory resource"), then the
     *   method returns {@code Collections.singletonMap(name, rootName)}.
     * </p>
     * <p>
     *   Otherwise, if the <var>root</var> designates a "directory resource", then this method returns a name-to-URL
     *   mapping of all resources that are located "under" the root resource. Iff <var>includeDirectories</var> is
     *   {@code true}, then <var>rootName</var>, and all the subdirectory resources underneath, are also included in
     *   the result set; their names all ending with a slash.
     * </p>
     *
     * @return Keys ending with a slash map to "directory resources", the other keys map to "content resources"
     */
    public static Map<String, URL>
    getSubresourcesOf(URL root, String rootName, boolean includeDirectories) throws IOException {

        String protocol = root.getProtocol();
        if (protocol.equalsIgnoreCase("jar")) {

            JarURLConnection juc = (JarURLConnection) root.openConnection();
            juc.setUseCaches(false);

            JarFile  jarFile  = juc.getJarFile();
            JarEntry jarEntry = juc.getJarEntry();

            if (!jarEntry.isDirectory()) return Collections.singletonMap(rootName, root);

            Map<String, URL> result = new HashMap<String, URL>();
            if (includeDirectories) result.put(rootName, root);
            for (JarEntry je : Collections.list(jarFile.entries())) {
                if ((!je.isDirectory() || includeDirectories) && je.getName().startsWith(rootName)) {
                    result.put(
                        je.getName().substring(rootName.length()),
                        new URL("jar", null, "file:/" + juc.getJarFileURL().getFile() + "!/" + je.getName())
                    );
                }
            }
            return result;
        }

        if (protocol.equalsIgnoreCase("file")) {
            return ClassLoaders.getFileResources(root, rootName, includeDirectories);
        }

        return Collections.singletonMap(rootName, root);
    }

    private static Map<String, URL>
    getFileResources(URL fileUrl, String name, boolean includeDirectories) {

        File file = new File(fileUrl.getFile());

        if (file.isFile()) return Collections.singletonMap(name, fileUrl);

        if (file.isDirectory()) {
            if (!name.isEmpty() && !name.endsWith("/")) name += '/';

            Map<String, URL> result = new HashMap<String, URL>();

            if (includeDirectories) result.put(name, fileUrl);

            for (File member : file.listFiles()) {
                result.putAll(ClassLoaders.getFileResources(
                    ClassLoaders.fileUrl(member),
                    name + member.getName(),
                    includeDirectories
                ));
            }
            return result;
        }

        return Collections.emptyMap();
    }

    private static URL
    fileUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException mue) {
            throw ExceptionUtil.wrap(file.toString(), mue, IllegalStateException.class);
        }
    }
}
