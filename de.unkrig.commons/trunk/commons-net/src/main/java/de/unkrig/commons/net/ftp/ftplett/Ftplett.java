
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.net.ftp.ftplett;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Defines the <i>semantics</i> of the FTP protocol, in analogy with the {@link
 * de.unkrig.commons.net.http.servlett.Servlett}.
 */
public
interface Ftplett {

    /**
     * @param directoryName {@code null} means change to some 'root' or 'home' directory
     * @return              Whether the working directory could be changed to the given directory.
     */
    boolean
    changeWorkingDirectory(@Nullable String directoryName) throws IOException;

    /**
     * @return The current working directory
     */
    String
    getWorkingDirectory();

    /**
     * @return An {@link InputStream} producing the contents of the resource, or {@code null} iff the resource cannot
     *         be accessed.
     */
    @Nullable InputStream
    retrieve(String resourceName) throws IOException;

    /**
     * @return An {@link OutputStream} to which the contents can be written, or {@code null} iff the resource cannot
     *         be accessed.
     */
    @Nullable OutputStream
    store(String resourceName) throws IOException;

    /**
     * @param name         The name of the directory or file to list, or {@code null} to list the current working
     *                     directory
     * @param lineConsumer Consumes the listing
     */
    boolean
    list(@Nullable String name, ConsumerWhichThrows<String, IOException> lineConsumer) throws IOException;

    /**
     * @param name         The name of the directory or file to list, or {@code null} to list the current working
     *                     directory
     * @param lineConsumer Consumes the listing
     */
    boolean
    nameList(@Nullable String name, ConsumerWhichThrows<String, IOException> lineConsumer) throws IOException;

    /**
     * @return Whether the resource was successfully deleted
     */
    boolean
    delete(String resourceName) throws IOException;

    /**
     * @return Whether the resource was successfully renamed
     */
    boolean
    rename(String from, String to) throws IOException;

    /**
     * @return {@code null} iff the modification time cannot be determined
     */
    @Nullable Date
    getModificationTime(String resourceName) throws IOException;
}
