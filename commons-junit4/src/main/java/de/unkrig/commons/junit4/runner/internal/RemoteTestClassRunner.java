
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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

public
class RemoteTestClassRunner<T> extends ParentRunner<T> {

    public
    interface ParentRunnerInterface<T> extends Serializable {
        void        runChild(T method, RunNotifier notifier);
        Description describeChild(T method);
        List<T>     getChildren();
    }
    private ObjectOutputStream toSlave;
    private ObjectInputStream  fromSlave;
    
    
    public
    RemoteTestClassRunner(Class<?> clasS, OutputStream toSlave, InputStream fromSlave) throws Exception {
        super(clasS);
        
        this.toSlave = new ObjectOutputStream(toSlave);
        this.toSlave.writeObject(clasS.getName());
        this.toSlave.flush();
        
        this.fromSlave = new ObjectInputStream(fromSlave);
    }

    public
    interface FunctionDto<T, R> extends /*Function<T, R>,*/ Serializable {

        @Nullable R
        apply(T t);
    }
    
    public
    interface BackCaller extends Serializable {
        Object callBack(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception;
    }
    
    public abstract static
    class BackCallingFunction<T, R> implements FunctionDto<T, R> {

        private static final long serialVersionUID = 1L;
        
        @Nullable private BackCaller backCaller;

        public void
        setBackCaller(BackCaller backCaller) { this.backCaller = backCaller; }
        
        protected void
        callBack(String methodName, Class<?> parameterType, Object argument) throws Exception {
            BackCaller bc = this.backCaller;
            assert bc != null;
            bc.callBack(methodName, new Class<?>[] { parameterType }, new Object[] { argument });
        }
    }
    
    public static
    class FrameworkMethodDto implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Class<?>   declaringClass;
        private final String     methodName;
        private final Class<?>[] parameterTypes;

        public
        FrameworkMethodDto(FrameworkMethod frameworkMethod) {
            
            Method m = frameworkMethod.getMethod();
            
            this.declaringClass = m.getDeclaringClass();
            this.methodName     = m.getName();
            this.parameterTypes = m.getParameterTypes();
        }
        
        public FrameworkMethod
        toFrameworkMethod() {
            try {
                return new FrameworkMethod(this.declaringClass.getMethod(this.methodName, this.parameterTypes));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    @SuppressWarnings("unchecked") private <I extends FunctionDto<?, ?>, O> O
    callSlave(I input, @Nullable Object callbackTarget) {
        try {
            
            this.toSlave.writeObject(input);
            this.toSlave.flush();

            for (;;) {

                Object o = this.fromSlave.readObject();

                if (o instanceof Exception) throw (Exception) o;
                
                if (o == null) break;

                assert callbackTarget != null;
                String callbackMethodName = (String) o;

                Class<?>[] parameterTypes = (Class<?>[]) this.fromSlave.readObject();
                Object[]   arguments      = (Object[]) this.fromSlave.readObject();
                assert parameterTypes.length == arguments.length;
                
                Object callbackResult = (
                    callbackTarget
                    .getClass()
                    .getMethod(callbackMethodName, parameterTypes)
                    .invoke(callbackTarget, arguments)
                );
                this.toSlave.writeObject(callbackResult);
                this.toSlave.flush();
            }
            
            Object result = this.fromSlave.readObject();
            return (O) result;
        } catch (Exception e) {
            AssertionError ae = new AssertionError("Exception from client");
            ae.initCause(e);
            throw ae;
        }
    }
    
    @Override public List<T>
    getChildren() {
        return callSlave(new GetChildrenDto(), null);
    }

    public static
    class GetChildrenDto implements FunctionDto<
        ParentRunnerInterface<FrameworkMethodDto>,
        List<FrameworkMethodDto>
    > {
        
        private static final long serialVersionUID = 1L;

        @Override public List<FrameworkMethodDto>
        apply(ParentRunnerInterface<FrameworkMethodDto> pri) { return pri.getChildren(); }
    }

    @Override @NotNullByDefault(false) public Description
    describeChild(T child) {
        return callSlave(new DescribeChildDto<T>(child), null);
    }

    public static
    class DescribeChildDto<T> implements FunctionDto<ParentRunnerInterface<T>, Description> {
        
        private static final long serialVersionUID = 1L;

        private final T child;

        public
        DescribeChildDto(T child) { this.child = child; }
        
        @Override public Description
        apply(ParentRunnerInterface<T> pri) { return pri.describeChild(this.child); }
    }
    
    @Override @NotNullByDefault(false) public void
    runChild(final T child, final RunNotifier notifier) {
        this.<RunChildDto<T>, RunNotifier>callSlave(new RunChildDto<T>(child), notifier);
    }

    public static
    class RunChildDto<T> extends BackCallingFunction<ParentRunnerInterface<T>, Void> {
        
        private static final long serialVersionUID = 1L;
        
        private final T child;

        public
        RunChildDto(T child) {
            this.child = child;
        }

        @Override @Nullable public Void
        apply(ParentRunnerInterface<T> pri) {

            @NotNullByDefault(false)
            class MyRunListener extends RunListener {

                // SUPPRESS CHECKSTYLE LineLength:7
                @Override public void testRunStarted(Description description) throws Exception { callBack("fireTestRunStarted",        Description.class, description); }
                @Override public void testRunFinished(Result result) throws Exception          { callBack("fireTestRunFinished",       Result.class,      result);      }
                @Override public void testStarted(Description description) throws Exception    { callBack("fireTestStarted",           Description.class, description); }
                @Override public void testFinished(Description description) throws Exception   { callBack("fireTestFinished",          Description.class, description); }
                @Override public void testFailure(Failure failure) throws Exception            { callBack("fireTestFailure",           Failure.class,     failure);     }
                @Override public void testIgnored(Description description) throws Exception    { callBack("fireTestIgnored",           Description.class, description); }
                
                @Override public void
                testAssumptionFailure(Failure failure) {
                    try {
                        callBack("fireTestAssumptionFailure", Failure.class, failure);
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            }

            RunListener runListener = new MyRunListener();
            RunNotifier runNotifier = new RunNotifier();
            runNotifier.addListener(runListener);

            pri.runChild(this.child, runNotifier);
            
            runNotifier.removeListener(runListener);
            return null;
        }
    }
}
