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
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
import net.maritimeconnectivity.serviceregistry.repos.DocRepo;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * The Instance Repository.
     */
    @Autowired
    InstanceRepo instanceRepo;

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

        // Save the linked instances, if any
        Optional.of(this.instanceRepo)
                .map(InstanceRepo::findAllWithEagerRelationships)
                .orElse(Collections.emptyList())
                .stream()
                .filter(i -> i.getInstanceAsDoc() != null && i.getInstanceAsDoc().getId() == result.getId())
                .forEach(i -> {
                    log.debug("Updating Linked Instance: {}", i);
                    this.instanceRepo.save(i);
                });

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
        FullTextQuery searchQuery = this.searchDocQuery(instanceId, dtPagingRequest.getSearch().getValue());
        searchQuery.setFirstResult(dtPagingRequest.getStart());
        searchQuery.setMaxResults(dtPagingRequest.getLength());

        // Add sorting if requested
        Optional.of(dtPagingRequest)
                .map(DtPagingRequest::getLucenceSort)
                .filter(ls -> ls.getSort().length > 0)
                .ifPresent(searchQuery::setSort);

        // Map the results to a paged response
        return Optional.of(searchQuery)
                .map(FullTextQuery::getResultList)
                .map(docs -> new PageImpl<Doc>(docs, dtPagingRequest.toPageRequest(), searchQuery.getResultSize()))
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
    protected FullTextQuery searchDocQuery(Long instanceId, String searchText) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Doc.class)
                .get();

        Query luceneQuery = queryBuilder
                .keyword()
                .wildcard()
                .onFields(this.searchFields)
                .matching(Optional.ofNullable(searchText).orElse("").toLowerCase() + "*")
                .createQuery();

        Query instanceIdQuery = queryBuilder
                .keyword()
                .onField("instances.name")
                .ignoreFieldBridge()
                .matching("Test*")
                .createQuery();

        Query finalQuery = queryBuilder.bool()
                .must(luceneQuery)
                .must(instanceIdQuery)
                .createQuery();

        return fullTextEntityManager.createFullTextQuery(finalQuery, Doc.class);
    }
}
