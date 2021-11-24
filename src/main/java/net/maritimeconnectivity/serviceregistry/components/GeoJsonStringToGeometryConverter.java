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

package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The GeoJSON String to Geometry Converter.
 *
 * This utility class can convert GeoJSON strings to TechLocation Geometry
 * objects.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
public class GeoJsonStringToGeometryConverter implements Converter<String, Geometry> {

    /**
     * The Object Mapper.
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * Performs the geometry JSON string conversion to a LocationTech Geometry
     * object.
     *
     * @param value     The geometry JSON string
     * @return The LocationTech Geometry object
     */
    @Override
    public Geometry convert(String value) {
        return Optional.ofNullable(value)
                .map(json -> {
                try {
                    return this.objectMapper.readTree(json);
                } catch (JsonProcessingException ex) {
                    throw new InvalidRequestException(ex.getMessage(), ex);
                }
            })
            .map(GeometryJSONConverter::convertToGeometry)
            .orElse(null);
    }

}
