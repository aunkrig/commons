
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.file.org.apache.commons.compress.compressors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * Representation of one compression format, e.g. GZ or Z. Compression formats are managed by {@link
 * CompressionFormatFactory}.
 */
public
interface CompressionFormat {

    /** @return A short, familiar text which describes this compression format, e.g. 'bzip2', 'gz', 'z' */
    String
    getName();

    /** @return Whether the given {@code fileName} is typical for this {@link CompressionFormat} */
    boolean
    isCompressedFileName(String fileName);

    /**
     * Maps the given {@code fileName} to the name that a corresponding compressed file would have. This is typically
     * achieved by appending a suffix, like '.bzip2', '.gz', '.Z'.
     */
    String
    getCompressedFileName(String fileName);

    /**
     * Maps the given {@code fileName} to the name that a corresponding uncompressed file would have. This is typically
     * achieved by removing a suffix, like '.bzip2', '.gz', '.Z', or by changing the suffix, e.g. from '.tgz' to '.tar'.
     */
    String
    getUncompressedFileName(String fileName);

    /**
     * @return A {@link CompressorInputStream} for this format which reads from the given input stream
     */
    CompressorInputStream
    compressorInputStream(InputStream is) throws IOException;

    /**
     * Opens an existing compressed file for reading.
     *
     * @return An {@link CompressorInputStream} for this format which reads from the given {@code compressedFile}
     */
    CompressorInputStream
    open(File compressedFile) throws IOException;

    /**
     * @return                     An {@link CompressorOutputStream} for this format which writes to the given output
     *                             stream
     * @throws CompressorException Creation of compressed contents in in this format is not supported
     */
    CompressorOutputStream
    compressorOutputStream(OutputStream os) throws IOException, CompressorException;

    /**
     * Creates a new compressed file.
     *
     * @return                     An {@link CompressorOutputStream} for this format which writes to the given {@code
     *                             compressedFile}
     * @throws CompressorException Creation of compressed files in in this format is not supported
     */
    CompressorOutputStream
    create(File compressedFile) throws IOException, CompressorException;

    /** @return Whether the first few bytes of compressed data match what is expected for this format */
    boolean
    matches(byte[] signature, int signatureLength);
}
