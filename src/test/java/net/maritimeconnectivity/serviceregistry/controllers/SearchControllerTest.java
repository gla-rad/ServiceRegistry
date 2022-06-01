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
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = SearchController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class SearchControllerTest {

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

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the instances list
        this.instances = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            Instance instance = new Instance();
            instance.setId(i);
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", i));
            instance.setName(String.format("Test Instance %d", i));
            this.instances.add(instance);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);
    }

    /**
     * Test that we can search for instances using the search API endpoint that
     * supports Lucene queries and a paged result using GeoJSON geometries.
     */
    @Test
    void testSearchInstancesGeoJSON() throws Exception {
        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleSearchQueryRequest(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/_search/instances")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("queryString", "name:Test")
                        .param("geometry", "{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"LineString\",\"coordinates\":[[0,50],[0,52]]}]}")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        List<InstanceDto> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), List.class);
        assertEquals(this.instances.size(), result.size());
    }

    /**
     * Test that we can search for instances using the search API endpoint that
     * supports Lucene queries and a paged result using WKT geometries.
     */
    @Test
    void testSearchInstancesWKT() throws Exception {
        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleSearchQueryRequest(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/_search/instances")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("queryString", "name:Test")
                        .param("geometryWKT", "LINESTRING ( 0 50, 0 52 )")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        InstanceDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), InstanceDto[].class);
        assertEquals(this.instances.size(), result.length);
    }

    /**
     * Test that if we attempt to search for instances using multiple geometry
     * specifications (i.e. GeoJSON and WKT) a bad request will be returned
     */
    @Test
    void testSearchInstancesMultipleGeometries() throws Exception {
        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleSearchQueryRequest(any(), any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/_search/instances")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("queryString", "name:Test")
                        .param("geometry", "{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"LineString\",\"coordinates\":[[0,50],[0,52]]}]}")
                        .param("geometryWKT", "LINESTRING ( 0 50, 0 52 )")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

}