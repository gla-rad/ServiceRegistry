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
import net.maritimeconnectivity.serviceregistry.models.domain.Xsd;
import net.maritimeconnectivity.serviceregistry.services.XsdService;
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
 * REST controller for managing Xsd.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class XsdController {

    @Autowired
    private XsdService xsdService;

    /**
     * POST  /xsds : Create a new xsd.
     *
     * @param xsd the xsd to create
     * @return the ResponseEntity with status 201 (Created) and with body the new xsd, or with status 400 (Bad Request) if the xsd has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/xsds",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xsd> createXsd(@Valid @RequestBody Xsd xsd) throws URISyntaxException {
        log.debug("REST request to save Xsd : {}", xsd);
        if (xsd.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("xsd", "idexists", "A new xsd cannot already have an ID")).body(null);
        }
        Xsd result = this.xsdService.save(xsd);
        return ResponseEntity.created(new URI("/api/xsds/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("xsd", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /xsds : Updates an existing xsd.
     *
     * @param xsd the xsd to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated xsd,
     * or with status 400 (Bad Request) if the xsd is not valid,
     * or with status 500 (Internal Server Error) if the xsd couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/xsds",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xsd> updateXsd(@Valid @RequestBody Xsd xsd) throws URISyntaxException {
        log.debug("REST request to update Xsd : {}", xsd);
        if (xsd.getId() == null) {
            return createXsd(xsd);
        }
        Xsd result = this.xsdService.save(xsd);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("xsd", xsd.getId().toString()))
                .body(result);
    }

    /**
     * GET  /xsds : get all the xsds.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of xsds in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/xsds",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Xsd>> getAllXsds(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Xsds");
        Page<Xsd> page = this.xsdService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/xsds");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /xsds/:id : get the "id" xsd.
     *
     * @param id the id of the xsd to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the xsd, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/xsds/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xsd> getXsd(@PathVariable Long id) {
        log.debug("REST request to get Xsd : {}", id);
        Xsd xsd = this.xsdService.findOne(id);
        return Optional.ofNullable(xsd)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /xsds/:id : delete the "id" xsd.
     *
     * @param id the id of the xsd to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/xsds/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteXsd(@PathVariable Long id) {
        log.debug("REST request to delete Xsd : {}", id);
        this.xsdService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("xsd", id.toString())).build();
    }

}
