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

import net.maritimeconnectivity.serviceregistry.models.domain.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Design entity.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface DesignRepo extends JpaRepository<Design, Long> {

    /**
     * Find all with eager relationships list.
     *
     * @return the list
     */
    @Query("select distinct design " +
            " from Design design " +
            " left join fetch design.specifications " +
            " left join fetch design.docs ")
    List<Design> findAllWithEagerRelationships();

    /**
     * Find one with eager relationships design.
     *
     * @param id the id
     * @return the design
     */
    @Query("select design " +
            " from Design design " +
            " left join fetch design.specifications " +
            " left join fetch design.docs " +
            " where design.id =:id")
    Design findOneWithEagerRelationships(@Param("id") Long id);

    /**
     * Find by domain id list.
     *
     * @param id the id
     * @return the list
     */
    @Query("select distinct design " +
            " from Design design " +
            " left join fetch design.specifications " +
            " left join fetch design.docs " +
            " where design.designId = :id")
    List<Design> findByDomainId(@Param("id") String id);

    /**
     * Find by domain id and version list.
     *
     * @param id      the id
     * @param version the version
     * @return the list
     */
    @Query("select distinct design " +
            "from Design design " +
            "left join fetch design.specifications " +
            "left join fetch design.docs " +
            "where design.designId = :id " +
            "and design.version = :version")
    List<Design> findByDomainIdAndVersion(@Param("id") String id, @Param("version") String version);

    /**
     * Find by specification id list.
     *
     * @param id the id
     * @return the list
     */
    @Query("select distinct design " +
            "from Design design " +
            "left join fetch design.specifications " +
            "left join fetch design.docs " +
            "join design.specifications as specification " +
            "where specification.specificationId = :id")
    List<Design> findBySpecificationId(@Param("id") String id);

}
