
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

import de.unkrig.commons.nullanalysis.Nullable;


/**
 * Filters messages by their level; by default only {@link #info(String)} and higher are printed.
 */
public
class LevelFilteredPrinter extends AbstractPrinter {

    private boolean printWarnings = true;
    private boolean printInfos    = true;
    private boolean printVerbose;
    private boolean printDebug;

    private final Printer delegate;

    /**
     * @see LevelFilteredPrinter
     */
    public
    LevelFilteredPrinter(Printer delegate) { this.delegate = delegate;  }

    /** Suppress all messages but errors. */
    public void
    setNoWarn() {
        this.printWarnings = false;
        this.printInfos    = false;
        this.printVerbose  = false;
        this.printDebug    = false;
    }

    /** Suppress "normal" output; print only errors and warnings. */
    public void
    setQuiet() {
        this.printWarnings = true;
        this.printInfos    = false;
        this.printVerbose  = false;
        this.printDebug    = false;
    }

    /** Revert to default settings. */
    public void
    setNormal() {
        this.printWarnings = true;
        this.printInfos    = true;
        this.printVerbose  = false;
        this.printDebug    = false;
    }

    /** Print all messages (errors, warnings, infos and verbose) except debug. */
    public void
    setVerbose() {
        this.printWarnings = true;
        this.printInfos    = true;
        this.printVerbose  = true;
        this.printDebug    = false;
    }

    /** Print all messages (errors, warnings, info, verbose and debug). */
    public void
    setDebug() {
        this.printWarnings = true;
        this.printInfos    = true;
        this.printVerbose  = true;
        this.printDebug    = true;
    }

    @Override public void
    error(@Nullable String message) { this.delegate.error(message); }

    @Override public void
    warn(@Nullable String message) { if (this.printWarnings) this.delegate.warn(message); }

    @Override public void
    info(@Nullable String message) { if (this.printInfos) this.delegate.info(message); }

    @Override public void
    verbose(@Nullable String message) { if (this.printVerbose) this.delegate.verbose(message); }

    @Override public void
    debug(@Nullable String message) { if (this.printDebug) this.delegate.debug(message); }
}
