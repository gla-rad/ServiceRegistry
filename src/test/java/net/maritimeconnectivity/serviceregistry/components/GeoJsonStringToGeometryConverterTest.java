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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.*;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GeoJsonStringToGeometryConverterTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    private GeoJsonStringToGeometryConverter converter;

    // Test Variables
    private Point point;
    private LineString lineString;
    private Polygon polygon;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() {
        // Assign an oject mapper to the component
        converter.objectMapper = new ObjectMapper();

        // Create a temp geometry factory to get some shapes
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        this.point = factory.createPoint(new Coordinate(52.001, 1.002));
        this.lineString = factory.createLineString(new Coordinate[] {
                new Coordinate(52.001, 1.002),
                new Coordinate(53.001, 2.002)
        });
        this.polygon = factory.createPolygon(new Coordinate[] {
                new Coordinate(52.001, 1.002),
                new Coordinate(52.001, 2.002),
                new Coordinate(53.001, 2.002),
                new Coordinate(53.001, 1.002),
                new Coordinate(52.001, 1.002),
        });
    }

    /**
     * Test that we can convert a valid GeoJSON string into TechLocation
     * geometry objects.
     */
    @Test
    void testConvert() {
        assertEquals(this.point, this.converter.convert(GeometryJSONConverter.convertFromGeometry(this.point).toString()));
        assertEquals(this.lineString, this.converter.convert(GeometryJSONConverter.convertFromGeometry(this.lineString).toString()));
        assertEquals(this.polygon, this.converter.convert(GeometryJSONConverter.convertFromGeometry(this.polygon).toString()));
    }

    /**
     * Test that for empty inputs, the GeoJsonStringToGeometryConverter will
     * return null.
     */
    @Test
    void testConvertEmpty() {
        assertNull(this.converter.convert(null));
        assertNull(this.converter.convert(""));
        assertThrows(InvalidRequestException.class, () -> this.converter.convert("invalid"));
    }

    /**
     * Test that for invalid inputs, the GeoJsonStringToGeometryConverter will
     * throw an exception.
     */
    @Test
    void testConvertInvalid() {
        assertThrows(InvalidRequestException.class, () -> this.converter.convert("invalid"));
    }

}