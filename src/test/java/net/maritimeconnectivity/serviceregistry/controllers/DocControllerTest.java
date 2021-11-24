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
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.models.dto.DocDto;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.*;
import net.maritimeconnectivity.serviceregistry.services.DocService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = DocController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class DocControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public DomainDtoMapper docDomainToDtoMapper;

    @MockBean
    private DocService docService;

    // Test Variables
    private List<Doc> docs;
    private Pageable pageable;
    private Doc newDoc;
    private Doc existingDoc;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the docs list
        this.docs = new ArrayList<>();
        for(long i=0; i<10; i++) {
            Doc doc = new Doc();
            doc.setId(i);
            doc.setName(String.format("Test Doc {}", i));
            doc.setComment("No comment");
            doc.setFilecontentContentType("application/pdf");
            doc.setFilecontent(new byte[]{00});
            this.docs.add(doc);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new doc
        this.newDoc = new Doc();
        this.newDoc.setName("Doc Name");
        this.newDoc.setComment("No comment");
        this.newDoc.setMimetype("application/pdf");
        this.newDoc.setFilecontentContentType("application/pdf");
        this.newDoc.setFilecontent(new byte[]{0b00});

        // Create an existing doc
        this.existingDoc = new Doc();
        this.existingDoc.setId(100L);
        this.existingDoc.setName("Doc Name");
        this.existingDoc.setComment("No comment");
        this.existingDoc.setMimetype("application/pdf");
        this.existingDoc.setFilecontentContentType("application/pdf");
        this.existingDoc.setFilecontent(new byte[]{0b00});
    }

    /**
     * Test that we can retrieve all the docs currently in the database in
     * a paged result.
     */
    @Test
    void testGetAllDocs() throws Exception {
        // Created a result page to be returned by the mocked service
        Page<Doc> page = new PageImpl<>(this.docs.subList(0, 5), this.pageable, this.docs.size());
        doReturn(page).when(this.docService).findAll(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-Total-Count", Long.toString(page.getTotalElements())))
                .andExpect(header().exists(HttpHeaders.LINK))
                .andReturn();

        // Parse and validate the response
        DocDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DocDto[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that the API supports the jQuery Datatables server-side paging
     * and search requests.
     */
    @Test
    void testGetDocsForDatatables() throws Exception {
        // Create a test datatables paging request
        DtColumn dtColumn = new DtColumn("id");
        dtColumn.setName("ID");
        dtColumn.setOrderable(true);
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        DtPagingRequest dtPagingRequest = new DtPagingRequest();
        dtPagingRequest.setStart(0);
        dtPagingRequest.setLength(this.docs.size());
        dtPagingRequest.setDraw(1);
        dtPagingRequest.setSearch(new DtSearch());
        dtPagingRequest.setOrder(Collections.singletonList(dtOrder));
        dtPagingRequest.setColumns(Collections.singletonList(dtColumn));

        // Create a mocked paging response
        Page<Doc> page = new PageImpl<>(this.docs, this.pageable, this.docs.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.docService).handleDatatablesPagingRequest(eq(1L), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/docs/dt?instanceId=1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dtPagingRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        DtPage<InstanceDto> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DtPage.class);
        assertEquals(this.docs.size(), result.getData().size());
    }

    /**
     * Test that we can correctly retrieve a single doc based on the
     * provided entry ID.
     */
    @Test
    void testGetDoc() throws Exception {
        doReturn(this.existingDoc).when(this.docService).findOne(this.existingDoc.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/docs/{id}", this.existingDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        DocDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DocDto.class);
        assertNotNull(result);
        assertEquals(this.existingDoc.getId(), result.getId());
        assertEquals(this.existingDoc.getName(), result.getName());
        assertEquals(this.existingDoc.getMimetype(), result.getMimetype());
        assertEquals(this.existingDoc.getComment(), result.getComment());
        assertEquals(this.existingDoc.getFilecontentContentType(), result.getFilecontentContentType());
        assertTrue(Arrays.equals(this.existingDoc.getFilecontent(), result.getFilecontent()));
    }

    /**
     * Test that if we do NOT find the doc we are looking for, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testGetDocNotFound() throws Exception {
        Long id = 0L;
        doThrow(new DataNotFoundException()).when(this.docService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/docs/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new doc correctly through a POST request.
     * The incoming instance should NOT has an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    void testPostDoc() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingDoc).when(this.docService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/docs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.docDomainToDtoMapper.convertTo(this.newDoc, DocDto.class))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        DocDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DocDto.class);
        assertNotNull(result);
        assertEquals(this.existingDoc.getId(), result.getId());
        assertEquals(this.existingDoc.getName(), result.getName());
        assertEquals(this.existingDoc.getMimetype(), result.getMimetype());
        assertEquals(this.existingDoc.getComment(), result.getComment());
        assertEquals(this.existingDoc.getFilecontentContentType(), result.getFilecontentContentType());
        assertTrue(Arrays.equals(this.existingDoc.getFilecontent(), result.getFilecontent()));
    }

    /**
     * Test that if we try to create an doc with an existing ID field,
     * an HTTP BAR_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testPostDocWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/docs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.docDomainToDtoMapper.convertTo(this.existingDoc, DocDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that if we try to create an doc with th wrong format, a bad
     * request will be returned.
     */
    @Test
    void testPostDocWrongFormat() throws Exception {
        // Set a wrong format on the uploaded doc
        this.existingDoc.setFilecontentContentType("wrongType");

        // Perform the MVC request
        this.mockMvc.perform(post("/api/docs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.docDomainToDtoMapper.convertTo(this.existingDoc, DocDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can update an existing doc correctly through a PUT
     * request. The incoming doc should always have an ID.
     */
    @Test
    void testPutDoc() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingDoc).when(this.docService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/docs/{id}", this.existingDoc.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.docDomainToDtoMapper.convertTo(this.existingDoc, DocDto.class))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        DocDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DocDto.class);
        assertNotNull(result);
        assertEquals(this.existingDoc.getId(), result.getId());
        assertEquals(this.existingDoc.getName(), result.getName());
        assertEquals(this.existingDoc.getMimetype(), result.getMimetype());
        assertEquals(this.existingDoc.getComment(), result.getComment());
        assertEquals(this.existingDoc.getFilecontentContentType(), result.getFilecontentContentType());
        assertTrue(Arrays.equals(this.existingDoc.getFilecontent(), result.getFilecontent()));
    }

    /**
     * Test that if we try to update a doc with th wrong format, a bad
     * request will be returned.
     */
    @Test
    void testPutDocWrongFormat() throws Exception {
        // Set a wrong format on the uploaded doc
        this.existingDoc.setFilecontentContentType("wrongType");

        // Perform the MVC request
        this.mockMvc.perform(put("/api/docs/{id}", this.existingDoc.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.docDomainToDtoMapper.convertTo(this.existingDoc, DocDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing doc by using a valid
     * ID.
     */
    @Test
    void testDeleteDoc() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/docs/{id}", this.existingDoc.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the doc we are trying to delete, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteDocNotFound() throws Exception {
        doThrow(new DataNotFoundException()).when(this.docService).delete(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/docs/{id}", this.existingDoc.getId()))
                .andExpect(status().isNotFound());
    }

}