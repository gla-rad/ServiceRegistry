/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.Optional;

/**
 * Spring Data JPA repository for the Ledger Request entity.
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */
public interface LedgerRequestRepo extends JpaRepository<LedgerRequest, Long> {

    /**
     * Find by instance ID.
     *
     * @param instanceId the instance ID
     * @return the list
     */
    @Query("select distinct request " +
            "from LedgerRequest request " +
            "where request.serviceInstance.id = :instanceId")
    Optional<LedgerRequest> findByInstanceId(@Param("instanceId") Long instanceId);

}
