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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import net.maritimeconnectivity.serviceregistry.models.domain.Design;
import net.maritimeconnectivity.serviceregistry.repos.DesignRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing Design.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class DesignService {

    @Autowired
    private DesignRepo designRepo;

    /**
     * Save a design.
     *
     * @param design the entity to save
     * @return the persisted entity
     */
    public Design save(Design design) {
        log.debug("Request to save Design : {}", design);
        return this.designRepo.save(design);
    }

    /**
     * Get all the designs.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Design> findAll(Pageable pageable) {
        log.debug("Request to get all Designs");
        Page<Design> result = this.designRepo.findAll(pageable);
        return result;
    }

    /**
     * Get one design by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Design findOne(Long id) {
        log.debug("Request to get Design : {}", id);
        Design design = this.designRepo.findOneWithEagerRelationships(id);
        return design;
    }

    /**
     * Delete the  design by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Design : {}", id);
        this.designRepo.deleteById(id);
    }

    /**
     * Update the status of a design by id.
     *
     * @param id     the id of the entity
     * @param status the status of the entity
     */
    public void updateStatus(Long id, String status) {
        log.debug("Request to update status of Design : {}", id);
        Design design = this.designRepo.findOneWithEagerRelationships(id);
        design.setStatus(status);
        save(design);
    }

    /**
     *  Get one design by domain specific id (for example, maritime id) and version.
     *
     *  @param domainId the domain specific id of the design
     *  @param version the version identifier of the design
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Design findByDomainId(String domainId, String version) {
        log.debug("Request to get Design by domain id {} and version {}", domainId, version);
        Design design = null;
        try {
            Iterable<Design> designs = this.designRepo.findByDomainIdAndVersion(domainId, version);
            if (designs.iterator().hasNext()) {
                design = designs.iterator().next();
            }
        } catch (Exception e) {
            log.debug("Could not find design for domain id {} and version {}", domainId, version);
            e.printStackTrace();
        }
        return design;
    }

    /**
     *  Get one design by domain specific id (for example, maritime id), only return the latest version.
     *
     *  @param domainId the domain specific id of the design
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Design findLatestVersionByDomainId(String domainId) {
        log.debug("Request to get Design by domain id {}", domainId);
        Design design = null;
        DefaultArtifactVersion latestVersion = new DefaultArtifactVersion("0.0");
        try {
            Iterable<Design> designs = this.designRepo.findByDomainId(domainId);
            if (designs.iterator().hasNext()) {
                Design i = designs.iterator().next();
                //Compare version numbers, save the instance if it's a newer version
                DefaultArtifactVersion iv = new DefaultArtifactVersion(i.getVersion());
                if (iv.compareTo(latestVersion) > 0) {
                    design = i;
                    latestVersion = iv;
                }
            }
        } catch (Exception e) {
            log.debug("Could not find specification for domain id {}", domainId);
            e.printStackTrace();
        }
        return design;
    }

    /**
     *  Get all designs by specification id (for example, maritime id).
     *
     *  @param specificationId the domain specific id of the specification this design is for
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Design> findAllBySpecificationId(String specificationId) {
        log.debug("Request to get Design by specification id {}", specificationId);
        List<Design> designs = null;
        try {
            designs = this.designRepo.findBySpecificationId(specificationId);
        } catch (Exception e) {
            log.debug("Could not find design for domain id {}", specificationId);
            e.printStackTrace();
        }
        return designs;
    }

}
