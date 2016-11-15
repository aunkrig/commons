
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

import java.io.PrintWriter;

import de.unkrig.commons.text.json.Json.Member;
import de.unkrig.commons.text.json.Json.Value;

/**
 * A {@link Json.ValueVisitor} which prints a JSON value nicely formatted to a {@link PrintWriter}.
 */
public
class JsonUnparseVisitor implements Json.ValueVisitor {

    private static final String INDENT = "  ";

    private final PrintWriter pw;
    private final String      prefix;

    public
    JsonUnparseVisitor(PrintWriter pw, String prefix) {
        this.pw     = pw;
        this.prefix = prefix;
    }

    @Override public void
    visit(Json.StrinG string) {
        this.pw.write('"');
        for (int i = 0; i < string.text.length(); i++) {
            char c = string.text.charAt(i);

            {
                int idx = "\"\\/\b\f\n\r\t".indexOf(c);
                if (idx != -1) {
                    this.pw.write('\\');
                    this.pw.write("\"\\/bfnrt".charAt(idx));
                    continue;
                }
            }

            if (Character.isISOControl(c)) {
                this.pw.write('\\');
                this.pw.write('u');
                this.pw.write(Character.forDigit(c >> 12, 10));
                this.pw.write(Character.forDigit((c >> 8) & 0xf, 10));
                this.pw.write(Character.forDigit((c >> 4) & 0xf, 10));
                this.pw.write(Character.forDigit(c  & 0xf, 10));
                continue;
            }

            this.pw.write(c);
        }
        this.pw.write('"');
    }

    @Override public void
    visit(Json.NumbeR number) {
        this.pw.print(number.value);
    }

    @Override public void
    visit(Json.ObjecT object) {

        if (object.members.isEmpty()) {
            this.pw.print("{}");
            return;
        }

        String                   prefix2  = this.prefix + JsonUnparseVisitor.INDENT;
        final JsonUnparseVisitor visitor2 = new JsonUnparseVisitor(this.pw, prefix2);

        this.pw.println('{');
        boolean first = true;
        for (Member member : object.members) {
            if (first) {
                first = false;
            } else {
                this.pw.println(',');
            }
            this.pw.print(prefix2);
            this.visit(member.name);
            this.pw.print(" : ");
            member.value.accept(visitor2);
        }
        this.pw.println();
        this.pw.print(this.prefix + '}');
    }

    @Override public void
    visit(Json.Array array) {

        if (array.elements.isEmpty()) {
            this.pw.print("[]");
            return;
        }

        String                   prefix2  = this.prefix + JsonUnparseVisitor.INDENT;
        final JsonUnparseVisitor visitor2 = new JsonUnparseVisitor(this.pw, prefix2);

        this.pw.println('[');
        boolean first = true;
        for (Value value : array.elements) {
            if (first) {
                first = false;
            } else {
                this.pw.println(',');
            }
            this.pw.print(prefix2);
            value.accept(visitor2);
        }
        this.pw.println();
        this.pw.print(this.prefix + ']');
    }

    @Override public void
    visit(Json.True truE) { this.pw.print("true"); }

    @Override public void
    visit(Json.False falsE) { this.pw.print("false"); }

    @Override public void
    visit(Json.Null nulL) { this.pw.print("null"); }
}
