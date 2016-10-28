
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.util;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * Represents singular or periodic time events.
 */
public abstract
class TimeTable {

    TimeTable() {}

    /**
     * @return The next scheduled point in time after {@code previous}, or {@link #MAX_DATE} iff there is no "next"
     *         execution
     */
    public abstract Date
    next(Date previous);

    /**
     * A {@link TimeTable} who's {@link #next(Date)} method will return {@code date} if {@code previous} is before
     * {@code date}, and {@link #MAX_DATE} otherwise.
     */
    public static TimeTable
    once(final Date date) {
        return new TimeTable() {

            @Override public Date
            next(Date previous) { return date.compareTo(previous) > 0 ? date : TimeTable.MAX_DATE; }

            @Override public String
            toString() { return date.toString(); }
        };
    }

    /**
     * A {@link TimeTable} who's {@link #next(Date)} method always returns {@link #MAX_DATE}.
     */
    public static final TimeTable NEVER = new TimeTable() {

        @Override public Date
        next(Date previous) { return TimeTable.MAX_DATE; }

        @Override public String
        toString() { return "NEVER"; }
    };

    /**
     * A {@link Date} very far in the future.
     */
    public static final Date MAX_DATE = new Date(Long.MAX_VALUE);

    /**
     * A {@link Date} very far in the past.
     */
    public static final Date MIN_DATE = new Date(Long.MIN_VALUE);

    private enum TokenType { INTEGER, IDENTIFIER, OPERATOR, SPACE, BEFORE_TIME_PATTERN }
    private enum ScannerState { IN_TIME_PATTERN }

    /**
     * Creates a {@link TimeTable} from a string:
     * <table border="1">
     *   <tr><th>Example</th><th>Meaning</th></tr>
     *   <tr><td>2012-12-31 23:59:59<br>2012-12-31 23:59<br>2012-12-31</td><td>One-time</td></tr>
     *   <tr><td>*-12-31 23:59:59<br>*-12-31 23:59<br>*-12-31</td><td>Once per year</td></tr>
     *   <tr><td>*-*-31 23:59:59<br>*-*-31 23:59<br>*-*-31</td><td>Once per month</td></tr>
     *   <tr><td>Mon 23:59:59<br>Mon 23:59<br>Mon</td><td>Once per week</td></tr>
     *   <tr><td>23:59:59<br>23:59</td><td>Once per day</td></tr>
     *   <tr><td>*:59:59<br>*:59</td><td>Once per hour</td></tr>
     *   <tr><td>*:*:59<br>*:*</td><td>Once per minute</td></tr>
     *   <tr><td>*:*:*</td><td>Every second</td></tr>
     * </table>
     * Instead of '*', integer ranges, integer lists, and combinations thereof can be given:
     * <dl>
     *   <dd>(1,2,3)
     *   <dd>(2020-2031)
     *   <dd>(12-3,7)
     * </dl>
     * Instead of 'Mon', weekday ranges, weekday lists, and combinations thereof can be given:
     * <dl>
     *   <dd>Sat,Sun
     *   <dd>Mon-Fri
     *   <dd>Sun-Mon
     *   <dd>Mon-Wed,Sat
     * </dl>
     * If both day-of-month and day-of-week are specified, then <i>both</i> must match; e.g. '*-2-(8-14) Mon' means
     * 'second monday of february'.
     * <p>
     * Multiple patterns can be specified, separated with commas, which means that the {@link #next(Date)} method
     * will return the earliest next point-in-time that matches any of the patterns. Example: '*-*-1,Mon' -
     * next first-of-month or next monday, whichever comes first.
     */
    public static TimeTable
    parse(String s) throws ParseException {
        StatefulScanner<TokenType, ScannerState> scanner = (
            new StatefulScanner<TokenType, ScannerState>(ScannerState.class)
        );

        // A look-ahead pseudo token that is produced to indicate that the following pattern is a TIME pattern.
        scanner.addRule("(?=[^ ]*:)", TokenType.BEFORE_TIME_PATTERN, ScannerState.IN_TIME_PATTERN);

        scanner.addRule(
            "\\d+",
            TokenType.INTEGER
        );
        scanner.addRule(
            ScannerState.IN_TIME_PATTERN,
            "\\d+",
            TokenType.INTEGER,
            ScannerState.IN_TIME_PATTERN
        );
        scanner.addRule(
            "[:\\-/\\*(),]",
            TokenType.OPERATOR
        );
        scanner.addRule(
            ScannerState.IN_TIME_PATTERN,
            "[:\\-/\\*(),]",
            TokenType.OPERATOR,
            ScannerState.IN_TIME_PATTERN
        );

        scanner.addRule(null, "\\w+", TokenType.IDENTIFIER);
        scanner.addRule(null, " +", TokenType.SPACE);
        scanner.setInput(s);

        try {
            return new Parser(scanner).parse();
        } catch (ParseException pe) {
            throw ExceptionUtil.wrap("\"" + s + "\" at offset " + scanner.getPreviousTokenOffset(), pe);
        }
    }

    private static final
    class Parser extends AbstractParser<TokenType> {

        /**
         * @param scanner Must support {@link CharStream#peek(int)} with distances greater than zero
         */
        Parser(AbstractScanner<TokenType> scanner) {
            super(scanner);
        }

        /**
         * Parses a {@link TimeTable} object from a {@link CharStream}.
         */
        public TimeTable
        parse() throws ParseException {
            return this.parsePatternSequence();
        }

        /**
         * <pre>
         * pattern-sequence :=
         *     pattern { ',' pattern }
         * </pre>
         */
        private TimeTable
        parsePatternSequence() throws ParseException {
            TimeTable tt = this.parsePattern();
            while (this.peekRead(",")) {
                final TimeTable lhs = tt, rhs = this.parsePattern();
                tt = new TimeTable() {

                    @Override public Date
                    next(Date previous) {
                        Date d1 = lhs.next(previous);
                        Date d2 = rhs.next(previous);
                        return d1.compareTo(d2) < 0 ? d1 : d2;
                    }

                    @Override public String
                    toString() {
                        return lhs + "," + rhs;
                    }
                };
            }
            this.eoi();
            return tt;
        }

        /**
         * <pre>
         * pattern :=
         *     [ date-pattern ] [ day-of-week-pattern ] [ time-of-day-pattern ]
         * </pre>
         */
        private TimeTable
        parsePattern() throws ParseException {
            final IntegerPattern year, month, dayOfMonth, dayOfWeek, hour, minute, second;

            PARSE: {
                if (this.peek(TokenType.BEFORE_TIME_PATTERN) != null || this.peek(TokenType.IDENTIFIER) != null) {
                    year = (month = (dayOfMonth = IntegerPattern.ANY));
                } else {

                    // Date pattern.
                    year = this.parseYearPattern();
                    this.read("-");
                    month = this.parseMonthPattern();
                    this.read("-");
                    dayOfMonth = this.parseDayOfMonthPattern();

                    if (this.peek() == null) {
                        dayOfWeek = IntegerPattern.ANY;
                        hour      = (minute = (second = IntegerPattern.ZERO));
                        break PARSE;
                    }
                    this.read(TokenType.SPACE);
                }

                // Day-of-week.
                if (this.peek(TokenType.IDENTIFIER) != null) {
                    dayOfWeek = this.parseDayOfWeekPattern();

                    if (this.peek() == null) {
                        hour = (minute = (second = IntegerPattern.ZERO));
                        break PARSE;
                    }
                    this.read(TokenType.SPACE);
                } else {
                    dayOfWeek = IntegerPattern.ANY;
                }

                // Time-of-day.
                this.read(TokenType.BEFORE_TIME_PATTERN);
                hour = this.parseHourPattern();
                this.read(":");
                minute = this.parseMinutePattern();
                if (this.peekRead(":")) {
                    second = this.parseSecondPattern();
                } else {
                    second = IntegerPattern.ZERO;
                }
            }

            return new TimeTable() {

                @Override public Date
                next(Date previous) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(previous);
                    cal.add(Calendar.SECOND, 1);

                    {
                        int ss = second.getConstant();
                        if (ss == -1) {
                            while (!second.matches(cal.get(Calendar.SECOND))) cal.add(Calendar.SECOND, 1);
                        } else {
                            if (cal.get(Calendar.SECOND) > ss) cal.add(Calendar.MINUTE, 1);
                            cal.set(Calendar.SECOND, ss);
                        }
                    }

                    {
                        int mm = minute.getConstant();
                        if (mm == -1) {
                            while (!minute.matches(cal.get(Calendar.MINUTE))) cal.add(Calendar.MINUTE, 1);
                        } else {
                            if (cal.get(Calendar.MINUTE) > mm) cal.add(Calendar.HOUR_OF_DAY, 1);
                            cal.set(Calendar.MINUTE, mm);
                        }
                    }

                    {
                        int hh = hour.getConstant();
                        if (hh == -1) {
                            while (!hour.matches(cal.get(Calendar.HOUR_OF_DAY))) cal.add(Calendar.HOUR_OF_DAY, 1);
                        } else {
                            if (cal.get(Calendar.HOUR_OF_DAY) > hh) cal.add(Calendar.HOUR_OF_DAY, 1);
                            cal.set(Calendar.HOUR_OF_DAY, hh);
                        }
                    }

                    {
                        int yy = year.getConstant();
                        if (yy == -1) {
                            if (!year.matches(cal.get(Calendar.YEAR))) {
                                cal.set(Calendar.DAY_OF_MONTH, 1);
                                cal.set(Calendar.MONTH, Calendar.JANUARY);
                                do {
                                    cal.add(Calendar.YEAR, 1);
                                } while (!year.matches(cal.get(Calendar.YEAR)));
                            }
                        } else {
                            if (cal.get(Calendar.YEAR) > yy) return TimeTable.MAX_DATE;
                            if (yy > cal.get(Calendar.YEAR)) {
                                cal.set(Calendar.DAY_OF_MONTH, 1);
                                cal.set(Calendar.MONTH, Calendar.JANUARY);
                                cal.set(Calendar.YEAR, yy);
                            }
                        }
                    }

                    {
                        int mo = month.getConstant();
                        if (mo == -1) {
                            if (!month.matches(cal.get(Calendar.MONTH) + 1)) {
                                cal.set(Calendar.DAY_OF_MONTH, 1);
                                do {
                                    cal.add(Calendar.MONTH, 1);
                                } while (
                                    !month.matches(cal.get(Calendar.MONTH) + 1)
                                    || !year.matches(cal.get(Calendar.YEAR))
                                );
                            }
                        } else {
                            if (cal.get(Calendar.MONTH) + 1 > mo) {
                                do {
                                    cal.add(Calendar.YEAR, 1);
                                } while (!year.matches(cal.get(Calendar.YEAR)));
                            }
                            cal.set(Calendar.MONTH, mo - 1);
                        }
                    }

                    while (
                        !dayOfMonth.matches(cal.get(Calendar.DAY_OF_MONTH))
                        || !dayOfWeek.matches(cal.get(Calendar.DAY_OF_WEEK))
                        || !month.matches(cal.get(Calendar.MONTH) + 1)
                        || !year.matches(cal.get(Calendar.YEAR))
                    ) cal.add(Calendar.DAY_OF_MONTH, 1);

                    return cal.getTime();
                }

                @Override public String
                toString() {
                    StringBuilder sb = new StringBuilder();
                    if (year != IntegerPattern.ANY || month != IntegerPattern.ANY || dayOfMonth != IntegerPattern.ANY) {
                        sb.append(year).append('-').append(month).append('-').append(dayOfMonth);
                    }
                    if (dayOfWeek != IntegerPattern.ANY) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(dayOfWeek);
                    }
                    if (
                        sb.length() == 0
                        || hour.getConstant() != 0
                        || minute.getConstant() != 0
                        || second.getConstant() != 0
                    ) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(hour).append(':').append(minute);
                        if (second.getConstant() != 0) sb.append(':').append(second);
                    }
                    return sb.toString();
                }
            };
        }

        /**
         * <pre>
         * weekday-pattern :=
         *     weekday-range { ',' weekday-range }
         * </pre>
         */
        private IntegerPattern
        parseDayOfWeekPattern() throws ParseException {
            IntegerPattern ip = this.parseWeekdayRange();
            while (this.peekRead(",")) {
                final IntegerPattern lhs = ip, rhs = this.parseWeekdayRange();
                ip = new IntegerPattern() {

                    @Override public boolean
                    matches(int subject) { return lhs.matches(subject) || rhs.matches(subject); }

                    @Override public int
                    getConstant() { return -1; }

                    @Override public String
                    toString() { return lhs + "," + rhs; }
                };
            }
            return ip;
        }

        /**
         * <pre>
         * weekday-range :=
         *     weekday [ '-' weekday ]
         * </pre>
         */
        private IntegerPattern
        parseWeekdayRange() throws ParseException {
            final int from = this.scanWeekday();
            if (!this.peekRead("-")) return new ConstantIntegerPattern(from) {

                @Override public String
                toString() { return Parser.WEEKDAY_NAMES[from]; }
            };
            final int to = this.scanWeekday();
            return new RangeIntegerPattern(from, to) {

                @Override public String
                toString() { return Parser.WEEKDAY_NAMES[from] + '-' + Parser.WEEKDAY_NAMES[to]; }
            };
        }

        private IntegerPattern
        parseYearPattern() throws ParseException {
            return this.parseIntegerPattern(0, 3000, false);
        }

        private IntegerPattern
        parseMonthPattern() throws ParseException {
            return this.parseIntegerPattern(Calendar.JANUARY + 1, Calendar.DECEMBER + 1, false);
        }

        private IntegerPattern
        parseDayOfMonthPattern() throws ParseException {
            return this.parseIntegerPattern(1, 31, false);
        }

        private IntegerPattern
        parseHourPattern() throws ParseException {
            return this.parseIntegerPattern(0, 23, true);
        }

        private IntegerPattern
        parseMinutePattern() throws ParseException {
            return this.parseIntegerPattern(0, 59, true);
        }

        private IntegerPattern
        parseSecondPattern() throws ParseException {
            return this.parseIntegerPattern(0, 59, true);
        }

        /**
         * <pre>
         * integer-pattern :=
         *     '*' [ '/ integer ]
         *     | '(' integer-range { ',' integer-range } ')'
         *     | integer-range      <= Iff {@code allowUnparenthesizedRange}
         *     | integer            <= Iff {@code !allowUnparenthesizedRange}
         * </pre>
         */
        private IntegerPattern
        parseIntegerPattern(int min, int max, boolean allowUnparenthesizedRange) throws ParseException {

            // '*'
            if (this.peekRead("*")) {
                if (this.peekRead("/")) {
                    return new StepIntegerPattern(IntegerPattern.ANY, this.parseInteger(min, max));
                } else {
                    return IntegerPattern.ANY;
                }
            }

            // '(' integer-range { ',' integer-range } ')'
            if (this.peekRead("(")) {
                IntegerPattern ip = this.parseIntegerRange(min, max);
                while (this.peekRead(",")) {
                    final IntegerPattern lhs = ip, rhs = this.parseIntegerRange(min, max);
                    ip = new IntegerPattern() {

                        @Override public boolean
                        matches(int subject) { return lhs.matches(subject) || rhs.matches(subject); }

                        @Override public int
                        getConstant() { return -1; }

                        @Override public String
                        toString() { return lhs + "," + rhs; }
                    };
                }
                this.read(")");
                final IntegerPattern fip = ip;
                return new IntegerPattern() {

                    @Override public boolean
                    matches(int subject) { return fip.matches(subject); }

                    @Override public int
                    getConstant() { return fip.getConstant(); }

                    @Override public String
                    toString() { return '(' + fip.toString() + ')'; }
                };
            }

            // integer-range
            // integer
            return (
                allowUnparenthesizedRange
                ? this.parseIntegerRange(min, max)
                : new ConstantIntegerPattern(this.parseInteger(min, max))
            );
        }

        /**
         * <pre>
         *   integer-range :=
         *       integer [ '-' integer [ '/' integer ] ]
         * </pre>
         */
        private IntegerPattern
        parseIntegerRange(int min, int max) throws ParseException {
            int from = this.parseInteger(min, max);

            if (this.peekRead("-")) {
                int            to     = this.parseInteger(min, max);
                IntegerPattern result = new RangeIntegerPattern(from, to);
                if (this.peekRead("/")) {
                    result = new StepIntegerPattern(result, this.parseInteger(min, max));
                }
                return result;
            }

            return new ConstantIntegerPattern(from);
        }

        /**
         * @throws ParseException  Iff not '{@code min &lt;= x &lt;= max}'
         */
        private int
        parseInteger(int min, int max) throws ParseException {
            int result = this.scanInteger();
            if (result < min) {
                throw new ParseException("Value '" + result + "' is too small - must be '" + min + "' or greater");
            }
            if (result > max) {
                throw new ParseException("Value '" + result + "' is too large - mus be '" + max + "' or less");
            }
            return result;
        }

        private int
        scanInteger() throws ParseException {
            return Integer.parseInt(this.read(TokenType.INTEGER));
        }

        /**
         * <pre>
         * weekday :=
         *     'Mon' | 'Tue' | 'Wed' | 'Thu' | 'Fri' | 'Sat' | 'Sun'
         */
        private int
        scanWeekday() throws ParseException {
            String  word = this.read(TokenType.IDENTIFIER);
            Integer wd   = Parser.WEEKDAY_DISPLAY_NAMES.get(word);
            if (wd == null) {
                throw new ParseException(
                    "Invalid weekday '"
                    + word
                    + "' - valid weekdays are "
                    + Parser.WEEKDAY_DISPLAY_NAMES
                );
            }
            return wd;
        }

        // 'Calendar.getInstance().getDisplayNames(DAY_OF_WEEK, Calendar.ALL_STYLES, Locale.US)' would be nice to use,
        // but is only available in JRE 1.6+.
        private static final Map<String, Integer> WEEKDAY_DISPLAY_NAMES;
        static {
            Map<String, Integer> m = new HashMap<String, Integer>();
            m.put("Sun", Calendar.SUNDAY);
            m.put("Mon", Calendar.MONDAY);
            m.put("Tue", Calendar.TUESDAY);
            m.put("Wed", Calendar.WEDNESDAY);
            m.put("Thu", Calendar.THURSDAY);
            m.put("Fri", Calendar.FRIDAY);
            m.put("Sat", Calendar.SATURDAY);
            WEEKDAY_DISPLAY_NAMES = Collections.unmodifiableMap(m);
        }
        protected static final String[] WEEKDAY_NAMES = { null, "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

        interface IntegerPattern {

            boolean
            matches(int subject);

            int
            getConstant();

            IntegerPattern ANY = new IntegerPattern() {

                @Override public boolean
                matches(int subject) { return true; }

                @Override public int
                getConstant() { return -1; }

                @Override public String
                toString() { return "*"; }
            };
            IntegerPattern ZERO = new ConstantIntegerPattern(0);
        }

        static
        class ConstantIntegerPattern implements IntegerPattern {

            private final int constantValue;

            ConstantIntegerPattern(int constantValue) { this.constantValue = constantValue; }

            @Override public boolean
            matches(int subject) { return subject == this.constantValue; }

            @Override public int
            getConstant() { return this.constantValue; }

            @Override public String
            toString() { return String.valueOf(this.constantValue); }
        }

        static
        class RangeIntegerPattern implements IntegerPattern {

            private final int from, to;

            RangeIntegerPattern(int from, int to) {
                this.from = from;
                this.to   = to;
            }

            @Override public boolean
            matches(int subject) {
                return (
                    this.from <= this.to
                    ? subject >= this.from && subject <= this.to
                    : subject >= this.to || subject <= this.from
                );
            }
            @Override public int
            getConstant() { return -1; }

            @Override public String
            toString() { return this.from + "-" + this.to; }
        }

        static
        class StepIntegerPattern implements IntegerPattern {

            private final IntegerPattern delegate;
            private final int            step;

            StepIntegerPattern(IntegerPattern delegate, int step) {
                this.delegate = delegate;
                this.step     = step;
            }

            @Override public boolean
            matches(int subject) { return this.delegate.matches(subject) && subject % this.step == 0; }

            @Override public int
            getConstant() { return -1; }

            @Override public String
            toString() { return this.delegate + "/" + this.step; }
        }
    }

    @Override public abstract String
    toString();
}
