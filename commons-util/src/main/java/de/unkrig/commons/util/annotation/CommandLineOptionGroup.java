
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2016, Arno Unkrig
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

package de.unkrig.commons.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.unkrig.commons.util.CommandLineOptions;

/**
 * Indicates that the annotated class or interfaces represents a "command line option group".
 *
 * @see CommandLineOptions#parse(String[], Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public
@interface CommandLineOptionGroup {

    /**
     * @see CommandLineOptionGroup#cardinality()
     */
    enum Cardinality {

        /**
         * Indicates that zero or one of the options of the group must appear on the command line.
         */
        ZERO_OR_ONE,

        /**
         * Indicates that exactly one of the options of the group must appear on the command line.
         */
        EXACTLY_ONE,

        /**
         * Indicates that one or more of the options of the group must appear on the command line.
         */
        ONE_OR_MORE,
    }

    /**
     * Configures how many of the command line option group members may appear on the command line.
     * <dl>
     *   <dt>{@link Cardinality#ZERO_OR_ONE} (the default):</dt><dd>Zero or one</dd>
     *   <dt>{@link Cardinality#EXACTLY_ONE}:</dt><dd>Exactly one</dd>
     *   <dt>{@link Cardinality#ONE_OR_MORE}:</dt><dd>Onc or more</dd>
     * </dl>
     */
    Cardinality cardinality() default Cardinality.ZERO_OR_ONE;
}
