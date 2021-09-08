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
import net.maritimeconnectivity.serviceregistry.components.SmartContractProvider;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.services.LedgerRequestService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/ledgerrequests")
@Slf4j
@ConditionalOnBean(SmartContractProvider.class)
public class LedgerRequestController {

    /**
     * The Ledger Request Service.
     */
    @Autowired
    private LedgerRequestService ledgerRequestService;

    /**
     * The Instance Service.
     */
    @Autowired
    private InstanceService instanceService;

    /**
     * GET /api/ledgerrequests : get all the ledger requests.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of ledger requests in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LedgerRequest>> getLedgerRequests(Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of LedgerRequests");
        final Page<LedgerRequest> page = this.ledgerRequestService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/ledgerrequest"))
                .body(page.getContent());
    }

    /**
     * GET /api/ledgerrequests/{id} : get the "ID" ledger request.
     *
     * @param id the ID of the ledger request to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the ledger request
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> getLedgerRequest(@PathVariable Long id) {
        log.debug("REST request to get LedgerRequest : {}", id);
        final LedgerRequest result = this.ledgerRequestService.findOne(id);
        return ResponseEntity.ok()
                .body(result);
    }

    /**
     * POST /api/ledgerrequests : Create a new ledger request.
     *
     * @param request the ledger request to be created
     * @return the ResponseEntity with status 201 (Created) and with body the new ledger request
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> createLedgerRequest(@Valid @RequestBody LedgerRequest request) throws URISyntaxException {
        log.info("REST request to create a LedgerRequest for instance ID : {}", request);
        if (request.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("ledgerrequest", "idexists", "A new ledger request cannot already have an ID"))
                    .build();
        }
        final LedgerRequest receivedRequest = this.ledgerRequestService.save(request);
        return ResponseEntity.created(new URI("/api/ledgerrequest/" + receivedRequest.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("ledgerrequest", receivedRequest.getId().toString()))
                .body(receivedRequest);
    }

    /**
     * PUT /api/ledgerrequests/{id}/status : Updates the status of an existing
     * "ID" ledger request.
     *
     * @param id the ID of the ledger request to be updated
     * @param status the updated status
     * @return the ResponseEntity with status 200 (OK) and with body the updated ledger request
     */
    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LedgerRequest> updateRequestStatus(@PathVariable Long id, @NotNull @RequestParam(name="status") LedgerRequestStatus status) throws DataNotFoundException {
        log.debug("REST request to update request {} status : {}", id, status.value());
        final LedgerRequest ledgerRequest = this.ledgerRequestService.updateStatus(id, status);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityStatusUpdateAlert("ledgerrequest", ledgerRequest.getId().toString()))
                .body(ledgerRequest);
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
        this.ledgerRequestService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("ledgerrequest", id.toString()))
                .build();
    }

    /**
     * PUT /api/ledgerrequest/{id}/register : Proceed to register the "ID"
     * ledger request to the MSR ledger.
     *
     * @param id id of request
     * @return the ResponseEntity with status 202 (Accepted) when there is no problem
     */
    @PutMapping(value = "/{id}/register")
    public ResponseEntity registerToLedger(@PathVariable Long id) {
        log.debug("REST request to register to the MSR ledger the request : {}", id);
        final LedgerRequest request = ledgerRequestService.registerInstanceToLedger(id);
        return ResponseEntity.accepted()
                .body(request);
    }

}
