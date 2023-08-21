
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

package de.unkrig.commons.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerUtil.FromArrayProducer;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.Notations;
import de.unkrig.commons.text.Notations.Phrase;
import de.unkrig.commons.text.StringStream;
import de.unkrig.commons.text.StringStream.UnexpectedElementException;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.Pattern2;
import de.unkrig.commons.text.pattern.PatternUtil;
import de.unkrig.commons.util.CommandLineOptionException.ArgumentConversionFailed;
import de.unkrig.commons.util.CommandLineOptionException.ConflictingOptions;
import de.unkrig.commons.util.CommandLineOptionException.DuplicateOption;
import de.unkrig.commons.util.CommandLineOptionException.OptionProcessingException;
import de.unkrig.commons.util.CommandLineOptionException.OptionArgumentMissing;
import de.unkrig.commons.util.CommandLineOptionException.RequiredOptionGroupMissing;
import de.unkrig.commons.util.CommandLineOptionException.RequiredOptionMissing;
import de.unkrig.commons.util.CommandLineOptionException.UnrecognizedOption;
import de.unkrig.commons.util.annotation.CommandLineOption;
import de.unkrig.commons.util.annotation.CommandLineOptionGroup;
import de.unkrig.commons.util.annotation.RegexFlags;

/**
 * Parses "command line options" from the <var>args</var> of your {@code main()} method and configures a Java bean
 * accordingly.
 *
 * @see #parse(String[], Object)
 */
public final
class CommandLineOptions {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * Detects a "command line option", i.e. a "-" followed by at least one character.
     */
    private static final Pattern REGEX_OPTION = Pattern.compile("-.+");

    /**
     * Detects a "compact command lime option", like "-lar" for the UNIX "ls" command.
     * <dl>
     *   <dt>{@code group(1)}:<dt><dd>The <em>first</em> option letter (for "-lar": "l")</dd>
     *   <dt>{@code group(2)}:<dt><dd>The <em>following</em> option letters (for "-lar": "ar")</dd>
     * </dl>
     */
    private static final Pattern REGEX_COMPACT_OPTIONS = Pattern.compile("-([^\\-])(.*)");

    private CommandLineOptions() {}

    /**
     * Invokes methods of the <var>target</var> object based on the <var>args</var>.
     * <p>
     *   All public methods of the target (including those declared by superclasses) are regarded candidates iff
     *   they are annotated with the {@link de.unkrig.commons.util.annotation.CommandLineOption} annotation.
     * </p>
     * <p>
     *   The possible "names" of the command line option are derived from the {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption#name() name} element of the {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption} annotation, or, if that is missing, from the method name
     *   (see examples below).
     * </p>
     * <p>
     *   When an element of <var>args</var> equals such a name, then the following elements in <var>args</var> are
     *   converted to match the parameter types of the method (see example code below). After that, the method is
     *   invoked with the arguments.
     * </p>
     * <p>
     *   Parsing terminates iff either
     * </p>
     * <ul>
     *   <li>The <var>args</var> are exhausted</li>
     *   <li>The special arg {@code "--"} is reached (which is consumed)</li>
     *   <li>The special arg {@code "-"} is reached (which is <em>not</em>consumed)</li>
     *   <li>A "normal" command line arguments appears, i.e. one that does not start with "-"</li>
     * </ul>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *   public
     *   class MyMain {
     *
     *       // ...
     *
     *       // This one maps to "-font-size &lt;x>" and "--font-size &lt;x>".
     *       &#64;CommandLineOption public static void
     *       setFontSize(double size) { System.out.println("fontSize=" + size); }
     *
     *       // This one maps to "-help" and "--help".
     *       &#64;CommandLineOption public static void
     *       help() {}
     *
     *       // This one maps to "-alpha" and "--alpha".
     *       &#64;CommandLineOption(name = "alpha") public static void
     *       method1() {}
     *
     *       // This one maps to "-beta" and "--gamma".
     *       &#64;CommandLineOption(name = { "-beta", "--gamma" }) public static void
     *       method2() {}
     *
     *       // This one maps to "-foo &lt;a> &lt;b> &lt;c>". (A single dashes may be used instead of the double dash.)
     *       &#64;CommandLineOption public static void
     *       foo(int one, String two, java.util.Pattern three) {}
     *
     *       // This one maps to "--person [ --name &lt;>name> ] [ --age &lt;age> ]". (Single dashes may be used
     *       // instead of the double dashes.)
     *       &#64;CommandLineOption public static void
     *       addPerson(Person p) {}
     *
     *       public static
     *       class Person {
     *           public void setName(String name) {}
     *           public void setAge(int age) {}
     *       }
     *   }
     *
     *   ...
     *
     *   final MyMain main = new MyMain();
     *
     *   String[] args = { "--font-size", "17.5", "a", "b" };
     *   System.out.println(Arrays.toString(args));           // Prints "[--font-size, 17.5, a, b]".
     *
     *   args = MainBean.parseCommandLineOptions(args, main); // Prints "fontSize=17.5".
     *
     *   System.out.println(Arrays.toString(args);            // Prints "[a, b]".
     * </pre>
     *
     * <h3>Option cardinality</h3>
     *
     * <p>
     *   To enforce that a particular command line option must be given a specific number of times, use the
     *   {@link de.unkrig.commons.util.annotation.CommandLineOption#cardinality() cardinality()} element of the {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption @CommandLineOption} annotation:
     * </p>
     * <pre>
     *   &#64;CommandLineOption(cardinality = CommandLineOption.Cardinality.MANDATORY)
     *   setColor(String color) {
     *       this.color = color;
     *   }
     * </pre>
     * <p>
     *   For this example, it is now guaranteed that {@link #parse(String[], Object)} will invoke the "{@code
     *   setColor()}" method exactly once.
     * </p>
     * <p>
     *   The default value for the command line option cardinality is {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption.Cardinality#OPTIONAL}.
     * </p>
     *
     * <h3>Option groups</h3>
     *
     * <p>
     *   To enforce that a particular number of <em>a set of command line options</em> must be given, use the {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption#group() group()} element of the {@link
     *   de.unkrig.commons.util.annotation.CommandLineOption @CommandLineOption} annotation:
     * </p>
     * <pre>
     *   &#64;CommandLineOption(group = Sources.class)
     *   setFile(File file) {
     *       this.file = file;
     *   }
     *
     *   &#64;CommandLineOption(group = Sources.class)
     *   setStdin() {
     *       this.stdin = true;
     *   }
     *
     *   // This interface solely serves as the "connector" between the related command line options; it is (typically)
     *   // not used otherwise.
     *   &#64;CommandLineOptionGroup(cardinality = CommandLineOptionGroup.Cardinality.EXACTLY_ONE)
     *   interface Sources {}
     * </pre>
     * <p>
     *   For this example, it is now guaranteed that {@link #parse(String[], Object)} will invoke exactly one of the
     *   methods "{@code setFile()}" and "{@code setStdin()}".
     * </p>
     * <p>
     *   The default value for the command line option group cardinality is {@link
     *   de.unkrig.commons.util.annotation.CommandLineOptionGroup.Cardinality#ZERO_OR_ONE ZERO_OR_ONE}.
     * </p>
     *
     * <h3>Argument conversion</h3>
     *
     * <p>
     *   <var>args</var> strings are converted to match the parameter types of the methods as follows:
     * </p>
     * <ul>
     *   <li>
     * </ul>
     *
     * @return                            The <var>args</var>, less the elements that were parsed as command line
     *                                    options
     * @throws CommandLineOptionException An error occurred during the parsing; typically a command-line application
     *                                    would print the message of the exception to STDERR and call "{@code
     *                                    System.exit(1)}"
     */
    public static String[]
    parse(String[] args, Object target) throws CommandLineOptionException {

        StringStream<RuntimeException> ss = new StringStream<RuntimeException>(ProducerUtil.fromArray(args));

        Parser<RuntimeException> p = new Parser<RuntimeException>(target.getClass());

        // Parse all command line options and apply them to the "target".
        p.parseOptions(ss, target);

        // Consume the optional separator "--" between the command line options and the "normal" command line
        // arguments.
        if (!ss.peekRead("--") && ss.peek(CommandLineOptions.REGEX_OPTION)) {
            throw new UnrecognizedOption(AssertionUtil.notNull(ss.group(0)));
        }

        // Return the remaining command line arguments.
        return ss.readRest();
    }

    private static
    class Parser<EX extends Throwable> {

        // CONFIGURATION

        /** Option name => {@link Method} */
        private final Map<String, Method> allOptions = new LinkedHashMap<String, Method>();

        /** The options that must appear at most once. */
        private final Set<Method> singularOptions = new HashSet<Method>();

        /** The options that must appear at least once. */
        private final List<Method> requiredOptions = new ArrayList<Method>();

        /** The option groups with cardinality 1 or less. */
        private final Set<Class<?>> singularOptionGroups = new HashSet<Class<?>>();

        /** The option groups with cardinality 0 or more. */
        private final List<Class<?>> requiredOptionGroups = new ArrayList<Class<?>>();

        /** Maps option methods to the set of groups that each is a member of. */
        private final Map<Method, Set<Class<?>>>
        optionToOptionGroups = new HashMap<Method, Set<Class<?>>>();

        // PARSING STATE

        /** Options that were parsed so far. */
        private final Set<Method> actualOptions = new HashSet<Method>();

        /** Options groups that were parsed so far. */
        private final Set<Class<?>> actualOptionGroups = new HashSet<Class<?>>();

        /**
         * Analyzes the <var>targetClass</var> for any command line option setters.
         *
         * @throws AssertionError A {@link CommandLineOption @CommandLineOption} refers to a type that is <em>not</em>
         *                        annotated with {@link CommandLineOptionGroup @CommandLineOptionGroup}
         * @throws AssertionError Two methods map to the same name
         */
        Parser(Class<?> targetClass) {

            // Identify all command line options that apply to the target class, and populate the "methodCache".
            Method[] methods = targetClass.getMethods();
            Arrays.sort(methods, new Comparator<Method>() {
                @NotNullByDefault(false) @Override public int
                compare(Method m1, Method m2) { return m1.toString().compareTo(m2.toString()); }
            });

            for (Method m : methods) {

                CommandLineOption clo = m.getAnnotation(CommandLineOption.class);
                if (clo == null) continue;

                // Determine the "names" of the command-line option - either from the "name" element of the
                // "@CommandLineOption" annotation, or from the method name.
                String[] names = clo.name();
                if (names.length == 0) {
                    String n = m.getName();
                    if (n.startsWith("set")) {
                        n = n.substring(3);
                    } else
                    if (n.startsWith("add")) {
                        n = n.substring(3);
                    }
                    names = new String[] { Notations.fromCamelCase(n).toLowerCaseHyphenated() };
                }

                for (String name : names) {
                    for (String name2 : (
                        name.startsWith("-")
                        ? new String[] { name }
                        : new String[] { "-" + name, "--" + name }
                    )) {
                        Method prev = this.allOptions.put(name2, m);
                        assert prev == null : "Two methods map to option \"" + name2 + "\": \"" + prev + "\" and \"" + m + "\""; // SUPPRESS CHECKSTYLE LineLength
                    }
                }

                // Process the option cardinality.
                {
                    CommandLineOption.Cardinality c = clo.cardinality();

                    if (c == CommandLineOption.Cardinality.MANDATORY || c == CommandLineOption.Cardinality.OPTIONAL) {
                        this.singularOptions.add(m);
                    }

                    if (
                        c == CommandLineOption.Cardinality.MANDATORY
                        || c == CommandLineOption.Cardinality.ONCE_OR_MORE
                    ) {
                        this.requiredOptions.add(m);
                    }
                }

                // Process the option group cardinality.
                for (Class<?> optionGroup : clo.group()) {
                    CommandLineOptionGroup clog = optionGroup.getAnnotation(CommandLineOptionGroup.class);
                    if (clog == null) {
                        throw new AssertionError(
                            "Option group class \""
                            + optionGroup
                            + "\" lacks the \"@CommandLineOptionGroup\" annotation"
                        );
                    }

                    Set<Class<?>> optionGroups = this.optionToOptionGroups.get(m);
                    if (optionGroups == null) {
                        this.optionToOptionGroups.put(m, (optionGroups = new HashSet<Class<?>>()));
                    }

                    optionGroups.add(optionGroup);

                    CommandLineOptionGroup.Cardinality c = clog.cardinality();
                    if (
                        c == CommandLineOptionGroup.Cardinality.EXACTLY_ONE
                        || c == CommandLineOptionGroup.Cardinality.ZERO_OR_ONE
                    ) this.singularOptionGroups.add(optionGroup);
                    if (
                        c == CommandLineOptionGroup.Cardinality.EXACTLY_ONE
                        || c == CommandLineOptionGroup.Cardinality.ONE_OR_MORE
                    ) this.requiredOptionGroups.add(optionGroup);
                }
            }
        }

        /**
         * Parses tokens on the <var>ss</var> that map to options of the <var>target</var> and are not "--" nor "-".
         *
         * @return                            Whether the next token(s) on <var>ss</var> were parsed as an option of
         *                                    the <var>target</var>
         * @throws CommandLineOptionException An error occurred during the parsing
         * @throws EX                         <var>ss</var> threw an exception
         */
        private void
        parseOptions(StringStream<EX> ss, Object target) throws EX, CommandLineOptionException {

            // Parse as many command line options as possible.
            while (this.parseNextOption(ss, target));

            // Verify that all "required" options were parsed.
            for (Method option : this.requiredOptions) {

                if (!this.actualOptions.contains(option)) {
                    throw new RequiredOptionMissing(option, this.optionNames(option));
                }
            }

            for (Class<?> optionGroup : this.requiredOptionGroups) {

                if (!this.actualOptionGroups.contains(optionGroup)) {
                    throw new RequiredOptionGroupMissing(optionGroup, this.optionNames(optionGroup));
                }
            }
        }

        /**
         * @return All names of the <em>option</em>
         */
        private String[]
        optionNames(Method option) {

            List<String> result = new ArrayList<String>();
            for (Entry<String, Method> e : this.allOptions.entrySet()) {
                final String optionName = e.getKey();
                final Method o          = e.getValue();

                if (o != option) continue;

                result.add(optionName);
            }

            return result.toArray(new String[result.size()]);
        }

        /**
         * @return All names of all options that are members of the <var>optionGroup</var>
         */
        private String[]
        optionNames(Class<?> optionGroup) {

            List<String> result = new ArrayList<String>();
            for (Entry<String, Method> e : this.allOptions.entrySet()) {
                final String optionName = e.getKey();
                final Method option     = e.getValue();

                for (Class<?> g : option.getAnnotation(CommandLineOption.class).group()) {
                    if (g == optionGroup) {
                        result.add(optionName);
                        break;
                    }
                }
            }

            return result.toArray(new String[result.size()]);
        }

        /**
         * Parses an option of the <var>target</var> iff the next token on the <var>ss</var> is not "--" nor "-", and
         * maps to an option of the <var>target</var>.
         *
         * @return                            Whether the next token(s) on <var>ss</var> were parsed as an option of the
         *                                    <var>target</var>
         * @throws CommandLineOptionException An error occurred during the parsing
         * @throws EX                         <var>ss</var> threw an exception
         */
        private boolean
        parseNextOption(StringStream<EX> ss, Object target) throws CommandLineOptionException, EX {

            if (ss.atEnd()) return false;

            // Special option "--" indicates the end of the command line options.
            if (ss.peek("--")) return false;

            // "-" is not a command line option, but a normal command line argument (typically means "STDIN" or
            // "STDOUT").
            if (ss.peek("-")) return false;

            // Is it a "verbose" (non-compact) option?
            if (this.parseNextVerboseOption(ss, target)) return true;

            // Iff the first letter after "-" can be interpreted as a single-letter option, then interpret ALL letters
            // as such.
            // This feature is called "compact command line options": E.g. "-lar" is equivalent with "-l -a -r".
            // Even options with arguments are possible, e.g. "-abc AA BB CC" in place of "-a AA -b BB -c CC".
            COMPACT_OPTIONS: if (ss.peek(CommandLineOptions.REGEX_COMPACT_OPTIONS)) {

                char   firstOptionLetter      = AssertionUtil.notNull(ss.group(1)).charAt(0);
                String followingOptionLetters = AssertionUtil.notNull(ss.group(2));

                {
                    String firstOptionName = "-" + firstOptionLetter;

                    Method firstOption = this.getOptionByName(firstOptionName);
                    if (firstOption == null) break COMPACT_OPTIONS;

                    // Only now that we have verified that the first letter can be interpreted as a single-letter
                    // option, we consume the token.
                    ss.consume();

                    this.applyCommandLineOption(firstOptionName, firstOption, ss, target);
                }

                for (int i = 0; i < followingOptionLetters.length(); i++) {

                    String optionName = "-" + followingOptionLetters.charAt(i);

                    Method optionMethod = this.getOptionByName(optionName);
                    if (optionMethod == null) {
                        throw new UnrecognizedOption(optionName);
                    }

                    this.applyCommandLineOption(optionName, optionMethod, ss, target);
                }

                return true;
            }

            return false;
        }

        /**
         * @return                            Whether the next tokens on <var>ss</var> could be parsed as an option for
         *                                    the <var>target</var>
         * @throws CommandLineOptionException An error occurred during the parsing
         * @throws EX                         <var>ss</var> threw an exception
         */
        private boolean
        parseNextVerboseOption(StringStream<EX> ss, Object target) throws CommandLineOptionException, EX {

            String s = ss.next();

            if (s == null) return false;

            // Special handling for verbose option syntax "--regex=abc".
            ALTERNATE_VERBOSE_SYNTAX: {
                int ioeq = s.indexOf('=');
                if (ioeq == -1) break ALTERNATE_VERBOSE_SYNTAX;

                final String optionName     = s.substring(0, ioeq);
                final String singleArgument = s.substring(ioeq + 1);

                Method option = this.getOptionByName(optionName);
                if (option == null) return false;

                // Notice: "Method.getParameterCount()" is only available in Java 8+.
                if (option.getParameterTypes().length != 1) throw new UnrecognizedOption(s);

                ss.consume();

                this.applyCommandLineOption(
                    optionName,                                                                  // optionName
                    option,                                                                      // option
                    new StringStream<EX>(ProducerUtil.<String, EX>fromElements(singleArgument)), // stringStream
                    target                                                                       // target
                );

                return true;
            }

            // Parse syntax "-foo arg1 arg2 arg3".
            String optionName = s;
            Method option     = this.getOptionByName(optionName);
            if (option == null) return false;

            ss.consume();

            this.applyCommandLineOption(
                optionName, // optionName
                option,     // option
                ss,         // stringStream
                target      // target
            );

            return true;
        }

        /**
         * @return {@code null} iff there is no applicable method
         */
        @Nullable public Method
        getOptionByName(String optionName) {

            return this.allOptions.get(optionName);
        }

        /**
         * Parses the command line option's arguments from the <var>args</var> and invokes the <var>method</var>.
         * <p>
         *   The arguments for the method are parsed from the command line, see {@link #getArgument(String[], int[],
         *   Annotation[], Class)}.
         * </p>
         * <p>
         *   Iff the method is a {@code varargs} method, then <em>all remaining arguments</em> are converted to an
         *   array.
         * </p>
         *
         * @throws CommandLineOptionException An error occurred during the parsing
         * @throws EX                         The <var>stringStream</var> threw an exception
         */
        public void
        applyCommandLineOption(
            String            optionName,
            Method            option,
            StringStream<EX>  stringStream,
            @Nullable Object  target
        ) throws CommandLineOptionException, EX  {

            if (this.singularOptions.contains(option) && this.actualOptions.contains(option)) {
                throw new DuplicateOption(option, optionName, this.optionNames(option));
            }

            Set<Class<?>> optionGroups = this.optionToOptionGroups.get(option);
            if (optionGroups != null) {
                for (Class<?> optionGroup : optionGroups) {
                    if (
                        this.singularOptionGroups.contains(optionGroup)
                        && this.actualOptionGroups.contains(optionGroup)
                    ) {
                        throw new ConflictingOptions(optionGroup, option, optionName);
                    }
                }
            }

            Class<?>[]     methodParametersTypes       = option.getParameterTypes();
            Annotation[][] methodParametersAnnotations = option.getParameterAnnotations();

            assert methodParametersTypes.length == methodParametersAnnotations.length;

            // Convert the command line option arguments into method call arguments.
            Object[] methodArgs = new Object[methodParametersTypes.length];
            for (int i = 0; i < methodArgs.length; i++) {

                try {
                    methodArgs[i] = this.getArgument(
                        stringStream,                   // stringStream
                        methodParametersAnnotations[i], // annotations
                        methodParametersTypes[i]        // targetType
                    );
                } catch (UnexpectedElementException uee) {
                    throw new OptionArgumentMissing(option, optionName, i);
                }
            }

            // Now that the "methodArgs" array is filled, invoke the method.
            try {
                option.invoke(target, methodArgs);
            } catch (InvocationTargetException ite) {
                throw new OptionProcessingException(optionName, ite.getTargetException());
            } catch (Exception e) {
                throw new OptionProcessingException(optionName, e);
            }

            this.actualOptions.add(option);

            if (optionGroups != null) {
                for (Class<?> optionGroup : optionGroups) {
                    this.actualOptionGroups.add(optionGroup);
                }
            }
        }

        /**
         * Creates and returns an object of the given <var>targetType</var> from tokens from <var>ss</var>.
         * <ul>
         *   <li>
         *     For an array target type, <em>all</em> remaining tokens from <var>ss</var> are parsed as array elements.
         *   </li>
         *   <li>
         *     For {@link Pattern} target type, the next token from <var>ss</var> is converted through {@link
         *     Pattern2#compile(String, int)}, with the flags as configured by the {@link RegexFlags} annotation of
         *     the method.
         *   </li>
         *   <li>
         *     For {@link Glob} target type, the next token from <var>ss</var> is converted through {@link
         *     Glob#compile(String, int)}, with the flags as configured by the {@link RegexFlags} annotation of
         *     the method.
         *   </li>
         *   <li>
         *     For a bean target type (a class that has only the zero-arg constructor), the bean is instantiated and as
         *     many tokens from <var>ss</var> as possible are parsed as options of the bean.
         *   </li>
         *   <li>
         *     For {@link InetAddress} target type, the next token from <var>ss</var> is converted through {@link
         *     InetAddress#getByName(String)}, except "any", which maps to {@code null} (the "wildcard address").
         *   </li>
         *   <li>
         *     For any other target type, the <em>next</em> token from <var>ss</var> is converted to the target type, as
         *     described {@link ObjectUtil#fromString(String, Class) here}.
         *   </li>
         * </ul>
         *
         * @throws CommandLineOptionException An error occurred during the parsing
         * @throws UnexpectedElementException The <var>ss</var> has too few tokens
         * @throws EX                         The <var>ss</var> threw an exception
         * @throws AssertionError             Bean creation failed
         */
        @Nullable private Object
        getArgument(StringStream<EX> ss, Annotation[] annotations, Class<?> targetType)
        throws CommandLineOptionException, UnexpectedElementException, EX {

            // Special case: The target type is an array type. (This handles also the case of a VARARGS method.)
            if (targetType.isArray()) {

                final Class<?> componentType = targetType.getComponentType();

                // Treat all remaining arguments as array elements (even those starting with a dash!).
                List<Object> elements = new ArrayList<Object>();
                while (!ss.atEnd()) {
                    elements.add(this.getArgument(ss, annotations, componentType));
                }

                int    size   = elements.size();
                Object result = Array.newInstance(componentType, size);
                if (componentType.isPrimitive()) {

                    // Can't use "System.arraycopy()" to copy to array of primitive.
                    for (int i = 0; i < size; i++) Array.set(result, i, elements.get(i));
                } else {
                    System.arraycopy(elements.toArray(), 0, result, 0, size);
                }

                return result;
            }

            // Special case: Target type is a Java bean.
            Constructor<?>[] cs = targetType.getConstructors();
            if (cs.length == 1 && cs[0].getParameterTypes().length == 0) {

                Object bean;
                try {
                    bean = cs[0].newInstance();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }

                // Invoke as many setter methods on the bean as possible.
                new Parser<EX>(targetType).parseOptions(ss, bean);
                ss.peekRead("--");

                return bean;
            }

            // At this point, the target type is "primitive", i.e. it maps to exactly ONE command line arg.
            if (ss.peek(CommandLineOptions.REGEX_OPTION)) throw new UnrecognizedOption(ss.peek());

            // Special case: Target type "java.util.Pattern".
            if (targetType == Pattern.class) {

                // Use "Pattern2", so that the Pattern2.WILDCARD flag can also be used.
                return Pattern2.compile(ss.read(), CommandLineOptions.getRegexFlags(annotations));
            }

            // Special case: Target type "de.unkrig.commons.text.pattern.Glob".
            if (targetType == Glob.class) {
                return Glob.compile(ss.read(), CommandLineOptions.getRegexFlags(annotations));
            }

            // Special case: Target type "java.net.InetAddress".
            if (targetType == InetAddress.class) {

                String host = ss.read();
                if ("any".equals(host)) return null;
                try {
                    return InetAddress.getByName(host);
                } catch (UnknownHostException uhe) {
                    throw new ArgumentConversionFailed(host, InetAddress.class, uhe);
                }
            }

            // Fall back to "ObjectUtil.fromString()".
            String arg = ss.read();
            try {
                return ObjectUtil.fromString(arg, targetType);
            } catch (IllegalArgumentException iae) {
                throw new ArgumentConversionFailed(arg, targetType, iae);
            }
        }
    }

    /**
     * Checks for a {@link RegexFlags} annotation and returns its value.
     */
    private static int
    getRegexFlags(Annotation[] annotations) {

        for (Annotation a : annotations) {
            if (a.annotationType() == RegexFlags.class) {
                return ((RegexFlags) a).value();
            }
        }

        return 0;
    }

    /**
     * Determines the method of {@code target.getClass()} that is applicable for the given <var>optionName</var>. The
     * rules for matching are as follows:
     * <ul>
     *   <li>The method must be annotated with {@link CommandLineOption @CommandLineOption}.</li>
     *   <li>
     *     If that annotation has the "{@code name}" element:
     *     <ul>
     *       <li>
     *         If the {@code name} value starts with "-":
     *         <ul>
     *           <li>
     *             The <var>optionName</var> equals the {@code name} value.
     *           </li>
     *         </ul>
     *       </li>
     *       <li>
     *         Otherwise, if the {@code name} value does not start with "-":
     *         <ul>
     *           <li>
     *             The <var>optionName</var> equals {@code "-"+name} or {@code "--"+name}
     *           </li>
     *         </ul>
     *       </li>
     *     </ul>
     *     Example:
     *     <br />
     *     <code>@CommandLineOption(name = { "alpha", "-beta" }) public void method() { ...</code>
     *     <br />
     *     matches if <var>optionName</var> is {@code "-alpha"}, {@code "--alpha"} and {@code "-beta"}.
     *   </li>
     *   <li>
     *     Otherwise, if that annotation does not have the "{@code name}" element:
     *     <ul>
     *       <li>
     *         The method name, with an optional "set" or "add" prefix removed, then converted to {@link
     *         Phrase#toLowerCaseHyphenated() lower-case-hyphenated}, then prefixed with "-" and "--", equals the
     *         <var>optionName</var>.
     *         <br />
     *         Example: Method name "setFooBar()" matches if <var>optionName</var> is "-foo-bar" or "--foo-bar".
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @return {@code null} iff there is no applicable method
     */
    @Nullable public static Method
    getMethodForOption(String optionName, Class<?> targetClass) {

        return new Parser<RuntimeException>(targetClass).getOptionByName(optionName);
    }

    /**
     * Parses the command line option's arguments from the <var>args</var> and invokes the <var>method</var>.
     * <p>
     *   Iff the method is a {@code varargs} method, then <em>all remaining arguments</em> are converted to an array.
     * </p>
     *
     * @param optionArgumentIndex         The position of the first option argument in <var>args</var>
     * @return                            The position of the last option argument in <var>args</var> plus one
     * @throws CommandLineOptionException An error occurred during the parsing
     * @throws AssertionError             The <var>method</var> is not annotated with {@link
     *                                    CommandLineOption @CommandLineOption}
     */
    public static int
    applyCommandLineOption(
        String           optionName,
        Method           option,
        String[]         args,
        int              optionArgumentIndex,
        @Nullable Object target
    ) throws CommandLineOptionException {

        Class<? extends Object> targetClass = target != null ? target.getClass() : option.getDeclaringClass();

        FromArrayProducer<String> fap = ProducerUtil.fromArray(args, optionArgumentIndex, args.length);

        new Parser<RuntimeException>(targetClass).applyCommandLineOption(
            optionName,
            option,
            new StringStream<RuntimeException>(fap),
            target
        );

        return fap.index();
    }

    /**
     * Reads (and decodes) the contents of a resource, replaces all occurrences of
     * "<code>${</code><var>system-property-name</var><code>}</code>" with the value of the designated system property,
     * and writes the result to the <var>outputStream</var> (typically {@code System.out}).
     * <p>
     *   The resource is found through the <var>baseClass</var>'s class loader and the name {@code
     *   "}<var>package-name</var>{@code /}<var>simple-class-name</var>{@code .}<var>relativeResourceName</var>{@code
     *   "}, where all periods in the <var>package-name</var> have been replaced with slashes.
     * </p>
     * <p>
     *   To ensure that the resource is decoded with the same charset as it was encoded, you should not use the JVM
     *   charset (which could be different in the build environment and the runtime environment).
     * </p>
     *
     * @param baseClass              Poses the base for the resource name
     * @param relativeResourceName   The name of the resource, relative to the <var>baseClass</var>
     * @param resourceCharset        The charset to use for decoding the contents of the resource; {@code null} for the
     *                               JVM default charset
     * @throws FileNotFoundException The resource could not be found
     * @see Class#getResource(String)
     */
    public static void
    printResource(
        Class<?>          baseClass,
        String            relativeResourceName,
        @Nullable Charset resourceCharset,
        OutputStream      outputStream
    ) throws IOException {

        String resourceName = baseClass.getSimpleName() + "." + relativeResourceName;

        InputStream is = baseClass.getResourceAsStream(resourceName);
        if (is == null) throw new FileNotFoundException(resourceName);

        CommandLineOptions.replaceSystemProperties(
            is,              // inputStream
            resourceCharset, // inputCharset
            true,            // closeInputStream
            outputStream,    // outputStream
            null,            // outputCharset
            false            // closeOutputStream
        );
    }

    /**
     * Reads (and decodes) the contents of a resource, replaces all occurrences of
     * "<code>${</code><var>system-property-name</var><code>}</code>" with the value of the designated system property,
     * and writes the result to the <var>outputStream</var> (typically {@code System.out}).
     * <p>
     *   The resource is found through the <var>classLoader</var> and (absolute) <var>resourceName</var>.
     * </p>
     * <p>
     *   To ensure that the resource is decoded with the same charset as it was encoded, you should not use the JVM
     *   charset (which could be different in the build environment and the runtime environment).
     * </p>
     *
     * @param resourceCharset        The charset to use for decoding the contents of the resource; {@code null} for the
     *                               JVM default charset
     * @throws FileNotFoundException The resource could not be found
     * @see ClassLoader#getResource(String)
     */
    public static void
    printResource(
        ClassLoader       classLoader,
        String            resourceName,
        @Nullable Charset resourceCharset,
        OutputStream      outputStream
    ) throws IOException {

        InputStream is = classLoader.getResourceAsStream(resourceName);
        if (is == null) throw new FileNotFoundException(resourceName);

        CommandLineOptions.replaceSystemProperties(
            is,              // inputStream
            resourceCharset, // inputCharset
            true,            // closeInputStream
            outputStream,    // outputStream
            null,            // outputCharset
            false            // closeOutputStream
        );
    }

    private static void
    replaceSystemProperties(
        InputStream       inputStream,
        @Nullable Charset inputCharset,
        boolean           closeInputStream,
        OutputStream      outputStream,
        @Nullable Charset outputCharset,
        boolean           closeOutputStream
    ) throws IOException {

    try {

        if (outputCharset == null) outputCharset = Charset.defaultCharset();
        Writer w = new OutputStreamWriter(outputStream, outputCharset);

        if (inputCharset == null) inputCharset = Charset.defaultCharset();
        Reader r = new InputStreamReader(inputStream, inputCharset);

        PatternUtil.replaceSystemProperties(r, w);
        w.flush();
        } finally {
            if (closeInputStream)  try { inputStream.close();  } catch (Exception e) {}
            if (closeOutputStream) try { outputStream.close(); } catch (Exception e) {}
        }
    }
}
