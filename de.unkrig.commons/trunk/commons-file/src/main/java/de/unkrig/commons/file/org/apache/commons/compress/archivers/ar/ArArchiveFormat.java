
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers.ar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.compressors.FileNameUtil;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.io.pipe.PipeFactory;
import de.unkrig.commons.io.pipe.PipeUtil;
import de.unkrig.commons.io.pipe.PipeUtil.InputOutputStreams;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.collections.MapUtil;

/**
 * Representation of the 'ar' archive format.
 */
public final
class ArArchiveFormat implements ArchiveFormat {

    private static final FileNameUtil
    FILE_NAME_UTIL = new FileNameUtil(MapUtil.<String, String>map(".a", "", ".ar", ""), ".a");

    private ArArchiveFormat() {}

    /** Required by {@link ArchiveFormatFactory}. */
    public static ArchiveFormat
    get() { return ArArchiveFormat.INSTANCE; }
    private static final ArchiveFormat INSTANCE = new ArArchiveFormat();

    @Override public String
    getName() { return ArchiveStreamFactory.AR; }

    @Override public boolean
    isArchiveFileName(String fileName) { return ArArchiveFormat.FILE_NAME_UTIL.isCompressedFilename(fileName); }

    @Override public String
    getArchiveFileName(String fileName) { return ArArchiveFormat.FILE_NAME_UTIL.getCompressedFilename(fileName); }

    @Override public ArchiveInputStream
    archiveInputStream(InputStream is) {

        final ArArchiveInputStream aais = new ArArchiveInputStream(is);

        return new ArchiveInputStream() {

            @Override public ArchiveEntry
            getNextEntry() throws IOException {
                try {
                    return aais.getNextEntry();
                } catch (NumberFormatException nfe) {

                    // A corrupt AR entry may cause this RTE:
                    if (
                        nfe
                        .getStackTrace()[2]
                        .getClassName()
                        .equals("org.apache.commons.compress.archivers.ar.ArArchiveInputStream")
                    ) throw ExceptionUtil.wrap("Corrupt AR entry", nfe, IOException.class);

                    throw nfe;
                }
            }


            @Override public void
            close() throws IOException { aais.close(); }

            @Override @NotNullByDefault(false) public int
            read(byte[] b, final int off, final int len) throws IOException { return aais.read(b, off, len); }
        };
    }

    @Override public ArchiveInputStream
    open(File archiveFile)
    throws IOException { return this.archiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile))); }

    @Override public ArchiveOutputStream
    archiveOutputStream(OutputStream os) { return new ArArchiveOutputStream(os); }

    @Override public ArchiveOutputStream
    create(File archiveFile)
    throws IOException { return new ArArchiveOutputStream(new FileOutputStream(archiveFile)); }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final String                                                           name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof ArArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Copy the contents to a temporary storage in order to count the bytes.

        InputOutputStreams ios = PipeUtil.asInputOutputStreams(PipeFactory.elasticPipe());
        try {
            long count = OutputStreams.writeAndCount(writeContents, ios.getOutputStream());
            ios.getOutputStream().close();

            archiveOutputStream.putArchiveEntry(new ArArchiveEntry(name, count));

            IoUtil.copy(ios.getInputStream(), archiveOutputStream);
            ios.getInputStream().close();

            archiveOutputStream.closeArchiveEntry();
        } finally {
            try { ios.getOutputStream().close(); } catch (Exception e) {}
            try { ios.getInputStream().close(); } catch (Exception e) {}
        }
    }

    @Override public void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) {
        if (!(archiveOutputStream instanceof ArArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        throw new UnsupportedOperationException("The AR archive format does not support directory entries");
    }

    @Override public void
    writeEntry(
        final ArchiveOutputStream                                              archiveOutputStream,
        final ArchiveEntry                                                     archiveEntry,
        @Nullable final String                                                 name,
        final ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException {
        if (!(archiveOutputStream instanceof ArArchiveOutputStream)) {
            throw new IllegalArgumentException(archiveOutputStream.getClass().getName());
        }

        // Template for user id, group id and modification date.
        final ArArchiveEntry aae = (
            archiveEntry instanceof ArArchiveEntry
            ? (ArArchiveEntry) archiveEntry
            : new ArArchiveEntry("", 0)
        );

        if (archiveEntry.isDirectory()) return;

        // Copy the contents to a temporary storage in order to count the bytes.
        InputOutputStreams ios = PipeUtil.asInputOutputStreams(PipeFactory.elasticPipe());
        try {
            long count = OutputStreams.writeAndCount(writeContents, ios.getOutputStream());
            ios.getOutputStream().close();

            ArArchiveEntry naae = new ArArchiveEntry(
                name != null ? name : archiveEntry.getName(),
                count,
                aae.getUserId(),
                aae.getGroupId(),
                aae.getMode(),
                archiveEntry.getLastModifiedDate().getTime()
            );
            archiveOutputStream.putArchiveEntry(naae);

            IoUtil.copy(ios.getInputStream(), archiveOutputStream);
            ios.getInputStream().close();

            archiveOutputStream.closeArchiveEntry();
        } finally {
            try { ios.getOutputStream().close(); } catch (Exception e) {}
            try { ios.getInputStream().close();  } catch (Exception e) {}
        }
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) { return ArArchiveInputStream.matches(signature, signatureLength); }

    @Override public String
    toString() { return this.getName(); }
}
