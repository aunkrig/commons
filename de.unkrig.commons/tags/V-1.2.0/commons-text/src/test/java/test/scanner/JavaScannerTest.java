
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

package test.scanner;

import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.CXX_COMMENT;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.C_COMMENT;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.FLOATING_POINT_LITERAL;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.IDENTIFIER;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.INTEGER_LITERAL;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.KEYWORD;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.MULTI_LINE_C_COMMENT_BEGINNING;
import static de.unkrig.commons.text.scanner.JavaScanner.TokenType.SPACE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.DocumentScanner;
import de.unkrig.commons.text.scanner.JavaScanner;
import de.unkrig.commons.text.scanner.JavaScanner.TokenType;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.ScannerUtil;
import de.unkrig.commons.text.scanner.StringScanner;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class JavaScannerTest {

    @Test public void
    testLiterals() throws ScanException {
        JavaScannerTest.assertOneToken("\t  \n\r",                               SPACE);
        JavaScannerTest.assertOneToken("// sdkfhhk ffdf",                        CXX_COMMENT);
        JavaScannerTest.assertOneToken("/** * ***/",                             C_COMMENT);
        JavaScannerTest.assertOneToken("/*** ** **",                             MULTI_LINE_C_COMMENT_BEGINNING);
        JavaScannerTest.assertOneToken("catch",                                  KEYWORD);
        JavaScannerTest.assertOneToken("$ddd_$",                                 IDENTIFIER);
        JavaScannerTest.assertOneToken("9.",                                     FLOATING_POINT_LITERAL);
        JavaScannerTest.assertOneToken(".9",                                     FLOATING_POINT_LITERAL);
        JavaScannerTest.assertOneToken("9e1",                                    FLOATING_POINT_LITERAL);
        JavaScannerTest.assertOneToken("9f",                                     FLOATING_POINT_LITERAL);
        JavaScannerTest.assertOneToken("0",                                      INTEGER_LITERAL);
        JavaScannerTest.assertOneToken("0x77889872349829834798237498273498798l", INTEGER_LITERAL);
        JavaScannerTest.assertOneToken("77889872349829834798237498273498798l",   INTEGER_LITERAL);
        JavaScannerTest.assertOneToken("033L",                                   INTEGER_LITERAL);
    }

    private static void
    assertOneToken(String text, TokenType tokenType) throws ScanException {
        StringScanner<TokenType> ss = JavaScanner.rawStringScanner();
        ss.setInput(text);
        Token<TokenType> t = ss.produce();
        Assert.assertNotNull(t);
        assert t != null;
        Assert.assertSame(tokenType, t.type);
        Assert.assertEquals(text, t.text);
        Assert.assertNull(ss.produce());
    }

    @Test public void
    testScanUnscan() throws ScanException, IOException {

        // This file contains unicode escapes both in character literals and in string literals.
        File f = new File(
            "../commons-util/src/main/java/de/unkrig/commons/util/logging/handler/ArchivingFileHandler.java"
        );

        String raw;
        {
            Reader r = new InputStreamReader(new FileInputStream(f), Charset.forName("Cp1252"));

            r = JavaScanner.unicodeEscapesDecodingReader(r);

            raw = IoUtil.readAll(r, true);
        }

        String scannedUnscanned;
        {

            Reader r = new InputStreamReader(new FileInputStream(f), Charset.forName("Cp1252"));
            try {
                StringWriter               sw = new StringWriter();
                r = JavaScanner.unicodeEscapesDecodingReader(r);
                DocumentScanner<TokenType> js = ScannerUtil.toDocumentScanner(JavaScanner.rawStringScanner(), r);
                for (;;) {
                    Token<TokenType> jt = js.produce();
                    if (jt == null) break;
                    sw.write(jt.text);
                }
                scannedUnscanned = sw.toString();
            } finally {
                try { r.close(); } catch (Exception e) {}
            }
        }

        Assert.assertEquals(raw, scannedUnscanned);
    }

    @Test public void
    testRescan() throws ScanException, FileNotFoundException {
        FileReader reader = new FileReader("../commons-util/src/main/java/de/unkrig/commons/util/TimeTable.java");
        String     unscanned1;
        try {
            unscanned1 = JavaScannerTest.scanAndUnscan(reader);
        } finally {
            try { reader.close(); } catch (Exception e) {}
        }
        Assert.assertTrue(unscanned1.length() > 10000);

        String unscanned2 = JavaScannerTest.scanAndUnscan(new StringReader(unscanned1));
        Assert.assertEquals(unscanned1, unscanned2);
    }

    private static String
    scanAndUnscan(Reader reader) throws ScanException {
        StringWriter               sw = new StringWriter();
        DocumentScanner<TokenType> js = ScannerUtil.toDocumentScanner(JavaScanner.stringScanner(), reader);
        for (;;) {
            Token<TokenType> jt = js.produce();
            if (jt == null) break;
            sw.write(jt.text);
            sw.write(' ');
        }
        return sw.toString();
    }

    private static final String DOC1 = (
        ""
        + "\r"
        + "  // FOO \n"
        + "\r\n"
        + "/*BAR   */\r"
        + "  /*FOO\n"
        + "BAR*/\n"
        + "//THE END   "
    );

    @SuppressWarnings("null")
    @Test public void
    testComments() throws ScanException {

        DocumentScanner<TokenType> js = ScannerUtil.toDocumentScanner(
            JavaScanner.rawStringScanner(),
            new StringReader(JavaScannerTest.DOC1)
        );
        Assert.assertEquals("\r",           js.produce().toString());
        Assert.assertEquals("  ",           js.produce().toString());
        Assert.assertEquals("// FOO \n",    js.produce().toString());
        Assert.assertEquals("\r\n",         js.produce().toString());
        Assert.assertEquals("/*BAR   */",   js.produce().toString());
        Assert.assertEquals("\r",           js.produce().toString());
        Assert.assertEquals("  ",           js.produce().toString());
        Assert.assertEquals("/*FOO\n",      js.produce().toString());
        Assert.assertEquals("BAR*/",        js.produce().toString());
        Assert.assertEquals("\n",           js.produce().toString());
        Assert.assertEquals("//THE END   ", js.produce().toString());
        Assert.assertNull(js.produce());
    }

    @SuppressWarnings("null")
    @Test public void
    testCombineCompress() throws ScanException {

        ProducerWhichThrows<Token<TokenType>, ScanException> js = ScannerUtil.toDocumentScanner(
            JavaScanner.rawStringScanner(),
            new StringReader(JavaScannerTest.DOC1)
        );
        js = JavaScanner.combineMultiLineCComments(js);
        js = JavaScanner.compressSpaces(js);
        Assert.assertEquals("\r  ",         js.produce().toString());
        Assert.assertEquals("// FOO \n",    js.produce().toString());
        Assert.assertEquals("\r\n",         js.produce().toString());
        Assert.assertEquals("/*BAR   */",   js.produce().toString());
        Assert.assertEquals("\r  ",         js.produce().toString());
        Assert.assertEquals("/*FOO\nBAR*/", js.produce().toString());
        Assert.assertEquals("\n",           js.produce().toString());
        Assert.assertEquals("//THE END   ", js.produce().toString());
        Assert.assertNull(js.produce());
    }
}
