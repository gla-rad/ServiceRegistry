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

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;

/**
 * The type Resource resolver.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class ResourceResolver implements LSResourceResolver  {

    /**
     * Implements the LSResourceResolver interface function.
     *
     * @param type the resource type
     * @param namespaceURI the namespace URI
     * @param publicId the public ID
     * @param systemId the system ID
     * @param baseURI the base URI
     * @return the resolved resource as an XML input stream
     */
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {

        // note: XSD's are expected to be in the root of the classpath
        InputStream resourceAsStream = this.getClass().getClassLoader()
                .getResourceAsStream(systemId);
        return new XmlInput(publicId, systemId, resourceAsStream);
    }

}
