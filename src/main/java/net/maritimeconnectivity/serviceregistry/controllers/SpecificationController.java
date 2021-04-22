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
import net.maritimeconnectivity.serviceregistry.models.domain.Specification;
import net.maritimeconnectivity.serviceregistry.services.SpecificationService;
import net.maritimeconnectivity.serviceregistry.utils.EntityUtils;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import net.maritimeconnectivity.serviceregistry.utils.XmlUtil;
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
 * REST controller for managing Specification.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    /**
     * POST  /specifications : Create a new specification.
     *
     * @param specification the specification to create
     * @return the ResponseEntity with status 201 (Created) and with body the new specification, or with status 400 (Bad Request) if the specification has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specifications",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Specification> createSpecification(@Valid @RequestBody Specification specification) throws URISyntaxException {
        log.debug("REST request to save Specification : {}", specification);
        if (specification.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("specification", "idexists", "A new specification cannot already have an ID")).body(null);
        }
        try {
            String xml = specification.getSpecAsXml().getContent().toString();
            log.info("XML:" + xml);
            XmlUtil.validateXml(xml, "ServiceSpecificationSchema.xsd");
        } catch (Exception e) {
            log.error("Error parsing xml: ", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("specification", e.getMessage(), e.toString()))
                    .body(specification);
        }
        specification.setPublishedAt(EntityUtils.getCurrentUTCTimeISO8601());
        specification.setLastUpdatedAt(specification.getPublishedAt());
        Specification result = specificationService.save(specification);
        return ResponseEntity.created(new URI("/api/specifications/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("specification", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /specifications : Updates an existing specification.
     *
     * @param specification the specification to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated specification,
     * or with status 400 (Bad Request) if the specification is not valid,
     * or with status 500 (Internal Server Error) if the specification couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/specifications",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Specification> updateSpecification(@Valid @RequestBody Specification specification) throws URISyntaxException {
        log.debug("REST request to update Specification : {}", specification);
        if (specification.getId() == null) {
            return createSpecification(specification);
        }
        try {
            String xml = specification.getSpecAsXml().getContent().toString();
            log.info("XML:" + xml);
            XmlUtil.validateXml(xml, "ServiceSpecificationSchema.xsd");
        } catch (Exception e) {
            log.error("Error parsing xml: ", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("specification", e.getMessage(), e.toString()))
                    .body(specification);
        }
        specification.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        if (specification.getPublishedAt() == null) {
            specification.setPublishedAt(specification.getLastUpdatedAt());
        }

        Specification result = specificationService.save(specification);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("specification", specification.getId().toString()))
                .body(result);
    }

    /**
     * GET  /specifications : get all the specifications.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of specifications in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/specifications",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Specification>> getAllSpecifications(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Specifications");
        Page<Specification> page = specificationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/specifications");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /specifications/:id : get the "id" specification.
     *
     * @param id the id of the specification to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the specification, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/specifications/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Specification> getSpecification(@PathVariable Long id) {
        log.debug("REST request to get Specification : {}", id);
        Specification specification = specificationService.findOne(id);
        return Optional.ofNullable(specification)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /specifications/:id : delete the "id" specification.
     *
     * @param id the id of the specification to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/specifications/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteSpecification(@PathVariable Long id) {
        log.debug("REST request to delete Specification : {}", id);
        specificationService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("specification", id.toString())).build();
    }

}
