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

import org.efficiensea2.maritime_cloud.service_registry.v1.servicedesignschema.ServiceDesign;
import org.efficiensea2.maritime_cloud.service_registry.v1.serviceinstanceschema.ServiceInstance;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceSpecification;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;

public class G1128Utils {

    /**
     * Using the G1128 utilities we can easily parse through a G1128 Service
     * Design XML definition and create a ServiceDesign POJO.
     *
     * @param serviceSpecificationXML the G1128 service design XML definition
     * @return The unmarshalled G1128 ServiceDesign object
     * @throws JAXBException any JAXB exceptions while unmarshalling the XML
     */
    public static ServiceDesign unmarshallG1128SD(String serviceSpecificationXML) throws JAXBException {
        // Create the JAXB objects
        JAXBContext jaxbContext = JAXBContext.newInstance(ServiceInstance.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Transform the S125 context into an input stream
        ByteArrayInputStream is = new ByteArrayInputStream(serviceSpecificationXML.getBytes());

        // And translate
        return (ServiceDesign) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(is));
    }

    /**
     * Using the G1128 utilities we can easily parse through a G1128 Service
     * Specification XML definition and create a ServiceSpecification POJO.
     *
     * @param serviceSpecificationXML the G1128 service specification XML specification
     * @return The unmarshalled G1128 ServiceSpecification object
     * @throws JAXBException any JAXB exceptions while unmarshalling the XML
     */
    public static ServiceSpecification unmarshallG1128SS(String serviceSpecificationXML) throws JAXBException {
        // Create the JAXB objects
        JAXBContext jaxbContext = JAXBContext.newInstance(ServiceInstance.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Transform the S125 context into an input stream
        ByteArrayInputStream is = new ByteArrayInputStream(serviceSpecificationXML.getBytes());

        // And translate
        return (ServiceSpecification) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(is));
    }

    /**
     * Using the G1128 utilities we can easily parse through a G1128 Service
     * Instance XML definition and create a ServiceInstance POJO.
     *
     * @param serviceInstanceXML the G1128 service instance XML specification
     * @return The unmarshalled G1128 ServiceInstance object
     * @throws JAXBException any JAXB exceptions while unmarshalling the XML
     */
    public static ServiceInstance unmarshallG1128SI(String serviceInstanceXML) throws JAXBException {
        // Create the JAXB objects
        JAXBContext jaxbContext = JAXBContext.newInstance(ServiceInstance.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Transform the S125 context into an input stream
        ByteArrayInputStream is = new ByteArrayInputStream(serviceInstanceXML.getBytes());

        // And translate
        return (ServiceInstance) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(is));
    }

}
