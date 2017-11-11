
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.Test;

import de.unkrig.commons.util.logging.ActiveSocketOutputStream;
import de.unkrig.commons.util.logging.formatter.PrintfFormatter;

public
class ActiveSocketOutputStreamTest {

    @Test public void
    test() throws IOException {
        OutputStream os = new ActiveSocketOutputStream("localhost", 9999);

        for (int i = 0; i < 5; i++) {
            os.write(("LINE " + (i + 1) + "\r\n").getBytes());
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                ;
            }
        }

        os.close();
    }

    @Test public void
    testLoggingToActiveSocketOutputStream() {
        Logger        logger  = Logger.getLogger(this.getClass().getName());
        StreamHandler handler = new StreamHandler(
            new ActiveSocketOutputStream("localhost", 9999),
            new PrintfFormatter("MESSAGE")
        );

        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.info("HI THERE");
        logger.removeHandler(handler);
        handler.close();
    }
}
