
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

package de.unkrig.commons.file.org.apache.commons.compress.compressors;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An abstract {@link URLStreamHandler} which makes the implementation of archive-based {@link URLStreamHandler}s
 * simple.
 * <p>
 *   The path component is interpreted as follows:
 * </p>
 * <quote><code><i>container-url</i>!<i>entry-name</i></code></quote>
 */
public abstract
class CompressionURLStreamHandler extends URLStreamHandler {

    /**
     * Opens the given {@code container} and returns an {@link InputStream} that reads from it.
     */
    protected abstract InputStream open(InputStream containerInputStream) throws IOException;

    @Override protected URLConnection
    openConnection(@Nullable URL url) {
        assert url != null;

        return new URLConnection(url) {

            @Nullable private URL container;

            @Override public void
            connect() throws IOException {

                if (this.connected) return;

                final String authority = this.url.getAuthority();
                final String host      = this.url.getHost();
                final String path      = this.url.getPath();
                final int    port      = this.url.getPort();
                final String query     = this.url.getQuery();
                final String ref       = this.url.getRef();
                final String userInfo  = this.url.getUserInfo();

                if (authority != null) throw new IllegalArgumentException(this.url + ": 'Authority' not allowed");
                if (host.length() > 0) throw new IllegalArgumentException(this.url + ": 'Host' not allowed");
                if (port != -1)        throw new IllegalArgumentException(this.url + ": 'Port' not allowed");
                if (query != null)     throw new IllegalArgumentException(this.url + ": 'Query' not allowed");
                if (ref != null)       throw new IllegalArgumentException(this.url + ": 'Fragment' not allowed");
                if (userInfo != null)  throw new IllegalArgumentException(this.url + ": 'User info' not allowed");

                this.container = new URL(path);

                this.connected = true;
            }

            @Override public InputStream
            getInputStream() throws IOException {

                // Implicitly invoke "connect()"
                this.connect();

                URL container = this.container;
                assert container != null;

                return CompressionURLStreamHandler.this.open(container.openStream());
            }
        };
    }
}
