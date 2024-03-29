
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

package de.unkrig.commons.text.scanner;

import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;

/**
 * A scanner which has a notion of an 'offset' where the previously scanned token starts.
 *
 * @param <TT> Defines the token types that this scanner will produce
 * @see        #produce()
 */
public
interface StringScanner<TT extends Enum<TT>> extends ProducerWhichThrows<Token<TT>, ScanException> {

    /**
     * @param cs The {@link CharSequence} from which following calls to {@link #produce()} will scan tokens
     *
     * @return This scanner
     */
    StringScanner<TT> setInput(CharSequence cs);

    /**
     * @param cs The {@link CharSequence} from which following calls to {@link #produce()} will scan tokens
     *
     * @return This scanner
     */
    StringScanner<TT> setInput(CharSequence cs, int start, int end);

    /**
     * Before {@link #setInput(CharSequence)} is called, this method returns {@code null}. After {@link
     * #setInput(CharSequence)} was called, this method breaks the input char character sequence up into tokens and
     * returns them one by one. When the input char sequence is exhausted, {@code null} is returned until {@link
     * #setInput(CharSequence)} is called again.
     */
    @Override @Nullable Token<TT>
    produce() throws ScanException;

    /**
     * @return The offset within the input string set through {@link #setInput(CharSequence)} where the previously
     *         scanned token ends, or 0 after the call to {@link #setInput(CharSequence)} and before the first
     *         call to {@link #produce()}
     */
    int getOffset();

    /**
     * @return The offset within the input string set through {@link #setInput(CharSequence)} where the previously
     *         scanned token begins, or -1 after the call to {@link #setInput(CharSequence)} and before the first
     *         call to {@link #produce()}
     */
    int getPreviousTokenOffset();
}
