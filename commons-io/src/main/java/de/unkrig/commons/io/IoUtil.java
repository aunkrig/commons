
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

package de.unkrig.commons.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ClassLoaders;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerUtil.Produmer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@code java.io}-related utility methods.
 */
public final
class IoUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final Logger LOGGER = Logger.getLogger(IoUtil.class.getName());

    private
    IoUtil() {}

    /**
     * Finds and returns a named resource along a "path", e.g. a Java "class path" or "source path".
     *
     * @param path Each element should designate either a directory or a JAR (or ZIP) file
     * @return     {@code null} if {@code path == null}, or if the resource could not be found
     */
    @Nullable public static URL
    findOnPath(@Nullable File[] path, String resourceName) throws IOException {

        if (path == null) return null;

        for (File directoryOrArchiveFile : path) {
            if (directoryOrArchiveFile.isDirectory()) {

                // The path entry is a directory; look for a file designated by the directory plus the resource name.
                File file = new File(directoryOrArchiveFile, resourceName);
                if (file.isFile()) {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException mue) {
                        AssertionError ae = new AssertionError(resourceName);
                        ae.initCause(mue);
                        throw ae;
                    }
                }
            } else
            if (directoryOrArchiveFile.isFile()) {

                // The path entry is a (regular) file; assume that the file is a JAR (or ZIP) archive file, and look
                // for an archive entry with the name equal to the resource name.
                URL url = new URL("jar", null, directoryOrArchiveFile.toURI().getPath() + "!/" + resourceName);
                try {
                    url.openConnection().connect();
                } catch (FileNotFoundException fnfe) {
                    return null;
                }
                return url;
            } else
            {

                // The path entry designates neither a directory nor a JAR (or ZIP) archive.
                ;
            }
        }

        return null;
    }

    /**
     * Reads the input stream until end-of-input and writes all data to the output stream. Closes none of the two
     * streams.
     *
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        return IoUtil.copy(inputStream, outputStream, Long.MAX_VALUE);
    }

    /**
     * Reads at most <var>n</var> bytes from the <var>inputStream</var> and writes all data to the
     * <var>outputStream</var>. Closes none of the two streams.
     *
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, OutputStream outputStream, long n) throws IOException {
        byte[] buffer = new byte[4096];
        long   count  = 0L;
        while (n > 0) {
            try {
                IoUtil.LOGGER.log(Level.FINEST, "About to ''read(byte[{0}])''", buffer.length);
                int m = inputStream.read(buffer, 0, (int) Math.min(n, buffer.length));
                IoUtil.LOGGER.log(Level.FINEST, "''read()'' returned {0}", m);
                if (m == -1) break;
                IoUtil.LOGGER.log(Level.FINEST, "About to ''write(byte[{0}])''", m);
                outputStream.write(buffer, 0, m);
                IoUtil.LOGGER.log(Level.FINEST, "'write()' returned");
                count += m;
                n     -= m;
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap(count + " bytes copied so far", ioe);
            }
        }

        outputStream.flush();

        IoUtil.LOGGER.log(Level.FINEST, "{0} bytes copied", count);
        return count;
    }

    /**
     * Copies the contents of the <var>inputStream</var> to the <var>outputStream</var>.
     *
     * @param closeInputStream  Whether to close the <var>inputStream</var> (also if an {@link IOException} is thrown)
     * @param closeOutputStream Whether to close the <var>outputStream</var> (also if an {@link IOException} is thrown)
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, boolean closeInputStream, OutputStream outputStream, boolean closeOutputStream)
    throws IOException {

        try {
            final long count = IoUtil.copy(inputStream, outputStream, Long.MAX_VALUE);

            if (closeInputStream) inputStream.close();
            if (closeOutputStream) outputStream.close();

            return count;
        } catch (IOException ioe) {
            if (closeInputStream) try { inputStream.close(); } catch (Exception e) {}
            if (closeOutputStream) try { outputStream.close(); } catch (Exception e) {}
            throw ioe;
        }
    }

    /**
     * Creates and returns a {@link RunnableWhichThrows} that copies bytes from <var>in</var> to <var>out</var> until
     * end-of-input.
     */
    public static RunnableWhichThrows<IOException>
    copyRunnable(final InputStream in, final OutputStream out) {
        return new RunnableWhichThrows<IOException>() {

            @Override public void
            run() throws IOException {
                IoUtil.copy(in, out);
            }
        };
    }

    /**
     * Reads the reader until end-of-input and writes all data to the writer. Closes neither the reader nor the writer.
     *
     * @return The number of characters copied
     */
    public static long
    copy(Reader reader, Writer writer) throws IOException {
        return IoUtil.copy(reader, false, writer, false);
    }

    /**
     * Copies the contents of the <var>reader</var> to the <var>writer</var>.
     *
     * @return The number of characters copied
     */
    public static long
    copy(Reader reader, boolean closeReader, Writer writer, boolean closeWriter) throws IOException {

        char[] buffer = new char[4096];
        long   count  = 0L;
        try {
            for (;;) {
                IoUtil.LOGGER.log(Level.FINEST, "About to ''read(char[{0}])''", buffer.length);
                int n = reader.read(buffer);
                IoUtil.LOGGER.log(Level.FINEST, "''read()'' returned {0}", n);
                if (n == -1) break;
                IoUtil.LOGGER.log(Level.FINEST, "About to ''write(char[{0}])''", n);
                writer.write(buffer, 0, n);
                IoUtil.LOGGER.log(Level.FINEST, "'write()' returned");
            }
            writer.flush();
            if (closeReader) reader.close();
            if (closeWriter) writer.close();
            IoUtil.LOGGER.log(Level.FINEST, "{0} bytes copied", count);
            return count;
        } catch (IOException ioe) {
            if (closeReader) try { reader.close(); } catch (Exception e) {}
            if (closeWriter) try { writer.close(); } catch (Exception e) {}
            throw ExceptionUtil.wrap(count + " characters copied so far", ioe);
        }
    }

    /**
     * Reads the reader until end-of-input and writes all data to the output stream. Closes neither the reader nor the
     * output stream.
     *
     * @return The number of characters copied
     */
    public static long
    copy(Reader reader, OutputStream outputStream, Charset charset) throws IOException {
        return IoUtil.copy(reader, new OutputStreamWriter(outputStream, charset));
    }

    /**
     * Reads the {@link Readable} until end-of-input and writes all data to the {@link Appendable}.
     *
     * @return The number of characters copied
     */
    public static long
    copy(Readable r, Appendable a) throws IOException {

        CharBuffer cb    = CharBuffer.allocate(4096);
        long       count = 0;
        for (;;) {
            int n = r.read(cb);
            if (n == -1) break;
            cb.flip();
            a.append(cb);
            count += n;
            cb.clear();
        }
        return count;
    }

    /**
     * Copies the contents of the <var>inputStream</var> to the <var>outputFile</var>. Attempts to delete a partially
     * written output file if the operation fails.
     *
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, boolean closeInputStream, File outputFile, boolean append) throws IOException {

        try {
            return IoUtil.copy(inputStream, closeInputStream, new FileOutputStream(outputFile, append), true);
        } catch (IOException ioe) {
            outputFile.delete();
            throw ioe;
        } catch (RuntimeException re) {
            outputFile.delete();
            throw re;
        }
    }

    /**
     * Copies the contents of the <var>reader</var> to the {@code outputFile}, encoded with the given
     * <var>outputCharset</var>. Attempts to delete a partially written output file if the operation fails.
     *
     * @return The number of characters copied
     */
    public static long
    copy(Reader reader, boolean closeReader, File outputFile, boolean append, Charset outputCharset)
    throws IOException {
        try {
            long count = IoUtil.copy(
                reader,
                closeReader,
                new OutputStreamWriter(new FileOutputStream(outputFile, append), outputCharset),
                true
            );
            IoUtil.LOGGER.log(Level.FINEST, "{0} bytes copied", count);
            return count;
        } catch (IOException ioe) {
            outputFile.delete();
            throw ioe;
        } catch (RuntimeException re) {
            outputFile.delete();
            throw re;
        }
    }

    /**
     * Copies the contents of the <var>inputFile</var> to the <var>outputStream</var>.
     *
     * @param closeOutputStream Whether to close the <var>outputStream</var> when execution completes
     * @return                  The number of bytes copied
     */
    public static long
    copy(File inputFile, OutputStream outputStream, boolean closeOutputStream) throws IOException {

        FileInputStream is;
        try {
            is = new FileInputStream(inputFile);
        } catch (IOException ioe) {
            if (closeOutputStream) try { outputStream.close(); } catch (Exception e) {}
            throw ioe;
        }

        return IoUtil.copy(is, true, outputStream, closeOutputStream);
    }

    /**
     * Copies the contents of the <var>inputStream</var> to the <var>outputFile</var>.
     * <p>
     *   The parent directory of the <var>outputFile</var> must already exist.
     * </p>
     *
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, boolean closeInputStream, File outputFile) throws IOException {

        OutputStream os = new FileOutputStream(outputFile);
        try {
            return IoUtil.copy(inputStream, closeInputStream, os, true);
        } catch (IOException ioe) {
            if (!outputFile.delete()) {
                throw new IOException("Cannot delete '" + outputFile + "'"); // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
            throw ioe;
        }
    }

    /**
     * Copies the contents of the <var>inputStream</var> to the <var>outputFile</var>.
     * <p>
     *   The parent directory of the <var>outputFile</var> must already exist.
     * </p>
     *
     * @return The number of bytes copied
     */
    public static long
    copy(InputStream inputStream, boolean closeInputStream, File outputFile, CollisionStrategy collisionStrategy)
    throws IOException {

        if (!outputFile.exists()) return IoUtil.copy(inputStream, closeInputStream, outputFile);

        // The outputFile already exists - we have a "collision".

        switch (collisionStrategy) {

        case LEAVE_OLD:
            if (closeInputStream) inputStream.close();
            return -1;

        case OVERWRITE:
            return IoUtil.copy(inputStream, closeInputStream, outputFile);

        case IO_EXCEPTION:
            throw new IOException("File \"" + outputFile + "\" already exists");

        case IO_EXCEPTION_IF_DIFFERENT:
            if (!IoUtil.isContentIdentical(inputStream, outputFile)) {
                throw new IOException("File \"" + outputFile + "\" already exists with non-identical content");
            }
            if (closeInputStream) inputStream.close();
            return -1;

        default:
            throw new AssertionError(collisionStrategy);
        }
    }

    /**
     * Copies the contents of the <var>inputFile</var> to the <var>outputFile</var>.
     * <p>
     *   If the output file already exists, it is silently re-created (and its original content is lost).
     * </p>
     * <p>
     *   The parent directory of the <var>outputFile</var> must already exist.
     * </p>
     *
     * @return The number of bytes copied
     */
    public static long
    copy(File inputFile, File outputFile) throws IOException {

        return IoUtil.copy(new FileInputStream(inputFile), true, outputFile);
    }

    /**
     * Determines the behavior of the {@link IoUtil#copyTree(File, File, CollisionStrategy)} and {@link
     * IoUtil#copyTree(File, File, CollisionStrategy)} methods when files collide while copying.
     *
     * @see #LEAVE_OLD
     * @see #OVERWRITE
     * @see #IO_EXCEPTION
     * @see #IO_EXCEPTION_IF_DIFFERENT
     */
    public
    enum CollisionStrategy {

        /**
         * Do nothing; the input file is not copied, and the output file remains unchanged.
         */
        LEAVE_OLD,

        /**
         * Re-create the output file and copy the contents of the input file into it. The original content of the
         * output file is lost.
         */
        OVERWRITE,

        /**
         * Throw an {@link IOException} indicating the fact.
         */
        IO_EXCEPTION,

        /**
         * If the content of the input file and the output file is identical, do nothing. Otherwise throw an
         * {@link IOException} indicating the fact.
         */
        IO_EXCEPTION_IF_DIFFERENT,
    }

    /**
     * Copies the contents of the <var>inputFile</var> to the <var>outputFile</var>.
     * <p>
     *   If the output file already exists, then the <var>collisionStartegy</var> determines what is done.
     * </p>
     * <p>
     *   The parent directory of the <var>outputFile</var> must already exist.
     * </p>
     *
     * @return The number of bytes copied, or -1 iff the <var>outputFile</var> exists and <var>collisionStrategy</var>
     *         {@code ==} {@link CollisionStrategy#LEAVE_OLD} {@code ||} <var>collisionStrategy</var> {@code ==} {@link
     *         CollisionStrategy#IO_EXCEPTION_IF_DIFFERENT}
     * @see CollisionStrategy
     */
    public static long
    copy(File inputFile, File outputFile, CollisionStrategy collisionStrategy) throws IOException {

        if (!outputFile.exists()) return IoUtil.copy(inputFile, outputFile);

        // The outputFile already exists - we have a "collision".

        switch (collisionStrategy) {

        case LEAVE_OLD:
            ;
            return -1;

        case OVERWRITE:
            return IoUtil.copy(inputFile, outputFile);

        case IO_EXCEPTION:
            throw new IOException("File \"" + outputFile + "\" already exists");

        case IO_EXCEPTION_IF_DIFFERENT:
            if (!IoUtil.isContentIdentical(inputFile, outputFile)) {
                throw new IOException("File \"" + outputFile + "\" already exists with non-identical content");
            }
            return -1;

        default:
            throw new AssertionError(collisionStrategy);
        }
    }

    /**
     * Copies a directory tree.
     * <p>
     *   Iff the <var>source</var> is a normal file, then {@link IoUtil#copy(File, File, CollisionStrategy)} is called.
     * </p>
     * <p>
     *   Otherwise, the <var>destination</var> is created as a directory (if it did not exist), and all members
     *   of the <var>source</var> directory are recursively copied to the <var>destination</var> directory.
     * </p>
     * <p>
     *   The parent directory for the <var>destination</var> must already exist.
     * </p>
     */
    public static void
    copyTree(File source, File destination, CollisionStrategy collisionStrategy) throws IOException {

        if (source.isFile()) {
            IoUtil.copy(source, destination, collisionStrategy);
            return;
        }

        boolean destinationDirectoryAlreadyExisted = destination.exists();

        if (!destinationDirectoryAlreadyExisted) {
            if (!destination.mkdir()) throw new IOException(destination.toString());
        }
        try {

            for (String memberName : source.list()) {
                IoUtil.copyTree(new File(source, memberName), new File(destination, memberName), collisionStrategy);
            }
        } catch (IOException ioe) {
            if (!destinationDirectoryAlreadyExisted) destination.delete();
            throw ioe;
        } catch (RuntimeException re) {
            if (!destinationDirectoryAlreadyExisted) destination.delete();
            throw re;
        }
    }

    /**
     * Copies a resource tree to a directory in the file system.
     * <p>
     *   Iff the <var>source</var> is a "content resource", then {@link IoUtil#copy(InputStream, boolean, File)} is
     *   called.
     * </p>
     * <p>
     *   Otherwise, the <var>destination</var> is created as a directory (if it did not exist), and all members
     *   of the <var>source</var> directory are recursively copied to the <var>destination</var> directory.
     * </p>
     * <p>
     *   The parent directory for the <var>destination</var> must already exist.
     * </p>
     */
    public static void
    copyTree(URL source, File destination, CollisionStrategy collisionStrategy) throws IOException {

        Map<String, URL> rs = ClassLoaders.getSubresourcesOf(source, "", true);

        if (rs.isEmpty()) return;

        if (rs.size() == 1) {
            IoUtil.copy(
                rs.values().iterator().next().openStream(), // inputStream
                true,                                       // closeInputStream
                destination,                                // outputFile
                collisionStrategy                           // collisionStrategy
            );
            return;
        }

        IoUtil.copySubtree(rs, "", destination, collisionStrategy);
    }

    private static void
    copySubtree(Map<String, URL> rs, String namePrefix, File destinationDirectory, CollisionStrategy collisionStrategy)
    throws IOException {

        assert rs.get(namePrefix) != null;

        if (!destinationDirectory.mkdir()) {
            throw new IOException("Could not create destination directory \"" + destinationDirectory + "\"");
        }

        for (Entry<String, URL> e : rs.entrySet()) {
            String name     = e.getKey();
            URL    location = e.getValue();

            if (!name.startsWith(namePrefix)) continue;

            int npl = namePrefix.length();
            if (name.length() == npl) continue;

            int idx = name.indexOf('/', npl);
            if (idx == -1) {
                IoUtil.copy(
                    location.openStream(),                              // inputStream
                    true,                                               // closeInputStream
                    new File(destinationDirectory, name.substring(npl)) // outputFile
                );
            } else
            if (idx == name.length() - 1) {
                IoUtil.copySubtree(rs, name, new File(destinationDirectory, name.substring(npl)), collisionStrategy);
            }
        }
    }

    /**
     * @return A {@code Comsumer<OutputStream>} which copies <var>inputStream</var> to its subject
     */
    public static ConsumerWhichThrows<OutputStream, IOException>
    copyFrom(final InputStream inputStream) {

        return new ConsumerWhichThrows<OutputStream, IOException>() {

            @Override public void
            consume(OutputStream os) throws IOException { IoUtil.copy(inputStream, os); }
        };
    }

    /**
     * @return Whether the contents of the two files is byte-wise identical
     */
    public static boolean
    isContentIdentical(File file1, File file2) throws IOException {

        if (file1.length() != file2.length()) return false;

        InputStream fis1 = new FileInputStream(file1);
        try {

            boolean result = IoUtil.isContentIdentical(fis1, file2);

            fis1.close();

            return result;
        } finally {
            try { fis1.close(); } catch (Exception e) {}
        }
    }

    /**
     * @return Whether the byte sequences produced by the <var>stream</var> is identical with the contents of the
     *         <var>file</var>
     */
    private static boolean
    isContentIdentical(InputStream stream, File file) throws FileNotFoundException, IOException {

        InputStream fis = new FileInputStream(file);
        try {
            boolean result = IoUtil.isContentIdentical(stream, fis);

            fis.close();

            return result;
        } finally {
            try { fis.close(); } catch (Exception e) {}
        }
    }

    /**
     * @return Whether the byte sequences produced by the two streams are identical
     */
    private static boolean
    isContentIdentical(InputStream stream1, InputStream stream2) throws IOException {

        byte[] buffer1 = new byte[4096], buffer2 = new byte[4096];

        for (;;) {

            // Read next chunk from file1.
            int n1 = stream1.read(buffer1);
            if (n1 == -1) break;
            for (int off = 0; off < n1;) {

                // Read next chunk from file2.
                int n2 = stream2.read(buffer2, off, n1 - off);
                if (n2 == -1) return false;

                // Compare chunk contents.
                for (; n2 > 0; n2--, off++) {
                    if (buffer2[off] != buffer1[off]) return false;
                }
            }
        }

        return true;
    }

    /**
     * Creates and returns an {@link OutputStream} that delegates all work to the given <var>delegates</var>:
     * <ul>
     *   <li>
     *     The {@link OutputStream#write(byte[], int, int) write()} methods write the given data to all the delegates;
     *     if any of these throw an {@link IOException}, it is rethrown, and it is undefined whether all the data was
     *     written to all the delegates.
     *   </li>
     *   <li>
     *     {@link OutputStream#flush() flush()} flushes the delegates; throws the first {@link IOException} that any
     *     of the delegates throws.
     *   </li>
     *   <li>
     *     {@link OutputStream#close() close()} attempts to close <i>all</i> the <var>delegates</var>; if any of these
     *     throw {@link IOException}s, one of them is rethrown.
     *   </li>
     * </ul>
     */
    @NotNullByDefault(false) public static OutputStream
    tee(final OutputStream... delegates) {
        return new OutputStream() {

            @Override public void
            close() throws IOException {
                IOException caughtIOException = null;
                for (OutputStream delegate : delegates) {
                    try {
                        delegate.close();
                    } catch (IOException ioe) {
                        caughtIOException = ioe;
                    }
                }
                if (caughtIOException != null) throw caughtIOException;
            }

            @Override public void
            flush() throws IOException {
                for (OutputStream delegate : delegates) delegate.flush();
            }

            @Override public void
            write(byte[] b, int off, int len) throws IOException {
                // Overriding this method is not strictly necessary, because "OutputStream.write(byte[], int, int)"
                // calls "OutputStream.write(int)", but "delegate.write(byte[], int, int)" is probably more
                // efficient. However, the behavior is different when one of the delegates throws an exception
                // while being written to.
                for (OutputStream delegate : delegates) delegate.write(b, off, len);
            }

            @Override public void
            write(int b) throws IOException {
                for (OutputStream delegate : delegates) delegate.write(b);
            }
        };
    }

    /**
     * Creates and returns a {@link FilterInputStream} that duplicates all bytes read through it and writes them to
     * an {@link OutputStream}.
     * <p>
     *   The {@link OutputStream} is flushed on end-of-input and calls to {@link InputStream#available()}.
     * </p>
     */
    public static InputStream
    wye(InputStream in, final OutputStream out) {

        return new FilterInputStream(in) {

            @Override public int
            read() throws IOException {
                int b = super.read();
                if (b == -1) {
                    out.flush();
                } else {
                    out.write(b);
                }
                return b;
            }

            @Override public int
            read(@Nullable byte[] b, int off, int len) throws IOException {
                int count = super.read(b, off, len);
                if (count > 0) out.write(b, off, count);
                if (count == 0) out.flush();
                return count;
            }


            @Override public int
            available() throws IOException {
                out.flush();
                return this.in.available();
            }
        };
    }

    /**
     * Invokes <var>writeContents</var>{@code .consume()} with an output stream subject that writes the data through to
     * the given <var>outputStream</var>.
     *
     * @return The number of bytes that were written through
     */
    public static long
    writeAndCount(
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents,
        OutputStream                                                     outputStream
    ) throws IOException {

        Produmer<Long, Long> count = ConsumerUtil.store();

        writeContents.consume(IoUtil.tee(outputStream, IoUtil.lengthWritten(ConsumerUtil.cumulate(count, 0L))));

        Long result = count.produce();
        return result == null ? 0L : result;
    }

    /**
     * An entity which writes characters to a {@link Writer}.
     */
    public
    interface WritingRunnable {

        /**
         * @see WritingRunnable
         */
        void
        run(Writer w) throws Exception;
    }

    private static final ExecutorService EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(
        3 * Runtime.getRuntime().availableProcessors(),
        ThreadUtil.DAEMON_THREAD_FACTORY
    );

    /**
     * Executes the <var>writingRunnables</var> in parallel, <i>concatenates</i> their output, and writes it to the
     * {@code writer}, i.e. the output of the runnables does not mix, but the <i>complete</i> output of the first
     * runnable appears before that of the second runnable, and so on.
     * <p>
     *   Since the character buffer for each {@link WritingRunnable} has a limited size, the runnables with higher
     *   indexes tend to block if the runnables with lower indexes do not complete quickly enough.
     * </p>
     */
    public static void
    parallel(WritingRunnable[] writingRunnables, final Writer writer) {
        List<Callable<Void>> callables = IoUtil.toCallables(writingRunnables, writer);

        try {
            IoUtil.EXECUTOR_SERVICE.invokeAll(callables);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // Preserve interrupt status.
        }
    }

    /**
     * Creates and returns a list of callables; when all of these have been called, then all the given
     * <var>writingRunnables</var> have been run, and their output is written strictly sequentially to the given
     * <var>writer</var>, even if the callables were called out-of-sequence or in parallel.
     * <p>
     *   Deadlocks may occur if lower-index <var>writingRunnables</var> "depend" on higher-index
     *   <var>writingRunnables</var>, i.e. the former do not complete because they wait for a certain state of
     *   completion of the latter.
     * </p>
     */
    private static List<Callable<Void>>
    toCallables(WritingRunnable[] writingRunnables, final Writer writer) {
        List<Callable<Void>> callables = new ArrayList<Callable<Void>>(writingRunnables.length + 1);
        final List<Reader>   readers   = new ArrayList<Reader>(writingRunnables.length);

        // Create the 'collector' that concatenates the runnnables' outputs.
        callables.add(new Callable<Void>() {

            @Override @Nullable public Void
            call() throws Exception {
                for (Reader reader : readers) {
                    IoUtil.copy(reader, writer);
                }
                return null;
            }
        });

        for (final WritingRunnable wr : writingRunnables) {

            // Create a PipedReader/PipedWriter pair for communication between the runnable and the 'collector'.
            final PipedWriter pw = new PipedWriter();
            try {
                readers.add(new PipedReader(pw));
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap(
                    "Should never throw an IOException if the argument is a 'fresh' PipedWriter",
                    ioe,
                    AssertionError.class
                );
            }

            // Create a callable that will run the runnable.
            callables.add(new Callable<Void>() {

                @Override @Nullable public Void
                call() throws Exception {
                    try {
                        wr.run(pw);
                        return null;
                    } catch (Exception e) {
                        IoUtil.LOGGER.log(Level.WARNING, null, e);
                        throw e;
                    } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch
                        IoUtil.LOGGER.log(Level.SEVERE, null, e);
                        throw e;
                    } finally {
                        try { pw.close(); } catch (Exception e) {}
                    }
                }
            });
        }
        return callables;
    }

    /**
     * @return All bytes that the given {@link InputStream} produces
     */
    public static byte[]
    readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtil.copy(is, baos);
        return baos.toByteArray();
    }

    /**
     * @return All bytes that the given {@link InputStream} produces, decoded into a string
     */
    public static String
    readAll(InputStream inputStream, Charset charset, boolean closeInputStream) throws IOException {

        StringWriter sw = new StringWriter();
        IoUtil.copy(
            new InputStreamReader(inputStream, charset),
            closeInputStream,
            sw,
            false
        );
        return sw.toString();
    }

    /**
     * Skips <var>n</var> bytes on the <var>inputStream</var>. Notice that {@link InputStream#skip(long)} sometimes
     * skips less than the requested number of bytes for no good reason, e.g. {@link BufferedInputStream#skip(long)}
     * is known for that. <em>This</em> method tries "harder" to skip exactly the requested number of bytes.
     *
     * @return                     The number of bytes that were skipped
     * @see InputStream#skip(long)
     */
    public static long
    skip(InputStream inputStream, long n) throws IOException {

        long result = 0;
        while (result < n) {
            long skipped = inputStream.skip(n - result);
            if (skipped == 0) return result;
            result += skipped;
        }

        return result;
    }

    /**
     * Skips all remaining data on the <var>inputStream</var>.
     *
     * @return                     The number of bytes that were skipped
     * @see InputStream#skip(long)
     */
    public static long
    skipAll(InputStream inputStream) throws IOException {

        long result = 0;
        for (;;) {
            long skipped = inputStream.skip(Long.MAX_VALUE);
            if (skipped == 0) return result;
            result += skipped;
        }
    }

    /**
     * Creates and returns an {@link OutputStream} which writes at most <var>byteCountLimits</var>{@code .produce()}
     * bytes to <var>delegates</var>{@code .produce()} before closing it and writing the next
     * <var>byteCountLimits</var>{@code .produce()} bytes to <var>delegates</var>{@code .produce()}, and so on.
     *
     * @param delegates       Must produce a (non-{@code null}) series of {@link OutputStream}s
     * @param byteCountLimits Must produce a (non-{@code null}) series of {@link Long}s
     */
    public static OutputStream
    split(
        final ProducerWhichThrows<? extends OutputStream, ? extends IOException> delegates,
        final Producer<? extends Long>                                           byteCountLimits
    ) throws IOException {

        return new OutputStream() {

            /** Current delegate to write to. */
            private OutputStream delegate = AssertionUtil.notNull(delegates.produce(), "'delegates' produced <null>");

            /** Number of remaining bytes to be written. */
            private long delegateByteCount = AssertionUtil.notNull(
                byteCountLimits.produce(),
                "'byteCountLimits' produced <null>"
            );

            @Override public void
            write(int b) throws IOException { this.write(new byte[] { (byte) b }, 0, 1); }

            @Override public synchronized void
            write(@Nullable byte[] b, int off, int len) throws IOException {

                while (len > this.delegateByteCount) {
                    this.delegate.write(b, off, (int) this.delegateByteCount);
                    this.delegate.close();
                    off += this.delegateByteCount;
                    len -= this.delegateByteCount;

                    this.delegate = AssertionUtil.notNull(
                        delegates.produce(),
                        "'delegates' produced <null>"
                    );
                    this.delegateByteCount = AssertionUtil.notNull(
                        byteCountLimits.produce(),
                        "'byteCountLimits' produced <null>"
                    );
                }

                this.delegate.write(b, off, len);
                this.delegateByteCount -= len;
            }

            @Override public void
            flush() throws IOException { this.delegate.flush(); }

            @Override public void
            close() throws IOException { this.delegate.close(); }
        };
    }

    /** An {@link InputStream} that produces exactly 0 bytes. */
    public static final InputStream
    EMPTY_INPUT_STREAM = new InputStream() {
        @Override public int read()                                       { return -1; }
        @Override public int read(@Nullable byte[] buf, int off, int len) { return -1; }
    };

    /** An {@link OutputStream} that discards all bytes written to it. */
    public static final OutputStream
    NULL_OUTPUT_STREAM = new OutputStream() {
        @Override public void write(@Nullable byte[] b, int off, int len) {}
        @Override public void write(int b)                                {}
    };

    /**
     * @return An input stream that reads an endless stream of bytes of value <var>b</var>
     */
    public static InputStream
    constantInputStream(final byte b) {
        return new InputStream() {

            @Override public int
            read() { return 0; }

            @Override public int
            read(@Nullable byte[] buf, int off, int len) { Arrays.fill(buf,  off, len, b); return len; }
        };
    }

    /**
     * An input stream that reads an endless stream of zeros.
     */
    public static final InputStream
    ZERO_INPUT_STREAM = IoUtil.constantInputStream((byte) 0);

    /**
     * @return An {@link InputStream} which ignores all invocations of {@link InputStream#close()}
     */
    public static InputStream
    unclosableInputStream(InputStream delegate) {

        return new FilterInputStream(delegate) { @Override public void close() {} };
    }

    /**
     * @return An {@link OutputStream} which ignores all invocations of {@link OutputStream#close()}
     */
    public static OutputStream
    unclosableOutputStream(OutputStream delegate) {

        return new FilterOutputStream(delegate) {

            @Override public void
            close() {}

            @Override public void
            write(@Nullable byte[] b, int off, int len) throws IOException { this.out.write(b, off, len); }
        };
    }

    /**
     * Writes <var>count</var> bytes of value <var>b</var> to the given <var>outputStream</var>.
     */
    public static void
    fill(OutputStream outputStream, byte b, long count) throws IOException {

        if (count > 8192) {
            byte[] ba = new byte[8192];
            if (b != 0) Arrays.fill(ba, b);
            do {
                outputStream.write(ba);
                count -= 8192;
            } while (count > 8192);
        }

        byte[] ba = new byte[(int) count];
        Arrays.fill(ba, b);
        outputStream.write(ba);
    }

    /**
     * @return An input stream which reads the data produced by the <var>delegate</var> byte producer; {@code null}
     *         products are returned as 'end-of-input'
     */
    public static InputStream
    byteProducerInputStream(final ProducerWhichThrows<? extends Byte, ? extends IOException> delegate) {

        return new InputStream() {

            @Override public int
            read() throws IOException {
                Byte b = delegate.produce();
                return b != null ? 0xff & b : -1;
            }
        };
    }

    /**
     * @return An input stream which reads the data produced by the <var>delegate</var> byte producer; {@code null}
     *         products are returned as 'end-of-input'
     */
    public static InputStream
    byteProducerInputStream(final Producer<? extends Byte> delegate) {

        return IoUtil.byteProducerInputStream(ProducerUtil.<Byte, IOException>asProducerWhichThrows(delegate));
    }

    /**
     * @return An input stream which reads and endless stream of random bytes
     */
    public static InputStream
    randomInputStream(final long seed) {

        return IoUtil.byteProducerInputStream(ProducerUtil.randomByteProducer(seed));
    }

    /**
     * @return An output stream which feeds the data to the <var>delegate</var> byte consumer
     */
    public static OutputStream
    byteConsumerOutputStream(final ConsumerWhichThrows<? super Byte, ? extends IOException> delegate) {

        return new OutputStream() {

            @Override public void
            write(int b) throws IOException { delegate.consume((byte) b); }
        };
    }

    /**
     * @return All characters that the given {@link Reader} produces
     */
    public static String
    readAll(Reader reader) throws IOException {
        return IoUtil.readAll(reader, false);
    }

    /**
     * @param closeReader Whether the <var>reader</var> should be closed before the method returns
     * @return            All characters that the given {@link Reader} produces
     */
    public static String
    readAll(Reader reader, boolean closeReader) throws IOException {

        char[]        buf = new char[4096];
        StringBuilder sb  = new StringBuilder();

        try {

            for (;;) {
                int n = reader.read(buf);
                if (n == -1) break;
                sb.append(buf, 0, n);
            }

            if (closeReader) reader.close();

            return sb.toString();
        } finally {
            if (closeReader) {
                try { reader.close(); } catch (Exception e) {}
            }
        }
    }

    /**
     * @return An {@link InputStream} which first closes the <var>delegate</var>, and then attempts to delete the
     *         <var>file</var>
     */
    protected static InputStream
    deleteOnClose(InputStream delegate, final File file) {

        return new FilterInputStream(delegate) {

            @Override public void
            close() throws IOException {
                super.close();
                file.delete();
            }
        };
    }

    /**
     * Creates and returns an array of <var>n</var> {@link OutputStream}s.
     * <p>
     *   Iff exactly the same bytes are written to all of these streams, and then all the streams are closed, then
     *   <var>whenIdentical</var> will be run (exactly once).
     * </p>
     * <p>
     *   Otherwise, when the first non-identical byte is written to one of the streams, or at the latest when that
     *   stream is closed, <var>whenNotIdentical</var> will be run (possibly more than once).
     * </p>
     */
    public static OutputStream[]
    compareOutput(final int n, final Runnable whenIdentical, final Runnable whenNotIdentical) {

        /**
         * Logs checksums of the first n1, n2, n3, ... bytes written.
         * <p>
         *   This class is used to compare the data written to multiple output streams without storing the entire data
         *   in memory.
         * </p>
         * <p>
         *   n1, n2, n3, ... is an exponentially growing series, starting with a very small value.
         * </p>
         */
        abstract
        class ChecksumOutputStream extends OutputStream {

            /** The checksum of the bytes written to this stream so far. */
            private final Checksum checksum  = new CRC32();

            /** The number of bytes written to this stream so far. */
            private long count;

            /**
             * {@code checksums[i]} is the checksum of the first {@code THRESHOLD[i]} that were written to this stream.
             * <p>
             *   After this stream was closed, {@code checksums[idx - 1]} is the checksum of <b>all</b> bytes that were
             *   written to this stream.
             * </p>
             */
            protected final long[] checksums = new long[IoUtil.THRESHOLDS.length];

            /** The number of checksums in the {@link #checksums} array. */
            protected int idx;

            /**
             * Indicates that this stream is closed and that {@code checksums[idx - 1]} is the checksum of <b>all</b>
             * bytes that were written to this stream.
             */
            private boolean closed;

            @Override public void
            write(int b) throws IOException {
                if (this.closed) throw new IOException("Stream is closed");

                if (this.count == IoUtil.THRESHOLDS[this.idx]) this.pushChecksum();
                this.checksum.update(b);
                this.count++;
            }

            @Override public void
            write(@Nullable byte[] b, int off, int len) throws IOException {
                assert b != null;
                if (this.closed) throw new IOException("Stream is closed");

                while (this.count + len > IoUtil.THRESHOLDS[this.idx]) {
                    int part = (int) Math.min(Integer.MAX_VALUE, IoUtil.THRESHOLDS[this.idx] - this.count);
                    this.checksum.update(b, off, part);
                    this.count = IoUtil.THRESHOLDS[this.idx];
                    this.pushChecksum();
                    off += part;
                    len -= part;
                }

                this.checksum.update(b, off, len);
                this.count += len;
            }

            private void
            pushChecksum() {
                this.checksums[this.idx] = this.checksum.getValue();
                this.checksumWasPushed(this.idx);
                this.idx++;
            }

            /**
             * Is called when another checksum is entered in {@link #checksums}.
             *
             * @param idx The index in {@link #checksums} where the checksum was stored
             */
            abstract void checksumWasPushed(int idx);

            @Override public void
            close() {
                if (this.closed) return;
                this.pushChecksum();
                this.closed = true;
                this.wasClosed();
            }

            /**
             * Is called after this stream has been closed (for the first time).
             */
            abstract void wasClosed();
        }

        final ChecksumOutputStream[] result = new ChecksumOutputStream[n];
        for (int i = 0; i < n; i++) {
            result[i] = new ChecksumOutputStream() {

                @Override void
                checksumWasPushed(int idx) {
                    for (int i = 0; i < n; i++) {
                        if (result[i].idx == idx + 1 && result[i].checksums[idx] != this.checksums[idx]) {
                            whenNotIdentical.run();
                            return;
                        }
                    }
                }

                @Override void
                wasClosed() {
                    for (int i = 0; i < n; i++) {
                        if (!result[i].closed) return;
                        if (
                            result[i].idx != this.idx
                            || result[i].checksums[this.idx - 1] != this.checksums[this.idx - 1]
                        ) {
                            whenNotIdentical.run();
                            return;
                        }
                    }
                    whenIdentical.run();
                }
            };
        }

        return result;
    }
    private static final long[] THRESHOLDS;

    static {
        THRESHOLDS = new long[126];
        long x = 2;
        for (int i = 0; i < IoUtil.THRESHOLDS.length; x <<= 1) {
            IoUtil.THRESHOLDS[i++] = x;
            IoUtil.THRESHOLDS[i++] = x + (x >> 1);
        }
    }

    /**
     * Creates and returns an {@link OutputStream} which ignores the data written to it and only honors the
     * <i>number of bytes written</i>:
     * <p>
     *   Every time data is written to the {@link OutputStream}, it invokes the {@link Consumer#consume(Object)
     *   consume()} method on the <var>delegate</var> with the number of bytes written (not the <i>cumulated</i> number
     *   of bytes written!).
     * </p>
     */
    public static OutputStream
    lengthWritten(final Consumer<? super Integer> delegate) {

        return new OutputStream() {

            @Override public void
            write(int b) { delegate.consume(1); }

            @Override public void
            write(@Nullable byte[] b, int off, int len) { delegate.consume(len); }
        };
    }

    /**
     * Wraps the given {@link CharSequence} in a {@link Reader} - much more efficient than "{@code new
     * StringReader(cs.toString)}".
     */
    public static Reader
    asReader(final CharSequence cs) {
        return new Reader() {

            int pos;

            @Override public int
            read() { return this.pos >= cs.length() ? -1 : cs.charAt(this.pos++); }

            @Override public int
            read(@Nullable char[] cbuf, int off, int len) {
                assert cbuf != null;

                if (len <= 0) return 0;

                if (this.pos >= cs.length()) return -1;

                int end = cs.length();
                if (this.pos + len > end) {
                    len = end - this.pos;
                } else {
                    end = this.pos + len;
                }
                for (int i = this.pos; i < end; cbuf[off++] = cs.charAt(i++));
                return len;
            }

            @Override public void
            close() {}
        };
    }

    /**
     * Copies the contents of a resource to the given <var>outputStream</var>.
     * <p>
     *   The resource is addressed by the <var>classLoader</var> and the <var>resourceName</var>, as described for
     *   {@link ClassLoader#getResourceAsStream(String)}.
     * </p>
     *
     * @param closeOutputStream Whether the <var>outputStream</var> should be closed after the content of the resource
     *                          has been copied
     */
    public static void
    copyResource(ClassLoader classLoader, String resourceName, OutputStream outputStream, boolean closeOutputStream)
    throws IOException {

        InputStream is = classLoader.getResourceAsStream(resourceName);
        if (is == null) throw new FileNotFoundException(resourceName);

        IoUtil.copy(is, true, outputStream, closeOutputStream);
    }

    /**
     * Copies the contents a resource to the given <var>outputStream</var>.
     * <p>
     *   The resource is addressed by the <var>clasS</var> and the <var>resourceName</var>, as described for {@link
     *   Class#getResourceAsStream(String)}.
     * </p>
     *
     * @param closeOutputStream Whether the <var>outputStream</var> should be closed after the content of the resource
     *                          has been copied
     */
    public static void
    copyResource(Class<?> clasS, String resourceName, OutputStream outputStream, boolean closeOutputStream)
    throws IOException {

        InputStream is = clasS.getResourceAsStream(resourceName);
        if (is == null) throw new FileNotFoundException(resourceName);

        IoUtil.copy(is, true, outputStream, closeOutputStream);
    }

    /**
     * Copies the contents a resource to the given <var>toFile</var>.
     * <p>
     *   The resource is addressed by the <var>classLoader</var> and the <var>resourceName</var>, as described for
     *   {@link ClassLoader#getResourceAsStream(String)}.
     * </p>
     *
     * @param createMissingParentDirectories Whether to create any missing parent directories for the <var>toFile</var>
     */
    public static void
    copyResource(
        final ClassLoader classLoader,
        final String      resourceName,
        File              toFile,
        boolean           createMissingParentDirectories
    ) throws IOException {

        IoUtil.outputFileOutputStream(
            toFile,
            new ConsumerWhichThrows<OutputStream, IOException>() {

                @Override public void
                consume(OutputStream outputStream) throws IOException {
                    IoUtil.copyResource(classLoader, resourceName, outputStream, false);
                }
            },
            createMissingParentDirectories
        );
    }

    /**
     * Copies the contents a resource to the given <var>toFile</var>.
     * <p>
     *   The resource is addressed by the <var>clasS</var> and the <var>resourceName</var>, as described for {@link
     *   Class#getResourceAsStream(String)}.
     * </p>
     *
     * @param createMissingParentDirectories Whether to create any missing parent directories for the <var>toFile</var>
     */
    public static void
    copyResource(
        final Class<?> clasS,
        final String   resourceName,
        File           toFile,
        boolean        createMissingParentDirectories
    ) throws IOException {

        IoUtil.outputFileOutputStream(
            toFile,
            new ConsumerWhichThrows<OutputStream, IOException>() {

                @Override public void
                consume(OutputStream outputStream) throws IOException {
                    IoUtil.copyResource(clasS, resourceName, outputStream, false);
                }
            },
            createMissingParentDirectories
        );
    }

    /**
     * Creates a temporary file, stores all data that can be read from the <var>inputStream</var> into it, closes the
     * file, invokes the <var>delegate</var> with the file, and eventually deletes the file.
     *
     * @see File#createTempFile(String, String, File)
     */
    public static <EX extends Throwable> void
    asFile(
        InputStream                                     inputStream,
        boolean                                         closeInputStream,
        @Nullable String                                prefix,
        @Nullable String                                suffix,
        @Nullable File                                  directory,
        ConsumerWhichThrows<? super File, ? extends EX> delegate
    ) throws IOException, EX {

        File temporaryFile = File.createTempFile(prefix, suffix, directory);
        try {

            temporaryFile.deleteOnExit();
            IoUtil.copy(inputStream, closeInputStream, temporaryFile);
            delegate.consume(temporaryFile);
            temporaryFile.delete();
        } finally {
            if (closeInputStream) {
                try { inputStream.close(); } catch (Exception e) {}
            }
            temporaryFile.delete();
        }
    }

    /**
     * Equivalent with {@link #outputFilePrintWriter(File, Charset, ConsumerWhichThrows, boolean)
     * printToFile(<var>file</var>, <var>charset</var>, <var>printer</var>, false)}.
     */
    public static <EX extends Throwable> void
    outputFilePrintWriter(
        File                                                   file,
        Charset                                                charset,
        ConsumerWhichThrows<? super PrintWriter, ? extends EX> printer
    ) throws IOException, EX { IoUtil.outputFilePrintWriter(file, charset, printer, false); }

    /**
     * Lets the <var>delegate</var> print to a {@link PrintWriter} (effectively a temporary file), and eventually
     * renames the temporary file to "<var>file</var>" (replacing a possibly existing file).
     * <p>
     *   In case anthing goes wrong, the temporary file is deleted, and a possibly existing "original" file remains
     *   unchanged.
     * </p>
     *
     * @param delegate                       Prints text to the {@link PrintWriter} it receives
     * @param charset                        The charset to be used for printing
     * @param createMissingParentDirectories Whether to create any missing parent directories for the <var>file</var>
     * @throws IOException                   Creating the temporary file failed
     * @throws IOException                   Closing the temporary file failed
     * @throws IOException                   Deleting the original file (immediately before renaming the temporary
     *                                       file) failed
     * @throws IOException                   Renaming the temporary file failed
     * @throws EX                            The throwable that the <var>delegate</var> may throw
     */
    public static <EX extends Throwable> void
    outputFilePrintWriter(
        File                                                         file,
        final Charset                                                charset,
        final ConsumerWhichThrows<? super PrintWriter, ? extends EX> delegate,
        boolean                                                      createMissingParentDirectories
    ) throws IOException, EX {

        final boolean[] hasError = new boolean[1];
        IoUtil.outputFileOutputStream(
            file,
            new ConsumerWhichThrows<OutputStream, EX>() {

                @Override public void
                consume(OutputStream os) throws EX {
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, charset));
                    delegate.consume(pw);
                    hasError[0] = pw.checkError();
                }
            },
            createMissingParentDirectories
        );
        if (hasError[0]) throw new IOException();
    }

    /**
     * Lets the <var>delegate</var> write to an {@link OutputStream} (effectively a temporary file), and eventually
     * renames the temporary file to "<var>file</var>" (replacing a possibly existing file).
     * <p>
     *   In case anthing goes wrong, the temporary file is deleted, and a possibly existing "original" file remains
     *   unchanged.
     * </p>
     *
     * @throws IOException Creating the temporary file failed
     * @throws IOException Closing the temporary file failed
     * @throws IOException Deleting the original file (immediately before renaming the temporary file) failed
     * @throws IOException Renaming the temporary file failed
     * @throws EX          The <var>delegate</var> threw an <var>EX</var>
     */
    public static <EX extends Throwable> void
    outputFileOutputStream(
        File                                                          file,
        final ConsumerWhichThrows<? super OutputStream, ? extends EX> delegate,
        boolean                                                       createMissingParentDirectories
    ) throws IOException, EX {

        // Bummer we can't generalize this as a generic class: "The generic class CheckedExceptionRuntimeException<EX>
        // may not subclass java.lang.Throwable".
        class IORuntimeException extends RuntimeException {
            private static final long serialVersionUID = 1L;
            private final IOException ioe;
            IORuntimeException(IOException ioe) { this.ioe = ioe; }
        }

        try {

            IoUtil.outputFile(
                file,
                new ConsumerWhichThrows<File, EX>() {

                    @Override public void
                    consume(File file) throws EX {

                        try {

                            OutputStream os = new FileOutputStream(file);
                            try {

                                delegate.consume(os);

                            } catch (RuntimeException re) {

                                try { os.close(); } catch (Exception e2) {}
                                throw re;
                            } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch

                                try { os.close(); } catch (Exception e2) {}
                                throw e;
                            } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch

                                try { os.close(); } catch (Exception e2) {}

                                // "t" must be a checked exception.
                                @SuppressWarnings("unchecked") EX tmp = (EX) t;
                                throw tmp;
                            }
                            os.close();
                        } catch (IOException ioe) {
                            throw new IORuntimeException(ioe);
                        }
                    }
                },
                createMissingParentDirectories
            );
        } catch (IORuntimeException iore) {
            throw iore.ioe; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    /**
     * Creates a temporary file, invokes the <var>delegate</var> with that file, and eventually renames the
     * temporary file to its "real" name (replacing a possibly existing file).
     * <p>
     *   In case anthing goes wrong, the temporary file is deleted, and a possibly existing "original" file remains
     *   unchanged.
     * </p>
     *
     * @throws IOException Deleting the original file (immediately before renaming the temporary file) failed
     * @throws IOException Renaming the temporary file failed
     * @throws EX          The <var>delegate</var> threw an <var>EX</var>
     */
    public static <EX extends Throwable> void
    outputFile(
        File                                            file,
        ConsumerWhichThrows<? super File, ? extends EX> delegate,
        boolean                                         createMissingParentDirectories
    ) throws IOException, EX {

        if (createMissingParentDirectories) IoUtil.createMissingParentDirectoriesFor(file);

        File newFile = new File(file.getParentFile(), "." + file.getName() + ".new");

        try {

            delegate.consume(newFile);

            if (file.exists() && !file.delete()) {
                throw new IOException("Could not delete existing file \"" + file + "\"");
            }
            if (!newFile.renameTo(file)) {
                throw new IOException("Could not rename \"" + newFile + "\" to \"" + file + "\"");
            }
        } catch (RuntimeException re) {
            newFile.delete();

            throw re;
        } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch
            newFile.delete();

            throw e;
        } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
            newFile.delete();

            @SuppressWarnings("unchecked") EX tmp = (EX) t;
            throw tmp;
        }
    }

    /**
     * Creates any missing parent directories for the given <var>file</var>.
     *
     * @throws IOException A directory is missing, but could not be created
     * @throws IOException A non-directory (e.g. a "normal" file) is in the way
     */
    public static void
    createMissingParentDirectoriesFor(File file) throws IOException {

        File parentDirectory = file.getParentFile();
        if (parentDirectory == null) return;

        if (!parentDirectory.isDirectory() && !parentDirectory.mkdirs()) {
            throw new IOException("Cannot create directory \"" + parentDirectory + "\"");
        }
    }

    /**
     * Reads data from the input stream and writes it to the output stream. Closes none of the two streams. Returns
     * when no data is {@link InputStream#available() available} on the <var>inputStream</var>.
     *
     * @return The number of bytes copied
     */
    public static long
    copyAvailable(InputStream inputStream, OutputStream outputStream) throws IOException {
        return IoUtil.copyAvailable(inputStream, outputStream, Long.MAX_VALUE);
    }

    /**
     * Reads at most <var>n</var> bytes from the input stream and writes all data to the output stream. Closes none of
     * the two streams. Returns when no data is {@link InputStream#available() available} on the
     * <var>inputStream</var>.
     *
     * @return The number of bytes copied
     */
    public static long
    copyAvailable(InputStream inputStream, OutputStream outputStream, long n) throws IOException {

        byte[] buffer = new byte[4096];
        long   count  = 0L;
        while (n > 0 && inputStream.available() > 0) {
            try {
                IoUtil.LOGGER.log(Level.FINEST, "About to ''read(byte[{0}])''", buffer.length);
                int m = inputStream.read(buffer, 0, (int) Math.min(n, buffer.length));
                IoUtil.LOGGER.log(Level.FINEST, "''read()'' returned {0}", m);
                if (m == -1) throw new IllegalStateException("EOI despite available()");
                IoUtil.LOGGER.log(Level.FINEST, "About to ''write(byte[{0}])''", m);
                outputStream.write(buffer, 0, m);
                IoUtil.LOGGER.log(Level.FINEST, "'write()' returned");
                count += m;
                n     -= m;
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap(count + " bytes copied so far", ioe);
            }
        }

        outputStream.flush();

        IoUtil.LOGGER.log(Level.FINEST, "{0} bytes copied", count);
        return count;
    }

    /**
     * Creates and returns an {@link InputStream} that reads from the <var>delegate</var> and invokes the
     * <var>runnable</var> on the <em>first</em> end-of-input condition.
     */
    public static InputStream
    onEndOfInput(InputStream delegate, final Runnable runnable) {

        return new FilterInputStream(delegate) {

            boolean hadEndOfInput;

            @Override public int
            read() throws IOException {

                int b = super.read();

                if (b == -1 && !this.hadEndOfInput) {
                    this.hadEndOfInput = true;
                    runnable.run();
                }

                return b;
            }

            @Override public int
            read(@Nullable byte[] b, int off, int len) throws IOException {

                int count = super.read(b, off, len);

                if (count == -1 && !this.hadEndOfInput) {
                    this.hadEndOfInput = true;
                    runnable.run();
                }

                return count;
            }
        };
    }
}
