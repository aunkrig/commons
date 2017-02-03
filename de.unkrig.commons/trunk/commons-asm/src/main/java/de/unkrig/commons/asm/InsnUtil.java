
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

package de.unkrig.commons.asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A utility class for handling of ASM instruction lists.
 */
public final
class InsnUtil {

    private
    InsnUtil() {}

    /**
     * @return An {@link InsnList} with the single instruction <var>ain</var>.
     */
    public static InsnList
    il(AbstractInsnNode ain) {
        InsnList insns = new InsnList();
        insns.add(ain);
        return insns;
    }

    /**
     * @return An {@link InsnList} that contains the given instruction nodes.
     */
    public static InsnList
    il(AbstractInsnNode... ains) {
        InsnList insns = new InsnList();
        for (AbstractInsnNode ain : ains) {
            insns.add(ain);
        }
        return insns;
    }

    /**
     * @param  il {@link InsnList}s that each create exactly one {@link Object} on the operand stack
     * @return Code that produces an object array filled with the values produced by the <var>il</var>s
     */
    public static InsnList
    oa(InsnList... il) {
        InsnList insns = new InsnList();
        insns.add(new LdcInsnNode(il.length));
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        for (int i = 0; i < il.length; i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new LdcInsnNode(i));
            insns.add(il[i]);
            insns.add(new InsnNode(Opcodes.AASTORE));
        }
        return insns;
    }

    /**
     * @param  insnLists {@link InsnList}s that each create exactly one {@link Object} on the operand stack
     * @return Code that produces an object array filled with the values produced by the <var>insnLists</var>
     */
    public static InsnList
    oa(List<InsnList> insnLists) {
        if (insnLists.isEmpty()) return InsnUtil.il(InsnUtil.push(null));

        InsnList insns = new InsnList();
        insns.add(InsnUtil.push(insnLists.size()));
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        for (int i = 0; i < insnLists.size(); i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new LdcInsnNode(i));
            insns.add(insnLists.get(i));
            insns.add(new InsnNode(Opcodes.AASTORE));
        }
        return insns;
    }

    /** @return Code that puts the given constant value on the operand stack */
    public static AbstractInsnNode
    push(@Nullable Object constantValue) {
        return (
            constantValue == null
            ? new InsnNode(Opcodes.ACONST_NULL)
            : new LdcInsnNode(constantValue)
        );
    }
}
