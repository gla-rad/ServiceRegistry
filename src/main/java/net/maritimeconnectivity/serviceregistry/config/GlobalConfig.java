/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.config;

import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.iala_aism.g1128.v1_7.serviceinstanceschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * The Global Configuration.
 *
 * A class to define the global configuration for the application.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class GlobalConfig {

    /**
     * <p>
     * Add an HTTP trace repository in memory to be used for the respective
     * actuator.
     * </p>
     * <p>
     * The functionality has been removed by default in Spring Boot 2.2.0. For
     * more info see:
     * </p>
     * <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.2.0-M3-Release-Notes#actuator-http-trace-and-auditing-are-disabled-by-default">...</a>
     *
     * @return the in memory HTTP trance repository
     */
    @ConditionalOnProperty(value = "management.endpoint.httpexchanges.enabled", havingValue = "true")
    @Bean
    public HttpExchangeRepository httpTraceRepository() {
        return new InMemoryHttpExchangeRepository();
    }

    /**
     * The Model Mapper allows easy mapping between DTOs and domain objects.
     *
     * @return the model mapper bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // ============================================================================ //
        // Provide a configuration for all the SECOM v1.0 mappings here to keep tidy    //
        // ============================================================================ //
        // Create a map between the instances and the SECOM search result object
        modelMapper.createTypeMap(Instance.class, net.maritimeconnectivity.serviceregistry.models.dto.secom.v1.SearchObjectResultWithCert.class)
                .implicitMappings()
                .addMappings(mapper -> {
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Iterable.class::cast)
                                    .map(kl -> String.join(",", kl))
                                    .orElse(null))
                            .map(Instance::getKeywords, org.grad.secom.core.models.SearchObjectResult::setKeywords);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Xml.class::cast)
                                    .map(Xml::getContent)
                                    .orElse(null))
                            .map(Instance::getInstanceAsXml, org.grad.secom.core.models.SearchObjectResult::setInstanceAsXml);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .filter(Geometry.class::isInstance)
                                    .map(Geometry.class::cast)
                                    .map(GeometryJSONConverter::convertFromGeometry)
                                    .orElse(null))
                            .map(Instance::getGeometry, org.grad.secom.core.models.SearchObjectResult::setGeometry);
                    mapper.using(ctx -> Stream.of(Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .filter(List.class::isInstance)
                                    .map(List.class::cast)
                                    .map(List::toArray)
                                    .orElseGet(() -> new org.grad.secom.core.models.enums.SECOM_DataProductType[]{}))
                                    .findFirst()
                                    .filter(not(org.grad.secom.core.models.enums.SECOM_DataProductType.OTHER::equals))
                                    .orElse(org.grad.secom.core.models.enums.SECOM_DataProductType.OTHER)
                             )
                            .map(Instance::getDataProductType, org.grad.secom.core.models.SearchObjectResult::setDataProductType);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Instance.class::cast)
                                    .map(Instance::getStatus)
                                    .map(ServiceStatus::name)
                                    .orElse(""))
                            .map(src -> src, net.maritimeconnectivity.serviceregistry.models.dto.secom.v1.SearchObjectResultWithCert::setStatus);
                });
        // ================================================================== //

        // ========================================================================== //
        // Provide a configuration for all the SECOM V2 mappings here to keep tidy    //
        // ========================================================================== //
        // Create a map between the instances and the SECOM search result object
        modelMapper.createTypeMap(Instance.class, net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert.class)
                .implicitMappings()
                .addMappings(mapper -> {
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(List.class::cast)
                                    .map(l -> l.toArray(new String[]{}))
                                    .orElse(new String[]{}))
                            .map(Instance::getKeywords, org.grad.secomv2.core.models.SearchObjectResult::setKeywords);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Xml.class::cast)
                                    .map(Xml::getContent)
                                    .orElse(null))
                            .map(Instance::getInstanceAsXml, org.grad.secomv2.core.models.SearchObjectResult::setInstanceAsXml);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .filter(Geometry.class::isInstance)
                                    .map(Geometry.class::cast)
                                    .map(GeometryJSONConverter::convertFromGeometry)
                                    .orElse(null))
                            .map(Instance::getGeometry, org.grad.secomv2.core.models.SearchObjectResult::setGeometry);
                    mapper.using(ctx ->Stream.of(Optional.of(ctx)
                                            .map(MappingContext::getSource)
                                            .filter(List.class::isInstance)
                                            .map(List.class::cast)
                                            .map(List::toArray)
                                            .orElseGet(() -> new org.grad.secom.core.models.enums.SECOM_DataProductType[]{SECOM_DataProductType.OTHER}))
                                            .filter(org.grad.secom.core.models.enums.SECOM_DataProductType.class::isInstance)
                                            .map(org.grad.secom.core.models.enums.SECOM_DataProductType.class::cast)
                                            .map(org.grad.secom.core.models.enums.SECOM_DataProductType::getDescription)
                                            .map(org.grad.secomv2.core.models.enums.SECOM_DataProductType::fromDescription)
                                            .toList()
                                            .toArray(new org.grad.secomv2.core.models.enums.SECOM_DataProductType[]{})
                            )
                            .map(Instance::getDataProductType, org.grad.secomv2.core.models.SearchObjectResult::setDataProductType);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Instance.class::cast)
                                    .map(Instance::getStatus)
                                    .map(ServiceStatus::name)
                                    .orElse(""))
                            .map(src -> src, net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert::setStatus);
                });
        // ================================================================== //

        return modelMapper;
    }

}
