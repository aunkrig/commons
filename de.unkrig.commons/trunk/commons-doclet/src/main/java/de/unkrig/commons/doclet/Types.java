
/*
 * de.unkrig.commons.doclet - Writing doclets made easy
 *
 * Copyright (c) 2015, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Type;

import de.unkrig.commons.lang.protocol.Longjump;

/**
 * Helper functionality related to doclet {@link Type}s.
 */
public final
class Types {

    private Types() {}

    /**
     * @return          The class described by the given {@link Type}
     * @throws Longjump A class described by the given {@link Type} could not be loaded
     */
    public static Class<?>
    loadType(SourcePosition position, Type t, DocErrorReporter errorReporter) throws Longjump {

        String cn = t.qualifiedTypeName();
        for (;;) {
            try {
                return Types.class.getClassLoader().loadClass(cn);
            } catch (Exception e) {
                int idx = cn.lastIndexOf('.');
                if (idx == -1) {
                    errorReporter.printError(position, t.qualifiedTypeName() + "': " + e.getMessage());
                    // SUPPRESS CHECKSTYLE AvoidHidingCause
                    throw new Longjump();
                }
                cn = cn.substring(0, idx) + '$' + cn.substring(idx + 1);
            }
        }
    }

    /**
     * @return "com.acme.pkg.Outer$Inner$Inner2"
     */
    public static String
    className(ClassDoc classDoc) {

        // "Outer.Inner.Inner2" => "Outer$Inner$Inner2"
        String name = classDoc.name().replace('.', '$');

        String packageName = classDoc.containingPackage().name();

        return packageName.isEmpty() ? name : packageName + '.' + name;
    }
}
