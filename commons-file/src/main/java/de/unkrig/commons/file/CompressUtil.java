
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

package de.unkrig.commons.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.compressors.CompressorInputStream;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;
import de.unkrig.commons.io.EventCounter;
import de.unkrig.commons.io.ExponentiallyLoggingEventCounter;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.io.MarkableFileInputStream;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/** Utility class which implements functionality that is related to {@code org.apache.commons.compress}. */
public final
class CompressUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final Logger LOGGER                         = Logger.getLogger(CompressUtil.class.getName());
    private static final Logger LOGGER_INPUT_STREAM_STATISTICS = Logger.getLogger(CompressUtil.class.getName() + ".inputStreamStatistics"); // SUPPRESS CHECKSTYLE LineLength

    private CompressUtil() {}

    /**
     * @param <T>                                             {@link #handleArchive(ArchiveInputStream, ArchiveFormat)}
     *                                                        returns a value of this type, which is, in turn, returned
     *                                                        by the {@code processStream()} and {@code processValue()}
     *                                                        methods
     * @see #handleArchive(ArchiveInputStream, ArchiveFormat)
     */
    public
    interface ArchiveHandler<T> {

        /**
         * May or may not read entries and data from the <var>archiveInputStream</var>, and may or may not close it.
         *
         * @see #processFile(File, Predicate, ArchiveHandler, Predicate, CompressorHandler, NormalContentsHandler)
         * @see #processStream(InputStream, Predicate, ArchiveHandler, Predicate, CompressorHandler,
         *      NormalContentsHandler)
         */
        @Nullable T
        handleArchive(ArchiveInputStream archiveInputStream, ArchiveFormat archiveFormat) throws IOException;
    }

    /**
     * @param <T>                                                       {@link #handleCompressor(CompressorInputStream,
     *                                                                  CompressionFormat)} returns a value of this
     *                                                                  type, which is, in turn, returned by the {@code
     *                                                                  processStream()} and {@code processValue()}
     *                                                                  methods
     * @see #handleCompressor(CompressorInputStream, CompressionFormat)
     */
    public
    interface CompressorHandler<T> {

        /**
         * May or may not read from the <var>compressorInputStream</var>.
         *
         * @see #processFile(File, Predicate, ArchiveHandler, Predicate, CompressorHandler, NormalContentsHandler)
         * @see #processStream(InputStream, Predicate, ArchiveHandler, Predicate, CompressorHandler,
         *      NormalContentsHandler)
         */
        @Nullable T
        handleCompressor(CompressorInputStream compressorInputStream, CompressionFormat compressionFormat)
        throws IOException;
    }

    /**
     * @param <T>                             {@link #handleNormalContents(InputStream)} returns a value of this type,
     *                                        which is, in turn, returned by the {@code processStream()} and {@code
     *                                        processValue()} methods
     * @see #handleNormalContents(InputStream)
     */
    public
    interface NormalContentsHandler<T> {

        /**
         * May or may not read from the <var>inputStream</var>, and may or may not close it.
         *
         * @see #processFile(File, Predicate, ArchiveHandler, Predicate, CompressorHandler, NormalContentsHandler)
         * @see CompressUtil#processStream(InputStream, Predicate, ArchiveHandler, Predicate, CompressorHandler,
         *      NormalContentsHandler)
         */
        @Nullable T
        handleNormalContents(InputStream inputStream) throws IOException;
    }

    /**
     * Invokes exactly <i>one</i> of <var>archiveHandler</var>, <var>compressorHandler</var> or
     * <var>normalContentsHandler</var>.
     * <p>
     *   An archive file is introspected iff <var>lookIntoFormat</var> evaluates to {@code true} for {@code
     *   "<i>archive-format-name</i>:<i>path</i>"}.
     * </p>
     * <p>
     *   A compressed file is introspected iff <var>lookIntoFormat</var> evaluates to {@code true} for {@code
     *   "<i>compression-format-name</i>:<i>path</i>"}.
     * </p>
     *
     * @see ArchiveFormatFactory#allFormats()
     * @see CompressionFormatFactory#allFormats()
     */
    @Nullable public static <T> T
    processStream(
        String                             path,
        InputStream                        inputStream,
        Predicate<? super String>          lookIntoFormat,
        ArchiveHandler<? extends T>        archiveHandler,
        CompressorHandler<? extends T>     compressorHandler,
        NormalContentsHandler<? extends T> normalContentsHandler
    ) throws IOException {

        try {

            return CompressUtil.processStream(
                inputStream,                                           // inputStream
                CompressUtil.lookIntoArchive(path, lookIntoFormat),    // lookIntoArchive
                archiveHandler,                                        // archiveHandler
                CompressUtil.lookIntoCompressed(path, lookIntoFormat), // lookIntoCompressed
                compressorHandler,                                     // compressorHandler
                normalContentsHandler                                  // normalContentsHandler
            );
        } catch (UnsupportedZipFeatureException uzfe) {

            // Cannot use "ExceptionUtil.wrap(prefix, cause)" here, because this exception has none of the "usual"
            // constructors.
            throw new IOException(
                path + ": Unsupported ZIP feature \"" + uzfe.getFeature() +  "\"",
                uzfe
            );
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(path, ioe);
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap(path, re);
        } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch
            throw ExceptionUtil.wrap(path, e);
        }

    }

    /**
     * Invokes exactly <i>one</i> of <var>archiveHandler</var>, <var>compressorHandler</var> or
     * <var>normalContentsHandler</var>.
     *
     * @param lookIntoArchive    An archive stream is introspected iff <var>lookIntoArchive</var> evaluates to {@code
     *                           true} for the archive format
     * @param lookIntoCompressed A compressed stream is introspected iff <var>lookIntoCompressed</var> evaluates to
     *                           {@code true} for the compression format
     * @see                      CompressionFormatFactory#allFormats()
     */
    @Nullable public static <T> T
    processStream(
        InputStream                          inputStream,
        Predicate<? super ArchiveFormat>     lookIntoArchive,
        ArchiveHandler<? extends T>          archiveHandler,
        Predicate<? super CompressionFormat> lookIntoCompressed,
        CompressorHandler<? extends T>       compressorHandler,
        NormalContentsHandler<? extends T>   normalContentsHandler
    ) throws IOException {

        if (!inputStream.markSupported()) inputStream = new BufferedInputStream(inputStream);

        ARCHIVE: {
            final ArchiveFormat archiveFormat = ArchiveFormatFactory.forContents(inputStream);
            if (archiveFormat == null) break ARCHIVE;

            if (!lookIntoArchive.evaluate(archiveFormat)) {
                return normalContentsHandler.handleNormalContents(inputStream);
            }

            ArchiveInputStream ais;
            try {
                ais = archiveFormat.archiveInputStream(InputStreams.unclosable(inputStream));
            } catch (ArchiveException ae) {
                throw new IOException(archiveFormat.getName(), ae);
            }

            return archiveHandler.handleArchive(ais, archiveFormat);
        }

        COMPRESSED: {
            final CompressionFormat compressionFormat = CompressionFormatFactory.forContents(inputStream);
            if (compressionFormat == null) break COMPRESSED;

            if (!lookIntoCompressed.evaluate(compressionFormat)) {
                return normalContentsHandler.handleNormalContents(inputStream);
            }

            return compressorHandler.handleCompressor(
                compressionFormat.compressorInputStream(InputStreams.unclosable(inputStream)),
                compressionFormat
            );
        }

        return normalContentsHandler.handleNormalContents(inputStream);
    }

    /**
     * Invokes exactly <i>one</i> of <var>archiveHandler</var>, <var>compressorHandler</var> or
     * <var>normalContentsHandler</var>.
     * <p>
     *   An archive file is introspected iff <var>lookIntoFormat</var> evaluates to {@code true} for
     *   "<i>{@code archive-format-name}</i>{@code :}<i>{@code path}</i>".
     * </p>
     * <p>
     *   A compressed file is introspected iff <var>lookIntoFormat</var> evaluates to {@code true} for "<i>{@code
     *   compression-format-name}</i>{@code :}<i>{@code path}</i>".
     * </p>
     *
     * @param file Must be a "normal file"
     * @see        ArchiveFormatFactory#allFormats()
     * @see        CompressionFormatFactory#allFormats()
     * @see        File#isFile()
     */
    @Nullable public static <T> T
    processFile(
        String                             path,
        File                               file,
        Predicate<? super String>          lookIntoFormat,
        ArchiveHandler<? extends T>        archiveHandler,
        CompressorHandler<? extends T>     compressorHandler,
        NormalContentsHandler<? extends T> normalContentsHandler
    ) throws IOException {
        return CompressUtil.processFile(
            file,
            CompressUtil.lookIntoArchive(path, lookIntoFormat),
            archiveHandler,
            CompressUtil.lookIntoCompressed(path, lookIntoFormat),
            compressorHandler,
            normalContentsHandler
        );
    }

    /**
     * Invokes exactly <i>one</i> of <var>archiveHandler</var>, <var>compressorHandler</var> or
     * <var>normalContentsHandler</var>.
     *
     * @param file               Must be a "normal file"
     * @param lookIntoArchive    An archive file is introspected iff <var>lookIntoArchive</var> evaluates to {@code
     *                           true} for the archive format
     * @param lookIntoCompressed A compressed file is introspected iff <var>lookIntoCompressed</var> evaluates to
     *                           {@code true} for the compression format
     * @see                      CompressionFormatFactory#allFormats()
     */
    @Nullable public static <T> T
    processFile(
        final File                           file,
        Predicate<? super ArchiveFormat>     lookIntoArchive,
        ArchiveHandler<? extends T>          archiveHandler,
        Predicate<? super CompressionFormat> lookIntoCompressed,
        CompressorHandler<? extends T>       compressorHandler,
        NormalContentsHandler<? extends T>   normalContentsHandler
    ) throws IOException {

        InputStream is = CompressUtil.markableFileInputStream(file);

        try {

            ArchiveFormat archiveFormat = ArchiveFormatFactory.forContents(is);
            if (archiveFormat != null) {

                if (!lookIntoArchive.evaluate(archiveFormat)) {
                    return normalContentsHandler.handleNormalContents(is);
                }

                is.close();
                ArchiveInputStream ais = archiveFormat.open(file);
                is = ais;

                T result = archiveHandler.handleArchive(ais, archiveFormat);

                ais.close();
                return result;
            }

            CompressionFormat compressionFormat = CompressionFormatFactory.forContents(is);
            if (compressionFormat != null) {

                if (!lookIntoCompressed.evaluate(compressionFormat)) {
                    return normalContentsHandler.handleNormalContents(is);
                }

                CompressorInputStream cis = compressionFormat.compressorInputStream(new BufferedInputStream(is));

                T result = compressorHandler.handleCompressor(cis, compressionFormat);

                cis.close();
                return result;
            }

            CompressUtil.LOGGER.log(Level.FINER, "Processing normal file \"{0}\"", file);
            T result = normalContentsHandler.handleNormalContents(is);

            is.close();
            return result;
        } catch (UnsupportedZipFeatureException uzfe) {

            // Cannot use "ExceptionUtil.wrap(prefix, cause)" here, because this exception has none of the "usual"
            // constructors.
            throw new IOException(
                file + "!" + uzfe.getEntry().getName() + ": Unsupported ZIP feature \"" + uzfe.getFeature() +  "\"",
                uzfe
            );
        } catch (ArchiveException ae) {
            throw ExceptionUtil.wrap(file.toString(), ae, IOException.class);
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(file.toString(), ioe);
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap(file.toString(), re);
        } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch
            throw ExceptionUtil.wrap(file.toString(), e);
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    private static Predicate<ArchiveFormat>
    lookIntoArchive(final String path, final Predicate<? super String> lookIntoFormat) {

        return new Predicate<ArchiveFormat>() {

            @Override public boolean
            evaluate(ArchiveFormat af) {

                boolean result = lookIntoFormat.evaluate(af.getName() + ':' + path);
                CompressUtil.LOGGER.log(
                    Level.FINER,
                    "Look into archive \"{0}\"? => {1}",
                    new Object[] { path, result }
                );

                return result;
            }
        };
    }

    private static Predicate<CompressionFormat>
    lookIntoCompressed(final String path, final Predicate<? super String> lookIntoFormat) {

        return new Predicate<CompressionFormat>() {

            @Override public boolean
            evaluate(CompressionFormat cf) {

                boolean result = lookIntoFormat.evaluate(cf.getName() + ':' + path);

                CompressUtil.LOGGER.log(
                    Level.FINER,
                    "Look into compressed \"{0}\"? => {1}",
                    new Object[] { path, result }
                );

                return result;
            }
        };
    }

    private static final EventCounter
    FILE_INPUT_STREAM_STATISTICS = new ExponentiallyLoggingEventCounter(
        "fileInputStream",
        CompressUtil.LOGGER_INPUT_STREAM_STATISTICS,
        Level.FINE
    );

    private static InputStream
    markableFileInputStream(final File file) throws FileNotFoundException {
        return InputStreams.statisticsInputStream(
            new MarkableFileInputStream(file),
            CompressUtil.FILE_INPUT_STREAM_STATISTICS
        );
    }
}
