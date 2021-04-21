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
import net.maritimeconnectivity.serviceregistry.models.domain.Xsd;
import net.maritimeconnectivity.serviceregistry.repos.XsdRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Xsd.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class XsdService {

    @Autowired
    private XsdRepo xsdRepo;

    /**
     * Save a xsd.
     *
     * @param xsd the entity to save
     * @return the persisted entity
     */
    public Xsd save(Xsd xsd) {
        log.debug("Request to save Xsd : {}", xsd);
        Xsd result = this.xsdRepo.save(xsd);
        return result;
    }

    /**
     *  Get all the xsds.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Xsd> findAll(Pageable pageable) {
        log.debug("Request to get all Xsds");
        Page<Xsd> result = this.xsdRepo.findAll(pageable);
        return result;
    }

    /**
     *  Get one xsd by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Xsd findOne(Long id) {
        log.debug("Request to get Xsd : {}", id);
        Xsd xsd = this.xsdRepo.findById(id).orElse(null);
        return xsd;
    }

    /**
     *  Delete the  xsd by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Xsd : {}", id);
        this.xsdRepo.deleteById(id);
    }

}
