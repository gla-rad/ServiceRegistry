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

package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
import net.maritimeconnectivity.serviceregistry.repos.DocRepo;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Optional;

/**
 * Service Implementation for managing Doc.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class DocService {

    /**
     * The Entity Manager.
     */
    @Autowired
    EntityManager entityManager;

    /**
     * The Doc Repository.
     */
    @Autowired
    DocRepo docRepo;

    // Service Variables
    private final String[] searchFields = new String[] {
            "name",
            "comment",
            "mimetype"
    };

    /**
     *  Get all the docs.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Doc> findAll(Pageable pageable) {
        this.log.debug("Request to get all Docs");
        return this.docRepo.findAll(pageable);
    }

    /**
     *  Get one doc by ID.
     *
     *  @param id       the ID of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Doc findOne(Long id) throws DataNotFoundException {
        this.log.debug("Request to get Doc : {}", id);
        return this.docRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException("No doc found for the provided ID", null));
    }

    /**
     * Save a doc.
     *
     * @param doc       the entity to save
     * @return the persisted entity
     */
    @Transactional
    public Doc save(Doc doc){
        log.debug("Request to save Doc : {}", doc);
        Doc result = this.docRepo.save(doc);

        return result;
    }

    /**
     *  Delete the doc by ID.
     *
     *  @param id       the ID of the entity
     */
    @Transactional
    public void delete(Long id) throws DataNotFoundException {
        log.debug("Request to delete Doc : {}", id);
        if(this.docRepo.existsById(id)) {
            this.docRepo.deleteById(id);
        } else {
            throw new DataNotFoundException("No doc found for the provided ID", null);
        }
    }

    /**
     * Handles a datatables pagination request and returns the results list in
     * an appropriate format to be viewed by a datatables jQuery table.
     *
     * @param dtPagingRequest the Datatables pagination request
     * @return the paged response
     */
    @Transactional(readOnly = true)
    public Page<Doc> handleDatatablesPagingRequest(Long instanceId, DtPagingRequest dtPagingRequest) {
        // Create the search query
        SearchQuery searchQuery = this.getSearchDocQueryByText(instanceId,
                dtPagingRequest.getSearch().getValue(),
                dtPagingRequest.getLucenceSort(null));

        // Map the results to a paged response
        return Optional.of(searchQuery)
                .map(query -> query.fetch(dtPagingRequest.getStart(), dtPagingRequest.getLength()))
                .map(searchResult -> new PageImpl<Doc>(searchResult.hits(), dtPagingRequest.toPageRequest(), searchResult.total().hitCount()))
                .orElseGet(() -> new PageImpl<>(Collections.emptyList(), dtPagingRequest.toPageRequest(), 0));
    }

    /**
     * Constructs a hibernate search query using Lucene based on the provided
     * search test. This query will be based solely on the docs table and
     * will include the following fields:
     * <ul>
     *  <li>Name</li>Name
     *  <li>Comment</li>
     *  <li>MIME TYpe</li>
     * </ul>
     *
     * @param searchText    the text to be searched
     * @return the full text query
     */
    protected SearchQuery<Doc> getSearchDocQueryByText(Long instanceId, String searchText, org.apache.lucene.search.Sort sort) {
        SearchSession searchSession = Search.session( entityManager );
        SearchScope<Doc> scope = searchSession.scope( Doc.class );
        return searchSession.search( scope )
                .extension(LuceneExtension.get())
                .where( f -> f.bool(b -> {
                    b.must( f.matchAll() );
                    if(searchText != null && StringUtils.isNotBlank(searchText)) {
                        b.must(f.wildcard()
                                .fields(this.searchFields)
                                .matching( Optional.ofNullable(searchText).map(st -> "*"+st).orElse("") + "*" )
                                .toPredicate());
                    }
                    if(instanceId != null) {
                        b.must(f.match()
                                .field( "instance.id_search" )
                                .matching( instanceId )
                                .toPredicate());
                    }
                }))
                .sort(f -> f.fromLuceneSort(sort))
                .toQuery();
    }
}
