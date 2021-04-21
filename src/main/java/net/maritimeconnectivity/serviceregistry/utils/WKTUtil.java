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
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;

/**
 * The type Wkt util.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Slf4j
public class WKTUtil {

    /**
     * Converts a WKT geometry into GeoJson format, via JTS geometry
     *
     * @param geometryAsWKT The geometry in WKT format
     * @return JsonNode with the geometry expressed in GeoJson format
     * @throws ParseException if the WKT geometry was invalid
     * @throws IOException    if the geoJson string could not be read by the Json parser
     */
    public static JsonNode convertWKTtoGeoJson(String geometryAsWKT) throws ParseException, IOException {
        WKTReader wktReader = new WKTReader();
        Geometry geometry = wktReader.read(geometryAsWKT);
        if (geometry == null) {
            log.debug("WKT geometry parsing error");
        }
        return GeometryJSONConverter.convertFromGeometry(geometry);
    }

}
