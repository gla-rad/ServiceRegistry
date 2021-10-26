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

package net.maritimeconnectivity.serviceregistry.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.util.Optional;

/**
 * The Geometry Bridge Class.
 *
 * The geometry object can be represented by a Location Tech Geometry class
 * which is then parsed as a WKT string and indexed in Lucene. The opposite
 * operation can be used when translating the indexed value back into the
 * Geometry object.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeometryBridge implements ValueBridge<Geometry, String> {

    /**
     * Translates the geometry object into a WKT string representation.
     *
     * @param geometry The geometry to be translated
     * @param valueBridgeToIndexedValueContext the value bridge context
     * @return the WKT string representation of the geometry
     */
    @Override
    public String toIndexedValue(Geometry geometry, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
        return Optional.ofNullable(geometry)
                .map(new WKTWriter()::write)
                .orElse(null);
    }

    /**
     * Translates the WKT string back into a geometry object.
     *
     * @param value The WKT string to be transtated
     * @param context the value bridge context
     * @return the geometry representation of the WKT string
     */
    @Override
    public Geometry fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
        return Optional.ofNullable(value)
                .map(v -> {
                    try { return new WKTReader().read(v); }
                    catch (ParseException ex) { return null; }
                })
                .orElse(null);
    }

}
