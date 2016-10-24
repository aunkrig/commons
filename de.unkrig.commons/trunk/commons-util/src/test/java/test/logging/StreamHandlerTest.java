
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

package test.logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Test;

import de.unkrig.commons.util.logging.formatter.PrintfFormatter;
import de.unkrig.commons.util.logging.handler.StreamHandler;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class StreamHandlerTest {

    @Test public void
    testActiveSocket() throws IOException {
        String       loggerName    = this.getClass().getName();
        String       handlerName   = StreamHandler.class.getName();
        String       formatterName = PrintfFormatter.class.getName();
        final String s             = (
            ""
            + loggerName + ".handlers = " + handlerName + "\n"
            + "\n"
            + handlerName + ".level = FINEST\n"
            + handlerName + ".formatter = " + formatterName + "\n"
            + handlerName + ".outputStream = de.unkrig.commons.util.logging.ActiveSocketOutputStream(\"localhost\",9999)\n" // SUPPRESS CHECKSTYLE LineLength
            + "\n"
            + formatterName + ".format = MESSAGE\n"
        );
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(s.getBytes()));

        Logger logger = Logger.getLogger(loggerName);
        logger.info("INFO");
    }

    @Test public void
    testPassiveSocket() throws IOException, InterruptedException {
        String       loggerName    = this.getClass().getName();
        String       handlerName   = StreamHandler.class.getName();
        String       formatterName = PrintfFormatter.class.getName();
        final String s             = (
            ""
            + loggerName + ".handlers = " + handlerName + "\n"
            + "\n"
            + handlerName + ".level = FINEST\n"
            + handlerName + ".formatter = " + formatterName + "\n"
            + handlerName + ".outputStream = de.unkrig.commons.net.stream.PassiveSocketOutputStream(9999)\n"
            + "\n"
            + formatterName + ".format = MESSAGE\n"
        );
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(s.getBytes()));

        final Logger logger = Logger.getLogger(loggerName);

        System.out.println("Please connect your TELNET client to port 9999 of this machine...");
        Thread.sleep(10000L);

        logger.info("INFO");
    }
}
