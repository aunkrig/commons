
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A scanner that produces {@link AbstractScanner.Token Token}s from a character stream.
 * <p>
 *   Before {@link #produce()} is called, the scanner must be configured by invoking its {@link #addRule(String, Enum)}
 *   methods.
 *   These define how character sequences are converted into {@link AbstractScanner.Token}s, and also how the scanner
 *   changes state. Initially the scanner is in the default state.
 * </p>
 * <p>
 *   The non-default states are defined by the <var>S</var> type parameter.
 * </p>
 * <p>
 *   For an example usage, see {@link JavaScanner}.
 * </p>
 *
 * @param <TT> Enumerates the scanner-specific token types
 * @param <S>  Enumerates the scanner-specific non-default states
 */
public
class StatefulScanner<TT extends Enum<TT>, S extends Enum<S>> extends AbstractScanner<TT> {

    @Nullable public final EnumSet<S> ANY_STATE = null;

    public
    StatefulScanner(Class<S> states) {
        this.defaultStateRules    = new ArrayList<Rule<TT, S>>();
        this.nonDefaultStateRules = new HashMap<S, List<Rule<TT, S>>>();
        this.currentStateRules    = this.defaultStateRules;

        for (S state : states.getEnumConstants()) {
            this.nonDefaultStateRules.put(state, new ArrayList<StatefulScanner.Rule<TT, S>>());
        }
    }

    /**
     * Clones the configuration of the other scanner, but has a separate state.
     */
    public
    StatefulScanner(StatefulScanner<TT, S> that) {
        this.defaultStateRules    = that.defaultStateRules;
        this.nonDefaultStateRules = that.nonDefaultStateRules;
        this.currentStateRules    = this.defaultStateRules;
    }

    /**
     * Adds a rule that applies iff the scanner is in the "default state". After the rule has matched, the scanner
     * remains in the default state.
     *
     * @see Pattern
     */
    public void
    addRule(String regex, TT tokenType) {
        this.addRule(regex, tokenType, this.defaultStateRules);
    }

    /**
     * Adds a rule that applies iff the scanner is in the "default state".
     *
     * @param nextState The new current state after the rule has matched
     * @see Pattern
     */
    public void
    addRule(String regex, TT tokenType, S nextState) {
        this.addRule(regex, tokenType, this.nonDefaultStateRules.get(nextState));
    }

    /**
     * Adds a rule that applies iff the scanner is in the given non-default <var>state</var>. The scanner returns to
     * the default state after the rule has matched.
     *
     * @param state {@code null} means "any state"
     * @see Pattern
     */
    public void
    addRule(S state, String regex, TT tokenType) {
        this.addRule(EnumSet.of(state), regex, tokenType);
    }

    /**
     * Adds a rule that applies iff <var>states</var>{@code ==null}, or the scanner is in one of the given non-default
     * <var>states</var>. The scanner returns to the default state after the rule has matched.
     *
     * @see Pattern
     */
    public void
    addRule(@Nullable EnumSet<S> states, String regex, TT tokenType) {

        Rule<TT, S> rule = new Rule<TT, S>(regex, tokenType, this.defaultStateRules);

        if (states == null) {
            for (List<Rule<TT, S>> rules : this.nonDefaultStateRules.values()) rules.add(rule);
            this.defaultStateRules.add(rule);
        } else {
            for (S s : states) this.nonDefaultStateRules.get(s).add(rule);
        }
    }

    /**
     * Adds a rule that applies iff the scanner is in the given non-default <var>state</var>.
     *
     * @param nextState The new state after the rule has matched; {@code null} means "remain in current state"
     * @see Pattern
     */
    public void
    addRule(S state, String regex, TT tokenType, @Nullable S nextState) {
        this.addRule(EnumSet.of(state), regex, tokenType, nextState);
    }

    /**
     * Adds a rule that applies iff <var>states</var>{@code == null}, or if the scanner is in one of the the given
     * non-default <var>states</var>.
     *
     * @param nextState The new current state after the rule has matched; {@code null} means "remain in current state"
     * @see Pattern
     */
    public void
    addRule(@Nullable EnumSet<S> states, String regex, TT tokenType, @Nullable S nextState) {

        if (nextState != null) {
            Rule<TT, S> rule = new Rule<TT, S>(regex, tokenType, this.nonDefaultStateRules.get(nextState));

            if (states == null) {
                for (List<Rule<TT, S>> rules : this.nonDefaultStateRules.values()) {
                    rules.add(rule);
                }
                this.defaultStateRules.add(rule);
            } else {
                for (S s : states) this.nonDefaultStateRules.get(s).add(rule);
            }
        } else {

            if (states == null) {
                for (List<Rule<TT, S>> rules : this.nonDefaultStateRules.values()) {
                    rules.add(new Rule<TT, S>(regex, tokenType, rules));
                }
                this.defaultStateRules.add(new Rule<TT, S>(regex, tokenType, this.defaultStateRules));
            } else {
                for (S s : states) {
                    List<Rule<TT, S>> rules = this.nonDefaultStateRules.get(s);
                    rules.add(new Rule<TT, S>(regex, tokenType, rules));
                }
            }
        }
    }

    /**
     * @return {@code null} iff the input string is exhausted
     */
    @Override @Nullable public Token<TT>
    produce() throws ScanException {

        int length = this.cs.length();
        if (this.offset == length) return null;

        for (Rule<TT, S> rule : this.currentStateRules) {
            Matcher matcher = rule.regex.matcher(this.cs);
            matcher.region(this.offset, length);
            if (matcher.lookingAt()) {
                this.currentStateRules   = rule.nextStateRules;
                this.previousTokenOffset = this.offset;
                this.offset              = matcher.end();
                return new Token<TT>(rule.tokenType, matcher.group());
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

    private void
    addRule(String regex, TT tokenType, List<Rule<TT, S>> nextRules) {

        Rule<TT, S> rule = new Rule<TT, S>(regex, tokenType, nextRules);

        this.defaultStateRules.add(rule);
    }

    private static
    class Rule<TT extends Enum<TT>, S extends Enum<S>> {
        final TT                tokenType;
        final Pattern           regex;
        final List<Rule<TT, S>> nextStateRules;

        Rule(String regex, TT tokenType, List<Rule<TT, S>> nextStateRules) {
            this.regex          = Pattern.compile(regex);
            this.tokenType      = tokenType;
            this.nextStateRules = nextStateRules;
        }
    }

    // CONFIGURATION

    private final List<Rule<TT, S>>         defaultStateRules;
    private final Map<S, List<Rule<TT, S>>> nonDefaultStateRules;

    // STATE

    private List<Rule<TT, S>> currentStateRules;
}
