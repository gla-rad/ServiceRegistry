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

package net.maritimeconnectivity.serviceregistry;

import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.models.dto.DocDto;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.XmlDto;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * This is a test only configuration that will get activated when the "test"
 * profile is active.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@TestConfiguration
public class TestingConfiguration {

    /**
     * The Model Mapper Bean.
     *
     * @return  the model mapper bean.
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /**
     * Instance Mapper from Domain to DTO.
     */
    @Bean
    public DomainDtoMapper instanceDomainToDtoMapper() {
        return new DomainDtoMapper<Instance, InstanceDto>();
    }

    /**
     * Instance Mapper from DTO to Domain.
     */
    @Bean
    public DomainDtoMapper instanceDtoToDomainMapper() {
        return new DomainDtoMapper<InstanceDto, Instance>();
    }

    /**
     * Xml Mapper from Domain to DTO.
     */
    @Bean
    public DomainDtoMapper xmlDomainToDtoMapper() {
        return new DomainDtoMapper<Xml, XmlDto>();
    }

    /**
     * Xml Mapper from DTO to Domain.
     */
    @Bean
    public DomainDtoMapper xmlDtoToDomainMapper() {
        return new DomainDtoMapper<XmlDto, Xml>();
    }

    /**
     * Doc Mapper from Domain to DTO.
     */
    @Bean
    public DomainDtoMapper docDomainToDtoMapper() {
        return new DomainDtoMapper<Doc, DocDto>();
    }

    /**
     * Doc Mapper from DTO to Domain.
     */
    @Bean
    public DomainDtoMapper docDtoToDomainMapper() {
        return new DomainDtoMapper<DocDto, Doc>();
    }

}