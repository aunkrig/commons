
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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;

import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunnerClient;
import de.unkrig.commons.junit4.runner.internal.RemoteTestClassRunner.FrameworkMethodDto;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

public
class PipeTestClassRunner extends ParentRunner<FrameworkMethod> {

    private RemoteTestClassRunner<FrameworkMethodDto> delegate;
    
    public
    PipeTestClassRunner(Class<?> clasS) throws Exception {
        super(clasS);
        
        final PipedOutputStream toSlave    = new PipedOutputStream();
        final PipedInputStream  fromMaster = new PipedInputStream();
        fromMaster.connect(toSlave);

        final PipedOutputStream toMaster  = new PipedOutputStream();
        final PipedInputStream  fromSlave = new PipedInputStream();
        fromSlave.connect(toMaster);
        
        new Thread() {

            @Override public void
            run() {
                try {
                    RemoteTestClassRunnerClient.run(fromMaster, toMaster, "(piped)");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
            
        }.start();
        
        this.delegate = new RemoteTestClassRunner<FrameworkMethodDto>(clasS, toSlave, fromSlave);
    }

    @Override @NotNullByDefault(false) protected void
    runChild(final FrameworkMethod child, final RunNotifier notifier) {
        this.delegate.runChild(new FrameworkMethodDto(child), notifier);
    }

    @Override protected List<FrameworkMethod>
    getChildren() {
        List<FrameworkMethodDto> sfms   = this.delegate.getChildren();
        List<FrameworkMethod>    result = new ArrayList<FrameworkMethod>(sfms.size());
        for (FrameworkMethodDto sfm : sfms) result.add(sfm.toFrameworkMethod());
        return result;
    }
    
    @Override @NotNullByDefault(false) protected Description
    describeChild(FrameworkMethod child) {
        return this.delegate.describeChild(new FrameworkMethodDto(child));
    }
}
