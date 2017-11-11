
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test.json;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.CountingReader;
import de.unkrig.commons.text.json.Json;
import de.unkrig.commons.text.json.JsonParser;
import de.unkrig.commons.text.json.JsonUnparseVisitor;
import de.unkrig.commons.text.parser.ParseException;

public
class JsonTest {

    @Test public void
    testParseUnparse() throws IOException, ParseException {
        String text = (
            ""
            + "{\r\n"
            + "  \"member1\" : \"hallo\",\r\n"
            + "  \"member2\" : \"welt\",\r\n"
            + "  \"member3\" : [\r\n"
            + "    1,\r\n"
            + "    \"apple\",\r\n"
            + "    \"bean\"\r\n"
            + "  ],\r\n"
            + "  \"member4\" : 1.23E40\r\n"
            + "}"
        );
        CountingReader cr = new CountingReader(new StringReader(text));

        Json.Value value;
        try {
            value = new JsonParser(cr).parseValue();
        } catch (ParseException pe) {
            throw new ParseException((
                "Text '" + text + "', "
                + "line " + cr.lineNumber() + ", "
                + "column " + cr.columnNumber() + ": "
                + pe.getMessage()
            ), pe);
        }
        StringWriter sw = new StringWriter();
        value.accept(new JsonUnparseVisitor(new PrintWriter(sw), ""));
        Assert.assertEquals(text, sw.toString());
    }

    @Test public void
    parse() throws Exception {
        // SUPPRESS CHECKSTYLE LineLength:35
        String s = (
            ""
            + "{\n"
            + "    \"data\" : \n"
            + "    [\n"
            + "    {\n"
            + "        \"id\" : \"fa998617-6a9f-4e19-8e98-3e34fedb7a10\",\n"
            + "        \"title\" : \"Run DalContentStores_vb_inbound.js\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"ruleType\" : [\"inbound\"],    \n"
            + "        \"disabled\" : false,\n"
            + "        \"owningNode\" :\n"
            + "        {\n"
            + "            \"nodeRef\" : \"workspace://SpacesStore/201576d3-f39e-cebb-2563-0a310833ad13\",\n"
            + "            \"name\" : \"BMS\"\n"
            + "        },    \n"
            + "        \"url\" : \"\\/api\\/node\\/workspace\\/SpacesStore\\/201576d3-f39e-cebb-2563-0a310833ad13\\/ruleset\\/rules\\/fa998617-6a9f-4e19-8e98-3e34fedb7a10\"\n"
            + "    }\n"
            + "        , \n"
            + "    {\n"
            + "        \"id\" : \"c0681dca-717a-4a5f-a952-e3ad21b0eb9c\",\n"
            + "        \"title\" : \"Run DalContentStores_vb_outbound.js\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"ruleType\" : [\"outbound\"],    \n"
            + "        \"disabled\" : false,\n"
            + "        \"owningNode\" :\n"
            + "        {\n"
            + "            \"nodeRef\" : \"workspace://SpacesStore/201576d3-f39e-cebb-2563-0a310833ad13\",\n"
            + "            \"name\" : \"BMS\"\n"
            + "        },    \n"
            + "        \"url\" : \"\\/api\\/node\\/workspace\\/SpacesStore\\/201576d3-f39e-cebb-2563-0a310833ad13\\/ruleset\\/rules\\/c0681dca-717a-4a5f-a952-e3ad21b0eb9c\"\n"
            + "    }\n"
            + "         \n"
            + "    ]\n"
            + "}\n"
        );
        new JsonParser(new StringReader(s)).parseValue();
    }
}
