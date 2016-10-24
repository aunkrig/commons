
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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility class around {@link org.objectweb.asm.Opcodes}.
 */
public final
class OpcodeUtil {

    private
    OpcodeUtil() {}

    private static final String[] OPCODE_NAMES = {
        "NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4",
        "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1 = 15",
        "BIPUSH", "SIPUSH", "LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD", "FLOAD",
        "DLOAD", "ALOAD", "ILOAD_0", "ILOAD_1", "ILOAD_2", "ILOAD_3", "LLOAD_0", "LLOAD_1",
        "LLOAD_2", "LLOAD_3", "FLOAD_0", "FLOAD_1", "FLOAD_2", "FLOAD_3", "DLOAD_0", "DLOAD_1",
        "DLOAD_2", "DLOAD_3", "ALOAD_0", "ALOAD_1", "ALOAD_2", "ALOAD_3", "IALOAD", "LALOAD",
        "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD", "ISTORE", "LSTORE",
        "FSTORE", "DSTORE", "ASTORE", "ISTORE_0", "ISTORE_1", "ISTORE_2", "ISTORE_3", "LSTORE_0",
        "LSTORE_1", "LSTORE_2", "LSTORE_3", "FSTORE_0", "FSTORE_1", "FSTORE_2", "FSTORE_3", "DSTORE_0",
        "DSTORE_1", "DSTORE_2", "DSTORE_3", "ASTORE_0", "ASTORE_1", "ASTORE_2", "ASTORE_3", "IASTORE",
        "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE", "POP",
        "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1", "DUP2_X2", "SWAP",
        "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB",
        "IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV",
        "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG",
        "ISHL", "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND",
        "IOR", "LOR", "IXOR", "LXOR", "IINC", "I2L", "I2F", "I2D",
        "L2I", "L2F", "L2D", "F2I", "F2L", "F2D", "D2I", "D2L",
        "D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL",
        "DCMPG", "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ",
        "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO",
        "JSR", "RET", "TABLESWITCH", "LOOKUPSWITCH", "IRETURN", "LRETURN", "FRETURN", "DRETURN",
        "ARETURN", "RETURN", "GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL", "INVOKESPECIAL",
        "INVOKESTATIC", "INVOKEINTERFACE", "INVOKEDYNAMIC", "NEW", "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH", "ATHROW",
        "CHECKCAST", "INSTANCEOF", "MONITORENTER", "MONITOREXIT", "WIDE", "MULTIANEWARRAY", "IFNULL", "IFNONNULL",
        "GOTO_W", "JSR_W", null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
    };

    /**
     * @return The name of the opcode of the given {@link AbstractInsnNode}
     */
    public static String
    getName(AbstractInsnNode ain) {
        return OpcodeUtil.OPCODE_NAMES[ain.getOpcode()];
    }

    /**
     * Description of a Java&trade; bytecode instruction.
     */
    public static final
    class InsnDescription {

        /**
         * The types of the instructions's operands.
         */
        @Nullable public final Type[] argumentOperandTypes;

        /**
         * The type of the instruction's result operand (or {@code null}).
         */
        @Nullable public final Type resultOperandType;

        public
        InsnDescription(@Nullable Type[] argumentOperandTypes, @Nullable Type resultOperandType) {
            this.argumentOperandTypes = argumentOperandTypes;
            this.resultOperandType    = resultOperandType;
        }
    }

    private static final InsnDescription[] INSN_DESCRIPTIONS = new InsnDescription[256];
    static { OpcodeUtil.initDescriptions(); }
    private static void
    initDescriptions() {
        OpcodeUtil.addRInsn(Type.INT_TYPE,     Opcodes.ILOAD);
        OpcodeUtil.addRInsn(Type.LONG_TYPE,    Opcodes.LLOAD);
        OpcodeUtil.addRInsn(Type.FLOAT_TYPE,   Opcodes.FLOAD);
        OpcodeUtil.addRInsn(Type.DOUBLE_TYPE,  Opcodes.DLOAD);
        OpcodeUtil.addRInsn(Types.OBJECT_TYPE, Opcodes.ALOAD);

        OpcodeUtil.addAInsn(Type.INT_TYPE,     Opcodes.ISTORE);
        OpcodeUtil.addAInsn(Type.LONG_TYPE,    Opcodes.LSTORE);
        OpcodeUtil.addAInsn(Type.FLOAT_TYPE,   Opcodes.FSTORE);
        OpcodeUtil.addAInsn(Type.DOUBLE_TYPE,  Opcodes.DSTORE);
        OpcodeUtil.addAInsn(Types.OBJECT_TYPE, Opcodes.ASTORE);

        OpcodeUtil.addAARInsn(Types.INT_ARRAY_TYPE,       Type.INT_TYPE, Type.INT_TYPE,     Opcodes.IALOAD);
        OpcodeUtil.addAARInsn(Types.LONG_ARRAY_TYPE,      Type.INT_TYPE, Type.LONG_TYPE,    Opcodes.LALOAD);
        OpcodeUtil.addAARInsn(Types.FLOAT_ARRAY_TYPE,     Type.INT_TYPE, Type.FLOAT_TYPE,   Opcodes.FALOAD);
        OpcodeUtil.addAARInsn(Types.DOUBLE_ARRAY_TYPE,    Type.INT_TYPE, Type.DOUBLE_TYPE,  Opcodes.DALOAD);
        OpcodeUtil.addAARInsn(Types.REFERENCE_ARRAY_TYPE, Type.INT_TYPE, Types.OBJECT_TYPE, Opcodes.AALOAD);
        OpcodeUtil.addAARInsn(Types.BYTE_ARRAY_TYPE,      Type.INT_TYPE, Type.BYTE_TYPE,    Opcodes.BALOAD);
        OpcodeUtil.addAARInsn(Types.CHAR_ARRAY_TYPE,      Type.INT_TYPE, Type.CHAR_TYPE,    Opcodes.CALOAD);
        OpcodeUtil.addAARInsn(Types.SHORT_ARRAY_TYPE,     Type.INT_TYPE, Type.SHORT_TYPE,   Opcodes.SALOAD);

        OpcodeUtil.addAAAInsn(Types.INT_ARRAY_TYPE,       Type.INT_TYPE, Type.INT_TYPE,     Opcodes.IASTORE);
        OpcodeUtil.addAAAInsn(Types.LONG_ARRAY_TYPE,      Type.INT_TYPE, Type.LONG_TYPE,    Opcodes.LASTORE);
        OpcodeUtil.addAAAInsn(Types.FLOAT_ARRAY_TYPE,     Type.INT_TYPE, Type.LONG_TYPE,    Opcodes.FASTORE);
        OpcodeUtil.addAAAInsn(Types.DOUBLE_ARRAY_TYPE,    Type.INT_TYPE, Type.DOUBLE_TYPE,  Opcodes.DASTORE);
        OpcodeUtil.addAAAInsn(Types.REFERENCE_ARRAY_TYPE, Type.INT_TYPE, Types.OBJECT_TYPE, Opcodes.AASTORE);
        OpcodeUtil.addAAAInsn(Types.BYTE_ARRAY_TYPE,      Type.INT_TYPE, Type.BYTE_TYPE,    Opcodes.BASTORE);
        OpcodeUtil.addAAAInsn(Types.CHAR_ARRAY_TYPE,      Type.INT_TYPE, Type.CHAR_TYPE,    Opcodes.CASTORE);
        OpcodeUtil.addAAAInsn(Types.SHORT_ARRAY_TYPE,     Type.INT_TYPE, Type.SHORT_TYPE,   Opcodes.SASTORE);

        // SUPPRESS CHECKSTYLE LineLength:4
        OpcodeUtil.addAARInsn(Type.INT_TYPE,    Type.INT_TYPE,    Type.INT_TYPE,    Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR);
        OpcodeUtil.addAARInsn(Type.LONG_TYPE,   Type.LONG_TYPE,   Type.LONG_TYPE,   Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR);
        OpcodeUtil.addAARInsn(Type.FLOAT_TYPE,  Type.FLOAT_TYPE,  Type.FLOAT_TYPE,  Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM);
        OpcodeUtil.addAARInsn(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM);

        OpcodeUtil.addAARInsn(Type.LONG_TYPE,   Type.LONG_TYPE,   Type.INT_TYPE, Opcodes.LCMP);
        OpcodeUtil.addAARInsn(Type.FLOAT_TYPE,  Type.FLOAT_TYPE,  Type.INT_TYPE, Opcodes.FCMPG, Opcodes.FCMPL);
        OpcodeUtil.addAARInsn(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.INT_TYPE, Opcodes.DCMPG, Opcodes.DCMPL);

        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.INT_TYPE,     Opcodes.INEG);
        OpcodeUtil.addARInsn(Type.LONG_TYPE,   Type.LONG_TYPE,    Opcodes.LNEG);
        OpcodeUtil.addARInsn(Type.FLOAT_TYPE,  Type.FLOAT_TYPE,   Opcodes.FNEG);
        OpcodeUtil.addARInsn(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE,  Opcodes.DNEG);

        OpcodeUtil.addAARInsn(Type.INT_TYPE,  Type.INT_TYPE, Type.INT_TYPE,  Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR);
        OpcodeUtil.addAARInsn(Type.LONG_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR);

        OpcodeUtil.addAAInsn(Type.INT_TYPE,     Type.INT_TYPE,     Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE); // SUPPRESS CHECKSTYLE LineLength
        OpcodeUtil.addAAInsn(Types.OBJECT_TYPE, Types.OBJECT_TYPE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE);

        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.LONG_TYPE,   Opcodes.I2L);
        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.FLOAT_TYPE,  Opcodes.I2F);
        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.DOUBLE_TYPE, Opcodes.I2D);
        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.BYTE_TYPE,   Opcodes.I2B);
        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.CHAR_TYPE,   Opcodes.I2C);
        OpcodeUtil.addARInsn(Type.INT_TYPE,    Type.SHORT_TYPE,  Opcodes.I2S);
        OpcodeUtil.addARInsn(Type.LONG_TYPE,   Type.INT_TYPE,    Opcodes.L2I);
        OpcodeUtil.addARInsn(Type.LONG_TYPE,   Type.FLOAT_TYPE,  Opcodes.L2F);
        OpcodeUtil.addARInsn(Type.LONG_TYPE,   Type.DOUBLE_TYPE, Opcodes.L2D);
        OpcodeUtil.addARInsn(Type.FLOAT_TYPE,  Type.INT_TYPE,    Opcodes.F2I);
        OpcodeUtil.addARInsn(Type.FLOAT_TYPE,  Type.LONG_TYPE,   Opcodes.F2L);
        OpcodeUtil.addARInsn(Type.FLOAT_TYPE,  Type.DOUBLE_TYPE, Opcodes.F2D);
        OpcodeUtil.addARInsn(Type.DOUBLE_TYPE, Type.INT_TYPE,    Opcodes.D2I);
        OpcodeUtil.addARInsn(Type.DOUBLE_TYPE, Type.LONG_TYPE,   Opcodes.D2L);
        OpcodeUtil.addARInsn(Type.DOUBLE_TYPE, Type.FLOAT_TYPE,  Opcodes.D2F);

        OpcodeUtil.addAInsn(Type.INT_TYPE, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH);

        OpcodeUtil.addAInsn(Type.INT_TYPE,     Opcodes.IRETURN);
        OpcodeUtil.addAInsn(Type.LONG_TYPE,    Opcodes.LRETURN);
        OpcodeUtil.addAInsn(Type.FLOAT_TYPE,   Opcodes.FRETURN);
        OpcodeUtil.addAInsn(Type.DOUBLE_TYPE,  Opcodes.DRETURN);
        OpcodeUtil.addAInsn(Types.OBJECT_TYPE, Opcodes.ARETURN);
        OpcodeUtil.addInsn(Opcodes.RETURN);
    }
    private static void
    addInsn(int... opcodes) {
        OpcodeUtil.addInsns(new InsnDescription(null, null), opcodes);
    }
    private static void
    addAInsn(Type argumentOperandType, int... opcodes) {
        OpcodeUtil.addInsns(new InsnDescription(new Type[] { argumentOperandType }, null), opcodes);
    }
    private static void
    // SUPPRESS CHECKSTYLE AbbreviationAsWordInName
    addARInsn(Type argumentOperandType, Type resultOperandType, int... opcodes) {
        OpcodeUtil.addInsns(new InsnDescription(new Type[] { argumentOperandType }, resultOperandType), opcodes);
    }
    private static void
    // SUPPRESS CHECKSTYLE AbbreviationAsWordInName
    addAAInsn(Type argumentOperandType1, Type argumentOperandType2, int... opcodes) {
        OpcodeUtil.addInsns(
            new InsnDescription(new Type[] { argumentOperandType1, argumentOperandType2 }, null),
            opcodes
        );
    }
    private static void
    // SUPPRESS CHECKSTYLE AbbreviationAsWordInName
    addAARInsn(Type argumentOperandType1, Type argumentOperandType2, Type resultOperandType, int... opcodes) {
        OpcodeUtil.addInsns(
            new InsnDescription(new Type[] { argumentOperandType1, argumentOperandType2 }, resultOperandType),
            opcodes
        );
    }
    private static void
    // SUPPRESS CHECKSTYLE AbbreviationAsWordInName
    addAAAInsn(Type argumentOperandType1, Type argumentOperandType2, Type argumentOperandType3, int... opcodes) {
        OpcodeUtil.addInsns(
            new InsnDescription(new Type[] { argumentOperandType1, argumentOperandType2, argumentOperandType3 }, null),
            opcodes
        );
    }
    private static void
    addRInsn(Type resultOperandType, int... opcodes) {
        OpcodeUtil.addInsns(new InsnDescription(null, resultOperandType), opcodes);
    }
    private static void
    addInsns(InsnDescription insnDescription, int... opcodes) {
        for (int opcode : opcodes) {
            assert OpcodeUtil.INSN_DESCRIPTIONS[opcode] == null;
            OpcodeUtil.INSN_DESCRIPTIONS[opcode] = insnDescription;
        }
    }

    /**
     * @return Metainformation about the given {@code opcode}
     */
    public static InsnDescription
    getInsnDescription(int opcode) {
        return OpcodeUtil.INSN_DESCRIPTIONS[opcode];
    }
}
