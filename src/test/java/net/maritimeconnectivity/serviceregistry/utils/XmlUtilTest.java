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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlUtilTest {

    /**
     * Test that we can correctly validate an XML if compared to a valid XSD
     * schema definition.
     */
    @Test
    void testValidateXmlTrue() throws IOException, SAXException {
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xml = IOUtils.toString(in, StandardCharsets.UTF_8);
        assertTrue(XmlUtil.validateXml(xml, G1128Utils.SOURCES_LIST));
    }

    /**
     * Test that we can detect errors while validating an XML against an
     * XSD schema definition, is the XMl does not conform to it. These
     * errors will be thrown as exceptions so we should expect them.
     */
    @Test
    void testValidateXmlFalse() throws IOException, SAXException {
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Introduce an error in the XML
        String wrongXml = xml.replaceAll("id", "wrongIdTag");

        // And  the exception
        assertThrows(SAXParseException.class, () ->
            XmlUtil.validateXml(wrongXml, G1128Utils.SOURCES_LIST)
        );
    }

}