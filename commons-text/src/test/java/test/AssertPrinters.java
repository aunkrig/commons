
/*
 * de.unkrig.commons - A general-purpose Java class library
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

package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.AbstractPrinter;
import de.unkrig.commons.text.Printer;
import de.unkrig.commons.text.Printers;

/**
 * Utility methods related to JUNIT and {@code de.unkrig.commons.text}.
 */
public final
class AssertPrinters {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private AssertPrinters() {}

    /**
     * Asserts that the given <var>runnable</var> prints exactly the <var>expectedMessages</var>. Each actual message
     * is prefixed with {@code "E: "}, {@code "W: "}, {@code "I: "}, {@code "V: "} or {@code "D: "} to indicate the
     * message "level" (error, warning, info, verbose or debug).
     */
    public static <EX extends Exception> void
    assertMessages(RunnableWhichThrows<EX> runnable, String... expectedMessages) throws EX {

        assertEquals(Arrays.asList(expectedMessages), recordMessages(runnable));
    }

    /**
     * Asserts that the given <var>runnable</var> prints exactly the <var>expectedMessages</var>. Each actual message
     * is prefixed with {@code "E: "}, {@code "W: "}, {@code "I: "}, {@code "V: "} or {@code "D: "} to indicate the
     * message "level" (error, warning, info, verbose or debug).
     */
    public static <EX extends Exception> void
    assertContainsMessages(RunnableWhichThrows<EX> runnable, String... expectedMessages) throws EX {

        assertContainsAll(Arrays.asList(expectedMessages), recordMessages(runnable));
    }

    /**
     * Records the messages printed by the <var>runnable</var>. Each printed message is prefixed with {@code "E: "},
     * {@code "W: "}, {@code "I: "}, {@code "V: "} or {@code "D: "} to indicate the message "level" (error, warning,
     * info, verbose or debug).
     *
     * @return The printed messages, in chronological order
     */
    private static <EX extends Exception> List<String>
    recordMessages(RunnableWhichThrows<EX> runnable) throws EX {

        final List<String> result   = new ArrayList<String>();
        Printer            recorder = new AbstractPrinter() {
            @Override public void error(@Nullable String message)   { result.add("E: " + message); }
            @Override public void warn(@Nullable String message)    { result.add("W: " + message); }
            @Override public void info(@Nullable String message)    { result.add("I: " + message); }
            @Override public void verbose(@Nullable String message) { result.add("V: " + message); }
            @Override public void debug(@Nullable String message)   { result.add("D: " + message); }
        };

        Printers.withPrinter(recorder, runnable);

        return result;
    }

    /**
     * Asserts that the two lists are equal, element by element.
     */
    public static void
    assertEquals(List<?> expected, List<?> actual) { assertEquals(null, expected, actual); }

    /**
     * Asserts that the two lists are equal, element by element.
     */
    public static void
    assertEquals(@Nullable String message, List<?> expected, List<?> actual) {

        for (int i = 0; i < expected.size() && i < actual.size(); i++) {

            TestCase.assertEquals(
                message == null ? "[" + i + "]" : message + " [" + i + "]",
                expected.get(i),
                actual.get(i)
            );
        }
        if (expected.size() != actual.size()) {
            TestCase.fail("expected " + expected.size() + " element(s), but was " + actual.size() + " element(s)");
        }
    }

    /**
     * Asserts that <var>actual</var> contains all of <var>expected</var>.
     */
    public static void
    assertContainsAll(Collection<?> expected, Collection<?> actual) { assertContainsAll(null, expected, actual); }

    /**
     * Asserts that <var>actual</var> contains all of <var>expected</var>.
     */
    public static void
    assertContainsAll(@Nullable String message, Collection<?> expected, Collection<?> actual) {

        for (Object e : expected) {
            if (!actual.contains(e)) {
                String m = "\"" + e + "\" is missing from " + actual;
                TestCase.fail(message == null ? m : message + ": " + m);
            }
        }
    }
}
