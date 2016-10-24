
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

package test;

import static org.junit.Assert.*;

import org.junit.Test;

import de.unkrig.commons.lang.PrettyPrinter;

// CHECKSTYLE JavadocMethod:OFF
// CHECKSTYLE JavadocType:OFF

public
class PrettyPrinterTest {

    private static final String S10  = "0123456789";
    private static final String S100 = (
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
    );

    @Test public void
    testToString() {
        assertEquals("null", PrettyPrinter.toString(null));
        assertEquals("7", PrettyPrinter.toString(7));
        assertEquals("\"HELLO\\n\"", PrettyPrinter.toString("HELLO\n"));
    }

    @Test public void
    testIntArray0ToString() {
        assertEquals("int[0]", PrettyPrinter.toString(new int[0]));
    }

    @Test public void
    testStringArray0ToString() {
        assertEquals("String[0]", PrettyPrinter.toString(new String[0]));
    }

    @Test public void
    testCharArray4ToString() {
        assertEquals("char[4] 'abc\\n'", PrettyPrinter.toString(new char[] { 'a', 'b', 'c', '\n' }));
    }

    @Test public void
    testIntArray3ToString() {
        assertEquals("int[3] { 1, 2, 3 }", PrettyPrinter.toString(new int[] { 1, 2, 3 }));
    }

    @Test public void
    testIntArray12ToString() {
        assertEquals("int[12] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ... }", PrettyPrinter.toString(new int[12]));
    }

    @Test public void
    testNestedByteArrayToString() {
        assertEquals(
            "byte[3][] { byte[2] { 0B, 0B }, byte[2] { 0B, 0B }, byte[2] { 0B, 0B } }",
            PrettyPrinter.toString(new byte[3][2])
        );
    }

    @Test public void
    testNestedObjectArrayToString() {
        assertEquals(
            "Object[3] { \"WORLD\\n\", 7, char[3] 'abc' }",
            PrettyPrinter.toString(new Object[] { "WORLD\n", 7, new char[] { 'a', 'b', 'c' } })
        );

        {
            Object[] oa = new Object[3];
            oa[1] = oa;
            assertEquals("Object[3] { null, [self], null }", PrettyPrinter.toString(oa));
        }

        {
            Object[] oa1 = new Object[3];
            Object[] oa2 = new Object[3];
            oa1[0] = oa1;
            oa1[1] = oa2;
            oa2[0] = oa2;
            oa2[1] = oa1;
            assertEquals(
                "Object[3] { [self], Object[3] { [self], [parent], null }, null }",
                PrettyPrinter.toString(oa1)
            );
        }
    }

    @Test public void
    testString0ToString() {
        assertEquals("\"\"", PrettyPrinter.toString(""));
    }

    @Test public void
    testString1ToString() {
        assertEquals("\"\\n\"", PrettyPrinter.toString("\n"));
    }

    @Test public void
    testString100ToString() {
        assertEquals("\"" + S100 + "\"", PrettyPrinter.toString(S100));
    }

    @Test public void
    testString101ToString() {
        assertEquals("\"" + S100 + "\"... (101 chars)", PrettyPrinter.toString(S100 + "0"));
    }

    @Test public void
    testCharArray0ToString() {
        assertEquals("char[0]", PrettyPrinter.toString(new char[0]));
    }

    @Test public void
    testCharArray1ToString() {
        assertEquals("char[1] 'A'", PrettyPrinter.toString(new char[] { 'A' }));
    }

    @Test public void
    testCharArray20ToString() {
        assertEquals("char[20] '01234567890123456789'", PrettyPrinter.toString("01234567890123456789".toCharArray()));
    }

    @Test public void
    testCharArray21ToString() {
        assertEquals(
            "char[21] '01234567890123456789'...",
            PrettyPrinter.toString("012345678901234567890".toCharArray())
        );
    }

    @Test public void
    testLargeArray() {

        {
            String[] sa = new String[10];
            for (int i = 0; i < sa.length; i++) sa[i] = S10;
            assertEquals((
                "String[10] { \""
                + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \""
                + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10
                + "\" }"
            ), PrettyPrinter.toString(sa));
        }

        {
            String[] sa = new String[11];
            for (int i = 0; i < sa.length; i++) sa[i] = S10;
            assertEquals((
                "String[11] { \""
                + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \""
                + S10 + "\", \"" + S10 + "\", \"" + S10 + "\", \"" + S10
                + "\", ... }"
            ), PrettyPrinter.toString(sa));
        }
    }

    @Test public void
    testLargeResult() {

        String[] sa = new String[4];
        for (int i = 0; i < sa.length; i++) sa[i] = S100;
        String ex1 = (
            "String[" + sa.length + "] { \"" + S100 + "\", \"" + S100 + "\", \"" + S100 + "\", \"" + S100 + "\" }"
        );

        {
            String[][] saa = new String[2][];
            for (int i = 0; i < saa.length; i++) saa[i] = sa;
            assertEquals(
                "String[" + saa.length + "][] { " + ex1 + ", " + ex1 + " }",
                PrettyPrinter.toString(saa)
            );
        }

        {
            String[][] saa = new String[3][];
            for (int i = 0; i < saa.length; i++) saa[i] = sa;
            assertEquals((
                "String[" + saa.length + "][] { "
                + ex1
                + ", "
                + ex1
                + ", "
                + "String[" + sa.length + "] { \"" + S100 + "\", \"" + S100.substring(0, 33)
                + "\"... (100 chars), ... }"
                + " }"
            ), PrettyPrinter.toString(saa));
        }
    }
}
