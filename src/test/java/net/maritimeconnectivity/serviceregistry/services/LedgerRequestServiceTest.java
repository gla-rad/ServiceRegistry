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

import net.maritimeconnectivity.serviceregistry.components.SmartContractProvider;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.LedgerRegistrationError;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.repos.LedgerRequestRepo;
import net.maritimeconnectivity.serviceregistry.utils.MsrContract;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
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
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

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
    private MsrContract msrContract;

    // For the We3j transaction calls
    private RemoteFunctionCall remoteFunctionCall;
    private CompletableFuture completableFuture;
    private CompletableFuture completableFutureWithEx;
    private TransactionReceipt transactionReceipt;

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
            instance.setName("Test Instance No" + i);
            instance.setId(i+10);
            instance.setVersion("1.0.0");
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", instance.getId()));
            instance.setStatus(ServiceStatus.RELEASED);
            ledgerRequest.setServiceInstance(instance);

            // And append to the list
            this.ledgerRequests.add(ledgerRequest);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create an instance to link to the ledger requests
        this.instance = new Instance();
        this.instance.setName("Test Instance");
        this.instance.setId(123456L);
        this.instance.setVersion("1.0.0");
        this.instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", instance.getId()));
        this.instance.setStatus(ServiceStatus.RELEASED);

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
        this.existingLedgerRequest.setCreatedAt(LocalDateTime.now());
        this.existingLedgerRequest.setLastUpdatedAt(LocalDateTime.now());
        this.existingLedgerRequest.setServiceInstance(instance);

        // Mock an MSR smart contract
        this.msrContract = mock(MsrContract.class);

        // Mock the Web3j remote function call and response
        this.remoteFunctionCall = mock(RemoteFunctionCall.class);
        this.transactionReceipt = mock(TransactionReceipt.class);
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

        // Make sure the eager relationships' repo call was called
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

    /**
     * Test that we can retrieve a single ledger request entry based on the
     * ledger request instance ID and all the eager relationships are loaded.
     */
    @Test
    void testFindByInstanceId() {
        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());

        // Make sure the eager relationships' repo call was called
        verify(this.ledgerRequestRepo, times(1)).findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());

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
     * Test that if we do not find the ledger request by the instance ID, a
     * DataNotFoundException will be thrown.
     */
    @Test
    void testFindByInstanceIdNotFound() {
        doReturn(Optional.empty()).when(this.ledgerRequestRepo).findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId())
        );
    }

    /**
     * Test that we can correctly save a new ledger request when the provided
     * information is valid.
     */
    @Test
    void testSaveNew() {
        doReturn(this.instance).when(this.instanceService).findOne(this.newLedgerRequest.getServiceInstance().getId());
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestRepo).save(this.newLedgerRequest);

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.save(this.newLedgerRequest);

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
     * Test that we can correctly save a new ledger request when the provided
     * information is valid.
     */
    @Test
    void testSaveExisting() {
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.save(this.existingLedgerRequest);

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
     * Test that if we are trying to save an existing ledger request with an
     * invalid ID, a DataNotFoundException will be thrown.
     */
    @Test
    void testSaveNoValidId() {
        doReturn(Boolean.FALSE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.save(this.existingLedgerRequest)
        );
    }

    /**
     * Test that if we are trying to save an existing ledger request with an
     * invalid instance ID, a DataNotFoundException will be thrown.
     */
    @Test
    void testSaveNoValidInstanceId() {
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.save(this.existingLedgerRequest)
        );
    }

    /**
     * Test that we can successfully delete an existing ledger request.
     */
    @Test
    void testDelete() throws DataNotFoundException {
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());

        // Perform the service call
        this.ledgerRequestService.delete(this.existingLedgerRequest.getId());

        // Verify that a deletion call took place in the repository
        verify(this.ledgerRequestRepo, times(1)).deleteById(this.existingLedgerRequest.getId());
    }

    /**
     * Test that if we try to delete a non-existing ledger request, then a
     * DataNotFoundException will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.delete(this.existingLedgerRequest.getId())
        );
    }

    /**
     * Test that we can successfully delete an existing ledger request by its
     * instance ID.
     */
    @Test
    void testDeleteByInstanceId() {
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).findByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());

        // Perform the service call
        this.ledgerRequestService.deleteByInstanceId(this.existingLedgerRequest.getServiceInstance().getId());

        // Verify that a deletion call took place in the repository
        verify(this.ledgerRequestRepo, times(1)).deleteById(this.existingLedgerRequest.getId());
    }

    /**
     * Test that if we try to delete a non-existing ledger request by its
     * instance ID, then a DataNotFoundException will be thrown.
     */
    @Test
    void testDeleteByInstanceIdNotFound() {
        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.deleteByInstanceId(this.existingLedgerRequest.getServiceInstance().getId())
        );
    }

    /**
     * Test that we can update the ledger status of a ledger request, at least
     * for all the non-restricted values.
     */
    @Test
    void testStatusUpdate() {
        // Create a ledger request with the updated status
        LedgerRequest savedLedgerRequest = new LedgerRequest();
        savedLedgerRequest.setId(this.existingLedgerRequest.getId());
        savedLedgerRequest.setStatus(LedgerRequestStatus.VETTED);
        savedLedgerRequest.setReason("Testing the status update");
        savedLedgerRequest.setLastUpdatedAt(this.existingLedgerRequest.getLastUpdatedAt());
        savedLedgerRequest.setCreatedAt(this.existingLedgerRequest.getCreatedAt());
        savedLedgerRequest.setServiceInstance(this.existingLedgerRequest.getServiceInstance());

        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(savedLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.updateStatus(savedLedgerRequest.getId(), savedLedgerRequest.getStatus());

        // Test the result
        assertNotNull(result);
        assertEquals(savedLedgerRequest.getId(), result.getId());
        assertEquals(savedLedgerRequest.getStatus(), result.getStatus());
        assertEquals(savedLedgerRequest.getReason(), result.getReason());
        assertEquals(savedLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(savedLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(savedLedgerRequest.getServiceInstance().getId(), result.getServiceInstance().getId());
    }

    /**
     * Test that we can update the ledger status of a ledger request with a
     * reason explanation as well, at least for all the non-restricted values.
     */
    @Test
    void testStatusUpdateWithReason() {
        // Create a ledger request with the updated status
        LedgerRequest savedLedgerRequest = new LedgerRequest();
        savedLedgerRequest.setId(this.existingLedgerRequest.getId());
        savedLedgerRequest.setStatus(LedgerRequestStatus.VETTED);
        savedLedgerRequest.setReason("Testing the status update");
        savedLedgerRequest.setLastUpdatedAt(this.existingLedgerRequest.getLastUpdatedAt());
        savedLedgerRequest.setCreatedAt(this.existingLedgerRequest.getCreatedAt());
        savedLedgerRequest.setServiceInstance(this.existingLedgerRequest.getServiceInstance());

        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(savedLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);

        // Perform the service call
        LedgerRequest result = this.ledgerRequestService.updateStatus(savedLedgerRequest.getId(), savedLedgerRequest.getStatus(), savedLedgerRequest.getReason());

        // Test the result
        assertNotNull(result);
        assertEquals(savedLedgerRequest.getId(), result.getId());
        assertEquals(savedLedgerRequest.getStatus(), result.getStatus());
        assertEquals(savedLedgerRequest.getReason(), result.getReason());
        assertEquals(savedLedgerRequest.getLastUpdatedAt(), result.getLastUpdatedAt());
        assertEquals(savedLedgerRequest.getCreatedAt(), result.getCreatedAt());
        assertEquals(savedLedgerRequest.getServiceInstance().getId(), result.getServiceInstance().getId());
    }

    /**
     * Test that we do not allow updates of restricted ledger status values
     * directly from all publicly available class functions.
     */
    @Test
    void testStatusUpdateRestricted() {
        // Create a ledger request with the updated status
        LedgerRequest savedLedgerRequest = new LedgerRequest();
        savedLedgerRequest.setId(this.existingLedgerRequest.getId());
        savedLedgerRequest.setStatus(LedgerRequestStatus.REQUESTING);
        savedLedgerRequest.setReason("Testing the status update");
        savedLedgerRequest.setLastUpdatedAt(this.existingLedgerRequest.getLastUpdatedAt());
        savedLedgerRequest.setCreatedAt(this.existingLedgerRequest.getCreatedAt());
        savedLedgerRequest.setServiceInstance(this.existingLedgerRequest.getServiceInstance());

        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(this.msrContract).when(this.smartContractProvider).getMsrContract();

        // Perform the service calls
        assertThrows(LedgerRegistrationError.class, () ->
                this.ledgerRequestService.updateStatus(savedLedgerRequest.getId(), savedLedgerRequest.getStatus())
        );
        assertThrows(LedgerRegistrationError.class, () ->
                this.ledgerRequestService.updateStatus(savedLedgerRequest.getId(), savedLedgerRequest.getStatus(), savedLedgerRequest.getReason())
        );
    }

    /**
     * Test that internally we can try to update the ledger status of a ledger
     * request with a restricted status value.
     */
    @Test
    void testStatusUpdateForce() {
        // Create a ledger request with the updated status
        LedgerRequest savedLedgerRequest = new LedgerRequest();
        savedLedgerRequest.setId(this.existingLedgerRequest.getId());
        savedLedgerRequest.setStatus(LedgerRequestStatus.REQUESTING);
        savedLedgerRequest.setReason("Testing the status update");
        savedLedgerRequest.setLastUpdatedAt(this.existingLedgerRequest.getLastUpdatedAt());
        savedLedgerRequest.setCreatedAt(this.existingLedgerRequest.getCreatedAt());
        savedLedgerRequest.setServiceInstance(this.existingLedgerRequest.getServiceInstance());

        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.updateStatus(savedLedgerRequest.getId(), savedLedgerRequest.getStatus(), savedLedgerRequest.getReason(), true)
        );
    }

    /**
     * Test that if we try to update the ledger status of a ledger request with
     * an invalid ledger request ID, a DataNotFoundException will be thrown.
     */
    @Test
    void testStatusUpdateNoValidId() {
        // Create a ledger request with the updated status
        LedgerRequest savedLedgerRequest = new LedgerRequest();
        savedLedgerRequest.setId(this.existingLedgerRequest.getId());
        savedLedgerRequest.setStatus(LedgerRequestStatus.VETTED);
        savedLedgerRequest.setReason("Testing the status update");
        savedLedgerRequest.setLastUpdatedAt(this.existingLedgerRequest.getLastUpdatedAt());
        savedLedgerRequest.setCreatedAt(this.existingLedgerRequest.getCreatedAt());
        savedLedgerRequest.setServiceInstance(this.existingLedgerRequest.getServiceInstance());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.ledgerRequestService.updateStatus(this.existingLedgerRequest.getId(), this.newLedgerRequest.getStatus(), this.newLedgerRequest.getReason(), false)
        );
    }

    /**
     * Test that we can successfully register an instance to the ledger and
     * the response will distance the new ledger request status.
     */
    @Test
    void testRegisterInstanceToLedger() {
        // Create a future task to mimic the ledger response
        this.completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> completableFuture.complete(this.transactionReceipt));

        // Set the status to VETTED
        this.existingLedgerRequest.setStatus(LedgerRequestStatus.VETTED);

        doReturn(this.msrContract).when(this.smartContractProvider).getMsrContract();
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).findOne(this.existingLedgerRequest.getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);
        doCallRealMethod().when(this.smartContractProvider).createNewServiceInstance(any());

        // Mock the We3j transaction calls
        doReturn(this.remoteFunctionCall).when(this.msrContract).registerServiceInstance(any(), any());
        doReturn(this.completableFuture).when(this.remoteFunctionCall).sendAsync();
        doReturn("0x1").when(this.transactionReceipt).getStatus();

        // Perform the service call
        this.ledgerRequestService.registerInstanceToLedger(this.existingLedgerRequest.getId());

        // Make sure we updated the ledger request status twice, once with
        // requesting and once with succeeded after the ledger Web3j call.
        verify(this.ledgerRequestService, times(1)).updateStatus(this.existingLedgerRequest.getId(), LedgerRequestStatus.REQUESTING, null, Boolean.TRUE);
        //verify(this.ledgerRequestService, times(1)).updateStatus(eq(this.existingLedgerRequest.getId()), eq(LedgerRequestStatus.SUCCEEDED), any(String.class), eq(Boolean.TRUE));
    }

    /**
     * Test that we if we fail to successfully register an instance to the
     * ledger, the final status of the ledger request will be set to failed.
     */
    @Test
    void testRegisterInstanceToLedgerFailed() {
        // Create a future task to mimic the ledger response
        this.completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> completableFuture.complete(this.transactionReceipt));

        // Set the status to VETTED
        this.existingLedgerRequest.setStatus(LedgerRequestStatus.VETTED);

        doReturn(this.msrContract).when(this.smartContractProvider).getMsrContract();
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).findOne(this.existingLedgerRequest.getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);
        doCallRealMethod().when(this.smartContractProvider).createNewServiceInstance(any());

        // Mock the We3j transaction calls
        doReturn(this.remoteFunctionCall).when(this.msrContract).registerServiceInstance(any(), any());
        doReturn(this.completableFuture).when(this.remoteFunctionCall).sendAsync();
        doReturn("0x2").when(this.transactionReceipt).getStatus();

        // Perform the service call
        this.ledgerRequestService.registerInstanceToLedger(this.existingLedgerRequest.getId());

        // Make sure we updated the ledger request status twice, once with
        // requesting and once with failure after the ledger Web3j call.
        verify(this.ledgerRequestService, times(1)).updateStatus(this.existingLedgerRequest.getId(), LedgerRequestStatus.REQUESTING, null, Boolean.TRUE);
        //verify(this.ledgerRequestService, times(1)).updateStatus(eq(this.existingLedgerRequest.getId()), eq(LedgerRequestStatus.FAILED), any(String.class), eq(Boolean.TRUE));
    }

    /**
     * Test that we if we fail to successfully register an instance to the
     * ledger with an exception, the final status of the ledger request will be
     * set to failed.
     */
    @Test
    void testRegisterInstanceToLedgerWithException() {
        // Create a future task to mimic the ledger response
        this.completableFutureWithEx = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> completableFutureWithEx.completeExceptionally(new Throwable("Something went wrong")));

        // Set the status to REQUESTING
        this.existingLedgerRequest.setStatus(LedgerRequestStatus.VETTED);

        doReturn(this.msrContract).when(this.smartContractProvider).getMsrContract();
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestService).findOne(this.existingLedgerRequest.getId());
        doReturn(Boolean.TRUE).when(this.ledgerRequestRepo).existsById(this.existingLedgerRequest.getId());
        doReturn(Optional.of(this.existingLedgerRequest)).when(this.ledgerRequestRepo).findById(this.existingLedgerRequest.getId());
        doReturn(this.instance).when(this.instanceService).findOne(this.existingLedgerRequest.getServiceInstance().getId());
        doReturn(this.existingLedgerRequest).when(this.ledgerRequestRepo).save(this.existingLedgerRequest);
        doCallRealMethod().when(this.smartContractProvider).createNewServiceInstance(any());

        // Mock the We3j transaction calls
        doReturn(this.remoteFunctionCall).when(this.msrContract).registerServiceInstance(any(), any());
        doReturn(this.completableFutureWithEx).when(this.remoteFunctionCall).sendAsync();

        // Perform the service call
        this.ledgerRequestService.registerInstanceToLedger(this.existingLedgerRequest.getId());

        // Make sure we updated the ledger request status twice, once with
        // requesting and once with failure after the ledger Web3j call.
        verify(this.ledgerRequestService, times(1)).updateStatus(this.existingLedgerRequest.getId(), LedgerRequestStatus.REQUESTING, null, Boolean.TRUE);
        //verify(this.ledgerRequestService).updateStatus(eq(this.existingLedgerRequest.getId()), eq(LedgerRequestStatus.FAILED), any(String.class), eq(Boolean.TRUE));
        verify(this.ledgerRequestService, times(2)).updateStatus(any(), any(), any(), any());
    }
}
