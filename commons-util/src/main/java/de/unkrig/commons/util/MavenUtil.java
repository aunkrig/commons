
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2021, Arno Unkrig
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * Various MAVEN-related utility methods.
 */
public final
class MavenUtil {

    static {
        AssertionUtil.enableAssertionsForThisClass();
    }

    private MavenUtil() {}

    /**
     * Determines the MAVEN artifact version.
     * <p>
     *   Iff the project is built with <a href="http://maven.apache.org">APACHE MAVEN</a>, the a synthetic properties
     *   file is injected for each artifcat, which contains the artifact's version. This method parses that resource
     *   and extracts the version.
     * </p>
     *
     * @throws IllegalStateException The MAVEN resource for the given <var>groupId</var> and <var>artifactId</var> is
     *         not on the system class path
     * @throws IllegalStateException The version cannot be determined from the resource's content
     */
    public static String
    getMavenArtifactVersion(String groupId, String artifactId) throws IOException {

        String resourceName = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";

        // Load properties from the resource.
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IllegalStateException(
                "Resource \"" + resourceName + "\" not found - cannot determine maven version"
            );
        }
        Properties p;
        try {
            p = new Properties();
            p.load(is);
            is.close();
        } finally {
            try { is.close(); } catch (Exception e ) {}
        }

        String version = p.getProperty("version");
        if (version == null) {
            throw new IllegalStateException("Version property missing from resource \"" + resourceName + "\"");
        }

        return version;
    }
}
