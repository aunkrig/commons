
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.commons.text.expression;

import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * An implementation of {@link Expression} which implements the "{@code evaluateTo*()}" convenience methods.
 */
public abstract
class AbstractExpression implements Expression {

    /**
     * @see ExpressionEvaluator#to(Object, Class)
     */
    @Override @Nullable public final Object
    evaluateTo(Mapping<String, ?> variables, Class<?> targetType) throws EvaluationException {
        return ExpressionEvaluator.to(this.evaluate(variables), targetType);
    }

    /**
     * @see ExpressionEvaluator#toPrimitive(Object, Class)
     */
    @Override public final Object
    evaluateToPrimitive(Mapping<String, ?> variables, Class<?> targetType) throws EvaluationException {
        return ExpressionEvaluator.toPrimitive(this.evaluate(variables), targetType);
    }

    /**
     * @see ExpressionEvaluator#toBoolean(Object)
     */
    @Override public final boolean
    evaluateToBoolean(Mapping<String, ?> variables) throws EvaluationException {
        return ExpressionEvaluator.toBoolean(this.evaluate(variables));
    }
}
