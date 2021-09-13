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

import com.fasterxml.jackson.core.JsonProcessingException;
import net.maritimeconnectivity.serviceregistry.components.SmartContractProvider;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.repos.LedgerRequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerRequestServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    private LedgerRequestService ledgerRequestService;

    /**
     * The Ledger Smart Component Provider mock.
     */
    @Mock
    SmartContractProvider smartContractProvider;

    /**
     * The Instance Service mock.
     */
    @Mock
    InstanceService instanceService;

    /**
     * The Ledger Request Repo mock.
     */
    @Mock
    LedgerRequestRepo ledgerRequestRepo;

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
    void setUp() throws JsonProcessingException {
        // Initialise the ledger requests list
        this.ledgerRequests = new ArrayList<>();
        for(long i=0; i<10; i++) {
            LedgerRequest ledgerRequest = new LedgerRequest();
            ledgerRequest.setId(i);
            ledgerRequest.setStatus(LedgerRequestStatus.CREATED);
            ledgerRequest.setReason("Some reason");
            ledgerRequest.setLastUpdatedAt("02/01/2001");
            ledgerRequest.setCreatedAt("01/01/2001");

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
        this.newLedgerRequest.setLastUpdatedAt("02/01/2001");
        this.newLedgerRequest.setCreatedAt("01/01/2001");
        this.newLedgerRequest.setServiceInstance(instance);

        // Create an existing ledger request
        this.existingLedgerRequest = new LedgerRequest();
        this.existingLedgerRequest.setId(100L);
        this.existingLedgerRequest.setStatus(LedgerRequestStatus.CREATED);
        this.existingLedgerRequest.setReason("Some reason");
        this.existingLedgerRequest.setLastUpdatedAt("02/01/2001");
        this.existingLedgerRequest.setCreatedAt("01/01/2001");
        this.existingLedgerRequest.setServiceInstance(instance);
    }

    /**
     * Test that we can retrieve all the ledger requests currently present in
     * the database through a paged call.
     */
    @Test
    void testFindAll() {
        // Created a result page to be returned by the mocked repository
        Page<LedgerRequest> page = new PageImpl<>(this.ledgerRequests.subList(0, 5), this.pageable, this.ledgerRequests.size());
        doReturn(page).when(this.ledgerRequestRepo).findAll(this.pageable);

        // Perform the service call
        Page<LedgerRequest> result = this.ledgerRequestService.findAll(pageable);

        // Test the result
        assertEquals(page.getSize(), result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getSize(); i++){
            assertEquals(result.getContent().get(i), this.ledgerRequests.get(i));
        }
    }

    /**
     * Test that we can retrieve a single ledger request entry based on the
     * ledger request ID and all the eager relationships are loaded.
     */
    @Test
    void testFindOne() throws DataNotFoundException {
        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.findOne(this.existingLedgerRequest.getId());

        // Make sure the eager relationships repo call was called
        verify(this.ledgerRequestRepo, times(1)).findById(this.existingLedgerRequest.getId());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingLedgerRequest.getId(), result.getId());
        assertEquals(this.existingLedgerRequest.getStatus(), result.getStatus());
        assertEquals(this.existingLedgerRequest.getReason(), result.getReason());
        assertEquals(this.existingLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(this.existingLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.existingLedgerRequest.getServiceInstance().getId(), result.getServiceInstance().getId());
    }

    /**
     * Test that if we do not find the ledger request we are looking for, a
     * DataNotFoundException will be thrown.
     */
    @Test
    void testFindOneNotFound() {
        doReturn(Optional.empty()).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.findOne(this.existingLedgerRequest.getId())
        );
    }

}