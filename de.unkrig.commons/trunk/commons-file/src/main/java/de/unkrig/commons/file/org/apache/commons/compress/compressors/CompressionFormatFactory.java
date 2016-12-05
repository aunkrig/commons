
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

package de.unkrig.commons.file.org.apache.commons.compress.compressors;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.io.MarkableFileInputStream;
import de.unkrig.commons.io.OutputStreams;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Manages {@link CompressionFormat}s.
 * <p>
 *   Additional compression formats can be 'plugged in' at runtime by putting a resource named {@link
 *   #FORMATS_RESOURCE} on the classpath.
 * </p>
 */
public abstract
class CompressionFormatFactory {

    /**
     * All resources on the classpath with this name are read; each line contains the name of a class which must
     * declare a method "{@code public static CompressionFormat get()}" which returns a {@link CompressionFormat}
     * object describing the compression format.
     * <p>
     *   Blank lines and line starting with "#" are ignored.
     * </p>
     */
    private static final String
    FORMATS_RESOURCE = "de/unkrig/commons/file/org/apache/commons/compress/compressors/formats";

    /**
     * See {@link URL#URL(String, String, int, String)}
     */
    private static final String
    SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";

    private static final Map<String, CompressionFormat> ALL_COMPRESSION_FORMATS;
    static {

        Map<String, CompressionFormat> m = new HashMap<String, CompressionFormat>();
        try {
            ClassLoader cl = CompressionFormatFactory.class.getClassLoader();

            Enumeration<URL> en = cl.getResources(CompressionFormatFactory.FORMATS_RESOURCE);
            if (!en.hasMoreElements()) throw new FileNotFoundException(CompressionFormatFactory.FORMATS_RESOURCE);

            while (en.hasMoreElements()) {
                URL url = en.nextElement();

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(url.openConnection().getInputStream(), Charset.forName("ISO8859-1"))
                );
                try {

                    for (String line = br.readLine(); line != null; line = br.readLine()) {

                        line = line.trim();
                        if (line.length() == 0 || line.charAt(0) == '#') continue;

                        Class<?> compressionFormatClass = cl.loadClass(line);

                        CompressionFormat cf = (CompressionFormat) compressionFormatClass.getMethod("get").invoke(null);
                        if (m.put(cf.getName(), cf) != null) {
                            throw new ExceptionInInitializerError(
                                "Duplicate compression format name '"
                                + cf.getName()
                                + "'"
                            );
                        }
                    }
                    br.close();
                } finally {
                    try { br.close(); } catch (Exception e) {}
                }
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        ALL_COMPRESSION_FORMATS = Collections.unmodifiableMap(m);
    }

    /**
     * Register this package for {@link java.net.URLStreamHandler}s for the supported compressor formats.
     *
     * @see URL#URL(String, String, int, String)
     */
    static {

        String packagE = ArchiveFormatFactory.class.getPackage().getName();

        String phps = System.getProperty(CompressionFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS);
        if (phps == null) {
            System.setProperty(CompressionFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS, packagE);
        } else {
            if (!Arrays.asList(phps.split("\\|")).contains(packagE)) {
                System.setProperty(
                    CompressionFormatFactory.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS,
                    phps + '|' + packagE
                );
            }
        }
    }

    private CompressionFormatFactory() {}

    /** @return A collection of all registered compression formats */
    public static Collection<CompressionFormat>
    allFormats() { return CompressionFormatFactory.ALL_COMPRESSION_FORMATS.values(); }

    /**
     * @return The {@link CompressionFormat} for the given file name, or {@code null} iff a compression format cannot
     *         be deduced from the {@code fileName}
     * @see    FileNameUtil#isCompressedFilename(String)
     */
    @Nullable public static CompressionFormat
    forFileName(String fileName) {
        for (CompressionFormat cf : CompressionFormatFactory.ALL_COMPRESSION_FORMATS.values()) {
            if (cf.isCompressedFileName(fileName)) return cf;
        }
        return null;
    }

    /**
     * @return The {@link CompressionFormat} for the given {@code compressionFormatName}
     * @throws CompressorException The {@code compressionFormatName} is unknown
     */
    public static CompressionFormat
    forFormatName(String compressionFormatName) throws CompressorException {
        CompressionFormat result = CompressionFormatFactory.ALL_COMPRESSION_FORMATS.get(compressionFormatName);
        if (result == null) throw new CompressorException(compressionFormatName);

        return result;
    }

    /**
     * Reads the first few bytes from the given input stream and determines the compression format.
     *
     * @return One of {@link #allFormats()}, or {@code null} iff the stream contents has none of the known
     *         compression formats
     */
    @Nullable public static CompressionFormat
    forContents(InputStream is) throws IOException {

        final byte[] signature = new byte[512];
        is.mark(signature.length);
        int signatureLength = IOUtils.readFully(is, signature);
        is.reset();

        for (CompressionFormat af : CompressionFormatFactory.ALL_COMPRESSION_FORMATS.values()) {
            if (af.matches(signature, signatureLength)) return af;
        }

        return null;
    }

    /**
     * Reads the first few bytes from the given file and determines the compression format.
     *
     * @return One of {@link #allFormats()}, or {@code null} iff the file contents has none of the known
     *         compression formats
     */
    @Nullable public static CompressionFormat
    forContents(File file) throws IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            CompressionFormat result = CompressionFormatFactory.forContents(is);
            is.close();
            return result;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    /**
     * @return     The compression format produced by the {@code compressorOutputStream}
     * @deprecated Should be replaced by a new method {@code CompressorOutputStream.getFormat()}
     */
    @Deprecated public static CompressionFormat
    forCompressorOutputStream(CompressorOutputStream compressorOutputStream) {
        for (CompressionFormat cf : CompressionFormatFactory.ALL_COMPRESSION_FORMATS.values()) {
            try {
                if (
                    cf.compressorOutputStream(OutputStreams.DISCARD).getClass()
                    == compressorOutputStream.getClass()
                ) return cf;
            } catch (CompressorException ce) {
                ;
            } catch (IOException ioe) {
                AssertionError ae = new AssertionError(cf.getClass());
                ae.initCause(ioe);
                throw ae; // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
        }
        throw new AssertionError(compressorOutputStream.getClass());
    }

    /**
     * @return     The size of the uncompressed contents of the given compressor input stream, or -1L if unknown
     * @deprecated Should be replaced by a new method {@code CompressorInputStream.getUncompressedSize()}
     */
    @Deprecated public static long
    getUncompressedSize(CompressorInputStream compressorInputStream) {

        if (compressorInputStream instanceof SnappyCompressorInputStream) {
            return ((SnappyCompressorInputStream) compressorInputStream).getSize();
        }

        return -1;
    }

    /**
     * Determines the compression format from the first few bytes of the {@code inputStream} and then wraps it in the
     * appropriate {@link CompressorInputStream}.
     */
    public static CompressorInputStream
    compressorInputStream(InputStream inputStream) throws IOException, CompressorException {

        CompressionFormat cf = CompressionFormatFactory.forContents(inputStream);
        if (cf == null) {
            throw new CompressorException("Cannot determine compression format from stream contents");
        }

        return cf.compressorInputStream(inputStream);
    }

    /**
     * Determines the compression format from the first few bytes of the {@code compressedFile} and then returns a
     * {@link CompressorInputStream} reading from the {@code compressedFile}.
     * <p>
     *   Is typically faster than {@code
     *   CompressionFormatFactory.forContents(compressedFile).compressorInputStream(compressedFile)}.
     * </p>
     */
    public static CompressorInputStream
    open(File compressedFile) throws IOException, CompressorException {

        MarkableFileInputStream is = new MarkableFileInputStream(compressedFile);

        try {
            CompressionFormat cf = CompressionFormatFactory.forContents(is);
            if (cf == null) {
                try { is.close(); } catch (Exception e) {}
                throw new CompressorException(
                    "Cannot determine compression format from the contents of '"
                    + compressedFile
                    + "'"
                );
            }

            return cf.compressorInputStream(is);
        } catch (IOException ioe) {
            try { is.close(); } catch (Exception e) {}
            throw ioe;
        } catch (RuntimeException re) {
            try { is.close(); } catch (Exception e) {}
            throw re;
        }
    }
}
