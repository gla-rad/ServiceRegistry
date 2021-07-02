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

import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.UnLoCodeMapEntry;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import org.apache.commons.io.IOUtils;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UnLoCodeServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    private UnLoCodeService unLoCodeService;

    // Test Variables
    private String xmlContent;
    private Instance existingInstance;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException {
        // Retrieve the instance specification xml from the test sources
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        this.xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Create the XML object of the instance
        Xml xml = new Xml();
        xml.setId(1000L);
        xml.setName("XML Name");
        xml.setContentContentType("G1128 Instance Specification XML");
        xml.setContent(this.xmlContent);
        xml.setComment("No comment");

        // Create an instance with an ID
        this.existingInstance = new Instance();
        this.existingInstance.setId(100L);
        this.existingInstance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.existingInstance.setName("Instance Name");
        this.existingInstance.setVersion("1.0.0");
        this.existingInstance.setComment("No comment");
        this.existingInstance.setStatus(ServiceStatus.RELEASED);
        this.existingInstance.setGeometry(null);
        this.existingInstance.setInstanceAsXml(xml);

        // And finally initialise the service
        this.unLoCodeService.init();
    }

    /**
     * Test that the UnLoCodeService when loading it can read the UnLoCode
     * mapping from the resources.
     */
    @Test
    void testInit() {
        assertFalse(this.unLoCodeService.UnLoCodeMap.isEmpty());
    }

    /**
     * Test that we can apply a UnLoCode into an instance and its XML.
     */
    @Test
    void testApplyUnLoCodeMapping() {
        // Get a UnLoCode entry from the loaded map
        String unLoCode = this.unLoCodeService.UnLoCodeMap.keySet().stream().findFirst().orElse(null);
        assertNotNull(unLoCode);
        UnLoCodeMapEntry unLoCodeMapEntry = this.unLoCodeService.UnLoCodeMap.get(unLoCode);
        assertNotNull(unLoCodeMapEntry);

        // Perform the service call
        this.unLoCodeService.applyUnLoCodeMapping(this.existingInstance, unLoCode);

        // Make sure the UnLoCode was applied correctly
        assertNotNull(this.existingInstance.getGeometry());
        assertEquals("Point", this.existingInstance.getGeometry().getGeometryType());
        assertEquals(1, this.existingInstance.getGeometry().getCoordinates().length);
        assertEquals(unLoCodeMapEntry.getLongitude(), this.existingInstance.getGeometry().getCoordinate().getX());
        assertEquals(unLoCodeMapEntry.getLatitude(), this.existingInstance.getGeometry().getCoordinate().getY());
        assertNotEquals(this.xmlContent, this.existingInstance.getInstanceAsXml().getContent());

        // Now also try to find the WKT notation in the instance XML
        String wktNotation =  String.format("POINT (%.2f %.2f)", unLoCodeMapEntry.getLongitude(), unLoCodeMapEntry.getLatitude());
        assertTrue(this.existingInstance.getInstanceAsXml().getContent().contains(wktNotation));
    }

    /**
     * Test that if the provided UnLoCode is not found in the loaded map of
     * the service, then no assignment will be made.
     */
    @Test
    void testApplyUnLoCodeMappingNotFound() {
        // Perform the service call
        this.unLoCodeService.applyUnLoCodeMapping(this.existingInstance, "INVALID");

        // Make sure the UnLoCode was applied correctly
        assertNull(this.existingInstance.getGeometry());
        assertEquals(this.xmlContent, this.existingInstance.getInstanceAsXml().getContent());
    }

}