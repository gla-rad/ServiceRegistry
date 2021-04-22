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
import net.maritimeconnectivity.serviceregistry.models.domain.SpecificationTemplateSet;
import net.maritimeconnectivity.serviceregistry.services.SpecificationTemplateSetService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Specification Template Set.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class SpecificationTemplateSetController {

    @Autowired
    private SpecificationTemplateSetService specificationTemplateSetService;

    /**
     * POST  /specification-template-sets : Create a new specificationTemplateSet.
     *
     * @param specificationTemplateSet the specificationTemplateSet to create
     * @return the ResponseEntity with status 201 (Created) and with body the new specificationTemplateSet, or with status 400 (Bad Request) if the specificationTemplateSet has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specification-template-sets",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplateSet> createSpecificationTemplateSet(@Valid @RequestBody SpecificationTemplateSet specificationTemplateSet) throws URISyntaxException {
        log.debug("REST request to save SpecificationTemplateSet : {}", specificationTemplateSet);
        if (specificationTemplateSet.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("specificationTemplateSet", "idexists", "A new specificationTemplateSet cannot already have an ID")).body(null);
        }
        SpecificationTemplateSet result = this.specificationTemplateSetService.save(specificationTemplateSet);
        return ResponseEntity.created(new URI("/api/specification-template-sets/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("specificationTemplateSet", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /specification-template-sets : Updates an existing specificationTemplateSet.
     *
     * @param specificationTemplateSet the specificationTemplateSet to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated specificationTemplateSet,
     * or with status 400 (Bad Request) if the specificationTemplateSet is not valid,
     * or with status 500 (Internal Server Error) if the specificationTemplateSet couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specification-template-sets",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplateSet> updateSpecificationTemplateSet(@Valid @RequestBody SpecificationTemplateSet specificationTemplateSet) throws URISyntaxException {
        log.debug("REST request to update SpecificationTemplateSet : {}", specificationTemplateSet);
        if (specificationTemplateSet.getId() == null) {
            return createSpecificationTemplateSet(specificationTemplateSet);
        }
        SpecificationTemplateSet result = this.specificationTemplateSetService.save(specificationTemplateSet);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("specificationTemplateSet", specificationTemplateSet.getId().toString()))
                .body(result);
    }

    /**
     * GET  /specification-template-sets : get all the specificationTemplateSets.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of specificationTemplateSets in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/specification-template-sets",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SpecificationTemplateSet>> getAllSpecificationTemplateSets(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of SpecificationTemplateSets");
        Page<SpecificationTemplateSet> page = this.specificationTemplateSetService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/specification-template-sets");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /specification-template-sets/:id : get the "id" specificationTemplateSet.
     *
     * @param id the id of the specificationTemplateSet to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the specificationTemplateSet, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/specification-template-sets/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplateSet> getSpecificationTemplateSet(@PathVariable Long id) {
        log.debug("REST request to get SpecificationTemplateSet : {}", id);
        SpecificationTemplateSet specificationTemplateSet = this.specificationTemplateSetService.findOne(id);
        return Optional.ofNullable(specificationTemplateSet)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /specification-template-sets/:id : delete the "id" specificationTemplateSet.
     *
     * @param id the id of the specificationTemplateSet to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/specification-template-sets/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteSpecificationTemplateSet(@PathVariable Long id) {
        log.debug("REST request to delete SpecificationTemplateSet : {}", id);
        this.specificationTemplateSetService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("specificationTemplateSet", id.toString())).build();
    }
}
