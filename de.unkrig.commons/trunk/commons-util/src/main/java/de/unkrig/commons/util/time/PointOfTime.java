
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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Extends {@link Date} with a more powerful string-arg constructor.
 */
public
class PointOfTime implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Non-leap milliseconds since 1970-01-01 00:00:00 UTC. */
    private final long ms;

    /** The current time, a.k.a 'now'. */
    public
    PointOfTime() { this.ms = System.currentTimeMillis(); }

    /**
     * Accepts point-of-time specifications in any of the following formats:
     * <ul>
     *   <li>{@code 1999-12-31T23:59:59} (the "{@code T}" separates the date from the time-of-day)</li>
     *   <li>{@code 1999-12-31T23:59}</li>
     *   <li>{@code 1999-12-31} (midnight on the specified date)</li>
     * </ul>
     */
    public
    PointOfTime(String s) throws ParseException { this.ms = PointOfTime.parse(s); }

    /** @param ms The number of non-leap milliseconds since 1970-01-01 00:00:00 UTC */
    public
    PointOfTime(long ms) { this.ms = ms; }

    /** @return The number of non-leap milliseconds since 1970-01-01 00:00:00 UTC */
    public long
    milliseconds() { return this.ms; }

    @Override public String
    toString() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(this.ms); }

    /** @return The given number of <var>seconds</var> after this {@link PointOfTime} */
    public PointOfTime
    add(double seconds) { return new PointOfTime(this.ms + (long) (1000.0 * seconds)); }

    /** @return The given <var>duration</var> after this {@link PointOfTime} */
    public PointOfTime
    add(Duration duration) { return new PointOfTime(this.ms + duration.milliseconds()); }

    /** @return The length of the interval from <var>other</var> to {@code this} */
    public Duration
    subtract(PointOfTime other) { return new Duration(this.ms - other.ms); }

    private static long
    parse(String s) throws ParseException {

        try { return DateFormat.getInstance().parse(s).getTime();                      } catch (ParseException pe) {}
        try { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s).getTime(); } catch (ParseException pe) {}
        try { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(s).getTime();    } catch (ParseException pe) {}
        try { return new SimpleDateFormat("yyyy-MM-dd").parse(s).getTime();            } catch (ParseException pe) {}

        throw new ParseException("Cannot parse '" + s + "' to a valid date and/or time", 0);
    }
}
