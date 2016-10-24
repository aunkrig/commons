
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

package test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.XMLFormatterWriter;

//CHECKSTYLE JavadocMethod:OFF
//CHECKSTYLE JavadocType:OFF

public
class XMLFormatterWriterTest {

    @SuppressWarnings("resource") @Test public void
    test() throws IOException {
        StringWriter sw = new StringWriter();
        // SUPPRESS CHECKSTYLE LineLength
        new XMLFormatterWriter(sw).append("[17.02.12 13:06:39:171 CET] 000000a5 MFCImplementa E   CWSXM0205E: Es ist eine unerwartete Ausnahmebedingung in der Business-Logik aufgetreten.: CWSXM3300E: Das primitive Fehlerelement 'Fail2' (Komponente 'Vorgangsanlage/Vorgangsanlage', Modul 'PW2', Schnittstelle '{http://PW2/wsdl/Vorgangsanlage}Vorgangsanlage', Operation 'legeVorgaengeAn') hat eine FailFlowException ausgel�st. Vom Benutzer bereitgestellte Fehlernachricht: '17.02.12 13:06, 8B34F943-0135-4000-E000-0F300AFC4342, Fail2, PW2, <Se:smo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:Se=\"http://www.bim.com/websphere/sibx/smo/v6.0.1\" xmlns:se=\"wsdl.http://TK_SBI_lib/SbiService\" xmlns:vo=\"http://PW2/med/Vorgangsanlage\"><context><correlation xsi:type=\"vo:CorrelationContext\"><anliegen><sendungsId>SENDUNGS-ID 1</sendungsId><eingangskanal>Post</eingangskanal><prioritaet>3</prioritaet><anliegentyp>Storno</anliegentyp><prozesstemplate>P2_Vertragsbeendigung</prozesstemplate><dokumentIds>DOKUMENT-ID 1</dokumentIds><dokumentIds>DOKUMENT-ID 2</dokumentIds><fuehrendeDokumentId>DOKUMENT-ID 1</fuehrendeDokumentId><dokumente><dokumentId>DOKUMENT-ID 1</dokumentId><dokumentTimestamp></dokumentTimestamp><dokumentTyp>302</dokumentTyp><vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV1234</vsnr><vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">VM1</vmnr><ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA1</ebanr><erfassungszeitpunkt>2011-12-22T11:35:59.000Z</erfassungszeitpunkt><mandant>00001</mandant><ablageort>nscale</ablageort><produktgesellschaft>001</produktgesellschaft><kundenmerkmal>104</kundenmerkmal><produktgruppe>59</produktgruppe><berechtigungFuer>001</berechtigungFuer><berechtigungAufWas>002</berechtigungAufWas><berechtigungAufWen>003</berechtigungAufWen><korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal></dokumente><nachkorrekturKontext>NACHKORREKTUR-KONTEXT</nachkorrekturKontext></anliegen><anliegen><sendungsId>SENDUNGS-ID 1</sendungsId><eingangskanal>Post</eingangskanal><prioritaet>39</prioritaet><anliegentyp>KontoAEA</anliegentyp><prozesstemplate>P2_Vertragsaenderung</prozesstemplate><dokumentIds>DOKUMENT-ID 1</dokumentIds><dokumentIds>DOKUMENT-ID 2</dokumentIds><fuehrendeDokumentId>DOKUMENT-ID 2</fuehrendeDokumentId><dokumente><dokumentId>DOKUMENT-ID 2</dokumentId><dokumentTimestamp></dokumentTimestamp><dokumentTyp>309</dokumentTyp><vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV12345</vsnr><vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\"></vmnr><ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA2</ebanr><erfassungszeitpunkt>2011-12-22T11:35:59.001Z</erfassungszeitpunkt><mandant>00001</mandant><ablageort>nscale</ablageort><produktgesellschaft>001</produktgesellschaft><kundenmerkmal>101</kundenmerkmal><produktgruppe>60</produktgruppe><berechtigungFuer>004</berechtigungFuer><berechtigungAufWas>005</berechtigungAufWas><berechtigungAufWen>006</berechtigungAufWen><korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal></dokumente><dokumente><dokumentId>DOKUMENT-ID 3</dokumentId><dokumentTimestamp></dokumentTimestamp><dokumentTyp>233</dokumentTyp><vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV1234567</vsnr><vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\"></vmnr><ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA2</ebanr><erfassungszeitpunkt>2011-12-22T11:35:59.001Z</erfassungszeitpunkt><mandant>00001</mandant><ablageort>nscale</ablageort><produktgesellschaft>001</produktgesellschaft><kundenmerkmal>101</kundenmerkmal><produktgruppe></produktgruppe><berechtigungFuer>007</berechtigungFuer><berechtigungAufWas>008</berechtigungAufWas><berechtigungAufWen>009</berechtigungAufWen><korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal></dokumente><nachkorrekturKontext>NACHKORREKTUR-KONTEXT</nachkorrekturKontext></anliegen><eingangskanal>01</eingangskanal></correlation><failInfo><failureString>CWSXM0201E: Der Mediationsablauf f�r Komponente VorgangMediation in Modul TK_SBI hat eine Ausnahmebedingung zur�ckgegeben.: CWSXM3300E: Das primitive Fehlerelement 'Fail1' (Komponente 'VorgangMediation', Modul 'TK_SBI', Schnittstelle '{http://TK_SBI_lib/SbiService}Vorgang', Operation 'legeAn') hat eine FailFlowException ausgel�st. Vom Benutzer bereitgestellte Fehlernachricht: '17.02.12 13:06, 8B34F934-0135-4000-E000-0F300AFC4342, Fail1, TK_SBI, &lt;Se:smo xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xmlns:Se=&quot;http://www.bim.com/websphere/sibx/smo/v6.0.1&quot; xmlns:we=&quot;wsdl.http://webservices.SbClPtsWqs.kvb.de&quot; xmlns:we_1=&quot;http://webservices.SbClPtsWqs.kvb.de&quot;>&lt;context/>&lt;headers>&lt;SMOHeader>&lt;MessageUUID>8B34F934-0135-4000-E000-0F300AFC4342&lt;/MessageUUID>&lt;Version>&lt;Version>7&lt;/Version>&lt;Release>0&lt;/Release>&lt;Modification>0&lt;/Modification>&lt;/Version>&lt;MessageType>Response&lt;/MessageType>&lt;Operation>ptsAnlegen&lt;/Operation>&lt;SourceNode>SbClPtsWqsImport&lt;/SourceNode>&lt;SourceBindingType>WebService&lt;/SourceBindingType>&lt;Interface>wsdl:http://webservices.SbClPtsWqs.kvb.de&lt;/Interface>&lt;/SMOHeader>&lt;/headers>&lt;body xsi:type=&quot;we:ptsAnlegenResponse&quot;>&lt;we_1:ROPtsAnlegen>&lt;messages>&lt;message>&lt;fehlerklasse>DF&lt;/fehlerklasse>&lt;fehlerart>SB20501&lt;/fehlerart>&lt;fehlermeldung>Referenzierter Datensatz in der Schl�sseltabelle existiert nicht.&lt;/fehlermeldung>&lt;extraFehlertext>Dokumentenberechtigung AUF WEN, Schl�ssel:  006&lt;/extraFehlertext>&lt;/message>&lt;/messages>&lt;returncode>RC-NOK&lt;/returncode>&lt;pVgErgebnisListe/>&lt;pObjErgebnisListe/>&lt;/we_1:ROPtsAnlegen>&lt;/body>&lt;/Se:smo>, 7'</failureString><origin>External Service</origin></failInfo></context><headers><SMOHeader><MessageUUID>8B34F943-0135-4000-E000-0F300AFC4342</MessageUUID><Version><Version>7</Version><Release>0</Release><Modification>0</Modification></Version><MessageType>Exception</MessageType></SMOHeader><SOAPFaultInfo/></headers><body xsi:type=\"se:legeAnRequestMsg\"/></Se:smo>, 7'");
        Assert.assertEquals((
            ""
            // SUPPRESS CHECKSTYLE LineLength
            + "[17.02.12 13:06:39:171 CET] 000000a5 MFCImplementa E   CWSXM0205E: Es ist eine unerwartete Ausnahmebedingung in der Business-Logik aufgetreten.: CWSXM3300E: Das primitive Fehlerelement 'Fail2' (Komponente 'Vorgangsanlage/Vorgangsanlage', Modul 'PW2', Schnittstelle '{http://PW2/wsdl/Vorgangsanlage}Vorgangsanlage', Operation 'legeVorgaengeAn') hat eine FailFlowException ausgel�st. Vom Benutzer bereitgestellte Fehlernachricht: '17.02.12 13:06, 8B34F943-0135-4000-E000-0F300AFC4342, Fail2, PW2, <Se:smo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:Se=\"http://www.bim.com/websphere/sibx/smo/v6.0.1\" xmlns:se=\"wsdl.http://TK_SBI_lib/SbiService\" xmlns:vo=\"http://PW2/med/Vorgangsanlage\">\r\n"
            + "  <context>\r\n"
            + "    <correlation xsi:type=\"vo:CorrelationContext\">\r\n"
            + "      <anliegen>\r\n"
            + "        <sendungsId>SENDUNGS-ID 1</sendungsId>\r\n"
            + "        <eingangskanal>Post</eingangskanal>\r\n"
            + "        <prioritaet>3</prioritaet>\r\n"
            + "        <anliegentyp>Storno</anliegentyp>\r\n"
            + "        <prozesstemplate>P2_Vertragsbeendigung</prozesstemplate>\r\n"
            + "        <dokumentIds>DOKUMENT-ID 1</dokumentIds>\r\n"
            + "        <dokumentIds>DOKUMENT-ID 2</dokumentIds>\r\n"
            + "        <fuehrendeDokumentId>DOKUMENT-ID 1</fuehrendeDokumentId>\r\n"
            + "        <dokumente>\r\n"
            + "          <dokumentId>DOKUMENT-ID 1</dokumentId>\r\n"
            + "          <dokumentTimestamp>\r\n"
            + "          </dokumentTimestamp>\r\n"
            + "          <dokumentTyp>302</dokumentTyp>\r\n"
            + "          <vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV1234</vsnr>\r\n"
            + "          <vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">VM1</vmnr>\r\n"
            + "          <ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA1</ebanr>\r\n"
            + "          <erfassungszeitpunkt>2011-12-22T11:35:59.000Z</erfassungszeitpunkt>\r\n"
            + "          <mandant>00001</mandant>\r\n"
            + "          <ablageort>nscale</ablageort>\r\n"
            + "          <produktgesellschaft>001</produktgesellschaft>\r\n"
            + "          <kundenmerkmal>104</kundenmerkmal>\r\n"
            + "          <produktgruppe>59</produktgruppe>\r\n"
            + "          <berechtigungFuer>001</berechtigungFuer>\r\n"
            + "          <berechtigungAufWas>002</berechtigungAufWas>\r\n"
            + "          <berechtigungAufWen>003</berechtigungAufWen>\r\n"
            + "          <korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal>\r\n"
            + "        </dokumente>\r\n"
            + "        <nachkorrekturKontext>NACHKORREKTUR-KONTEXT</nachkorrekturKontext>\r\n"
            + "      </anliegen>\r\n"
            + "      <anliegen>\r\n"
            + "        <sendungsId>SENDUNGS-ID 1</sendungsId>\r\n"
            + "        <eingangskanal>Post</eingangskanal>\r\n"
            + "        <prioritaet>39</prioritaet>\r\n"
            + "        <anliegentyp>KontoAEA</anliegentyp>\r\n"
            + "        <prozesstemplate>P2_Vertragsaenderung</prozesstemplate>\r\n"
            + "        <dokumentIds>DOKUMENT-ID 1</dokumentIds>\r\n"
            + "        <dokumentIds>DOKUMENT-ID 2</dokumentIds>\r\n"
            + "        <fuehrendeDokumentId>DOKUMENT-ID 2</fuehrendeDokumentId>\r\n"
            + "        <dokumente>\r\n"
            + "          <dokumentId>DOKUMENT-ID 2</dokumentId>\r\n"
            + "          <dokumentTimestamp>\r\n"
            + "          </dokumentTimestamp>\r\n"
            + "          <dokumentTyp>309</dokumentTyp>\r\n"
            + "          <vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV12345</vsnr>\r\n"
            + "          <vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">\r\n"
            + "          </vmnr>\r\n"
            + "          <ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA2</ebanr>\r\n"
            + "          <erfassungszeitpunkt>2011-12-22T11:35:59.001Z</erfassungszeitpunkt>\r\n"
            + "          <mandant>00001</mandant>\r\n"
            + "          <ablageort>nscale</ablageort>\r\n"
            + "          <produktgesellschaft>001</produktgesellschaft>\r\n"
            + "          <kundenmerkmal>101</kundenmerkmal>\r\n"
            + "          <produktgruppe>60</produktgruppe>\r\n"
            + "          <berechtigungFuer>004</berechtigungFuer>\r\n"
            + "          <berechtigungAufWas>005</berechtigungAufWas>\r\n"
            + "          <berechtigungAufWen>006</berechtigungAufWen>\r\n"
            + "          <korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal>\r\n"
            + "        </dokumente>\r\n"
            + "        <dokumente>\r\n"
            + "          <dokumentId>DOKUMENT-ID 3</dokumentId>\r\n"
            + "          <dokumentTimestamp>\r\n"
            + "          </dokumentTimestamp>\r\n"
            + "          <dokumentTyp>233</dokumentTyp>\r\n"
            + "          <vsnr abgeleitet=\"false\" fuehrend=\"true\" mehrfach=\"false\">LV1234567</vsnr>\r\n"
            + "          <vmnr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">\r\n"
            + "          </vmnr>\r\n"
            + "          <ebanr abgeleitet=\"false\" fuehrend=\"false\" mehrfach=\"false\">EBA2</ebanr>\r\n"
            + "          <erfassungszeitpunkt>2011-12-22T11:35:59.001Z</erfassungszeitpunkt>\r\n"
            + "          <mandant>00001</mandant>\r\n"
            + "          <ablageort>nscale</ablageort>\r\n"
            + "          <produktgesellschaft>001</produktgesellschaft>\r\n"
            + "          <kundenmerkmal>101</kundenmerkmal>\r\n"
            + "          <produktgruppe>\r\n"
            + "          </produktgruppe>\r\n"
            + "          <berechtigungFuer>007</berechtigungFuer>\r\n"
            + "          <berechtigungAufWas>008</berechtigungAufWas>\r\n"
            + "          <berechtigungAufWen>009</berechtigungAufWen>\r\n"
            + "          <korrelationsmerkmal>KORRELATIONSMERKMAL</korrelationsmerkmal>\r\n"
            + "        </dokumente>\r\n"
            + "        <nachkorrekturKontext>NACHKORREKTUR-KONTEXT</nachkorrekturKontext>\r\n"
            + "      </anliegen>\r\n"
            + "      <eingangskanal>01</eingangskanal>\r\n"
            + "    </correlation>\r\n"
            + "    <failInfo>\r\n"
            + "      <failureString>CWSXM0201E: Der Mediationsablauf f�r Komponente VorgangMediation in Modul TK_SBI hat eine Ausnahmebedingung zur�ckgegeben.: CWSXM3300E: Das primitive Fehlerelement 'Fail1' (Komponente 'VorgangMediation', Modul 'TK_SBI', Schnittstelle '{http://TK_SBI_lib/SbiService}Vorgang', Operation 'legeAn') hat eine FailFlowException ausgel�st. Vom Benutzer bereitgestellte Fehlernachricht: '17.02.12 13:06, 8B34F934-0135-4000-E000-0F300AFC4342, Fail1, TK_SBI, &lt;Se:smo xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xmlns:Se=&quot;http://www.bim.com/websphere/sibx/smo/v6.0.1&quot; xmlns:we=&quot;wsdl.http://webservices.SbClPtsWqs.kvb.de&quot; xmlns:we_1=&quot;http://webservices.SbClPtsWqs.kvb.de&quot;>&lt;context/>&lt;headers>&lt;SMOHeader>&lt;MessageUUID>8B34F934-0135-4000-E000-0F300AFC4342&lt;/MessageUUID>&lt;Version>&lt;Version>7&lt;/Version>&lt;Release>0&lt;/Release>&lt;Modification>0&lt;/Modification>&lt;/Version>&lt;MessageType>Response&lt;/MessageType>&lt;Operation>ptsAnlegen&lt;/Operation>&lt;SourceNode>SbClPtsWqsImport&lt;/SourceNode>&lt;SourceBindingType>WebService&lt;/SourceBindingType>&lt;Interface>wsdl:http://webservices.SbClPtsWqs.kvb.de&lt;/Interface>&lt;/SMOHeader>&lt;/headers>&lt;body xsi:type=&quot;we:ptsAnlegenResponse&quot;>&lt;we_1:ROPtsAnlegen>&lt;messages>&lt;message>&lt;fehlerklasse>DF&lt;/fehlerklasse>&lt;fehlerart>SB20501&lt;/fehlerart>&lt;fehlermeldung>Referenzierter Datensatz in der Schl�sseltabelle existiert nicht.&lt;/fehlermeldung>&lt;extraFehlertext>Dokumentenberechtigung AUF WEN, Schl�ssel:  006&lt;/extraFehlertext>&lt;/message>&lt;/messages>&lt;returncode>RC-NOK&lt;/returncode>&lt;pVgErgebnisListe/>&lt;pObjErgebnisListe/>&lt;/we_1:ROPtsAnlegen>&lt;/body>&lt;/Se:smo>, 7'</failureString>\r\n" // SUPPRESS CHECKSTYLE LineLength
            + "      <origin>External Service</origin>\r\n"
            + "    </failInfo>\r\n"
            + "  </context>\r\n"
            + "  <headers>\r\n"
            + "    <SMOHeader>\r\n"
            + "      <MessageUUID>8B34F943-0135-4000-E000-0F300AFC4342</MessageUUID>\r\n"
            + "      <Version>\r\n"
            + "        <Version>7</Version>\r\n"
            + "        <Release>0</Release>\r\n"
            + "        <Modification>0</Modification>\r\n"
            + "      </Version>\r\n"
            + "      <MessageType>Exception</MessageType>\r\n"
            + "    </SMOHeader>\r\n"
            + "    <SOAPFaultInfo/>\r\n"
            + "  </headers>\r\n"
            + "  <body xsi:type=\"se:legeAnRequestMsg\"/>\r\n"
            + "</Se:smo> 7'"
        ), sw.toString());
    }

    public static void
    main(String[] args) throws IOException {
        XMLFormatterWriter w = new XMLFormatterWriter(new OutputStreamWriter(System.out));
        IoUtil.copy(new InputStreamReader(System.in), w);
        w.flush();
    }
}
