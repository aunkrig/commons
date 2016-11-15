
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

package de.unkrig.commons.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link WritableByteChannel} which forwards the data to a delegate. The {@link FileBufferedChannel} will never
 * block because when the delegate is not writable, the data is buffered in a temporary file.
 */
@NotNullByDefault(false) public
class FileBufferedChannel implements WritableByteChannel {

    /**
     * @param delegate Must also be a {@link WritableByteChannel}
     */
    public
    FileBufferedChannel(Multiplexer multiplexer, SelectableChannel delegate) throws IOException {
        this.multiplexer = multiplexer;
        this.delegate    = delegate;
        this.file        = File.createTempFile("prefix", "suffix");
        this.file.deleteOnExit();
        this.buffer = new RandomAccessFile(this.file, "rw").getChannel();
    }

    @Override public synchronized int
    write(ByteBuffer src) throws IOException {
        if (!this.open) throw new ClosedChannelException();

        int n = this.buffer.write(src);
        if (n == 0) return 0;

        boolean flusherRegistered = this.head != this.tail;
        this.tail += n;

        if (!flusherRegistered) this.flusher.run();

        return n;
    }

    @Override public boolean
    isOpen() {
        return this.open;
    }

    @Override public void
    close() throws IOException {
        if (!this.open) return;
        this.open = false;
        if (this.head == this.tail) {
            this.buffer.close();
            this.file.delete();
        }
    }

    /** ??? */
    public synchronized long
    transferFrom(ReadableByteChannel src, long count) throws IOException {
        if (!this.open) throw new ClosedChannelException();

        long n = this.buffer.transferFrom(src, this.tail, count);
        if (n == 0L) return 0L;

        boolean flusherRegistered = this.head != this.tail;
        this.tail += n;

        if (!flusherRegistered) this.flusher.run();

        return n;
    }

    // CONFIGURATION

    private final Multiplexer                      multiplexer;
    private final SelectableChannel                delegate;
    private final FileChannel                      buffer;
    private final File                             file;

    // CONSTANTS

    private final RunnableWhichThrows<IOException> flusher = new RunnableWhichThrows<IOException>() {

        @Override public void
        run() throws IOException {
            for (;;) {

                long n = FileBufferedChannel.this.buffer.transferTo(
                    FileBufferedChannel.this.head,
                    FileBufferedChannel.this.tail - FileBufferedChannel.this.head,
                    (WritableByteChannel) FileBufferedChannel.this.delegate
                );

                if (n == 0) {

                    // No more data can be written to the delegate.
                    FileBufferedChannel.this.multiplexer.register(
                        FileBufferedChannel.this.delegate,
                        SelectionKey.OP_WRITE,
                        FileBufferedChannel.this.flusher
                    );
                    return;
                }

                synchronized (FileBufferedChannel.this) {
                    FileBufferedChannel.this.head += n;
                    if (FileBufferedChannel.this.head == FileBufferedChannel.this.tail) {

                        // The buffer is now empty.
                        if (!FileBufferedChannel.this.open) {
                            FileBufferedChannel.this.buffer.close();
                            FileBufferedChannel.this.file.delete();
                        }
                        return;
                    }
                }
            }
        }
    };

    // STATE

    private boolean open = true;
    private long    head, tail;
}
