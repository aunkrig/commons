
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

package de.unkrig.commons.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceSignatureVisitor;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility class for simple handling of class/method descriptors/signatures.
 */
public final
class Signature {

    private
    Signature() {}

    /**
     * Converts a class descriptor / signature into strings like
     * <pre>
     * List&lt;T extends java.lang.Exception></pre>
     * Notice that the result does not include the accessibility.
     */
    public static String
    toString(String className, @Nullable String classSignature) {
        if (classSignature == null) {
            return className.replace('/', '.');
        }
        SignatureWriter sw = new SignatureWriter();
        new SignatureReader(classSignature).accept(sw);
        return sw.toString();
    }

    /**
     * Converts an ASM method node into strings like
     * <pre>
     * meth(java.lang.String, java.util.Map)}
     * &lt;T extends java.io.IOException> meth(T, double, java.lang.List&lt;T>)</pre>
     * Notice that the result includes neither the return type nor the accessibility.
     */
    public static String
    toString(MethodNode methodNode) {
        return Signature.toString(methodNode.name, methodNode.desc, methodNode.signature);
    }

    /**
     * Converts a method descriptor / signature into strings like
     * <pre>
     * meth(java.lang.String, java.util.Map)
     * &lt;T extends java.io.IOException>meth(T, double, java.lang.List&lt;T>)</pre>
     * Notice that the result includes neither the return type nor the accessibility.
     */
    public static String
    toString(String methodName, String methodDescriptor, @Nullable String methodSignature) {

        if (methodSignature != null) {
            TraceSignatureVisitor tsv = new TraceSignatureVisitor(0);
            new SignatureReader(methodSignature).accept(tsv);
            return methodName + tsv.getDeclaration();
        }

        StringBuilder sb  = new StringBuilder(methodName).append('(');
        Type[]        ats = Type.getArgumentTypes(methodDescriptor);
        if (ats.length >= 1) {
            for (int i = 0;;) {
                sb.append(ats[i++].getClassName());
                if (i == ats.length) break;
                sb.append(", ");
            }
        }
        return sb.append(')').toString();
    }
}
