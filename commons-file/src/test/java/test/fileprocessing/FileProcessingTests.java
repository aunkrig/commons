
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

package test.fileprocessing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessor;
import de.unkrig.commons.file.fileprocessing.FileProcessings;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Glob;
import de.unkrig.commons.text.pattern.Pattern2;
import de.unkrig.commons.util.concurrent.ConcurrentUtil;
import de.unkrig.commons.util.concurrent.SquadExecutor;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@code de.unkrig.commons.file.fileprocessing' package.
 */
public
class FileProcessingTests {

    private static final File TEST_FILES = new File("./.test_files");

    @Before public void
    setUp() throws Exception {
        if (FileProcessingTests.TEST_FILES.exists()) FileUtil.deleteRecursively(FileProcessingTests.TEST_FILES);
        new Files(new Object[] {
            "dir1", new Object[] {
                "file1", "line1\nline2\nline3\n",
                "file2.gz", "line1\nline2\nline3\n",
                "dir2", new Object[] {
                    "file2", "line1\nline2\nline3\n",
                    "file.zip", new Object[] {
                        "dir1/dir2/file1", "line1\nline2\nline3\n",
                        "dir3/dir4/file2", "line1\nline2\nline3\n",
                    },
                    "file3.tgz", new Object[] {
                        "dir3/dir4/file2",    "line1\nline2\nline3\n",
                        "dir1/dir2/file1",    "line1\nline2\nline3\n",
                        "dir3/dir4/file3.gz", "line1\nline2\nline3\n",
                    },
                },
                "dir3", new Object[] {
                    "file1", "line1\nline2\nline3\n",
                },
            },
        }).save(FileProcessingTests.TEST_FILES);
    }

    @After public void
    tearDown() throws Exception {
        if (FileProcessingTests.TEST_FILES.exists()) FileUtil.deleteRecursively(FileProcessingTests.TEST_FILES);
    }

    static
    class PathRecorder implements ContentsProcessor<Void> {

        private final List<String> paths = new ArrayList<String>();

        @Override @Nullable public Void
        process(
            String                                                            path,
            InputStream                                                       inputStream,
            long                                                              size,
            long                                                              crc32,
            ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
        ) {
            this.paths.add(path);
            return null;
        }

        public List<String>
        getPaths() { return this.paths; }
    }

    @Test public void
    test1() throws IOException, InterruptedException {
        Glob pathPredicate = Glob.compile("**file*", Pattern2.WILDCARD);

        PathRecorder pr = new PathRecorder();

        ExceptionHandler<IOException> exceptionHandler = ExceptionHandler.<IOException>defaultHandler();
        FileProcessings.directoryTreeProcessor(
            pathPredicate,                                                       // pathPredicate,
            FileProcessings.recursiveCompressedAndArchiveFileProcessor(          // regularFileProcessor
                PredicateUtil.<String>always(),                      // lookIntoFormat
                PredicateUtil.<String>always(),                      // pathPredicate
                ContentsProcessings.<Void>nopArchiveCombiner(),      // archiveEntryCombiner
                pr,                                                  // contentsProcessor
                exceptionHandler                                     // exceptionHandler
            ),
            Collator.getInstance(),                                              // directoryMemberNameComparator
            FileProcessings.<Void>nopDirectoryCombiner(),                        // directoryCombiner
            new SquadExecutor<Void>(ConcurrentUtil.SEQUENTIAL_EXECUTOR_SERVICE), // squadExecutor
            exceptionHandler                                                     // exceptionHandler
        ).process("prefix::", FileProcessingTests.TEST_FILES);
        Assert.assertEquals(Arrays.asList(new String[] {
            "prefix::\\dir1\\dir2\\file.zip!dir1/dir2/file1",
            "prefix::\\dir1\\dir2\\file.zip!dir3/dir4/file2",
            "prefix::\\dir1\\dir2\\file2",
            "prefix::\\dir1\\dir2\\file3.tgz!!dir3/dir4/file2",
            "prefix::\\dir1\\dir2\\file3.tgz!!dir1/dir2/file1",
            "prefix::\\dir1\\dir2\\file3.tgz!!dir3/dir4/file3.gz!",
            "prefix::\\dir1\\dir3\\file1",
            "prefix::\\dir1\\file1",
            "prefix::\\dir1\\file2.gz!",
        }), pr.getPaths());
    }
}
