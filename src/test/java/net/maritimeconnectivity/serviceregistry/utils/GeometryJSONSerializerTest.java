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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryJSONSerializerTest {

    // Test Variables
    private GeometryJSONSerializer serializer;
    private ObjectMapper objectMapper;
    private Geometry geometry;
    private String json;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.serializer = new GeometryJSONSerializer();
        this.objectMapper = new ObjectMapper();

        // Create a temp geometry factory to get a test point geometry
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geometry = factory.createPoint(new Coordinate(52.001, 1.002));

        // Create the same thing as a JSON string
        this.json = "\"{\\\"type\\\":\\\"Point\\\",\\\"coordinates\\\":[52.001,1.002],\\\"crs\\\":{\\\"type\\\":\\\"name\\\",\\\"properties\\\":{\\\"name\\\":\\\"EPSG:4326\\\"}}}\"";
    }

    /**
     * test that we can successfully serialize a geometry into a JSON node
     * object.
     */
    @Test
    void testSerialize() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = this.objectMapper.createGenerator(os);
        this.serializer.serialize(this.geometry, jsonGenerator, this.objectMapper.getSerializerProvider());
        assertEquals(this.json, os.toString("UTF-8"));
    }
}