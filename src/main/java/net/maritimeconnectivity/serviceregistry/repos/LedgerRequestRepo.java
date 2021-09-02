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
package net.maritimeconnectivity.serviceregistry.repos;

import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Ledger Request entity.
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */
public interface LedgerRequestRepo extends JpaRepository<LedgerRequest, Long> {

    /**
     * Find all with eager relationships list.
     *
     * @return the list
     */
    @Query("select distinct request " +
            "from LedgerRequest request ")
    List<LedgerRequest> findAll();

    /**
     * Find by domain id list.
     *
     * @param id the id
     * @return the list
     */
    @Query("select request " +
            "from LedgerRequest request " +
            "where request.id = :id")
    LedgerRequest findOne(@Param("id") Long id);

    /**
     * Find by domain id list.
     *
     * @param domainId the id
     * @return the list
     */
    @Query("select distinct request " +
            "from LedgerRequest request " +
            "where request.serviceInstance.instanceId = :domainId")
    List<LedgerRequest> findByDomainId(@Param("domainId") String domainId);

}
