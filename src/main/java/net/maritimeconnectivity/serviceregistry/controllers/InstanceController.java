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
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.EntityUtils;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Instance.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class InstanceController {

    /**
     * The Instance Service.
     */
    @Autowired
    private InstanceService instanceService;

    /**
     * GET  /instances : get all the instances.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of instances in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Instance>> getAllInstances(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Instances");
        Page<Instance> page = this.instanceService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/instances"))
                .body(page.getContent());
    }

    /**
     * GET  /instances/{id} : get the "ID" instance.
     *
     * @param id the ID of the instance to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the instance, or with status 404 (Not Found)
     */
    @GetMapping(value = "/instances/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Instance> getInstance(@PathVariable Long id) {
        log.debug("REST request to get Instance : {}", id);
        return Optional.ofNullable(this.instanceService.findOne(id))
                .map(ResponseEntity.ok()::body)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST  /instances : Create a new instance.
     *
     * @param instance the instance to create
     * @return the ResponseEntity with status 201 (Created) and with body the new instance, or with status 400 (Bad Request) if the instance has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Instance> createInstance(@Valid @RequestBody Instance instance) throws URISyntaxException {
        log.debug("REST request to save Instance : {}", instance);
        if (instance.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", "idexists", "A new instance cannot already have an ID"))
                    .body(null);
        }
        instance.setPublishedAt(EntityUtils.getCurrentUTCTimeISO8601());
        instance.setLastUpdatedAt(instance.getPublishedAt());

        return saveInstance(instance, true);
    }

    /**
     * PUT  /instances/{id} : Updates an existing "ID" instance.
     *
     * @param id the ID of the instance to be updated
     * @param instance the instance to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated instance,
     * or with status 400 (Bad Request) if the instance is not valid,
     * or with status 500 (Internal Server Error) if the instance couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/instances/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Instance> updateInstance(@PathVariable Long id, @RequestBody Instance instance) throws URISyntaxException {
        log.debug("REST request to update Instance : {}", instance);
        instance.setId(id);
        instance.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        if (instance.getPublishedAt() == null) {
            instance.setPublishedAt(instance.getLastUpdatedAt());
        }

        return saveInstance(instance, false);
    }

    /**
     * DELETE  /instances/{id} : delete the "ID" instance.
     *
     * @param id the ID of the instance to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/instances/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        log.debug("REST request to delete Instance : {}", id);
        this.instanceService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("instance", id.toString()))
                .build();
    }

    /**
     * A helper function that performs the actual instance saving operation and
     * handles and issues.
     *
     * @param instance the instance to be save
     * @param newInstance Whether this is a new instance
     * @return the save instance
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    private ResponseEntity<Instance> saveInstance(Instance instance, boolean newInstance) throws URISyntaxException {
        try {
            instance = this.instanceService.save(instance);
        } catch (XMLValidationException e) {
            log.error("Error parsing xml: ", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", e.getMessage(), e.toString()))
                    .body(instance);
        } catch (GeometryParseException e) {
            log.error("Error parsing geometry: ", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", e.getMessage(), e.toString()))
                    .body(instance);
        } catch (Exception e) {
            log.error("Unknown error: ", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("instance", e.getMessage(), e.toString()))
                    .body(instance);
        }

        return (newInstance ? ResponseEntity.created(new URI("/api/instances/" + instance.getId())) : ResponseEntity.ok())
                .headers(HeaderUtil.createEntityUpdateAlert("instance", instance.getId().toString()))
                .body(instance);
    }


}
