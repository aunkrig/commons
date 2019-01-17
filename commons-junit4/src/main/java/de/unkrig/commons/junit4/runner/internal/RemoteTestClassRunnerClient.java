
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

package de.unkrig.commons.junit4.runner.internal;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

public final
class RemoteTestClassRunnerClient {

    private RemoteTestClassRunnerClient() {}
    
    public static void
    main(String[] args) throws Exception {
        
        RemoteTestClassRunnerClient.run(System.in, System.out);
    }
    
    public static void
    run(InputStream fromMaster, OutputStream toMaster) throws Exception {
        
        ObjectInputStream  ois = new ObjectInputStream(fromMaster);
        ObjectOutputStream oos = new ObjectOutputStream(toMaster);
        
        try {
            run(ois, oos);
        } catch (Exception e) {
            oos.writeObject(e);
        }
        
        oos.flush();
    }
        
    public static void
    run(final ObjectInputStream ois, final ObjectOutputStream oos) throws Exception {
        
        String       testClassName   = (String) ois.readObject();
        String       runnerClassName = (String) ois.readObject();
        final String nameSuffix      = (String) ois.readObject();
        
        ClassLoader scl = ClassLoader.getSystemClassLoader();

        Class<?> clasS = scl.loadClass(testClassName);

        Runner runner;
        try {
            try {
                runner = (ParentRunner<?>) (
                    scl
                    .loadClass(runnerClassName)
                    .getConstructor(Class.class)
                    .newInstance(clasS)
                );
            } catch (InvocationTargetException ite) {
                Throwable tt = ite.getTargetException();
                if (tt instanceof Exception) throw (Exception) tt;
                throw ite;
            }
        } catch (InitializationError ie) {
            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            pw.println("clasS=" + clasS.getName());
            pw.println("Causes are:");
            for (Throwable cause : ie.getCauses()) cause.printStackTrace(pw);
            pw.flush();
            throw new Exception(sw.toString(), ie);
        } catch (Exception e) {
            throw new Exception("clasS=" + clasS.getName(), e);
        }

        @NotNullByDefault(false)
        class MyRunListener extends RunListener {

            // SUPPRESS CHECKSTYLE LineLength:7
            @Override public void testRunStarted(Description description) throws Exception { this.callBack("fireTestRunStarted",  Description.class, fixDescription(description)); }
            @Override public void testRunFinished(Result result)          throws Exception { this.callBack("fireTestRunFinished", Result.class,      result);                      }
            @Override public void testStarted(Description description)    throws Exception { this.callBack("fireTestStarted",     Description.class, fixDescription(description)); }
            @Override public void testFinished(Description description)   throws Exception { this.callBack("fireTestFinished",    Description.class, fixDescription(description)); }
            @Override public void testFailure(Failure failure)            throws Exception { this.callBack("fireTestFailure",     Failure.class,     fixFailure(failure));         }
            @Override public void testIgnored(Description description)    throws Exception { this.callBack("fireTestIgnored",     Description.class, fixDescription(description)); }

            private Failure
            fixFailure(Failure failure) {
                return new Failure(fixDescription(failure.getDescription()), failure.getException());
            }
            
            private Description
            fixDescription(Description desc) {

                Collection<Annotation> ac = desc.getAnnotations();

                Description result = Description.createSuiteDescription(
                    desc.getDisplayName() + nameSuffix,                       // name
                    desc.getDisplayName() + nameSuffix,                       // uniqueId
                    ac == null ? null : ac.toArray(new Annotation[ac.size()]) // annotations
                );
                
                return result;
            }
            @Override public void
            testAssumptionFailure(Failure failure) {
                try {
                    this.callBack("fireTestAssumptionFailure", Failure.class, failure);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }

            private void
            callBack(String methodName, Class<?> parameterType, Object argument) throws Exception {
                oos.writeObject(methodName);
                oos.writeObject(new Class<?>[] { parameterType });
                oos.writeObject(new Object[]   { argument      });
                oos.flush();
            }
        }
        RunNotifier runNotifier = new RunNotifier();
        runNotifier.addListener(new MyRunListener());

        for (;;) {
            
            ois.readObject(); // Wait until the Runner is run.

            runner.run(runNotifier);
            
            oos.writeObject(null);
            oos.flush();
        }
    }
}
