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
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.repos.DesignRepo;
import net.maritimeconnectivity.serviceregistry.repos.DocRepo;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.repos.SpecificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private DocRepo docRepo;

    @Autowired
    private DesignRepo designRepo;

    @Autowired
    private SpecificationRepo specificationRepo;

    @Autowired
    private InstanceRepo instanceRepo;

    /**
     * Save a doc.
     *
     * @param doc the entity to save
     * @return the persisted entity
     */
    public Doc save(Doc doc){
        log.debug("Request to save Doc : {}", doc);
        this.docRepo.save(doc);

        // Save the linked designed, if any
        Optional.of(this.designRepo)
                .map(DesignRepo::findAllWithEagerRelationships)
                .orElse(Collections.emptyList())
                .stream()
                .filter(d -> d.getDesignAsDoc() != null && d.getDesignAsDoc().getId() == doc.getId())
                .forEach(d -> {
                    log.debug("Updating Linked Specification: {}", d);
                    this.designRepo.save(d);
                });

        // Save the linked specifications, if any
        Optional.of(this.specificationRepo)
                .map(SpecificationRepo::findAllWithEagerRelationships)
                .orElse(Collections.emptyList())
                .stream()
                .filter(s -> s.getSpecAsDoc() != null && s.getSpecAsDoc().getId() == doc.getId())
                .forEach(s -> {
                    log.debug("Updating Linked Specification: {}", s);
                    this.specificationRepo.save(s);
                });

        // Save the linked instances, if any
        Optional.of(this.instanceRepo)
                .map(InstanceRepo::findAllWithEagerRelationships)
                .orElse(Collections.emptyList())
                .stream()
                .filter(i -> i.getInstanceAsDoc() != null && i.getInstanceAsDoc().getId() == doc.getId())
                .forEach(i -> {
                    log.debug("Updating Linked Instance: {}", i);
                    this.instanceRepo.save(i);
                });

        return doc;
    }

    /**
     *  Get all the docs.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Doc> findAll(Pageable pageable) {
        this.log.debug("Request to get all Docs");
        Page<Doc> result = this.docRepo.findAll(pageable);
        return result;
    }

    /**
     *  Get one doc by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Doc findOne(Long id) {
        this.log.debug("Request to get Doc : {}", id);
        Doc doc = this.docRepo.findById(id).orElse(null);
        return doc;
    }

    /**
     *  Delete the  doc by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Doc : {}", id);
        this.docRepo.deleteById(id);
    }

}