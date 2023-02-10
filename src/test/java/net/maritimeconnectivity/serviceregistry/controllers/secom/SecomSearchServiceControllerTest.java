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

package net.maritimeconnectivity.serviceregistry.controllers.secom;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchObjectResult;
import org.grad.secom.core.models.SearchParameters;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.grad.secom.core.interfaces.SearchServiceSecomInterface.SEARCH_SERVICE_INTERFACE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
class SecomSearchServiceControllerTest {

    /**
     * The Reactive Web Test Client.
     */
    @Autowired
    WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public DomainDtoMapper searchObjectResultMapper;

    @MockBean
    private InstanceService instanceService;

    // Test Variables
    private List<Instance> instances;
    private Pageable pageable;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Create a temp geometry factory to get some shapes
        final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

        // Initialise the instances list
        this.instances = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            Instance instance = new Instance();
            instance.setId(i);
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", i));
            instance.setName(String.format("Test Instance %d", i));
            instance.setStatus(ServiceStatus.RELEASED);
            instance.setVersion("0.0.1");
            instance.setGeometry(factory.createPoint(new Coordinate(i, i)));

            Xml xml = new Xml();
            xml.setId(i);
            xml.setName(String.format("XML Name %d", i));
            xml.setComment(String.format("XML Comment %d", i));
            xml.setContent("<xml></xml>");
            instance.setInstanceAsXml(xml);

            this.instances.add(instance);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);
    }

    /**
     * Test that we can search for instances using the SECOM discovery service
     * search API endpoint that supports Lucene queries and a paged result
     * using GeoJSON geometries.
     */
    @Test
    void testSearchGeoJSON() throws Exception {
        // Create the search filter object
        SearchFilterObject searchFilterObject = new SearchFilterObject();
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setName("Test");
        searchFilterObject.setQuery(searchParameters);
        searchFilterObject.setGeometry("{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"LineString\",\"coordinates\":[[0,50],[0,52]]}]}");

        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleSearchQueryRequest(any(), any(), any());

        // Perform the web request
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/secom/" + SEARCH_SERVICE_INTERFACE_PATH)
                        .queryParam("page", 0)
                        .queryParam("pageSize", Integer.MAX_VALUE)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(Mono.just(searchFilterObject), SearchFilterObject.class))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResponseSearchObject.class)
                .consumeWith(response -> {
                    ResponseSearchObject result = response.getResponseBody();
                    assertNotNull(result);
                    assertNotNull(result.getSearchServiceResult());
                    assertEquals(this.instances.size(), result.getSearchServiceResult().size());

                    // Test each of the result entries
                    for(SearchObjectResult searchObjectResult: result.getSearchServiceResult()) {
                        int i = result.getSearchServiceResult().indexOf(searchObjectResult);
                        assertEquals(this.instances.get(i).getInstanceId(), searchObjectResult.getInstanceId());
                        assertEquals(this.instances.get(i).getName(), searchObjectResult.getName());
                        assertEquals(this.instances.get(i).getStatus().toString(), searchObjectResult.getStatus());
                        assertEquals(this.instances.get(i).getVersion(), searchObjectResult.getVersion());
                        assertEquals(this.instances.get(i).getInstanceAsXml().getContent(), searchObjectResult.getInstanceAsXml());
                        assertEquals(SECOM_DataProductType.OTHER, searchObjectResult.getDataProductType());
                    }
                });
    }

    /**
     * Test that we can search for instances using the SECOM discovery service
     * search API endpoint that supports Lucene queries and a paged result
     * using WKT geometries.
     */
    @Test
    void testSearchWKT() throws Exception {
        // Create the search filter object
        SearchFilterObject searchFilterObject = new SearchFilterObject();
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setName("Test");
        searchFilterObject.setQuery(searchParameters);
        searchFilterObject.setGeometry("LINESTRING ( 0 50, 0 52 )");
        // Create a mocked paging response
        Page<Instance> page = new PageImpl<>(this.instances, this.pageable, this.instances.size());

        // Mock the service call for creating a new instance
        doReturn(page).when(this.instanceService).handleSearchQueryRequest(any(), any(), any());

        // Perform the web request
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/secom/" + SEARCH_SERVICE_INTERFACE_PATH)
                        .queryParam("page", 0)
                        .queryParam("pageSize", Integer.MAX_VALUE)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(Mono.just(searchFilterObject), SearchFilterObject.class))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResponseSearchObject.class)
                .consumeWith(response -> {
                    ResponseSearchObject result = response.getResponseBody();
                    assertNotNull(result);
                    assertNotNull(result.getSearchServiceResult());
                    assertEquals(this.instances.size(), result.getSearchServiceResult().size());

                    // Test each of the result entries
                    for(SearchObjectResult searchObjectResult: result.getSearchServiceResult()) {
                        int i = result.getSearchServiceResult().indexOf(searchObjectResult);
                        assertEquals(this.instances.get(i).getInstanceId(), searchObjectResult.getInstanceId());
                        assertEquals(this.instances.get(i).getName(), searchObjectResult.getName());
                        assertEquals(this.instances.get(i).getStatus().toString(), searchObjectResult.getStatus());
                        assertEquals(this.instances.get(i).getVersion(), searchObjectResult.getVersion());
                        assertEquals(this.instances.get(i).getInstanceAsXml().getContent(), searchObjectResult.getInstanceAsXml());
                        assertEquals(SECOM_DataProductType.OTHER, searchObjectResult.getDataProductType());
                    }
                });
    }
}