
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

package test;

import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.PropertyPermission;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.security.Sandbox;

// CHECKSTYLE Javadoc:OFF

public
class SandboxTest {

    @Test public void
    testNoPermissions() {

        final Runnable r = new Runnable() { @Override public void run() { System.getProperty("foo"); } };

        r.run();

        Sandbox.confine(r.getClass(), new Permissions());

        try {
            r.run();
            Assert.fail("SecurityException expected");
        } catch (SecurityException se) {
            SandboxTest.assertMatchesRegex(
                "access denied \\(\\[?\"?java.util.PropertyPermission\"? \"?foo\"? \"?read\"?\\]?\\)",
                se.getMessage()
            );
        }

        try {
            r.run();
            Assert.fail("AccessControlException expected");
        } catch (AccessControlException ace) {
            SandboxTest.assertMatchesRegex(
                "access denied \\(\\[?\"?java.util.PropertyPermission\"? \"?foo\"? \"?read\"?\\]?\\)",
                ace.getMessage()
            );
        }
    }

    @Test public void
    testAllPermissions() {

        final Runnable r = new Runnable() { @Override public void run() { System.getProperty("foo"); } };

        r.run();

        {
            Permissions permissions = new Permissions();
            permissions.add(new AllPermission());
            Sandbox.confine(r.getClass(), permissions);
        }

        r.run();
    }

    @Test public void
    testSpecificPermissions() {

        final Runnable r = new Runnable() { @Override public void run() { System.getProperty("foo"); } };

        // Run with SPECIFIC permissions.
        {
            Permissions permissions = new Permissions();
            permissions.add(new PropertyPermission("foo", "read"));
            Sandbox.confine(r.getClass(), permissions);
        }

        r.run();
    }

    @Test public void
    testWrongPermissions() {

        final Runnable r = new Runnable() { @Override public void run() { System.getProperty("foo"); } };

        Permissions permissions = new Permissions();
        permissions.add(new PropertyPermission("foo2", "read"));
        Sandbox.confine(r.getClass(), permissions);

        try {
            r.run();
            Assert.fail("SecurityException expected");
        } catch (SecurityException se) {
            SandboxTest.assertMatchesRegex(
                "access denied \\(\\[?\"?java.util.PropertyPermission\"? \"?foo\"? \"?read\"?\\]?\\)",
                se.getMessage()
            );
        }
    }

    // Cannot use "AssertRegex" from module "commons-junit" here, because that would introduce a mutual
    // dependency between "commons-lang" and "commons-junit4".
    private static void
    assertMatchesRegex(String regex, String actual) {
        if (!Pattern.compile(regex).matcher(actual).matches()) {
            Assert.fail("[" + actual + "] did not match regex [" + regex + "]");
        }
    }
}
