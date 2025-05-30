/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
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

import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;

import jakarta.xml.bind.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class G1128Utils<T> {

    // Create a public definition of all the G1128 sources
    public static List<String> SOURCES_LIST = Arrays.asList(new String[]{
            G1128Schemas.INSTANCE.getPath()
    });

    // Class Variables
    private Class<T> g1128Spec;

    /**
     * The G1128Utils constructor that initialises the G1128 specification
     * object class type.
     *
     * @param g1128Spec the G1128 specification object class type
     */
    public G1128Utils(Class<T> g1128Spec) {
        this.g1128Spec = g1128Spec;
    }

    /**
     * Using the G1128 utilities we can marshall back a service instance
     * specification document into it's XML view.
     *
     * @param serviceInstance the Service Instance object
     * @return the marshalled G1128 Service Instance XML representation
     */
    public String marshalG1128(T serviceInstance) throws JAXBException {
        // Create the JAXB objects
        JAXBContext jaxbContext = JAXBContext.newInstance(this.g1128Spec);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Transform the G1128 object to an output stream
        ByteArrayOutputStream xmlStream = new ByteArrayOutputStream();
        jaxbMarshaller.marshal(serviceInstance,xmlStream);

        // Return the XML string
        return xmlStream.toString(Charset.defaultCharset());
    }

    /**
     * Using the G1128 utilities we can easily parse through a G1128 XML
     * specification and create the respective G1128 POJO.
     *
     * @param g1128Xml the G1128 XML specification
     * @return The unmarshalled G1128 object
     * @throws JAXBException any JAXB exceptions while unmarshalling the XML
     */
    public T unmarshallG1128(String g1128Xml) throws JAXBException {
        // Create the JAXB objects
        JAXBContext jaxbContext = JAXBContext.newInstance(this.g1128Spec);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Transform the G1128 context into an input stream
        ByteArrayInputStream is = new ByteArrayInputStream(g1128Xml.getBytes());

        // And translate
        return (T) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(is));
    }

}
