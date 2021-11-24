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
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.*;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = InstanceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class InstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public DomainDtoMapper instanceDomainToDtoMapper;

    @MockBean
    private InstanceService instanceService;

    // Test Variables
    private List<Instance> instances;
    private Pageable pageable;
    private Instance newInstance;
    private Instance existingInstance;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the instances list
        this.instances = new ArrayList<>();
        for(long i=0; i<10; i++) {
            Instance instance = new Instance();
            instance.setId(i);
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", i));
            instance.setName(String.format("Test Instance %d", i));
            this.instances.add(instance);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new instance
        this.newInstance = new Instance();
        this.newInstance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.newInstance.setName("Instance Name");
        this.newInstance.setVersion("1.0.0");
        this.newInstance.setComment("No comment");
        this.newInstance.setStatus(ServiceStatus.RELEASED);

        // Create an instance with an ID
        this.existingInstance = new Instance();
        this.existingInstance.setId(100L);
        this.existingInstance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.existingInstance.setName("Instance Name");
        this.existingInstance.setVersion("1.0.0");
        this.existingInstance.setComment("No comment");
        this.existingInstance.setStatus(ServiceStatus.RELEASED);
    }

    /**
     * Test that we can retrieve all the instances currently in the database in
     * a paged result.
     */
    @Test
    void testGetAllInstances() throws Exception {
        // Created a result page to be returned by the mocked service
        Page<Instance> page = new PageImpl<>(this.instances.subList(0, 5), this.pageable, this.instances.size());
        doReturn(page).when(this.instanceService).findAll(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/instances"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-Total-Count", Long.toString(page.getTotalElements())))
                .andExpect(header().exists(HttpHeaders.LINK))
                .andReturn();

        // Parse and validate the response
        InstanceDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), InstanceDto[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that the API supports the jQuery Datatables server-side paging
     * and search requests.
     */
    @Test
    void testGetInstancesForDatatables() throws Exception {
        // Create a test datatables paging request
        DtColumn dtColumn = new DtColumn("id");
        dtColumn.setName("ID");
        dtColumn.setOrderable(true);
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        DtPagingRequest dtPagingRequest = new DtPagingRequest();
        dtPagingRequest.setStart(0);
        dtPagingRequest.setLength(this.instances.size());
        dtPagingRequest.setDraw(1);
        dtPagingRequest.setSearch(new DtSearch());
        dtPagingRequest.setOrder(Collections.singletonList(dtOrder));
        dtPagingRequest.setColumns(Collections.singletonList(dtColumn));

        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleDatatablesPagingRequest(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/instances/dt")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(dtPagingRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        DtPage<InstanceDto> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DtPage.class);
        assertEquals(this.instances.size(), result.getData().size());
    }

    /**
     * Test that we can correctly retrieve a single instance based on the
     * provided entry ID.
     */
    @Test
    void testGetInstance() throws Exception {
        doReturn(this.existingInstance).when(this.instanceService).findOne(this.existingInstance.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/instances/{id}", this.existingInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        InstanceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), InstanceDto.class);
        assertNotNull(result);
        assertEquals(this.existingInstance.getId(), result.getId());
        assertEquals(this.existingInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.existingInstance.getVersion(), result.getVersion());
        assertEquals(this.existingInstance.getName(), result.getName());
        assertEquals(this.existingInstance.getStatus(), result.getStatus());
        assertEquals(this.existingInstance.getGeometry(), result.getGeometry());
    }

    /**
     * Test that if we do NOT find the instance we are looking for, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testGetInstanceNotFound() throws Exception {
        Long id = 0L;
        doThrow(new DataNotFoundException()).when(this.instanceService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/instances/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new instance correctly through a POST request.
     * The incoming instance should NOT have an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    void testPostInstance() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingInstance).when(this.instanceService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/instances")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.newInstance, InstanceDto.class))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        InstanceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), InstanceDto.class);
        assertNotNull(result);
        assertEquals(this.existingInstance.getId(), result.getId());
        assertEquals(this.existingInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.existingInstance.getVersion(), result.getVersion());
        assertEquals(this.existingInstance.getName(), result.getName());
        assertEquals(this.existingInstance.getStatus(), result.getStatus());
        assertEquals(this.existingInstance.getGeometry(), result.getGeometry());
    }

    /**
     * Test that if we try to create an instance with an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testPostInstanceWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/instances")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.existingInstance, InstanceDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can update an existing instance correctly through a PUT
     * request. The incoming instance should always have an ID.
     */
    @Test
    void testPutInstance() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingInstance).when(this.instanceService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.existingInstance, InstanceDto.class))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        InstanceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), InstanceDto.class);
        assertNotNull(result);
        assertEquals(this.existingInstance.getId(), result.getId());
        assertEquals(this.existingInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.existingInstance.getVersion(), result.getVersion());
        assertEquals(this.existingInstance.getName(), result.getName());
        assertEquals(this.existingInstance.getStatus(), result.getStatus());
        assertEquals(this.existingInstance.getGeometry(), result.getGeometry());
    }

    /**
     * Test that if we fail to update the provided instance due to an XML error,
     * an HTTP BAD_REQUEST response will be returned, with a description of
     * the error in the header.
     */
    @Test
    void testPutInstanceXMLFailure() throws Exception {
        // Mock an XML Validation exception when saving the instance
        doThrow(XMLValidationException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.existingInstance, InstanceDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that if we fail to update the provided instance due to a geometry
     * parse error, an HTTP BAD_REQUEST response will be returned, with a
     * description of the error in the header.
     */
    @Test
    void testPutInstanceGeometryParseFailure() throws Exception {
        // Mock a Geometry Parse exception when saving the instance
        doThrow(GeometryParseException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.existingInstance, InstanceDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that if we fail to update the provided instance due to a general
     * error, an HTTP BAD_REQUEST response will be returned, with a description
     * of the error in the header.
     */
    @Test
    void testPutInstanceGeneralFailure() throws Exception {
        // Mock a general Exception when saving the instance
        doThrow(RuntimeException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.instanceDomainToDtoMapper.convertTo(this.existingInstance, InstanceDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing instance by using a valid
     * ID.
     */
    @Test
    void testDeleteInstance() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the instance we are trying to delete, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteInstanceNotFound() throws Exception {
        doThrow(new DataNotFoundException()).when(this.instanceService).delete(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/instances/{id}", this.existingInstance.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can update an existing instance's status correctly through a
     * PUT request. The status values should be as specified in the G1128
     * specification.
     */
    @Test
    void testPutInstanceStatus() throws Exception {
        // Mock the service call for updating an existing instance
        doNothing().when(this.instanceService).updateStatus(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}/status", this.existingInstance.getId())
                .queryParam("status", ServiceStatus.DEPRECATED.value())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }
    /**
     * Test that if we do NOT provide a valid service instance status, a
     * bad request response will be returned.
     */
    @Test
    void testPutInstanceStatusError() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}/status", this.existingInstance.getId())
                .queryParam("status", "Wrong Value")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}