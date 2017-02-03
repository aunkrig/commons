
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

package de.unkrig.commons.util.time;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of the length of time between two points of time, with a resolution of 1 millisecond.
 */
public final
class Duration {

    /**
     * Patterns for valid duraction specs.
     * <p>
     *   Notice that the pattern "{@code 12:34}" is intentionally <i>not</i> valid, because it is ambiguous
     *   ("{@code hh:mm}" vs. "{@code mm:ss}"). Same for "{@code 99[.9]}".
     * </p>
     */
    // CHECKSTYLE WrapAndIndent:OFF
    // CHECKSTYLE ConstantName:OFF
    // CHECKSTYLE LineLength:OFF
    private static final Pattern
    TI_X            = Pattern.compile("\\d++(?:\\.\\d*)?"),                                                                           // 9[.9] - disallowed!
    TI_MS           = Pattern.compile("(\\d+)ms(?:ecs?)?"),                                                                           // 999ms[ec[s]]
    TI_S            = Pattern.compile("(\\d+)(\\.\\d*)?s(?:ecs?)?"),                                                                  // 59[.9]s[ec[s]]
    TI_M            = Pattern.compile("(\\d+)mins?"),                                                                                 // 59min[s]
    TI_X_X          = Pattern.compile("(?:\\d+):(?:\\d+)"),                                                                           // 12:34 - disallowed!
    TI_M_S__1       = Pattern.compile("(\\d+)mins? (\\d+)(\\.\\d*)?s(?:ecs?)?"),                                                      // 59min 59[.9]s[ec[s]]
    TI_M_S__2       = Pattern.compile("(\\d+):(\\d+)(\\.\\d*)?(?:mins?)?"),                                                           // 59:59[.9][min[s]]
    TI_H            = Pattern.compile("(\\d++)h(?:ours?)?"),                                                                          // 23h[our[s]]
    TI_H_M__1       = Pattern.compile("(\\d+):(\\d+)h(?:ours?)?"),                                                                    // 23:59h[our[s]]
    TI_H_M__2       = Pattern.compile("(\\d+)h(?:ours?)? (\\d+)mins?"),                                                               // 23h[our[s]] 59min[s]
    TI_H_M_S__1     = Pattern.compile("(\\d+):(\\d+):(\\d+)(\\.\\d*)?"),                                                              // 23:59:59[.9]
    TI_H_M_S__2     = Pattern.compile("(\\d+)h(?:ours?)? (\\d+)mins? (\\d+)(\\.\\d*)?s(?:ecs?)?"),                                    // 23h[our[s]] 59min[s] 59[.9]s[ec[s]]
    TI_D            = Pattern.compile("(\\d++)d(?:ays?)?"),                                                                           // 7d[ay[s]]
    TI_D_H          = Pattern.compile("(\\d+)d(?:ays?)? (\\d+)h(?:ours?)?"),                                                          // 7d[ay[s]] 23h[our[s]]
    TI_D_H_M__1     = Pattern.compile("(\\d+)d(?:ays?)? (\\d+):(\\d+)"),                                                              // 7d[ay[s]] 23:59
    TI_D_H_M__2     = Pattern.compile("(\\d+)d(?:ays?)? (\\d+)h(?:ours?)? (\\d+)mins?"),                                              // 7d[ay[s]] 23h[our[s]] 59min[s]
    TI_D_H_M_S__1   = Pattern.compile("(\\d+)d(?:ays?)? (\\d+):(\\d+):(\\d+)(\\.\\d*)?"),                                             // 7d[ay[s]] 23:59:59[.9]
    TI_D_H_M_S__2   = Pattern.compile("(\\d+)d(?:ays?)? (\\d+)h(?:ours?)? (\\d+)mins? (\\d+)(\\.\\d*)?s(?:ecs?)?"),                   // 7d[ay[s]] 23h[our[s]] 59min[s] 59[.9]s[ec[s]]
    TI_W            = Pattern.compile("(\\d++)w(?:eeks?)?"),                                                                          // 1w
    TI_W_D          = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)?"),                                                          // 1w[eek[s]] 1d[ay[s]]
    TI_W_D_H        = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)? (\\d+)h(?:ours?)?"),                                        // 1w[eek[s]] 1d[ay[s]] 23h[our[s]]
    TI_W_D_H_M__1   = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)? (\\d+):(\\d+)"),                                            // 1w[eek[s]] 1d[ay[s]] 23:59
    TI_W_D_H_M__2   = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)? (\\d+)h(?:ours?)? (\\d+)mins?"),                            // 1w[eek[s]] 1d[ay[s]] 23h[our[s]] 59min[s]
    TI_W_D_H_M_S__1 = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)? (\\d+):(\\d+):(\\d+)(\\.\\d*)?"),                           // 1w[eek[s]] 1d[ay[s]] 23:59:59[.9]
    TI_W_D_H_M_S__2 = Pattern.compile("(\\d+)w(?:eeks?)? (\\d+)d(?:ays?)? (\\d+)h(?:ours?)? (\\d+)mins? (\\d+)(\\.\\d*)?s(?:ecs?)?"); // 1w[eek[s]] 1d[ay[s]] 23h[our[s]] 59min[s] 59[.9]s[ec[s]]
    // CHECKSTYLE WrapAndIndent:ON
    // CHECKSTYLE ConstantName:ON
    // CHECKSTYLE LineLength:ON

    private final long ms;

    public
    Duration(long ms) { this.ms = ms; }

    public
    Duration(double seconds) { this.ms = (long) (1000.0 * seconds); }

    /**
     * Creates a {@link Duration} from a string representation.
     * <p>
     *   Accepts duration specifications in any of the following formats:
     * </p>
     * <ul>
     *   <li>{@code 999ms} (milliseconds)</li>
     *   <li>{@code 59[.9]s} (seconds)</li>
     *   <li>{@code 59min} (minutes)</li>
     *   <li>{@code 59min 59[.9]s}</li>
     *   <li>{@code 59:59[.9][min]}</li>
     *   <li>{@code 23h} (hours)</li>
     *   <li>{@code 23:59h}</li>
     *   <li>{@code 23h 59min}</li>
     *   <li>{@code 23:59:59[.9]} (hours, minutes and seconds)</li>
     *   <li>{@code 23h 59min 59[.9]sec}</li>
     *   <li>{@code 7d} (days)</li>
     *   <li>{@code 7d 23h}</li>
     *   <li>{@code 7d 23:59}</li>
     *   <li>{@code 7d 23h 59min}</li>
     *   <li>{@code 7d 23:59:59[.9]}</li>
     *   <li>{@code 7d 23h 59min 59[.9]s}</li>
     *   <li>{@code 1w} (weeks, i.e. intervals of seven days)</li>
     *   <li>{@code 1w 1d}</li>
     *   <li>{@code 1w 1d 23h}</li>
     *   <li>{@code 1w 1d 23:59}</li>
     *   <li>{@code 1w 1d 23h 59min}</li>
     *   <li>{@code 1w 1d 23:59:59[.9]}</li>
     *   <li>{@code 1w 1d 23h 59min 59[.9]s}</li>
     * </ul>
     * <p>
     *   Notice that the pattern "{@code 12:34}" is intentionally <i>not</i> valid, because it is ambiguous
     *   ("{@code hh:mm}" vs. "{@code mm:ss}"). Same for "{@code 99[.9]}".
     * </p>
     * <p>
     *   Some alternative unit notations are allowed:
     * </p>
     * <dl>
     *   <dt>ms</dt>
     *   <dd>msec msecs</dd>
     *   <dt>s</dt>
     *   <dd>sec secs</dd>
     *   <dt>min</dt>
     *   <dd>mins</dd>
     *   <dt>h</dt>
     *   <dd>hour hours</dd>
     *   <dt>d</dt>
     *   <dd>day days</dd>
     *   <dt>w</dt>
     *   <dd>week weeks</dd>
     * </dl>
     */
    public
    Duration(String s) {
        Matcher m;

        if ((m = Duration.TI_X.matcher(s)).matches()) {
            throw new IllegalArgumentException(
                "'"
                + s
                + "' could mean weeks, days, hours, minutes, seconds or milliseconds; please write '"
                + s
                + "w, "
                + s
                + "d, "
                + s
                + "h, "
                + s
                + "min, "
                + s
                + "s, "
                + s
                + "ms, "
                + s
                + ":00:00', '0:"
                + s
                + ":00' or '0:0:"
                + s
                + "'"
            );
        }

        if ((m = Duration.TI_MS.matcher(s)).matches()) {
            this.ms = Duration.milliseconds(m.group(1));
            return;
        }

        if ((m = Duration.TI_S.matcher(s)).matches()) {
            this.ms = Duration.seconds(m.group(1)) + Duration.sfrac(m.group(2));
            return;
        }

        if ((m = Duration.TI_M.matcher(s)).matches()) {
            this.ms = Duration.minutes(m.group(1));
            return;
        }

        if ((m = Duration.TI_X_X.matcher(s)).matches()) {
            throw new IllegalArgumentException(
                "'"
                + s
                + "' could mean 'hh:mm' or 'mm:ss'; please use either '"
                + s
                + ":00' or '0:"
                + s
                + "'"
            );
        }

        if ((m = Duration.TI_M_S__1.matcher(s)).matches() || (m = Duration.TI_M_S__2.matcher(s)).matches()) {
            this.ms = Duration.minutes(m.group(1)) + Duration.seconds(m.group(2)) + Duration.sfrac(m.group(3));
            return;
        }

        if ((m = Duration.TI_H.matcher(s)).matches()) {
            this.ms = Duration.hours(m.group(1));
            return;
        }

        if ((m = Duration.TI_H_M__1.matcher(s)).matches() || (m = Duration.TI_H_M__2.matcher(s)).matches()) {
            this.ms = Duration.hours(m.group(1)) + Duration.minutes(m.group(2));
            return;
        }

        if ((m = Duration.TI_H_M_S__1.matcher(s)).matches() || (m = Duration.TI_H_M_S__2.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.hours(m.group(1))
                + Duration.minutes(m.group(2))
                + Duration.seconds(m.group(3))
                + Duration.sfrac(m.group(4))
            );
            return;
        }

        if ((m = Duration.TI_D.matcher(s)).matches()) {
            this.ms = Duration.days(m.group(1));
            return;
        }

        if ((m = Duration.TI_D_H.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.days(m.group(1))
                + Duration.hours(m.group(2))
            );
            return;
        }

        if ((m = Duration.TI_D_H_M__1.matcher(s)).matches() || (m = Duration.TI_D_H_M__2.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.days(m.group(1))
                + Duration.hours(m.group(2))
                + Duration.minutes(m.group(3))
            );
            return;
        }

        if ((m = Duration.TI_D_H_M_S__1.matcher(s)).matches() || (m = Duration.TI_D_H_M_S__2.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.days(m.group(1))
                + Duration.hours(m.group(2))
                + Duration.minutes(m.group(3))
                + Duration.seconds(m.group(4))
                + Duration.sfrac(m.group(5))
            );
            return;
        }

        if ((m = Duration.TI_W.matcher(s)).matches()) {
            this.ms = Duration.weeks(m.group(1));
            return;
        }

        if ((m = Duration.TI_W_D.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.weeks(m.group(1))
                + Duration.days(m.group(2))
            );
            return;
        }

        if ((m = Duration.TI_W_D_H.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.weeks(m.group(1))
                + Duration.days(m.group(2))
                + Duration.hours(m.group(3))
            );
            return;
        }

        if ((m = Duration.TI_W_D_H_M__1.matcher(s)).matches() || (m = Duration.TI_W_D_H_M__2.matcher(s)).matches()) {
            this.ms = (
                0
                + Duration.weeks(m.group(1))
                + Duration.days(m.group(2))
                + Duration.hours(m.group(3))
                + Duration.minutes(m.group(4))
            );
            return;
        }

        if (
            (m = Duration.TI_W_D_H_M_S__1.matcher(s)).matches()
            || (m = Duration.TI_W_D_H_M_S__2.matcher(s)).matches()
        ) {
            this.ms = (
                0
                + Duration.weeks(m.group(1))
                + Duration.days(m.group(2))
                + Duration.hours(m.group(3))
                + Duration.minutes(m.group(4))
                + Duration.seconds(m.group(5))
                + Duration.sfrac(m.group(6))
            );
            return;
        }

        throw new IllegalArgumentException("Time interval '" + s + "' cannot be parsed");
    }

    private static long
    weeks(String s) {
        return Long.parseLong(s) * (7 * 24 * 3600 * 1000);
    }

    private static long
    days(String s) {
        return Long.parseLong(s) * (24 * 3600 * 1000);
    }

    private static long
    hours(String s) {
        return Long.parseLong(s) * (3600 * 1000);
    }

    private static long
    minutes(String s) {
        return Long.parseLong(s) * (60 * 1000);
    }

    private static long
    seconds(String s) {
        return Long.parseLong(s) * 1000;
    }

    private static long
    milliseconds(String s) {
        return Long.parseLong(s);
    }

    /**
     * Parses strings like {@code . .9 .99 .999}.
     * <p>
     *   Iff the string is longer than four characters, then the excess characters are ignored.
     * </p>
     * <p>
     *   Iff the first character is not the period, or iff the the second, third and fourth character is not a
     *   digit, then the result is undefined.
     * </p>
     *
     * @return The parsed value in milliseconds, e.g. {@code 900 990 999}, or {@code 0} iff {@code s == null}
     */
    private static long
    sfrac(@Nullable String s) {

        if (s == null) return 0;

        int l = s.length();

        int result = 0;
        if (l >= 2) {
            result += Character.digit(s.charAt(1), 10) * 100;
            if (l >= 3) {
                result += Character.digit(s.charAt(2), 10) * 10;
                if (l >= 4) result += Character.digit(s.charAt(3), 10);
            }
        }
        return result;
    }

    /** @return The number of milliseconds represented by this object */
    public long milliseconds() { return this.ms; }

    /** @return The length of time in seconds represented by this object */
    public double toSeconds() { return this.ms / 1000.0; }

    @Override public String
    toString() {
        return (
            this.ms < 1000 ? this.ms + "ms" :
            this.ms < 60 * 1000 ? Duration.msToString(this.ms, false) + 's' :
            this.ms < 24 * 60 * 60 * 1000 ? String.format(
                Locale.US,
                "%d:%02d:%s",
                (this.ms / (60 * 60 * 1000)),
                (this.ms /      (60 * 1000)) % 60,
                Duration.msToString(this.ms % (60 * 1000), true)
            ) :
            this.ms < 7 * 24 * 60 * 60 * 1000 ? String.format(
                Locale.US,
                "%dd %d:%02d:%s",
                (this.ms / (24 * 60 * 60 * 1000)),
                (this.ms /      (60 * 60 * 1000)) % 24,
                (this.ms /           (60 * 1000)) % 60,
                Duration.msToString(this.ms % (60 * 1000), true)
            ) :
            String.format(
                Locale.US,
                "%dw %dd %d:%02d:%s",
                (this.ms / (7 * 24 * 60 * 60 * 1000)),
                (this.ms /     (24 * 60 * 60 * 1000)) % 7,
                (this.ms /          (60 * 60 * 1000)) % 24,
                (this.ms /               (60 * 1000)) % 60,
                Duration.msToString(this.ms % (60 * 1000), true)
            )
        );
    }

    /**
     * @param twoDigits Whether to left-pad the integral part with '0'
     */
    private static String
    msToString(long t, boolean twoDigits) {

        StringBuilder sb = new StringBuilder();

        long s = (int) (t / 1000);
        if (twoDigits && s <= 9) sb.append('0');
        sb.append(s);

        int ms = (int) (t % 1000);
        if (ms != 0) {
            sb.append('.').append(Character.forDigit(ms / 100, 10));
            if (ms % 100 != 0) {
                sb.append(Character.forDigit(ms / 10 % 10, 10));
                if (ms % 10 != 0) {
                    sb.append(Character.forDigit(ms % 10, 10));
                }
            }
        }

        return sb.toString();
    }

    /** @return A duration that represents the sum of {@code this} and <var>other</var> */
    public Duration
    add(Duration other) { return new Duration(this.ms + other.ms); }

    /** @return A duration that is <var>factor</var> as long as this duration */
    public Duration
    multiply(double factor) { return new Duration((long) (this.ms * factor)); }

    /** @return A duration which is one <var>divisor</var>th of this duration long */
    public Duration
    divide(double divisor) { return new Duration((long) (this.ms / divisor)); }

    /** @return Whether this object represents the zero-length interval */
    public boolean
    isZero() { return this.ms == 0; }

    @Override public int
    hashCode() { return (int) (this.ms ^ (this.ms >>> 32)); }

    @Override public boolean
    equals(@Nullable Object obj) { return obj == this || (obj instanceof Duration && ((Duration) obj).ms == this.ms); }
}

