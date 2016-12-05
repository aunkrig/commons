
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

package de.unkrig.commons.util.concurrent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.ProxyOutputStream;
import de.unkrig.commons.io.pipe.Pipe;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.InputOutputStreams;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Executes multiple tasks asynchronously, but forwards their output to a delegate {@link OutputStream} <en>in the
 * order the tasks were submitted</em>, i.e. the last byte of output of one task appears <em>before</em> the output
 * of all subsequently submitted tasks.
 */
@NotNullByDefault(false) public
class ByteStreamSequentializer {

    private final SquadExecutor<Void> squadExecutor;
    private Producer<? extends Pipe>  pipeProvider = new Producer<Pipe>() {
        @Override @Nullable public Pipe produce() { return PipeFactory.byteArrayRingBuffer(8192); }
    };

    /** The consumer to which the next task must write its output. */
    private OutputStream nextTarget;

    /** @see ByteStreamSequentializer */
    public
    ByteStreamSequentializer(OutputStream delegate, ExecutorService squadExecutor) {

        // The QuadExecutor will allow us to await the completion of all submitted tasks.
        this.squadExecutor = new SquadExecutor<Void>(squadExecutor);

        // The first task will write directly to the delegate.
        this.nextTarget = delegate;
    }

    /**
     * Sets an alternative producer that is called when another pipe is required.
     */
    void
    setPipeProvider(Producer<? extends Pipe> pipeProvider) {
        this.pipeProvider = pipeProvider;
    }

    /**
     * Data written to the <var>task</var>'s subject will be written to the {@link
     * #ByteStreamSequentializer(OutputStream, ExecutorService) delegate} <em>after<em> all data written by the
     * previously submitted tasks, and <em>before</em> the data written by all tasks submitted afterwards.
     * <p>
     *   Notice that when a <var>tasks</var> submits another task (subtask), then the output of the subtask will
     *   appear <em>after the output of the task</em> (and the output of all other tasks submitted in the meantime).
     *   If you want to have the output of subtasks to be <em>in the middle of the task's output</em>, then you should
     *   create a second {@link ByteStreamSequentializer} and await its completion.
     * </p>
     */
    public synchronized void
    submit(final Consumer<? super OutputStream> task) {
        this.submit(ConsumerUtil.widen2(task));
    }

    /**
     * @see #submit(Consumer)
     */
    public synchronized <EX extends Throwable> void
    submit(final ConsumerWhichThrows<? super OutputStream, EX> task) {

        final OutputStream previousTarget = this.nextTarget;

        final Pipe buffer = this.pipeProvider.produce();
        assert buffer != null;

        final InputOutputStreams ios = PipeUtil.asInputOutputStreams(buffer);
        final ProxyOutputStream  pos = new ProxyOutputStream(ios.getOutputStream());

        // The next task(s) will write to the proxy output stream.
        this.nextTarget = pos;

        this.squadExecutor.submit(new Callable<Void>() {

            @Override public Void
            call() throws Exception {

                // Run the task.
                try {
                    task.consume(OutputStreams.unclosable(previousTarget));
                } catch (Exception ex) {
                    throw ex;
                } catch (Error er) {     // SUPPRESS CHECKSTYLE IllegalCatch
                    throw er;
                } catch (Throwable th) { // SUPPRESS CHECKSTYLE IllegalCatch
                    throw new Exception(th);
                }

                synchronized (pos) {

                    // Copy the buffered products of the FOLLOWING tasks to the output.
                    IoUtil.copyAvailable(ios.getInputStream(), previousTarget);

                    // Redirect FUTURE output of the FOLLOWING tasks directly to MY output.
                    pos.setDelegate(previousTarget);
                    buffer.close();
                }

                return null;
            }
        });
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(Consumer)} and {@link
     * #submit(ConsumerWhichThrows)} have completed.
     *
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    public void
    awaitCompletion() throws InterruptedException, ExecutionException, CancellationException, IOException {

        this.nextTarget.close();
        this.squadExecutor.awaitCompletion();
    }

    /**
     * Returns when all tasks that were previously submitted with {@link #submit(Consumer)} and {@link
     * #submit(ConsumerWhichThrows)} have completed, or when then {@code timeout} expires.
     *
     * @throws CancellationException One of the tasks was cancelled
     * @throws ExecutionException    One of the tasks threw an exception
     * @throws InterruptedException  The current thread was interrupted while waiting
     */
    void
    awaitCompletion(long timeout, TimeUnit unit)
    throws CancellationException, ExecutionException, InterruptedException, IOException {

        this.nextTarget.close();
        this.squadExecutor.awaitCompletion(timeout, unit);
    }
}
