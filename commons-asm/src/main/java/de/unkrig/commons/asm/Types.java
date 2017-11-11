
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

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Type;

/**
 * Constants and helper methods for ASM's {@link Type}.
 */
public final
class Types {

    private
    Types() {}

    // SUPPRESS CHECKSTYLE JavadocVariable:13
    public static final Type OBJECT_TYPE          = Type.getObjectType("java/lang/Object");
    public static final Type STRING_TYPE          = Type.getObjectType("java/lang/String");
    public static final Type CHAR_SEQUENCE_TYPE   = Type.getObjectType("java/lang/CharSequence");
    public static final Type STRING_BUFFER_TYPE   = Type.getObjectType("java/lang/StringBuffer");
    public static final Type STRING_BUILDER_TYPE  = Type.getObjectType("java/lang/StringBuilder");
    public static final Type BYTE_ARRAY_TYPE      = Type.getType("[B");
    public static final Type SHORT_ARRAY_TYPE     = Type.getType("[S");
    public static final Type INT_ARRAY_TYPE       = Type.getType("[I");
    public static final Type LONG_ARRAY_TYPE      = Type.getType("[J");
    public static final Type FLOAT_ARRAY_TYPE     = Type.getType("[F");
    public static final Type DOUBLE_ARRAY_TYPE    = Type.getType("[D");
    public static final Type CHAR_ARRAY_TYPE      = Type.getType("[C");
    public static final Type REFERENCE_ARRAY_TYPE = Type.getType("[Ljava/lang/Object;");

    /**
     * Guess the operand/result type from the opcode. Notice that this is not always reliable
     */
    public static Type
    fromOpcode(int opcode) {
        switch (opcode) {

        case IALOAD: case IASTORE:
            return Types.INT_ARRAY_TYPE;

        case LALOAD: case LASTORE:
            return Types.LONG_ARRAY_TYPE;

        case FALOAD: case FASTORE:
            return Types.FLOAT_ARRAY_TYPE;

        case DALOAD: case DASTORE:
            return Types.DOUBLE_ARRAY_TYPE;

        case AALOAD: case AASTORE:
            return Types.REFERENCE_ARRAY_TYPE;

        case BALOAD: case BASTORE:
            return Types.BYTE_ARRAY_TYPE;

        case CALOAD: case CASTORE:
            return Types.CHAR_ARRAY_TYPE;

        case SALOAD: case SASTORE:
            return Types.SHORT_ARRAY_TYPE;

        case ICONST_M1: case ICONST_0: case ICONST_1: case ICONST_2: case ICONST_3: case ICONST_4: case ICONST_5:
        case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE: case IF_ICMPGT: case IF_ICMPLE:
        case ILOAD: case ISTORE:
        case IADD: case IAND: case IDIV: case IMUL: case INEG: case IOR: case IREM: case ISUB: case IXOR:
        case ISHL: case ISHR: case IUSHR:
        case IRETURN:
        case BIPUSH: case SIPUSH:
        case I2L: case I2F: case I2D: case I2B: case I2C: case I2S:
        case IINC:
            return Type.INT_TYPE;

        case LCONST_0: case LCONST_1:
        case LADD: case LAND: case LDIV: case LMUL: case LNEG: case LOR: case LREM: case LSUB: case LXOR:
        case LCMP:
        case LLOAD: case LSTORE:
        case LRETURN:
        case LSHL: case LSHR: case LUSHR:
        case L2I: case L2F: case L2D:
            return Type.LONG_TYPE;

        case FADD:
        case FCMPG:
        case FCMPL:
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
        case FDIV:
        case FLOAD:
        case FMUL:
        case FNEG:
        case FREM:
        case FRETURN:
        case FSTORE:
        case FSUB:
        case F2I:
        case F2L:
        case F2D:
            return Type.FLOAT_TYPE;

        case DADD:
        case DCMPG:
        case DCMPL:
        case DCONST_0:
        case DCONST_1:
        case DDIV:
        case DLOAD:
        case DMUL:
        case DNEG:
        case DREM:
        case DRETURN:
        case DSTORE:
        case DSUB:
        case D2I:
        case D2L:
        case D2F:
            return Type.DOUBLE_TYPE;

        case ALOAD:
        case ARETURN:
        case ASTORE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
            return Types.OBJECT_TYPE;

        default:
            throw new IllegalArgumentException(Integer.toString(opcode));
        }
    }
}
