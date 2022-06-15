
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

package de.unkrig.commons.util;

import static de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality.EXACTLY_ONE;

import java.lang.reflect.Method;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.util.annotation.CommandLineOption;
import de.unkrig.commons.util.annotation.CommandLineOptionGroup;

/**
 * Superclass for all the exceptions that the command line options parser may throw.
 *
 * @see CommandLineOptionException.RequiredOptionMissing
 * @see CommandLineOptionException.ArgumentConversionFailed
 * @see CommandLineOptionException.ConflictingOptions
 * @see CommandLineOptionException.DuplicateOption
 * @see CommandLineOptionException.OptionArgumentMissing
 * @see CommandLineOptionException.RequiredOptionGroupMissing
 * @see CommandLineOptionException.UnrecognizedOption
 * @see CommandLineOptions#parse(String[], Object)
 */
public abstract
class CommandLineOptionException extends Exception {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final long serialVersionUID = 1L;

    private static String
    join(String[] optionNames) {

        switch (optionNames.length) {

        case 0:
            throw new AssertionError();

        case 1:
            return '"' + optionNames[0] + '"';

        default:
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0;;) {
                sb.append(optionNames[i++]);
                if (i == optionNames.length) return sb.append('"').toString();
                sb.append(i == optionNames.length - 1 ? "\" and \"" : "\", \"");
            }
        }
    }

    public
    CommandLineOptionException(String message) { super(message); }

    public
    CommandLineOptionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * A command line argument was encountered that appears to be an option (because it starts with "-"), but its name
     * is not recognized.
     */
    public static
    class UnrecognizedOption extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final String optionName;

        protected
        UnrecognizedOption(String optionName) {
            super("Unrecognized command line option \"" + optionName + "\"");

            this.optionName = optionName;
        }

        public String getOptionName() { return this.optionName; }
    }

    /**
     * A command line option has cardinality {@link
     * de.unkrig.commons.util.annotation.CommandLineOption.Cardinality#MANDATORY MANDATORY} or {@link
     * de.unkrig.commons.util.annotation.CommandLineOption.Cardinality#ONCE_OR_MORE ONCE_OR_MORE}, but is missing on
     * the command line.
     *
     * @see CommandLineOption#name()
     */
    public static
    class RequiredOptionMissing extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final Method   option;
        private final String[] optionNames;

        protected
        RequiredOptionMissing(Method option, String[] optionNames) {
            super((
                optionNames.length == 1
                ? "Required command line option \"" + optionNames[0] + "\" is missing"
                : (
                    "Exactly one of command line options "
                    + CommandLineOptionException.join(optionNames)
                    + " must be given"
                )
            ));

            this.option      = option;
            this.optionNames = optionNames;
        }

        public Method
        getOption() { return this.option; }

        /**
         * @return All names of the option
         */
        public String[]
        getOptionNames() { return this.optionNames; }
    }

    /**
     * A command line option group has cardinality {@link
     * de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality#EXACTLY_ONE EXACTLY_ONE} or {@link
     * de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality#ONE_OR_MORE ONE_OR_MORE}, but is missing on
     * the command line.
     *
     * @see CommandLineOption#name()
     */
    public static
    class RequiredOptionGroupMissing extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final Class<?> optionGroup;
        private final String[] optionNames;

        protected
        RequiredOptionGroupMissing(Class<?> optionGroup, String[] optionNames) {
            super((
                optionGroup.getAnnotation(CommandLineOptionGroup.class).cardinality() == EXACTLY_ONE
                ? "Exactly one of " + CommandLineOptionException.join(optionNames) + " must be specified"
                : "One or more of " + CommandLineOptionException.join(optionNames) + " must be specified"
            ));

            this.optionGroup = optionGroup;
            this.optionNames = optionNames;
        }

        public Class<?>
        getOptionGroup() { return this.optionGroup; }

        /**
         * @return All names of all options that are members of the group
         */
        public String[] getOptionNames() { return this.optionNames; }
    }

    /**
     * A command line option requires one or more arguments, but the command line arguments are exhausted before all
     * required option arguments were parsed.
     */
    public static
    class OptionArgumentMissing extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final Method option;
        private final String optionName;
        private final int    argumentIndex;

        /**
         * @param argumentIndex 0=First method argument, ...
         */
        protected
        OptionArgumentMissing(Method option, String optionName, int argumentIndex) {
            super("Argument #" + (argumentIndex + 1) + " for command line option \"" + optionName + "\" is missing");

            this.option        = option;
            this.optionName    = optionName;
            this.argumentIndex = argumentIndex;
        }

        public Method getOption()        { return this.option;        }
        public String getOptionName()    { return this.optionName;    }
        public int    getArgumentIndex() { return this.argumentIndex; }
    }

    /**
     * A command line option with cardinality {@link
     * de.unkrig.commons.util.annotation.CommandLineOption.Cardinality#OPTIONAL OPTIONAL} or {@link
     * de.unkrig.commons.util.annotation.CommandLineOption.Cardinality#MANDATORY MANDATORY} appeared more than
     * once.
     */
    public static
    class DuplicateOption extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final Method   option;
        private final String   optionName;
        private final String[] optionNames;

        protected
        DuplicateOption(Method option, String optionName, String[] optionNames) {
            super("Option \"" + optionName + "\" must appear at most once");

            this.option      = option;
            this.optionName  = optionName;
            this.optionNames = optionNames;
        }

        public Method
        getOption() { return this.option; }

        /**
         * @return The actual name that appeared on the command line
         */
        public String
        getOptionName() { return this.optionName; }

        /**
         * @return All names that identify the option
         */
        public String[]
        getOptionNames() { return this.optionNames; }
    }

    /**
     * A command line option group has cardinality {@link
     * de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality#EXACTLY_ONE EXACTLY_ONE}, and there are
     * more than one options of the group on the command line.
     */
    public static
    class ConflictingOptions extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final Class<?> optionGroup;
        private final Method   option;
        private final String   optionName;

        protected
        ConflictingOptions(Class<?> optionGroup, Method option, String optionName) {
            super("Option \"" + optionName + "\" is exclusive with a preceding option");

            this.optionGroup = optionGroup;
            this.option      = option;
            this.optionName  = optionName;
        }

        public Class<?> getOptionGroup() { return this.optionGroup; }
        public Method   getOption()      { return this.option;      }
        public String   getOptionName()  { return this.optionName;  }
    }

    /**
     * A command line option argument could not be converted to the required type.
     */
    public static
    class ArgumentConversionFailed extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        private final String   argument;
        private final Class<?> targetType;

        protected
        ArgumentConversionFailed(String argument, Class<?> targetType, Throwable cause) {
            super(
                (
                    "Converting option argument \""
                    + argument
                    + "\" to \""
                    + targetType.getSimpleName()
                    + "\": "
                    + cause.getMessage()
                ),
                cause
            );

            this.argument   = argument;
            this.targetType = targetType;
        }

        public String    getArgument()   { return this.argument;   }
        public Class<?>  getTargetType() { return this.targetType; }
    }

    /**
     * A command line option could not be processed.
     */
    public static
    class OptionProcessingException extends CommandLineOptionException {

        private static final long serialVersionUID = 1L;

        protected
        OptionProcessingException(String optionName, Throwable cause) {
            super(
                (
                    "Processing option \""
                    + optionName
                    + "\": "
                    + cause.toString()
                ),
                cause
            );
        }
    }
}
