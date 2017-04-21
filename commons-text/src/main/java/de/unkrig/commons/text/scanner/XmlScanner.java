
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.commons.text.scanner;

/**
 * A scanner for <a href="https://www.w3.org/TR/REC-xml/">XML</a>.
 */
public final
class XmlScanner {

    private XmlScanner() {}

    // PUBLIC INTERFACE

    public
    enum TokenType {

        /**
         * {@code XMLDecl      ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'}<br />
         * {@code VersionInfo  ::= S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')}<br />
         * {@code Eq           ::= S? '=' S?}<br />
         * {@code VersionNum   ::= '1.' [0-9]+}<br />
         * {@code EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' | "'" EncName "'" )}<br />
         * {@code EncName      ::= [A-Za-z] ([A-Za-z0-9._] | '-')*}<br>
         * {@code SDDecl       ::= S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))}
         */
        XML_DECLARATION,

        /** {@code Comment ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->' */
        COMMENT,

        /**
         * {@code PI       ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>' }<br />
         * {@code PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))}
         */
        PROCESSING_INSTRUCTION,

        /**
         * {@code doctypedecl   ::= '<!DOCTYPE' S Name (S ExternalID)? S? ('[' intSubset ']' S?)? '>'}<br />
         * {@code ExternalID    ::= 'SYSTEM' S SystemLiteral | 'PUBLIC' S PubidLiteral S SystemLiteral}<br />
         * {@code SystemLiteral ::= ('"' [^"]* '"') | ("'" [^']* "'")}<br />
         * {@code PubidLiteral  ::= '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"}<br />
         * {@code PubidChar     ::= #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]}<br />
         * {@code intSubset     ::= (markupdecl | DeclSep)*}<br />
         * {@code markupdecl    ::= elementdecl | AttlistDecl | EntityDecl | NotationDecl | PI | Comment}<br />
         * {@code elementdecl   ::= '<!ELEMENT' S Name S contentspec S? '>'}<br />
         * {@code contentspec   ::= 'EMPTY' | 'ANY' | Mixed | children}<br />
         * {@code Mixed         ::= '(' S? '#PCDATA' (S? '|' S? Name)* S? ')*' | '(' S? '#PCDATA' S? ')'}<br />
         * {@code children      ::= (choice | seq) ('?' | '*' | '+')?}<br />
         * {@code cp            ::= (Name | choice | seq) ('?' | '*' | '+')?}<br />
         * {@code choice        ::= '(' S? cp ( S? '|' S? cp )+ S? ')'}<br />
         * {@code seq           ::= '(' S? cp ( S? ',' S? cp )* S? ')'}
         */
        DOCUMENT_TYPE_DECLARATION,

        /** {@code EmptyElemTag ::= '<' Name (S Attribute)* S? '/>'} */
        EMPTY_ELEMENT_TAG,

        /**
         * {@code STag      ::= '<' Name (S Attribute)* S? '>'}<br />
         * {@code Attribute ::= Name Eq AttValue}<br />
         * {@code AttValue  ::= '"' ([^<&"] | Reference)* '"' | "'" ([^<&'] | Reference)* "'"}
         */
        START_TAG,

        /** {@code ETag ::= '</' Name S? '>'} */
        END_TAG,

        /** {@code CharData ::= [^<&]* - ([^<&]* ']]>' [^<&]*)} */
        CHAR_DATA,

        /** {@code EntityRef ::= '&' Name ';'} */
        ENTITY_REFERENCE,

        /** {@code CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'} */
        CHARACTER_REFERENCE,

        /**
         * {@code CDSect  ::= CDStart CData CDEnd}<br />
         * {@code CDStart ::= '<![CDATA['}<br />
         * {@code CData   ::= (Char* - (Char* ']]>' Char*))}<br />
         * {@code CDEnd   ::= ']]>'}
         */
        CDATA_SECTION,
    }

    public static StringScanner<TokenType>
    stringScanner() {
        StatelessScanner<TokenType> scanner = new StatelessScanner<TokenType>();

        String s                  = "(?:[ \\t\\r\\n]+)";
        String eq                 = "(?:" + s + "?=" + s + "?)";
        String nameStartChar      = "[:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD]"; // SUPPRESS CHECKSTYLE LineLength
        String nameChar           = "(?:" + nameStartChar + "|[\\-.0-9\u00B7\u0300-\u036F\u203F-\u2040])";
        String name               = "(?:" + nameStartChar + nameChar + "*)";
        String entityReference    = "(?:&" + name + ";)";
        String characterReference = "(?:&#[0-9]+|&#x[0-9a-zA-Z]+;)";
        String reference          = "(?:" + entityReference + "|" + characterReference + ")";
        String attributeValue     = "(?:'(?:[^<&\"]|" + reference + ")*'|\"(?:[^<&\"]|" + reference + ")*\")";
        String attribute          = "(?:" + name + eq + attributeValue + ")";

        scanner.addRule("<!--.*?-->", TokenType.COMMENT);

        // The "<?xml ..." rule must appear BEFORE the "<?name ..." rule.
        scanner.addRule((
            ""
            + "<\\?xml"
            + "(?:@S@version@Eq@"    + XmlScanner.quoted("1\\.[0-9]+")                + ")"
            + "(?:@S@encoding@Eq@"   + XmlScanner.quoted("[A-Za-z][A-Za-z0-9._\\-]*") + ")?"
            + "(?:@S@standalone@Eq@" + XmlScanner.quoted("yes|no")                    + ")?"
            + "\\?>"
        ).replace("@S@", s).replace("@Eq@", eq), TokenType.XML_DECLARATION);
        scanner.addRule((
            ""
            + "<\\?@Name@"
            + "(?:@S@.*?)?"
            + "\\?>"
        ).replace("@Name@", name).replace("@S@", s), TokenType.PROCESSING_INSTRUCTION);

        scanner.addRule("<!DOCTYPE.*?>", TokenType.DOCUMENT_TYPE_DECLARATION);

        scanner.addRule((
            ""
            + "<@Name@"
            + "(?:@S@@Attribute@)*"
            + "@S@?"
            + "/>"
        ).replace("@Name@", name).replace("@S@", s).replace("@Attribute@", attribute), TokenType.EMPTY_ELEMENT_TAG);

        scanner.addRule((
            ""
            + "<@Name@"
            + "(?:@S@@Attribute@)*"
            + "@S@?"
            + ">"
        ).replace("@Name@", name).replace("@S@", s).replace("@Attribute@", attribute), TokenType.START_TAG);

        scanner.addRule((
            ""
            + "</"
            + "@Name@"
            + "@S@?"
            + ">"
        ).replace("@Name@", name).replace("@S@", s), TokenType.END_TAG);

        scanner.addRule("(?:[^<&](?!-->))+", TokenType.CHAR_DATA);

        scanner.addRule(entityReference, TokenType.ENTITY_REFERENCE);

        scanner.addRule(characterReference, TokenType.CHARACTER_REFERENCE);

        scanner.addRule("(?:<!\\[CDATA\\[.*?]]>)", TokenType.CDATA_SECTION);

        return scanner;
    }

    /**
     * Must only be used for <var>subpatterns</var> that consume neither an apostrophe nor a quote, e.g. {@code
     * "1\\.[0-9]+"} is ok, but {@code ".*"} is not.
     */
    private static String
    quoted(String subpattern) {
        return (
            ""
            + "(?:"
            +   "'(?:" + subpattern + ")*'"
            +   "|"
            +   "\"(?:" + subpattern + ")*\""
            + ")"
        );
    }
}
