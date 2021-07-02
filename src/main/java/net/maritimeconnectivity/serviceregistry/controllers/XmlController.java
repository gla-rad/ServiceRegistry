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
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import net.maritimeconnectivity.serviceregistry.services.XmlService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Xml.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class XmlController {

    /**
     * The XML Service.
     */
    @Autowired
    private XmlService xmlService;

    /**
     * GET /xmls : get all the xmls.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of xmls in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(value = "/xmls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Xml>> getAllXmls(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Xmls");
        Page<Xml> page = this.xmlService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/xmls"))
                .body(page.getContent());
    }

    /**
     * GET /xmls/{id} : get the "ID" xml.
     *
     * @param id the ID of the xml to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the xml,
     * or with status 404 (Not Found)
     */
    @GetMapping(value = "/xmls/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xml> getXml(@PathVariable Long id) {
        log.debug("REST request to get Xml : {}", id);
        try {
            Xml result = this.xmlService.findOne(id);
            return ResponseEntity.ok()
                    .body(result);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    /**
     * POST /xmls : Create a new xml.
     *
     * @param xml the xml to create
     * @return the ResponseEntity with status 201 (Created) and with body the new xml,
     * or with status 400 (Bad Request) if the xml has already an ID, or couldn't be created
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/xmls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xml> createXml(@Valid @RequestBody Xml xml) throws URISyntaxException {
        log.debug("REST request to save Xml : {}", xml);
        if (xml.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("xml", "idexists", "A new xml cannot already have an ID"))
                    .build();
        }
        Xml result = this.xmlService.save(xml);
        return ResponseEntity.created(new URI("/api/xmls/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("xml", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT /xmls/{id} : Updates an existing xml.
     *
     * @param id the ID of the xml to be updated
     * @param xml the xml to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated xml,
     * or with status 400 (Bad Request) if the xml is not valid or couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/xmls/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Xml> updateXml(@PathVariable Long id, @Valid @RequestBody Xml xml) {
        log.debug("REST request to update Xml : {}", xml);
        xml.setId(id);
        Xml result = this.xmlService.save(xml);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("xml", xml.getId().toString()))
                .body(result);
    }

    /**
     * DELETE /xmls/{id} : delete the "id" xml.
     *
     * @param id the id of the xml to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/xmls/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteXml(@PathVariable Long id) {
        log.debug("REST request to delete Xml : {}", id);
        try {
            this.xmlService.delete(id);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("xml", id.toString()))
                .build();
    }

    /**
     * GET /xmls/schemas/{schema} : Returns the requested G1128 schema
     * specification, as it is loaded in the classpath.
     *
     * @return the ResponseEntity with status 200 (OK) and with body of the G1128 schema specification,
     * or with status 404 (Not Found) if the schema is not found
     */
    @GetMapping(value = "/xmls/schemas/{schema}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getG1128Schema(@PathVariable G1128Schemas schema) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8).toString());
        return Optional.of(schema)
                .map(sc -> getClass().getClassLoader().getResourceAsStream(sc.getPath()))
                .map(is -> {
                    try {
                        return IOUtils.toString(is, StandardCharsets.UTF_8.name());
                    } catch (IOException e) {
                        return null;
                    }
                })
                .map(xml -> ResponseEntity.ok().headers(responseHeaders).body(xml))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /xmls/validate/{schema{}} : Validates the provided XML input based
     * on the specified G1128 schema specification.
     *
     * @param content the xml content to be validated
     * @return the ResponseEntity with status 200 (OK) and with body of the parsed G1128-compliant object,
     * or with status 400 (Bad Request) if the content is invalid
     */
    @PostMapping(value = "/xmls/validate/{schema}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> validateXmlWithG1128Schema(@PathVariable G1128Schemas schema, @Valid @RequestBody String content) {
        log.debug("REST request to validate design xml : {}", content);
        try {
            return ResponseEntity.ok()
                    .body(this.xmlService.validate(content, schema));
        } catch (JAXBException | DataNotFoundException ex) {
            return ResponseEntity.badRequest()
                    .build();
        }
    }


}
