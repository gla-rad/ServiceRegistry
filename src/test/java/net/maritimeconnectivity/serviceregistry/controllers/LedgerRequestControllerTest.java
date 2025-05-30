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
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.models.dto.LedgerRequestDto;
import net.maritimeconnectivity.serviceregistry.services.LedgerRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.text.ParseException;
import java.time.LocalDateTime;
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
@WebMvcTest(controllers = LedgerRequestController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class LedgerRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public DomainDtoMapper ledgerRequestDomainToDtoMapper;

    @MockitoBean
    private LedgerRequestService ledgerRequestService;

    // Test Variables
    private List<LedgerRequest> ledgerRequests;
    private Pageable pageable;
    private Instance instance;
    private LedgerRequest newLedgerRequest;
    private LedgerRequest existingLedgerRequest;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws ParseException {
        // Initialise the ledger requests list
        this.ledgerRequests = new ArrayList<>();
        for(long i=0; i<10; i++) {
            LedgerRequest ledgerRequest = new LedgerRequest();
            ledgerRequest.setId(i);
            ledgerRequest.setStatus(LedgerRequestStatus.CREATED);
            ledgerRequest.setReason("Some reason");
            ledgerRequest.setCreatedAt(LocalDateTime.now());
            ledgerRequest.setLastUpdatedAt(LocalDateTime.now());

            // Add the instance link
            Instance instance = new Instance();
            instance.setId(i+10);
            instance.setVersion("1.0.0");
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", instance.getId()));
            ledgerRequest.setServiceInstance(instance);

            // And append to the list
            this.ledgerRequests.add(ledgerRequest);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create an instance to link to the ledger requests
        this.instance = new Instance();
        this.instance.setId(123456L);
        this.instance.setVersion("1.0.0");
        this.instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", instance.getId()));

        // Create a new ledger request
        this.newLedgerRequest = new LedgerRequest();
        this.newLedgerRequest.setStatus(LedgerRequestStatus.CREATED);
        this.newLedgerRequest.setReason("Some reason");
        this.newLedgerRequest.setServiceInstance(instance);

        // Create an existing ledger request
        this.existingLedgerRequest = new LedgerRequest();
        this.existingLedgerRequest.setId(100L);
        this.existingLedgerRequest.setStatus(LedgerRequestStatus.CREATED);
        this.existingLedgerRequest.setReason("Some reason");
        this.existingLedgerRequest.setServiceInstance(instance);
    }

    /**
     * Test that we can retrieve all the ledger requests currently in the
     * database in a paged result.
     */
    @Test
    void testGetAllLedgerRequests() throws Exception {
        // Created a result page to be returned by the mocked service
        Page<LedgerRequest> page = new PageImpl<>(this.ledgerRequests.subList(0, 5), this.pageable, this.ledgerRequests.size());
        doReturn(page).when(this.ledgerRequestService).findAll(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/ledgerrequests"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-Total-Count", Long.toString(page.getTotalElements())))
                .andExpect(header().exists(HttpHeaders.LINK))
                .andReturn();

        // Parse and validate the response
        LedgerRequestDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), LedgerRequestDto[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that we can correctly retrieve a single ledger request based on the
     * provided entry ID.
     */
    @Test
    void testGetLedgerRequest() throws Exception {
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).findOne(this.existingLedgerRequest.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/ledgerrequests/{id}", this.existingLedgerRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        LedgerRequestDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), LedgerRequestDto.class);
        assertNotNull(result);
        assertEquals(this.existingLedgerRequest.getId(), result.getId());
        assertEquals(this.existingLedgerRequest.getStatus(), result.getStatus());
        assertEquals(this.existingLedgerRequest.getReason(), result.getReason());
        assertEquals(this.existingLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(this.existingLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.existingLedgerRequest.getServiceInstance().getId(), result.getServiceInstanceId());
    }

    /**
     * Test that if we do NOT find the ledger request we are looking for, an
     * HTTP NOT_FOUND response will be returned.
     */
    @Test
    void testGetLedgerRequestNotFound() throws Exception {
        Long id = 0L;
        doThrow(new DataNotFoundException()).when(this.ledgerRequestService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/ledgerrequests/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new ledger request correctly through a POST
     * request. The incoming ledger request should NOT have an ID, while the
     * returned value will have the ID field populated.
     */
    @Test
    void testPostLedgerRequest() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/ledgerrequests")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.ledgerRequestDomainToDtoMapper.convertTo(this.newLedgerRequest, LedgerRequestDto.class))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        LedgerRequestDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), LedgerRequestDto.class);
        assertNotNull(result);
        assertEquals(this.existingLedgerRequest.getId(), result.getId());
        assertEquals(this.existingLedgerRequest.getStatus(), result.getStatus());
        assertEquals(this.existingLedgerRequest.getReason(), result.getReason());
        assertEquals(this.existingLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(this.existingLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.existingLedgerRequest.getServiceInstance().getId(), result.getServiceInstanceId());
    }

    /**
     * Test that if we try to create a ledger request with an existing ID
     * field, an HTTP BAD_REQUEST response will be returns, with a description
     * of the error in the header.
     */
    @Test
    void testPostLedgerRequestWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/ledgerrequests")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.ledgerRequestDomainToDtoMapper.convertTo(this.existingLedgerRequest, LedgerRequestDto.class))))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-mcsrApp-error"))
                .andExpect(header().exists("X-mcsrApp-params"))
                .andReturn();
    }

    /**
     * Test that we can update the status of an existing ledger request
     * correctly through a PUT request. The incoming ledger request should
     * always have an ID.
     */
    @Test
    void testPutLedgerRequestStatus() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).updateStatus(any(), eq(LedgerRequestStatus.VETTED));

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/ledgerrequests/{id}/status?status={status}",
                                this.existingLedgerRequest.getId(),
                                LedgerRequestStatus.VETTED.name())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.ledgerRequestDomainToDtoMapper.convertTo(this.existingLedgerRequest, LedgerRequestDto.class))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        LedgerRequestDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), LedgerRequestDto.class);
        assertNotNull(result);
        assertEquals(this.existingLedgerRequest.getId(), result.getId());
        assertEquals(this.existingLedgerRequest.getStatus(), result.getStatus());
        assertEquals(this.existingLedgerRequest.getReason(), result.getReason());
        assertEquals(this.existingLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(this.existingLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.existingLedgerRequest.getServiceInstance().getId(), result.getServiceInstanceId());
    }

    /**
     * Test that we can correctly delete an existing ledger request by using a
     * valid ID.
     */
    @Test
    void testDeleteLedgerRequest() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/ledgerrequests/{id}", this.existingLedgerRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the ledger request we are trying to delete,
     * an HTTP NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteLedgerRequestNotFound() throws Exception {
        doThrow(new DataNotFoundException()).when(this.ledgerRequestService).delete(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/ledgerrequests/{id}", this.existingLedgerRequest.getId()))
                .andExpect(status().isNotFound());
    }

}