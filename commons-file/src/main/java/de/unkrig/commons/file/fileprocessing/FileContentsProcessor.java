
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

package de.unkrig.commons.file.fileprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.unkrig.commons.file.contentsprocessing.ContentsProcessor;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Processes a file by opening the file, feeding its contents to the {@link ContentsProcessor}, and eventually closing
 * the file.
 *
 * @param <T> The type that {@link #process(String, File)} returns; use "{@code Void}" if not needed
 */
public
class FileContentsProcessor<T> implements FileProcessor<T> {

    /**
     * The {@link ContentsProcessor} that is used to process a file's contents when {@link #process(String, File)} is
     * invoked.
     */
    protected final ContentsProcessor<T> contentsProcessor;

    public
    FileContentsProcessor(ContentsProcessor<T> contentsProcessor) {
        this.contentsProcessor = contentsProcessor;
    }

    /**
     * Opens the {@code file}, passes the input stream to {@link ContentsProcessor#process(String, InputStream, long,
     * long, ProducerWhichThrows)}, then closes the file.
     * <p>
     *   Subclasses may override this behavior, e.g. by recursing into directories or archives.
     * </p>
     *
     * @throws IOException Message should include the file name
     */
    @Override @Nullable public T
    process(String path, final File file) throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.contentsProcessor.process(
                file.getPath(),                                       // path
                is,                                                   // inputStream
                file.length(),                                        // size
                -1L,                                                  // crc32
                new ProducerWhichThrows<InputStream, IOException>() { // opener

                    @Override @Nullable public InputStream
                    produce() throws IOException { return new FileInputStream(file); }
                }
            );
            is.close();
            return null;
        } catch (IOException ioe) {
            throw ExceptionUtil.wrap(file.getPath(), ioe);
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap(file.getPath(), re);
        } finally {
            try { is.close(); } catch (IOException ioe) {}
        }
    }
}
