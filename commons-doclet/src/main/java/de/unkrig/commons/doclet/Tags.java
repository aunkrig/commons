
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

import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Tag;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility methods related to doclet tags.
 */
public final
class Tags {

    private Tags() {}

    /**
     * Verifies that the named block tag exists at most <b>once</b>, and returns it.
     *
     * @return {@code null} iff the tag does not exist
     */
    @Nullable public static Tag
    optionalTag(Doc doc, String tagName, DocErrorReporter docErrorReporter)  {

        Tag[] tags = doc.tags(tagName);

        if (tags.length == 0) return null;

        if (tags.length > 1) {
            docErrorReporter.printError(doc.position(), "'" + tagName + "' must appear at most once");
        }

        return tags[0];
    }

    /**
     * Verifies that the named block tag exists exactly <b>once</b>, and returns it.
     */
    public static Tag
    requiredTag(Doc doc, String tagName, DocErrorReporter docErrorReporter)  {

        Tag[] tags = doc.tags(tagName);

        if (tags.length == 0) {
            docErrorReporter.printError(doc.position(), "'" + tagName + "' is missing");
        }

        if (tags.length > 1) {
            docErrorReporter.printError(doc.position(), "'" + tagName + "' must appear only once");
        }

        return tags[0];
    }
}