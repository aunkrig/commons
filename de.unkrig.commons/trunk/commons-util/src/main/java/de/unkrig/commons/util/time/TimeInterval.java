
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

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A time interval has an optional beginning, an optional duration and an optional ending.
 * <p>
 * An 'undefined time interval' has neither a beginning, duration nor ending.
 * <p>
 * A 'defined time interval' has a beginning, a duration and an ending, which are consistent with each other, i.e.
 * beginning + duration = ending.
 * <p>
 * A 'half-defined' time interval has
 * <ul>
 *   <li>A beginning, but no duration and ending, or
 *   <li>A duration, but no beginning and ending, or
 *   <li>An ending, but no beginning and duration
 * </ul>
 */
public final
class TimeInterval {

    @Nullable private PointOfTime beginning;
    @Nullable private Duration    duration;
    @Nullable private PointOfTime ending;

    public
    TimeInterval(TimeInterval other) {
        this.beginning = other.beginning;
        this.duration  = other.duration;
        this.ending    = other.ending;
    }

    /** Constructs an uninitialized {@link TimeInterval}. */
    public
    TimeInterval() {}

    /**
     * Changes the beginning of this interval.
     *
     * @return                       This object
     * @throws IllegalStateException The <var>beginning</var> is inconsistent with this object's duration and ending,
     *                               i.e. all three are non-{@code null}, and beginning + duration != ending
     */
    public TimeInterval
    setBeginning(@Nullable PointOfTime beginning) { this.beginning = beginning; this.checkConsistency(); return this; }

    /**
     * Changes the duration of this interval.
     *
     * @return                       This object
     * @throws IllegalStateException The <var>duration</var> is inconsistent with this object's beginning and ending,
     *                               i.e. all three are non-{@code null}, and beginning + duration != ending
     */
    public TimeInterval
    setDuration(@Nullable Duration duration) { this.duration = duration; this.checkConsistency(); return this; }

    /**
     * Changes the ending of this interval.
     *
     * @return                       This object
     * @throws IllegalStateException The <var>ending</var> is inconsistent with this object's beginning and duration,
     *                               i.e. all three are non-{@code null}, and beginning + duration != ending
     */
    public TimeInterval
    setEnding(@Nullable PointOfTime ending) { this.ending = ending; this.checkConsistency(); return this; }

    /** The beginning of this time interval */
    @Nullable public PointOfTime
    getBeginning() {
        PointOfTime beginning = this.beginning;
        Duration    duration  = this.duration;
        PointOfTime ending    = this.ending;

        return (
            beginning == null && duration != null && ending != null
            ? new PointOfTime(ending.milliseconds() - duration.milliseconds())
            :  this.beginning
        );
    }

    /** The duration of this time interval */
    @Nullable public Duration
    getDuration() {
        PointOfTime beginning = this.beginning;
        Duration    duration  = this.duration;
        PointOfTime ending    = this.ending;

        return (
            beginning != null && duration == null && ending != null
            ? new Duration(ending.milliseconds() - beginning.milliseconds())
            : this.duration
        );
    }

    /** The ending of this time interval */
    @Nullable public PointOfTime
    getEnding() {
        PointOfTime beginning = this.beginning;
        Duration    duration  = this.duration;
        PointOfTime ending    = this.ending;

        return (
            beginning != null && duration != null && ending == null
            ? new PointOfTime(beginning.milliseconds() + duration.milliseconds())
            : this.ending
        );
    }

    private void
    checkConsistency() {
        PointOfTime beginning = this.beginning;
        Duration    duration  = this.duration;
        PointOfTime ending    = this.ending;

        // Iff all three fields are set, verify that their values are consistent.
        if (beginning != null && duration != null && ending != null) {
            if (beginning.milliseconds() + duration.milliseconds() != ending.milliseconds()) {
                throw new IllegalStateException();
            }
        }
    }
}
