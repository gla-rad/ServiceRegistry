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

package net.maritimeconnectivity.serviceregistry.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.TestingConfiguration;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import net.maritimeconnectivity.serviceregistry.models.dto.XmlDto;
import net.maritimeconnectivity.serviceregistry.services.XmlService;
import org.apache.commons.io.IOUtils;
import org.iala_aism.g1128.v1_3.servicedesignschema.ServiceDesign;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.ServiceInstance;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = XmlController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class XmlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public DomainDtoMapper xmlDomainToDtoMapper;

    @MockBean
    private XmlService xmlService;

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
     * Test that we can retrieve all the xmls currently in the database in
     * a paged result.
     */
    @Test
    void testGetAllXmls() throws Exception {
        // Created a result page to be returned by the mocked service
        Page<Xml> page = new PageImpl<>(this.xmls.subList(0, 5), this.pageable, this.xmls.size());
        doReturn(page).when(this.xmlService).findAll(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/xmls"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-Total-Count", Long.toString(page.getTotalElements())))
                .andExpect(header().exists(HttpHeaders.LINK))
                .andReturn();

        // Parse and validate the response
        XmlDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), XmlDto[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that we can correctly retrieve a single xml based on the
     * provided entry ID.
     */
    @Test
    void testGetXml() throws Exception {
        doReturn(this.existingXml).when(this.xmlService).findOne(this.existingXml.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/xmls/{id}", this.existingXml.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        XmlDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), XmlDto.class);
        assertNotNull(result);
        assertEquals(this.existingXml.getId(), result.getId());
        assertEquals(this.existingXml.getName(), result.getName());
        assertEquals(this.existingXml.getComment(), result.getComment());
        assertEquals(this.existingXml.getContentContentType(), result.getContentContentType());
        assertEquals(this.existingXml.getContent(), result.getContent());
    }

    /**
     * Test that if we do NOT find the xml we are looking for, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testGetXmlNotFound() throws Exception {
        Long id = 0L;
        doThrow(new DataNotFoundException()).when(this.xmlService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/xmls/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new xml correctly through a POST request.
     * The incoming instance should NOT have an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    void testPostXml() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingXml).when(this.xmlService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/xmls")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.xmlDomainToDtoMapper.convertTo(this.newXml, XmlDto.class))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        XmlDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), XmlDto.class);
        assertNotNull(result);
        assertEquals(this.existingXml.getId(), result.getId());
        assertEquals(this.existingXml.getName(), result.getName());
        assertEquals(this.existingXml.getComment(), result.getComment());
        assertEquals(this.existingXml.getContentContentType(), result.getContentContentType());
        assertEquals(this.existingXml.getContent(), result.getContent());
    }

    /**
     * Test that if we try to create an xml with an existing ID field,
     * an HTTP BAR_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testPostXmlWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/xmls")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.xmlDomainToDtoMapper.convertTo(this.existingXml, XmlDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can update an existing xml correctly through a PUT
     * request. The incoming xml should always have an ID.
     */
    @Test
    void testPutXml() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingXml).when(this.xmlService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/xmls/{id}", this.existingXml.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.xmlDomainToDtoMapper.convertTo(this.existingXml, XmlDto.class))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        XmlDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), XmlDto.class);
        assertNotNull(result);
        assertEquals(this.existingXml.getId(), result.getId());
        assertEquals(this.existingXml.getName(), result.getName());
        assertEquals(this.existingXml.getComment(), result.getComment());
        assertEquals(this.existingXml.getContentContentType(), result.getContentContentType());
        assertEquals(this.existingXml.getContent(), result.getContent());
    }

    /**
     * Test that we can correctly delete an existing xml by using a valid
     * ID.
     */
    @Test
    void testDeleteXml() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/xmls/{id}", this.existingXml.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the xml we are trying to delete, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteXmlNotFound() throws Exception {
        doThrow(new DataNotFoundException()).when(this.xmlService).delete(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/xmls/{id}", this.existingXml.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can retrieve the G1128 Design Specification schema
     * correctly.
     */
    @Test
    void testGetG1128SchemaDesign() throws Exception {
        // Get the G1128 Design schema to test with
        G1128Schemas schema = G1128Schemas.DESIGN;
        InputStream is = getClass().getClassLoader().getResourceAsStream(schema.getPath());
        String schemaXml = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/xmls/schemas/{schema}", schema.getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8)))
                .andReturn();

        // Make sure the retrieved schema is correct
        assertEquals(schemaXml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that we can retrieve the G1128 Service Specification schema
     * correctly.
     */
    @Test
    void testGetG1128SchemaService() throws Exception {
        // Get the G1128 Design schema to test with
        G1128Schemas schema = G1128Schemas.SERVICE;
        InputStream is = getClass().getClassLoader().getResourceAsStream(schema.getPath());
        String schemaXml = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/xmls/schemas/{schema}", schema.getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8)))
                .andReturn();

        // Make sure the retrieved schema is correct
        assertEquals(schemaXml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that we can retrieve the G1128 Instance Specification schema
     * correctly.
     */
    @Test
    void testGetG1128SchemaInstance() throws Exception {
        // Get the G1128 Design schema to test with
        G1128Schemas schema = G1128Schemas.INSTANCE;
        InputStream is = getClass().getClassLoader().getResourceAsStream(schema.getPath());
        String schemaXml = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/xmls/schemas/{schema}", schema.getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8)))
                .andReturn();

        // Make sure the retrieved schema is correct
        assertEquals(schemaXml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that when we request an invalid G1128 Specification schema, then a
     * 500 (INTERNAL_SERVER_ERROR) response will be returned.
     */
    @Test
    void testGetG1128SchemaInvalid() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(get("/api/xmls/schemas/{schema}", "invalid"))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test that we can validate correctly a G1128 design specification XML.
     */
    @Test
    void testValidateXmlDesign() throws Exception {
        doReturn(new ServiceDesign()).when(this.xmlService).validate(any(), eq(G1128Schemas.DESIGN));
        String xml = "<serviceDesign></serviceDesign>";

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.DESIGN.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        ServiceDesign result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ServiceDesign.class);
        assertNotNull(result);
    }

    /**
     * Test that we can detect when the provided G1128 design specification
     * XML is invalid.
     */
    @Test
    void testValidateXmlDesignFails() throws Exception {
        doThrow(new JAXBException("JAXBException", new Exception("With a cause"))).when(this.xmlService).validate(any(), eq(G1128Schemas.DESIGN));
        String xml = "<serviceDesign></serviceDesign>";

        // Perform the MVC request
        this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.DESIGN.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can validate correctly a G1128 service specification XML.
     */
    @Test
    void testValidateXmlService() throws Exception {
        doReturn(new ServiceSpecification()).when(this.xmlService).validate(any(), eq(G1128Schemas.SERVICE));
        String xml = "<serviceSpecification></serviceSpecification>";

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.SERVICE.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        ServiceSpecification result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ServiceSpecification.class);
        assertNotNull(result);
    }

    /**
     * Test that we can detect when the provided G1128 service specification
     * XML is invalid.
     */
    @Test
    void testValidateXmlServiceFails() throws Exception {
        doThrow(new JAXBException("JAXBException", new Exception("With a cause"))).when(this.xmlService).validate(any(), eq(G1128Schemas.SERVICE));
        String xml = "<serviceSpecification></serviceSpecification>";

        // Perform the MVC request
        this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.SERVICE.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can validate correctly a G1128 instance specification XML.
     */
    @Test
    void testValidateXmlInstance() throws Exception {
        doReturn(new ServiceDesign()).when(this.xmlService).validate(any(), eq(G1128Schemas.INSTANCE));
        String xml = "<serviceInstance></serviceInstance>";

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.INSTANCE.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        ServiceInstance result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ServiceInstance.class);
        assertNotNull(result);
    }

    /**
     * Test that we can detect when the provided G1128 instance specification
     * XML is invalid.
     */
    @Test
    void testValidateXmlInstanceFails() throws Exception {
        doThrow(new JAXBException("JAXBException", new Exception("With a cause"))).when(this.xmlService).validate(any(), eq(G1128Schemas.INSTANCE));
        String xml = "<serviceInstance></serviceInstance>";

        // Perform the MVC request
        this.mockMvc.perform(post("/api/xmls/validate/{schema}", G1128Schemas.INSTANCE.getName())
                .contentType(MediaType.APPLICATION_XML)
                .content(xml))
                .andExpect(status().isBadRequest());
    }

}