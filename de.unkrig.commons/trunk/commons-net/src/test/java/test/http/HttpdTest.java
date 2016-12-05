
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

package test.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.net.http.HttpProxy;
import de.unkrig.commons.net.security.KeyStores;
import de.unkrig.commons.net.tool.httpd.Httpd;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class HttpdTest {

    private static final File HTTP_ROOT_DIR = new File("src/test/resources/http_root");
    private static final File KEYSTORE_FILE = new File("src/test/resources/keystore");

    
    @Test public void
    testGetFile() throws IOException {

//        SimpleLogging.setDebug();

        Httpd httpd = new Httpd(new InetSocketAddress(0), HTTP_ROOT_DIR + "{path}");
        try {
            ThreadUtil.runInBackground(httpd, "httpd");

            URLConnection connection = new URL(
                "http",
                "localhost",
                httpd.getEndpointAddress().getPort(),
                "/index.html"
            ).openConnection();
            Assert.assertEquals("text/html", connection.getContentType());
            InputStream is1 = connection.getInputStream();
            InputStream is2 = new FileInputStream(new File(HTTP_ROOT_DIR, "index.html"));
            HttpdTest.assertContentsEqual(is2, is1);
            is1.close();
            is2.close();
        } finally {
            httpd.stop();
        }
    }

    @Test public void
    testGetSecureFile() throws Exception {

        char[] keyStorePassword = "mypassword".toCharArray();
        String serverAlias      = "localhost";

        java.security.KeyStore keyStore = KeyStores.loadKeyStore(KEYSTORE_FILE, keyStorePassword);

        SSLContext sslContext = KeyStores.getSslContext(keyStore, keyStorePassword, serverAlias);

        Httpd httpd = new Httpd(new InetSocketAddress(0), sslContext, HTTP_ROOT_DIR + "{path}");
        try {
            ThreadUtil.runInBackground(httpd, "httpd");

            System.setProperty("javax.net.ssl.trustStore",         KEYSTORE_FILE.getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", new String(keyStorePassword));
            URLConnection connection = new URL(
                "https",
                "localhost",
                httpd.getEndpointAddress().getPort(),
                "/index.html"
            ).openConnection();
            Assert.assertEquals("text/html", connection.getContentType());
            InputStream is1 = connection.getInputStream();
            InputStream is2 = new FileInputStream(new File(HTTP_ROOT_DIR, "index.html"));
            HttpdTest.assertContentsEqual(is2, is1);
            is1.close();
            is2.close();
        } finally {
            httpd.stop();
        }
    }

    @Test public void
    testGetIndexFile() throws IOException {

//        SimpleLogging.setDebug();

        Httpd httpd = new Httpd(new InetSocketAddress(0), HTTP_ROOT_DIR + "{path}");
        try {
            ThreadUtil.runInBackground(httpd, "httpd");

            URLConnection connection = new URL(
                "http",
                "localhost",
                httpd.getEndpointAddress().getPort(),
                "/"
            ).openConnection();
            Assert.assertEquals("text/html", connection.getContentType());
            InputStream is1 = connection.getInputStream();
            InputStream is2 = new FileInputStream(new File(HTTP_ROOT_DIR, "index.html"));
            HttpdTest.assertContentsEqual(is2, is1);
            is1.close();
            is2.close();
        } finally {
            httpd.stop();
        }
    }

    @Test public void
    testGetDirListing() throws IOException {

//        SimpleLogging.init();
//        Logger.getLogger("de.unkrig.commons.net.http.HttpServer").setLevel(Level.FINEST);

        Httpd httpd = new Httpd(new InetSocketAddress(0), HTTP_ROOT_DIR + "{path}");
        try {
            ThreadUtil.runInBackground(httpd, "httpd");

            URLConnection connection = new URL(
                "http",
                "localhost",
                httpd.getEndpointAddress().getPort(),
                "/dir"
            ).openConnection();
            Assert.assertEquals("text/html", connection.getContentType());
            InputStream is = connection.getInputStream();
            HttpdTest.assertContentsEqual((
                ""
                + "<html>\r\n"
                + "  <head>\r\n"
                + "    <title>Directory Listing</title>\r\n"
                + "  </head>\r\n"
                + "  <body>\r\n"
                + "  <h2>Directory listing of '[^']*[\\\\/]http_root[\\\\/]dir'</h2>\r\n"
                + "  <pre><a href=\"../\">../</a>\r\n"
//                + "<a href=\".svn/\">.svn</a> +... ... .. ..:..:.. \\w+ ....\r\n"
                + "<a href=\"file.txt\">file.txt</a> +... ... .. ..:..:.. \\w+ ....</pre>\r\n"
                + "  </body>\r\n"
                + "</html>\r\n"
            ), is);
            is.close();
        } finally {
            httpd.stop();
        }
    }

    @Test public void
    testHttpdProxy() throws IOException {

//        SimpleLogging.setDebug();

        Httpd     httpd     = new Httpd(new InetSocketAddress(0), HTTP_ROOT_DIR + "{path}");
        HttpProxy httpProxy = new HttpProxy(new InetSocketAddress(0), httpd.getEndpointAddress());
        try {
            ThreadUtil.runInBackground(httpd, "httpd");
            ThreadUtil.runInBackground(httpProxy, "http_proxy");

            URLConnection connection = new URL(
                "http",
                "localhost",
                httpProxy.getEndpointAddress().getPort(),
                "/index.html"
            ).openConnection();
            Assert.assertEquals("text/html", connection.getContentType());
            InputStream is1 = connection.getInputStream();
            InputStream is2 = new FileInputStream(new File(HTTP_ROOT_DIR, "index.html"));
            HttpdTest.assertContentsEqual(is2, is1);
            is1.close();
            is2.close();
        } finally {
            httpd.stop();
            httpProxy.stop();
        }
    }

    private static void
    assertContentsEqual(InputStream expected, InputStream actual) throws IOException {
        for (int offset = 0;; offset++) {
            int expectedC = expected.read();
            int actualC   = actual.read();
            Assert.assertEquals("Streams differ at offset " + offset, expectedC, actualC);
            if (actualC == -1) return;
        }
    }

    private static void
    assertContentsEqual(String expected, InputStream actual) throws IOException {
        String actualText = new String(InputStreams.readAll(actual));
        if (Pattern.matches(expected, actualText)) return;
        Assert.assertEquals(expected, actualText);
    }
}
