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
import net.maritimeconnectivity.serviceregistry.models.domain.SpecificationTemplate;
import net.maritimeconnectivity.serviceregistry.services.SpecificationTemplateService;
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
 * REST controller for managing Specification Template.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class SpecificationTemplateController {

    @Autowired
    private SpecificationTemplateService specificationTemplateService;

    /**
     * POST  /specification-templates : Create a new specificationTemplate.
     *
     * @param specificationTemplate the specificationTemplate to create
     * @return the ResponseEntity with status 201 (Created) and with body the new specificationTemplate, or with status 400 (Bad Request) if the specificationTemplate has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specification-templates",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplate> createSpecificationTemplate(@Valid @RequestBody SpecificationTemplate specificationTemplate) throws URISyntaxException {
        log.debug("REST request to save SpecificationTemplate : {}", specificationTemplate);
        if (specificationTemplate.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("specificationTemplate", "idexists", "A new specificationTemplate cannot already have an ID")).body(null);
        }
        SpecificationTemplate result = this.specificationTemplateService.save(specificationTemplate);
        return ResponseEntity.created(new URI("/api/specification-templates/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("specificationTemplate", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /specification-templates : Updates an existing specificationTemplate.
     *
     * @param specificationTemplate the specificationTemplate to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated specificationTemplate,
     * or with status 400 (Bad Request) if the specificationTemplate is not valid,
     * or with status 500 (Internal Server Error) if the specificationTemplate couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specification-templates",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplate> updateSpecificationTemplate(@Valid @RequestBody SpecificationTemplate specificationTemplate) throws URISyntaxException {
        log.debug("REST request to update SpecificationTemplate : {}", specificationTemplate);
        if (specificationTemplate.getId() == null) {
            return createSpecificationTemplate(specificationTemplate);
        }
        SpecificationTemplate result = this.specificationTemplateService.save(specificationTemplate);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("specificationTemplate", specificationTemplate.getId().toString()))
                .body(result);
    }

    /**
     * GET  /specification-templates : get all the specificationTemplates.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of specificationTemplates in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/specification-templates",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SpecificationTemplate>> getAllSpecificationTemplates(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of SpecificationTemplates");
        Page<SpecificationTemplate> page = this.specificationTemplateService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/specification-templates");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /specification-templates/:id : get the "id" specificationTemplate.
     *
     * @param id the id of the specificationTemplate to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the specificationTemplate, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/specification-templates/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecificationTemplate> getSpecificationTemplate(@PathVariable Long id) {
        log.debug("REST request to get SpecificationTemplate : {}", id);
        SpecificationTemplate specificationTemplate = this.specificationTemplateService.findOne(id);
        return Optional.ofNullable(specificationTemplate)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /specification-templates/:id : delete the "id" specificationTemplate.
     *
     * @param id the id of the specificationTemplate to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/specification-templates/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteSpecificationTemplate(@PathVariable Long id) {
        log.debug("REST request to delete SpecificationTemplate : {}", id);
        this.specificationTemplateService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("specificationTemplate", id.toString())).build();
    }
}
