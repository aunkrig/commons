
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

package de.unkrig.commons.util.logging;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.util.collections.MapUtil;

/**
 * Various {@code java.util.logging}-related utility methods.
 */
public final
class LogUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private LogUtil() {}

    /**
     * An alternative to repeated calls to {@link LogManager#getLogManager()}.
     */
    public static final LogManager LOG_MANAGER     = LogManager.getLogManager();
    private static final String[]  LOGGING_IMPORTS = {
        "java.lang",
        "java.io",
        "java.util",
        "java.util.logging",
        "de.unkrig.commons.util.logging",
    };

    /**
     * Is {@code Logger.getLogger("")}.
     */
    public static final Logger ROOT_LOGGER = Logger.getLogger("");

    /**
     * A {@link Level} one higher (towards {@link Level#SEVERE}) than {@link Level#WARNING}.
     */
    public static final Level  WARNING_PLUS_1 = Level.parse(Integer.toString(1 + Level.WARNING.intValue()));

    /**
     * A {@link Level} one higher (towards {@link Level#WARNING}) than {@link Level#INFO}.
     */
    public static final Level  INFO_PLUS_1 = Level.parse(Integer.toString(1 + Level.INFO.intValue()));

    /**
     * Strings passed to the returned {@link Consumer} are logged to the given logger at the given level.
     *
     * @param prefix Is prepended to each string before it is logged
     */
    public static Consumer<String>
    logConsumer(final Logger logger, final Level level, @Nullable final String prefix) {

        return new Consumer<String>() {

            @Override public void
            consume(String message) {
                if (prefix != null) message = prefix + message;

                logger.log(level, message);
            }
        };
    }

    /**
     * Lines written to the returned {@link Writer} are optionally prepended with the given prefix, and then logged
     * to the given {@link Logger} at the given {@link Level}.
     */
    public static Writer
    logWriter(Logger logger, Level level, @Nullable String prefix) {
        return ConsumerUtil.characterConsumerWriter(
            ConsumerUtil.lineAggregator(
                ConsumerUtil.<String, IOException>widen2(
                    LogUtil.logConsumer(logger, level, prefix)
                )
            )
        );
    }

    /**
     * @return A {@link Filter} that regards levels <i>lower</i> than {@code upperBound} as loggable
     */
    @NotNullByDefault(false) public static Filter
    levelLimitFilter(Level upperBound) {
        final int upperBoundValue = upperBound.intValue();
        return new Filter() {

            @Override public boolean
            isLoggable(LogRecord logRecord) {
                return logRecord.getLevel().intValue() < upperBoundValue;
            }
        };
    }

    /**
     * Regards levels less than {@link Level#WARNING} (e.g. INFO, CONFIG, FINE, FINER, FINEST) as loggable.
     */
    public static final Filter LESS_THAN_WARNING = LogUtil.levelLimitFilter(Level.WARNING);

    /**
     * Regards levels less than {@link Level#INFO} (e.g. CONFIG, FINE, FINER, FINEST) as loggable.
     */
    public static final Filter LESS_THAN_INFO = LogUtil.levelLimitFilter(Level.INFO);

    /**
     * Regards levels less than {@link Level#CONFIG} (e.g. FINE, FINER, FINEST) as loggable.
     */
    public static final Filter LESS_THAN_CONFIG = LogUtil.levelLimitFilter(Level.CONFIG);

    /**
     * @return The boolean value of the named logging property, or the {@code defaulT}
     */
    public static Boolean
    getLoggingProperty(String propertyName, Boolean defaulT) {
        String s = LogUtil.LOG_MANAGER.getProperty(propertyName);
        if (s == null) return defaulT;
        return Boolean.parseBoolean(s.trim());
    }

    /**
     * Evaluates the value of the named property to an object of the given {@code type} and returns it.
     *
     * @throws IllegalArgumentException The logging property is not defined
     * @throws EvaluationException      The property evaluates to {@code null}
     * @throws EvaluationException      The value of the property is not assignable to {@code T}
     */
    public static <T> T
    getLoggingProperty(String propertyName, Class<T> type) throws ParseException, EvaluationException {

        Map<String, Object> variables = MapUtil.map("propertyName", propertyName, "type", type);

        return new ExpressionEvaluator(variables.keySet()).setImports(LogUtil.LOGGING_IMPORTS).evaluateTo(
            LogUtil.requireLoggingProperty(propertyName),
            Mappings.fromMap(variables),
            type
        );
    }

    /**
     * Evaluates the value of the named property to an object of the given {@code type} and returns it, or the
     * {@code defaulT}.
     *
     * @see Parser The expression syntax of the property value
     */
    @Nullable public static <T> T
    getLoggingProperty(String propertyName, Class<T> type, @Nullable T defaulT)
    throws ParseException, EvaluationException {

        String spec = LogUtil.LOG_MANAGER.getProperty(propertyName);

        if (spec == null) return defaulT;

        Map<String, Object> variables = MapUtil.map("propertyName", propertyName, "type", type);

        return new ExpressionEvaluator(variables.keySet()).setImports(LogUtil.LOGGING_IMPORTS).evaluateTo(
            spec,
            Mappings.fromMap(variables),
            type
        );
    }

    /**
     * @return                          The level value of the named logging property, or the given {@code defaulT}
     * @throws IllegalArgumentException The value could not be parsed to a valid level
     */
    public static Level
    getLoggingProperty(String propertyName, Level defaulT) {
        String s = LogUtil.LOG_MANAGER.getProperty(propertyName);
        if (s == null) return defaulT;

        try {
            return Level.parse(s.trim());
        } catch (IllegalArgumentException iae) {
            return defaulT;
        }
    }

    /**
     * @return                          The value of the named logging property, converted to LONG, or the {@code
     *                                  defaulT}
     * @throws IllegalArgumentException The property text cannot be parsed into a LONG
     */
    public static long
    getLoggingProperty(String propertyName, Long defaulT) {
        String s = LogUtil.LOG_MANAGER.getProperty(propertyName);
        if (s == null) return defaulT;

        s = s.trim();
        Matcher m = LogUtil.QUANTITY_FORMAT.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                "Value '"
                + s
                + "'  of logging property '"
                + propertyName
                + "' is not an integer"
            );
        }

        long   result       = Long.parseLong(m.group(1));
        String metricPrefix = m.group(2);
        if (metricPrefix != null) {
            switch (metricPrefix.charAt(0)) {
            case 'k': result *= 1024L; break;
            case 'M': result *= 1024L * 1024L; break;
            case 'G': result *= 1024L * 1024L * 1024L; break;
            case 'T': result *= 1024L * 1024L * 1024L * 1024L; break;
            case 'P': result *= 1024L * 1024L * 1024L * 1024L * 1024L; break;
            }
        }
        return result;
    }
    private static final Pattern QUANTITY_FORMAT = Pattern.compile("(\\d+)([kMGTP])?");

    /**
     * @return The value of the named logging property, or the {@code defaulT}
     */
    @Nullable public static String
    getLoggingProperty(String propertyName, @Nullable String defaulT) {
        String result = LogUtil.LOG_MANAGER.getProperty(propertyName);
        if (result == null) return defaulT;
        return result;
    }

    /**
     * @return                          The value of the named logging property
     * @throws IllegalArgumentException The named logging property is not defined
     */
    public static String
    requireLoggingProperty(String propertyName) {
        String result = LogUtil.LOG_MANAGER.getProperty(propertyName);
        if (result == null) throw new IllegalArgumentException("Logging property '" + propertyName + "' missing");
        return result;
    }

    /**
     * Parses an expression from the value of the named logging property.
     *
     * @throws IllegalArgumentException The named logging property is not defined
     */
    public static Expression
    parseLoggingProperty(String propertyName, Predicate<? super String> isValidVariableNames) throws ParseException {

        return (
            new ExpressionEvaluator(isValidVariableNames)
            .setImports(LogUtil.LOGGING_IMPORTS)
            .parse(LogUtil.requireLoggingProperty(propertyName))
        );
    }
}
