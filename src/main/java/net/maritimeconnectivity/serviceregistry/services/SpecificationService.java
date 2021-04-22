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
import net.maritimeconnectivity.serviceregistry.models.domain.Specification;
import net.maritimeconnectivity.serviceregistry.repos.SpecificationRepo;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Specification.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class SpecificationService {

    @Autowired
    private SpecificationRepo specificationRepo;

    /**
     * Save a specification.
     *
     * @param specification the entity to save
     * @return the persisted entity
     */
    public Specification save(Specification specification) {
        log.debug("Request to save Specification : {}", specification);
        Specification result = this.specificationRepo.save(specification);
        return result;
    }

    /**
     *  Get all the specifications.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Specification> findAll(Pageable pageable) {
        log.debug("Request to get all Specifications");
        Page<Specification> result = this.specificationRepo.findAll(pageable);
        return result;
    }

    /**
     *  Get one specification by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Specification findOne(Long id) {
        log.debug("Request to get Specification : {}", id);
        Specification specification = this.specificationRepo.findOneWithEagerRelationships(id);
        return specification;
    }

    /**
     *  Delete the  specification by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Specification : {}", id);
        this.specificationRepo.deleteById(id);
    }

    /**
     *  update the status of a specification by id.
     *
     *  @param id the id of the entity
     *  @param status the status of the entity
     */
    public void updateStatus(Long id, String status) {
        log.debug("Request to update status of Specification : {}", id);
        Specification specification = this.specificationRepo.findOneWithEagerRelationships(id);
        specification.setStatus(status);
        save(specification);
    }

    /**
     *  Get one specification by domain specific id (for example, maritime id) and version.
     *
     *  @param domainId the domain specific id of the specification
     *  @param version the version identifier of the specification
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Specification findByDomainId(String domainId, String version) {
        log.debug("Request to get Specification by domain id {} and version {}", domainId, version);
        Specification specification = null;
        try {
            Iterable<Specification> specifications = this.specificationRepo.findByDomainIdAndVersion(domainId, version);
            if (specifications.iterator().hasNext()) {
                specification = specifications.iterator().next();
            }
        } catch (Exception e) {
            log.debug("Could not find specification for domain id {} and version {}", domainId, version);
            e.printStackTrace();
        }
        return specification;
    }

    /**
     *  Get one specification by domain specific id (for example, maritime id), only return the latest version.
     *
     *  @param domainId the domain specific id of the specification
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Specification findLatestVersionByDomainId(String domainId) {
        log.debug("Request to get Specification by domain id {}", domainId);
        Specification specification = null;
        DefaultArtifactVersion latestVersion = new DefaultArtifactVersion("0.0");
        try {
            Iterable<Specification> specifications = this.specificationRepo.findByDomainId(domainId);
            if (specifications.iterator().hasNext()) {
                Specification i = specifications.iterator().next();
                //Compare version numbers, save the instance if it's a newer version
                DefaultArtifactVersion iv = new DefaultArtifactVersion(i.getVersion());
                if (iv.compareTo(latestVersion) > 0) {
                    specification = i;
                    latestVersion = iv;
                }
            }
        } catch (Exception e) {
            log.debug("Could not find specification for domain id {}", domainId);
            e.printStackTrace();
        }
        return specification;
    }

}
