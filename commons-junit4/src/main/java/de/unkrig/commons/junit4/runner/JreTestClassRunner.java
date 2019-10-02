
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2019, Arno Unkrig
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

package de.unkrig.commons.junit4.runner;

import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;

import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunnerClient;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

public
class JreTestClassRunner extends ParentRunner<Runner> {

    private final ParentRunner<?> delegate;
    private final String          name;

    public
    JreTestClassRunner(Class<?> clasS) throws Exception {
        this(clasS, null, clasS.getAnnotation(JavaHome.class).value()/*"c:/Program Files/Java/jdk-11.0.1"*/);
    }

    public
    JreTestClassRunner(Class<?> clasS, @Nullable ParentRunner<?> runWith, String javaHome) throws Exception {
        super(clasS);
        String classpath = System.getProperty("java.class.path");
        assert classpath != null;

        this.name = "[JAVA_HOME=" + javaHome + "]";

        ProcessBuilder pb = new ProcessBuilder(
            javaHome + "/bin/java.exe",
            "-classpath",
            classpath,
            RemoteTestClassRunnerClient.class.getName(),
            "[JAVA_HOME=" + javaHome + "]"
        );

        Process p = pb.start();

        this.delegate = new RemoteTestClassRunner(
            clasS,               // clasS
            runWith,             // delegate
            this.name,           // nameSuffix
            p.getOutputStream(), // toSlave
            p.getInputStream()   // fromSlave
        );
    }

    @Override protected String
    getName() { return this.name; }

    @Override @NotNullByDefault(false) protected Description
    describeChild(Runner child) {
        assert child == this.delegate;
        return child.getDescription();
    }

    @Override @NotNullByDefault(false) protected void
    runChild(Runner child, RunNotifier notifier) {
        assert child == this.delegate;
        child.run(notifier);
    }

    @Override protected List<Runner>
    getChildren() {
        return Collections.<Runner>singletonList(this.delegate);
    }
}
