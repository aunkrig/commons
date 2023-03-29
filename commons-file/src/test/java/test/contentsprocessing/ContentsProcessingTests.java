
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2021, Arno Unkrig
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

// SUPPRESS CHECKSTYLE JavadocMethod:9999

package test.contentsprocessing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.utils.Charsets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessings;
import de.unkrig.commons.file.contentsprocessing.ContentsProcessor;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.pattern.Pattern2;
import test.fileprocessing.Files;

/**
 * Tests for the {@code de.unkrig.commons.file.fileprocessing} package.
 */
public
class ContentsProcessingTests {

    private static final File TEST_FILES = new File("./.test_files");

    @Before public void
    setUp() throws Exception {
        if (ContentsProcessingTests.TEST_FILES.exists()) FileUtil.deleteRecursively(ContentsProcessingTests.TEST_FILES);
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
        }).save(ContentsProcessingTests.TEST_FILES);
    }

    @After public void
    tearDown() throws Exception {
        if (ContentsProcessingTests.TEST_FILES.exists()) FileUtil.deleteRecursively(ContentsProcessingTests.TEST_FILES);
    }

    @Test public void
    globTest() throws IOException, InterruptedException {
        File   tf   = ContentsProcessingTests.TEST_FILES;
        String tfp  = tf.getPath();
        String tfpq = tfp.replace(File.separatorChar, '/');

        final List<String>      result = new ArrayList<String>();
        ContentsProcessor<Void> cp     = new ContentsProcessor<Void>() {

            @NotNullByDefault @Override @Nullable public Void
            process(
                String                                                            path,
                InputStream                                                       inputStream,
                @Nullable Date                                                    lastModifiedDate,
                long                                                              size,
                long                                                              crc32,
                ProducerWhichThrows<? extends InputStream, ? extends IOException> opener
            ) throws IOException {
                result.add(path + "=" + InputStreams.readAll(inputStream, Charsets.UTF_8, /*closeInputStream*/ false));
                return null;
            }
        };

        {
            result.clear();
            ContentsProcessings.glob(Pattern2.compile(tfpq, Pattern2.WILDCARD), cp);
            Assert.assertEquals(Collections.emptyList(), result);
        }

        {
            result.clear();
            ContentsProcessings.glob(Pattern2.compile(tfpq + "/*/*", Pattern2.WILDCARD), cp);
            Assert.assertEquals(
                Arrays.asList(
                    new File(tf, "/dir1/file1")    + "=line1\nline2\nline3\n",
                    new File(tf, "/dir1/file2.gz") + "%=line1\nline2\nline3\n"
                ),
                result
            );
        }

        {
            result.clear();
            ContentsProcessings.glob(Pattern2.compile(tfpq + "/**", Pattern2.WILDCARD), cp);
            Assert.assertEquals(
                Arrays.asList(
                    new File(tf, "dir1\\dir2\\file2")     + "=line1\nline2\nline3\n",
                    new File(tf, "dir1\\dir3\\file1")     + "=line1\nline2\nline3\n",
                    new File(tf, "dir1\\file1")           + "=line1\nline2\nline3\n",
                    new File(tf, "dir1\\file2.gz")        + "%=line1\nline2\nline3\n"
                ),
                result
            );
        }

        {
            result.clear();
            ContentsProcessings.glob(Pattern2.compile(tfpq + "/***.gz", Pattern2.WILDCARD), cp);
            Assert.assertEquals(
                Arrays.asList(
                    new File(tf, "dir1\\dir2\\file3.tgz") + "%!dir3/dir4/file3.gz%=line1\nline2\nline3\n",
                    new File(tf, "dir1\\file2.gz")        +                     "%=line1\nline2\nline3\n"
                ),
                result
            );
        }

        {
            result.clear();
            ContentsProcessings.glob(Pattern2.compile(tfpq + "***/file1", Pattern2.WILDCARD), cp);
            Assert.assertEquals(
                Arrays.asList(
                    new File(tf, "dir1\\dir2\\file.zip")  +  "!dir1/dir2/file1=line1\nline2\nline3\n",
                    new File(tf, "dir1\\dir2\\file3.tgz") + "%!dir1/dir2/file1=line1\nline2\nline3\n",
                    new File(tf, "dir1\\dir3\\file1")     +                  "=line1\nline2\nline3\n",
                    new File(tf, "dir1\\file1")          +                  "=line1\nline2\nline3\n",
                    new File(tf, "dir1\\file2.gz")       +                 "%=line1\nline2\nline3\n"
                ),
                result
            );
        }
    }
}
