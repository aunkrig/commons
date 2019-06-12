
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
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

public
class RemoteTestClassRunner extends ParentRunner<Runner> {

    private ObjectOutputStream toSlave;
    private ObjectInputStream  fromSlave;
    private Runner             delegate;
    private String             nameSuffix;
    
    public
    RemoteTestClassRunner(
        Class<?>         clasS,
        @Nullable Runner runWith,
        String           nameSuffix,
        OutputStream     toSlave,
        InputStream      fromSlave
    ) throws Exception {
        super(clasS);
        
        final Class<? extends Runner> runWithClass = (
            runWith != null ? runWith.getClass() : BlockJUnit4ClassRunner.class
        );
        
        this.toSlave = new ObjectOutputStream(toSlave);
        this.toSlave.writeObject(clasS.getName());
        this.toSlave.writeObject(runWithClass.getName());
        this.toSlave.writeObject(nameSuffix);
        this.toSlave.flush();
        
        this.nameSuffix = nameSuffix;
        this.fromSlave  = new ObjectInputStream(fromSlave);
        

        try {
            this.delegate = runWith != null ? runWith : new BlockJUnit4ClassRunner(clasS);
        } catch (Exception e) {
            throw new Exception("clasS=" + clasS, e);
        }
    }

    @Override protected List<Runner>
    getChildren() {
        return Collections.<Runner>singletonList(this.delegate);
    }

    @Override @NotNullByDefault(false) protected Description
    describeChild(Runner child) {
        assert child == this.delegate;
        return fixDescription(this.delegate.getDescription());
    }

    public Description
    fixDescription(Description desc) {
        
        Collection<Annotation> ac = desc.getAnnotations();

        Description result = Description.createSuiteDescription(
            desc.getDisplayName() + this.nameSuffix,                  // name
            desc.getDisplayName() + this.nameSuffix,                  // uniqueId
            ac == null ? null : ac.toArray(new Annotation[ac.size()]) // annotations
        );
        
        for (Description d : desc.getChildren()) {
            result.addChild(fixDescription(d));
        }
        
        return result;
    }

    @Override @NotNullByDefault(false) protected void
    runChild(Runner child, RunNotifier notifier) {
        throw new AssertionError();
    }

    @Override protected String
    getName() {
        return super.getName() + this.nameSuffix;
    }

    @Override @NotNullByDefault(false) public void
    run(RunNotifier notifier) {
        try {

            this.toSlave.writeObject(null);
            this.toSlave.flush();
            
            for (;;) {
                Object o = this.fromSlave.readObject();
                if (o == null) break;
                if (o instanceof Exception) throw (Exception) o;
                
                String     notifierMethodName = (String) o;
                Class<?>[] parameterTypes     = (Class<?>[]) this.fromSlave.readObject();
                Object[]   arguments          = (Object[])   this.fromSlave.readObject();
                
                RunNotifier.class.getMethod(notifierMethodName, parameterTypes).invoke(notifier, arguments);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
