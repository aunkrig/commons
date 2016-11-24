
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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
 * An {@link AbstractPrinter} that farwards all method calls to a <var>delegate</var> printer. Can serve as a base
 * class when you want to override only a few of {@link AbstractPrinter}'s methods.
 */
public
class ProxyPrinter extends AbstractPrinter {

    private final Printer delegate;

    /**
     * @see ProxyPrinter
     */
    public ProxyPrinter(Printer delegate) { this.delegate = delegate; }

    @Override public void error(@Nullable String message)                                   { this.delegate.error(message);               }
    @Override public void error(@Nullable String message, @Nullable Throwable t)            { this.delegate.error(message, t);            }
    @Override public void error(String pattern, Object... arguments)                        { this.delegate.error(pattern, arguments);    }
    @Override public void error(String pattern, @Nullable Throwable t, Object... arguments) { this.delegate.error(pattern, t, arguments); }

    @Override public void warn(@Nullable String message)                                    { this.delegate.warn(message);                }
    @Override public void warn(String pattern, Object... arguments)                         { this.delegate.warn(pattern, arguments);     }

    @Override public void info(@Nullable String message)                                    { this.delegate.info(message);                }
    @Override public void info(String pattern, Object... arguments)                         { this.delegate.info(pattern, arguments);     }

    @Override public void verbose(@Nullable String message)                                 { this.delegate.verbose(message);             }
    @Override public void verbose(String pattern, Object... arguments)                      { this.delegate.verbose(pattern, arguments);  }

    @Override public void debug(@Nullable String message)                                   { this.delegate.debug(message);               }
    @Override public void debug(String pattern, Object... arguments)                        { this.delegate.debug(pattern, arguments);    }
}
