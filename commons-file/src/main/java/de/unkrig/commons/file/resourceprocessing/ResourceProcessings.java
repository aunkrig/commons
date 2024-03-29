
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
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

package de.unkrig.commons.file.resourceprocessing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Pattern;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings.ArchiveCombiner;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessor;
import de.unkrig.commons.file.fileprocessing.FileProcessings;
import de.unkrig.commons.file.fileprocessing.FileProcessings.DirectoryCombiner;
import de.unkrig.commons.file.fileprocessing.FileProcessor;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.concurrent.ConcurrentUtil;
import de.unkrig.commons.util.concurrent.SquadExecutor;

/** {@link ResourceProcessor}-related utility methods. */
public final
class ResourceProcessings {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private ResourceProcessings() {}

    /**
     * Returns a {@link ResourceProcessor} which processes documents by feeding them to the {@code
     * normalContentsProcessor}, but automagically detects various archive and compression formats and processes the
     * <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the "raw" contents.
     * <p>
     *   Archive files and compressed files are introspected iff <var>lookIntoFormat</var> evaluates {@code true} for
     *   "<i>format</i><b>:</b><i>path</i>".
     * </p>
     * <p>
     *   Does not check whether a file is a directory.
     * </p>
     *
     * @see ContentsProcessings#compressedAndArchiveContentsProcessor(Predicate, Predicate, ContentsProcessor,
     *      ArchiveCombiner, ContentsProcessor, ContentsProcessor, ExceptionHandler)
     */
    public static <T> ResourceProcessor<T>
    compressedAndArchiveResourceProcessor(
        final Predicate<? super String>     lookIntoFormat,
        final Predicate<? super String>     pathPredicate,
        final ContentsProcessor<T>          archiveContentsProcessor,
        final ArchiveCombiner<T>            archiveEntryCombiner,
        final ContentsProcessor<T>          compressedContentsProcessor,
        final ContentsProcessor<T>          normalContentsProcessor,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return ResourceProcessings.fromContentsProcessorAndFileProcessor(
            ContentsProcessings.compressedAndArchiveContentsProcessor(
                lookIntoFormat,
                pathPredicate,
                archiveContentsProcessor,
                archiveEntryCombiner,
                compressedContentsProcessor,
                normalContentsProcessor,
                exceptionHandler
            ),
            FileProcessings.compressedAndArchiveFileProcessor(
                lookIntoFormat,
                pathPredicate,
                archiveContentsProcessor,
                archiveEntryCombiner,
                compressedContentsProcessor,
                normalContentsProcessor,
                exceptionHandler
            )
        );
    }

    /**
     * Returns a {@link ResourceProcessor} which processes a document by feeding it into the {@code
     * normalContentsProcessor}, but automagically detects various archive formats and compression formats (also
     * nested) and processes the <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the
     * "raw" contents.
     * <p>
     *   Archive streams/entries and compressed streams/entries are introspected iff <var>lookIntoFormat</var> evaluates
     *   {@code true} for "<i>format</i><b>:</b><i>path</i>".
     * </p>
     *
     * @param directoryMemberNameComparator The comparator used to sort a directory's members; a {@code null} value
     *                                      means to NOT sort the members, i.e. leave them in their 'natural' order as
     *                                      {@link File#list()} returns them
     * @param squadExecutor                 Is used to process independent subtrees - could be {@link
     *                                      ConcurrentUtil#SEQUENTIAL_EXECUTOR_SERVICE}
     * @see                                 ContentsProcessings#recursiveCompressedAndArchiveContentsProcessor(
     *                                      Predicate, Predicate, ArchiveCombiner, ContentsProcessor, ExceptionHandler)
     */
    public static <T> ResourceProcessor<T>
    recursiveCompressedAndArchiveResourceProcessor(
        final Predicate<? super String>    lookIntoFormat,
        final Predicate<? super String>    pathPredicate,
        @Nullable final Comparator<Object> directoryMemberNameComparator,
        boolean                            recurseSubdirectories,
        final DirectoryCombiner<T>         directoryEntryCombiner,
        final ArchiveCombiner<T>           archiveEntryCombiner,
        final ContentsProcessor<T>         normalContentsProcessor,
        final SquadExecutor<T>             squadExecutor,
        ExceptionHandler<IOException>      exceptionHandler
    ) {

        FileProcessor<T> fp = FileProcessings.recursiveCompressedAndArchiveFileProcessor(
            lookIntoFormat,
            pathPredicate,
            archiveEntryCombiner,
            normalContentsProcessor,
            exceptionHandler
        );

        if (recurseSubdirectories) {
            fp = FileProcessings.directoryTreeProcessor(
                pathPredicate,
                fp,                            // regularFileProcessor
                directoryMemberNameComparator,
                directoryEntryCombiner,
                squadExecutor,
                exceptionHandler
            );
        } else {
            fp = FileProcessings.directoryProcessor(
                pathPredicate,
                fp,                            // regularFileProcessor
                directoryMemberNameComparator,
                fp,                            // directoryMemberProcessor
                directoryEntryCombiner,
                squadExecutor,
                exceptionHandler
            );
        }

        ContentsProcessor<T> cp = ContentsProcessings.recursiveCompressedAndArchiveContentsProcessor(
            lookIntoFormat,
            pathPredicate,
            archiveEntryCombiner,
            normalContentsProcessor,
            exceptionHandler
        );

        return ResourceProcessings.fromContentsProcessorAndFileProcessor(cp, fp);
    }

    /**
     * @return A {@link ResourceProcessor} that wraps the <var>delegateCp</var> and the (optional) <var>delegateFp</var>
     */
    public static <T> ResourceProcessor<T>
    fromContentsProcessorAndFileProcessor(
        final ContentsProcessor<T>       delegateCp,
        @Nullable final FileProcessor<T> delegateFp
    ) {

        return new ResourceProcessor<T>() {

            @Override @Nullable public T
            process(String path, final URL location) throws IOException, InterruptedException {

                // The special URL "ResourceProcessings.STDIN_URL" designates standard input ("System.in"):
                if (location.equals(ResourceProcessings.STDIN_URL)) {
                    byte[] input = InputStreams.readAll(System.in);
                    return delegateCp.process(
                        "-",                                                  // path
                        new ByteArrayInputStream(input),                      // inputStream
                        new Date(),                                           // lastModifiedDate
                        -1,                                                   // size
                        -1,                                                   // crc32
                        new ProducerWhichThrows<InputStream, IOException>() { // opener

                            @Override @Nullable public InputStream
                            produce() { return new ByteArrayInputStream(input); }
                        }
                    );
                }

                // Optimize processing for files.
                if (delegateFp != null) {
                    File file = ResourceProcessings.isFile(location);
                    if (file != null) {
                        return delegateFp.process(path, file);
                    }
                }

                ProducerWhichThrows<InputStream, IOException>
                opener = new ProducerWhichThrows<InputStream, IOException>() {

                    @Override @Nullable public InputStream
                    produce() throws IOException { return location.openConnection().getInputStream(); }
                };

                final URLConnection conn = location.openConnection();
//                conn.setAllowUserInteraction(true);

                long size = -1, crc32 = -1;

                if (conn instanceof JarURLConnection) {
                    JarURLConnection juc = (JarURLConnection) conn;
                    size  = juc.getJarEntry().getSize();
                    crc32 = juc.getJarEntry().getCrc();
                } else
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection huc = (HttpURLConnection) conn;
                    size = huc.getContentLength();
                }

                long lastModified     = conn.getLastModified();
                Date lastModifiedDate = lastModified == 0 ? null : new Date(lastModified);

                InputStream is = conn.getInputStream();
                try {
                    T result = delegateCp.process(path, is, lastModifiedDate, size, crc32, opener);
                    is.close();
                    return result;
                } finally {
                    try { is.close(); } catch (Exception e) {}
                }
            }
        };
    }

    /**
     * Converts a string into a {@link URL}.
     * <ul>
     *   <li>Iff <var>filePathnameOrUrl</var> appears to be a URL, use it to construct that {@link URL}</li>
     *   <li>Otherwise, construct a {@code file:} URL that designates the file with that pathname</li>
     * </ul>
     * <p>
     *   The special string "-" is converted to {@link #STDIN_URL}. That URL cannot be opened; it must be checked with
     *   {@code "myUrl.equals(ResourceProcessings.STDIN_URL)"}.
     * </p>
     */
    public static URL
    toUrl(String filePathnameOrUrl) throws MalformedURLException {
        return (
            ResourceProcessings.LOOKS_LIKE_URL.matcher(filePathnameOrUrl).find()
            ? new URL(filePathnameOrUrl)
            : "-".equals(filePathnameOrUrl)
            ? ResourceProcessings.STDIN_URL
            : new File(filePathnameOrUrl).toURI().toURL()
        );
    }

    /**
     * Many methods use this special URL as a representation for "standard input". However, that URL cannot be opened
     * as usual; it must be checked with {@code "myUrl.equals(ResourceProcessings.STDIN_URL)"}.
     */
    public static final URL STDIN_URL = ResourceProcessings.stdinUrl();
    private static URL
    stdinUrl() {
        try {
            return new URL("file", null /*host*/, "-" /*path*/);
        } catch (MalformedURLException mue) {
            throw new ExceptionInInitializerError(mue);
        }
    }

    // https://tools.ietf.org/html/rfc1738#section-5 says:
    //   ; the scheme is in lower case; interpreters should use case-ignore
    //   scheme         = 1*[ lowalpha | digit | "+" | "-" | "." ]
    // MS WINDOWS file pathnames can start with "x:", so assume that a URL scheme is at least TWO letters long.
    private static final Pattern LOOKS_LIKE_URL = Pattern.compile("^[A-Za-z0-9+\\-.]{2,}:");

    /**
     * @return {@code null} iff the <var>location</var> does not designate a file
     */
    @Nullable public static File
    isFile(URL location) {

        if (!location.getProtocol().equalsIgnoreCase("file")) return null;

        // NOTICE: Only the java.net.URI decodes %xx escape sequences (the java.net.URL does NOT!).
        URI uri;
        try { uri = location.toURI(); } catch (URISyntaxException e) { throw new AssertionError(e); }

        return new File(uri);
    }
}
