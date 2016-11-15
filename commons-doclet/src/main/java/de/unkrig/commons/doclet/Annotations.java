
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

package de.unkrig.commons.doclet;

import java.lang.reflect.Array;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

import de.unkrig.commons.lang.protocol.Longjump;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to doclets and annotations.
 */
public final
class Annotations {

    private Annotations() {}

    /**
     * @return The annotation with the given simple (unqualified) type name, or {@code null} iff the <var>doc</var> is
     *         not annotated with the <var>annotationTypeSimpleName</var>
     */
    @Nullable public static AnnotationDesc
    get(ProgramElementDoc doc, String annotationTypeSimpleName) {

        for (AnnotationDesc ad : doc.annotations()) {
            if (ad.annotationType().simpleTypeName().equals(annotationTypeSimpleName)) return ad;
        }

        return null;
    }

    /**
     * @return The annotation with the given <var>annotationType</var>, or {@code null} iff the <var>doc</var> is
     *         not annotated with the <var>annotationType</var>
     */
    @Nullable public static AnnotationDesc
    get(ProgramElementDoc doc, ClassDoc annotationType) {

        for (AnnotationDesc ad : doc.annotations()) {
            if (ad.annotationType().equals(annotationType)) return ad;
        }

        return null;
    }

    /**
     * @return          The annotation with the <var>annotationType</var>, or {@code null} iff the <var>doc</var> is
     *                  not annotated with the <var>annotationType</var>
     * @throws Longjump The <var>annotationType</var> is not on JAVADOC's source path or class path
     */
    @Nullable public static AnnotationDesc
    get(ProgramElementDoc doc, Class<?> annotationType, RootDoc rootDoc) throws Longjump {

        ClassDoc brpcd = rootDoc.classNamed(annotationType.getName());
        return Annotations.get(doc, brpcd);
    }

    /**
     * @return The value of the element with the given name and type, or {@code null}
     */
    @Nullable public static <T> T
    getElementValue(AnnotationDesc annotationDesc, String elementName, Class<T> clasS) {

        Object result = Annotations.getAnnotationElementValue(annotationDesc, elementName);
        if (result == null) return null;

        if (clasS.isArray() && result instanceof Object[]) {
            Object[] oa = (Object[]) result;
            result = Array.newInstance(clasS.getComponentType(), oa.length);
            System.arraycopy(oa, 0, result, 0, oa.length);
        } else
        if (clasS == String.class && result instanceof Object[]) {
            Object[] oa = (Object[]) result;
            if (oa.length == 0) {
                result = "";
            } else
            if (oa.length == 1) {
                result = String.valueOf(oa[0]);
            } else
            {
                StringBuilder sb = new StringBuilder().append(oa[0]);
                for (int i = 1; i < oa.length; i++) {
                    sb.append(',').append(oa[i]);
                }
                result = sb.toString();
            }
        }

        assert clasS.isAssignableFrom(result.getClass());

        @SuppressWarnings("unchecked") T tmp = (T) result;
        return tmp;
    }

    /**
     * Determines and returns the value of the named element.
     * <p>
     *   This can be:
     * </p>
     * <ul>
     *   <li>A wrapper for a primitive type</li>
     *   <li>A string</li>
     *   <li>A {@link Type} (representing a class literal)</li>
     *   <li>A {@link FieldDoc} (representing an enum constant)</li>
     *   <li>An {@link AnnotationDesc} (representing what??)</li>
     *   <li>An array of any of these (including the case of nested arrays)</li>
     * </ul>
     */
    @Nullable public static Object
    getAnnotationElementValue(AnnotationDesc annotationDesc, String elementName) {

        for (ElementValuePair evp : annotationDesc.elementValues()) {

            if (evp.element().name().equals(elementName)) {

                return Annotations.getAnnotationValue(evp.value());
            }
        }

        return null;
    }

    /**
     * Determines and returns the value of an {@link AnnotationValue}.
     *
     * @return See {@link #getAnnotationElementValue(AnnotationDesc, String)}
     */
    @Nullable public static Object
    getAnnotationValue(AnnotationValue value) {

        Object o = value.value();

        if (o instanceof AnnotationValue[]) {
            AnnotationValue[] avs = (AnnotationValue[]) o;

            Object[] oa = new Object[avs.length];
            for (int i = 0; i < avs.length; i++) {
                oa[i] = Annotations.getAnnotationValue(avs[i]);
            }

            return oa;
        }

        return o;
    }
}
