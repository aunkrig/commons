
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A scanner that produces {@link AbstractScanner.Token}s. Before {@link #produce()} is called, the scanner must be
 * configured by invoking its {@link #addRule(String, Enum)} methods. These define how character sequences are
 * converted into {@link AbstractScanner.Token}s.
 *
 * <p>For an example usage, see the source code of {@link de.unkrig.commons.text.expression.Scanner}
 *
 * @param <TT> Enumerates the scanner-specific token types
 */
public
class StatelessScanner<TT extends Enum<TT>> extends AbstractScanner<TT> {

    /**
     * Adds a rule that produces the given <var>tokenType</var> if the next characters of the input match the given
     * <var>regex</var>.
     */
    public void
    addRule(String regex, TT tokenType) {
        this.rules.add(new Rule<TT>(regex, tokenType));
    }

    /**
     * @return {@code null} iff the input string is exhausted
     */
    @Override @Nullable public Token<TT>
    produce() throws ScanException {

        int length = this.cs.length();
        if (this.offset == length) return null;

        for (Rule<TT> rule : this.rules) {
            Matcher matcher = rule.regex.matcher(this.cs);
            matcher.region(this.offset, length);
            if (matcher.lookingAt()) {
                this.previousTokenOffset = this.offset;
                this.offset              = matcher.end();

                int gc = matcher.groupCount();
                String[] captured = new String[gc];
                for (int i = 0; i < gc; i++) captured[i] = matcher.group(i + 1);

                return new Token<TT>(rule.tokenType, matcher.group(), captured);
            }
        }

        throw new ScanException(
            "Unexpected character '"
            + this.cs.charAt(this.offset)
            + "' at offset "
            + this.offset
            + " of '"
            + this.cs
            + "'"
        );
    }

    // IMPLEMENTATION

    private static
    class Rule<TT extends Enum<TT>> {
        final TT      tokenType;
        final Pattern regex;

        Rule(String regex, TT tokenType) {
            this.regex     = Pattern.compile(regex);
            this.tokenType = tokenType;
        }
    }

    // CONFIGURATION

    private final List<Rule<TT>> rules = new ArrayList<Rule<TT>>();
}
