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
import net.maritimeconnectivity.serviceregistry.models.domain.SpecificationTemplateSet;
import net.maritimeconnectivity.serviceregistry.repos.SpecificationTemplateSetRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Specification Template Set.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class SpecificationTemplateSetService {

    @Autowired
    private SpecificationTemplateSetRepo specificationTemplateSetRepo;

    /**
     * Save a specificationTemplateSet.
     *
     * @param specificationTemplateSet the entity to save
     * @return the persisted entity
     */
    public SpecificationTemplateSet save(SpecificationTemplateSet specificationTemplateSet) {
        log.debug("Request to save SpecificationTemplateSet : {}", specificationTemplateSet);
        SpecificationTemplateSet result = this.specificationTemplateSetRepo.save(specificationTemplateSet);
        return result;
    }

    /**
     *  Get all the specificationTemplateSets.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<SpecificationTemplateSet> findAll(Pageable pageable) {
        log.debug("Request to get all SpecificationTemplateSets");
        Page<SpecificationTemplateSet> result = this.specificationTemplateSetRepo.findAll(pageable);
        return result;
    }

    /**
     *  Get one specificationTemplateSet by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public SpecificationTemplateSet findOne(Long id) {
        log.debug("Request to get SpecificationTemplateSet : {}", id);
        SpecificationTemplateSet specificationTemplateSet = this.specificationTemplateSetRepo.findOneWithEagerRelationships(id);
        return specificationTemplateSet;
    }

    /**
     *  Delete the  specificationTemplateSet by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete SpecificationTemplateSet : {}", id);
        this.specificationTemplateSetRepo.deleteById(id);
    }

}
