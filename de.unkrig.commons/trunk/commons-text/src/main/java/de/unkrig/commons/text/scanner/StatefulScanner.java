
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
import java.util.Map.Entry;
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

    /**
     * Special value for some method parameters.
     */
    @Nullable public final EnumSet<S> ANY_STATE = null; // SUPPRESS CHECKSTYLE MemberName

    /**
     * Special value for some method parameters; indicates that the current state should <em>remain</em> when the
     * rule applies.
     *
     * @see #addRule(EnumSet, String, Enum, Enum)
     */
    @Nullable public final S REMAIN = null; // SUPPRESS CHECKSTYLE MemberName

    private final List<List<Rule>> stateStack = new ArrayList<List<Rule>>();

    public
    StatefulScanner(Class<S> states) {
        this.defaultStateRules    = new ArrayList<Rule>();
        this.nonDefaultStateRules = new HashMap<S, List<Rule>>();
        this.currentStateRules    = this.defaultStateRules;

        for (S state : states.getEnumConstants()) {
            this.nonDefaultStateRules.put(state, new ArrayList<Rule>());
        }
    }

    /**
     * Clones the configuration of the other scanner, but has a separate state.
     */
    public
    StatefulScanner(StatefulScanner<TT, S> that) {
        this.defaultStateRules    = that.defaultStateRules;
        this.nonDefaultStateRules = that.nonDefaultStateRules;

        // We start in the default state.
        this.currentStateRules = this.defaultStateRules;
    }

    /**
     * Adds a rule that applies iff the scanner is in the "default state". After the rule has matched, the scanner
     * remains in the default state.
     *
     * @see Pattern
     */
    public Rule
    addRule(String regex, TT tokenType) {
        Rule rule = new Rule(regex, tokenType, this.defaultStateRules);
        this.defaultStateRules.add(rule);
        return rule;
    }

    /**
     * Adds a rule that applies iff the scanner is in the given non-default <var>state</var>. The scanner returns to
     * the default state after the rule has matched.
     *
     * @param state {@code null} means "any state"
     * @see Pattern
     */
    public Rule
    addRule(S state, String regex, TT tokenType) {
        Rule rule = new Rule(regex, tokenType, this.defaultStateRules);
        this.nonDefaultStateRules.get(state).add(rule);
        return rule;
    }

    /**
     * Adds a rule that applies iff <var>states</var>{@code ==} {@link #ANY_STATE}, or the scanner is in one of the
     * given non-default <var>states</var>, or the <var>states</var> contain {@code null} and the scanner is in the
     * default state. The scanner returns to the default state after the rule has matched.
     *
     * @see Pattern
     */
    public Rule
    addRule(@Nullable EnumSet<S> states, String regex, TT tokenType) {

        Rule rule = new Rule(regex, tokenType, this.defaultStateRules);

        if (states == /*this.ANY_STATE*/ null) {
            for (List<Rule> rules : this.nonDefaultStateRules.values()) rules.add(rule);
            this.defaultStateRules.add(rule);
        } else {
            for (S s : states) this.nonDefaultStateRules.get(s).add(rule);
        }

        return rule;
    }

    /**
     * @return The current state of this scanner; {@code null} for the default state
     */
    @Nullable public S
    getCurrentState() {

        if (this.currentStateRules == this.defaultStateRules) return null;

        for (Entry<S, List<Rule>> e : this.nonDefaultStateRules.entrySet()) {
            S          state = e.getKey();
            List<Rule> rules = e.getValue();

            if (rules == this.currentStateRules) return state;
        }

        throw new AssertionError(this.currentStateRules);
    }

    /**
     * @param newState The new state of this scanner; {@code null} for the default state
     */
    public void
    setCurrentState(@Nullable S newState) {

        this.currentStateRules = newState == null ? this.defaultStateRules : this.nonDefaultStateRules.get(newState);
    }

    /**
     * @return {@code null} iff the input string is exhausted
     */
    @Override @Nullable public Token<TT>
    produce() throws ScanException {

        int length = this.cs.length();
        if (this.offset == length) return null;

        for (Rule rule : this.currentStateRules) {
            Matcher matcher = rule.regex.matcher(this.cs);
            matcher.region(this.offset, length);
            if (matcher.lookingAt()) {
                if (rule.popState) {
                    this.currentStateRules = this.stateStack.remove(this.stateStack.size() - 1);
                } else {
                    if (rule.pushState) this.stateStack.add(this.currentStateRules);
                    if (rule.nextStateRules != null/*this.REMAIN*/) this.currentStateRules = rule.nextStateRules;
                }

                this.previousTokenOffset = this.offset;
                this.offset              = matcher.end();

                int      gc       = matcher.groupCount();
                String[] captured = new String[gc];
                for (int i = 0; i < gc; i++) captured[i] = matcher.group(i + 1);

                return new Token<TT>(rule.tokenType, matcher.group(), captured);
            }
        }

        String message = (
            "Unexpected character \""
            + this.cs.charAt(this.offset)
            + "\" at offset "
            + this.offset
            + " of input string \""
            + this.cs
            + "\""
        );

        if (this.currentStateRules == this.defaultStateRules) {
            message += " in default state";
        } else {
            for (Entry<S, List<Rule>> e : this.nonDefaultStateRules.entrySet()) {
                S          state = e.getKey();
                List<Rule> rules = e.getValue();

                if (this.currentStateRules == rules) {
                    message += " in state " + state;
                    break;
                }
            }
        }

        if (this.currentStateRules.size() == 1) {
            message += "; expected " + this.currentStateRules.get(0).tokenType;
        } else
        if (this.currentStateRules.size() > 1) {
            message += "; expected one of " + this.currentStateRules.get(0).tokenType;
            for (int i = 1; i < this.currentStateRules.size(); i++) {
                message += ", " + this.currentStateRules.get(i).tokenType;
            }
        }
        throw new ScanException(message);
    }

    /**
     * @deprecated Use {@code ss.addRule(regex, tokenType).goTo(nextState)} instead.
     */
    @Deprecated public Rule
    addRule(String regex, TT tokenType, S nextState) {
        return this.addRule(regex, tokenType).goTo(nextState);
    }

    /**
     * @deprecated Use {@code addRule(state, regex, tokenType).goTo(nextState)}
     */
    @Deprecated public Rule
    addRule(S state, String regex, TT tokenType, @Nullable S nextState) {
        return this.addRule(state, regex, tokenType).goTo(nextState);
    }

    /**
     * @deprecated Use {@code ss.addRule(states, regex, tokenType).goTo(nextState)} instead
     */
    @Deprecated public Rule
    addRule(@Nullable EnumSet<S> states, String regex, TT tokenType, @Nullable S nextState) {
        return this.addRule(states, regex, tokenType).goTo(nextState);
    }

    // IMPLEMENTATION

    public
    class Rule {
        final TT             tokenType;
        final Pattern        regex;
        @Nullable List<Rule> nextStateRules;
        private boolean      pushState, popState;

        /**
         * @param nextStateRules {@code null} means remain in current state
         */
        Rule(String regex, TT tokenType, @Nullable List<Rule> nextStateRules) {
            this.regex          = Pattern.compile(regex);
            this.tokenType      = tokenType;
            this.nextStateRules = nextStateRules;
        }

        /**
         * @param nextState {@code ss.REMAIN} means remain in current state
         */
        public Rule
        goTo(@Nullable S nextState) {
            this.nextStateRules = StatefulScanner.this.nonDefaultStateRules.get(nextState);
            return this;
        }

        /**
         * Saves the current state and changes to the <var>nextState</var>.
         *
         * @see #pop()
         */
        public Rule
        push(S nextState) {
            this.pushState      = true;
            this.nextStateRules = StatefulScanner.this.nonDefaultStateRules.get(nextState);
            return this;
        }

        /**
         * Restores a previously pushed state.
         *
         * @see #push(Enum)
         */
        public Rule
        pop() {
            this.popState = true;
            return this;
        }

        @Override public String
        toString() { return ">>" + this.regex + "<< => " + this.tokenType; }
    }

    // CONFIGURATION

    private final List<Rule>         defaultStateRules;
    private final Map<S, List<Rule>> nonDefaultStateRules;

    // STATE

    private List<Rule> currentStateRules;
}
