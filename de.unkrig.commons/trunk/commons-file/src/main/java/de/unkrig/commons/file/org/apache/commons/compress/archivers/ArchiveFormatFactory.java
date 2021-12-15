
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.compress.utils.IOUtils;

import de.unkrig.commons.io.MarkableFileInputStream;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Manages {@link ArchiveFormat}s.
 * <p>
 *   Additional archive formats can be 'plugged in' at runtime by putting a resource named {@link #FORMATS_RESOURCE} on
 *   the classpath.
 * </p>
 */
public abstract
class ArchiveFormatFactory {

    /**
     * All resources on the classpath with this name are read; each line contains the name of a class which must
     * declare a method "{@code public static ArchiveFormat get()}" which returns an {@link ArchiveFormat} object
     * describing the archive format.
     * <p>
     *   Blank lines and line starting with "#" are ignored.
     * </p>
     */
    private static final String
    FORMATS_RESOURCE = "de/unkrig/commons/file/org/apache/commons/compress/archivers/formats";

    /**
     * See {@link URL#URL(String, String, int, String)}
     */
    private static final String
    SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";

    private static final Map<String, ArchiveFormat> ALL_ARCHIVE_FORMATS;
    static {

        Map<String, ArchiveFormat> m = new HashMap<String, ArchiveFormat>();
        try {
            ClassLoader cl = ArchiveFormatFactory.class.getClassLoader();

            Enumeration<URL> en = cl.getResources(ArchiveFormatFactory.FORMATS_RESOURCE);
            if (!en.hasMoreElements()) throw new FileNotFoundException(ArchiveFormatFactory.FORMATS_RESOURCE);

            List<URL> scannedResources = new ArrayList<URL>();
            while (en.hasMoreElements()) {
                URL url = en.nextElement();

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(url.openConnection().getInputStream(), Charset.forName("ISO8859-1"))
                );
                try {

                    for (String line = br.readLine(); line != null; line = br.readLine()) {

                        line = line.trim();
                        if (line.length() == 0 || line.charAt(0) == '#') continue;

                        Class<?> archiveFormatClass = cl.loadClass(line);

                        ArchiveFormat af = (ArchiveFormat) archiveFormatClass.getMethod("get").invoke(null);

                        // Silently override formats implemented in de.unkrig.commons.file.
                        ArchiveFormat prev = m.get(af.getName());
                        if (prev != null) {
                            if (prev == af) {
                                ;
                            } else
                            if (prev.getClass().getName().startsWith("de.unkrig.commons.file.org.apache.commons.compress.archivers.")) {
                                ;
                            } else
                            if (af.getClass().getName().startsWith("de.unkrig.commons.file.org.apache.commons.compress.archivers.")) {
                                continue;
                            } else
                            {
                                throw new ExceptionInInitializerError(
                                    "Scanning \"" + url + "\": Duplicate archive format name '"
                                    + af.getName()
                                    + "'. Previously scanned locations: "
                                    + scannedResources
                                    + ". Archive formats scanned so far: "
                                    + m.keySet()
                                );
                            }
                        }

                        m.put(af.getName(), af);
                    }
                    br.close();
                } finally {
                    try { br.close(); } catch (Exception e) {}
                    scannedResources.add(url);
                }
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        ALL_ARCHIVE_FORMATS = Collections.unmodifiableMap(m);
    }

    /**
     * Register this package for {@link java.net.URLStreamHandler}s for the supported archive formats.
     *
     * @see URL#URL(String, String, int, String)
     */
    static {

        String packagE = ArchiveFormatFactory.class.getPackage().getName();

        String phps = System.getProperty(ArchiveFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS);
        if (phps == null) {
            System.setProperty(ArchiveFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS, packagE);
        } else {
            if (!Arrays.asList(phps.split("\\|")).contains(packagE)) {
                System.setProperty(ArchiveFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS, phps + '|' + packagE);
            }
        }
    }

    private ArchiveFormatFactory() {}

    /** @return A collection of all registered archive formats */
    public static Collection<ArchiveFormat>
    allFormats() { return ArchiveFormatFactory.ALL_ARCHIVE_FORMATS.values(); }

    /**
     * @return The {@link ArchiveFormat} for the given file name, or {@code null} iff an archive format cannot
     *         be deduced from the <var>fileName</var>
     * @see    FileNameUtil#isCompressedFilename(String)
     */
    @Nullable public static ArchiveFormat
    forFileName(String fileName) {
        for (ArchiveFormat af : ArchiveFormatFactory.ALL_ARCHIVE_FORMATS.values()) {
            if (af.isArchiveFileName(fileName)) return af;
        }
        return null;
    }

    /**
     * @return The {@link ArchiveFormat} for the given <var>archiveFormatName</var>
     * @throws ArchiveException The <var>archiveFormatName</var> is unknown
     */
    public static ArchiveFormat
    forFormatName(String archiveFormatName) throws ArchiveException {
        ArchiveFormat result = ArchiveFormatFactory.ALL_ARCHIVE_FORMATS.get(archiveFormatName);
        if (result == null) throw new ArchiveException(archiveFormatName);

        return result;
    }

    /**
     * Reads the first few bytes from the given input stream and determines the archive format.
     *
     * @return One of {@link #allFormats()}, or {@code null} iff the stream contents has none of the known
     *         archive formats
     */
    @Nullable public static ArchiveFormat
    forContents(InputStream is) throws IOException {

        final byte[] signature = new byte[512];
        is.mark(signature.length);
        int signatureLength = IOUtils.readFully(is, signature);
        is.reset();

        for (ArchiveFormat af : ArchiveFormatFactory.ALL_ARCHIVE_FORMATS.values()) {
            if (af.matches(signature, signatureLength)) return af;
        }

        return null;
    }

    /**
     * Reads the first few bytes from the given file and determines the archive format.
     *
     * @return One of {@link #allFormats()}, or {@code null} iff the file contents has none of the known
     *         archive formats
     */
    @Nullable public static ArchiveFormat
    forContents(File file) throws IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            ArchiveFormat result = ArchiveFormatFactory.forContents(is);
            is.close();
            return result;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    /**
     * @return     The {@link ArchiveFormat} corresponding with the <var>archiveOutputStream</var>
     * @deprecated Should be replaced by a new method {@code ArchiveOutputStream.getFormat()}
     */
    @Deprecated public static ArchiveFormat
    forArchiveOutputStream(ArchiveOutputStream archiveOutputStream) {
        for (ArchiveFormat af : ArchiveFormatFactory.ALL_ARCHIVE_FORMATS.values()) {
            try {
                if (
                    af.archiveOutputStream(OutputStreams.DISCARD).getClass()
                    == archiveOutputStream.getClass()
                ) return af;
            } catch (StreamingNotSupportedException e) {
                ;
            } catch (ArchiveException e) {
                ;
            }
        }
        throw new AssertionError(archiveOutputStream.getClass());
    }

    /**
     * @return     The CRC32 (0...0xffffffffL) of the contents of the given archive entry, or -1L if unknown
     * @deprecated Should be replaced by a new method {@code ArchiveEntry.getCrc32()}
     */
    @Deprecated public static long
    getEntryCrc32(ArchiveEntry archiveEntry) {

        if (archiveEntry instanceof ZipArchiveEntry) return ((ZipArchiveEntry) archiveEntry).getCrc();

        if (archiveEntry instanceof SevenZArchiveEntry) {
            SevenZArchiveEntry szae = (SevenZArchiveEntry) archiveEntry;
            return szae.getHasCrc() ? 0xffffffffL & szae.getCrcValue() : -1;
        }

        return -1;
    }

    /**
     * Determines the archive format from the first few bytes of the <var>inputStream</var> and then wraps it in the
     * appropriate {@link ArchiveInputStream}.
     */
    public static ArchiveInputStream
    archiveInputStream(InputStream inputStream) throws IOException, ArchiveException {

        ArchiveFormat af = ArchiveFormatFactory.forContents(inputStream);
        if (af == null) {
            throw new ArchiveException("Cannot determine archive format from stream contents");
        }

        return af.archiveInputStream(inputStream);
    }

    /**
     * Determines the archive format from the first few bytes of the <var>archiveFile</var> and then returns a
     * {@link ArchiveInputStream} reading from the <var>archiveFile</var>.
     * <p>
     *   Is typically faster than {@code
     *   ArchiveFormatFactory.forContents(archiveFile).archiveInputStream(archiveFile)}.
     * </p>
     */
    public static ArchiveInputStream
    open(File archiveFile) throws IOException, ArchiveException {

        MarkableFileInputStream is = new MarkableFileInputStream(archiveFile);

        try {
            ArchiveFormat af = ArchiveFormatFactory.forContents(is);
            if (af == null) {
                try { is.close(); } catch (Exception e) {}
                throw new ArchiveException(
                    "Cannot determine archive format from the contents of '"
                    + archiveFile
                    + "'"
                );
            }

            return af.archiveInputStream(is);
        } catch (IOException ioe) {
            try { is.close(); } catch (Exception e) {}
            throw ioe;
        } catch (RuntimeException re) {
            try { is.close(); } catch (Exception e) {}
            throw re;
        }
    }

    /**
     * Normalizes an entry name produced by {@link ArchiveEntry#getName()} such that
     * <ul>
     *   <li>The names of two entries in two archives are typically regarded as equal by users
     *   <li>The name never ands with a slash ('/')
     * </ul>
     * This is necessary because e.g. for the ZIP-derived archive formats the names of DIRECTORY entries always have
     * a trailing slash, which is not the case for other archive formats.
     */
    public static String
    normalizeEntryName(String name) {

        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);

        return name;
    }
}
