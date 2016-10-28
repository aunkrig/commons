
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.asm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Bytecode generator for {@link StringBuilder}-based concatenation of strings.
 */
public
class StringConcatenator {

    private static final Map<Type, String> STRING_BUILDER_APPEND_TYPES;
    static {
        Map<Type, String> m = new HashMap<Type, String>();
        m.put(Type.BOOLEAN_TYPE,        "(Z)Ljava/lang/StringBuilder;");
        m.put(Type.CHAR_TYPE,           "(C)Ljava/lang/StringBuilder;");
        m.put(Type.BYTE_TYPE,           "(I)Ljava/lang/StringBuilder;");
        m.put(Type.SHORT_TYPE,          "(I)Ljava/lang/StringBuilder;");
        m.put(Type.INT_TYPE,            "(I)Ljava/lang/StringBuilder;");
        m.put(Type.FLOAT_TYPE,          "(F)Ljava/lang/StringBuilder;");
        m.put(Type.LONG_TYPE,           "(J)Ljava/lang/StringBuilder;");
        m.put(Type.DOUBLE_TYPE,         "(D)Ljava/lang/StringBuilder;");
        m.put(Types.OBJECT_TYPE,        "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        m.put(Types.CHAR_SEQUENCE_TYPE, "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;");
        m.put(Types.STRING_TYPE,        "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        m.put(Types.STRING_BUFFER_TYPE, "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;");
        STRING_BUILDER_APPEND_TYPES = Collections.unmodifiableMap(m);
    }

    private static final Map<Type, String> STRING_VALUE_OF_TYPES;
    static {
        Map<Type, String> m = new HashMap<Type, String>();
        m.put(Type.BOOLEAN_TYPE, "(Z)Ljava/lang/String;");
        m.put(Type.CHAR_TYPE,    "(C)Ljava/lang/String;");
        m.put(Type.BYTE_TYPE,    "(I)Ljava/lang/String;");
        m.put(Type.SHORT_TYPE,   "(I)Ljava/lang/String;");
        m.put(Type.INT_TYPE,     "(I)Ljava/lang/String;");
        m.put(Type.FLOAT_TYPE,   "(F)Ljava/lang/String;");
        m.put(Type.LONG_TYPE,    "(J)Ljava/lang/String;");
        m.put(Type.DOUBLE_TYPE,  "(D)Ljava/lang/String;");
        m.put(Types.OBJECT_TYPE, "(Ljava/lang/Object;)Ljava/lang/String;");
        STRING_VALUE_OF_TYPES = Collections.unmodifiableMap(m);
    }

    private final List<Component> components = new ArrayList<Component>();

    /**
     * Registers the given constant string, which will be added on {@link #finish()}.
     *
     * @return {@code this}
     */
    public StringConcatenator
    appendConstant(String value) {
        if (value.length() == 0) return this;

        if (!this.components.isEmpty()) {
            Component c = this.components.get(this.components.size() - 1);
            if (c instanceof ConstantComponent) {
                ConstantComponent cc = (ConstantComponent) c;
                cc.value += value;
                return this;
            }
        }
        this.components.add(new ConstantComponent(value));
        return this;
    }

    /**
     * Registers the given code fragment, which will be executed, and the result added on {@link #finish()}.
     *
     * @param insns Instructions which must produce exactly one value of {@code type} on the operand stack
     * @return      {@code this}
     */
    public StringConcatenator
    appendVariable(InsnList insns, Type type) {
        this.components.add(new VariableComponent(insns, type));
        return this;
    }

    /**
     * Registers the given code fragment, which will be executed, and the result added on {@link #finish()}. If the
     * {@code type} is an array, a string, {@link StringBuffer}, {@link StringBuilder}, {@link CharSequence} or a
     * {@code char}, then it is 'pretty-printed' i nthe style of a Java constant.
     *
     * @param code Instructions which must produce exactly one value of {@code type} on the operand stack
     * @return     {@code this}
     */
    public StringConcatenator
    appendVariablePrettily(InsnList code, Type type) {
        if (type.getSort() == Type.ARRAY) {
            Type et = type.getElementType();
            if (et.getSort() == Type.ARRAY || et.getSort() == Type.OBJECT) {
                code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Arrays",
                    "deepToString",
                    "([Ljava/lang/Object;)Ljava/lang/String;"
                ));
                this.appendVariable(code, Types.STRING_TYPE);
            } else {
                code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Arrays",
                    "toString",
                    "([" + et.getDescriptor() + ")Ljava/lang/String;"
                ));
                this.appendVariable(code, Types.STRING_TYPE);
            }
        } else
        if (
            type.equals(Types.STRING_TYPE)
            || type.equals(Types.STRING_BUFFER_TYPE)
            || type.equals(Types.STRING_BUILDER_TYPE)
            || type.equals(Types.CHAR_SEQUENCE_TYPE)
        ) {
            this.appendConstant("\"");
            this.appendVariable(code, type);
            this.appendConstant("\"");
        } else
        if (type.equals(Type.CHAR_TYPE)) {
            this.appendConstant("'");
            this.appendVariable(code, Type.CHAR_TYPE);
            this.appendConstant("'");
        } else
        {
            this.appendVariable(code, type);
        }
        return this;
    }

    /**
     * @return The {@link InsnList} that implements the creation of the string result
     */
    public InsnList
    finish() {
        InsnList insns = new InsnList();
        switch (this.components.size()) {
        case 0:
            insns.add(new LdcInsnNode(""));
            break;
        case 1:
            this.components.get(0).pushAsString(insns);
            break;
        case 2:
            this.components.get(0).pushAsString(insns);
            this.components.get(1).pushAsString(insns);
            insns.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "concat",
                "(Ljava/lang/String;)Ljava/lang/String;"
            ));
            break;
        default:
            insns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
            insns.add(new InsnNode(Opcodes.DUP));
            (this.components.get(0)).pushAsString(insns);
            insns.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V"
            ));
            for (int i = 1; i < this.components.size(); ++i) {
                Component c  = this.components.get(i);
                Type      t  = c.push(insns);
                String    md = StringConcatenator.STRING_BUILDER_APPEND_TYPES.get(t);
                if (md == null) md = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", md));
            }
            insns.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;"
            ));
        }

        return insns;
    }

    interface Component {

        /**
         * Adds instructions to the given which produces the value of this component on the operand stack.
         *
         * @return The type of this component
         */
        Type
        push(InsnList result);

        /**
         * Adds instructions to the given which produces the value of this component on the operand stack and converts
         * it to a string.
         */
        void
        pushAsString(InsnList result);
    }

    class ConstantComponent implements StringConcatenator.Component {

        /**
         * The constant string value.
         */
        String value;

        ConstantComponent(String value) {
            assert value != null;
            this.value = value;
        }

        @Override public Type
        push(InsnList result) {
            result.add(new LdcInsnNode(this.value));
            return Types.STRING_TYPE;
        }

        @Override public void
        pushAsString(InsnList result) {
            result.add(new LdcInsnNode(this.value));
        }
    }

    class VariableComponent implements StringConcatenator.Component {
        private final InsnList insns;
        private final Type     type;

        VariableComponent(InsnList insns, Type type) {
            this.insns = insns;
            this.type  = type;
        }

        @Override public Type
        push(InsnList result) {
            result.add(this.insns);
            return this.type;
        }

        @Override public void
        pushAsString(InsnList result) {
            result.add(this.insns);
            String md = StringConcatenator.STRING_VALUE_OF_TYPES.get(this.type);
            if (md == null) md = "(Ljava/lang/Object;)Ljava/lang/String;";
            result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", md));
        }
    }
}
