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

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.SmartContractProvider;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.LedgerConnectionException;
import net.maritimeconnectivity.serviceregistry.exceptions.LedgerRegistrationError;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.repos.LedgerRequestRepo;
import net.maritimeconnectivity.serviceregistry.utils.EntityUtils;
import net.maritimeconnectivity.serviceregistry.utils.MsrContract;
import net.maritimeconnectivity.serviceregistry.utils.MsrErrorConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;

import static net.maritimeconnectivity.serviceregistry.utils.StreamUtils.peek;

/**
 * Service Implementation for managing Ledger Requests in the database.
 *
 * This service is optional:
 *  To disable add the "ledger.enabled=false" in the application properties.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
@ConditionalOnBean(SmartContractProvider.class)
public class LedgerRequestService {

    /**
     * The Ledger Request Repo.
     */
    @Autowired
    LedgerRequestRepo ledgerRequestRepo;

    /**
     * The Instance Service.
     */
    @Autowired
    InstanceService instanceService;

    /**
     * The Ledger Smart Component Provider.
     */
    @Autowired
    SmartContractProvider smartContractProvider;

    /**
     * Get all the ledger requests.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<LedgerRequest> findAll(Pageable pageable) {
        log.debug("Request to get all LedgerRequests");
        return this.ledgerRequestRepo.findAll(pageable);
    }

    /**
     * Get one ledger request by ID.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public LedgerRequest findOne(@NotNull Long id) {
        log.debug("Request to delete LedgerRequest : {}", id);
        return this.ledgerRequestRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException(String.format("No LedgerRequest found for the provided ID {}", id), null));
    }

    /**
     * Get all the ledger requests by instance ID.
     *
     * @param instanceId the ID of the instance to find the ledger request for
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public LedgerRequest findByInstanceId(@NotNull Long instanceId){
        log.debug("Request to delete LedgerRequest related to Instance ID : {}", instanceId);
        return this.ledgerRequestRepo.findByInstanceId(instanceId)
                .orElseThrow(() -> new DataNotFoundException(String.format("No LedgerRequest found for the provided Instance ID {}", instanceId), null));
    }

    /**
     * Save a ledger request.
     *
     * @param request the entity to save
     * @return the persisted entity
     */
    @Transactional
    public LedgerRequest save(@NotNull LedgerRequest request) {
        // First validate the object
        this.validateRequestForSave(request);

        // If the submission date is missing
        if(StringUtils.isBlank(request.getCreatedAt())) {
            request.setCreatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // And don't forget the last update
        if(StringUtils.isBlank(request.getLastUpdatedAt())) {
            request.setLastUpdatedAt(request.getCreatedAt());
        }
        else{
            request.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // The save and return
        return this.ledgerRequestRepo.save(request);
    }

    /**
     * Delete ledger request by ID.
     *
     * @param id the id of the entity
     */
    @Transactional
    public void delete(@NotNull Long id) {
        Optional.of(id)
                .filter(this.ledgerRequestRepo::existsById)
                .ifPresentOrElse(i -> {
                    this.ledgerRequestRepo.deleteById(id);
                }, () -> {
                    throw new DataNotFoundException(String.format("No LedgerRequest found for the provided ID {}", id), null);
                });
    }

    /**
     * Delete the ledger request by the linked instance ID.
     *
     * @param instanceId the instance ID to delete the entity for
     */
    @Transactional(propagation = Propagation.NESTED)
    public void deleteByInstanceId(@NotNull Long instanceId) {
        log.debug("Request to delete LedgerRequest related to instance ID : {}", instanceId);
        Optional.of(instanceId)
                .map(this::findByInstanceId)
                .map(LedgerRequest::getId)
                .ifPresent(this::delete);
    }

    /**
     * Update the status of LedgerRequest by id.
     *
     * @param id     the id of the entity
     * @param status the status of the entity
     * @throws Exception any exceptions thrown while updating the status
     */
    @Transactional
    public LedgerRequest updateStatus(@NotNull Long id, @NotNull LedgerRequestStatus status) {
        return updateStatus(id, status, null);
    }

    /**
     * Update the status of LedgerRequest by id.
     *
     * @param id     the id of the entity
     * @param status the status of the entity
     * @throws Exception any exceptions thrown while updating the status
     */
    @Transactional
    public LedgerRequest updateStatus(@NotNull Long id, @NotNull LedgerRequestStatus status, String reason) {
        log.debug("Request to update status of LedgerRequest : {}", id);

        // Try to find if the instance does indeed exist
        LedgerRequest request = this.ledgerRequestRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException(String.format("No LedgerRequest found for the provided ID {}", id), null));

        // Update the instance status and reason if applicable
        request.setStatus(status);
        Optional.ofNullable(reason)
                .filter(StringUtils::isNotBlank)
                .ifPresent(request::setReason);

        // Finally, save and return
        return save(request);
    }

    /**
     * Performs the registration of a local service instance to the global MSR
     * ledger if that is currently activates and connected. Note that this is
     * the primary goal of this service, and without the smart contract that
     * connects us to the ledger, this service doesn't event initialise.
     *
     * @param id    The ID of the entity
     * @return The updated ledger request pending the result
     */
    public LedgerRequest registerInstanceToLedger(Long id) {
        // Make sure we actually have a valid connection to
        final MsrContract msrContract = Optional.ofNullable(this.smartContractProvider)
                .filter(SmartContractProvider::isMsrContractConnected)
                .map(SmartContractProvider::getMsrContract)
                .orElseThrow(() -> new LedgerConnectionException(MsrErrorConstant.LEDGER_NOT_CONNECTED, null));

        // Now get the ledger request, update it and contact the MSR ledger
        return Optional.of(id)
                .map(this::findOne)
                .map(peek(l -> Optional.of(l)
                        .map(LedgerRequest::getStatus)
                        .filter(LedgerRequestStatus.VETTED::equals)
                        .orElseThrow(() -> new LedgerRegistrationError(MsrErrorConstant.LEDGER_REQUEST_STATUS_NOT_FULFILLED + "- current status: " + l.getStatus(), null))))
                .map(peek(l -> this.updateStatus(l.getId(), LedgerRequestStatus.REQUESTING)))
                .map(this::save)
                .map(peek(l -> {
                    msrContract.registerServiceInstance(this.smartContractProvider.createNewServiceInstance(l.getServiceInstance()),
                                    l.getServiceInstance().getKeywordsList())
                            .sendAsync()
                            .whenComplete((receipt, ex) -> this.handleLedgerRegistrationResponse(l, receipt, ex));
                }))
                .orElseThrow(() -> new DataNotFoundException(MsrErrorConstant.LEDGER_REQUEST_NOT_FOUND, null));
    }

    /**
     * Prepare LedgerRequest for save.
     *
     * Essentially this validation function is supposed to check whether the
     * provided ledger request object conforms to all the standards for being
     * persisted in the database.
     *
     * @param request the ledger request to be saved
     * @throws DataNotFoundException If the ledger request or the referenced instance is invalid
     */
    protected void validateRequestForSave(LedgerRequest request) {
        // Validate the ledger request ID
        if(Objects.nonNull(request.getId())) {
            Optional.of(request)
                    .map(LedgerRequest::getId)
                    .filter(this.ledgerRequestRepo::existsById)
                    .orElseThrow(() -> new DataNotFoundException(String.format("No LedgerRequest found for the provided ID {}", request.getId()), null));
        }

        // Validate the instance link
        Optional.of(request)
                .map(LedgerRequest::getServiceInstance)
                .map(Instance::getId)
                .map(instanceService::findOne)
                .orElseThrow(() -> new DataNotFoundException("No valid Instance for the provided LedgerRequest", null));
    }

    /**
     * This helper function handles the ledger registration operation
     * responses. It is designed as a BiConsumer that handles the response
     * of a asynchronous call completion, hence a throwable is also present
     * in the input argument list.
     *
     * @param transactionReceipt    The ledger transaction receipt
     * @param ex                    Any exception that might have been thrown during the transaction
     */
    protected void handleLedgerRegistrationResponse(LedgerRequest ledgerRequest, TransactionReceipt transactionReceipt, Throwable ex) {
        if (Objects.isNull(ex)) {
            final Instance instance = ledgerRequest.getServiceInstance();
            if (transactionReceipt.getStatus().equals("0x1")) {
                log.info("Instance is successfully registered to the ledger - instance name: " + instance.getName());
                this.updateStatus(ledgerRequest.getId(), LedgerRequestStatus.SUCCEEDED);
            } else {
                log.error(MsrErrorConstant.LEDGER_REGISTRATION_FAILED + " - instance name: " + instance.getName());
                this.updateStatus(ledgerRequest.getId(), LedgerRequestStatus.FAILED);
            }
        } else {
            log.error(MsrErrorConstant.LEDGER_REGISTRATION_FAILED + " - ", ex.getMessage(), ex);
            this.updateStatus(ledgerRequest.getId(), LedgerRequestStatus.FAILED);
        }
    }

}
