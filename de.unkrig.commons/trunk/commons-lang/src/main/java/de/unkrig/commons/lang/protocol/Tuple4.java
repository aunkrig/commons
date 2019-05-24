
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

package de.unkrig.commons.lang.protocol;

import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A container for four elements.
 *
 * @param <T1> Type of the first element
 * @param <T2> Type of the second element
 * @param <T3> Type of the third element
 * @param <T4> Type of the fourth element
 */
public final
class Tuple4<T1, T2, T3, T4> {

    /**
     * The first element of the tuple.
     */
    public final T1 first;

    /**
     * The second element of the tuple.
     */
    public final T2 second;

    /**
     * The third element of the tuple.
     */
    public final T3 third;

    /**
     * The third element of the tuple.
     */
    public final T4 fourth;

    public
    Tuple4(T1 first, T2 second, T3 third, T4 fourth) {
        this.first  = first;
        this.second = second;
        this.third  = third;
        this.fourth = fourth;
    }

    @Override public boolean
    equals(@Nullable Object obj) {
        if (!(obj instanceof Tuple4)) return false;
        @SuppressWarnings("unchecked") Tuple4<T1, T2, T3, T4> that = (Tuple4<T1, T2, T3, T4>) obj;
        return (
            ObjectUtil.equals(this.first, that.first)
            && ObjectUtil.equals(this.second, that.second)
            && ObjectUtil.equals(this.third, that.third)
            && ObjectUtil.equals(this.fourth, that.fourth)
        );
    }

    @Override public int
    hashCode() {
        return (
            ObjectUtil.hashCode(this.first)
            + ObjectUtil.hashCode(this.second)
            + ObjectUtil.hashCode(this.third)
            + ObjectUtil.hashCode(this.fourth)
        );
    }

    @Override public String
    toString() {
        return this.first + ":" + this.second + ":" + this.third + ":" + this.fourth;
    }
}
