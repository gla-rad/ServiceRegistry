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

package net.maritimeconnectivity.serviceregistry.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.UnLoCodeMapEntry;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.utils.G1128Utils;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.CoverageArea;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.ServiceInstance;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@Service
@Slf4j
public class UnLoCodeService {

    private static final String JSON_KEY_COUNTRY = "Country";
    private static final String JSON_KEY_LOCATION = "Location";
    private static final String JSON_KEY_COORDINATES = "Coordinates";

    HashMap<String, UnLoCodeMapEntry> UnLoCodeMap = null;

    /**
     * Once the service has been initialised, it will read the full list of the
     * UN LoCodes from the JSON-encoded resource file.
     */
    @PostConstruct
    public void init() {
        InputStream s = this.getClass().getClassLoader().getResourceAsStream("UnLoCodeLists.json");
        try {
            this.loadUnLoCodeMapping(s);
        } catch (IOException e) {
            log.error(e.getMessage());
            this.UnLoCodeMap = new HashMap<>();
        }
    }

    /**
     * Applies the UN LoCode mapping onto the provided instance.
     *
     * @param instance the instance to provide the mapping to
     * @param unLoCode the UN LoCode
     */
    public void applyUnLoCodeMapping(Instance instance, String unLoCode) {
        UnLoCodeMapEntry e = UnLoCodeMap.get(unLoCode);
        String pointWKT = "";
        try {
            if (e != null) {
                // Translate the WKT notation to a JSON node
                pointWKT = "POINT (" + e.getLongitude() + " " + e.getLatitude() + ")";
                JsonNode pointJson = WKTUtil.convertWKTtoGeoJson(pointWKT);

                // Update the json geometry so E2 can find it
                instance.setGeometryJson(pointJson);

                // Create the G1128 geometry from the point WKT notation
                CoverageArea coverageArea = new CoverageArea();
                coverageArea.setGeometryAsWKT(pointWKT);

                // Insert the G1128 geometry into the XML
                Xml instanceXml = instance.getInstanceAsXml();
                ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(instanceXml.getContent());
                serviceInstance.getCoversAreas().getCoversAreasAndUnLoCodes().clear();
                serviceInstance.getCoversAreas().getCoversAreasAndUnLoCodes().add(coverageArea);
                instanceXml.setContent(new G1128Utils<>(ServiceInstance.class).marshalG1128(serviceInstance));
                instance.setInstanceAsXml(instanceXml);
            }
        } catch (Exception ex) {
            log.error("Error parsing point geometry generated from UnLoCode mapping " + pointWKT + ": ", ex);
        }
    }

    /**
     * Fetch lat/lon from json mapping file and populate coverage geometry with it as point
     *
     * @throws Exception if the unLoCode mapping file could not be found
     */
    private void loadUnLoCodeMapping(InputStream inStream) throws IOException {
        UnLoCodeMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        double invalid = -99999;

        JsonNode unLoCodeJson = mapper.readTree(inStream);
        for (JsonNode entry : unLoCodeJson) {
            try {
                UnLoCodeMapEntry unLoCode = new UnLoCodeMapEntry();
                String country = entry.get(JSON_KEY_COUNTRY).textValue();
                String location = entry.get(JSON_KEY_LOCATION).textValue();
                String coordinatesCombined = entry.get(JSON_KEY_COORDINATES).textValue();
                unLoCode.setLatitude(invalid);
                unLoCode.setLongitude(invalid);
                //coordinates are given in the form of "DDMM[N/S] DDDMM[W/E]"
                if (coordinatesCombined != null) {
                    coordinatesCombined = coordinatesCombined.trim();
                    if (coordinatesCombined.length() > 0) {
                        String[] c = coordinatesCombined.split("\\s");
                        String latDegrees = c[0].substring(0, 2);
                        String latMinutes = c[0].substring(2, 4);
                        String latDirection = c[0].substring(4, 5);
                        unLoCode.setLatitude(Double.parseDouble(latDegrees + "." + latMinutes));
                        if (latDirection == "S") {
                            unLoCode.setLatitude(-unLoCode.getLatitude());
                        }
                        String lonDegrees = c[1].substring(0, 3);
                        String lonMinutes = c[1].substring(3, 5);
                        String lonDirection = c[1].substring(5, 6);
                        unLoCode.setLongitude(Double.parseDouble(lonDegrees + "." + lonMinutes));
                        if (lonDirection == "W") {
                            unLoCode.setLongitude(-unLoCode.getLongitude());
                        }
                    }
                }
                String status = entry.get("Status").textValue();

                unLoCode.setStatus(status);
                if (unLoCode.getLatitude() != invalid && unLoCode.getLongitude() != invalid) {
                    UnLoCodeMap.put(country + location, unLoCode);
                }
            } catch (Exception e) {
                log.error("Error parsing UnLoCode mapping file: ", e);
            }
        }
    }
}
