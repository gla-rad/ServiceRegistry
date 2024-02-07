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

import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Instance entity.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface InstanceRepo extends JpaRepository<Instance, Long> {

    /**
     * Find all with eager relationships list.
     *
     * @return the list
     */
    @Query("select distinct instance " +
            "from Instance instance " +
            "left join fetch instance.docs " +
            "left join fetch instance.ledgerRequest")
    List<Instance> findAllWithEagerRelationships();

    /**
     * Find by domain id eager relationships list.
     *
     * @param id the id
     * @return the list
     */
    @Query("select distinct instance " +
            "from Instance instance " +
            "left join fetch instance.docs " +
            "left join fetch instance.ledgerRequest " +
            "where instance.instanceId = :id ")
    List<Instance> findByDomainIdEagerRelationships(@Param("id") String id);

    /**
     * Find one with eager relationships instance.
     *
     * @param id the id
     * @return the instance
     */
    @Query("select instance " +
            "from Instance instance " +
            "left join fetch instance.docs " +
            "left join fetch instance.ledgerRequest " +
            "where instance.id =:id")
    Instance findOneWithEagerRelationships(@Param("id") Long id);

    /**
     * Find by domain id list.
     *
     * @param id the id
     * @return the list
     */
    @Query("select distinct instance " +
            "from Instance instance " +
            "where instance.instanceId = :id")
    List<Instance> findByDomainId(@Param("id") String id);

    /**
     * Find by domain id and version list.
     *
     * @param id      the id
     * @param version the version
     * @return the list
     */
    @Query("select distinct instance " +
            "from Instance instance " +
            "where instance.instanceId = :id " +
            "and instance.version = :version")
    Optional<Instance> findByDomainIdAndVersion(@Param("id") String id, @Param("version") String version);

    /**
     * Find by domain id and version eager relationships list.
     *
     * @param id      the id
     * @param version the version
     * @return the list
     */
    @Query("select distinct instance " +
            "from Instance instance " +
            "left join fetch instance.docs " +
            "left join fetch instance.ledgerRequest " +
            "where instance.instanceId = :id " +
            "and instance.version = :version")
    Optional<Instance> findByDomainIdAndVersionEagerRelationships(@Param("id") String id, @Param("version") String version);

}
