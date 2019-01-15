
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

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

public
class MultipleJresTestClassRunner extends ParentRunner<Runner> {

    public static final String JAVA6_HOME  = "c:/Program Files/Java/jdk1.6.0_43";
    public static final String JAVA7_HOME  = "c:/Program Files/Java/jdk1.7.0_17";
    public static final String JAVA8_HOME  = "c:/Program Files/Java/jdk1.8.0_192";
    public static final String JAVA9_HOME  = "c:/Program Files/Java/jdk-9.0.4";
    public static final String JAVA10_HOME = "c:/Program Files/Java/jdk-10.0.2";
    public static final String JAVA11_HOME = "c:/Program Files/Java/jdk-11.0.1";

    public static final String[] JAVA_HOMES = {
        JAVA6_HOME,
        JAVA7_HOME,
        JAVA8_HOME,
        JAVA9_HOME,
        JAVA10_HOME,
        JAVA11_HOME,    
    };
    
    public
    MultipleJresTestClassRunner(Class<?> clasS) throws Exception { super(clasS); }

    @Override @NotNullByDefault(false) protected void
    runChild(final Runner child, final RunNotifier notifier) {
        child.run(notifier);
    }

    @Override protected List<Runner>
    getChildren() {
        try {
            List<Runner> result = new ArrayList<Runner>();
            for (String javaHome : JAVA_HOMES) {
                result.add(new JreTestClassRunner(this.getTestClass().getJavaClass(), javaHome));
            }
            return result;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override @NotNullByDefault(false) protected Description
    describeChild(Runner child) { return child.getDescription(); }
}
