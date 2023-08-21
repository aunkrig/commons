
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

package de.unkrig.commons.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.unkrig.commons.text.Notations.Phrase;
import de.unkrig.commons.util.CommandLineOptions;

/**
 * Indicates that the annotated method maps to a "command line option".
 *
 * @see CommandLineOption#name()
 * @see CommandLineOption#cardinality()
 * @see CommandLineOptions#getMethodForOption(String, Class) The algorithm that determines the command line option(s)
 *                                                           for this method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public
@interface CommandLineOption {

    /**
     * The name(s) that identify the option on the command line.
     * <p>
     *   If the name starts with "--", then only the double-hyphen variant is recognized; if it starts with "-", then
     *   only the single-hyphen is recognized; otherwise, <em>both</em> variants are recognized.
     * </p>
     * <p>
     *   If this element is not specified, then the name is derived from the method name as follows: An optional prefix
     *   "set" or "add" is removed. The rest of the method name is converted from camel case into {@link
     *   Phrase#toLowerCaseHyphenated() lower-case-hyphenated} notation.
     *   <br />
     *   Example: If the method name is "{@code setFooBar}", then the following command line options are
     *   recognized: {@code "-foo-bar"} and {@code "--foo-bar"}.
     * </p>
     */
    String[] name() default {};

    /**
     * @see CommandLineOption#cardinality()
     */
    enum Cardinality {

        /**
         * Indicates that the annotated command line option must appear zero times or once.
         */
        OPTIONAL,

        /**
         * Indicates that the annotated command line option must appear exactly once.
         */
        MANDATORY,

        /**
         * Indicates that the annotated command line option must appear once or multiple times.
         */
        ONCE_OR_MORE,

        /**
         * Indicates that the annotated command line option may appear any number of times.
         */
        ANY,
    }

    /**
     * Configures how often this option may appear on the command line.
     * <dl>
     *   <dt>{@link Cardinality#OPTIONAL} (the default):</dt><dd>Zero times or once</dd>
     *   <dt>{@link Cardinality#MANDATORY}:</dt><dd>Exactly once</dd>
     *   <dt>{@link Cardinality#ONCE_OR_MORE}:</dt><dd>Once or multiple times</dd>
     *   <dt>{@link Cardinality#ANY}:</dt><dd>Any number of times, including 0 and 1</dd>
     * </dl>
     */
    Cardinality cardinality() default Cardinality.OPTIONAL;

    /**
     * Indicates that this command line option is related with other command line options, e.g. they are mutually
     * exclusive.
     */
    Class<?>[] group() default {};
}
