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
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = InstanceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class InstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    public void testGetAllInstances() throws Exception {
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
        Instance[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Instance[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that we can correctly retrieve a single instance based on the
     * provided entry ID.
     */
    @Test
    public void testGetInstance() throws Exception {
        doReturn(this.existingInstance).when(this.instanceService).findOne(this.existingInstance.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/instances/{id}", this.existingInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        Instance result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Instance.class);
        assertEquals(this.existingInstance, result);
    }

    /**
     * Test that if we do NOT find the instance we are looking for, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    public void testGetInstanceNotFound() throws Exception {
        Long id = 0L;
        doThrow(DataNotFoundException.class).when(this.instanceService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/instances/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new instance correctly through a POST request.
     * The incoming instance should NOT has an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    public void testPostInstance() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingInstance).when(this.instanceService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/instances")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.newInstance)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        Instance result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Instance.class);
        assertEquals(this.existingInstance, result);
    }

    /**
     * Test that if we try to create an instance with an existing ID field,
     * an HTTP BAR_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    public void testPostInstanceWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/instances")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.existingInstance)))
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
    public void testPutInstance() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingInstance).when(this.instanceService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.existingInstance)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        Instance result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Instance.class);
        assertEquals(this.existingInstance, result);
    }

    /**
     * Test that if we fail to update the provided instance due to an XML error,
     * an HTTP BAD_REQUEST response will be returned, with a description of
     * the error in the header.
     */
    @Test
    public void testPutInstanceXMLFailure() throws Exception {
        // Mock an XML Validation exception when saving the instance
        doThrow(XMLValidationException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.existingInstance)))
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
    public void testPutInstanceGeometryParseFailure() throws Exception {
        // Mock a Geometry Parse exception when saving the instance
        doThrow(GeometryParseException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.existingInstance)))
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
    public void testPutInstanceGeneralFailure() throws Exception {
        // Mock a general Exception when saving the instance
        doThrow(RuntimeException.class).when(this.instanceService).save(any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}", this.existingInstance.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(this.existingInstance)))
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
    public void testDeleteInstance() throws Exception {
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
    public void testDeleteInstanceNotFound() throws Exception {
        doThrow(DataNotFoundException.class).when(this.instanceService).delete(any());

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
    public void testPutInstanceStatus() throws Exception {
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
    public void testPutInstanceStatusError() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(put("/api/instances/{id}/status", this.existingInstance.getId())
                .queryParam("status", "Wrong Value")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}