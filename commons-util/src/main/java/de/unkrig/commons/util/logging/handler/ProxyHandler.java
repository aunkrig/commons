
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

package de.unkrig.commons.util.logging.handler;

import java.io.UnsupportedEncodingException;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link Handler} that redirects all method calls to a delegate, which can be changed at runtime.
 */
@NotNullByDefault(false) public
class ProxyHandler extends Handler {

    @Nullable private Handler delegate;

    /**
     * Constructs a proxy handler <i>without</i> a delegate.
     */
    public
    ProxyHandler() {
        this.delegate = null;
    }

    /**
     * @param delegate {@code null} if a delegate is not (yet) desired
     */
    public
    ProxyHandler(@Nullable Handler delegate) {
        this.delegate = delegate;
    }

    /**
     * Changes the delegate handler.
     *
     * @param delegate {@code null} if a delegate is no longer desired
     */
    public void
    setDelegate(@Nullable Handler delegate) {
        this.delegate = delegate;
    }

    // Override getters.

    @Override public Formatter
    getFormatter() { return this.delegate != null ? this.delegate.getFormatter() : null; }

    @Override public String
    getEncoding() { return this.delegate != null ? this.delegate.getEncoding() : null; }

    @Override public Filter
    getFilter() { return this.delegate != null ? this.delegate.getFilter() : null; }

    @Override public ErrorManager
    getErrorManager() { return this.delegate != null ? this.delegate.getErrorManager() : null; }

    @Override public Level
    getLevel() { return this.delegate != null ? this.delegate.getLevel() : null; }

    // Override setters.

    @Override public void
    setFormatter(Formatter formatter) throws SecurityException {
        if (this.delegate != null) this.delegate.setFormatter(formatter);
    }

    @Override public void
    setEncoding(String encoding) throws SecurityException, UnsupportedEncodingException {
        if (this.delegate != null) this.delegate.setEncoding(encoding);
    }

    @Override public void
    setFilter(Filter newFilter) throws SecurityException {
        if (this.delegate != null) this.delegate.setFilter(newFilter);
    }

    @Override public void
    setErrorManager(ErrorManager errorManager) {
        if (this.delegate != null) this.delegate.setErrorManager(errorManager);
    }

    @Override public void
    setLevel(Level level) throws SecurityException {
        if (this.delegate != null) this.delegate.setLevel(level);
    }

    // Override services.

    @Override public boolean
    isLoggable(LogRecord record) {
        return this.delegate != null ? this.delegate.isLoggable(record) : false;
    }

    @Override public void
    publish(LogRecord record) {
        if (this.delegate != null) this.delegate.publish(record);
    }

    @Override public void
    flush() {
        if (this.delegate != null) this.delegate.flush();
    }

    @Override public void
    close() throws SecurityException {
        if (this.delegate != null) this.delegate.close();
    }
}
