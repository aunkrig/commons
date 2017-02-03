
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

package de.unkrig.commons.text.pattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * This class extends the concepts of the JDK {@link Pattern java.util.regex.Pattern} and {@link Pattern2
 * de.unkrig.commons.util.pattern.Pattern2} classes as follows:
 * <ul>
 *   <li>
 *     {@link Pattern} defines the <i>both</i> the interface of pattern matching <i>and</i> its implementation
 *     (regular expressions). This makes it impossible to have different pattern matchers with the same interface.
 *     {@link Glob} is that new interface, and {@link #compile(String, int) Glob.compile()} compiles a regular
 *     expression into a {@link Glob}, just like {@link Pattern2#compile(String, int) Pattern2.compile()} compiles it
 *     into a {@link Pattern}.
 *   </li>
 *   <li>
 *     {@link Glob} replaces the powerful (yet huge) API of {@code java.util.regex} with a simple one: {@link
 *     #matches(String)} and {@link #replace(String)}. Pattern <i>finding</i> (as opposed to <i>matching</i>) and
 *     repeated replacements are no longer supported. However, this approach is much more generic than (highly
 *     regex-specific) concepts of "groups", "anchors", "lookaheads" and "lookbehinds".
 *   </li>
 *   <li>
 *     The new {@link #REPLACEMENT} compilation flag modifies the pattern compilation such that a "replacement string"
 *     can be defined <i>in</i> the pattern, which is often convenient.
 *   </li>
 *   <li>
 *     The new {@link #INCLUDES_EXCLUDES} compilation flag modifies the pattern compilation such that a pattern can
 *     be composed from a <i>sequence</i> of patterns, which are combined logically.
 *   </li>
 * </ul>
 *
 * @see #compile(String)
 * @see #compile(String, int)
 * @see #compileRegex(Pattern)
 * @see #compileRegex(Pattern, String)
 */
public abstract
class Glob implements Predicate<String> {

    /**
     * @return Whether the <var>subject</var> matches this {@link Glob}.
     */
    public abstract boolean
    matches(String subject);

    /**
     * Implementation of {@link Predicate#evaluate}; calls {#matches}.
     * <p>
     * If your code uses only {@link Glob} and not {@link Predicate}, you should favor calling {@link #matches},
     * because that method name is more expressive.
     * <p>
     * A <var>subject</var> value {@code null} evaluates to {@code false}.
     */
    @Override public boolean
    evaluate(@Nullable String subject) {
        return subject != null && this.matches(subject);
    }

    /**
     * Iff the <var>subject</var> matches this {@link Glob}, then a non-null string ist returned; the algorithm that
     * computes that string depends on the concrete {@link Glob} implementation; the default implementation simply
     * returns the <var>subject</var>.
     * <p>
     * Otherwise, {@code null} is returned.
     */
    @Nullable public String
    replace(String subject) {
        return this.matches(subject) ? subject : null;
    }

    /**
     * Modifies the pattern compilation as follows:
     * <p>
     * '=' is now a metacharacter, i.e. to include it literally in the pattern, it must be escaped with a backslash.
     * <p>
     * The semantics of '=' is as follows:
     * <table border="1">
     *   <tr>
     *   <tr align="left">
     *     <th colspan="2" id="alternatives"><font color="red">Replacement</font></th>
     *   </tr>
     *     <td valign="top" headers="construct characters"><i>a</i><tt>=</tt><i>b</i></td>
     *     <td headers="matches">
     *       If a subject matches <i>a</i>, then the {@link #replace(String)} method does not return the {@code
     *       subject}, but <i>b</i>. The replacement string <i>b</i> may contain references to captured subsequences as
     *       in the {@link Matcher#appendReplacement} method.
     *     </td>
     *   </tr>
     * </table>
     */
    public static final int REPLACEMENT = 0x40000000;

    /**
     * Modifies the pattern compilation as follows:
     * <p>
     * ',' and '~' are now metacharacters, i.e. to include them literally in the pattern, they must be escaped with a
     * backslash.
     * <p>
     * The semantics of ',' and '~' are as follows:
     * <table border="1">
     *   <tr align="left">
     *     <th bgcolor="#CCCCFF" align="left" id="construct">Construct</th>
     *     <th bgcolor="#CCCCFF" align="left" id="matches">Matches</th>
     *   </tr>
     *   <tr align="left">
     *     <th colspan="2" id="alternatives"><font color="red">Includes and excludes</font></th>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><i>a</i><tt>,</tt><i>b</i></td>
     *     <td headers="matches">Any subject that matches <i>a</i> or <i>b</i></td>
     *   </tr>
     *   <tr>
     *     <td valign="top" headers="construct characters"><i>a</i><tt>~</tt><i>b</i></td>
     *     <td headers="matches">Any subject that matches <i>a</i>, but not <i>b</i></td>
     *   </tr>
     * </table>
     * Patterns are applied right-to-left, i.e. the rightmost pattern that matches determines the result. This is
     * particularly relevant in conjunction with {@link #REPLACEMENT}.
     */
    public static final int INCLUDES_EXCLUDES = 0x80000000;

    /**
     * A {@link Glob} that {@link #matches(String) matches} any string (and thus {@link #replace(String) replace}s it
     * with itself).
     */
    public static final Glob ANY = new Glob() {
        @Override public boolean matches(String subject) { return true; }
        @Override public String  toString()              { return "ANY"; }
    };

    /**
     * A {@link Glob} that {@link #matches(String) matches} no string.
     */
    public static final Glob NONE = new Glob() {
        @Override public boolean matches(String subject) { return false; }
        @Override public String  toString()              { return "NONE"; }
    };

    /**
     * Like {@link #compile(String, int)}, but without support for {@link #INCLUDES_EXCLUDES}.
     */
    private static Glob
    compileWithReplacement(String pattern, int flags) {

        // Process the REPLACEMENT flag.
        final String replacement;
        if ((flags & (Pattern.LITERAL | Glob.REPLACEMENT)) == Glob.REPLACEMENT) {

            String[] tmp = Pattern2.parsePatternAndReplacement(pattern);

            pattern     = tmp[0];
            replacement = tmp[1];
        } else {
            replacement = null;
        }

        final Glob glob = Glob.compileRegex(Pattern2.compile(pattern, flags), replacement);

        // In the WILDCARD mode, wrap the glob in order to override the "toString()" method, so it returns the
        // *wildcard patten*, and not the *regex pattern*.
        if ((flags | Pattern2.WILDCARD) == Pattern2.WILDCARD) {

            final String pattern2 = pattern;
            return new Glob() {

                @Override public boolean          matches(String subject)            { return glob.matches(subject);  }
                @Override public boolean          evaluate(@Nullable String subject) { return glob.evaluate(subject); }
                @Override @Nullable public String replace(String subject)            { return glob.replace(subject);  }

                @Override public String
                toString() { return replacement == null ? pattern2 : pattern2 + "=" + replacement; }
            };
        }

        return glob;
    }

    /**
     * Returns a {@link Glob} who's {@link #replace(String)} method will return its <var>subject</var> argument if the
     * subject matches the given <var>regex</var>.
     */
    public static Glob
    compileRegex(final Pattern regex) {
        return Glob.compileRegex(regex, null);
    }

    /**
     * The behavior of the {@link #matches(String)} method of the returned {@link Glob} is as follows:
     * <ul>
     *   <li>If the <var>regex</var> matches the <var>subject</var>, then {@code true} is returned.</li>
     *   <li>
     *     Otherwise, if the <var>regex</var> matches a prefix of <var>subject</var>, and that prefix is followed by
     *     '/' or '!', then {@code true} is returned. (Effectively, a glob 'dir' or 'dir/file.zip' matches all members
     *     and entries under 'dir' resp. 'dir/file.zip'.)
     *   </li>
     *   <li>
     *     Otherwise, if the subject ends with "!" or "/", and the <var>regex</var> could match the concatenation of
     *     the <var>subject</var> and another string, then {@code true} is returned. (Example: The <var>subject</var>
     *     {@code "dir/"} is matched by <var>regex</var>s {@code "dir"}, {@code "dir/"}, {@code "dir/anything"} and
     *     {@code "**.c"}, but not by <var>regex</var>s {@code "dirr/anything"}, {@code "file"}, {@code "*.c"} and
     *     {@code "file.zip!file"}.)
     *   </li>
     *   <li>Otherwise {@code false} is returned.</li>
     * </ul>
     * The behavior of the {@link #replace(String)} method of the returned {@link Glob} is as follows:
     * <ul>
     *   <li>
     *     If the subject matches the <var>regex</var>, then a non-null string is returned:
     *     <ul>
     *       <li>If <var>replacementString</var> is {@code null}, then the subject is returned.</li>
     *       <li>
     *         Otherwise, the <var>replacementString</var> is returned, with '$1', '$2', ... replaced with the {@code
     *         regex}'s "capturing groups" (see {@link Pattern}).
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @see Matcher#appendReplacement(StringBuffer, String)
     */
    public static Glob
    compileRegex(final Pattern regex, @Nullable final String replacementString) {

        return new Glob() {

            @Override public boolean
            matches(String subject) {
                Matcher matcher = regex.matcher(subject);
                if (subject.isEmpty() || subject.endsWith("/") || subject.endsWith("!")) {

                    // Subcomponent match (e.g. subject 'a/b/c/d' vs. glob 'a/b/')?
                    return matcher.matches() || matcher.hitEnd();
                }

                for (;;) {

                    // Precise match (e.g. subject 'a/b/c' vs. glob 'a/b/c') or subcomponent match (e.g. subject
                    // 'a/b/c/d' vs. glob 'a/b/c')?
                    if (matcher.matches()) return true;
                    for (int i = subject.length() - 1;; i--) {
                        if (i < 0) return false;
                        char c = subject.charAt(i);
                        if (c == '/' || c == '!') {
                            subject = subject.substring(0, i);
                            break;
                        }
                    }
                }
            }

            @Override @Nullable public String
            replace(String subject) {

                Matcher matcher = regex.matcher(subject);

                if ((regex.flags() & Pattern2.WILDCARD) == 0 ? matcher.matches() : matcher.lookingAt()) {

                    StringBuffer sb = new StringBuffer();

                    final int matchEnd = matcher.end();
                    matcher.appendReplacement(sb, replacementString == null ? "$0" : replacementString);

                    // Precise match (e.g. subject 'a/b/c' vs. glob 'a/b/c')?
                    if (matchEnd == subject.length()) return sb.toString();

                    // Subcomponent match (e.g. subject 'a/b/c!d' vs. glob 'a/b/c')?
                    char c = subject.charAt(matchEnd);
                    if (c == '/' || c == '!') return sb.append(subject.substring(matchEnd)).toString();

                    return null;
                }

                return null;
            }

            @Override public String
            toString() {
                return replacementString == null ? regex.toString() : regex + "=" + replacementString;
            }
        };
    }

    /**
     * Equivalent with {@code compile(regex, 0)}.
     *
     * @see #compile(String, int)
     */
    public static Glob
    compile(final String pattern) {
        return Glob.compile(pattern, 0);
    }

    /**
     * Similar to {@link Pattern#compile(String, int)}, but returns a {@link Glob} instead of a {@link Pattern}.
     * <p>
     *   Iff the flag {@link #REPLACEMENT} is set, then the pattern may include a "replacement".
     * </p>
     * <p>
     *   Iff a replacement is specified, then {@link Glob#replace(String)} will return the replacement, with
     *   "{@code $1}"... replaces with the match groups; otherwise the <var>subject</var> will be returned.
     * </p>
     *
     * @param flags Modifies the semantics of the <var>pattern</var>, e.g. {@link Pattern2#WILDCARD} switches from
     *              regular expressions to wildcards
     * @see         #INCLUDES_EXCLUDES
     * @see         #REPLACEMENT
     * @see         Pattern2#WILDCARD
     * @see         Pattern#CANON_EQ
     * @see         Pattern#CASE_INSENSITIVE
     * @see         Pattern#COMMENTS
     * @see         Pattern#DOTALL
     * @see         Pattern#LITERAL
     * @see         Pattern#MULTILINE
     * @see         Pattern#UNICODE_CASE
     * @see         Pattern#UNIX_LINES
     */
    public static Glob
    compile(final String pattern, int flags) {

        if ((flags & Glob.INCLUDES_EXCLUDES) == 0) {
            return Glob.compileWithReplacement(pattern, flags);
        }

        // Break the wildcard pattern up at ',' and '~' and construct an 'IncludeExclude' object from it.

        int            idx;
        IncludeExclude includeExclude = new IncludeExclude();
        if (pattern.startsWith("~")) {
            Glob glob = Glob.compileWithReplacement(
                pattern.substring(1, (idx = Pattern2.findMeta(",~", pattern, 1))),
                flags
            );
            includeExclude.addExclude(glob, true);
        } else {
            Glob glob = Glob.compileWithReplacement(
                pattern.substring(0, (idx = Pattern2.findMeta(",~", pattern, 0))),
                flags
            );

            // Shortcut for a wildcard pattern without ',' and '~'.
            if (idx == pattern.length()) return glob;

            includeExclude.addInclude(glob, true);
        }

        while (idx != pattern.length()) {
            char c    =  pattern.charAt(idx++);
            Glob glob = Glob.compileWithReplacement(
                pattern.substring(idx, (idx = Pattern2.findMeta(",~", pattern, idx))),
                flags
            );
            if (c == ',') {
                includeExclude.addInclude(glob, true);
            } else {
                includeExclude.addExclude(glob, true);
            }
        }

        return includeExclude;
    }

    /**
     * The {@link #matches(String)} method of the returned {@link Glob} returns whether its <var>subject</var>
     * argument matches both <var>pattern1</var> and <var>pattern2</var>.
     * <p>
     * The {@link #replace(String)} method of the returned {@link Glob} returns checks whether the <var>subject</var>
     * matches <var>pattern1</var>; if so, it calls {@link #replace(String)} on <var>pattern2</var> and returns the
     * result; otherwise it returns {@code null}.
     */
    public static Glob
    and(final Glob pattern1, final Glob pattern2) {
        return new Glob() {

            @Override public boolean
            matches(String subject) {
                return pattern1.matches(subject) && pattern2.matches(subject);
            }

            @Override @Nullable public String
            replace(String subject) {
                return pattern1.matches(subject) ? pattern2.replace(subject) : null;
            }

            @Override public String
            toString() {
                return pattern1 + " && " + pattern2;
            }
        };
    }

    /**
     * The {@link #matches(String)} method of the returned {@link Glob} returns whether its <var>subject</var>
     * argument matches <var>pattern1</var> or, if not, <var>pattern2</var>.
     * <p>
     * The {@link #replace(String)} method of the returned {@link Glob} returns calls {@link #replace(String)} on
     * <var>pattern1</var> and returns the result if it is not {@code null}; otherwise it calls {@link
     * #replace(String)} on <var>pattern2</var> and returns the result.
     */
    public static Glob
    or(final Glob pattern1, final Glob pattern2) {
        return new Glob() {

            @Override public boolean
            matches(String subject) {
                return pattern1.matches(subject) || pattern2.matches(subject);
            }

            @Override @Nullable public String
            replace(String subject) {
                String replacement = pattern1.replace(subject);
                if (replacement != null) return replacement;
                return pattern2.replace(subject);
            }

            @Override public String
            toString() {
                return pattern1 + " || " + pattern2;
            }
        };
    }

    /**
     * The {@link #matches(String)} method of the returned {@link Glob} returns whether the <var>predicate</var>
     * evaluates to {@code true} and the <var>subject</var> argument matches the <var>pattern</var>.
     * <p>
     * The {@link #replace(String)} method of the returned {@link Glob} returns checks whether the <var>predicate</var>
     * evaluates to {@code true}; if so, it calls {@link #replace(String)} on <var>pattern</var> and returns the
     * result; otherwise it returns {@code null}.
     */
    public static Glob
    and(final Predicate<? super String> predicate, final Glob pattern) {
        return new Glob() {

            @Override public boolean
            matches(String subject) {
                return predicate.evaluate(subject) && pattern.matches(subject);
            }

            @Override @Nullable public String
            replace(String subject) {
                return predicate.evaluate(subject) ? pattern.replace(subject) : null;
            }

            @Override public String
            toString() {
                return predicate + " && " + pattern;
            }
        };
    }

    /**
     * The {@link #matches(String)} method of the returned {@link Glob} returns whether the <var>subject</var> argument
     * matches the <var>pattern</var> and the <var>predicate</var> evaluates to {@code true}.
     * <p>
     * The {@link #replace(String)} method of the returned {@link Glob} returns checks whether the {@link
     * Glob#replace(String)} on the <var>pattern</var> returns nuon-{@code null} and the <var>predicate</var> evaluates
     * to {@code true}; if so, it returns the result of the {@link Glob#replace(String)} call; otherwise it returns {@code
     * null}.
     */
    public static Glob
    and(final Glob pattern, final Predicate<? super String> predicate) {
        return new Glob() {

            @Override public boolean
            matches(String subject) {
                return pattern.matches(subject) && predicate.evaluate(subject);
            }

            @Override @Nullable public String
            replace(String subject) {
                String replacement = pattern.replace(subject);
                return replacement != null && predicate.evaluate(subject) ? replacement : null;
            }

            @Override public String
            toString() {
                return pattern + " && " + predicate;
            }
        };
    }

    /** @return A glob that wraps the given <var>predicate</var> */
    public static Glob
    fromPredicate(final Predicate<? super String> predicate) {
        return new Glob() {

            @Override public boolean
            matches(String subject) { return predicate.evaluate(subject); }
        };
    }
}
