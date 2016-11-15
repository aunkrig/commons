
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

package de.unkrig.commons.file.filetransformation;

import java.io.File;
import java.io.IOException;

/**
 * Transforms the members of a directory.
 *
 * @see DirectoryTransformer
 * @see FileTransformations#directoryTreeTransformer(java.util.Comparator, de.unkrig.commons.lang.protocol.Predicate,
 *      de.unkrig.commons.text.pattern.Glob,
 *      de.unkrig.commons.file.filetransformation.FileTransformations.DirectoryCombiner, FileTransformer, boolean,
 *      boolean, de.unkrig.commons.file.ExceptionHandler)
 */
public
interface DirectoryMembersTransformer {

    /**
     * This method is invoked for each member of the directory.
     *
     * @param directoryMemberPath The path designating this member
     */
    void
    visitMember(String directoryMemberPath, File directoryMemberIn, File directoryMemberOut, FileTransformer.Mode mode)
    throws IOException;

    /**
     * This method is invoked after the last member of the directory.
     *
     * @param directoryPath The path designating this directory
     */
    void
    visitEnd(String directoryPath, File directoryIn, File directoryOut, FileTransformer.Mode mode) throws IOException;
}
