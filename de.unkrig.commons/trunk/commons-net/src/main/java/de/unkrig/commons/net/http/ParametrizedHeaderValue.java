
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.net.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Parser for a "parametrized value" as defined in several HTTP-related RFCs.
 * <ul>
 *   <li><a href="https://tools.ietf.org/html/rfc2616#section-3.7">RFC2616, 3.7 Media Types</></li>
 *   <li><a href="https://tools.ietf.org/html/rfc6265#section-4.1">RFC6265, 4.1 Set-Cookie</></li>
 * </ul>
 * <p>
 *   Examples:
 * </p>
 * <pre>
 * Content-Type: text/plain; charset=ASCII
 * Set-Cookie: __VCAP_ID__=a8f33cb4-bdc9-4cef-7679-15212b52907c; Path=/; HttpOnly; Secure
 * </pre>
 * <p>
 *   "text/plain" is the {@link #getToken() token}, "char=ASCII" is a {@link #getParameter(String) parameter}
 *   (which is sometimes, e.g. in the context of RFC6265, called an "attribute"), and "HttpOnly" is a parameter
 *   with an empty value ({@code ""}).
 * </p>
 */
public
class ParametrizedHeaderValue {
    private final String token;

    /** lower-cased-name => value */
    private final Map<String, String> parameters = new HashMap<String, String>();

    public
    ParametrizedHeaderValue(String s) {

        StringTokenizer st = new StringTokenizer(s, ";");

        this.token = st.nextToken().trim();

        while (st.hasMoreTokens()) {

            String t = st.nextToken();

            String parameterName, parameterValue;
            {
                int idx = t.indexOf('=');
                if (idx == -1) {
                    parameterName  = t.trim();
                    parameterValue = "";
                } else {
                    parameterName  = t.substring(0, idx).trim();
                    parameterValue = t.substring(idx + 1).trim();
                }
            }

            this.parameters.put(parameterName.toLowerCase(), parameterValue);
        }
    }

    /** @see ParametrizedHeaderValue */
    public String
    getToken() {
        return this.token;
    }

    /**
     * @param name The (case-insensitive) parameter name
     * @return     The value of the named parameter, or {@code null} iff a parameter with that name does not exist
     * @see        ParametrizedHeaderValue
     */
    @Nullable public String
    getParameter(String name) {
        return this.parameters.get(name.toLowerCase());
    }

    /**
     * Adds the given parameter, or changes the value of an existing parameter with that <var>name</var>.
     *
     * @return The previous value of the named parameter, or {@code null} iff a parameter with that <var>name</var> did
     *         not exist
     */
    @Nullable public String
    setParameter(String name, String value) {
        return this.parameters.put(name.toLowerCase(), value);
    }

    /**
     * Removes the given parameter if it exists.
     *
     * @return The previous value of the named parameter, or {@code null} iff a parameter with that <var>name</var> did
     *         not exist
     */
    @Nullable public String
    removeParameter(String name) {
        return this.parameters.remove(name.toLowerCase());
    }

    @Override public String
    toString() {
        if (this.parameters.isEmpty()) return this.token;
        StringBuilder sb = new StringBuilder(this.token);
        for (Entry<String, String> entry : this.parameters.entrySet()) {
            String parameterName  = entry.getKey();
            String parameterValue = entry.getValue();

            sb.append("; ").append(parameterName);
            if (!parameterValue.isEmpty()) sb.append("=").append(parameterValue);
        }
        return sb.toString();
    }
}
