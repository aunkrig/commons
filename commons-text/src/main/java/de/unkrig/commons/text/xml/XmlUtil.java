
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

package de.unkrig.commons.text.xml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utiltity methods related to the JRE's {@code org.xml}.
 */
public final
class XmlUtil {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private XmlUtil() {}

    @SuppressWarnings("null") private static final ErrorListener
    SIMPLE_ERROR_LISTENER = new ErrorListener() {
        @Override public void warning(@Nullable TransformerException e)    throws TransformerException { throw e; }
        @Override public void fatalError(@Nullable TransformerException e) throws TransformerException { throw e; }
        @Override public void error(@Nullable TransformerException e)      throws TransformerException { throw e; }
    };

    private static final String LOCATOR_KEY = "locationDataKey";

    /**
     * A drop-in replacement for {@link DocumentBuilder#parse(File)}, where each node of the parsed DOM contains
     * location information.
     *
     * @see #getLocation(Node)
     */
    public static Document
    parse(DocumentBuilder documentBuilder, File inputFile, @Nullable String encoding)
    throws ParserConfigurationException, SAXException, TransformerException, IOException {

        InputSource inputSource = new InputSource("file:" + inputFile.getAbsolutePath());

        if (encoding != null) inputSource.setEncoding(encoding);
        inputSource.setPublicId(inputFile.getPath());

        return XmlUtil.parse(documentBuilder, inputSource);
    }

    /**
     * A drop-in replacement for {@link DocumentBuilder#parse(InputSource)}, where each node of the parsed DOM contains
     * location information.
     *
     * @see #getLocation(Node)
     */
    public static Document
    parse(DocumentBuilder documentBuilder, InputSource inputSource)
    throws ParserConfigurationException, SAXException, TransformerException, IOException {

        Transformer nullTransformer = TransformerFactory.newInstance().newTransformer();
        nullTransformer.setErrorListener(XmlUtil.SIMPLE_ERROR_LISTENER);

        // Create an empty document to be populated within a DOMResult.
        Document document = documentBuilder.newDocument();

        // Create SAX parser/XMLReader that will parse XML. If factory options are not required then this can be short
        // cut by:
        //     xmlReader = XMLReaderFactory.createXMLReader();
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        // saxParserFactory.setNamespaceAware(true);
        // saxParserFactory.setValidating(true);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();

        // Create our filter to wrap the SAX parser, that captures the locations of elements and annotates their nodes
        // as they are inserted into the DOM.
        xmlReader = XmlUtil.anotateLocations(xmlReader, (EventTarget) document);

        // Create the SAXSource to use the annotator.
        SAXSource saxSource = new SAXSource(xmlReader, inputSource);

        // Finally read the XML into the DOM.
        try {
            nullTransformer.transform(saxSource, new DOMResult(document));
        } catch (TransformerException te) {
            Throwable t = te.getException();
            if (t instanceof SAXException) throw (SAXException) t; // SUPPRESS CHECKSTYLE AvoidHidingCause
            if (t instanceof IOException) throw (IOException) t; // SUPPRESS CHECKSTYLE AvoidHidingCause
            throw te;
        }

        return document;
    }

    /**
     * @return The location data of the given DOM <var>node</var>
     */
    @Nullable public static Locator
    getLocation(Node node) { return (Locator) node.getUserData(XmlUtil.LOCATOR_KEY); }

    /**
     * @return An {@link XMLReader} that wraps the given <var>delegate</var> and annotates all nodes with their
     *         locations
     * @see    #getLocation(Node)
     */
    private static XMLReader
    anotateLocations(final XMLReader delegate, final EventTarget eventTarget) {

        return new PositionAnnotatingXMLReader(delegate, eventTarget);
    }

    static
    class PositionAnnotatingXMLReader extends XMLFilterImpl {

        @Nullable private Locator     locator;
        private final UserDataHandler dataHandler = new UserDataHandler() {

            @NotNullByDefault(false) @Override public void
            handle(short operation, String key, Object data, Node src, Node dst) {

                if (src != null && dst != null) {
                    dst.setUserData(XmlUtil.LOCATOR_KEY, src.getUserData(XmlUtil.LOCATOR_KEY), this);
                }
            }
        };

        PositionAnnotatingXMLReader(final XMLReader delegate, final EventTarget eventTarget) {
            super(delegate);

            // Add listener to DOM, so we know which node was added.
            eventTarget.addEventListener("DOMNodeInserted", new EventListener() {

                @Override public void
                handleEvent(@Nullable Event event) {

                    if (event instanceof MutationEvent) {
                        MutationEvent mutationEvent = (MutationEvent) event;

                        EventTarget target = mutationEvent.getTarget();
                        if (target instanceof Node) {
                            Node node = (Node) target;

                            // Copy the locator.
                            LocatorImpl l = new LocatorImpl(PositionAnnotatingXMLReader.this.locator);

                            // Annotate the node with the copied locator.
                            node.setUserData(XmlUtil.LOCATOR_KEY, l, PositionAnnotatingXMLReader.this.dataHandler);
                        }
                    }
                }
            }, true);
        }

        @NotNullByDefault(false) @Override public void
        setDocumentLocator(Locator locator) {
            super.setDocumentLocator(locator);

            this.locator = locator;
        }
    }

    /**
     * The "{@code toString()}" method of class {@link Node} does not produce very impressing results; this methods
     * is a plug-in substitute.
     */
    public static String
    toString(Node node) {
        return (
            node.getNodeType() == Node.ELEMENT_NODE ? '<' + node.getNodeName() + '>' :
            node.toString()
        );
    }

    /**
     * @return The <var>nodeList</var>, wrapped in an {@link Iterable}
     */
    public static Iterable<Node>
    iterable(final NodeList nodeList) {

        return new Iterable<Node>() {

            @Override public Iterator<Node>
            iterator() {
                return new Iterator<Node>() {

                    private int idx;

                    @Override public Node
                    next() {
                        if (this.idx >= nodeList.getLength()) throw new NoSuchElementException();
                        return nodeList.item(this.idx++);
                    }

                    @Override public boolean
                    hasNext() { return this.idx < nodeList.getLength(); }

                    @Override public void
                    remove() { throw new UnsupportedOperationException("remove"); }
                };
            }
        };
    }
}
