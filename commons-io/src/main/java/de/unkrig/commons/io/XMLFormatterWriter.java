
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

package de.unkrig.commons.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * This {@link FilterWriter} scans the character stream for tags and inserts "artificial" line breaks as follows:
 * <dl>
 *   <dt>{@code <a><b}
 *   <dd>Wrap between '>' and '<' and indent
 *   <dt>{@code </a><b}
 *   <dd>Wrap between '>' and '<'
 *   <dt>{@code <a/><b}
 *   <dd>Wrap between '>' and '<'
 *   <dt>{@code </a></b}
 *   <dd>Wrap between '>' and '<' and unindent
 *   <dt>{@code <a/></b}
 *   <dd>Wrap between '>' and '<' and unindent
 * </dl>
 */
@NotNullByDefault(false) public
class XMLFormatterWriter extends FilterWriter {

    private int state;
    private int indentation;

    /**
     * @see XMLFormatterWriter
     */
    public
    XMLFormatterWriter(Writer out) {
        super(out);
    }

    @Override public void
    write(int c) throws IOException {
        switch (this.state) {

        case 0:
            if (c == '<') this.state = 1;
            break;

        case 1: // <
            this.state = c == '/' ? 2 : 5;
            break;

        case 2: // </
            if (c == '>') this.state = 3;
            break;

        case 3: // </a>
            if (c == ' ') break; // Tolerate space between '</a>' and '<'.
            if (c == '<') {
                this.state = 4;
                return;
            }
            this.state = 0;
            return;

        case 4: // </a><
            this.out.write("\r\n");
            if (c == '/' && this.indentation > 0) this.indentation--;
            for (int i = 0; i < this.indentation; i++) this.out.write("  ");
            this.out.write('<');
            this.state = c == '/' ? 2 : 5;
            break;

        case 5: // <a
            if (c == '/') {
                this.state = 6;
            } else
            if (c == '>') {
                this.state = 9;
            }
            break;

        case 6: // <a/
            this.state = c == '>' ? 7 : 5;
            break;

        case 7: // <a/>
            if (c == ' ') break; // Tolerate space between '<a/>' and '<'.
            if (c == '<') {
                this.state = 8;
                return;
            }
            this.state = 0;
            break;

        case 8: // <a/><
            this.out.write("\r\n");
            if (c == '/' && this.indentation > 0) this.indentation--;
            for (int i = 0; i < this.indentation; i++) this.out.write("  ");
            this.out.write('<');
            this.state = c == '/' ? 2 : 5;
            break;

        case 9: // <a>
            if (c == ' ') break; // Tolerate space between '<a>' and '<'.
            if (c == '<') {
                this.state = 10;
                return;
            }
            this.state = 0;
            break;

        case 10: // <a><
            this.out.write("\r\n");
            if (c != '/') this.indentation++;
            for (int i = 0; i < this.indentation; i++) this.out.write("  ");
            this.out.write('<');
            this.state = c == '/' ? 2 : 5;
            break;
        }
        this.out.write(c);
    }

    @Override public void
    write(char[] cbuf, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) this.write(cbuf[i]);
    }

    @Override public void
    write(String str, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) this.write(str.charAt(i));
    }
}
