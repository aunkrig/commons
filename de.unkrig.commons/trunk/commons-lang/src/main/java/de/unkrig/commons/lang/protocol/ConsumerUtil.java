
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

package de.unkrig.commons.lang.protocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Consumer}-related utility methods.
 */
public final
class ConsumerUtil {

    private
    ConsumerUtil() {}

    /**
     * @return A consumer that forwards the subjects to both {@code delegate1} and {@code delegate2}
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    tee(final ConsumerWhichThrows<? super T, EX> delegate1, final ConsumerWhichThrows<? super T, EX> delegate2) {

        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) throws EX {
                delegate1.consume(subject);
                delegate2.consume(subject);
            }
        };
    }

    /**
     * @return A consumer that forwards the subjects to both {@code delegate1} and {@code delegate2}
     */
    public static <T> Consumer<T>
    tee(final Consumer<? super T> delegate1, final Consumer<? super T> delegate2) {

        return new Consumer<T>() {

            @Override public void
            consume(T subject) {
                delegate1.consume(subject);
                delegate2.consume(subject);
            }
        };
    }

    /**
     * @return A consumer that forwards the subjects to all the {@code delegates}
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    tee(final Collection<ConsumerWhichThrows<? super T, EX>> delegates) {

        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) throws EX {

                for (ConsumerWhichThrows<? super T, EX> delegate : delegates) {
                    delegate.consume(subject);
                }
            }
        };
    }

    /**
     * Converts the <var>source</var> into a {@link ConsumerWhichThrows ConsumerWhichThrows&lt;T, EX>}.
     * <p>
     *   This is always possible, because the <var>source</var> is only allowed to throw unchecked exceptions.
     * </p>
     *
     * @param <T>  The element type
     * @param <EX> The target consumer's exception
     * @deprecated Superseded by {@link #widen2(ConsumerWhichThrows)}.
     */
    @Deprecated public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    asConsumerWhichThrows(final Consumer<? super T> source) {

        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) { source.consume(subject); }
        };
    }

    /**
     * Converts the <var>source</var> into a {@link ConsumerWhichThrows ConsumerWhichThrows&lt;T, EX>}.
     * <p>
     *   This is always possible, because the <var>source</var> consumes a superclass of <var>T</var>, and the
     *   <var>source</var> is only allowed to throw a subclass of <var>EX</var>.
     * </p>
     *
     * @param <T>  The element type
     * @param <EX> The target consumer's exception
     * @deprecated Not necessary if you declare variables, fields an parameters as "{@code ConsumerWhichThrows<? super
     *             consumed-type, ? extends thrown-exception>}"
     */
    @Deprecated public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    widen(final ConsumerWhichThrows<? super T, ? extends EX> source) {

        @SuppressWarnings("unchecked") ConsumerWhichThrows<T, EX> result = (ConsumerWhichThrows<T, EX>) source;

        return result;
    }

    /**
     * Converts the <var>source</var> into a {@link ConsumerWhichThrows ConsumerWhichThrows&lt;T, EX>}.
     * <p>
     *   This is always possible, because the <var>source</var> consumes a superclass of <var>T</var>, and the
     *   <var>source</var> is only allowed to throw a {@code RuntimeException}s.
     * </p>
     *
     * @param <T>  The element type
     * @param <EX> The target consumer's exception
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    widen2(final ConsumerWhichThrows<? super T, ? extends RuntimeException> source) {

        @SuppressWarnings("unchecked") ConsumerWhichThrows<T, EX> result = (ConsumerWhichThrows<T, EX>) source;

        return result;
    }

    /**
     * Converts the <var>source</var> into a {@link Consumer Consumer&lt;T>}.
     * <p>
     *   This is always possible, because both ar allowed to throw {@link RuntimeException}s.
     * </p>
     *
     * @param <T>  The element type
     * @param <EX> The source consumer's exception
     */
    public static <T, EX extends RuntimeException> Consumer<T>
    asConsumer(final ConsumerWhichThrows<? super T, ? extends RuntimeException> source) {

        @SuppressWarnings("unchecked") Consumer<T> result = (Consumer<T>) source;

        return result;
    }

    /**
     * @return A {@link Writer} which forwards the characters to a {@link ConsumerWhichThrows
     *         ConsumerWhichThrows&lt;Character, IOException>}
     */
    @NotNullByDefault(false) public static Writer
    characterConsumerWriter(final ConsumerWhichThrows<? super Character, IOException> delegate) {
        return new Writer() {

            @Override public void
            write(int c) throws IOException {
                delegate.consume((char) c);
            }

            @Override public void
            write(char[] cbuf, int off, int len) throws IOException {
                for (; len > 0; len--) this.write(cbuf[off++]);
            }

            @Override public void
            flush() {
            }

            @Override public void
            close() {
            }
        };
    }

    /**
     * Creates and returns a {@link Consumer Consumer&lt;Character>} which aggregates characters to lines, which it
     * passes to the {@code delegate}.
     * <p>
     *   Notice that iff the last consumed character is neither a CR nor an LF (a.k.a. "last line lacks a line
     *   separator"), then that last line will not be sent to the <var>delegate</var>.
     * </p>
     */
    public static <E extends Exception> ConsumerWhichThrows<Character, E>
    lineAggregator(final ConsumerWhichThrows<? super String, E> delegate) {

        return new ConsumerWhichThrows<Character, E>() {

            private final StringBuilder sb = new StringBuilder();
            private boolean             crPending;

            @Override public void
            consume(Character c) throws E {
                if (c == '\r') {
                    delegate.consume(this.sb.toString());
                    this.sb.setLength(0);
                    this.crPending = true;
                } else
                if (c == '\n') {
                    if (this.crPending) {
                        this.crPending = false;
                    } else {
                        delegate.consume(this.sb.toString());
                        this.sb.setLength(0);
                    }
                } else
                {
                    this.crPending = false;
                    this.sb.append(c);
                }
            }
        };
    }

    /**
     * @return A {@link Consumer} that writes lines to the given file with the default character encoding
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumer(File file, boolean append) throws IOException {
        return ConsumerUtil.lineConsumer(new FileWriter(file, append), true);
    }

    /**
     * @return A {@link Consumer} that writes lines to the given file with the given encoding
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumer(File file, String charsetName, boolean append) throws IOException {
        return ConsumerUtil.lineConsumer(new FileOutputStream(file, append), charsetName, true);
    }

    /**
     * @return A {@link Consumer} that writes lines to the given {@link OutputStream} with the given encoding
     */
    private static ConsumerWhichThrows<String, IOException>
    lineConsumer(OutputStream stream, String charsetName, boolean closeOnFinalize)
    throws UnsupportedEncodingException {
        return ConsumerUtil.lineConsumer(new OutputStreamWriter(stream, charsetName), closeOnFinalize);
    }

    /**
     * @return A {@link Consumer} that writes strings to the given {@link Writer}, augmented with a line separator
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumer(final Writer writer, final boolean closeOnFinalize) {

        return new ConsumerWhichThrows<String, IOException>() {

            @Override public void
            consume(String line) throws IOException {
                writer.write(line + ConsumerUtil.LINE_SEPARATOR); // Write line and line separator atomically.
                writer.flush();
            }

            @Override protected void
            finalize() throws Throwable { if (closeOnFinalize) writer.close(); }
        };
    }
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * @return A {@link Consumer} that writes strings to the given {@link Writer}, augmented with a line separator
     */
    public static Consumer<String>
    lineConsumer(final PrintStream printStream, final boolean closeOnFinalize) {

        return new Consumer<String>() {
            @Override public void
            consume(String line) { printStream.println(line); }

            @Override protected void
            finalize() { if (closeOnFinalize) printStream.close(); }

            @Override public String
            toString() { return printStream.toString(); }
        };
    }

    /**
     * The returned producer is a factory for consumers of {@code T}. The subjects sent to these consumers are
     * forwarded immediately to the given {@code target}.
     *
     * @see #combineInOrder(ConsumerWhichThrows)
     */
    public static <T> Producer<Consumer<T>>
    combine(final Consumer<? super T> target) {

        return new Producer<Consumer<T>>() {

            @Override public Consumer<T>
            produce() {

                return new Consumer<T>() {

                    @Override public void
                    consume(T subject) {
                        target.consume(subject);
                    }
                };
            }
        };
    }

    /**
     * The returned producer is a factory for consumers of {@code T}. The first subject sent to each of these consumers
     * is passed to the given {@code target} <i>in the order the consumers were created</i> (not in the order in
     * which the subjects were sent to the consumers).
     *
     * @see #combine(Consumer)
     */
    public static <T, EX extends Throwable> Producer<ConsumerWhichThrows<T, EX>>
    combineInOrder(final ConsumerWhichThrows<? super T, EX> target) {

        final Queue<ConsumerWhichThrows<? super T, EX>>
        outstanding = new LinkedList<ConsumerWhichThrows<? super T, EX>>();

        final Map<ConsumerWhichThrows<? super T, EX>, T>
        postponed = new HashMap<ConsumerWhichThrows<? super T, EX>, T>();

        return new Producer<ConsumerWhichThrows<T, EX>>() {

            @Override public ConsumerWhichThrows<T, EX>
            produce() {

                ConsumerWhichThrows<T, EX> consumer = new ConsumerWhichThrows<T, EX>() {

                    @Override public void
                    consume(T subject) throws EX {
                        synchronized (outstanding) {
                            if (outstanding.isEmpty()) {
                                throw new IllegalStateException("Can consume only one subject");
                            }
                            if (outstanding.peek() == this) {

                                // The call appeared "at the right time"; pass the subject immediately to the
                                // target consumer.
                                outstanding.remove();
                                target.consume(subject);

                                // Flush as many postponed results as possible.
                                while (!outstanding.isEmpty() && postponed.containsKey(outstanding.element())) {
                                    target.consume(postponed.get(outstanding.remove()));
                                }
                            } else {

                                // The invocation happened "too early"; store the subject only to pass it to the the
                                // target when the time is right.
                                if (postponed.containsKey(this)) {
                                    throw new IllegalStateException("Can consume only one subject");
                                }
                                postponed.put(this, subject);
                            }
                        }
                    }
                };
                synchronized (outstanding) {
                    outstanding.add(consumer);
                }
                return consumer;
            }
        };
    }

    /**
     * Returns a list of consumers of size {@code n}. When all comsumers have consumed their first subject, then these
     * subjects are passed to the {@code target} consumer; then again when all consumers have consumed their second
     * subject, and so on.
     */
    public static <T, EX extends Throwable> List<ConsumerWhichThrows<T, EX>>
    splice(final int n, final ConsumerWhichThrows<? super List<T>, EX> target) {
        final List<Queue<T>> buffers = new ArrayList<Queue<T>>(n);

        List<ConsumerWhichThrows<T, EX>> consumers = new ArrayList<ConsumerWhichThrows<T, EX>>(n);
        for (int i = 0; i < n; i++) {
            consumers.add(new ConsumerWhichThrows<T, EX>() {

                final Queue<T> buffer = new LinkedList<T>();
                { buffers.add(this.buffer); }

                @Override public void
                consume(T subject) throws EX {
                    synchronized (buffers) {
                        this.buffer.add(subject);

                        // Verify that all buffers contain at least one element.
                        for (Queue<T> b : buffers) {
                            if (b.isEmpty()) return;
                        }

                        // Move the HEAD elements of all buffers to a temporary list.
                        List<T> subjects = new ArrayList<T>(n);
                        for (Queue<T> b : buffers) {
                            subjects.add(b.remove());
                        }

                        // Pass the list to the target consumer.
                        target.consume(subjects);
                    }
                }
            });
        }
        return consumers;
    }

    /**
     * @return A consumer that adds the subjects it consumes to the given collection
     */
    public static <T> Consumer<T>
    addToCollection(final Collection<T> drain) {

        return new Consumer<T>() {

            @Override public void
            consume(T subject) { drain.add(subject); }
        };
    }

    /**
     * @return A {@link ConsumerWhichThrows} which throws each subject it consumes
     */
    public static <EX extends Throwable> ConsumerWhichThrows<EX, EX>
    throwsSubject() {

        return new ConsumerWhichThrows<EX, EX>() {
            @Override public void consume(EX throwable) throws EX { throw throwable; }
        };
    }

    /**
     * The combination of a {@link Producer} and a {@link Consumer}.
     *
     * @param <PT> The type that the {@link Produmer} <em>produces</em>
     * @param <CT> The type that the {@link Produmer} <em>consumes</em>
     */
    public
    interface Produmer<PT, CT> extends Producer<PT>, Consumer<CT> {}

    /**
     * The returned {@link Produmer} simply produces the <i>last consumed subject</i>, or {@code null} if no subject
     * has been consumed yet.
     */
    public static <T> Produmer<T, T>
    store() {

        return new Produmer<T, T>() {

            @Nullable private T store;

            @Override public void
            consume(T subject) { this.store = subject; }

            @Override @Nullable public T
            produce() { return this.store; }
        };
    }

    /**
     * Creates and returns a {@link Consumer} which forwards the <i>cumulated</i> quantity to the given {@code
     * delegate}.
     *
     * @param initialCount Initial value for the cumulated quantity, usually {@code 0L}
     */
    public static Consumer<Number>
    cumulate(final Consumer<? super Long> delegate, final long initialCount) {

        return new Consumer<Number>() {

            long count = initialCount;

            @Override public void
            consume(Number n) { delegate.consume((this.count += n.longValue())); }
        };
    }

    /**
     * Creates and returns a {@link Consumer Consumer&lt;Long>} which forwards the quantity to the given {@code
     * delegate}, but only if the quantity is equal to or greater than the limit, which starts with {@code
     * initialLimit} and increases exponentially.
     */
    public static Consumer<Long>
    compressExponentially(final long initialLimit, final Consumer<? super Long> delegate) {

        return new Consumer<Long>() {

            long limit = initialLimit;

            @Override public void
            consume(Long n) {

                if (n >= this.limit) {

                    delegate.consume(n);

                    do {
                        this.limit <<= 1;
                    } while (n >= this.limit);
                }
            }
        };
    }

    /**
     * Forwards each <var>subject</var> it consumes to the given <var>delegate</var>, but only iff the
     * <var>subject</var> is not <var>compressable</var>.
     */
    public static <T> Consumer<T>
    compress(final Consumer<T> delegate, final Predicate<T> compressable) {

        return new Consumer<T>() {

            @Override public void
            consume(T subject) {  if (!compressable.evaluate(subject)) delegate.consume(subject); }
        };
    }

    /**
     * Replaces sequences of <var>compressable</var> subjects with <i>one</i> <var>compressed</var> subject.
     * <p>
     *   Leading and trailing <var>compressable</var>s are discarded.
     * </p>
     */
    public static <T> Consumer<T>
    compress(final Consumer<? super T> delegate, final Predicate<? super T> compressable, final T compressed) {

        return new Consumer<T>() {

            int state;

            @Override public void
            consume(T subject) {

                boolean isCompressable = compressable.evaluate(subject);

                if (isCompressable) {
                    if (this.state == 1) this.state = 2;
                } else {
                    if (this.state == 2) {
                        delegate.consume(compressed);
                    }
                    delegate.consume(subject);
                    this.state = 1;
                }
            }
        };
    }

    @SuppressWarnings("unchecked") public static <T> Consumer<T>
    nop() { return (Consumer<T>) ConsumerUtil.NOP; }

    private static final Consumer<?> NOP = new Consumer<Object>() {

        @Override public void
        consume(Object subject) {
            ;
        }
    };

    /**
     * Wraps the <var>delegate</var> such that its declared exception is caught and ignored.
     */
    public static <T, EX extends Throwable> Consumer<T>
    ignoreExceptions(final Class<EX> exceptionClass, final ConsumerWhichThrows<T, EX> delegate) {

        return new Consumer<T>() {

            @Override public void
            consume(@Nullable T subject) {

                assert subject != null;
                try {
                    delegate.consume(subject);
                } catch (RuntimeException re) {
                    if (!exceptionClass.isAssignableFrom(re.getClass())) throw re;
                    ;
                } catch (Error e) {     // SUPPRESS CHECKSTYLE IllegalCatch
                    if (!exceptionClass.isAssignableFrom(e.getClass())) throw e;
                    ;
                } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                    assert exceptionClass.isAssignableFrom(t.getClass());
                    ;
                }
            }
        };
    }
}