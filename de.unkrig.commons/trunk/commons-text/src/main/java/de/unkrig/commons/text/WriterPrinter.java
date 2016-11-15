
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

package de.unkrig.commons.text;

import java.io.PrintWriter;
import java.io.Writer;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A printer that prints its messages to two {@link Writer}s
 */
public
class WriterPrinter extends AbstractPrinter {

    private PrintWriter out = new PrintWriter(System.out), err = new PrintWriter(System.err);

    public void
    setOut(Writer out) { this.out = out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter(out); }

    public void
    setErr(Writer err) { this.err = err instanceof PrintWriter ? (PrintWriter) err : new PrintWriter(err); }

    @Override public void error(@Nullable String message)   { this.err.println(message); }
    @Override public void warn(@Nullable String message)    { this.err.println(message); }
    @Override public void info(@Nullable String message)    { this.out.println(message); }
    @Override public void verbose(@Nullable String message) { this.out.println(message); }
    @Override public void debug(@Nullable String message)   { this.out.println(message); }
}
