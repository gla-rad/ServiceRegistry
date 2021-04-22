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

import net.maritimeconnectivity.serviceregistry.models.domain.SpecificationTemplateSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the SpecificationTemplateSet entity.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface SpecificationTemplateSetRepo extends JpaRepository<SpecificationTemplateSet, Long> {

    /**
     * Find all with eager relationships list.
     *
     * @return the list
     */
    @Query("select distinct specificationTemplateSet " +
            "from SpecificationTemplateSet specificationTemplateSet " +
            "left join fetch specificationTemplateSet.templates " +
            "left join fetch specificationTemplateSet.docs")
    List<SpecificationTemplateSet> findAllWithEagerRelationships();

    /**
     * Find one with eager relationships specification template set.
     *
     * @param id the id
     * @return the specification template set
     */
    @Query("select specificationTemplateSet " +
            "from SpecificationTemplateSet specificationTemplateSet " +
            "left join fetch specificationTemplateSet.templates " +
            "left join fetch specificationTemplateSet.docs " +
            "where specificationTemplateSet.id =:id")
    SpecificationTemplateSet findOneWithEagerRelationships(@Param("id") Long id);

}
