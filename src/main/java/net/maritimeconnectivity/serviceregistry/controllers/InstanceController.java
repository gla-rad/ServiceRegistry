/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDtDto;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPage;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.modelmapper.PropertyMap;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing Instance.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api/instances")
@Slf4j
public class InstanceController {

    /**
     * The Instance Service.
     */
    @Autowired
    InstanceService instanceService;

    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, InstanceDto> instanceDomainToDtoMapper;

    /**
     * Object Mapper from DTO to Domain.
     */
    @Autowired
    DomainDtoMapper<InstanceDto, Instance> instanceDtoToDomainMapper;

    /**
     * Object Mapper from Domain to Datatable DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, InstanceDtDto> instanceDomainToDtDtoMapper;

    /**
     * Setup up addition model mapper configurations.
     */
    @PostConstruct
    void setup() {
        this.instanceDtoToDomainMapper.getModelMapper().addMappings(new PropertyMap<InstanceDto, Instance>() {
            @Override
            protected void configure() {
                map(source.getLedgerRequestId()).setLedgerRequest(null);
            }
        });
        this.instanceDomainToDtoMapper.getModelMapper().addMappings(new PropertyMap<Instance, InstanceDto>() {
            @Override
            protected void configure() {
                map(source.getImplementsDesign()).setImplementsServiceDesign(null);
                map(source.getImplementsDesignVersion()).setImplementsServiceDesignVersion(null);
            }
        });
        this.instanceDomainToDtDtoMapper.getModelMapper().addMappings(new PropertyMap<Instance, InstanceDtDto>() {
            @Override
            protected void configure() {
                map(source.getImplementsDesign()).setImplementsServiceDesign(null);
                map(source.getImplementsDesignVersion()).setImplementsServiceDesignVersion(null);
            }
        });
    }

    /**
     * GET /api/instances : get all the instances.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of instances in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InstanceDto>> getInstances(@ParameterObject Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of Instances");
        final Page<Instance> page = this.instanceService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/instances"))
                .body(this.instanceDomainToDtoMapper.convertToList(page.getContent(), InstanceDto.class));
    }

    /**
     * POST /api/instances/dt : Returns a paged list of all current instances
     * for the datatables display.
     *
     * @param dtPagingRequest the datatables paging request
     * @return the ResponseEntity with status 200 (OK) and the list of stations in body
     */
    @Hidden
    @PostMapping(value = "/dt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DtPage<InstanceDtDto>> getInstancesForDatatables(@RequestBody DtPagingRequest dtPagingRequest) {
        log.debug("REST request to get page of Instances for datatables");
        final Page<Instance> page = this.instanceService.handleDatatablesPagingRequest(dtPagingRequest);
        return ResponseEntity.ok()
                .body(this.instanceDomainToDtDtoMapper.convertToDtPage(page, dtPagingRequest, InstanceDtDto.class));
    }

    /**
     * GET /api/instances/{id} : get the "ID" instance.
     *
     * @param id the ID of the instance to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the instance
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceDto> getInstance(@PathVariable Long id) {
        log.debug("REST request to get Instance : {}", id);
        final Instance result = this.instanceService.findOne(id);
        return ResponseEntity.ok()
                .body(this.instanceDomainToDtoMapper.convertTo(result, InstanceDto.class));
    }

    /**
     * GET /api/instances/mrn/{mrn} : get instances by MRN
     *
     * @param mrn the instance ID of the instances to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the instance
     */
    @GetMapping(value = "/mrn/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InstanceDto>> getInstancesByMRN(@PathVariable String mrn) {
        log.debug("REST request to get a list of Instances with mrn {}", mrn);
        final List<Instance> result = this.instanceService.findAllByDomainId(mrn);
        return ResponseEntity.ok()
                .body(this.instanceDomainToDtoMapper.convertToList(result, InstanceDto.class));
    }

    /**
     * GET /api/instances/mrn/{mrn}/{version} : get an instance by MRN and version.
     *
     * @param mrn the instance ID of the instance to retrieve
     * @param version the instance ID of the instance to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the instance
     */
    @GetMapping(value = "/mrn/{mrn}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceDto> getInstanceByMRNAndVersion(@PathVariable String mrn, @PathVariable String version) {
        log.debug("REST request to get Instance with mrn {} and version {}", mrn, version);
        final Instance result = this.instanceService.findByDomainIdAndVersion(mrn, version);
        return ResponseEntity.ok()
                .body(this.instanceDomainToDtoMapper.convertTo(result, InstanceDto.class));
    }

    /**
     * POST /api/instances : Create a new instance.
     *
     * @param instanceDto the instance to create
     * @return the ResponseEntity with status 201 (Created) and with body the new instance, or with status 400 (Bad Request) if the instance has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceDto> createInstance(@Valid @RequestBody InstanceDto instanceDto) throws URISyntaxException {
        log.debug("REST request to save Instance : {}", instanceDto);
        if (instanceDto.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", "idexists", "A new instance cannot already have an ID"))
                    .build();
        }
        return this.saveInstance(this.instanceDtoToDomainMapper.convertTo(instanceDto, Instance.class), true);
    }

    /**
     * PUT /api/instances/{id} : Updates an existing "ID" instance.
     *
     * @param id the ID of the instance to be updated
     * @param instanceDto the instance to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated instance
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceDto> updateInstance(@PathVariable Long id, @Valid @RequestBody InstanceDto instanceDto) throws URISyntaxException {
        log.debug("REST request to update Instance : {}", instanceDto);
        instanceDto.setId(id);
        ResponseEntity<InstanceDto> response = saveInstance(this.instanceDtoToDomainMapper.convertTo(instanceDto, Instance.class), false);
        return response;
    }

    /**
     * DELETE /api/instances/{id} : delete the "ID" instance.
     *
     * @param id the ID of the instance to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        log.debug("REST request to delete Instance : {}", id);
        this.instanceService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("instance", id.toString()))
                .build();
    }

    /**
     * PUT /api/instances/{id}/status : Updates the "ID" instance status
     *
     * @param id the ID of the instance to be updated
     * @param status the new status value
     * @return the ResponseEntity with status 200 (OK), or with status 400 (Bad Request) if the instance status couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateInstanceStatus(@PathVariable Long id, @NotNull @RequestParam(name="status") ServiceStatus status) {
        log.debug("REST request to update instance {} status : {}", id, status.value());

        try {
            this.instanceService.updateStatus(id, status);
        } catch (XMLValidationException ex) {
            log.error("Error parsing xml: ", ex);
            return ResponseEntity.badRequest()
                    .build();
        } catch (GeometryParseException ex) {
            log.error("Error parsing geometry: ", ex);
            return ResponseEntity.badRequest()
                    .build();
        } catch (Exception ex) {
            log.error("Update status error: ", ex);
            return ResponseEntity.badRequest()
                    .build();
        }

        // Return an OK response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityStatusUpdateAlert("instance", id.toString()))
                .build();
    }

    /**
     * PUT /api/instances/{id}/ledger-status : Updates the "ID" instance ledger
     * status.
     *
     * @param id the ID of the instance to be updated
     * @param ledgerRequestStatus the new ledger status value
     * @return the ResponseEntity with status 200 (OK), or with status 400 (Bad Request) if the instance ledger status couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/{id}/ledger-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateInstanceLedgerStatus(@PathVariable Long id, @NotNull @RequestParam(name="ledgerStatus") LedgerRequestStatus ledgerRequestStatus) {
        log.debug("REST request to update instance {} ledger status : {}", id, ledgerRequestStatus.value());

        // Update the instance's ledger status
        this.instanceService.updateLedgerStatus(id, ledgerRequestStatus, null);

        // Return an OK response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityStatusUpdateAlert("instance", id.toString()))
                .build();
    }

    /**
     * A helper function that performs the actual instance saving operation and
     * handles and issues.
     *
     * @param instance the instance to be save
     * @param newInstance Whether this is a new instance
     * @return the saved instance
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    private ResponseEntity<InstanceDto> saveInstance(Instance instance, boolean newInstance) throws URISyntaxException {
        try {
            instance = this.instanceService.save(instance);
        } catch (XMLValidationException ex) {
            log.error("Error parsing xml: ", ex);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", ex.getMessage(), ex.toString()))
                    .body(this.instanceDomainToDtoMapper.convertTo(instance, InstanceDto.class));
        } catch (GeometryParseException ex) {
            log.error("Error parsing geometry: ", ex);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", ex.getMessage(), ex.toString()))
                    .body(this.instanceDomainToDtoMapper.convertTo(instance, InstanceDto.class));
        } catch (Exception ex) {
            log.error("Saving error: ", ex);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", ex.getMessage(), ex.toString()))
                    .body(this.instanceDomainToDtoMapper.convertTo(instance, InstanceDto.class));
        }

        return newInstance ?
                ResponseEntity.created(new URI("/api/instances/" + instance.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("instance", instance.getId().toString()))
                        .body(this.instanceDomainToDtoMapper.convertTo(instance, InstanceDto.class)) :
                ResponseEntity.ok()
                        .headers(HeaderUtil.createEntityUpdateAlert("instance", instance.getId().toString()))
                        .body(this.instanceDomainToDtoMapper.convertTo(instance, InstanceDto.class));
    }

}
