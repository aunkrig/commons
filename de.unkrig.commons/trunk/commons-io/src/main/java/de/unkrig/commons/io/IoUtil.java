
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.Segment;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ClassLoaders;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ThreadUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.lang.protocol.RunnableWhichThrows;
import de.unkrig.commons.lang.protocol.Transformer;
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
     * @param path         Each element should designate either a directory or a JAR (or ZIP) file
     * @param resourceName Must not start with a "/"
     * @return             {@code null} if {@code path == null}, or if the resource could not be found
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
                URL url = new URL("jar", null, directoryOrArchiveFile.toURI() + "!/" + resourceName);
                try {
                    url.openConnection().connect();
                    return url;
                } catch (FileNotFoundException fnfe) {

                    // A resource with that name does not exist in the archive.
                    ;
                }
            } else
            {

                // The path entry designates neither an existing directory nor a JAR (or ZIP) archive.
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
     * Reads chunks from <var>in</var>, repeatedly, (until end-of-input), transforms each chunk through the
     * <var>transformer</var>, and writes the results to <var>out</var>.
     *
     * @param bufferCapacity The number of chars that are read per chunk
     */
    public static void
    copyAndTransform(
        Reader                                                    in,
        Transformer<? super CharSequence, ? extends CharSequence> transformer,
        Appendable                                                out,
        int                                                       bufferCapacity
    ) throws IOException {

        char[] buffer = new char[bufferCapacity];
        for (;;) {
            int n = in.read(buffer);
            if (n == -1) break;
            out.append(transformer.transform(new Segment(buffer, 0, n)));
        }

        out.append(transformer.transform(""));
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

    /** @deprecated Use {@link OutputStreams#tee(OutputStream...)} instead */
    @Deprecated @NotNullByDefault(false) public static OutputStream
    tee(final OutputStream... delegates) { return OutputStreams.tee(delegates); }

    /** @deprecated Use {@link InputStreams#wye(InputStream, OutputStream)} instead*/
    @Deprecated public static InputStream
    wye(InputStream in, final OutputStream out) { return InputStreams.wye(in, out); }

    /** @deprecated Use {@link OutputStreams#writeAndCount(ConsumerWhichThrows, OutputStream)} instead */
    @Deprecated public static long
    writeAndCount(
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents,
        OutputStream                                                     outputStream
    ) throws IOException { return OutputStreams.writeAndCount(writeContents, outputStream); }

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

    /** @deprecated Use {@link InputStreams#readAll(InputStream)} instead */
    @Deprecated public static byte[]
    readAll(InputStream is) throws IOException { return InputStreams.readAll(is); }

    /** @deprecated Use {@link InputStreams#readAll(InputStream, Charset, boolean)} instead */
    @Deprecated public static String
    readAll(InputStream inputStream, Charset charset, boolean closeInputStream) throws IOException {
        return InputStreams.readAll(inputStream, charset, closeInputStream);
    }

    /** @deprecated Use {@link InputStreams#skip(InputStream, long)} instead */
    @Deprecated public static long
    skip(InputStream inputStream, long n) throws IOException { return InputStreams.skip(inputStream, n); }

    /** @deprecated Use {@link InputStreams#skipAll(InputStream)} instead */
    @Deprecated public static long
    skipAll(InputStream inputStream) throws IOException { return InputStreams.skipAll(inputStream); }

    /** @deprecated Use {@link OutputStreams#split(ProducerWhichThrows, Producer)} instead */
    @Deprecated public static OutputStream
    split(
        final ProducerWhichThrows<? extends OutputStream, ? extends IOException> delegates,
        final Producer<? extends Long>                                           byteCountLimits
    ) throws IOException { return OutputStreams.split(delegates, byteCountLimits); }

    /** @deprecated Use {@link InputStreams#EMPTY} instead */
    @Deprecated public static final InputStream
    EMPTY_INPUT_STREAM = InputStreams.EMPTY;

    /** @deprecated Use {@link OutputStreams#DISCARD} instead */
    @Deprecated public static final OutputStream
    NULL_OUTPUT_STREAM = OutputStreams.DISCARD;

    /** @deprecated Use {@link InputStreams#constantInputStream(byte)} instead */
    @Deprecated public static InputStream
    constantInputStream(final byte b) { return InputStreams.constantInputStream(b); }

    /** @deprecated Use {@link InputStreams#ZERO} instead */
    @Deprecated public static final InputStream
    ZERO_INPUT_STREAM = InputStreams.ZERO;

    /** @deprecated Use {@link InputStreams#unclosable(InputStream)} instead */
    @Deprecated public static InputStream
    unclosableInputStream(InputStream delegate) { return InputStreams.unclosable(delegate); }

    /** @deprecated Use {@link OutputStreams#unclosable(OutputStream)} instead */
    @Deprecated public static OutputStream
    unclosableOutputStream(OutputStream delegate) { return OutputStreams.unclosable(delegate); }

    /** @deprecated Use {@link OutputStreams#fill(OutputStream, byte, long)} instead */
    @Deprecated public static void
    fill(OutputStream outputStream, byte b, long count) throws IOException {
        OutputStreams.fill(outputStream, b, count);
    }

    /** @deprecated Use {@link InputStreams#byteProducerInputStream(ProducerWhichThrows)} instead */
    @Deprecated public static InputStream
    byteProducerInputStream(final ProducerWhichThrows<? extends Byte, ? extends IOException> delegate) {
        return InputStreams.byteProducerInputStream(delegate);
    }

    /** @deprecated Use {@link InputStreams#byteProducerInputStream(Producer)} instead */
    @Deprecated public static InputStream
    byteProducerInputStream(final Producer<? extends Byte> delegate) {
        return InputStreams.byteProducerInputStream(delegate);
    }

    /** @deprecated Use {@link InputStreams#randomInputStream(long)} instead */
    @Deprecated public static InputStream
    randomInputStream(final long seed) { return InputStreams.randomInputStream(seed); }

    /** @deprecated Use {@link OutputStreams#byteConsumerOutputStream(ConsumerWhichThrows)} instead */
    @Deprecated public static OutputStream
    byteConsumerOutputStream(final ConsumerWhichThrows<? super Byte, ? extends IOException> delegate) {
        return OutputStreams.byteConsumerOutputStream(delegate);
    }

    /** @deprecated Use {@link Readers#readAll(Reader)} instead */
    @Deprecated public static String
    readAll(Reader reader) throws IOException { return Readers.readAll(reader); }

    /** @deprecated Use {@link Readers#readAll(Reader, boolean)} instead */
    @Deprecated public static String
    readAll(Reader reader, boolean closeReader) throws IOException { return Readers.readAll(reader, closeReader); }

    /** @deprecated Use {@link InputStreams#deleteOnClose(InputStream, File)} instead */
    @Deprecated protected static InputStream
    deleteOnClose(InputStream delegate, final File file) { return InputStreams.deleteOnClose(delegate, file); }

    /** @deprecated Use {@link OutputStreams#compareOutput(int, Runnable, Runnable)} instead */
    @Deprecated public static OutputStream[]
    compareOutput(final int n, final Runnable whenIdentical, final Runnable whenNotIdentical) {
        return OutputStreams.compareOutput(n, whenIdentical, whenNotIdentical);
    }

    /** @deprecated Use {@link OutputStreams#lengthWritten(Consumer)} instead */
    @Deprecated public static OutputStream
    lengthWritten(final Consumer<? super Integer> delegate) { return OutputStreams.lengthWritten(delegate); }

    /** @deprecated Use {@link Readers#asReader(CharSequence)} instead */
    @Deprecated public static Reader
    asReader(final CharSequence cs) { return Readers.asReader(cs); }

    /**
     * Copies the contents of a resource to the given <var>outputStream</var>.
     * <p>
     *   The resource is addressed by the <var>classLoader</var> and the <var>resourceName</var>, as described for
     *   {@link ClassLoader#getResourceAsStream(String)}.
     * </p>
     *
     * @param resourceName      Must not start with a "/"
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
     * @param resourceName      Must not start with a "/"
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
     * @param resourceName                   Must not start with a "/"
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
     * @param resourceName                   Must not start with a "/"
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

    /** @deprecated Use {@link InputStreams#onEndOfInput(InputStream, Runnable)} instead */
    @Deprecated public static InputStream
    onEndOfInput(InputStream delegate, final Runnable runnable) {
        return InputStreams.onEndOfInput(delegate, runnable);
    }

    /** @deprecated Use {@link Readers#singlingFilterReader(Reader)} instead */
    @Deprecated public static Reader
    singlingFilterReader(Reader delegate) { return Readers.singlingFilterReader(delegate); }

    /** @deprecated Use {@link InputStreams#singlingFilterInputStream(InputStream)} instead */
    @Deprecated public static InputStream
    singlingFilterInputStream(InputStream delegate) { return InputStreams.singlingFilterInputStream(delegate); }
}
