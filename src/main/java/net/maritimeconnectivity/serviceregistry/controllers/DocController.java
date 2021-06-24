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
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.services.DocService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing Doc.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class DocController {

    /**
     * Definition of the allows Doc Content Types.
     */
    @Value("${net.maritimeconnectivity.serviceregistry.allowedContentTypes:application/pdf}")
    List<String> allowedContentTypes;

    /**
     * The Doc Service.
     */
    @Autowired
    private DocService docService;

    /**
     * GET  /docs : get all the docs.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of docs in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Doc>> getAllDocs(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Docs");
        Page<Doc> page = this.docService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/docs"))
                .body(page.getContent());
    }

    /**
     * GET  /docs/:id : get the "ID" doc.
     *
     * @param id the ID of the doc to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the doc,
     * or with status 404 (Not Found)
     */
    @GetMapping(value = "/docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Doc> getDoc(@PathVariable Long id) {
        log.debug("REST request to get Doc : {}", id);
        try {
            Doc result = this.docService.findOne(id);
            return ResponseEntity.ok()
                    .body(result);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    /**
     * POST  /docs : Create a new doc.
     *
     * @param doc the doc to create
     * @return the ResponseEntity with status 201 (Created) and with body the new doc,
     * or with status 400 (Bad Request) if the doc has already an ID, or couldn't be created
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Doc> createDoc(@Valid @RequestBody Doc doc) throws URISyntaxException {
        log.debug("REST request to save Doc : {}", doc);
        if (doc.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "idexists", "A new doc cannot already have an ID"))
                    .build();
        }
        if (this.allowedContentTypes.stream().noneMatch(ft -> ft.equalsIgnoreCase(doc.getFilecontentContentType()))) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "formaterror", "Unsupported document format. Only PDF, ODT or DOCX are allowed."))
                    .build();
        }
        Doc result = this.docService.save(doc);
        return ResponseEntity.created(new URI("/api/docs/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("doc", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /docs/{id} : Updates an existing doc.
     *
     * @param id the ID of the doc to be updated
     * @param doc the doc to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated doc,
     * or with status 400 (Bad Request) if the doc is not valid or couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Doc> updateDoc(@PathVariable Long id, @Valid @RequestBody Doc doc) {
        log.debug("REST request to update Doc : {}", doc);
        if (this.allowedContentTypes.stream().noneMatch(ft -> ft.equalsIgnoreCase(doc.getFilecontentContentType()))) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "formaterror", "Unsupported document format. Only PDF, ODT or DOCX are allowed."))
                    .build();
        }
        doc.setId(id);
        Doc result = this.docService.save(doc);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("doc", doc.getId().toString()))
                .body(result);
    }

    /**
     * DELETE  /docs/{id} : delete the "ID" doc.
     *
     * @param id the ID of the doc to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteDoc(@PathVariable Long id) {
        log.debug("REST request to delete Doc : {}", id);
        try {
            this.docService.delete(id);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.notFound()
                    .build();
        }
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("doc", id.toString()))
                .build();
    }

}
