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
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.repos.LedgerRequestRepo;
import net.maritimeconnectivity.serviceregistry.utils.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

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
@ConditionalOnProperty(value = "ledger.enabled", matchIfMissing = true)
public class LedgerRequestDBService {

    /**
     * The Instance Service.
     */
    @Autowired
    InstanceService instanceService;

    /**
     * The Ledger Request Repo.
     */
    @Autowired
    LedgerRequestRepo ledgerRequestRepo;

    /**
     * Get all the LedgerRequests.
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
     * Get one LedgerRequest by ID.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public LedgerRequest findOne(@NotNull Long id) throws DataNotFoundException {
        log.debug("Request to delete LedgerRequest : {}", id);
        return this.ledgerRequestRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException("No ledger request found for the provided ID", null));
    }

    /**
     * Get all the requests by instance domain ID.
     *
     * @param domainId the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<LedgerRequest> findByDomainID(@NotNull String domainId){
        log.debug("Request to delete LedgerRequest related to Instance domain ID : {}", domainId);
        return this.ledgerRequestRepo.findByDomainId(domainId);
    }

    /**
     * Save a LedgerRequest.
     *
     * @param request the entity to save
     * @return the persisted entity
     */
    @Transactional
    public LedgerRequest save(@NotNull LedgerRequest request) throws DataNotFoundException {
        // First validate the object
        this.validateRequestForSave(request);

        // If the submission date is missing
        if(request.getCreatedAt() == null || request.getCreatedAt().length() == 0) {
            request.setCreatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // And don't forget the last update
        if(request.getLastUpdatedAt() == null || request.getLastUpdatedAt().length() == 0) {
            request.setLastUpdatedAt(request.getCreatedAt());
        }
        else{
            request.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // The save and return
        return this.ledgerRequestRepo.save(request);
    }

    /**
     * Delete LedgerRequest by id.
     *
     * @param id the id of the entity
     */
    @Transactional
    public void delete(@NotNull Long id) throws DataNotFoundException {
        if(this.ledgerRequestRepo.existsById(id)) {
            this.ledgerRequestRepo.deleteById(id);
        } else {
            throw new DataNotFoundException("No LedgerRequest found for the provided ID", null);
        }
    }

    /**
     * Update the status of LedgerRequest by id.
     *
     * @param id     the id of the entity
     * @param status the status of the entity
     * @throws Exception any exceptions thrown while updating the status
     */
    @Transactional
    public LedgerRequest updateStatus(@NotNull Long id, @NotNull LedgerRequestStatus status) throws DataNotFoundException{
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
    public LedgerRequest updateStatus(@NotNull Long id, @NotNull LedgerRequestStatus status, String reason) throws DataNotFoundException{
        log.debug("Request to update status of LedgerRequest : {}", id);

        // Try to find if the instance does indeed exist
        LedgerRequest request = this.ledgerRequestRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException("No ledger request found for the provided ID", null));

        // Update the instance status
        try {
            request.setStatus(status);
            if (reason != null){
                request.setReason(reason);
            }
            return save(request);
        } catch (Exception e) {
            log.error("Problem during ledger request status update.", e);
            throw e;
        }
    }

    /**
     * Prepare LedgerRequest for save.
     *
     * @param request the ledger request to be saved
     * @throws DataNotFoundException If fails first phase (Validating existence of instance)
     */
    protected void validateRequestForSave(LedgerRequest request) throws DataNotFoundException {
        if(request == null) {
            return;
        }

        // Try to find the instance if an ID of instance is provided
        if(request.getServiceInstance() != null) {
            Optional.of(request.getServiceInstance().getId())
                    .map(instanceService::findOne)
                    .orElseThrow(() -> new DataNotFoundException("No instance found for the provided ID", null));
        } else {
            return;
        }
    }
}
