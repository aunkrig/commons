
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

import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.MemoryHandler;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.logging.LogUtil;

/**
 * A {@link Handler} that formats its message <i>immediately</i> and forwards it to a delegate. This is useful if
 * <i>mutable</i> objects are used as log record parameters, and the delegate handler formats messages with a delay
 * (like {@link MemoryHandler}.
 * <p>
 * This may cause noticable costs if the objects' Object{@link #toString()} methods are expensive and/or return large
 * strings.
 */
@NotNullByDefault(false) public
class EagerHandler extends ProxyHandler {

    public
    EagerHandler() throws ParseException, EvaluationException {
        this(null);
    }

    public
    EagerHandler(@Nullable String propertyNamePrefix) throws ParseException, EvaluationException {

        if (propertyNamePrefix == null) propertyNamePrefix = this.getClass().getName();

        this.setDelegate(LogUtil.getLoggingProperty(propertyNamePrefix + ".delegate", Handler.class));
    }

    @Override public void
    publish(LogRecord record) {

        // Format the message eagerly.
        Object[] parameters = record.getParameters();
        if (parameters != null && parameters.length > 0) {
            String message = record.getMessage();
            if (message.contains("{0")) {
                try {
                    record.setMessage(MessageFormat.format(message, parameters));
                    record.setParameters(null);
                } catch (Exception e) {
                    ;
                }
            }
        }

        super.publish(record);
    }
}
