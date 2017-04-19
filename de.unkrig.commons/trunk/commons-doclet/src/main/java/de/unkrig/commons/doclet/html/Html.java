
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

package de.unkrig.commons.doclet.html;

import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

import de.unkrig.commons.doclet.Docs;
import de.unkrig.commons.doclet.Tags;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Longjump;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Helper functionality in the context of doclets and HTML.
 */
public
class Html {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * When generating HTML from JAVADOC, this interface is used to generate links to JAVA elements.
     */
    public
    interface LinkMaker {

        /**
         * Generates a {@link Link} that refers from the HTML page on which <var>from</var> is described to the HTML
         * page (and possibly the anchor) that describes <var>to</var>.
         *
         * @throws Longjump A link href could be determined, but for some reason it was forbidden to link there
         */
        Link
        makeLink(Doc from, Doc to, RootDoc rootDoc) throws Longjump;
    }

    /**
     * Representation of a link in an HTML document.
     */
    public static final
    class Link {

        /**
         * {@code null} if the bare label should be displayed instead of a link
         */
        @Nullable public final String href;

        /**
         * The "default label" for the link; typically it not only depends on the link target, but also on the link
         * <em>source</em>.
         */
        public final String defaultLabelHtml;

        public
        Link(@Nullable String href, String defaultLabelHtml) {
            this.href             = href;
            this.defaultLabelHtml = defaultLabelHtml;
        }
    }

    /**
     * Implements the strategy of the standard JAVADOC doclet.
     * <p>
     *   Hrefs are generated as follows:
     * </p>
     * <dl>
     *   <dt>Field, constructor or method of same class:</dt>
     *   <dd>{@code "#toField"}</dd>
     *   <dd>{@code "#ToClass(java.lang.String)"}</dd>
     *   <dd>{@code "#toMethod(java.lang.String)"}</dd>
     *   <dt>Class, field, constructor or method in external package:</dt>
     *   <dd>{@code "http://external.url/to/package/ToClass"}</dd>
     *   <dd>{@code "http://external.url/to/package/ToClass#toField"}</dd>
     *   <dd>{@code "http://external.url/to/package/ToClass#ToClass(java.lang.String)"}</dd>
     *   <dd>{@code "http://external.url/to/package/ToClass#toMethod(java.lang.String)"}</dd>
     *   <dt>Class, field, constructor or method in same package:</dt>
     *   <dd>{@code "ToClass"}</dd>
     *   <dd>{@code "ToClass#toField"}</dd>
     *   <dd>{@code "ToClass#ToClass(String)"}</dd>
     *   <dd>{@code "ToClass#toMethod(String)"}</dd>
     *   <dt>Class, field, constructor or method in different (but "included") package:</dt>
     *   <dd>{@code "../../to/package/ToClass"}</dd>
     *   <dd>{@code "../../to/package/ToClass#toField"}</dd>
     *   <dd>{@code "../../to/package/ToClass#ToClass(String)"}</dd>
     *   <dd>{@code "../../to/package/ToClass#toMethod(String)"}</dd>
     *   <dt>Class, field or method in non-included package:</dt>
     *   <dd>{@code null}</dd>
     *   <dt>RootDoc:</dt>
     *   <dd>{@code "../../"}</dd>
     * </dl>
     * <p>
     *   Default labels are generated as follows:
     * </p>
     * <dl>
     *   <dt>Field, constructor or method of same class:</dt>
     *   <dd>{@code "toField"}</dd>
     *   <dd>{@code "ToClass(java.lang.String)"}</dd>
     *   <dd>{@code "toMethod(java.lang.String)"}</dd>
     *   <dt>Class, or field, constructor or method in different class:</dt>
     *   <dd>{@code ToClass}</dd>
     *   <dd>{@code ToClass.toField}</dd>
     *   <dd>{@code ToClass(java.lang.String)}</dd>
     *   <dd>{@code ToClass.toMethod(java.lang.String)}</dd>
     * </dl>
     */
    public static final LinkMaker
    STANDARD_LINK_MAKER = new LinkMaker() {

        @Override public Link
        makeLink(Doc from, Doc to, RootDoc rootDoc) {

            String href;

            if (to == from && !(to instanceof ClassDoc)) {
                href = null;
            } else
            if (!to.isIncluded() && !(to instanceof RootDoc)) {
                href = null;
            } else
            {
                StringBuilder sb = new StringBuilder();

                {
                    PackageDoc fromPackage = Docs.packageScope(from);
                    if (fromPackage != null) {

                        for (@SuppressWarnings("unused") String component : fromPackage.name().split("\\.")) {
                            sb.append("../");
                        }
                    }
                }

                PackageDoc toPackage = Docs.packageScope(to);
                if (toPackage != null) {

                    sb.append(toPackage.name().replace('.', '/')).append('/');

                    ClassDoc toClass = Docs.classScope(to);
                    if (toClass == null) {
                        sb.append("index.html");
                    } else {
                        sb.append(toClass.name()).append(".html");
                    }

                    sb.append(Html.fragmentIdentifier(to));
                }

                href = sb.toString();
            }

            String defaultLabelHtml;

            if (!(to instanceof MemberDoc)) {
                defaultLabelHtml = to.name();
            } else {

                MemberDoc toMember = (MemberDoc) to;

                defaultLabelHtml = (
                    toMember.containingClass() == from || (
                        from instanceof MemberDoc
                        && toMember.containingClass() == ((MemberDoc) from).containingClass()
                    ) || to.isEnumConstant()
                    ? ""
                    : toMember.containingClass().name() + '.'
                );

                if (to.isField() || to.isEnumConstant()) {
                    defaultLabelHtml += to.name();
            	} else
                if (to.isConstructor()) {
                    ConstructorDoc toConstructorDoc = (ConstructorDoc) to;
                    defaultLabelHtml += (
                        toConstructorDoc.containingClass().name()
                        + this.prettyPrintParameterList(toConstructorDoc)
                    );
                } else
                if (to.isMethod()) {
                    MethodDoc toMethodDoc = (MethodDoc) to;
                    defaultLabelHtml += to.name() + this.prettyPrintParameterList(toMethodDoc);
                } else
                {
                    throw new IllegalArgumentException(String.valueOf(to));
                }
            }

            return new Link(href, defaultLabelHtml);
        }

        private String
        prettyPrintParameterList(ExecutableMemberDoc executableMemberDoc) {

            StringBuilder result = new StringBuilder().append('(');
            for (int i = 0; i < executableMemberDoc.parameters().length; i++) {
                Parameter parameter = executableMemberDoc.parameters()[i];

                if (i > 0) {
                    result.append(", ");
                }

                Type pt = parameter.type();

                if (pt.isPrimitive()) {
                    result.append(pt.toString());
                    continue;
                }

                // Show erasure type, not type variable name.
                ClassDoc cd = pt.asClassDoc();
                assert cd != null : parameter;
                result.append(cd.name());
            }
            result.append(')');

            return result.toString();
        }
    };

    /**
     * Generates the "fragment" identifier for the given <var>doc</var>.
     * <dl>
     *   <dt>{@link FieldDoc}:</dt>
     *   <dd>{@code "#fieldName"}</dd>
     *   <dt>{@link MethodDoc}:</dt>
     *   <dd>{@code "#methodName(java.lang.String,int)"}</dd>
     *   <dt>Other:</dt>
     *   <dd>{@code ""}</dd>
     * </dl>
     */
    private static String
    fragmentIdentifier(Doc doc) {

        if (doc.isField()) return '#' + doc.name();

        if (doc.isConstructor()) {
            ConstructorDoc constructorDoc = (ConstructorDoc) doc;
            return (
                '#'
                + constructorDoc.containingClass().name()
                + Html.parameterListForFragmentIdentifier(constructorDoc)
            );
        }

        if (doc.isMethod()) {
            MethodDoc methodDoc = (MethodDoc) doc;
            return '#' + doc.name() + Html.parameterListForFragmentIdentifier(methodDoc);
        }

        return "";
    }

    private static String
    parameterListForFragmentIdentifier(ExecutableMemberDoc executableMemberDoc) {

        StringBuilder result = new StringBuilder().append('(');
        for (int i = 0; i < executableMemberDoc.parameters().length; i++) {
            Parameter parameter = executableMemberDoc.parameters()[i];

            if (i > 0) result.append(", ");

            result.append(parameter.type().qualifiedTypeName());
        }
        return result.append(')').toString();
    }

    /**
     * See <a href="http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDFIIJH">the
     * documentation of the '-linkoffline' option of the JAVADOC tool</a>.
     */
    public static final
    class ExternalJavadocsLinkMaker implements LinkMaker {

        private final Map<String /*packageName*/, URL /*target*/> externalJavadocs;
        private final LinkMaker                                   delegate;

        public
        ExternalJavadocsLinkMaker(Map<String /*packageName*/, URL /*target*/> externalJavadocs, LinkMaker delegate) {
            this.externalJavadocs = externalJavadocs;
            this.delegate         = delegate;
        }

        @Override public Link
        makeLink(Doc from, Doc to, RootDoc rootDoc) throws Longjump {

            PackageDoc toPackage = Docs.packageScope(to);
            assert toPackage != null;

            URL url = this.externalJavadocs.get(toPackage.name());
            if (url == null) return this.delegate.makeLink(from, to, rootDoc);

            ClassDoc toClass = Docs.classScope(to);

            String href;
            if (toClass == null) {
                href = url.toString() + '/' + toPackage.name().replace('.', '/') + "/index.html";
            } else {
                href = url + toClass.qualifiedName().replace('.', '/') + ".html" + Html.fragmentIdentifier(to);
            }

            String defaultLabelHtml = Html.STANDARD_LINK_MAKER.makeLink(from, to, rootDoc).defaultLabelHtml;

            return new Link(href, defaultLabelHtml);
        }
    }

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final LinkMaker linkMaker;

    public
    Html(LinkMaker linkMaker) { this.linkMaker = linkMaker; }

    /**
     * Expands inline tags to HTML. Inline tags, as of Java 8, are:
     * <pre>
     *   &#123;@code text}
     *   &#123;@docRoot}
     *   &#123;@inheritDoc}
     *   &#123;@link package.class#member label}
     *   &#123;@linkplain package.class#member label}
     *   &#123;@literal text}
     *   &#123;@value package.class#field}
     * </pre>
     * Only part of these are currently acceptable for the transformation into HTML.
     */
    public String
    fromTags(Tag[] tags, Doc ref, RootDoc rootDoc) throws Longjump {

        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            sb.append(this.expandTag(ref, rootDoc, tag));
        }

        return sb.toString();
    }

    /**
     * Expands a tag to HTML text. Supported tags are:
     * <dl>
     *   <dt>Text</dt>
     *   <dd>
     *     Expands to the literal text
     *   </dd>
     *   <dt><code>{&#64;code</code> <var>text</var><code>}</code></dt>
     *   <dd>
     *     Expands to the <var>text</var>, in monospace font and with HTML entities escaped
     *   </dd>
     *   <dt><code>{&#64;literal</code> <var>text</var><code>}</code></dt>
     *   <dd>
     *     Expands to the <var>text</var>, with HTML entities escaped
     *   </dd>
     *   <dt><code>{&#64;value</code> <var>field-ref</var><code>}</code></dt>
     *   <dd>
     *     Expands to the constant initializer value of the designated field
     *   </dd>
     *   <dt><code>{&#64;link</code> <var>ref</var> [ <var>text</var> ] <code>}</code></dt>
     *   <dd>
     *     Expands to a link (in monospace font) to the designated <var>ref</var>
     *   </dd>
     *   <dt><code>{&#64;linkplain</code> <var>ref</var> [ <var>text</var> ] <code>}</code></dt>
     *   <dd>
     *     Like <code>{&#64; link}</code>, but in default font
     *   </dd>
     * </dl>
     * <p>
     *   Subclasses may override this method to expand more than these tags.
     * </p>
     *
     * @param tag The tag to expand
     * @return    The text resulting from the expansion, typically an HTML link
     */
    protected String
    expandTag(Doc ref, RootDoc rootDoc, Tag tag) throws Longjump {

        String tagName  = tag.name();
        String argument = tag.text();

        if ("Text".equals(tagName)) {

            if (argument == null) return "";

            // Text tags contain UNIX line breaks ("\n").

            // DOC comments appear to be "String.trim()"med, i.e. leading and trailing spaces and line breaks are
            // removed:
            //
            //    /**
            //     *
            //     * foo   => "\n\n foo\n" => "foo"
            //     *
            //     */

            // From continuation lines, any leading " *\**" is removed:
            //
            //    /**
            //     * one
            //         ***** two
            //     */              => "one\n     ***** two" => "one\n two"

            // Notice that the standard JDK JAVADOC DOCLET treats continuation lines WITHOUT a leading blank as a
            // masked line break:
            //
            //    /**
            //     * one
            //     *two        => "one\n *two" => "onetwo"
            //     */

            for (int idx = argument.indexOf('\n'); idx != -1; idx = argument.indexOf('\n', idx)) {

                if (idx == argument.length() - 1) {

                    // This case should not occur, as, as described above, JAVADOC silently trims texts.
                    argument = argument.substring(0, idx) + Html.LINE_SEPARATOR;
                    break;
                }

                char c = argument.charAt(idx + 1);
                if (c == '\n') {

                    // "Short" line (" *").
                    argument = argument.substring(0, idx) + Html.LINE_SEPARATOR + argument.substring(idx + 1);
                    idx      += Html.LINE_SEPARATOR.length();
                } else
                if (c == ' ') {

                    // "Normal" continuation line (" * two").
                    argument = argument.substring(0, idx) + Html.LINE_SEPARATOR + argument.substring(idx + 2);
                    idx      += Html.LINE_SEPARATOR.length();
                } else
                {

                    // Masked line break (" *two").
                    argument = argument.substring(0, idx) + argument.substring(idx + 1);
                }
            }
            return argument;
        }

        if ("@code".equals(tagName)) {
            if (argument == null) {
                rootDoc.printError(ref.position(), "Argument missing for  '{@code ...}' tag");
                return "";
            }
            argument = Html.escapeSgmlEntities(argument);
            return "<code>" + argument  + "</code>";
        }

        if ("@literal".equals(tagName)) {
            if (argument == null) {
                rootDoc.printError(ref.position(), "Argument missing for  '{@literal ...}' tag");
                return "";
            }
            return Html.escapeSgmlEntities(argument);
        }

        if ("@value".equals(tagName)) {
            if (argument == null) {
                rootDoc.printError(ref.position(), "Argument missing for  '{@value ...}' tag");
                return "";
            }
            Doc doc = argument.length() == 0 ? ref : Docs.findDoc(ref, argument, rootDoc);
            if (doc == null) {
                rootDoc.printError(ref.position(), "Field '" + argument + "' not found");
                return argument;
            }
            if (!(doc instanceof FieldDoc)) {
                rootDoc.printError(doc.position(), "'" + argument + "' does not designate a field");
                return argument;
            }

            Object cv = ((FieldDoc) doc).constantValue();
            if (cv == null) {
                rootDoc.printError(
                    doc.position(),
                    "Field '" + argument + "' does not have a constant value"
                );
                return argument;
            }

            return this.makeLink(ref, doc, true, Html.escapeSgmlEntities(cv.toString()), null, rootDoc);
        }

        if ("@link".equals(tagName) || "@linkplain".equals(tagName)) {
            if (argument == null) {
                rootDoc.printError(ref.position(), "Argument missing for '{@link ...}' tag");
                return "";
            }

            Doc target = Html.targetOfSeeTag((SeeTag) tag);
            if (target == null) {
                rootDoc.printError(ref.position(), "Target \"" + tag.text() + "\" of {@link} tag not found");
                return "???";
            }

            return this.makeLink(ref, target, "@linkplain".equals(tagName), ((SeeTag) tag).label(), null, rootDoc);
        }

        if ("@docRoot".equals(tagName)) {
            String href = this.linkMaker.makeLink(ref, rootDoc, rootDoc).href;
            return href == null ? "" : href;
        }

        if ("@constantsof".equals(tagName) || "@constantsofplain".equals(tagName)) {
            if (argument == null) {
                rootDoc.printError(ref.position(), "Argument missing for  '{@constantsof ...}' tag");
                return "";
            }

            Doc doc = argument.length() == 0 ? ref : Docs.findDoc(ref, argument, rootDoc);
            if (doc == null) {
                rootDoc.printError(ref.position(), "Enum '" + argument + "' not found");
                return argument;
            }

            if (!(doc instanceof ClassDoc)) {
                rootDoc.printError(doc.position(), "'" + argument + "' does not designate an enum type");
                return argument;
            }
            ClassDoc cd = (ClassDoc) doc;

            if (!cd.isEnum()) {
                rootDoc.printError(
                    doc.position(),
                    "Type '" + argument + "' is not an enum"
                );
                return argument;
            }

            FieldDoc[] ecs = cd.enumConstants();
            if (ecs.length == 0) return "";

            StringBuilder sb = new StringBuilder();

            for (int i = 0;;) {
                FieldDoc ec = ecs[i];
                sb.append(this.makeLink(
                    ref,                                 // from
                    ec,                                  // to
                    "@constantsofplain".equals(tagName), // plain
                    ec.name(),                           // label
                    null,                                // target
                    rootDoc                              // rootDoc
                ));
                if (++i == ecs.length) break;
                sb.append(", ");
            }

            return sb.toString();
        }

        rootDoc.printError(ref.position(), (
            "Inline tag '{"
            + tagName
            + "}' is not supported; you could "
            + "(A) remove it from the text, or "
            + "(B) improve 'Html.expandTag()' to transform it into nice HTML (if that is "
            + "reasonably possible)"
        ));
        return "{" + tagName + (argument == null ? "" : " " + argument) + "}";
    }

    @Nullable public static Doc
    targetOfSeeTag(SeeTag seeTag) {
        Doc target = seeTag.referencedMember();
        if (target != null) return target;

        target = seeTag.referencedClass();
        if (target != null) return target;

        target = seeTag.referencedPackage();
        if (target != null) return target;

        return null;
    }

    /**
     * @param href A link like "{@code ../../pkg/MyClass#myMethod(java.lang.String)}"
     * @param from The {@link ClassDoc} to which this link is relative to
     * @return     The package, class, field or method that the <var>href</var> designates
     */
    public static Doc
    hrefToDoc(String href, RootDoc rootDoc, ClassDoc from) throws Longjump {

        String prefix = href.startsWith("#") ? from.qualifiedName() : from.containingPackage().name() + '.';

        while (href.startsWith("../")) {
            prefix = prefix.substring(0, prefix.lastIndexOf('.', prefix.length() - 2) + 1);
            href   = href.substring(3);
        }

        Doc result = Docs.findDoc(rootDoc, prefix + href.replace('/', '.'), rootDoc);
        if (result == null) {

            // It is a link to an "external javadoc", so leave it as is.
            throw new Longjump();
        }

        return result;
    }

    /**
     * @param plain  Whether this is a "{@code @plainlink}"
     * @param target The value of the (optional) 'target="..."' attribute of the HTML anchor
     * @return       An HTML snippet like "{@code <a href="#equals(java.lang.Object)">THE-LABEL</a>}"
     */
    public String
    makeLink(
        Doc              from,
        Doc              to,
        boolean          plain,
        @Nullable String label,
        @Nullable String target,
        RootDoc          rootDoc
    ) throws Longjump {

        Link link = this.linkMaker.makeLink(from, to, rootDoc);

        if (label == null || label.isEmpty()) label = link.defaultLabelHtml;
        if (!plain) label = "<code>" + label + "</code>";

        if (link.href == null) return label;

        return (
            "<a href=\""
            + link.href
            + "\""
            + (
                to.isOrdinaryClass() ? " title=\"class in "     + ((ClassDoc) to).containingPackage().name() + "\"" :
                to.isInterface()     ? " title=\"interface in " + ((ClassDoc) to).containingPackage().name() + "\"" :
                ""
            )
            + (target == null ? "" : " target=\"" + target + "\"")
            + ">"
            + label
            + "</a>"
        );
    }

    /**
     * Replaces "{@code <}", "{@code >}" and "{@code &}".
     */
    public static String
    escapeSgmlEntities(String text) {
        text = Html.AMPERSAND.matcher(text).replaceAll("&amp;");
        text = Html.LESS_THAN.matcher(text).replaceAll("&lt;");
        text = Html.GREATER_THAN.matcher(text).replaceAll("&gt;");
        return text;
    }
    private static final Pattern AMPERSAND    = Pattern.compile("&");
    private static final Pattern LESS_THAN    = Pattern.compile("<");
    private static final Pattern GREATER_THAN = Pattern.compile(">");

    /**
     * Verifies that the named block tag exists at most <b>once</b>, replaces line breaks with spaces, and convert
     * its text to HTML.
     *
     * @return {@code null} iff the tag does not exist
     */
    @Nullable public String
    optionalTag(Doc doc, String tagName, RootDoc rootDoc) throws Longjump {

        Tag t = Tags.optionalTag(doc, tagName, rootDoc);
        if (t == null) return null;

        return this.fromTags(t.inlineTags(), doc, rootDoc);
    }

    /**
     * Generates HTML markup for the given <var>doc</var> in the context of <var>ref</var>.
     */
    public String
    generateFor(Doc doc, RootDoc rootDoc) throws Longjump {

        String htmlText = "";

        // The "@deprecated" sections come BEFORE the main description.
        for (Tag dt : doc.tags("@deprecated")) {
            htmlText += (
                "<div class=\"block\"><span class=\"strong\">Deprecated.</span>&nbsp;<i>"
                + this.fromTags(dt.inlineTags(), doc, rootDoc)
                + "</i></div>"
            );
        }

        // Generate HTML text from the doc's inline tags.
        htmlText += this.fromTags(doc.inlineTags(), doc, rootDoc);

        // Append the "See also" list.
        Tag[] seeTags = doc.tags("@see");
        if (seeTags.length == 0) return htmlText;

        StringBuilder sb = new StringBuilder(htmlText).append("<dl><dt>See also:</dt>");
        for (Tag t : seeTags) {
            SeeTag seeTag = (SeeTag) t;

            try {
                sb.append("<dd>");

                Doc target = Html.targetOfSeeTag(seeTag);
                if (target == null) {

                    // Form 1: @see "The Java Programming Language"
                    // Form 2: @see <a href="URL#value">label</a>
                    sb.append(this.fromTags(seeTag.inlineTags(), doc, rootDoc));
                } else {

                    // Form 3: @see package.class#member label
                    String label = seeTag.label();
                    if (label.length() == 0) label = null;
                    sb.append(this.makeLink(doc, target, false, label, null, rootDoc));
                }

                sb.append("</dd>");
            } catch (Longjump e) {}
        }
        sb.append("</dl>");

        return sb.toString();
    }

    /**
     * @return The "description" text for a javadoc-ish summary table; composed from the first sentence of the doc
     *         comment and its (optional) "{@code @deprecated}" tags
     */
    public String
    summaryDescription(Doc doc, RootDoc rootDoc) {

        Tag[] dts = doc.tags("@deprecated");

        try {
            if (dts.length == 0) {
                return this.fromTags(doc.firstSentenceTags(), doc, rootDoc);
            } else {
                return (
                    "<div class=\"block\"><strong>Deprecated.</strong><div class=\"block\"><i>"
                    + this.fromTags(dts[0].inlineTags(), doc, rootDoc)
                    + "</i></div></div>"
                );
            }
        } catch (Longjump e) {
            return "???";
        }
    }
}
