
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

package de.unkrig.commons.file.org.apache.commons.compress.compressors.bzip2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;

import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;

/** Representation of the 'bzip2' compression format. */
public final
class BZip2CompressionFormat implements CompressionFormat {

    private BZip2CompressionFormat() {}

    /** Required by {@link CompressionFormatFactory}. */
    public static CompressionFormat
    get() { return BZip2CompressionFormat.INSTANCE; }
    private static final CompressionFormat INSTANCE = new BZip2CompressionFormat();

    @Override public String
    getName() { return CompressorStreamFactory.BZIP2; }

    @Override public boolean
    isCompressedFileName(String fileName) { return BZip2Utils.isCompressedFilename(fileName); }

    @Override public String
    getCompressedFileName(String fileName) { return BZip2Utils.getCompressedFilename(fileName); }

    @Override public String
    getUncompressedFileName(String fileName) { return BZip2Utils.getUncompressedFilename(fileName); }

    @Override public CompressorInputStream
    compressorInputStream(InputStream is) throws IOException { return new BZip2CompressorInputStream(is); }

    @Override public CompressorInputStream
    open(File compressedFile) throws IOException {
        InputStream is = new FileInputStream(compressedFile);
        try {
            return new BZip2CompressorInputStream(is);
        } catch (IOException ioe) {
            try { is.close(); } catch (Exception e) {}
            throw ioe;
        }
    }

    @Override public CompressorOutputStream
    compressorOutputStream(OutputStream os) throws IOException { return new BZip2CompressorOutputStream(os); }

    @Override public CompressorOutputStream
    create(File compressedFile) throws IOException {
        OutputStream os = new FileOutputStream(compressedFile);
        try {
            return new BZip2CompressorOutputStream(os);
        } catch (IOException ioe) {
            try { os.close(); } catch (Exception e) {}
            throw ioe;
        }
    }

    @Override public boolean
    matches(byte[] signature, int signatureLength) {
        return BZip2CompressorInputStream.matches(signature, signatureLength);
    }

    @Override public String
    toString() { return this.getName(); }
}
