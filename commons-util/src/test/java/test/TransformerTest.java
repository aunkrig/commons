
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

package test;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class TransformerTest {

    private static final TransformerWhichThrows<? super IOException, ? extends IOException, NoException>
    IOE_2_IOE = TransformerUtil.<IOException, IOException, NoException>identity();

    private static final TransformerWhichThrows<? super IOException, ? extends IOException, ? extends IOException>
    IOE_2_IOE_IOE = TransformerUtil.asTransformerWhichThrows(TransformerTest.IOE_2_IOE);

    @Test public void
    testWidenTransformer() {

        // Widen.
        TransformerWhichThrows<? super IOException, ? extends IOException, NoException>
        fnfe2e = TransformerTest.IOE_2_IOE;

        FileNotFoundException subject = new FileNotFoundException();
        Assert.assertSame(subject, fnfe2e.transform(subject));
    }

    @Test public void
    testWidenTransformerWhichThrows() throws Exception {

        // Widen.
        TransformerWhichThrows<? super FileNotFoundException, ? extends Exception, ? extends Exception>
        fnfe2eWe = TransformerTest.IOE_2_IOE_IOE;

        FileNotFoundException subject = new FileNotFoundException();
        Assert.assertSame(subject, fnfe2eWe.transform(subject));
    }

    @Test public void
    testCastTransformer() throws Exception {

        // Cast.
        TransformerWhichThrows<? super FileNotFoundException, ? extends Exception, ? extends Exception>
        fnfe2eWe = TransformerUtil.asTransformerWhichThrows(TransformerTest.IOE_2_IOE);

        FileNotFoundException subject = new FileNotFoundException();
        Assert.assertSame(subject, fnfe2eWe.transform(subject));
    }
}
