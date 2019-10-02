
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

public
class MultipleJresTestClassRunner extends ParentRunner<Runner> {

    public static final String JAVA6_HOME  = "c:/Program Files/Java/jdk1.6.0_43";
    public static final String JAVA7_HOME  = "c:/Program Files/Java/jdk1.7.0_17";
    public static final String JAVA8_HOME  = "c:/Program Files/Java/jdk1.8.0_192";
    public static final String JAVA9_HOME  = "c:/Program Files/Java/jdk-9.0.4";
    public static final String JAVA10_HOME = "c:/Program Files/Java/jdk-10.0.2";
    public static final String JAVA11_HOME = "c:/Program Files/Java/jdk-11.0.1";

    private String[] javaHomes = {
        JAVA6_HOME,
        JAVA7_HOME,
        JAVA8_HOME,
        JAVA9_HOME,
        JAVA10_HOME,
        JAVA11_HOME,
    };

    public
    MultipleJresTestClassRunner(Class<?> clasS) throws Exception {
        super(clasS);

        JavaHomes javaHomes = clasS.getAnnotation(JavaHomes.class);
        if (javaHomes != null) {
            this.javaHomes = javaHomes.value();
        }
    }

    @Override @NotNullByDefault(false) protected void
    runChild(final Runner child, final RunNotifier notifier) {
        child.run(notifier);
    }

    @Override protected List<Runner>
    getChildren() {

        Class<?>        clasS   = this.getTestClass().getJavaClass();
        ParentRunner<?> runWith = null;
        try {

            TestClass testClassAnnotation = clasS.getAnnotation(TestClass.class);
            if (testClassAnnotation != null) {

                clasS = testClassAnnotation.value();

                Class<? extends ParentRunner<?>> runWithClass = testClassAnnotation.runWith();
                if (runWithClass != null) {
                    runWith = runWithClass.getConstructor(Class.class).newInstance(clasS);
                }
            }

            List<Runner> result = new ArrayList<Runner>();
            for (String javaHome : this.javaHomes) {
                result.add(new JreTestClassRunner(clasS, runWith, javaHome));
            }
            return result;
        } catch (Exception e) {
            throw newAssertionError("clasS=" + clasS.getName() + ", runWith=" + runWith, e);
        }
    }

    public static AssertionError
    newAssertionError(@Nullable Throwable t) { return newAssertionError(null, t); }

    public static AssertionError
    newAssertionError(@Nullable String message, @Nullable Throwable t) {

        if (t instanceof InitializationError) {
            InitializationError ie = (InitializationError) t;

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            pw.println("Causes are:");
            for (Throwable cause : ie.getCauses()) cause.printStackTrace(pw);
            pw.flush();

            t = new Exception(sw.toString(), t);
        }

        AssertionError ae = new AssertionError(message);
        ae.initCause(t);
        return ae;
    }

    @Override @NotNullByDefault(false) protected Description
    describeChild(Runner child) { return child.getDescription(); }
}
