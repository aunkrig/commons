
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

package de.unkrig.commons.file.org.apache.commons.compress.compressors.xz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;

import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;

/** Representation of the 'xz' compression format. */
public final
class XZCompressionFormat implements CompressionFormat { // SUPPRESS CHECKSTYLE AbbreviationAsWord

    private XZCompressionFormat() {}

    /** Required by {@link CompressionFormatFactory}. */
    public static CompressionFormat
    get() { return XZCompressionFormat.INSTANCE; }
    private static final CompressionFormat INSTANCE = new XZCompressionFormat();

    @Override public String
    getName() { return CompressorStreamFactory.XZ; }

    @Override public boolean
    isCompressedFileName(String fileName) { return XZUtils.isCompressedFilename(fileName); }

    @Override public String
    getCompressedFileName(String fileName) { return XZUtils.getCompressedFilename(fileName); }

    @Override public String
    getUncompressedFileName(String fileName) { return XZUtils.getUncompressedFilename(fileName); }

    @Override public CompressorInputStream
    compressorInputStream(InputStream is) throws IOException { return new XZCompressorInputStream(is); }

    @Override public CompressorInputStream
    open(File compressedFile) throws IOException {
        InputStream is = new FileInputStream(compressedFile);
        try {
            return new XZCompressorInputStream(is);
        } catch (IOException ioe) {
            try { is.close(); } catch (Exception e) {}
            throw ioe;
        }
    }

    @Override public CompressorOutputStream
    compressorOutputStream(OutputStream os) throws IOException { return new XZCompressorOutputStream(os); }

    @Override public CompressorOutputStream
    create(File compressedFile) throws IOException {
        OutputStream os = new FileOutputStream(compressedFile);
        try {
            return new XZCompressorOutputStream(os);
        } catch (IOException ioe) {
            try { os.close(); } catch (Exception e) {}
            throw ioe;
        }
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) {
        return XZCompressorInputStream.matches(signature, signatureLength);
    }

    @Override public String
    toString() { return this.getName(); }
}
