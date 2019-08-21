
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
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

package de.unkrig.commons.text.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * The JSON DOM as defined on <a href="http://www.json.org/">json.org</a>.
 */
public final
class Json {

    private Json() {}

    /**
     * Base for a JSON value.
     */
    public
    interface Value {

        /** Invokes the value type-specific '{@code visit...()}' method of {@link ValueVisitor}. */
        void accept(ValueVisitor visitor);
    }

    /**
     * Representation of a member of an {@link ObjecT}.
     */
    public static
    class Member {

        /** The name of the member. */
        public final StrinG name;

        /** The value of the member. */
        public final Value value;

        public
        Member(StrinG string, Value value) {
            this.name  = string;
            this.value = value;
        }

        @Override public int
        hashCode() { return this.name.hashCode() ^ this.value.hashCode(); }

        @Override public boolean
        equals(@Nullable Object obj) {
            if (!(obj instanceof Member)) return false;
            Member that = (Member) obj;
            return this.name.equals(that.name) && this.value.equals(that.value);
        }
    }

    /**
     * Representation of a JSON 'string'.
     * <p>
     * Notice that the last letter of the class name is capitalized to avoid confusion with {@link java.lang.String}.
     */
    public static
    class StrinG implements Value {

        /** The (decoded) text of the JSON string. */
        public final String text;

        public
        StrinG(String text) { this.text = text; }

        @Override public void
        accept(ValueVisitor visitor) { visitor.visit(this); }

        @Override public int
        hashCode() { return this.text.hashCode(); }

        @Override public boolean
        equals(@Nullable Object obj) { return obj instanceof StrinG && ((StrinG) obj).text.equals(this.text); }
    }

    /**
     * Representation of a JSON 'number'.
     * <p>
     * Notice that the last letter of the class name is capitalized to avoid confusion with {@link java.lang.Number}.
     */
    public static
    class NumbeR implements Value {

        /** The value of the JSON number; either a {@link Long} or a {@link Double}. */
        public final Number value;

        public
        NumbeR(String text) {
            if (text.indexOf('.') != -1 || text.indexOf('e') != -1 || text.indexOf('E') != -1) {
                this.value = Double.parseDouble(text);
            } else {
                this.value = Long.parseLong(text);
            }
        }

        @Override public void
        accept(ValueVisitor visitor) { visitor.visit(this); }

        @Override public int
        hashCode() { return this.value.hashCode(); }

        @Override public boolean
        equals(@Nullable Object obj) { return obj instanceof NumbeR && ((NumbeR) obj).value.equals(this.value); }
    }

    /**
     * Representation of a JSON 'object'.
     * <p>
     * Notice that the last letter of the class name is capitalized to avoid confusion with {@link java.lang.Object}.
     */
    public static
    class ObjecT implements Value {

        /** The (unmodifiable and constant) list of object members. */
        public final List<Member> members;

        public
        ObjecT(List<Member> stringValuePairs) {
            this.members = Collections.unmodifiableList(new ArrayList<Member>(stringValuePairs));
        }

        @Override public void
        accept(ValueVisitor visitor) { visitor.visit(this); }

        @Override public int
        hashCode() { return this.members.hashCode(); }

        @Override public boolean
        equals(@Nullable Object obj) { return obj instanceof ObjecT && ((ObjecT) obj).members.equals(this.members); }
    }

    /** Representation of a JSON 'array'. */
    public static
    class Array implements Value {

        /** The (unmodifiable and constant) list of array elements. */
        public final List<Value> elements;

        public
        Array(List<Value> values) { this.elements = Collections.unmodifiableList(new ArrayList<Value>(values)); }

        @Override public void
        accept(ValueVisitor visitor) { visitor.visit(this); }

        @Override public int
        hashCode() { return this.elements.hashCode(); }

        @Override public boolean
        equals(@Nullable Object obj) { return obj instanceof Array && ((Array) obj).elements.equals(this.elements); }
    }

    /** Representation of a JSON "{@code true}" literal. */
    public static
    class True implements Value {
        @Override public void    accept(ValueVisitor visitor) { visitor.visit(this);        }
        @Override public int     hashCode()                   { return 723688273;           }
        @Override public boolean equals(@Nullable Object obj) { return obj instanceof True; }
    }

    /** Representation of a JSON "{@code false}" literal. */
    public static
    class False implements Value {
        @Override public void    accept(ValueVisitor visitor) { visitor.visit(this);         }
        @Override public int     hashCode()                   { return 238545234;            }
        @Override public boolean equals(@Nullable Object obj) { return obj instanceof False; }
    }

    /** Representation of a JSON "{@code null}" literal. */
    public static
    class Null implements Value {
        @Override public void    accept(ValueVisitor visitor) { visitor.visit(this);        }
        @Override public int     hashCode()                   { return 61278954;            }
        @Override public boolean equals(@Nullable Object obj) { return obj instanceof Null; }
    }

    /** Interface for the implementation of the 'visitor' pattern for a JSON value. */
    public
    interface ValueVisitor {

        /** Invoked by {@link Json.StrinG#accept(Json.ValueVisitor)}. */
        void visit(StrinG string);

        /** Invoked by {@link Json.NumbeR#accept(Json.ValueVisitor)}. */
        void visit(NumbeR number);

        /** Invoked by {@link Json.ObjecT#accept(Json.ValueVisitor)}. */
        void visit(ObjecT object);

        /** Invoked by {@link Json.Array#accept(Json.ValueVisitor)}. */
        void visit(Array array);

        /** Invoked by {@link Json.True#accept(Json.ValueVisitor)}. */
        void visit(True truE);

        /** Invoked by {@link Json.False#accept(Json.ValueVisitor)}. */
        void visit(False falsE);

        /** Invoked by {@link Json.Null#accept(Json.ValueVisitor)}. */
        void visit(Null nulL);
    }
}
