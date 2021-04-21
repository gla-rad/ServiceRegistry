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
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.repos.XmlRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Xml.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class XmlService {

    @Autowired
    private XmlRepo xmlRepo;

    /**
     * Save a xml.
     *
     * @param xml the entity to save
     * @return the persisted entity
     */
    public Xml save(Xml xml) {
        log.debug("Request to save Xml : {}", xml);
        Xml result = this.xmlRepo.save(xml);
        return result;
    }

    /**
     *  Get all the xmls.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Xml> findAll(Pageable pageable) {
        log.debug("Request to get all Xmls");
        Page<Xml> result = this.xmlRepo.findAll(pageable);
        return result;
    }

    /**
     *  Get one xml by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Xml findOne(Long id) {
        log.debug("Request to get Xml : {}", id);
        Xml xml = this.xmlRepo.findById(id).orElse(null);
        return xml;
    }

    /**
     *  Delete the  xml by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Xml : {}", id);
        this.xmlRepo.deleteById(id);
    }

}
