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

package net.maritimeconnectivity.serviceregistry.config;

import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.grad.secom.core.models.SearchObjectResult;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
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
     * The Model Mapper allows easy mapping between DTOs and domain objects.
     *
     * @return the model mapper bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // ================================================================== //
        // Provide a configuration for all the mappings here to keep tidy     //
        // ================================================================== //
        // Create a map between the instances and the SECOM search result object
        modelMapper.createTypeMap(Instance.class, SearchObjectResult.class)
                .implicitMappings()
                .addMappings(mapper -> {
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .map(Xml.class::cast)
                                    .map(Xml::getContent)
                                    .orElse(null))
                            .map(Instance::getInstanceAsXml, SearchObjectResult::setInstanceAsXml);
                    mapper.using(ctx -> Optional.of(ctx)
                                    .map(MappingContext::getSource)
                                    .filter(Geometry.class::isInstance)
                                    .map(Geometry.class::cast)
                                    .map(GeometryJSONConverter::convertFromGeometry)
                                    .orElse(null))
                            .map(Instance::getGeometry, SearchObjectResult::setGeometry);
                    mapper.using(ctx -> Stream.of(Optional.of(ctx)
                                            .map(MappingContext::getSource)
                                            .orElse(Stream.empty()))
                                .filter(source -> source instanceof List)
                                .map(Object::toString)
                                .map(type -> {
                                    try {
                                        return SECOM_DataProductType.valueOf(type);
                                    } catch (Exception ex){
                                        return SECOM_DataProductType.OTHER;
                                    }
                                })
                                .filter(not(SECOM_DataProductType.OTHER::equals))
                                .findAny()
                                .orElse(SECOM_DataProductType.OTHER)
                             )
                            .map(Instance::getServiceType, SearchObjectResult::setDataProductType);
                });
        // ================================================================== //

        return modelMapper;
    }

}
