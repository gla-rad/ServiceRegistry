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
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.DocDto;
import net.maritimeconnectivity.serviceregistry.models.dto.DocDtDto;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDtDto;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPage;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
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
@RequestMapping("/api/docs")
@Slf4j
public class DocController {

    /**
     * Definition of the allows Doc Content Types.
     */
    @Value("${net.maritimeconnectivity.serviceregistry.allowedContentTypes:application/pdf}")
    List<String> allowedContentTypes;

    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Doc, DocDto> docDomainToDtoMapper;

    /**
     * Object Mapper from DTO to Domain.
     */
    @Autowired
    DomainDtoMapper<DocDto, Doc> docDtoToDomainMapper;

    /**
     * Object Mapper from Domain to Datatable DTO.
     */
    @Autowired
    DomainDtoMapper<Doc, DocDtDto> docDomainToDtDtoMapper;

    /**
     * The Doc Service.
     */
    @Autowired
    private DocService docService;

    /**
     * GET /api/docs : get all the docs.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of docs in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocDto>> getDocs(Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of Docs");
        final Page<Doc> page = this.docService.findAll(pageable);
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/docs"))
                .body(this.docDomainToDtoMapper.convertToList(page.getContent(), DocDto.class));
    }

    /**
     * POST /api/docs/dt : Returns a paged list of all current docs for the
     * datatables display.
     *
     * @param dtPagingRequest the datatables paging request
     * @return the ResponseEntity with status 200 (OK) and the list of stations in body
     */
    @PostMapping(value = "/dt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DtPage<DocDtDto>> getDocsForDatatables(@RequestParam Long instanceId, @RequestBody DtPagingRequest dtPagingRequest) {
        log.debug("REST request to get page of Instances for datatables");
        final Page<Doc> page = this.docService.handleDatatablesPagingRequest(instanceId, dtPagingRequest);
        return ResponseEntity.ok()
                .body(this.docDomainToDtDtoMapper.convertToDtPage(page, dtPagingRequest, DocDtDto.class));
    }

    /**
     * GET /api/docs/:id : get the "ID" doc.
     *
     * @param id the ID of the doc to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the doc,
     * or with status 404 (Not Found)
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocDto> getDoc(@PathVariable Long id) {
        log.debug("REST request to get Doc : {}", id);
        final Doc result = this.docService.findOne(id);
        return ResponseEntity.ok()
                .body(this.docDomainToDtoMapper.convertTo(result, DocDto.class));
    }

    /**
     * POST /api/docs : Create a new doc.
     *
     * @param docDto the doc to create
     * @return the ResponseEntity with status 201 (Created) and with body the new doc,
     * or with status 400 (Bad Request) if the doc has already an ID, or couldn't be created
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocDto> createDoc(@Valid @RequestBody DocDto docDto) throws URISyntaxException {
        log.debug("REST request to save Doc : {}", docDto);
        if (docDto.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "idexists", "A new doc cannot already have an ID"))
                    .build();
        }
        if (this.allowedContentTypes.stream().noneMatch(ft -> ft.equalsIgnoreCase(docDto.getFilecontentContentType()))) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "formaterror", "Unsupported document format. Only PDF, ODT or DOCX are allowed."))
                    .build();
        }
        final Doc result = this.docService.save(this.docDtoToDomainMapper.convertTo(docDto, Doc.class));
        return ResponseEntity.created(new URI("/api/docs/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("doc", result.getId().toString()))
                .body(this.docDomainToDtoMapper.convertTo(result, DocDto.class));
    }

    /**
     * PUT /api/docs/{id} : Updates an existing doc.
     *
     * @param id the ID of the doc to be updated
     * @param docDto the doc to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated doc,
     * or with status 400 (Bad Request) if the doc is not valid or couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocDto> updateDoc(@PathVariable Long id, @Valid @RequestBody DocDto docDto) {
        log.debug("REST request to update Doc : {}", docDto);
        if (this.allowedContentTypes.stream().noneMatch(ft -> ft.equalsIgnoreCase(docDto.getFilecontentContentType()))) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("doc", "formaterror", "Unsupported document format. Only PDF, ODT or DOCX are allowed."))
                    .build();
        }
        docDto.setId(id);
        final Doc result = this.docService.save(this.docDtoToDomainMapper.convertTo(docDto, Doc.class));
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("doc", docDto.getId().toString()))
                .body(this.docDomainToDtoMapper.convertTo(result, DocDto.class));
    }

    /**
     * DELETE /api/docs/{id} : delete the "ID" doc.
     *
     * @param id the ID of the doc to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteDoc(@PathVariable Long id) {
        log.debug("REST request to delete Doc : {}", id);
        this.docService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("doc", id.toString()))
                .build();
    }

}
