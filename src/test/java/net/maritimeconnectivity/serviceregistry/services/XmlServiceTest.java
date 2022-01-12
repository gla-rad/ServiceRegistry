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

package net.maritimeconnectivity.serviceregistry.services;

import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import net.maritimeconnectivity.serviceregistry.repos.XmlRepo;
import org.apache.commons.io.IOUtils;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XmlServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    private XmlService xmlService;

    /**
     * The XML Repository Mock.
     */
    @Mock
    private XmlRepo xmlRepo;

    // Test Variables
    private List<Xml> xmls;
    private Pageable pageable;
    private Xml newXml;
    private Xml existingXml;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the xmls list
        this.xmls = new ArrayList<>();
        for(long i=0; i<10; i++) {
            Xml xml = new Xml();
            xml.setId(i);
            xml.setName(String.format("Test XML {}", i));
            xml.setComment("No comment");
            xml.setContentContentType("Service Instance XML");
            xml.setContent("<ServiceInstance></ServiceInstance>");
            this.xmls.add(xml);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new xml
        this.newXml = new Xml();
        this.newXml.setName("XML Name");
        this.newXml.setComment("No comment");
        this.newXml.setContentContentType("Service Instance XML");
        this.newXml.setContent("<ServiceInstance></ServiceInstance>");

        // Create an existing xml
        this.existingXml = new Xml();
        this.existingXml.setId(100L);
        this.existingXml.setName("XML Name");
        this.existingXml.setComment("No comment");
        this.existingXml.setContentContentType("Service Instance XML");
        this.existingXml.setContent("<ServiceInstance></ServiceInstance>");
    }

    /**
     * Test that we can retrieve all the xmls currently present in the
     * database through a paged call.
     */
    @Test
    void testFindAll() {
        // Created a result page to be returned by the mocked repository
        Page<Xml> page = new PageImpl<>(this.xmls.subList(0, 5), this.pageable, this.xmls.size());
        doReturn(page).when(this.xmlRepo).findAll(this.pageable);

        // Perform the service call
        Page<Xml> result = this.xmlService.findAll(pageable);

        // Test the result
        assertEquals(page.getSize(), result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getSize(); i++){
            assertEquals(result.getContent().get(i), this.xmls.get(i));
        }
    }

    /**
     * Test that we can retrieve a single xml entry based on the xml
     * ID and all the eager relationships are loaded.
     */
    @Test
    void testFindOne() throws DataNotFoundException {
        doReturn(Optional.of(this.existingXml)).when(this.xmlRepo).findById(this.existingXml.getId());

        // Perform the service call
        Xml result = this.xmlService.findOne(this.existingXml.getId());

        // Make sure the eager relationships repo call was called
        verify(this.xmlRepo, times(1)).findById(this.existingXml.getId());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingXml.getId(), result.getId());
        assertEquals(this.existingXml.getName(), result.getName());
        assertEquals(this.existingXml.getComment(), result.getComment());
        assertEquals(this.existingXml.getContentContentType(), result.getContentContentType());
        assertEquals(this.existingXml.getContent(), result.getContent());
    }

    /**
     * Test that if we do not find the xml we are looking for, a DataNotFound
     * exception will be thrown.
     */
    @Test
    void testFindOneNotFound() {
        doReturn(Optional.ofNullable(null)).when(this.xmlRepo).findById(this.existingXml.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.xmlService.findOne(this.existingXml.getId())
        );
    }

    /**
     * Test that we can save successfully a valid xml.
     */
    @Test
    void testSave() {
        doReturn(this.existingXml).when(this.xmlRepo).save(this.newXml);

        //Perform the service call
        Xml result = this.xmlService.save(this.newXml);

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingXml.getId(), result.getId());
        assertEquals(this.existingXml.getName(), result.getName());
        assertEquals(this.existingXml.getComment(), result.getComment());
        assertEquals(this.existingXml.getContentContentType(), result.getContentContentType());
        assertEquals(this.existingXml.getContent(), result.getContent());
    }

    /**
     * Test that we can successfully delete an existing xml.
     */
    @Test
    void testDelete() throws DataNotFoundException {
        doReturn(Boolean.TRUE).when(this.xmlRepo).existsById(this.existingXml.getId());
        doNothing().when(this.xmlRepo).deleteById(this.existingXml.getId());

        // Perform the service call
        this.xmlService.delete(this.existingXml.getId());

        // Verify that a deletion call took place in the repository
        verify(this.xmlRepo, times(1)).deleteById(this.existingXml.getId());
    }

    /**
     * Test that if we try to delete a non-existing xml then a DataNotFound
     * exception will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.xmlRepo).existsById(this.existingXml.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.xmlService.delete(this.existingXml.getId())
        );
    }

    /**
     * Test that we can successfully validate whether an XML content follows
     * the G1128 specification.
     * @throws IOException when the test XML input file cannot be read
     * @throws JAXBException for any JAXB parsing exceptions
     */
    @Test
    void testValidate() throws IOException, SAXException, JAXBException {
        // Read a test service instance specification
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Perform the serviec call
        ServiceInstance serviceInstance = (ServiceInstance) this.xmlService.validate(xml, G1128Schemas.INSTANCE);

        // Assert all information exists
        assertNotNull(serviceInstance);
        assertNotNull(serviceInstance.getName());
        assertNotNull(serviceInstance.getVersion());
        assertNotNull(serviceInstance.getId());
        assertNotNull( serviceInstance.getKeywords());
        assertNotNull(serviceInstance.getStatus());
        assertNotNull(serviceInstance.getDescription());
        assertNotNull(serviceInstance.getEndpoint());
        assertNotNull(serviceInstance.getMMSI());
        assertNotNull(serviceInstance.getIMO());
        assertNotNull(serviceInstance.getServiceType());
        assertNotNull(serviceInstance.getCoversAreas());
        assertNotNull(serviceInstance.getProducedBy());
        assertNotNull(serviceInstance.getProvidedBy());
    }

    /**
     * Test that when we don't have a valid G1128 schema class (e.g. for the
     * G1128 BASE case), the validation will fail with a DataNotFoundException.
     */
    @Test
    void testValidateNoClass() {
        // Create a test invalid input
        String xml = "Some random input";

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.xmlService.validate(xml, G1128Schemas.BASE)
        );
    }

    /**
     * Test that for an invalid input, the validation function will throw
     * a SAXException or a JAXBException which can then be caught.
     */
    @Test
    void testValidateFails() {
        // Create a test invalid input
        String xml = "Some invalid input";

        // Perform the service call
        assertThrows(SAXException.class, () ->
                this.xmlService.validate(xml, G1128Schemas.INSTANCE)
        );
    }

}