
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.commons.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;

import de.unkrig.commons.lang.protocol.Stoppable;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various {@link Process}-related utility methods.
 */
public final
class ProcessUtil {

    private
    ProcessUtil() {}

    interface ProcessStarter {

        /**
         * Starts a processes from the <var>processBuilder</var> such that the process reads from <var>stdin</var> and
         * writes to <var>stdout</var> and <var>stderr</var>.
         */
        Process
        startProcess(
            ProcessBuilder processBuilder,
            InputStream    stdin,
            boolean        closeStdin,
            OutputStream   stdout,
            boolean        closeStdout,
            OutputStream   stderr,
            boolean        closeStderr
        ) throws IOException;
    }

    private static final ProcessStarter PROCESS_STARTER;
    static {

        ProcessStarter ps;

        try {
            Class<?> redirect = Class.forName("java.lang.ProcessBuilder.Redirect");

            final Object redirectInherit;
            final Method redirectInput;
            final Method redirectOutput;
            final Method redirectError;
            try {
                redirectInherit = redirect.getField("INHERIT");
                redirectInput   = redirect.getMethod("redirectInput", redirect);
                redirectOutput  = redirect.getMethod("redirectOutput", redirect);
                redirectError   = redirect.getMethod("redirectError", redirect);
            } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                throw new ExceptionInInitializerError(t);
            }
            ps = new ProcessStarter() {

                @Override public Process
                startProcess(
                    ProcessBuilder processBuilder,
                    InputStream    stdin,
                    boolean        closeStdin,
                    OutputStream   stdout,
                    boolean        closeStdout,
                    OutputStream   stderr,
                    boolean        closeStderr
                ) throws IOException {

                    // Use "ProcessBuilder.redirect(Input|Output|Error)()" iff stdin/out/err is not redirectoed.
                    try {

                        if (stdin == System.in) {
                            // Only available in Java 7:
                            //     processBuilder.redirectInput(Redirect.INHERIT);
                            redirectInput.invoke(processBuilder, redirectInherit);
                        }

                        if (stdout == System.out) {
                            // Only available in Java 7:
                            //     processBuilder.redirectOutput(Redirect.INHERIT);
                            redirectOutput.invoke(processBuilder, redirectInherit);
                        }

                        if (stderr == System.err) {
                            // Only available in Java 7:
                            //     processBuilder.redirectError(Redirect.INHERIT);
                            redirectError.invoke(processBuilder, redirectInherit);
                        }
                    } catch (Exception e) {
                        throw ExceptionUtil.wrap(null, e, AssertionError.class);
                    }

                    final Process process = processBuilder.start();

                    // Copy the process's stdin/out/err iff it is redirected.
                    if (stdout != System.out) {
                        ProcessUtil.copyInBackground(process.getInputStream(), false, stdout, closeStdout);
                    }
                    if (stderr != System.err) {
                        ProcessUtil.copyInBackground(process.getErrorStream(), false, stderr, closeStderr);
                    }
                    if (stdin != System.in) {
                        try {
                            ProcessUtil.copy(stdin, closeStdin, process.getOutputStream(), true);
                        } catch (IOException e) {

                            // IOExceptions are normal here, because the processes may legitimately terminate before it
                            // has completely read ist standard input.
                            ;
                        }
                    }

                    return process;
                }
            };
        } catch (ClassNotFoundException e) {
            ps = new ProcessStarter() {

                @Override public Process
                startProcess(
                    ProcessBuilder processBuilder,
                    InputStream    stdin,
                    boolean        closeStdin,
                    OutputStream   stdout,
                    boolean        closeStdout,
                    OutputStream   stderr,
                    boolean        closeStderr
                ) throws IOException {

                    final Process process = processBuilder.start();

                    ProcessUtil.copyInBackground(process.getInputStream(), false, stdout, closeStdout);
                    ProcessUtil.copyInBackground(process.getErrorStream(), false, stderr, closeStderr);

                    try {
                        ProcessUtil.copy(stdin, closeStdin, process.getOutputStream(), true);
                    } catch (IOException e) {

                        // IOExceptions are normal here, because the processes may legitimately terminate before it has
                        // completely read ist standard input.
                    }

                    return process;
                }
            };
        }

        PROCESS_STARTER = ps;
    }

    /**
     * Executes the given <var>command</var> and waits until it completes.
     *
     * @param workingDirectory See {@link ProcessBuilder#directory(File)}
     * @param stdin            Where the <var>command</var>'s standard input is read from
     * @param closeStdin       Whether <var>stdin</var> should be closed
     * @param stdout           Where the <var>command</var>'s standard output is written to
     * @param closeStdout      Whether <var>stdout</var> should be closed
     * @param stderr           Where the <var>command</var>'s standard error is written to
     * @param closeStderr      Whether <var>stderr</var> should be closed
     * @return                 Whether the <var>command</var> terminated with exit status zero (i.e. success)
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean
    execute(
        List<String>   command,
        @Nullable File workingDirectory,
        InputStream    stdin,
        boolean        closeStdin,
        OutputStream   stdout,
        boolean        closeStdout,
        OutputStream   stderr,
        boolean        closeStderr
    ) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory);

        Process process = ProcessUtil.PROCESS_STARTER.startProcess(
            processBuilder,
            stdin,
            closeStdin,
            stdout,
            closeStdout,
            stderr,
            closeStderr
        );

        return process.waitFor() == 0;
    }

    private static Stoppable
    copyInBackground(final InputStream in, final boolean closeIn, final OutputStream out, final boolean closeOut) {

        return ThreadUtil.runInBackground(new Runnable() {

            @Override public void
            run() {
                try {
                    ProcessUtil.copy(in, closeIn, out, closeOut);
                } catch (IOException e) {
                    ;
                }
            }
        }, "copy-in-background");
    }

    /**
     * A copy of {@code IoUtil.copy()}, to avoid a dependency on project {@code de.unkrig.commons.io}.
     */
    private static void
    copy(InputStream inputStream, boolean closeInputStream, OutputStream outputStream, boolean closeOutputStream)
    throws IOException {

        try {
            byte[] buffer = new byte[4096];
            for (;;) {
                int m = inputStream.read(buffer);
                if (m == -1) break;
                outputStream.write(buffer, 0, m);
            }

            outputStream.flush();

            if (closeInputStream) inputStream.close();
            if (closeOutputStream) outputStream.close();
        } catch (IOException ioe) {
            if (closeInputStream) try { inputStream.close(); } catch (Exception e) {}
            if (closeOutputStream) try { outputStream.close(); } catch (Exception e) {}
            throw ioe;
        }
    }
}
