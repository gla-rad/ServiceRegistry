/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.utils;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for XML manipulation.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class XmlUtil {

    /**
     * Validate xml against a schema on classpath
     *
     * @param xml the XML as string
     * @param schemaFiles the location of the XSD schemas on the classpath
     * @return true if successful, throws SAXEXception if xml invalid
     * @throws SAXException the sax exception
     * @throws IOException  the io exception
     */
    public static boolean validateXml(String xml, List<String> schemaFiles) throws SAXException, IOException {
        Source xmlSource = new StreamSource(new StringReader(xml));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<StreamSource> sources = schemaFiles.stream()
                .map(XmlUtil.class.getClassLoader()::getResourceAsStream)
                .map(StreamSource::new)
                .collect(Collectors.toList());
        Schema schema = schemaFactory.newSchema(sources.toArray(new Source[]{}));
        Validator validator = schema.newValidator();
        validator.validate(xmlSource);
        return true;
    }
}
