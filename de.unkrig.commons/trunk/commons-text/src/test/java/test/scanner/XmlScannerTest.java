
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

package test.scanner;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StringScanner;
import de.unkrig.commons.text.scanner.XmlScanner;
import de.unkrig.commons.text.scanner.XmlScanner.TokenType;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class XmlScannerTest {

    @Test public void
    testSimple() throws ScanException {
        XmlScannerTest.assertTokens(
            (
                ""
                + "<?xml version=\"1.0\"?>\n"
                + "<outer a='a' b=\"b\">\n"
                + "  <inner1 />\n"
                + "  <inner2>bla</inner2>\n"
                + "</outer>\n"
            ),
            TokenType.XML_DECLARATION,   "<?xml version=\"1.0\"?>",
            TokenType.CHAR_DATA,         "\n",
            TokenType.START_TAG,         "<outer a='a' b=\"b\">",
            TokenType.CHAR_DATA,         "\n  ",
            TokenType.EMPTY_ELEMENT_TAG, "<inner1 />",
            TokenType.CHAR_DATA,         "\n  ",
            TokenType.START_TAG,         "<inner2>",
            TokenType.CHAR_DATA,         "bla",
            TokenType.END_TAG,           "</inner2>",
            TokenType.CHAR_DATA,         "\n",
            TokenType.END_TAG,           "</outer>",
            TokenType.CHAR_DATA,         "\n"
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
            String    text = (String) tokenTypesAndTokenTexts[i++];

            Token<TokenType> t = ss.produce();
            if (t == null) {
                Assert.fail("Unexpected EOI; expected " + type + " \"" + text + "\"");
                return; // SNO
            }

            if (t.type != type) Assert.fail("Expected " + type + " instead of " + t.type);
            if (!t.text.equals(text)) Assert.fail("Expected \"" + text + "\" instead of \"" + t.text + "\"");
        }
    }
}
