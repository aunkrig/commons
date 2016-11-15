
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Longjump;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to doclet tags.
 */
public final
class Docs {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Docs() {}

    /**
     * @param from  The 'reference' for {@code s}, e.g. the {@link MethodDoc} if this is a method doc comment
     * @param to    E.g. "pkg.MyClass" or "MyClass#meth" or "MyClass#meth(String)"
     * @return      The {@link Doc} specified by {@code s}, relative to {@code ref}, or {@code null} iff a doc cannot
     *              be found
     */
    @Nullable public static Doc
    findDoc(Doc from, String to, RootDoc rootDoc) throws Longjump {

        String where, what;
        {
            int hashPos = to.indexOf('#');
            if (hashPos == -1) {
                where = to;
                what  = null;
            } else
            if (hashPos == 0) {
                where = null;
                what  = to.substring(1);
            } else
            {
                where = to.substring(0, hashPos);
                what  = to.substring(hashPos + 1);
            }
        }

        ClassDoc classScope;
        if (from instanceof MemberDoc) {
            classScope = ((MemberDoc) from).containingClass();
        } else
        if (from instanceof ClassDoc) {
            classScope = (ClassDoc) from;
        } else
        {
            classScope = null;
        }

        ClassDoc referencedClass = null;

        // Current class?
        if (where == null) {
            if (classScope == null) {
                rootDoc.printError(from.position(), "\"" + to + "\": No type declaration in scope");
                throw new Longjump();
            }
            referencedClass = classScope;
        } else
        if (classScope != null) {

            // Find the specified class or interface within the context of this class doc. Search order:
            // 1) qualified name,
            // 2) nested in this class or interface,
            // 3) in this package,
            // 4) in the class imports,
            // 5) in the package imports
            // See http://docs.oracle.com/javase/7/docs/jdk/api/javadoc/doclet/com/sun/javadoc/ClassDoc.html#findClass%28java.lang.String%29 // SUPPRESS CHECKSTYLE LineLength
            referencedClass = classScope.findClass(where);

            // "ImportedClass.NestedClass" is NOT covered by the previous call.
            int firstDot = where.indexOf('.');
            if (referencedClass == null && firstDot != -1) {
                ClassDoc c = classScope.findClass(where.substring(0, firstDot));
                if (c != null) referencedClass = c.findClass(where.substring(firstDot + 1));
            }
        } else
        if (from instanceof PackageDoc) {

            // It is not clearly documented, but (hopefully) this method searches the following places:
            // 1) qualified name,
            // 2) in this package,
            // 3) in the class imports,
            // 4) in the package imports
            // See http://docs.oracle.com/javase/7/docs/jdk/api/javadoc/doclet/com/sun/javadoc/PackageDoc.html#findClass(java.lang.String) // SUPPRESS CHECKSTYLE LineLength
            referencedClass = ((PackageDoc) from).findClass(where);
        } else
        {

            // See http://docs.oracle.com/javase/7/docs/jdk/api/javadoc/doclet/com/sun/javadoc/RootDoc.html#classNamed(java.lang.String) // SUPPRESS CHECKSTYLE LineLength
            referencedClass = rootDoc.classNamed(where);
        }

        // Type in same package?
        if (referencedClass == null && classScope != null) {
            referencedClass = rootDoc.classNamed(classScope.containingPackage().name() + '.' + where);
        }

        // Package?
        if (referencedClass == null) {
            PackageDoc referencedPackage = rootDoc.packageNamed(where);
            if (referencedPackage != null) {
                if (what != null) {
                    rootDoc.printError(from.position(), "Cannot use '#' on package");
                }
            }
        }

        if (referencedClass == null) {
            return null;
        }

        where = referencedClass.qualifiedName();

        if (what == null) return referencedClass;

        {

            // Parse method name and (optional) parameter types.
            int          op = what.indexOf('(');
            String       methodName;
            List<Object> parameterTypes; // Contains "String"s and "ClassDoc"s.
            if (op == -1) {
                methodName     = what;
                parameterTypes = null;
            } else {
                methodName = what.substring(0, op);
                if (what.charAt(what.length() - 1) != ')') what += ')';
                parameterTypes = new ArrayList<Object>();
                String parameterTypeSequence = what.substring(op + 1, what.length() - 1);
                if (parameterTypeSequence.length() > 0) {
                    for (String ptn : parameterTypeSequence.split("\\s*,\\s*")) {
                        ClassDoc parameterTypeDoc = (ClassDoc) Docs.findDoc(from, ptn, rootDoc);
                        parameterTypes.add(parameterTypeDoc == null ? ptn : parameterTypeDoc);
                    }
                }
            }

            if (methodName.equals(referencedClass.simpleTypeName())) {
                for (ConstructorDoc cd : referencedClass.constructors(false)) {

                    // Check constructor parameter types.
                    if (Docs.equalParameters(parameterTypes, cd.parameters())) return cd;
                }
            } else {
                for (MethodDoc md : Docs.allMethods(referencedClass)) {

                    // Check method name.
                    if (!methodName.equals(md.name())) continue;

                    // Check method parameter types.
                    if (Docs.equalParameters(parameterTypes, md.parameters())) return md;
                }
            }
        }

        for (FieldDoc fd : Docs.allFields(referencedClass)) {
            if (what.equals(fd.name())) return fd;
        }

        for (FieldDoc fd : referencedClass.enumConstants()) {
            if (what.equals(fd.name())) return fd;
        }

        rootDoc.printError(from.position(), "Cannot find '" + what + "' in '" + where + "'");
        throw new Longjump();
    }

    /** @return All fields declared by the <var>clasS</var> and its superclasses */
    private static FieldDoc[]
    allFields(ClassDoc clasS) {
        List<FieldDoc> result = new ArrayList<FieldDoc>();
        for (ClassDoc cd : Docs.withSuperclasses(clasS)) {
            result.addAll(Arrays.asList(cd.fields()));
        }
        return result.toArray(new FieldDoc[result.size()]);
    }

    /**
     * @return The <var>clasS</var>, and all superclasses (including {@link Object}) it extends
     */
    public static ClassDoc[]
    withSuperclasses(ClassDoc clasS) {

        List<ClassDoc> result = new ArrayList<ClassDoc>();
        for (ClassDoc c = clasS; c != null; c = c.superclass()) {
            result.add(c);
        }
        return result.toArray(new ClassDoc[result.size()]);
    }

    /** @return All methods declared by the <var>clasS</var>, its superclasses and all the interfaces it implements */
    private static MethodDoc[]
    allMethods(ClassDoc clasS) {

        List<MethodDoc> result = new ArrayList<MethodDoc>();
        for (ClassDoc cd : Docs.withSuperclassesAndInterfaces(clasS)) {
            result.addAll(Arrays.asList(cd.methods()));
        }
        return result.toArray(new MethodDoc[result.size()]);
    }

    /**
     * @return The <var>clasS</var>, all superclasses (including {@link Object}) it extends, and all interfaces that it
     *         and its superclasses implement
     */
    public static ClassDoc[]
    withSuperclassesAndInterfaces(ClassDoc clasS) {
        List<ClassDoc> result = new ArrayList<ClassDoc>();
        Docs.addClassSuperclassesAndInterfaces(clasS, result);
        return result.toArray(new ClassDoc[result.size()]);
    }

    private static void
    addClassSuperclassesAndInterfaces(ClassDoc clasS, List<ClassDoc> result) {
        result.add(clasS);
        ClassDoc superclass = clasS.superclass();
        if (superclass != null) {
            Docs.addClassSuperclassesAndInterfaces(superclass, result);
        }
        for (ClassDoc interfacE : clasS.interfaces()) {
            Docs.addInterfaceAndExtendedInterfaces(interfacE, result);
        }
    }

    private static void
    addInterfaceAndExtendedInterfaces(ClassDoc interfacE, List<ClassDoc> result) {
        result.add(interfacE);
        for (ClassDoc extendedInterface : interfacE.interfaces()) {
            if (!result.contains(extendedInterface)) {
                result.add(extendedInterface);
                Docs.addInterfaceAndExtendedInterfaces(extendedInterface, result);
            }
        }
    }

    private static boolean
    equalParameters(@Nullable List<Object> expected, Parameter[] actual) {

        if (expected == null) return true;


        if (expected.size() != actual.length) return false;

        for (int i = 0; i < expected.size(); i++) {
            Object   parameterType       = expected.get(i);
            Type     mdParameterType     = actual[i].type();
            ClassDoc mdParameterClassDoc = mdParameterType.asClassDoc();
            if (mdParameterClassDoc == null) {
                if (!mdParameterType.toString().equals(parameterType)) return false;
            } else {
                if (!mdParameterClassDoc.equals(parameterType)) return false;
            }
        }
        return true;
    }

    /**
     * @return The {@link ClassDoc} containing the given {@code doc}, or {@code null} iff {@code doc} is a
     *         {@link PackageDoc} or a {@link RootDoc}
     */
    @Nullable public static ClassDoc
    classScope(Doc doc) {

        return (
            doc instanceof ClassDoc ? (ClassDoc) doc :
            doc instanceof MemberDoc ? ((MemberDoc) doc).containingClass() :
            null
        );
    }

    /**
     * @return The {@link PackageDoc} containing the given {@code doc}, or {@code null} iff {@code doc} is a
     *         {@link RootDoc}
     */
    @Nullable public static PackageDoc
    packageScope(Doc doc) {

        return (
            doc instanceof PackageDoc ? (PackageDoc) doc :
            doc instanceof ProgramElementDoc ? ((ProgramElementDoc) doc).containingPackage() :
            null
        );
    }

    /**
     * An enhanced version of {@link ClassDoc#methods(boolean)} which optionally also includes <i>inherited</i>
     * methods.
     *
     * @param filter           Specify {@code true} to filter according to the specified access modifier option; see
     *                         {@link ClassDoc#methods(boolean)}
     * @param includeInherited Whether to add the superclasses' and the implemented interfaces' methods after the
     *                         class's methods
     */
    public static MethodDoc[]
    methods(final ClassDoc classDoc, boolean filter, boolean includeInherited) {

        if (!includeInherited) return classDoc.methods(filter);

        List<MethodDoc> result = new ArrayList<MethodDoc>();

        result.addAll(Arrays.asList(classDoc.methods(filter)));

        ClassDoc superclassDoc = classDoc.superclass();
        if (
            superclassDoc != null
            && !"org.apache.tools.ant.ProjectComponent".equals(superclassDoc.qualifiedName())
            && !"org.apache.tools.ant.Task".equals(superclassDoc.qualifiedName())
        ) result.addAll(Arrays.asList(Docs.methods(superclassDoc, filter, true)));

        for (ClassDoc interfaceDoc : classDoc.interfaces()) {
            result.addAll(Arrays.asList(Docs.methods(interfaceDoc, filter, true)));
        }

        return result.toArray(new MethodDoc[result.size()]);
    }

    /**
     * A drop-in replacement for {@link RootDoc#classNamed(String)}, but instead of returning {@code null} it prints an
     * error and throws a {@link Longjump}.
     */
    public static ClassDoc
    classNamed(RootDoc rootDoc, String className) throws Longjump {

        ClassDoc result = rootDoc.classNamed(className);

        if (result == null) {
            rootDoc.printError("\"" + className + "\" missing on classpath");
            throw new Longjump();
        }

        return result;
    }

    /**
     * @return Whether {@code b} is {@code a}, or an interface or superclass of {@code a}
     */
    public static boolean
    isSubclassOf(ClassDoc a, ClassDoc b) {

        if (a == b) return true;

        for (ClassDoc i : a.interfaces()) {
            if (Docs.isSubclassOf(i, b)) return true;
        }

        ClassDoc s = a.superclass();
        return s != null && Docs.isSubclassOf(s, b);
    }

    /**
     * Reads package names from "<var>packageListUrl</var>/package-list" and puts them into the
     * <var>externalJavadocs</var> map.
     *
     * @param targetUrl      Designates the root folder of the external API documentation; must end in "/"
     * @param packageListUrl Designates the folder where the "{@code package-list}" file exists; must end in "/"
     */
    public static void
    readExternalJavadocs(
        URL              targetUrl,
        URL              packageListUrl,
        Map<String, URL> externalJavadocs,
        RootDoc          rootDoc
    ) throws IOException {

        assert targetUrl.toString().endsWith("/") : targetUrl;
        assert packageListUrl.getPath().endsWith("/") : packageListUrl;

        List<String> packageNames = LineUtil.readAllLines(
            new InputStreamReader(new URL(packageListUrl, "package-list").openStream()),
            true                                                                         // closeReader
        );

        for (String packageName : packageNames) {
            URL prev = externalJavadocs.put(packageName, targetUrl);
            if (prev != null && !prev.equals(targetUrl)) {
                rootDoc.printError((
                    "Inconsistent links: Package \""
                    + packageName
                    + "\" was first linked to \""
                    + prev
                    + "\", now to \""
                    + targetUrl
                    + "\""
                ));
            }
        }
    }

    /**
     * Compares two {@link Type}s.
     */
    public static final Comparator<Type>
    TYPE_COMPARATOR = new Comparator<Type>() {

        @Override public int
        compare(@Nullable Type t1, @Nullable Type t2) {
            assert t1 != null;
            assert t2 != null;
            return t1.toString().compareTo(t2.toString());
        }
    };

    /**
     * Compares {@link Doc}s by their name.
     */
    public static final Comparator<Doc>
    DOCS_BY_NAME_COMPARATOR = new Comparator<Doc>() {

        @Override public int
        compare(@Nullable Doc d1, @Nullable Doc d2) {
            if (d1 == null) return d2 == null ? 0 : 1;
            if (d2 == null) return -1;
            return d1.name().compareToIgnoreCase(d2.name());
        }
    };
}
