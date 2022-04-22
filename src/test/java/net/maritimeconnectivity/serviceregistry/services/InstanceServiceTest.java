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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.DuplicateDataException;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.*;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.UserContext;
import org.apache.commons.io.IOUtils;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManagerFactory;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstanceServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    private InstanceService instanceService;

    /**
     * The Instance Repository Mock.
     */
    @Mock
    private InstanceRepo instanceRepo;

    /**
     * The XML Service Mock.
     */
    @Mock
    private XmlService xmlService;

    /**
     * The Doc Service Mock.
     */
    @Mock
    private DocService docService;

    /**
     * The LedgerRequest Service Mock.
     */
    @Mock
    private LedgerRequestService ledgerRequestService;

    /**
     * The User Context.
     */
    @Mock
    private UserContext userContext;

    /**
     * The Entity Management Factory.
     */
    @Mock
    private EntityManagerFactory entityManagerFactory;

    // Test Variables
    private List<Instance> instances;
    private Pageable pageable;
    private Xml xml;
    private Instance newInstance;
    private Instance existingInstance;        // No need to add this since
    private Point point;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() {
        // Initialise the instances list
        this.instances = new ArrayList<>();
        for(long i=0; i<15; i++) {
            Instance instance = new Instance();
            instance.setId(i);
            instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", i));
            instance.setName(String.format("Test Instance %d", i));
            instance.setVersion("0.0." + i);
            this.instances.add(instance);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create the XML object of the instance
        Xml xml = new Xml();
        xml.setId(1000L);
        xml.setName("XML Name");
        xml.setContentContentType("G1128 Instance Specification XML");
        xml.setContent(null);
        xml.setComment("No comment");

        // Create a temp geometry factory to get some shapes
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        this.point = factory.createPoint(new Coordinate(52.001, 1.002));

        // Create a new instance
        this.newInstance = new Instance();
        this.newInstance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.newInstance.setName("Instance Name");
        this.newInstance.setVersion("1.0.0");
        this.newInstance.setComment("No comment");
        this.newInstance.setStatus(ServiceStatus.RELEASED);
        this.newInstance.setGeometry(this.point);
        this.newInstance.setInstanceAsXml(xml);

        // Create an instance with an ID
        this.existingInstance = new Instance();
        this.existingInstance.setId(100L);
        this.existingInstance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.existingInstance.setName("Instance Name");
        this.existingInstance.setVersion("1.0.0");
        this.existingInstance.setComment("No comment");
        this.existingInstance.setStatus(ServiceStatus.RELEASED);
        this.existingInstance.setGeometry(this.point);
        this.existingInstance.setInstanceAsXml(xml);
    }

    /**
     * Test that we can retrieve all the instances currently present in the
     * database through a paged call.
     */
    @Test
    void testFindAll() {
        // Created a result page to be returned by the mocked repository
        Page<Instance> page = new PageImpl<>(this.instances.subList(0, 5), this.pageable, this.instances.size());
        doReturn(page).when(this.instanceRepo).findAll(this.pageable);

        // Perform the service call
        Page<Instance> result = this.instanceService.findAll(pageable);

        // Test the result
        assertEquals(page.getSize(), result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getSize(); i++){
            assertEquals(result.getContent().get(i), this.instances.get(i));
        }
    }

    /**
     * Test that we can retrieve a single instance entry based on the instance
     * ID and all the eager relationships are loaded.
     */
    @Test
    void testFindOne() throws DataNotFoundException {
        doReturn(this.existingInstance).when(instanceRepo).findOneWithEagerRelationships(this.existingInstance.getId());

        // Perform the service call
        Instance result = this.instanceService.findOne(this.existingInstance.getId());

        // Make sure the eager relationships repo call was called
        verify(this.instanceRepo, times(1)).findOneWithEagerRelationships(this.existingInstance.getId());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingInstance.getId(), result.getId());
        assertEquals(this.existingInstance.getName(), result.getName());
        assertEquals(this.existingInstance.getVersion(), result.getVersion());
        assertEquals(this.existingInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.existingInstance.getKeywords(), result.getKeywords());
        assertEquals(this.existingInstance.getComment(), result.getComment());
        assertEquals(this.existingInstance.getEndpointUri(), result.getEndpointUri());
        assertEquals(this.existingInstance.getEndpointType(), result.getEndpointType());
        assertEquals(this.existingInstance.getMmsi(), result.getMmsi());
        assertEquals(this.existingInstance.getImo(), result.getImo());
        assertEquals(this.existingInstance.getServiceType(), result.getServiceType());
        assertEquals(this.existingInstance.getUnlocode(), result.getUnlocode());
        assertEquals(this.existingInstance.getGeometry(), result.getGeometry());
    }

    /**
     * Test that when there is a validation error in the provided instance
     * object, the saving operation will fail and not continue.
     */
    @Test
    void testSaveValidationError() throws XMLValidationException, GeometryParseException, DataNotFoundException {
        doThrow(XMLValidationException.class).when(this.instanceService).validateInstanceForSave(any());

        // Perform the service call
        assertThrows(XMLValidationException.class, () ->
                this.instanceService.save(this.newInstance)
        );

        // And also that no saving calls took place in the repository
        verify(this.instanceRepo, never()).save(any());
    }

    /**
     * Test that when we are trying to update an existing instance but the
     * provided ID is invalid, a DataNotFoundException will be thrown.
     */
    @Test
    void testSaveNoValidId() {
        doReturn(Optional.empty()).when(this.instanceRepo).findById(this.existingInstance.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.instanceService.save(this.existingInstance)
        );

        // And also that no saving calls took place in the repository
        verify(this.instanceRepo, never()).save(any());
    }

    /**
     * Test that when we are trying to register an instance with existing mrn and version
     * a DuplicationKeyException will be thrown.
     */
    @Test
    void testSaveWithDuplicatedMRNVersion() {
        doReturn(Optional.of(this.newInstance)).when(this.instanceRepo).findByDomainIdAndVersion(this.newInstance.getInstanceId(), this.newInstance.getVersion());

        // Perform the service call
        assertThrows(DuplicateDataException.class, () ->
                this.instanceService.save(this.newInstance)
        );

        // And also that no saving calls took place in the repository
        verify(this.instanceRepo, never()).save(any());
    }

    /**
     * Test that we can save correctly a new or existing instance if all
     * the validation checks are successful.
     */
    @Test
    void testSaveWithGeometry() throws XMLValidationException, GeometryParseException, ParseException, JsonProcessingException, DataNotFoundException {
        doReturn(this.newInstance).when(this.instanceRepo).save(any());
        doNothing().when(this.instanceService).validateInstanceForSave(any());
        doReturn(Optional.of(new UserToken()).map(t -> {t.setOrganisation("org"); return t;})).when(this.userContext).getJwtToken();

        // Perform the service call
        Instance result = this.instanceService.save(this.newInstance);

        // Test the result
        assertEquals(this.newInstance.getId(), result.getId());
        assertEquals(this.newInstance.getName(), result.getName());
        assertEquals(this.newInstance.getVersion(), result.getVersion());
        assertEquals(this.newInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.newInstance.getKeywords(), result.getKeywords());
        assertEquals(this.newInstance.getComment(), result.getComment());
        assertEquals(this.newInstance.getEndpointUri(), result.getEndpointUri());
        assertEquals(this.newInstance.getEndpointType(), result.getEndpointType());
        assertEquals(this.newInstance.getMmsi(), result.getMmsi());
        assertEquals(this.newInstance.getImo(), result.getImo());
        assertEquals(this.newInstance.getServiceType(), result.getServiceType());
        assertEquals(this.newInstance.getUnlocode(), result.getUnlocode());
        assertEquals(this.newInstance.getGeometry(), result.getGeometry());
        assertEquals("org", this.newInstance.getOrganizationId());

        // Also that a saving call took place in the repository
        verify(this.instanceRepo, times(1)).save(this.newInstance);
    }

    /**
     * Test that we can save correctly a new or existing instance if all
     * the validation checks are successful, even if there is no geometry
     * provided. In that case, the whole world will be assigned as the
     * instance geometry.
     */
    @Test
    void testSaveNoGeometry() throws XMLValidationException, GeometryParseException, ParseException, JsonProcessingException, DataNotFoundException {
        doAnswer(i -> i.getArguments()[0]).when(this.instanceRepo).save(any());
        doNothing().when(this.instanceService).validateInstanceForSave(any());
        doReturn(Optional.of(new UserToken()).map(t -> {t.setOrganisation("org"); return t;})).when(this.userContext).getJwtToken();

        // Make sure the geometry of the instance is empty
        this.newInstance.setGeometry(null);

        // Perform the service call
        Instance result = this.instanceService.save(this.newInstance);

        // Test the result
        assertEquals(this.newInstance.getId(), result.getId());
        assertEquals(this.newInstance.getName(), result.getName());
        assertEquals(this.newInstance.getVersion(), result.getVersion());
        assertEquals(this.newInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.newInstance.getKeywords(), result.getKeywords());
        assertEquals(this.newInstance.getComment(), result.getComment());
        assertEquals(this.newInstance.getEndpointUri(), result.getEndpointUri());
        assertEquals(this.newInstance.getEndpointType(), result.getEndpointType());
        assertEquals(this.newInstance.getMmsi(), result.getMmsi());
        assertEquals(this.newInstance.getImo(), result.getImo());
        assertEquals(this.newInstance.getServiceType(), result.getServiceType());
        assertEquals(this.newInstance.getUnlocode(), result.getUnlocode());
        assertEquals("org", this.newInstance.getOrganizationId());

        // Check the geometry
        assertNotNull(result.getGeometry());
        assertEquals(new ObjectMapper().readTree(this.instanceService.wholeWorldGeoJson).get("type"), result.getGeometryJson().get("type"));
        assertEquals(new ObjectMapper().readTree(this.instanceService.wholeWorldGeoJson).get("coordinates"), result.getGeometryJson().get("coordinates"));

        // Also that a saving call took place in the repository
        verify(this.instanceRepo, times(1)).save(this.newInstance);
    }

    /**
     * Test that we can successfully delete an existing instance.
     */
    @Test
    void testDelete() {
        doReturn(Optional.of(this.existingInstance)).when(this.instanceRepo).findById(this.existingInstance.getId());
        doNothing().when(this.instanceRepo).deleteById(this.existingInstance.getId());

        // Perform the service call
        this.instanceService.delete(this.existingInstance.getId());

        // Verify that a deletion call took place in the repository
        verify(this.instanceRepo, times(1)).deleteById(this.existingInstance.getId());
    }

    /**
     * Test that if we try to delete a non-existing instance then a DataNotFound
     * exception will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.instanceService.delete(this.existingInstance.getId())
        );
    }

    /**
     * Test that we can update the status of a service in a separate call.
     */
    @Test
    void testUpdateStatus() throws DataNotFoundException, XMLValidationException, GeometryParseException, JAXBException, ParseException, JsonProcessingException {
        doReturn(this.existingInstance).when(this.instanceRepo).findOneWithEagerRelationships(this.existingInstance.getId());
        doNothing().when(this.instanceService).validateInstanceForSave(any());

        // Perform the service call
        this.instanceService.updateStatus(this.existingInstance.getId(), ServiceStatus.DEPRECATED);

        // Capture the save instance i
        ArgumentCaptor<Instance> argument = ArgumentCaptor.forClass(Instance.class);
        verify(this.instanceRepo, times(1)).save(argument.capture());
        assertEquals(ServiceStatus.DEPRECATED, argument.getValue().getStatus());
    }

    /**
     * Test that we if we do NOT find the instance based on the provided ID,
     * to update the status of, a DataNotFoundException will be thrown.
     */
    @Test
    void testUpdateStatusNotFound() throws DataNotFoundException {
        doReturn(null).when(this.instanceRepo).findOneWithEagerRelationships(this.existingInstance.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.instanceService.updateStatus(this.existingInstance.getId(), ServiceStatus.DEPRECATED)
        );

        // Since this is a validation exception, no saving should have been attempted
        verify(this.instanceRepo, never()).save(any());
    }

    /**
     * Test that we if an error occurs while updating the status of an
     * instance, this will be propagated by the instance service.
     */
    @Test
    void testUpdateStatusError() throws DataNotFoundException, XMLValidationException, GeometryParseException {
        doReturn(this.existingInstance).when(this.instanceRepo).findOneWithEagerRelationships(this.existingInstance.getId());
        doThrow(XMLValidationException.class).when(this.instanceService).validateInstanceForSave(any());

        // Perform the service call
        assertThrows(XMLValidationException.class, () ->
                this.instanceService.updateStatus(this.existingInstance.getId(), ServiceStatus.DEPRECATED)
        );

        // Since this is a validation exception, no saving should have been attempted
        verify(this.instanceRepo, never()).save(any());
    }

    /**
     * Test that we can search for all the instances based on their specific
     * domain ID.
     */
    @Test
    void testFindAllByDomainId() {
        doReturn(this.instances).when(this.instanceRepo).findByDomainId("domainId");

        // Perform the service call
        List<Instance> result = this.instanceService.findAllByDomainId("domainId");

        // Test the result
        assertEquals(instances.size(), result.size());

        // Test each of the result entries
        for(int i=0; i < result.size(); i++){
            assertEquals(result.get(i), this.instances.get(i));
        }
    }

    /**
     * Test that we can retrieve a unique instance based on the provided
     * specific domain ID and version of the instance.
     */
    @Test
    void testFindByDomainIdAndVersion() {
        doReturn(Optional.of(this.existingInstance)).when(this.instanceRepo).findByDomainIdAndVersionEagerRelationships("domainId", "0.0.1Test");

        // Perform the service call
        Instance result = this.instanceService.findByDomainIdAndVersion("domainId", "0.0.1Test");

        // Make sure the eager relationships repo call was called
        verify(this.instanceRepo, times(1)).findByDomainIdAndVersionEagerRelationships("domainId", "0.0.1Test");

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingInstance.getId(), result.getId());
        assertEquals(this.existingInstance.getName(), result.getName());
        assertEquals(this.existingInstance.getVersion(), result.getVersion());
        assertEquals(this.existingInstance.getInstanceId(), result.getInstanceId());
        assertEquals(this.existingInstance.getKeywords(), result.getKeywords());
        assertEquals(this.existingInstance.getComment(), result.getComment());
        assertEquals(this.existingInstance.getEndpointUri(), result.getEndpointUri());
        assertEquals(this.existingInstance.getEndpointType(), result.getEndpointType());
        assertEquals(this.existingInstance.getMmsi(), result.getMmsi());
        assertEquals(this.existingInstance.getImo(), result.getImo());
        assertEquals(this.existingInstance.getServiceType(), result.getServiceType());
        assertEquals(this.existingInstance.getUnlocode(), result.getUnlocode());
        assertEquals(this.existingInstance.getGeometry(), result.getGeometry());
    }

    /**
     * Test that we can correctly retrieve the latest version of a service
     * providing just the specific domain ID.
     */
    @Test
    void testFindLatestVersionByDomainId() {
        doReturn(this.instances).when(this.instanceRepo).findByDomainIdEagerRelationships("domainId");

        // Perform the service call
        Instance result = this.instanceService.findLatestVersionByDomainId("domainId");

        // Test the result
        assertNotNull(result);
        assertEquals(this.instances.get(this.instances.size()-1).getId(), result.getId());
        assertEquals(this.instances.get(this.instances.size()-1).getName(), result.getName());
        assertEquals(this.instances.get(this.instances.size()-1).getVersion(), result.getVersion());
        assertEquals(this.instances.get(this.instances.size()-1).getInstanceId(), result.getInstanceId());
        assertEquals(this.instances.get(this.instances.size()-1).getKeywords(), result.getKeywords());
        assertEquals(this.instances.get(this.instances.size()-1).getComment(), result.getComment());
        assertEquals(this.instances.get(this.instances.size()-1).getEndpointUri(), result.getEndpointUri());
        assertEquals(this.instances.get(this.instances.size()-1).getEndpointType(), result.getEndpointType());
        assertEquals(this.instances.get(this.instances.size()-1).getMmsi(), result.getMmsi());
        assertEquals(this.instances.get(this.instances.size()-1).getImo(), result.getImo());
        assertEquals(this.instances.get(this.instances.size()-1).getServiceType(), result.getServiceType());
        assertEquals(this.instances.get(this.instances.size()-1).getUnlocode(), result.getUnlocode());
        assertEquals(this.instances.get(this.instances.size()-1).getGeometry(), result.getGeometry());
    }

    /**
     * That that we can validate incoming requests correctly based on the
     * provided XML and geometry values.
     */
    @Test
    void testValidateInstanceForSave() throws IOException, XMLValidationException, GeometryParseException, DataNotFoundException {
        // Load a valid test XML for our instance
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Set the content in a new instance to be validated
        this.newInstance.getInstanceAsXml().setContent(xmlContent);

        // Perform the service call
        this.instanceService.validateInstanceForSave(this.newInstance);
    }

    /**
     * That that we can detect XML errors when validating an incoming instance
     * saving request.
     */
    @Test
    void testValidateInstanceForSaveXMLError() throws IOException {
        // Load a valid test XML for our instance
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Set the content in a new instance to be validated
        String xmlContentDistorted = xmlContent+"some wrong stuff in the end!!!";
        this.newInstance.getInstanceAsXml().setContent(xmlContentDistorted);

        // Perform the service call
        assertThrows(XMLValidationException.class, () ->
                this.instanceService.validateInstanceForSave(this.newInstance)
        );
    }

    /**
     * Test that we can detect geometry errors when validating an incoming instance
     * saving request.
     */
    @Test
    void testValidateInstanceForSaveGeometryError() throws IOException {
        // Load a valid test XML for our instance
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Set the content in a new instance to be validated
        String xmlContentDistorted = xmlContent.replaceAll("POLYGON","ERRORGON");
        this.newInstance.getInstanceAsXml().setContent(xmlContentDistorted);

        // Perform the service call
        assertThrows(GeometryParseException.class, () ->
                this.instanceService.validateInstanceForSave(this.newInstance)
        );
    }

    /**
     * Test that we can detect when we receive an invalid document when
     * validating an incoming  instance saving request.
     */
    @Test
    void testValidateInstanceForSaveDocError() throws IOException {
        // Load a valid test XML for our instance
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Set the content in a new instance to be validated
        this.existingInstance.getInstanceAsXml().setContent(xmlContent);

        // Set the doc in a new instance to be validated
        Doc doc = new Doc();
        doc.setId(666L);
        this.existingInstance.setInstanceAsDoc(doc);

        doReturn(Optional.of(this.existingInstance)).when(this.instanceRepo).findById(this.existingInstance.getId());
        doThrow(DataNotFoundException.class).when(this.docService).findOne(doc.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.instanceService.validateInstanceForSave(this.existingInstance)
        );
    }

    /**
     * Test that we can retrieve the paged list of instances for a Datatables
     * pagination request (which by the way also includes search and sorting
     * definitions).
     */
    @Test
    void testHandleDatatablesPagingRequest() {
        // First create the pagination request
        DtPagingRequest dtPagingRequest = new DtPagingRequest();
        dtPagingRequest.setStart(0);
        dtPagingRequest.setLength(5);

        // Set the pagination request columns
        dtPagingRequest.setColumns(new ArrayList());
        Stream.of("name",
                "version",
                "lastUpdatedAt",
                "instanceId",
                "keywords",
                "status",
                "organizationId",
                "endpointUri",
                "mmsi",
                "imo",
                "serviceType")
                .map(DtColumn::new)
                .forEach(dtPagingRequest.getColumns()::add);

        // Set the pagination request ordering
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        dtPagingRequest.setOrder(Collections.singletonList(dtOrder));

        // Set the pagination search
        DtSearch dtSearch = new DtSearch();
        dtSearch.setValue("search-term");
        dtPagingRequest.setSearch(dtSearch);

        // Mock the full text query
        SearchQuery<Instance> mockedQuery = mock(SearchQuery.class);
        SearchResult<Instance> searchResult = mock(SearchResult.class);
        SearchResultTotal searchResultTotal = mock(SearchResultTotal.class);
        doReturn(searchResult).when(mockedQuery).fetch(any(), any());
        doReturn(this.instances.subList(0, 5)).when(searchResult).hits();
        doReturn(searchResultTotal).when(searchResult).total();
        doReturn(10L).when(searchResultTotal).hitCount();
        doReturn(mockedQuery).when(this.instanceService).getSearchInstanceQueryByText(any(), any());

        // Perform the service call
        Page<Instance> result = this.instanceService.handleDatatablesPagingRequest(dtPagingRequest);

        // Validate the result
        assertNotNull(result);
        assertEquals(5, result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getContent().size(); i++){
            assertEquals(this.instances.get(i), result.getContent().get(i));
        }
    }

    /**
     * Test that we can retrieve the paged list of instances based on a
     * Lucene search query. The pages request parameter contains any paging
     * and soring information.
     */
    @Test
    void testHandleSearchQueryRequest() {
        // Mock the full text query
        SearchQuery<Instance> mockedQuery = mock(SearchQuery.class);
        SearchResult<Instance> searchResult = mock(SearchResult.class);
        SearchResultTotal searchResultTotal = mock(SearchResultTotal.class);
        doReturn(searchResult).when(mockedQuery).fetch(any(), any());
        doReturn(this.instances.subList(0, 5)).when(searchResult).hits();
        doReturn(searchResultTotal).when(searchResult).total();
        doReturn(10L).when(searchResultTotal).hitCount();
        doReturn(mockedQuery).when(this.instanceService).getSearchInstanceQueryByQueryString(any(), any(), any());

        // Perform the service call
        Page<Instance> result = this.instanceService.handleSearchQueryRequest("search-term", this.point, this.pageable);

        // Validate the result
        assertNotNull(result);
        assertEquals(5, result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getContent().size(); i++){
            assertEquals(this.instances.get(i), result.getContent().get(i));
        }
    }

}
