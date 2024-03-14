
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

package de.unkrig.commons.text.expression;

import static de.unkrig.commons.text.expression.Scanner.TokenType.CHARACTER_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.FLOATING_POINT_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.IDENTIFIER;
import static de.unkrig.commons.text.expression.Scanner.TokenType.INTEGER_LITERAL;
import static de.unkrig.commons.text.expression.Scanner.TokenType.STRING_LITERAL;

import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.Scanner.TokenType;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.ScannerUtil;

/**
 * Parses an expression like
 * <pre>
 * s == "abc" &amp;&amp; (c == 'b' || !b == true)
 * </pre>
 *
 * Supported operators (in ascending priority) are:
 * <dl>
 *   <dt>{@code a ? b : c}</dt>
 *   <dd>
 *     {@code b} if {@code a}, {@link ExpressionEvaluator#toBoolean(Object) converted to boolean}, is true, otherwise
 *     {@code c}.
 *   </dd>
 *
 *   <dt>{@code a || b}</dt>
 *   <dd>
 *     {@code a} if {@code a}, {@link ExpressionEvaluator#toBoolean(Object) converted to boolean}, is true, otherwise
 *     {@code b}. (Equivalent to "{@code a ? a : b}", except that {@code a} is evaluated only once.)
 *   </dd>
 *
 *   <dt>{@code a && b}</dt>
 *   <dd>
 *     {@code b} if {@code a}, {@link ExpressionEvaluator#toBoolean(Object) converted to boolean}, is true, otherwise
 *     {@code false}. (Equivalent to "{@code a ? b : false}".)
 *   </dd>
 *
 *   <dt>{@code == != < <= > >=}</dt>
 *   <dd>
 *     Arithmetic comparison (if both operands are numeric) or lexicographic comparison (otherwise).
 *   </dd>
 *
 *   <dt>
 *     {@code a =* "*.c"}
 *     <br />
 *     {@code a =* "(*).c=$1.h"}
 *   </dt>
 *   <dd>
 *     Wildcard matching/replacing (see {@link Glob}).
 *     The left-hand-side operand is the <var>subject</var>, the right-hand-side operand is the <var>glob</var>.
 *     Iff the right-hand-side operand contains an equal sign ("{@code =}"), as in the second example, then it
 *     specifies a <var>glob</var> and the <var>replacement</var>.
 *     <br />
 *     Evaluates
 *     <ul>
 *       <li>to {@code null} iff the subject does not match the <var>glob</var>, otherwise,</li>
 *       <li>to the <var>replacement</var>, iff one is specified, otherwise,</li>
 *       <li>the <var>subject</var></li>
 *     </ul>
 *     The "{@code =*}" operator is only available if the {@link Parser.Extension#OPERATOR_GLOB} parsing extension is
 *     enabled (which is the default).
 *   </dd>
 *
 *   <dt>
 *     {@code a =~ "A.*"}
 *     <br />
 *     {@code a =~ "(.*)abc(.*)=$1$2"}
 *   </dt>
 *   <dd>
 *     Regex matching/replacing (see {@link Pattern}).
 *     The left-hand-side operand is the <var>subject</var>, the right-hand-side operand is the <var>pattern</var>.
 *     Iff the right-hand-side operand contains an equal sign ("{@code =}"), as in the second example, then it
 *     specifies a <var>pattern</var> and the <var>replacement</var>.
 *     <br />
 *     Evaluates
 *     <ul>
 *       <li>to {@code null} iff the subject does not match the <var>pattern</var>, otherwise,</li>
 *       <li>to the <var>replacement</var>, iff one is specified, otherwise,</li>
 *       <li>the <var>subject</var></li>
 *     </ul>
 *     The "{@code =~}" operator is only available if the {@link Parser.Extension#OPERATOR_REGEX} parsing extension is
 *     enabled (which is the default).
 *   </dd>
 *
 *   <dt>{@code + -}</dt>
 *   <dd>
 *     Addition (if both operands are numeric) or string concatenation (with arrays pretty-printed); subtraction.
 *   </dd>
 *
 *   <dt>{@code * / %}</dt>
 *   <dd>
 *     Multiplication; division; remainder (modulo).
 *   </dd>
 *
 *   <dt>{@code a instanceof reference-type}</dt>
 *   <dd>Whether {@code a} is non-null and a subtype of {@code reference-type}</dd>
 *
 *   <dt>{@code (...) ! -}</dt>
 *   <dd>
 *     Grouping; logical negation; arithmetic negation.
 *   </dd>
 * </dl>
 * <p>
 *   Primaries are:
 * </p>
 * <dl>
 *   <dt>{@code "abc"}</dt>
 *   <dd>
 *     String literal
 *   </dd>
 *
 *   <dt>{@code 123}</dt>
 *   <dd>
 *     Integer literal
 *   </dd>
 *
 *   <dt>{@code 123L 123l}</dt>
 *   <dd>
 *     Long literal
 *   </dd>
 *
 *   <dt>{@code 1F 1.2E+3f}</dt>
 *   <dd>
 *     Float literal
 *   </dd>
 *
 *   <dt>{@code 1D 1d 1. 1.1 1e-3}</dt>
 *   <dd>
 *     Double literal
 *   </dd>
 *
 *   <dt>{@code true false null}</dt>
 *   <dd>
 *     Special constant values
 *   </dd>
 *
 *   <dt>{@code abc}</dt>
 *   <dd>
 *     Variable reference, see {@link Expression#evaluate(de.unkrig.commons.lang.protocol.Mapping)}
 *   </dd>
 *
 *   <dt>
 *     {@code new pkg.Clazz(arg1, arg2)}
 *     <br />
 *     {@code new RootPackageClazz(arg1, arg2)}
 *     <br />
 *     {@code new ImportedClazz(arg1, arg2)}
 *   </dt>
 *   <dd>
 *     Class instance creation
 *   </dd>
 *
 *   <dt>
 *     {@code new pkg.Clazz} (Only with {@link Parser.Extension#NEW_CLASS_WITHOUT_PARENTHESES})
 *     <br />
 *     {@code pkg.Clazz()} (Only with {@link Parser.Extension#NEW_CLASS_WITHOUT_KEYWORD})
 *     <br />
 *     {@code pkg.Clazz} (Only with {@link Parser.Extension#NEW_CLASS_WITHOUT_KEYWORD} <em>and</em> {@link
 *     Parser.Extension#NEW_CLASS_WITHOUT_PARENTHESES})
 *   </dt>
 *   <dd>
 *     Abbreviated forms of "{@code new pkg.Clazz()}"
 *   </dd>
 *
 *   <dt>
 *     {@code new pkg.Clazz[x]}
 *     <br />
 *     {@code new pkg.Clazz[x][y][][]}
 *     <br />
 *     {@code new int[x]}
 *     <br />
 *     {@code new int[x][y][][]}
 *   </dt>
 *   <dd>
 *     Array creation (notice that there is no abbreviated notation, without the {@code new} keyword, as for class
 *     instance creation)
 *   </dd>
 *
 *   <dt>{@code x[y]}</dt>
 *   <dd>
 *     Array element access
 *   </dd>
 *
 *   <dt>{@code x.name}</dt>
 *   <dd>
 *     Attribute reference; gets the value of the field "{@code x.name}", or invokes "{@code x.name()}" or "{@code
 *     x.getName()}".
 *     The special attributes {@code "_attributes"} and {@code "_properties"} evaluate to the names of all non-static
 *     attributes;
 *     the special attributes {@code "_staticAttributes"} and {@code "_staticProperties"} evaluate to the names of all
 *     <em>static</em> attributes.
 *     <br>
 *     (It is <em>not</em> possible to <em>set</em> an attribute this way.)
 *   </dd>
 *
 *   <dt>{@code x.meth(a, b, c)}</dt>
 *   <dd>
 *     Method invocation
 *   </dd>
 * </dl>
 * <p>
 *   {@code Null} operands are handled leniently, e.g. "7 == null" is false, "null == null" is true.
 * </p>
 *
 * <p>
 *   To implement your parser, derive from this class and implement the abstract methods.
 * </p>
 *
 * @param <T>  The type returned by {@link #parse()}
 * @param <EX> The exception thrown by the abstract handlers (e.g. {@link #conditional(Object, Object, Object)})
 * @see #enableExtension(Extension)
 * @see #disableExtension(Extension)
 */
public abstract
class Parser<T, EX extends Throwable> extends AbstractParser<TokenType> {

    static {
        AssertionUtil.enableAssertionsForThisClass();
    }

    public
    Parser(ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> tokenProducer) {
        super(tokenProducer);
    }

    public
    Parser(Reader in) { this(ScannerUtil.toDocumentScanner(Scanner.stringScanner(), in)); }

    public
    Parser(String expression) { this(Scanner.stringScanner().setInput(expression)); }

    /**
     * @return The currently configured single imports (fully qualified class names)
     */
    public String[]
    getSingleImports() { return this.singleImports.toArray(new String[this.singleImports.size()]); }

    /**
     * @param singleImports Fully qualified class or interface names
     */
    public Parser<T, EX>
    addSingleImports(String... singleImports) {
        for (String si : singleImports) this.singleImports.add(si);
        return this;
    }

    /**
     * @return The currently configured on-demand imports (fully qualified package names)
     */
    public String[]
    getOnDemandImports() { return this.onDemandImports.toArray(new String[this.onDemandImports.size()]); }

    /**
     * By default, there is only one on-demand import: "java.lang".
     *
     * @param onDemandImports Fully qualified package names
     */
    public Parser<T, EX>
    addOnDemandImports(String... onDemandImports) {
        for (String si : onDemandImports) this.onDemandImports.add(si);
        return this;
    }

    /**
     * @return The currently configured {@link ClassLoader}
     */
    public ClassLoader
    getClassLoader() { return this.classLoader; }

    /**
     * @param classLoader Used to load classes named in the expression; by default the class loader which loaded
     *                    the {@link Parser} class.
     */
    public Parser<T, EX>
    setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Sets the given parsing <var>extensions</var>. By default, all extensions are enabled.
     *
     * @see Parser.Extension
     */
    public void
    setExtensions(Collection<Extension> extensions) {
        this.extensions = extensions.isEmpty() ? EnumSet.noneOf(Extension.class) : EnumSet.copyOf(extensions);
    }

    /**
     * Sets the given parsing <var>extensions</var>. By default, all extensions are enabled.
     *
     * @see Parser.Extension
     */
    public void
    setExtensions(EnumSet<Extension> extensions) { this.extensions = EnumSet.copyOf(extensions); }

    /**
     * Enables the given parsing <var>extension</var>. By default, all extensions are enabled.
     *
     * @see Parser.Extension
     */
    public void
    enableExtension(Extension extension) { this.extensions.add(extension); }

    /**
     * Disables the given parsing <var>extension</var>. By default, all extensions are enabled.
     *
     * @see Parser.Extension
     */
    public void
    disableExtension(Extension extension) { this.extensions.remove(extension); }

    /**
     * @return The parsed expression
     */
    public T
    parse() throws ParseException, EX {
        try {
            this.parseImports();
            T result = this.parseExpression().toValue();
            this.eoi();
            return result;
        } catch (ParseException pe) {
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), pe);
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), re);
        } catch (Exception e) {
            @SuppressWarnings("unchecked") EX ee = (EX) e;
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), ee);
        }
    }

    /**
     * Parses exactly <em>one</em> expression and leaves the following tokens in the input untouched.
     *
     * @return The parsed expression
     */
    public T
    parsePart() throws ParseException, EX {
        try {
            this.parseImports();
            return this.parseExpression().toValue();
        } catch (ParseException pe) {
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), pe);
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), re);
        } catch (Exception e) {
            @SuppressWarnings("unchecked") EX ee = (EX) e;
            throw ExceptionUtil.wrap("At " + this.scanner.toString(), ee);
        }
    }

    /**
     * Representation of all unary operators.
     */
    public
    enum UnaryOperator {

        MINUS("-"), // SUPPRESS CHECKSTYLE JavadocVariable:2
        LOGICAL_COMPLEMENT("!"),
        BITWISE_COMPLEMENT("~");

        private final String text;

        UnaryOperator(String text) { this.text = text; }

        @Override public String
        toString() { return this.text; }
    }

    /**
     * Representation of all binary operators.
     */
    public
    enum BinaryOperator {

        EQUAL("=="), // SUPPRESS CHECKSTYLE JavadocVariable:20
        GLOB("=*"),
        REGEX("=~"),
        NOT_EQUAL("!="),
        LESS("<"),
        LESS_EQUAL("<="),
        GREATER(">"),
        GREATER_EQUAL(">="),
        PLUS("+"),
        MINUS("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MODULO("%"),
        LOGICAL_OR("||"),
        LOGICAL_AND("&&"),
        BITWISE_OR("|"),
        BITWISE_XOR("^"),
        BITWISE_AND("&"),
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT(">>"),
        RIGHT_USHIFT(">>>"),

        ;

        private final String text;

        BinaryOperator(String text) { this.text = text; }

        @Override public String
        toString() { return this.text; }
    }

    /**
     * Various extensions to the Java expression syntax.
     *
     * @see Parser#enableExtension(Extension)
     * @see Parser#disableExtension(Extension)
     */
    public
    enum Extension {

        /**
         * The {@code new} keyword can be left out when a class object is instantiated. Notice that the {@code new}
         * can never be left out when creating arrays.
         */
        NEW_CLASS_WITHOUT_KEYWORD,

        /**
         * The "()" can be omitted when instantiating a class object with its zero-arg constructor.
         */
        NEW_CLASS_WITHOUT_PARENTHESES,

        /**
         * The operator {@code =*} implements a wildcard pattern matching algorithm.
         * <p>
         *   Usage example:
         * </p>
         * <pre>
         *   "foo.java" =* "*.java"
         * </pre>
         */
        OPERATOR_GLOB,

        /**
         * The operator {@code =~} implements a regex pattern matching algorithm.
         * <p>
         *   Usage example:
         * </p>
         * <pre>
         *   ".*" =~ "abc"
         * </pre>
         */
        OPERATOR_REGEX,
    }

    /**
     * The currently configured imports (fully qualified package names).
     */
    private final List<String>       singleImports   = new ArrayList<String>();
    private final List<String>       onDemandImports = new ArrayList<String>(Collections.singleton("java.lang"));

    private ClassLoader        classLoader = this.getClass().getClassLoader();
    private EnumSet<Extension> extensions  = EnumSet.allOf(Extension.class);

    /**
     * It is generally difficult to distinguish between values, types and packages, e.g. "a.b.c" could represent
     * <ul>
     *   <li>Field c of field b of variable a (a value)
     *   <li>Class or interface c in package a.b (a type)
     *   <li>Package a.b.c
     * </ul>
     * This is why many of the productions (corresponding with the various {@code parse...()} methods) return an
     * {@link Atom}.
     */
    interface Atom<T, E extends Throwable> {

        /**
         * @return                This atom's value (where {@code null} is a perfectly legal value)
         * @throws ParseException Iff this atom does not pose a value, e.g. "String[]"
         */
        T
        toValue() throws E, ParseException;

        /**
         * @return {@code null} iff this atom can impossibly denote a type, e.g. "1 + 2"
         */
        @Nullable Class<?>
        toType();

        /**
         * @return {@code null} iff this atom can impossibly denote a package, e.g. "1 + 2"
         */
        @Nullable String
        toPackage();
    }

    /**
     * @return An {@link Atom} that poses a value
     */
    private static <T, E extends Throwable> Atom<T, E>
    value(final T t) {
        return new Atom<T, E>() {

            @Override @NotNull public T
            toValue() { return t; }

            @Override @Nullable public Class<?>
            toType() { return null; }

            @Override @Nullable public String
            toPackage() { return null; }
        };
    }

    /**
     * @return An {@link Atom} that poses a type
     */
    private Atom<T, EX>
    type(final Class<?> type) {

        return new Atom<T, EX>() {

            @Override @NotNull public T
            toValue() throws EX, ParseException {
                if (
                    Parser.this.extensions.contains(Extension.NEW_CLASS_WITHOUT_KEYWORD)
                    && Parser.this.extensions.contains(Extension.NEW_CLASS_WITHOUT_PARENTHESES)
                ) {
                    return Parser.this.newClass(type, Collections.<T>emptyList());
                } else {
                    throw new ParseException("'" + type.getName() + "' is a type, not a value");
                }
            }

            @Override public Class<?>
            toType() { return type; }

            @Override @Nullable public String
            toPackage() { return null; }
        };
    }

    /**
     * <pre>
     * imports := { import }
     *
     * import := 'import' identifier { '.' identifier } [ '.' '*' ] ';'
     * </pre>
     */
    private void
    parseImports() throws ParseException {

        while (this.peekRead("import")) {
            String qn = this.read(TokenType.IDENTIFIER);

            QN:
            for (;;) {
                switch (this.read(";", ".")) {
                case 0: // ';'
                    this.singleImports.add(qn);
                    break QN;
                case 1: // '.'
                    if (this.peekRead("*")) {
                        this.read(";");
                        this.onDemandImports.add(qn);
                        break QN;
                    }
                    qn += "." + this.read(TokenType.IDENTIFIER);
                    break;
                }
            }
        }
    }

    /**
     * <pre>
     * expression := conditional
     * </pre>
     */
    private Atom<T, EX>
    parseExpression() throws ParseException, EX { return this.parseConditional(); }

    /**
     * <pre>
     * conditional := logical-or [ '?' logical-or ':' conditional ]
     * </pre>
     */
    private Atom<T, EX>
    parseConditional() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseLogicalOr();
        if (!this.peekRead("?")) return lhs;

        T mhs = this.parseLogicalOr().toValue();
        this.read(":");
        T rhs = this.parseConditional().toValue();
        return Parser.value(this.conditional(lhs.toValue(), mhs, rhs));
    }

    /**
     * <pre>
     * logical-or := logical-and { '||' logical-and }
     * </pre>
     */
    private Atom<T, EX>
    parseLogicalOr() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseLogicalAnd();
        while (this.peekRead("||")) {
            lhs = Parser.value(this.binaryOperation(
                lhs.toValue(),
                BinaryOperator.LOGICAL_OR,
                this.parseLogicalAnd().toValue()
            ));
        }
        return lhs;
    }

    /**
     * <pre>
     * logical-and := bitwise-or { '&&' bitwise-or }
     * </pre>
     */
    private Atom<T, EX>
    parseLogicalAnd() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseBitwiseOr();
        while (this.peekRead("&&")) {
            lhs = Parser.value(this.binaryOperation(
                lhs.toValue(),
                BinaryOperator.LOGICAL_AND,
                this.parseBitwiseOr().toValue()
            ));
        }
        return lhs;
    }

    /**
     * <pre>
     * bitwise-or := bitwise-xor { '|' bitwise-xor }
     * </pre>
     */
    private Atom<T, EX>
    parseBitwiseOr() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseBitwiseXor();
        while (this.peekRead("|")) {
            lhs = Parser.value(this.binaryOperation(
                lhs.toValue(),
                BinaryOperator.BITWISE_OR,
                this.parseBitwiseXor().toValue()
            ));
        }
        return lhs;
    }

    /**
     * <pre>
     * bitwise-xor := bitwise-and { '^' bitwise-and }
     * </pre>
     */
    private Atom<T, EX>
    parseBitwiseXor() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseBitwiseAnd();
        while (this.peekRead("^")) {
            lhs = Parser.value(this.binaryOperation(
                lhs.toValue(),
                BinaryOperator.BITWISE_XOR,
                this.parseBitwiseAnd().toValue()
            ));
        }
        return lhs;
    }

    /**
     * <pre>
     * bitwise-and := relational { '^' relational }
     * </pre>
     */
    private Atom<T, EX>
    parseBitwiseAnd() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseRelational();
        while (this.peekRead("&")) {
            lhs = Parser.value(this.binaryOperation(
                lhs.toValue(),
                BinaryOperator.BITWISE_AND,
                this.parseRelational().toValue()
            ));
        }
        return lhs;
    }

    /**
     * <pre>
     * relational :=
     *    shift {
     *       '==' shift
     *       | '=*' shift
     *       | '=~' shift
     *       | '!=' shift
     *       | '<'  shift
     *       | '<=' shift
     *       | '>'  shift
     *       | '>=' shift
     *       | 'instanceof' reference-type
     *    }
     * </pre>
     */
    private Atom<T, EX>
    parseRelational() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseShift();

        for (;;) {
            BinaryOperator operator;
            if (this.extensions.contains(Extension.OPERATOR_GLOB) && this.peekRead("=*")) {
                lhs = Parser.value(this.binaryOperation(
                    lhs.toValue(),
                    BinaryOperator.GLOB,
                    this.parseRelational().toValue()
                ));
            } else
            if (this.extensions.contains(Extension.OPERATOR_REGEX) && this.peekRead("=~")) {
                lhs = Parser.value(this.binaryOperation(
                    lhs.toValue(),
                    BinaryOperator.REGEX,
                    this.parseRelational().toValue()
                ));
            } else
            if ((operator = this.peekReadEnum(
                BinaryOperator.EQUAL,
                BinaryOperator.NOT_EQUAL,
                BinaryOperator.LESS,
                BinaryOperator.LESS_EQUAL,
                BinaryOperator.GREATER,
                BinaryOperator.GREATER_EQUAL
            )) != null) {
                lhs = Parser.value(this.binaryOperation(lhs.toValue(), operator, this.parseRelational().toValue()));
            } else
            if (this.peekRead("instanceof")) {
                lhs = Parser.value(this.instanceoF(lhs.toValue(), this.parseReferenceType()));
            } else
            {
                break;
            }

        }
        return lhs;
    }

    /**
     * <pre>
     * shift :=
     *     additive { ( '<<' | '>>' | '>>>' ) additive }
     * </pre>
     */
    private Atom<T, EX>
    parseShift() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseAdditive();
        for (;;) {
            final BinaryOperator op = this.peekReadEnum(
                BinaryOperator.LEFT_SHIFT,
                BinaryOperator.RIGHT_SHIFT,
                BinaryOperator.RIGHT_USHIFT
            );
            if (op == null) return lhs;
            lhs = Parser.value(this.binaryOperation(lhs.toValue(), op, this.parseAdditive().toValue()));
        }
    }

    /**
     * <pre>
     * additive :=
     *     multiplicative { ( '+' | '-' ) multiplicative }
     * </pre>
     */
    private Atom<T, EX>
    parseAdditive() throws ParseException, EX {
        Atom<T, EX> mul = this.parseMultiplicative();
        for (;;) {
            final BinaryOperator op = this.peekReadEnum(BinaryOperator.PLUS, BinaryOperator.MINUS);
            if (op == null) return mul;
            mul = Parser.value(this.binaryOperation(mul.toValue(), op, this.parseMultiplicative().toValue()));
        }
    }

    /**
     * <pre>
     * multiplicative :=
     *    selector { ( '*' | '/' | '%' ) selector }
     * </pre>
     */
    private Atom<T, EX>
    parseMultiplicative() throws ParseException, EX {
        Atom<T, EX> lhs = this.parseSelector();
        for (;;) {
            final BinaryOperator op = this.peekReadEnum(
                BinaryOperator.MULTIPLY,
                BinaryOperator.DIVIDE,
                BinaryOperator.MODULO
            );
            if (op == null) return lhs;
            lhs = Parser.value(this.binaryOperation(lhs.toValue(), op, this.parseSelector().toValue()));
        }
    }

    /**
     * <pre>
     * reference-type :=
     *    ( 'boolean' | 'byte' | 'short' | 'int' | 'long' | 'char' | 'float' | 'double' ) '[' ']' { '[' ']' }
     *    | class-or-interface-type { '[' ']' }
     * </pre>
     */
    private Class<?>
    parseReferenceType() throws ParseException {
        Class<?> result;

        if (this.peek(IDENTIFIER) != null) {
            result = this.parseClassOrInterfaceType();
        } else {
            result = this.parsePrimitiveType();
            this.read("[");
            this.read("]");
        }

        while (this.peekRead("[")) {
            this.read("]");
            result = Array.newInstance(result, 0).getClass();
        }

        return result;
    }

    private Class<?>
    parsePrimitiveType() throws ParseException {
        switch (this.read("boolean", "byte", "short", "int", "long", "char", "float", "double")) {
        case 0:  return boolean.class;
        case 1:  return    byte.class;
        case 2:  return   short.class;
        case 3:  return     int.class;
        case 4:  return    long.class;
        case 5:  return    char.class;
        case 6:  return   float.class;
        case 7:  return  double.class;
        default: throw new IllegalStateException();
        }
    }

    /**
     * <pre>
     * selector :=
     *    primary [ arguments ] { '.' identifier selector-rest }
     *    | primary [ arguments ] '[' expression ']'
     * </pre>
     */
    private Atom<T, EX>
    parseSelector() throws ParseException, EX {
        Atom<T, EX> result = this.parsePrimary();

        if (this.extensions.contains(Extension.NEW_CLASS_WITHOUT_KEYWORD)) {
            List<T> arguments = this.parseOptionalArguments();
            if (arguments != null) {

                // MyClass()
                Class<?> clasS = result.toType();
                if (clasS != null) result = Parser.value(this.newClass(clasS, arguments));
            }
        }

        for (;;) {
            if (this.peekRead(".")) {
                result = this.parseSelectorRest(result, this.read(IDENTIFIER));
            } else
            if (this.peekRead("[")) {
                Atom<T, EX> index = this.parseExpression();
                this.read("]");
                result = Parser.value(this.arrayAccess(result.toValue(), index.toValue()));
            } else
            {
                return result;
            }
        }
    }

    /**
     * <pre>
     * selector-rest :=
     *    arguments
     *    | (nothing)
     * </pre>
     */
    private Atom<T, EX>
    parseSelectorRest(final Atom<T, EX> target, final String identifier) throws ParseException, EX {

        {
            final List<T> arguments = this.parseOptionalArguments();
            if (arguments != null) {

                if (this.extensions.contains(Extension.NEW_CLASS_WITHOUT_KEYWORD)) {
                    String packagE = target.toPackage();
                    if (packagE != null) {

                        // pkg.MyClass(...)
                        final Class<?> clasS = this.loadClass(packagE + '.' + identifier);
                        if (clasS != null) return new Atom<T, EX>() {

                            @Override @NotNull public T
                            toValue() throws EX { return Parser.this.newClass(clasS, arguments); }

                            @Override public Class<?>
                            toType() { return clasS; }

                            @Override @Nullable public String
                            toPackage() { return null; }
                        };
                    }
                }

                // A method invocation.
                Class<?> clasS = target.toType();
                if (clasS != null) {
                    return Parser.value(this.staticMethodInvocation(clasS, identifier, arguments));
                } else {
                    return Parser.value(this.methodInvocation(target.toValue(), identifier, arguments));
                }
            }
        }

        // A qualified class name?
        {
            String packagE = target.toPackage();
            if (packagE != null) {
                Class<?> clasS = this.loadClass(packagE + '.' + identifier);
                if (clasS != null) return this.type(clasS);
            }
        }

        // A nested type or a static field reference?
        {
            Class<?> type = target.toType();
            if (type != null) {

                {
                    Class<?> nestedType = this.loadClass(type.getName() + '$' + identifier);
                    if (nestedType != null) return this.type(nestedType);
                }

                return Parser.value(this.staticFieldReference(type, identifier));
            }
        }

        // Must be a package or a nonstatic field reference.
        return new Atom<T, EX>() {

            @Override @NotNull public T
            toValue() throws EX, ParseException {
                return Parser.this.fieldReference(target.toValue(), identifier);
            }

            @Override @Nullable public Class<?>
            toType() { return null; }

            @Override @Nullable public String
            toPackage() {
                String packagE = target.toPackage();
                return packagE == null ? null : packagE + '.' + identifier;
            }
        };
    }

    /**
     * <pre>
     * primary :=
     *    '(' expression ')'
     *    | '!' primary
     *    | '-' primary
     *    | string-literal
     *    | int-literal
     *    | long-literal
     *    | float-literal
     *    | double-literal
     *    | 'null' | 'true' | 'false'
     *    | 'new' class-or-interface-type { '[' expression ']' } { '[' ']' }
     *    | 'new' class-or-interface-type arguments
     *    | 'new' primitive-type  '[' expression ']' { '[' expression ']' } { '[' ']' }
     *    | identifier                        // Variable reference
     *    | class-or-interface-type arguments // Class instantiation
     *    | class-or-interface-type           // Class instantiation
     *    | class-or-interface-type           // Class literal
     * </pre>
     */
    private Atom<T, EX>
    parsePrimary() throws ParseException, EX {

        if (this.peekRead("(")) {
            final Atom<T, EX> result = this.parseExpression();
            this.read(")");

            if (this.peek(
                "~", "!", "(",                              // SUPPRESS CHECKSTYLE WrapMethod:3
                IDENTIFIER,
                "this", "new",
                INTEGER_LITERAL, CHARACTER_LITERAL, STRING_LITERAL, FLOATING_POINT_LITERAL
            ) != -1) {
                Class<?> type = result.toType();
                if (type == null) throw new ParseException("'" + type + "' does not pose a type");
                return Parser.value(this.cast(type, this.parsePrimary().toValue()));
            }

            return Parser.value(this.parenthesized(result.toValue()));
        }

        {
            UnaryOperator operator = this.peekReadEnum(
                UnaryOperator.LOGICAL_COMPLEMENT,
                UnaryOperator.MINUS,
                UnaryOperator.BITWISE_COMPLEMENT
            );
            if (operator != null) {
                if (operator == UnaryOperator.MINUS && this.peek(INTEGER_LITERAL) != null) {
                    try {
                        return Parser.value(this.literal(Scanner.decodeIntegerLiteral('-' + this.read().text)));
                    } catch (ScanException se) {
                        throw ExceptionUtil.wrap(null, se, ParseException.class);
                    }
                }
                return Parser.value(this.unaryOperation(operator, this.parseSelector().toValue()));
            }
        }

        if (this.peek(CHARACTER_LITERAL) != null) {
            try {
                return Parser.value(this.literal(Scanner.decodeCharacterLiteral(this.read().text)));
            } catch (ScanException se) {
                throw ExceptionUtil.wrap(null, se, ParseException.class);
            }
        }
        if (this.peek(STRING_LITERAL) != null) {
            try {
                return Parser.value(this.literal(Scanner.decodeStringLiteral(this.read().text)));
            } catch (ScanException se) {
                throw ExceptionUtil.wrap(null, se, ParseException.class);
            }
        }
        if (this.peek(INTEGER_LITERAL) != null) {
            try {
                return Parser.value(this.literal(Scanner.decodeIntegerLiteral(this.read().text)));
            } catch (ScanException se) {
                throw ExceptionUtil.wrap(null, se, ParseException.class);
            }
        }
        if (this.peek(FLOATING_POINT_LITERAL) != null) {
            return Parser.value(this.literal(Scanner.decodeFloatingPointLiteral(this.read().text)));
        }

        if (this.peekRead("true"))  return Parser.value(this.literal(true));
        if (this.peekRead("false")) return Parser.value(this.literal(false));
        if (this.peekRead("null"))  return Parser.value(this.literal(null));

        if (this.peekRead("new")) {

            // Primitive array creation?
            if (this.peek(IDENTIFIER) == null) {
                return Parser.value(this.parseNewArrayRest(this.parsePrimitiveType()));
            }

            Class<?> clasS = this.parseClassOrInterfaceType();

            // Class or interface array creation?
            if (this.peek("[")) return Parser.value(this.parseNewArrayRest(clasS));

            // Class instance creation.
            List<T> arguments = this.parseOptionalArguments();
            if (arguments == null) {
                if (this.extensions.contains(Extension.NEW_CLASS_WITHOUT_PARENTHESES)) {
                    arguments = Collections.emptyList();
                } else {
                    throw new ParseException("Opening parenthesis expected");
                }
            }

            return Parser.value(this.newClass(clasS, arguments));
        }

        final String identifier = this.peekRead(IDENTIFIER);
        if (identifier != null) {

            // An imported type?
            {
                Class<?> clasS = this.loadImportedClass(identifier);
                if (clasS != null) return this.type(clasS);
            }

            // Must be a variable reference or a package.
            return new Atom<T, EX>() {

                @Override @NotNull public T
                toValue() throws EX, ParseException { return Parser.this.variableReference(identifier); }

                @Override @Nullable public Class<?>
                toType() { return null; }

                @Override public String
                toPackage() { return identifier; }
            };
        }

        throw new ParseException("Primary expected instead of \"" + this.peek() + "\"");
    }

    /**
     * <pre>
     *     '[' expression ']' { '[' expression ']' } { '[' ']' }
     * </pre>
     */
    private T
    parseNewArrayRest(Class<?> clasS) throws ParseException, EX {
        final List<T> dimensions = new ArrayList<T>();

        this.read("[");
        dimensions.add(this.parseExpression().toValue());
        this.read("]");

        while (this.peekRead("[")) {
            if (this.peekRead("]")) {
                clasS = Array.newInstance(clasS, 0).getClass();
                while (this.peekRead("[")) {
                    this.read("]");
                    clasS = Array.newInstance(clasS, 0).getClass();
                }
                break;
            }
            dimensions.add(this.parseExpression().toValue());
            this.read("]");
        }

        return this.newArray(clasS, dimensions);
    }

    /**
     * <pre>
     * class-or-interface-type := identifier { '.' identifier }
     * </pre>
     * <p>
     *   This method returns once it is able to load the class, i.e. it loads top-level types, but cannot be used to
     *   find nested types.
     * </p>
     */
    private Class<?>
    parseClassOrInterfaceType() throws ParseException {

        final String identifier = this.read(IDENTIFIER);

        // Imported class.
        {
            Class<?> importedClass = this.loadImportedClass(identifier);
            if (importedClass != null) return importedClass;
        }

        for (String qualifiedClassName = identifier;; qualifiedClassName += '.' + this.read(TokenType.IDENTIFIER)) {

            Class<?> clasS = this.loadClass(qualifiedClassName);
            if (clasS != null) return clasS;

            if (!this.peekRead(".")) throw new ParseException("Cannot load \"" + qualifiedClassName + "\"");
        }
    }

    /**
     * <pre>
     * optional-arguments := [ arguments ]
     *
     * arguments := '(' [ expression { ',' expression } ] ')'
     * </pre>
     *
     * @return {@code null} iff there is no opening parenthesis
     */
    @Nullable private List<T>
    parseOptionalArguments() throws ParseException, EX {
        if (!this.peekRead("(")) return null;
        if (this.peekRead(")")) return Collections.emptyList();

        List<T> arguments = new ArrayList<T>();
        do {
            arguments.add(this.parseExpression().toValue());
        } while (this.peekRead(","));
        this.read(")");
        return arguments;
    }

    // ABSTRACT HANDLERS.

    // SUPPRESS CHECKSTYLE JavadocMethod:15
    protected abstract T conditional(T lhs, T mhs, T rhs) throws EX;
    protected abstract T unaryOperation(UnaryOperator operator, T operand) throws EX;
    protected abstract T binaryOperation(T lhs, BinaryOperator operator, T rhs) throws EX;
    protected abstract T fieldReference(T target, String fieldName) throws EX;
    protected abstract T staticFieldReference(Class<?> type, String fieldName) throws EX;
    protected abstract T methodInvocation(T target, String methodName, List<T> arguments) throws EX;
    protected abstract T staticMethodInvocation(Class<?> target, String methodName, List<T> arguments) throws EX;
    protected abstract T variableReference(String variableName) throws EX, ParseException;
    protected abstract T literal(@Nullable Object value) throws EX;
    protected abstract T parenthesized(T value) throws EX;
    protected abstract T instanceoF(T lhs, Class<?> rhs) throws EX;
    protected abstract T newClass(Class<?> clasS, List<T> arguments) throws EX;
    protected abstract T newArray(Class<?> clasS, List<T> dimensions) throws EX;
    protected abstract T cast(Class<?> targetType, T operand) throws EX, ParseException;
    protected abstract T arrayAccess(T lhs, T rhs) throws EX;

    // HELPERS.

    @Nullable private Class<?>
    loadImportedClass(String simpleClassName) {

        for (String si : this.singleImports) {
            if (si.endsWith("." + simpleClassName)) {
                Class<?> clasS = this.loadClass(si);
                if (clasS != null) return clasS;
            }
        }

        for (String iod : this.onDemandImports) {
            Class<?> clasS = this.loadClass(iod + '.' + simpleClassName);
            if (clasS != null) return clasS;
        }

        return null;
    }

    /**
     * @return {@code null} iff a class with the given name cannot be loaded
     */
    @Nullable private Class<?>
    loadClass(String qualifiedClassName) {
        try {
            return this.classLoader.loadClass(qualifiedClassName);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }
}
