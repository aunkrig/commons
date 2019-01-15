
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
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.BackCaller;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.BackCallingFunction;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.ParentRunnerInterface;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.FrameworkMethodDto;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.FunctionDto;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

public final
class RemoteTestClassRunnerClient {

    private RemoteTestClassRunnerClient() {}
    
    public static void
    main(String[] args) throws Exception {
        
        RemoteTestClassRunnerClient.run(System.in, System.out, args.length == 1 ? args[0] : "");
    }
    
    public static void
    run(InputStream fromMaster, OutputStream toMaster, String nameSuffix) throws Exception {
        
        ObjectInputStream  ois = new ObjectInputStream(fromMaster);
        ObjectOutputStream oos = new ObjectOutputStream(toMaster);
        
        try {
            run(ois, oos, nameSuffix);
        } catch (Exception e) {
            oos.writeObject(e);
        }
        
        oos.flush();
    }
        
    public static void
    run(final ObjectInputStream ois, final ObjectOutputStream oos, final String nameSuffix) throws Exception {
        
        String   testClassName = (String) ois.readObject();
        Class<?> clasS         = ClassLoader.getSystemClassLoader().loadClass(testClassName);
        
        @NotNullByDefault(false)
        class BlockJUnit4ClassRunner2 extends BlockJUnit4ClassRunner {

            @SuppressWarnings("unused") private static final long serialVersionUID = 1L;

            BlockJUnit4ClassRunner2(Class<?> clasS) throws InitializationError { super(clasS); }

            // SUPPRESS CHECKSTYLE LineLength:3
            @Override public List<FrameworkMethod> getChildren()                                                      { return super.getChildren();        }
            @Override public Description           describeChild(FrameworkMethod child)                               { return super.describeChild(child); }
            @Override public void                  runChild(FrameworkMethod child, RunNotifier notifier)              { super.runChild(child, notifier);   }

            @Override protected String
            testName(FrameworkMethod method) {
                return super.testName(method) + nameSuffix;
            }
        }

        final BlockJUnit4ClassRunner2 bju4cr2 = new BlockJUnit4ClassRunner2(clasS);
        
        ParentRunnerInterface<FrameworkMethodDto>
        pri = new ParentRunnerInterface<FrameworkMethodDto>() {
            
            private static final long serialVersionUID = 1L;

            // SUPPRESS CHECKSTYLE LineLength:3
            @Override public List<FrameworkMethodDto> getChildren()                                             { return toSfmList(bju4cr2.getChildren());                  }
            @Override public Description              describeChild(FrameworkMethodDto method)                  { return bju4cr2.describeChild(method.toFrameworkMethod()); }
            @Override public void                     runChild(FrameworkMethodDto method, RunNotifier notifier) { bju4cr2.runChild(method.toFrameworkMethod(), notifier);   }

            private List<FrameworkMethodDto>
            toSfmList(List<FrameworkMethod> fmList) {
                List<FrameworkMethodDto> result = new ArrayList<FrameworkMethodDto>(fmList.size());
                for (FrameworkMethod fm : fmList) result.add(new FrameworkMethodDto(fm));
                return result;
            }
        };
        for (;;) {
            
            @SuppressWarnings("unchecked") FunctionDto<ParentRunnerInterface<FrameworkMethodDto>, ?>
            function = (FunctionDto<ParentRunnerInterface<FrameworkMethodDto>, ?>) ois.readObject();
            
            if (function instanceof BackCallingFunction) {
                ((BackCallingFunction<?, ?>) function).setBackCaller(
                    new BackCaller() {
                        private static final long serialVersionUID = 1L;

                        @Override public Object
                        callBack(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception {
                            oos.writeObject(methodName);
                            oos.writeObject(parameterTypes);
                            oos.writeObject(arguments);
                            oos.flush();
                            return ois.readObject();
                        }
                    }
                );
            }
            Object result = function.apply(pri);
            
            oos.writeObject(null);
            oos.writeObject(result);
            oos.flush();
        }
    }
}
