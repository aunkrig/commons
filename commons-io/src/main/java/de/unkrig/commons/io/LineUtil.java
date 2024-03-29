
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2012, Arno Unkrig
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

package de.unkrig.commons.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Various utility methods for processing "lines", i.e. sequences of strings (which typically don't contain line
 * breaks).
 */
public final
class LineUtil {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private
    LineUtil() {}

    /**
     * Produces lines from a UTF-8-encoded {@link InputStream}.
     */
    public static ProducerWhichThrows<String, IOException>
    lineProducerUtf8(InputStream in) {
    	return LineUtil.lineProducer(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Produces lines from a ISO 8859-1-encoded {@link InputStream}.
     */
    public static ProducerWhichThrows<String, IOException>
    lineProducerISO8859_1(InputStream in) { // SUPPRESS CHECKSTYLE MethodName|AbbreviationAsWord
        return LineUtil.lineProducer(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
    }

    /**
     * Produces lines from a {@link Reader}.
     */
    public static ProducerWhichThrows<String, IOException>
    lineProducer(Reader r) {

        final BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);

        return new ProducerWhichThrows<String, IOException>() {

            @Override public String
            produce() throws IOException {
                return br.readLine();
            }
        };
    }

    /**
     * Produces lines from a string by splitting that at line breaks.
     */
    public static Producer<String>
    lineProducer(final String text) {

        return new Producer<String>() {

            final int len = text.length();
            int       offset;

            @Override @Nullable public String
            produce() {

                if (this.offset == this.len) return null;

                for (int o = this.offset;;) {

                    char c = text.charAt(o);

                    if (c == '\r') {
                        final String result = text.substring(this.offset, o);
                        o++;
                        if (o < this.len && text.charAt(o) == '\n') o++;
                        this.offset = o;
                        return result;
                    }

                    if (c == '\n') {
                        String result = text.substring(this.offset, o);
                        this.offset = o + 1;
                        return result;
                    }

                    o++;

                    if (o == this.len) {

                        // Final line w/o trailing line separator.
                        String result = text.substring(this.offset);
                        this.offset = o;
                        return result;
                    }
                }
            }
        };
    }

    /**
     * Produces lines from a CharSequence by splitting that at line breaks.
     */
    public static Producer<CharSequence>
    lineProducer(final CharSequence text) {

        return new Producer<CharSequence>() {

            final int len = text.length();
            int       offset;

            @Override @Nullable public CharSequence
            produce() {

                if (this.offset == this.len) return null;

                for (int o = this.offset;;) {

                    char c = text.charAt(o);

                    if (c == '\r') {
                        final CharSequence result = text.subSequence(this.offset, o);
                        o++;
                        if (o < this.len && text.charAt(o) == '\n') o++;
                        this.offset = o;
                        return result;
                    }

                    if (c == '\n') {
                        CharSequence result = text.subSequence(this.offset, o);
                        this.offset = o + 1;
                        return result;
                    }

                    o++;

                    if (o == this.len) {

                        // Final line w/o trailing line separator.
                        CharSequence result = text.subSequence(this.offset, o);
                        this.offset = o;
                        return result;
                    }
                }
            }
        };
    }

    /**
     * Similar to {@link java.io.BufferedReader#readLine()}, except that
     * <ul>
     *   <li>The produced strings <i>include</i> the line separator</li>
     *   <li>
     *     Not only CR, LF and CRLF are recognized as line terminators, but also some other special characters, as
     *     described <a href="http://download.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt">here</a>
     *   </li>
     * </ul>
     *
     * @param r The source of characters
     * @return  Produces one string for each parsed line and {@code null} at end-of-input
     */
    public static ProducerWhichThrows<String, IOException>
    readLineWithSeparator(final Reader r) {

        return new ProducerWhichThrows<String, IOException>() {

            private int lookahead = -2;

            @Override @Nullable public String
            produce() throws IOException {
                int c;
                if (this.lookahead == -2) {
                    c = r.read();
                } else {
                    c              = this.lookahead;
                    this.lookahead = -2;
                }
                if (c == -1) return null;
                StringBuilder sb = new StringBuilder();
                for (;;) {
                    sb.append((char) c);
                    if (
                        c == '\n'        // Newline (line feed) character
                        || c == '\u0085' // Next-line character
                        || c == '\u2028' // Line-separator character
                        || c == '\u2029' // Paragraph-separator character
                    ) return sb.toString();
                    if (c == '\r') {
                        c = r.read();
                        if (c == '\n') {
                            // A carriage-return character followed immediately by a newline character
                            return sb.append('\n').toString();
                        }
                        // A standalone carriage-return character
                        this.lookahead = c;
                        return sb.toString();
                    }
                    c = r.read();
                    if (c == -1) return sb.toString();
                }
            }
        };
    }

    /**
     * Writes the consumed strings as lines to the UTF-8-encoded {@link OutputStream}.
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumerUtf8(OutputStream out) { // SUPPRESS CHECKSTYLE MethodName|AbbreviationAsWord
    	return LineUtil.lineConsumer(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    /**
     * Writes the consumed strings as lines to the ISO 8859-1-encoded {@link OutputStream}.
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumerISO8859_1(OutputStream out) { // SUPPRESS CHECKSTYLE MethodName|AbbreviationAsWord
        return LineUtil.lineConsumer(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1));
    }

    /**
     * Writes the consumed strings as lines to the given {@link Writer}.
     */
    public static ConsumerWhichThrows<String, IOException>
    lineConsumer(final Writer w) {

        return new ConsumerWhichThrows<String, IOException>() {

            @Override public void
            consume(String line) throws IOException {
                w.write(line + LineUtil.LINE_SEPARATOR);
                w.flush();
            }
        };
    }

    /**
     * Prints the consumed strings as lines to the given {@link PrintWriter}.
     */
    public static <T extends CharSequence> Consumer<T>
    lineConsumer(final PrintWriter pw) {

        return new Consumer<T>() { @Override public void consume(T line) { pw.println(line); } };
    }

    /**
     * Prints the consumed strings as lines to the given {@link PrintStream}.
     */
    public static Consumer<String>
    lineConsumer(final PrintStream ps) {

        return new Consumer<String>() {
            @Override public void consume(String line) { ps.println(line); }
        };
    }

    /**
     * Reads lines from the given reader until end-of-input.
     */
    public static List<String>
    readAllLines(Reader r, boolean closeReader) throws IOException {

        try {

            BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);

            final List<String> result = new ArrayList<String>();
            for (;;) {
                String line = br.readLine();
                if (line == null) break;

                result.add(line);
            }

            if (closeReader) r.close();

            return result;
        } finally {
            if (closeReader) {
                try { r.close(); } catch (Exception e) {}
            }
        }
    }

    /**
     * Keeps track of "line numbers" and "column numbers" while it {@link #consume(char) consumes} a stream of {@code
     * char}s.
     */
    public
    interface LineAndColumnTracker {

        /**
         * The TAB width that takes effect initially.
         */
        int DEFAULT_TAB_WIDTH = 8;

        /**
         * Reconfigures the TAB width. Value {@code 1} will treat TAB characters just like any other (non-line-break)
         * character. The initial TAB width is {@link #DEFAULT_TAB_WIDTH}.
         */
        void setTabWidth(int tabWidth);

        /**
         * Changes the tracker's state according to the given next character.
         */
        void consume(char c);

        // SUPPRESS CHECKSTYLE JavadocMethod:4
        int  getLineNumber();
        void setLineNumber(int lineNumber);
        int  getColumnNumber();
        void setColumnNumber(int columnNumber);

        /**
         * Resets the tracker's state (line and column number), but not its configuration (TAB width).
         */
        void reset();
    }

    /**
     * Creates a {@link LineAndColumnTracker} which implements line and column counting.
     * <p>
     *   Line breaks are identified as defined by the {@code \R} pattern of {@link Pattern}.
     * </p>
     * <p>
     *   Initially, line number and column number are both 1. Line breaks increment the line and reset the column to 1.
     *   TAB characters increase the column to the nearest multiple of the {@link LineAndColumnTracker#setTabWidth(int)
     *   TAB width}. All other characters increment the column.
     * </p>
     */
    public static LineAndColumnTracker
    lineAndColumnTracker() {

        return new LineAndColumnTracker() {

            // Configuration.
            private int tabWidth = LineAndColumnTracker.DEFAULT_TAB_WIDTH;

            // State.
            private int     line = 1, column = 1;
            private boolean crPending;

            @Override public void
            consume(char c) {

                if (this.crPending) {
                    this.crPending = false;
                    if (c == '\n') return; // Discard the '\n' after the '\r'.
                }

                switch (c) {
                case '\r':     // "carriage-return character"
                    this.line++;
                    this.column    = 1;
                    this.crPending = true;
                    break;
                case '\n':     // "new-line character"
                case '\u0085': // "next-line character"
                case '\u2028': // "line-separator character"
                case '\u2029': // "paragraph-separator character"
                    this.line++;
                    this.column = 1;
                    break;
                case '\t':     // "TAB character"
                    this.column = this.column - ((this.column - 1) % this.tabWidth) + this.tabWidth;
                    break;
                default:
                    this.column++;
                    break;
                }
            }

            @Override public int  getLineNumber()                   { return this.line;           }
            @Override public void setLineNumber(int lineNumber)     { this.line = lineNumber;     }
            @Override public int  getColumnNumber()                 { return this.column;         }
            @Override public void setColumnNumber(int columnNumber) { this.column = columnNumber; }

            @Override public void setTabWidth(int tabWidth) { this.tabWidth = tabWidth; }

            @Override public void
            reset() {
                this.line      = 1;
                this.column    = 1;
                this.crPending = false;
            }
        };
    }
}
