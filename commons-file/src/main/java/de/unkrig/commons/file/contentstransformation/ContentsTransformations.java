
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

package de.unkrig.commons.file.contentstransformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

import de.unkrig.commons.file.CompressUtil;
import de.unkrig.commons.file.CompressUtil.ArchiveHandler;
import de.unkrig.commons.file.CompressUtil.CompressorHandler;
import de.unkrig.commons.file.CompressUtil.NormalContentsHandler;
import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.filetransformation.FileTransformations;
import de.unkrig.commons.file.filetransformation.FileTransformations.ArchiveCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformations.NameAndContents;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.io.ByteFilterInputStream;
import de.unkrig.commons.io.ByteFilterOutputStream;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.HardReference;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Glob;

/**
 * Utility class for {@link ContentsTransformer}.
 */
public final
class ContentsTransformations {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private
    ContentsTransformations() {}

    /**
     * A {@link ContentsTransformer} which {@link IoUtil#copy(InputStream, OutputStream)}s all data unmodified and
     * returns the number of copied bytes.
     */
    public static final ContentsTransformer
    COPY = new ContentsTransformer() {

        @Override public void
        transform(String name, InputStream is, OutputStream os) throws IOException { IoUtil.copy(is, os); }

        @Override public String
        toString() { return "COPY"; }
    };

    /**
     * Returns a {@link ContentsTransformer} which transforms contents by feeding it through the {@code
     * normalContentsTransformer}, but automagically detects various archive and compression formats and processes the
     * <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the "raw" contents.
     * <p>
     *   Archive files and compressed files are introspected iff {@code lookIntoFormat} evaluates {@code true} for
     *   "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static ContentsTransformer
    compressedAndArchiveContentsTransformer(
        final Predicate<? super String>     lookIntoFormat,
        final Predicate<? super String>     archiveEntryRemoval,
        final Glob                          archiveEntryRenaming,
        final ContentsTransformer           archiveEntryContentsTransformer,
        final ArchiveCombiner               archiveCombiner,
        final ContentsTransformer           compressedContentsTransformer,
        final ContentsTransformer           normalContentsTransformer,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return new ContentsTransformer() {

            @Override public void
            transform(final String path, InputStream is, OutputStream os) throws IOException {

                CompressUtil.<Void>processStream(
                    is,                                            // is
                    new Predicate<ArchiveFormat>() {               // lookIntoArchive

                        @Override public boolean
                        evaluate(ArchiveFormat af) { return lookIntoFormat.evaluate(af.getName() + ':' + path); }

                        @Override public String
                        toString() { return "path =* " + lookIntoFormat; }
                    },
                    ContentsTransformations.archiveHandler(        // archiveHandler
                        path,
                        os,
                        archiveEntryRemoval,
                        archiveEntryRenaming,
                        archiveEntryContentsTransformer,
                        archiveCombiner,
                        exceptionHandler
                    ),
                    new Predicate<CompressionFormat>() {           // lookIntoCompressed

                        @Override public boolean
                        evaluate(CompressionFormat cf) { return lookIntoFormat.evaluate(cf.getName() + ':' + path); }

                        @Override public String
                        toString() { return "compression-format + ':' + path =* " + lookIntoFormat; }
                    },
                    ContentsTransformations.compressorHandler(     // compressorHandler
                        path,
                        os,
                        compressedContentsTransformer
                    ),
                    ContentsTransformations.normalContentsHandler( // normalContentsHandler
                        path,
                        os,
                        normalContentsTransformer
                    )
                );
            }

            @Override public String
            toString() { return "compressedAndArchiveContentsTransformer"; }
        };
    }

    /**
     * Returns a {@link ContentsTransformer} which transforms a stream by feeding it through the {@code delegate}, but
     * automagically detects various archive formats and compression formats (also nested) and processes the
     * <i>entries of the archive</i> and the <i>uncompressed contents</i> instead of the "raw" contents.
     * <p>
     *   Archive streams/entries and compressed streams/entries are introspected iff {@code lookIntoFormat} evaluates
     *   {@code true} for "<i>format</i><b>:</b><i>path</i>".
     * </p>
     */
    public static ContentsTransformer
    recursiveCompressedAndArchiveContentsTransformer(
        final Predicate<? super String> lookIntoFormat,
        final Predicate<? super String> archiveEntryRemoval,
        final Glob                      archiveEntryRenaming,
        final ArchiveCombiner           archiveCombiner,
        final ContentsTransformer       delegate,
        ExceptionHandler<IOException>   exceptionHandler
    ) {
        final HardReference<ContentsTransformer> loopback = new HardReference<ContentsTransformer>();

        // To implement the "feedback look" it is necessary to create a temporary contents transformer.
        ContentsTransformer tmp = new ContentsTransformer() {

            @Override public void
            transform(String name, InputStream is, OutputStream os) throws IOException {
                ContentsTransformer l = loopback.get();
                assert l != null;
                l.transform(name, is, os);
            }
        };
        ContentsTransformer result = ContentsTransformations.compressedAndArchiveContentsTransformer(
            lookIntoFormat,       // lookIntoFormat
            archiveEntryRemoval,  // archiveEntryRemoval
            archiveEntryRenaming, // archiveEntryRenaming
            tmp,                  // archiveEntryContentsTransformer
            archiveCombiner,      // archiveCombiner
            tmp,                  // compressedContentsTransformer
            delegate,             // normalContentsTransformer
            exceptionHandler      // exceptionHandler
        );
        loopback.set(result);
        return result;
    }

    /**
     * Transforms the given {@code archiveInputStream} into the given {@code archiveOutputStream}, honoring the given
     * {@code archiveEntryRemoval}, {@code archiveEntryRenaming} and {@code archiveEntryAddition}, and using the given
     * {@code contentsTransformer}.
     */
    public static void
    transformArchive(
        final String                    path,
        final ArchiveInputStream        archiveInputStream,
        final ArchiveOutputStream       archiveOutputStream,
        final Predicate<? super String> archiveEntryRemoval,
        final Glob                      archiveEntryRenaming,
        final ContentsTransformer       contentsTransformer,
        final ArchiveCombiner           archiveCombiner,
        ExceptionHandler<IOException>   exceptionHandler
    ) throws IOException {

        @SuppressWarnings("deprecation") final ArchiveFormat
        outputFormat = ArchiveFormatFactory.forArchiveOutputStream(archiveOutputStream);

        for (
            ArchiveEntry ae = archiveInputStream.getNextEntry();
            ae != null;
            ae = archiveInputStream.getNextEntry()
        ) {
            final String entryPath = path + '!' + ArchiveFormatFactory.normalizeEntryName(ae.getName());

            // Is the entry to be removed?
            if (archiveEntryRemoval.evaluate(entryPath)) continue;

            // Is the entry to be renamed?
            String newName = archiveEntryRenaming.replace(entryPath);
            if (newName != null) {
                {
                    int idx = newName.lastIndexOf('!');
                    if (idx != -1) {
                        if (!path.equals(newName.substring(0, idx))) {
                            throw new IOException(
                                "Cannot rename '"
                                + entryPath
                                + "' across archive boundaries"
                            );
                        }
                        newName = newName.substring(idx + 1);
                        assert newName != null;
                    }
                }
            }

            // Now append the entry to the output.
            try {
                outputFormat.writeEntry(
                    archiveOutputStream,
                    ae,
                    newName,
                    new ConsumerWhichThrows<OutputStream, IOException>() {

                        @Override public void
                        consume(OutputStream os) throws IOException {
                            contentsTransformer.transform(entryPath, archiveInputStream, os);
                        }

                        @Override public String
                        toString() { return "WRITE CONTENTS OF ARCHIVE ENTRY '" + entryPath + "'"; }
                    }
                );
            } catch (IOException ioe) {
                exceptionHandler.handle(path, ExceptionUtil.wrap("Transforming entry '" + ae + "'", ioe));
            } catch (RuntimeException re) {
                exceptionHandler.handle(path, ExceptionUtil.wrap("Transforming entry '" + ae + "'", re));
            }
        }

        // Optionally append new entries to the output.
        archiveCombiner.combineArchive(
            path,
            new ConsumerWhichThrows<FileTransformations.NameAndContents, IOException>() { // entryAdder

                @Override public void
                consume(final NameAndContents nac) throws IOException {
                    outputFormat.writeEntry(
                        archiveOutputStream,
                        nac.getName(),
                        new ConsumerWhichThrows<OutputStream, IOException>() {

                            @Override public void
                            consume(OutputStream os) throws IOException { IoUtil.copy(nac.open(), true, os, false); }
                        }
                    );
                }

                @Override public String
                toString() { return "ADD TO ARCHIVE"; }
            }
        );
    }

    /**
     * Creates and returns an {@link ArchiveHandler} which transforms {@link ArchiveInputStream}s into {@link
     * ArchiveOutputStream}s, honoring the given {@code archiveEntryRemoval}, {@code archiveEntryRenaming} and {@code
     * archiveEntryAddition}, and using the given {@code contentsTransformer}.
     */
    public static ArchiveHandler<Void>
    archiveHandler(
        final String                        path,
        final OutputStream                  os,
        final Predicate<? super String>     archiveEntryRemoval,
        final Glob                          archiveEntryRenaming,
        final ContentsTransformer           contentsTransformer,
        final ArchiveCombiner               archiveFileCombiner,
        final ExceptionHandler<IOException> exceptionHandler
    ) {

        return new ArchiveHandler<Void>() {

            @Override @Nullable public Void
            handleArchive(final ArchiveInputStream archiveInputStream, final ArchiveFormat archiveFormat)
            throws IOException {

                final ArchiveOutputStream aos;
                try {
                    aos = archiveFormat.archiveOutputStream(os);
                } catch (ArchiveException ae) {
                    throw new IOException(path, ae);
                }

                ContentsTransformations.transformArchive(
                    path,
                    archiveInputStream,
                    aos,
                    archiveEntryRemoval,
                    archiveEntryRenaming,
                    contentsTransformer,
                    archiveFileCombiner,
                    exceptionHandler
                );
                aos.finish();

                return null;
            }
        };
    }

    /**
     * Creates and returns a {@link CompressorHandler} which transforms an {@link ArchiveInputStream} into an {@link
     * OutputStream}, using the given {@code contentsTransformer}.
     */
    public static CompressorHandler<Void>
    compressorHandler(
        final String              path,
        final OutputStream        os,
        final ContentsTransformer contentsTransformer
    ) {

        return new CompressorHandler<Void>() {

            @Override @Nullable public Void
            handleCompressor(CompressorInputStream compressorInputStream, CompressionFormat compressionFormat)
            throws IOException {

                CompressorOutputStream cos;
                try {
                    cos = compressionFormat.compressorOutputStream(os);
                } catch (CompressorException ce) {
                    throw new IOException(ce);
                }

                contentsTransformer.transform(path + '!', compressorInputStream, cos);

                cos.flush();
                return null;
            }
        };
    }

    /**
     * Creates and returns a handler which transforms an {@link InputStream} into an {@link OutputStream} using the
     * given {@code contentsTransformer}.
     */
    public static NormalContentsHandler<Void>
    normalContentsHandler(final String path, final OutputStream os, final ContentsTransformer contentsTransformer) {

        return new NormalContentsHandler<Void>() {

            @Override @Nullable public Void
            handleNormalContents(InputStream inputStream) throws IOException {
                contentsTransformer.transform(path, inputStream, os);
                return null;
            }
        };
    }

    /**
     * Creates and returns a {@link ContentsTransformer}s that "chains" the two delegates.
     * <p>
     *   If neither of the two delegate transformers is {@link ContentsTransformations#COPY}, then the returned
     *   transfomer, when executed, will create and later joind one background thread.
     * </p>
     */
    public static ContentsTransformer
    chain(final ContentsTransformer transformer1, final ContentsTransformer transformer2) {

        if (transformer1 == ContentsTransformations.COPY) return transformer2;

        if (transformer2 == ContentsTransformations.COPY) return transformer1;

        return new ContentsTransformer() {

            @Override public void
            transform(final String name, InputStream is, OutputStream os) throws IOException {

                transformer2.transform(
                    name,
                    new ByteFilterInputStream(is, new ContentsTransformerByteFilter(transformer1, name)),
                    os
                );
            }
        };
    }

    /**
     * Creates and returns an {@link InputStream} that reads from the <var>delegate</var> and <em>through</em> the
     * <var>transformer</var>
     *
     * @param name Designates the contents that is transformed; may be used by the <var>transformer</var> e.g. to
     *             decide how to transform the contents
     */
    public static InputStream
    asInputStream(InputStream delegate, ContentsTransformer transformer, String name) {

        if (transformer == ContentsTransformations.COPY) return delegate;

        return new ByteFilterInputStream(delegate, new ContentsTransformerByteFilter(transformer, name));
    }

    /**
     * Creates and returns an {@link OutputStream} that writes <em>through</em> the <var>transformer</var> and to the
     * <var>delegate</var>.
     *
     * @param name Designates the contents that is transformed; may be used by the <var>transformer</var> e.g. to
     *             decide how to transform the contents
     */
    public static OutputStream
    asOutputStream(ContentsTransformer transformer, OutputStream delegate, String name) {

        if (transformer == ContentsTransformations.COPY) return delegate;

        return new ByteFilterOutputStream(new ContentsTransformerByteFilter(transformer, name), delegate);
    }
}
