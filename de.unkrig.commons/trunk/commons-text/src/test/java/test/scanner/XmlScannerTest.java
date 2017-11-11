
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test.scanner;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StringScanner;
import de.unkrig.commons.text.scanner.XmlScanner;
import de.unkrig.commons.text.scanner.XmlScanner.TokenType;

public
class XmlScannerTest {

    @Test public void
    testSimple() throws ScanException {
        XmlScannerTest.assertTokens(
            (
                ""
                + "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE html>\n"
                + "<?xml-stylesheet href=\"mystyle.css\" type=\"text/css\"?>\n"
                + "<!-- For testing only! -->\n"
                + "<outer a='a' b=\"b\">\n"
                + "  <inner1 />\n"
                + "  <inner2>bla</inner2>\n"
                + "  <![CDATA[FOO]]BAR]]>]]>\n"
                + "</outer>\n"
            ),
            TokenType.XML_DECLARATION,           "<?xml version=\"1.0\"?>", "\"1.0\"", null, null, // SUPPRESS CHECKSTYLE Wrap|LineLength:30
            TokenType.CHAR_DATA,                 "\n",
            TokenType.DOCUMENT_TYPE_DECLARATION, "<!DOCTYPE html>",
            TokenType.CHAR_DATA,                 "\n",
            TokenType.PROCESSING_INSTRUCTION,    "<?xml-stylesheet href=\"mystyle.css\" type=\"text/css\"?>", "xml-stylesheet", " href=\"mystyle.css\" type=\"text/css\"",
            TokenType.CHAR_DATA,                 "\n",
            TokenType.COMMENT,                   "<!-- For testing only! -->", " For testing only! ",
            TokenType.CHAR_DATA,                 "\n",
            TokenType.BEGIN_TAG,                 "<outer", "outer",
            TokenType.ATTRIBUTE_NAME,            " a", "a",
            TokenType.ATTRIBUTE_VALUE,           "='a'", "'a'",
            TokenType.ATTRIBUTE_NAME,            " b", "b",
            TokenType.ATTRIBUTE_VALUE,           "=\"b\"", "\"b\"",
            TokenType.END_START_TAG,             ">",
            TokenType.CHAR_DATA,                 "\n  ",
            TokenType.BEGIN_TAG,                 "<inner1", "inner1",
            TokenType.END_EMPTY_ELEMENT_TAG,     " />",
            TokenType.CHAR_DATA,                 "\n  ",
            TokenType.BEGIN_TAG,                 "<inner2", "inner2",
            TokenType.END_START_TAG,             ">",
            TokenType.CHAR_DATA,                 "bla",
            TokenType.END_TAG,                   "</inner2>", "inner2",
            TokenType.CHAR_DATA,                 "\n  ",
            TokenType.CDATA_SECTION,             "<![CDATA[FOO]]BAR]]>", "FOO]]BAR",
            TokenType.CHAR_DATA,                 "]]>\n",
            TokenType.END_TAG,                   "</outer>", "outer",
            TokenType.CHAR_DATA,                 "\n"
        );
    }

    private static void
    assertTokens(String input, Object... tokenTypesAndTokenTexts) throws ScanException {
        StringScanner<TokenType> ss = XmlScanner.stringScanner();
        ss.setInput(input);

        for (int i = 0;;) {

            if (i == tokenTypesAndTokenTexts.length) {
                Token<TokenType> t = ss.produce();
                if (t != null) Assert.fail("Extraneous " + t.type + " \"" + t.text + "\"");
                return;
            }

            TokenType type = (TokenType) tokenTypesAndTokenTexts[i++];
            String    text = (String)    tokenTypesAndTokenTexts[i++];

            Token<TokenType> t = ss.produce();
            if (t == null) {
                Assert.fail("Unexpected EOI; expected " + type + " \"" + text + "\"");
                return; // SNO
            }

            for (int j = 0; j < t.captured.length; j++) {

                if (i >= tokenTypesAndTokenTexts.length || tokenTypesAndTokenTexts[i] instanceof TokenType) {
                    Assert.fail((
                        "Capturing group count mismatch at index "
                        + i
                        + "; expected "
                        + t.captured.length
                        + ", actually "
                        + j
                    ));
                }

                String actual   = t.captured[j];
                String expected = (String) tokenTypesAndTokenTexts[i++];

                if (!ObjectUtil.equals(expected, actual)) {
                    Assert.fail((
                        "Capturing group mismatch: Expected \""
                        + expected
                        + "\", actually \""
                        + actual
                        + "\""
                    ));
                }
            }

            if (t.type != type) Assert.fail("Expected " + type + " instead of " + t.type);
            if (!t.text.equals(text)) Assert.fail("Expected \"" + text + "\" instead of \"" + t.text + "\"");
        }
    }
}
