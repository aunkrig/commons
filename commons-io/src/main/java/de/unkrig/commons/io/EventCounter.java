
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2019, Arno Unkrig
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

package de.unkrig.commons.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import de.unkrig.commons.lang.Comparators;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Counts named (and optionally parameterized) events.
 */
public abstract
class EventCounter {

    protected Map<String, AtomicLong>         events       = new HashMap<String, AtomicLong>();
    protected List<Entry<String, AtomicLong>> sortedEvents = new ArrayList<Map.Entry<String, AtomicLong>>();

    public void
    countEvent(String eventName) { this.countEvent(eventName, null); }

    public void
    countEvent(String eventName, @Nullable Object arg) {

        if (arg == null) {

            // Argumentless events are solely COUNTED.
            this.increment(eventName, 1);
        } else
        if (arg instanceof Number) {

            // Numeric arguments are also SUMMED UP in a separate counter.
            this.increment(eventName, 1);
            this.increment(eventName + "_", ((Number) arg).longValue());
        } else
        {

            // Non-numeric arguments are also counter PER ARGUMENT VALUE.
            this.increment(eventName, 1);
            this.increment(eventName + ":" + arg, 1);
        }
    }

    private void
    increment(String key, long delta) {

        AtomicLong value = this.events.get(key);
        if (value == null) {
            synchronized (this) {
                Map<String, AtomicLong> tmp = new HashMap<String, AtomicLong>(this.events);
                tmp.put(key, (value = new AtomicLong(delta)));

                List<Entry<String, AtomicLong>> tmp2 = new ArrayList<Map.Entry<String, AtomicLong>>();
                tmp2.addAll(tmp.entrySet());
                Collections.sort(
                    tmp2,
                    Comparators.keyComparator(
                        Comparators.nullSafeComparator(Comparators.<String>naturalOrderComparator())
                    )
                );

                this.events       = tmp;
                this.sortedEvents = tmp2;
            }
        } else {
            value.addAndGet(delta);
        }
    }
}
