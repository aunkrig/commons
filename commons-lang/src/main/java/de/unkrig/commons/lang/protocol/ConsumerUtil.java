
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
     * @return A consumer that forwards the subjects to both <var>delegate1</var> and <var>delegate2</var>
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
    tee(
        final ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        final ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) {

        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) throws EX {
                delegate1.consume(subject);
                delegate2.consume(subject);
            }
        };
    }

    /**
     * @return A consumer that forwards the subjects to both <var>delegate1</var> and <var>delegate2</var>
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
     * @return A consumer that forwards the subjects to all the <var>delegates</var>
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, ? extends EX>
    tee(final Collection<ConsumerWhichThrows<? super T, ? extends EX>> delegates) {

        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) throws EX {

                for (ConsumerWhichThrows<? super T, ? extends EX> delegate : delegates) {
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
    @Deprecated public static <T, EX extends Throwable> ConsumerWhichThrows<? super T, ? extends EX>
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
    @Deprecated public static <T, EX extends Throwable> ConsumerWhichThrows<? super T, ? extends EX>
    widen(final ConsumerWhichThrows<? super T, ? extends EX> source) {

        @SuppressWarnings("unchecked") ConsumerWhichThrows<T, EX> result = (ConsumerWhichThrows<T, EX>) source;

        return result;
    }

    /**
     * Converts the <var>source</var> into a {@link ConsumerWhichThrows ConsumerWhichThrows&lt;T, EX>}.
     * <p>
     *   This is always possible, because the <var>source</var> consumes a superclass of <var>T</var>, and the
     *   <var>source</var> is only allowed to throw a {@link RuntimeException}.
     * </p>
     *
     * @param <T>  The element type
     * @param <EX> The target consumer's exception
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<T, ? extends EX>
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
    characterConsumerWriter(final ConsumerWhichThrows<? super Character, ? extends IOException> delegate) {

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
     * passes to the <var>delegate</var>.
     * <p>
     *   Notice that iff the last consumed character is neither a CR nor an LF (a.k.a. "last line lacks a line
     *   separator"), then that last line will not be sent to the <var>delegate</var>.
     * </p>
     */
    public static <EX extends Throwable> ConsumerWhichThrows<? super Character, ? extends EX>
    lineAggregator(final ConsumerWhichThrows<String, EX> delegate) {

        return new ConsumerWhichThrows<Character, EX>() {

            private final StringBuilder sb = new StringBuilder();
            private boolean             crPending;

            @Override public void
            consume(Character c) throws EX {
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
     * Invokes <var>lineChar</var> for every non-line-terminator char, and <var>lineComplete</var> for every line
     * terminator.
     * <p>
     *   Notice that a line terminator could comprise <em>two</em> chars ({@code '\r', '\n'}); then,
     *   <var>lineComplete</var> is only invoked once (on the {@code '\r'}).
     * </p>
     */
    public static <EX extends Throwable> ConsumerWhichThrows<Character, ? extends EX>
    lineCounter(
        ConsumerWhichThrows<Character, ? extends EX> lineChar,
        ConsumerWhichThrows<Integer, ? extends EX>   lineComplete
    ) {

        return new ConsumerWhichThrows<Character, EX>() {

            int     lineNumber = 1;
            boolean crPending;

            @Override public void
            consume(Character c) throws EX {
                if (c == '\n') {
                    if (this.crPending) {
                        this.crPending = false;
                    } else {
                        lineComplete.consume(this.lineNumber++);
                    }
                } else
                if (c == '\r') {
                    this.crPending = true;
                    lineComplete.consume(this.lineNumber++);
                } else
                {
                    lineChar.consume(c);
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
     * forwarded immediately to the given <var>target</var>.
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
     * is passed to the given <var>target</var> <i>in the order the consumers were created</i> (not in the order in
     * which the subjects were sent to the consumers).
     *
     * @see #combine(Consumer)
     */
    public static <T, EX extends Throwable> Producer<? extends ConsumerWhichThrows<? super T, ? extends EX>>
    combineInOrder(final ConsumerWhichThrows<? super T, ? extends EX> target) {

        final Queue<ConsumerWhichThrows<T, EX>>
        outstanding = new LinkedList<ConsumerWhichThrows<T, EX>>();

        final Map<ConsumerWhichThrows<T, EX>, T>
        postponed = new HashMap<ConsumerWhichThrows<T, EX>, T>();

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
     * Returns a list of consumers of size <var>n</var>. When all comsumers have consumed their first subject, then
     * these subjects are passed to the <var>target</var> consumer; then again when all consumers have consumed their
     * second subject, and so on.
     */
    public static <T, EX extends Throwable> List<ConsumerWhichThrows<? super T, ? extends EX>>
    splice(final int n, final ConsumerWhichThrows<? super List<T>, ? extends EX> target) {
        final List<Queue<T>> buffers = new ArrayList<Queue<T>>(n);

        List<ConsumerWhichThrows<? super T, ? extends EX>>
        consumers = new ArrayList<ConsumerWhichThrows<? super T, ? extends EX>>(n);

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
    addToCollection(final Collection<? super T> drain) {

        return new Consumer<T>() {

            @Override public void
            consume(T subject) { drain.add(subject); }
        };
    }

    /**
     * @return A {@link ConsumerWhichThrows} which throws each subject it consumes
     */
    public static <EX extends Throwable> ConsumerWhichThrows<? super EX, ? extends EX>
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
    interface Produmer<PT, CT>
    extends ProdumerWhichThrows<PT, NoException, CT, NoException> {}

    /**
     * The combination of a {@link ProducerWhichThrows} and a {@link ConsumerWhichThrows}.
     *
     * @param <PT>  The type that the {@link ProdumerWhichThrows} <em>produces</em>
     * @param <PEX> The exception that {@link #produce()} may throw
     * @param <CT>  The type that the {@link ProdumerWhichThrows} <em>consumes</em>
     * @param <CEX> The exception that {@link #consume(Object)} may throw
     */
    public
    interface ProdumerWhichThrows<PT, PEX extends Throwable, CT, CEX extends Throwable>
    extends ProducerWhichThrows<PT, PEX>, ConsumerWhichThrows<CT, CEX> {}

    /**
     * Equivalent with {@link #store(Object) store(null)}.
     */
    public static <T> Produmer<T, T>
    store() { return ConsumerUtil.store(null); }

    /**
     * The returned {@link Produmer} simply produces the <i>last consumed subject</i>, or <var>initialValue</var> if no
     * subject has been consumed yet.
     */
    public static <T> Produmer<T, T>
    store(@Nullable final T initialValue) {

        return new Produmer<T, T>() {

            @Nullable private T store = initialValue;

            @Override public void
            consume(T subject) { this.store = subject; }

            @Override @Nullable public T
            produce() { return this.store; }
        };
    }

    /**
     * Equivalent with {@link #cumulate(ConsumerWhichThrows, long) cumulate(delegate)}.
     */
    public static <EX extends Throwable> ConsumerWhichThrows<? super Number, ? extends EX>
    cumulate(final ConsumerWhichThrows<? super Long, ? extends EX> delegate) {
        return ConsumerUtil.cumulate(delegate, 0);
    }

    /**
     * Creates and returns a {@link Consumer} which forwards the <i>cumulated</i> quantity to the given {@code
     * delegate}.
     *
     * @param initialCount Initial value for the cumulated quantity, usually {@code 0L}
     */
    public static <EX extends Throwable> ConsumerWhichThrows<? super Number, ? extends EX>
    cumulate(final ConsumerWhichThrows<? super Long, ? extends EX> delegate, final long initialCount) {

        return new ConsumerWhichThrows<Number, EX>() {

            long count = initialCount;

            @Override public void
            consume(Number n) throws EX { delegate.consume((this.count += n.longValue())); }
        };
    }

    /**
     * Equivalent with {@link #cumulate(long) cumulate(0L)}.
     */
    public static Produmer<Long, Number>
    cumulate() { return ConsumerUtil.cumulate(0L); }

    /**
     * Creates and returns a {@link Produmer} which adds up the quantities it consumes, and produces the current
     * total.
     *
     * @param initialValue Initial value for the cumulated quantity, usually {@code 0L}
     */
    public static Produmer<Long, Number>
    cumulate(final long initialValue) {

        return new Produmer<Long, Number>() {

            long count = initialValue;

            @Override public void
            consume(Number n) { this.count += n.longValue(); }

            @Override @Nullable public Long
            produce() { return this.count; }
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
    public static <T, EX extends Throwable> Consumer<? super T>
    ignoreExceptions(
        final Class<? extends EX>                          exceptionClass,
        final ConsumerWhichThrows<? super T, ? extends EX> delegate
    ) {

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

    /**
     * Passes the first <var>n</var> elements of the <var>subject</var> to <var>delegate1</var>, and all remaining
     * elements (if any) to <var>delegate2</var>.
     * <p>
     *   This method resembles UNIX's {@code head} command.
     * </p>
     */
    public static <T, EX extends Throwable> void
    head(
        Iterable<? extends T>                        subject,
        int                                          n,
        ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) throws EX { ConsumerUtil.head(subject.iterator(), n, delegate1, delegate2); }

    /**
     * Passes the first <var>n</var> products of the <var>subject</var> to <var>delegate1</var>, and all remaining
     * products (if any) to <var>delegate2</var>.
     * <p>
     *   This method resembles UNIX's {@code head} command.
     * </p>
     */
    public static <T, EX extends Throwable> void
    head(
        Iterator<? extends T>                        subject,
        int                                          n,
        ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) throws EX {

        ConsumerWhichThrows<? super T, ? extends EX> c = ConsumerUtil.head(n, delegate1, delegate2);

        while (subject.hasNext()) c.consume(subject.next());
    }

    /**
     * @return A consumer that forwards the first <var>n</var> consumed subjects to <var>delegate1</var>, and all
     *         following consumed subjects to <var>delegate2</var>
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<? super T, ? extends EX>
    head(
        final int                                          n,
        final ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        final ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) {

        return new ConsumerWhichThrows<T, EX>() {

            final AtomicInteger count = new AtomicInteger();

            @Override public void
            consume(T subject) throws EX {
                if (this.count.getAndIncrement() < n) {
                    delegate1.consume(subject);
                } else {
                    delegate2.consume(subject);
                }
            }
        };
    }

    /**
     * @return A consumer that forwards the first <var>n</var> consumed subjects to the <var>delegate</var>, and
     *         ignores all following consumed objects
     */
   public static <T, EX extends Throwable> ConsumerWhichThrows<T, EX>
   head(
       final int                                          n,
       final ConsumerWhichThrows<? super T, ? extends EX> delegate
   ) {

      return new ConsumerWhichThrows<T, EX>() {

         private final AtomicInteger remaining = new AtomicInteger(n);

         @Override public void
         consume(T subject) throws EX {
            if (this.remaining.getAndDecrement() > 0) delegate.consume(subject);
         }
      };
   }

    /**
     * Passes the first <var>x - n</var> products of the <var>subject</var> to <var>delegate1</var>, and all
     * remaining products (if any) to <var>delegate2</var>, where <var>x</var> is the number of products of the
     * <var>subject</var>.
     * <p>
     *   This method resembles UNIX's {@code tail} command.
     * </p>
     */
    public static <T, EX extends Throwable> void
    tail(
        Iterator<? extends T>                        subject,
        int                                          n,
        ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) throws EX {

        List<T> tmp = new ArrayList<T>();
        while (subject.hasNext()) tmp.add(subject.next());

        ConsumerUtil.tail(tmp, n, delegate1, delegate2);
    }

    /**
     * Passes the first <var>subject</var>{@code .length()} - <var>n</var> elements of the <var>subject</var>
     * collection to <var>delegate1</var>, and all remaining elements (if any) to <var>delegate2</var>.
     * <p>
     *   This method resembles UNIX's {@code tail} command.
     * </p>
     */
    public static <T, EX extends Throwable> void
    tail(
        Collection<? extends T>                      subject,
        int                                          n,
        ConsumerWhichThrows<? super T, ? extends EX> delegate1,
        ConsumerWhichThrows<? super T, ? extends EX> delegate2
    ) throws EX {

        int size = subject.size();

        Iterator<? extends T> it = subject.iterator();

        for (int i = 0; i < size - n && it.hasNext(); i++) {
            delegate1.consume(it.next());
        }
        while (it.hasNext()) {
            delegate2.consume(it.next());
        }
    }

    /**
     * @return Counts the number of subjects consumed so far
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<? super T, ? extends EX>
    count(final AtomicInteger delegate) {

       return new ConsumerWhichThrows<T, EX>() {
          @Override public void consume(T subject) { delegate.incrementAndGet(); }
       };
    }

    /**
     * @return A consumer that delegates its subjects' {@link Tuple2#first first} field to the <var>delegate</var>
     */
    public static <T1, T2, EX extends Throwable> ConsumerWhichThrows<? super Tuple2<T1, T2>, ? extends EX>
    getFirstOfTuple2(final ConsumerWhichThrows<? super T1, ? extends EX> delegate) {

       return new ConsumerWhichThrows<Tuple2<T1, T2>, EX>() {

          @Override public void
          consume(Tuple2<T1, T2> subject) throws EX { delegate.consume(subject.first); }
       };
    }

    /**
     * @return A consumer that delegates its subjects' {@link Tuple2#second second} field to the <var>delegate</var>
     */
    public static <T1, T2, EX extends Throwable> ConsumerWhichThrows<? super Tuple2<T1, T2>, ? extends EX>
    getSecondOfTuple2(final ConsumerWhichThrows<? super T2, ? extends EX> delegate) {

       return new ConsumerWhichThrows<Tuple2<T1, T2>, EX>() {

          @Override public void
          consume(Tuple2<T1, T2> subject) throws EX { delegate.consume(subject.second); }
       };
    }

    /**
     * @return A consumer that delegates its subjects' {@link Tuple3#first first} field to the <var>delegate</var>
     */
    public static <T1, T2, T3, EX extends Throwable> ConsumerWhichThrows<? super Tuple3<T1, T2, T3>, ? extends EX>
    getFirstOfTuple3(final ConsumerWhichThrows<? super T1, ? extends EX> delegate) {

        return new ConsumerWhichThrows<Tuple3<T1, T2, T3>, EX>() {

            @Override public void
            consume(Tuple3<T1, T2, T3> subject) throws EX { delegate.consume(subject.first); }
        };
    }

    /**
     * @return A consumer that delegates its subjects' {@link Tuple3#second second} field to the <var>delegate</var>
     */
    public static <T1, T2, T3, EX extends Throwable> ConsumerWhichThrows<? super Tuple3<T1, T2, T3>, ? extends EX>
    getSecondOfTuple3(final ConsumerWhichThrows<? super T2, ? extends EX> delegate) {

        return new ConsumerWhichThrows<Tuple3<T1, T2, T3>, EX>() {

            @Override public void
            consume(Tuple3<T1, T2, T3> subject) throws EX { delegate.consume(subject.second); }
        };
    }

    /**
     * @return A consumer that delegates its subjects' {@link Tuple3#third third} field to the <var>delegate</var>
     */
    public static <T1, T2, T3, EX extends Throwable> ConsumerWhichThrows<? super Tuple3<T1, T2, T3>, ? extends EX>
    getThirdOfTuple3(final ConsumerWhichThrows<? super T3, ? extends EX> delegate) {

        return new ConsumerWhichThrows<Tuple3<T1, T2, T3>, EX>() {

            @Override public void
            consume(Tuple3<T1, T2, T3> subject) throws EX { delegate.consume(subject.third); }
        };
    }

    /**
     * Puts the second element into the <var>delegate</var> map, with the first element as the key.
     */
    public static <T1, T2, EX extends Throwable> ConsumerWhichThrows<Tuple2<T1, T2>, ? extends EX>
    put(final Map<T1, T2> delegate) {

        return new ConsumerWhichThrows<Tuple2<T1, T2>, EX>() {

            @Override public void
            consume(Tuple2<T1, T2> subject) { delegate.put(subject.first, subject.second); }
        };
    }

    /**
     * Puts the second and third element as a {@link Tuple2} into the <var>delegate</var> map, with the first element
     * as the key.
     */
    public static <T1, T2, T3, EX extends Throwable> ConsumerWhichThrows<Tuple3<T1, T2, T3>, ? extends EX>
    putAsTuple2(final Map<T1, Tuple2<T2, T3>> delegate) {

        return new ConsumerWhichThrows<Tuple3<T1, T2, T3>, EX>() {

            @Override public void
            consume(Tuple3<T1, T2, T3> subject) {
                delegate.put(subject.first, new Tuple2<T2, T3>(subject.second, subject.third));
            }
        };
    }

    /**
     * @return A consumer that adds each subject it receives to the given <var>delegate</var>
     */
    public static <T extends Number> Consumer<? super T>
    add(final AtomicInteger delegate) {

        return new Consumer<T>() {

            @Override public void
            consume(T subject) { delegate.addAndGet(subject.intValue()); }
        };
    }

    /**
     * @return A consumer that adds each subject it receives to the given <var>delegate</var>
     */
    public static <T extends Number> Consumer<? super T>
    add(final AtomicLong delegate) {

        return new Consumer<T>() {
            @Override public void
            consume(T subject) { delegate.addAndGet(subject.longValue()); }
        };
    }

    /**
     * @return A consumer that forwards each consumed subjects to either the <var>trueDelegate</var> or the
     *         <var>falseDelegate</var>
     */
    public static <T, EX extends Throwable> ConsumerWhichThrows<? super T, ? extends EX>
    conditional(
        final PredicateWhichThrows<? super T, ? extends EX> condition,
        final ConsumerWhichThrows<? super T, ? extends EX>  trueDelegate,
        final ConsumerWhichThrows<? super T, ? extends EX>  falseDelegate
    ) {
        return new ConsumerWhichThrows<T, EX>() {

            @Override public void
            consume(T subject) throws EX {
                if (condition.evaluate(subject)) {
                    trueDelegate.consume(subject);
                } else {
                    falseDelegate.consume(subject);
                }
            }
        };
    }
}
