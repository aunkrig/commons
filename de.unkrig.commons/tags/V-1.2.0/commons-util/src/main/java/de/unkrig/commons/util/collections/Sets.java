
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.commons.util.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.unkrig.commons.lang.AssertionUtil;

/**
 * Utility methods related to {@link Set}s.
 */
public final
class Sets {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Sets() {}

    /**
     * @return A list of (immutable) sets, starting with the empty set, followed by a set containing the <em>last</em>
     *         element of the <var>set</var>, followed by all other subsets, and ending with (an immutable copy of)
     *         the <var>set</var> itself
     */
    public static <E> List<Set<E>>
    allSubsets(Set<E> set) {
        return Sets.allSubsets(set.iterator());
    }

    private static <E> List<Set<E>>
    allSubsets(Iterator<E> it) {

        if (!it.hasNext()) {
            List<Set<E>> result = new ArrayList<Set<E>>();
            result.add(Collections.<E>emptySet());
            return result;
        }

        E firstElement = it.next();

        List<Set<E>> allSubsets = Sets.allSubsets(it);

        List<Set<E>> tmp = new ArrayList<Set<E>>(allSubsets.size());
        for (Set<E> ss : allSubsets) {
            if (ss.isEmpty()) {
                tmp.add(Collections.singleton(firstElement));
            } else {
                Set<E> tmp2 = new HashSet<E>(ss);
                tmp2.add(firstElement);
                tmp.add(tmp2);
            }
        }

        allSubsets.addAll(tmp);

        return allSubsets;
    }
}
