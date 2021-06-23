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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeometryJSONConverterTest {

    // Test Variables
    private ObjectMapper objectMapper;
    private Geometry geometry;
    private JsonNode jsonNode;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();

        // Create a temp geometry factory to get a test point geometry
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geometry = factory.createPoint(new Coordinate(52.001, 1.002));

        // Create the same thing as a JSON node
        this.jsonNode = this.objectMapper.createObjectNode();
        ((ObjectNode) this.jsonNode).put("type", "Point");
        ((ObjectNode) this.jsonNode).putArray("coordinates")
                .add(52.001)
                .add(1.002);
        ((ObjectNode) this.jsonNode).putObject("crs")
                .put("type", "name")
                .putObject("properties")
                .put("name", "EPSG:4326");
    }

    /**
     * Test that we can successfully convert geometry objects onto JSON node
     * trees.
     */
    @Test
    void testConvertFromGeometry() {
        JsonNode result = GeometryJSONConverter.convertFromGeometry(this.geometry);
        assertEquals(this.jsonNode, result);
    }

    /**
     * Test the error cases were the conversion between a geometry and a
     * JSON node fails.
     */
    @Test
    void testConvertFromGeometryError() {
        assertNull(GeometryJSONConverter.convertFromGeometry(null));
    }

    /**
     * Test that we can successfully convert a JSON node back to a geometry.
     */
    @Test
    void textConvertToGeometry() {
        Geometry result = GeometryJSONConverter.convertToGeometry(this.jsonNode);
        assertEquals(this.geometry, result);
    }

    /**
     * Test the error cases were the conversion between a JSON node and a
     * geometry fails
     */
    @Test
    void textConvertToGeometryError() {
        assertNull(GeometryJSONConverter.convertToGeometry(null));
        assertNull(GeometryJSONConverter.convertToGeometry(this.objectMapper.createObjectNode()));
        assertNull(GeometryJSONConverter.convertToGeometry(this.objectMapper.createObjectNode().put("invalid","invalid")));
    }
}