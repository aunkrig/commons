
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

package de.unkrig.commons.file.org.apache.commons.compress.archivers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Representation of one archive format, e.g. ZIP or TAR. Archive formats are managed by {@link ArchiveFormatFactory}.
 */
public
interface ArchiveFormat {

    /** @return A short, familiar text which describes this archive format, e.g. '7z', 'zip', 'tar' */
    String
    getName();

    /** @return Whether the given <var>fileName</var> is typical for this {@link ArchiveFormat} */
    boolean
    isArchiveFileName(String fileName);

    /**
     * Maps the given <var>fileName</var> to the name that a corresponding archive file would have. This is typically
     * achieved by appending a suffix, like '.7z', '.zip', '.tar'.
     */
    String
    getArchiveFileName(String fileName);

    /**
     * @return                                An {@link ArchiveInputStream} for this format which reads from the given
     *                                        input stream
     * @throws StreamingNotSupportedException This archive format does not support streaming
     * @throws ArchiveException               The contents is invalid for this archive format
     */
    ArchiveInputStream
    archiveInputStream(InputStream is) throws StreamingNotSupportedException, ArchiveException;

    /**
     * Opens an existing archive file for reading.
     *
     * @return An {@link ArchiveInputStream} for this format which reads from the given <var>archiveFile</var>
     */
    ArchiveInputStream
    open(File archiveFile) throws IOException, ArchiveException;

    /**
     * @return                                An {@link ArchiveOutputStream} for this format which writes to the given
     *                                        output stream
     * @throws StreamingNotSupportedException This archive format does not support streaming
     * @throws ArchiveException               Creation of archives in in this format is not supported
     */
    ArchiveOutputStream
    archiveOutputStream(OutputStream os) throws StreamingNotSupportedException, ArchiveException;

    /**
     * Creates a new archive file.
     *
     * @return                  An {@link ArchiveOutputStream} for this format which writes to the given {@code
     *                          archiveFile}
     * @throws ArchiveException Creation of archives in this format is not supported
     */
    ArchiveOutputStream
    create(File archiveFile) throws IOException, ArchiveException;

    /**
     * Shorthand for {@link #writeEntry(ArchiveOutputStream, String, Date, ConsumerWhichThrows)
     * writeEntry(archiveOutputStream, name, null, writeContents)}.
     */
    void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        String                                                           name,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException;

    /**
     * Appends a 'normal' entry (as opposed to a 'directory entry') with the given contents to the given {@code
     * archiveOutputStream}. The archive entry is filled with "standard values", except for the entry <var>name</var>
     * and <var>lastModifiedDate</var>.
     * <p>
     *   <var>writeContents</var> is called exactly once unless the <var>name</var> designates a directory entry.
     * </p>
     *
     * @param archiveOutputStream       <i>Must</i> match this {@link ArchiveFormat}
     * @param name                      The name for the entry; may be slightly changed (in particulary wrt/ leading
     *                                  and trailing slashes) before the entry is created
     * @param lastModifiedDate          If {@code null}, then either "no date" or a default value is stored in the entry
     * @param writeContents             Writes the entry's contents to the 'subject' output stream
     * @throws IllegalArgumentException The type of the <var>archiveOutputStream</var> does not match this {@link
     *                                  ArchiveFormat}
     * @see                             #writeDirectoryEntry(ArchiveOutputStream, String)
     */
    void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        String                                                           name,
        @Nullable Date                                                   lastModifiedDate,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException;

    /**
     * Appends a 'directory entry', i.e. an entry without contents, to the given <var>archiveOutputStream</var>. The
     * archive entry is filled with "standard values", except for the entry <var>name</var>.
     *
     * @param archiveOutputStream            <i>Must</i> match this {@link ArchiveFormat}
     * @param name                           The name for the entry; may be slightly changed (in particulary wrt/
     *                                       leading and trailing slashes) before the entry is created
     * @throws IllegalArgumentException      The type of the <var>archiveOutputStream</var> does not match this {@link
     *                                       ArchiveFormat}
     * @throws UnsupportedOperationException This archive format does not support 'directory entries'
     */
    void
    writeDirectoryEntry(ArchiveOutputStream archiveOutputStream, String name) throws IOException;

    /**
     * Appends the given <var>archiveEntry</var> with the given contents to the given <var>archiveOutputStream</var>.
     * If <var>name</var> is not {@code null}, then it overrides the name in the archive entry.
     * <p>
     *   If the type of the <var>archiveEntry</var> does not match this {@link ArchiveFormat}, then it is automatically
     *   converted to the correct type, preserving as much information as possible ('re-archiving').
     * </p>
     * <p>
     *   <var>writeContents</var> is called exactly once unless the <var>archiveEntry</var> is a directory entry.
     * </p>
     *
     * @param archiveOutputStream       <i>Must</i> match this {@link ArchiveFormat}
     * @param archiveEntry              May or may not match this {@link ArchiveFormat} (see above)
     * @param name                      Overrides the name in the <var>archiveEntry</var>, or {@code null}
     * @param writeContents             Writes the entry's contents to the 'subject' output stream
     * @throws IllegalArgumentException The type of the <var>archiveOutputStream</var> does not match this {@link
     *                                  ArchiveFormat}
     */
    void
    writeEntry(
        ArchiveOutputStream                                              archiveOutputStream,
        ArchiveEntry                                                     archiveEntry,
        @Nullable String                                                 name,
        ConsumerWhichThrows<? super OutputStream, ? extends IOException> writeContents
    ) throws IOException;

    /** @return Whether the first few bytes of archive data match what is expected for this format */
    boolean
    matches(byte[] signature, int signatureLength);

    /**
     * Some archive formats provide a per-entry "compression method" - these would return a non-{@code null} value.
     *
     * @throws ClassCastException The <var>ae</var>'s is not suitable for this archive format
     * @since 1.2.16
     */
    @Nullable String
    getCompressionMethod(ArchiveEntry ae);
}
