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

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.McpBasicRestException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.services.LedgerRequestService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/ledgerrequests")
@Slf4j
@ConditionalOnProperty(value = "ledger.enabled", matchIfMissing = true)
public class LedgerRequestController {

    @Autowired
    private LedgerRequestService ledgerRequestService;

    @Autowired
    private InstanceService instanceService;

    /**
     * GET /api/ledgerrequests : get all the requests.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of instances in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LedgerRequest>> getLedgerRequests(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Instances");

        Page<LedgerRequest> page = this.ledgerRequestService.findAll(pageable);

        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/ledgerrequest"))
                .body(page.getContent());
    }

    /**
     * GET /api/ledgerrequests/{id} : get the "ID" ledger request.
     *
     * @param id the ID of the ledger request to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the instance,
     * or with status 404 (Not Found)
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> getLedgerRequest(@PathVariable Long id) {
        log.debug("REST request to get LedgerRequest : {}", id);
        try {
            LedgerRequest result = this.ledgerRequestService.findOne(id);
            return ResponseEntity.ok()
                    .body(result);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    /**
     * POST /api/ledgerrequests : Create a new ledger request.
     *
     * @param instanceId an instance information for the ledger request to create
     * @return the ResponseEntity with status 201 (Created) and with body the new request,
     * or with status 400 (Bad Request) if the request has already an ID, or coudln't be created
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> createLedgerRequest(@NotNull @RequestParam(name="instanceId") Long instanceId) throws URISyntaxException {
        log.info("REST request to create a LedgerRequest for instance ID : {}", instanceId);
        Instance instance;
        try{
            instance = instanceService.findOne(instanceId);
        } catch (DataNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("ledgerrequest", "idnotexists", e.getMessage()))
                    .build();
        }

        LedgerRequest request = new LedgerRequest();
        request.setId(0L);
        request.setServiceInstance(instance);
        LedgerRequest receivedRequest;
        try {
            receivedRequest = this.ledgerRequestService.save(request);
        } catch (Exception ex) {
            log.error("Unknown error: ", ex);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("ledgerrequest", ex.getMessage(), ex.toString()))
                    .body(request);
        }

        return ResponseEntity.created(new URI("/api/ledgerrequest/" + receivedRequest.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("ledgerrequest", receivedRequest.getId().toString()))
                .body(receivedRequest);
    }

    /**
     * PUT /api/ledgerrequests/{id} : Updates an existing "ID" ledger request.
     *
     * @param id the ID of the ledger request to be updated
     * @param status the status to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated instance,
     * or with status 400 (Bad Request) if the instance is not valid or couldn't be updated,
     */
    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> updateRequestStatus(@PathVariable Long id, @NotNull @RequestParam(name="status") LedgerRequestStatus status) throws DataNotFoundException {
        log.debug("REST request to update request {} status : {}", id, status.value());

        LedgerRequest request;
        try{
            request = ledgerRequestService.findOne(id);
        } catch (DataNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("ledgerrequest", "idnotexists", e.getMessage()))
                    .build();
        }

        request.setStatus(status);
        LedgerRequest receivedRequest;

        try {
            receivedRequest = this.ledgerRequestService.save(request);
        } catch (Exception ex) {
            log.error("Unknown error: ", ex);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("ledgerrequest", ex.getMessage(), ex.toString()))
                    .body(request);
        }

        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityStatusUpdateAlert("ledgerrequest", receivedRequest.getId().toString()))
                .body(receivedRequest);
    }

    /**
     * DELETE /api/ledgerrequests/{id} : delete the "ID" ledger request.
     *
     * @param id the ID of the ledger request to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        log.debug("REST request to delete ledger request : {}", id);
        try {
            // TODO: delete registered data from the MSR ledger
            this.ledgerRequestService.delete(id);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("ledgerrequest", id.toString()))
                .build();
    }

    /**
     * GET /api/ledgerrequest : Proceed to register the instance to the MSR ledger.
     *
     * @param id id of request
     * @return the ResponseEntity with status 202 (Accepted) when there is no problem,
     * or with status 400 (Bad Request) if corresponding instance does not exist or ledger request status is not VETTED
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(value = "/{id}/register")
    public ResponseEntity registerToLedger(@NotNull @PathVariable Long id) throws McpBasicRestException {
        try {
            LedgerRequest request = ledgerRequestService.registerInstanceToLedger(id);

            return ResponseEntity.accepted()
                    .body(request);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
    }
}
