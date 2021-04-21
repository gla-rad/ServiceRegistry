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
import net.maritimeconnectivity.serviceregistry.models.domain.Design;
import net.maritimeconnectivity.serviceregistry.services.DesignService;
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
 * REST controller for managing Design.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class DesignController {

    @Autowired
    private DesignService designService;

    /**
     * POST  /designs : Create a new design.
     *
     * @param design the design to create
     * @return the ResponseEntity with status 201 (Created) and with body the new design, or with status 400 (Bad Request) if the design has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/designs",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Design> createDesign(@Valid @RequestBody Design design) throws URISyntaxException {
        log.debug("REST request to save Design : {}", design);
        if (design.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("design", "idexists", "A new design cannot already have an ID")).body(null);
        }
        if(design.getDesignAsXml() != null) {
            try {
                String xml = design.getDesignAsXml().getContent().toString();
                log.info("XML:" + xml);
                XmlUtil.validateXml(xml, "ServiceDesignSchema.xsd");
            } catch (Exception e) {
                log.error("Error parsing xml: ", e);
                return ResponseEntity.badRequest()
                        .headers(HeaderUtil.createFailureAlert("design", e.getMessage(), e.toString()))
                        .body(design);
            }
        }
        design.setPublishedAt(EntityUtils.getCurrentUTCTimeISO8601());
        design.setLastUpdatedAt(design.getPublishedAt());

        Design result = designService.save(design);
        return ResponseEntity.created(new URI("/api/designs/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("design", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /designs : Updates an existing design.
     *
     * @param design the design to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated design,
     * or with status 400 (Bad Request) if the design is not valid,
     * or with status 500 (Internal Server Error) if the design couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/designs",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Design> updateDesign(@Valid @RequestBody Design design) throws URISyntaxException {
        log.debug("REST request to update Design : {}", design);
        if (design.getId() == null) {
            return createDesign(design);
        }
        if(design.getDesignAsXml() != null) {
            try {
                String xml = design.getDesignAsXml().getContent().toString();
                log.info("XML:" + xml);
                XmlUtil.validateXml(xml, "ServiceDesignSchema.xsd");
            } catch (Exception e) {
                log.error("Error parsing xml: ", e);
                return ResponseEntity.badRequest()
                        .headers(HeaderUtil.createFailureAlert("design", e.getMessage(), e.toString()))
                        .body(design);
            }
        }

        design.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        if (design.getPublishedAt() == null) {
            design.setPublishedAt(design.getLastUpdatedAt());
        }

        Design result = designService.save(design);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("design", design.getId().toString()))
                .body(result);
    }

    /**
     * GET  /designs : get all the designs.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of designs in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/designs",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Design>> getAllDesigns(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Designs");
        Page<Design> page = designService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/designs");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /designs/:id : get the "id" design.
     *
     * @param id the id of the design to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the design, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/designs/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Design> getDesign(@PathVariable Long id) {
        log.debug("REST request to get Design : {}", id);
        Design design = designService.findOne(id);
        return Optional.ofNullable(design)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /designs/:id : delete the "id" design.
     *
     * @param id the id of the design to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/designs/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        log.debug("REST request to delete Design : {}", id);
        designService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("design", id.toString())).build();
    }

}
