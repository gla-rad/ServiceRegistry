/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for XML manipulation.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class XmlUtil {

    /**
     * update an xml node with a new value
     *
     * @param newValue        the updated value to set
     * @param xml             the XML as string
     * @param xPathExpression the xpath expression to the element to be changed
     * @return the resulting XML as string
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the sax exception
     * @throws IOException                  the io exception
     * @throws XPathExpressionException     the x path expression exception
     * @throws TransformerException         the transformer exception
     */
    public static String updateXmlNode(String newValue, String xml, String xPathExpression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        Node result = (Node) xPath.evaluate(xPathExpression, doc, XPathConstants.NODE);
        if(result == null) {
            throw new TransformerException("Missing element");
        } else {
            result.getFirstChild().setNodeValue(newValue);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Validate xml against a schema on classpath
     *
     * @param xml            the XML as string
     * @param schemaFileName the name of the XSD on the classpath
     * @return true if successful, throws SAXEXception if xml invalid
     * @throws SAXException the sax exception
     * @throws IOException  the io exception
     */
    public static boolean validateXml(String xml, String schemaFileName) throws SAXException, IOException {
        //File schemaFile = new File(schemaFileName);
        Source xmlSource = new StreamSource(new StringReader(xml));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new ResourceResolver());
        InputStream schemaInputStream = XmlUtil.class.getClassLoader().getResourceAsStream(schemaFileName);
        StreamSource schemaStreamSource = new StreamSource(schemaInputStream);
        Schema schema = schemaFactory.newSchema(schemaStreamSource);
        Validator validator = schema.newValidator();
        validator.validate(xmlSource);
        return true;
    }
}
