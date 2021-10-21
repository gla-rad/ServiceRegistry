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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.*;
import net.maritimeconnectivity.serviceregistry.models.domain.*;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.CoverageArea;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.ServiceDesignReference;
import org.iala_aism.g1128.v1_3.serviceinstanceschema.ServiceInstance;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing Instance.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class InstanceService {

    /**
     * The Entity Manager.
     */
    @Autowired
    EntityManager entityManager;

    /**
     * The Instance Repository.
     */
    @Autowired
    InstanceRepo instanceRepo;

    /**
     * The XML Service.
     */
    @Autowired
    XmlService xmlService;

    /**
     * The Doc Service.
     */
    @Autowired
    DocService docService;

    /**
     * The LedgerRequest Service.
     */
    @Autowired(required = false)
    private LedgerRequestService ledgerRequestService;

    /**
     * The UnLoCode Service.
     *
     * Lazy load to avoid loading it every time.
     */
    @Autowired
    @Lazy
    private UnLoCodeService unLoCodeService;

    /**
     * The User Context.
     */
    @Autowired
    private UserContext userContext;

    // Service Variables
    private final String[] searchFields = new String[] {
            "name",
            "version",
            "lastUpdatedAt",
            "instanceId",
            "keywords",
            "status",
            "organizationId",
            "endpointUri",
            "mmsi",
            "imo",
            "serviceType"
    };

    /**
     * Definition of the whole world area in GeoJSON.
     */
    String wholeWorldGeoJson = "{\n" +
            "  \"type\": \"Polygon\",\n" +
            "  \"coordinates\": [\n" +
            "    [[-180, -90], [-180, 90], [180, 90], [180, -90], [-180, -90]]\n" +
            "  ]\n" +
            "}";

    /**
     * Get all the instances.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Instance> findAll(Pageable pageable) {
        log.debug("Request to get all Instances");
        return this.instanceRepo.findAll(pageable);
    }

    /**
     * Get one instance by ID.
     *
     * @param id        the ID of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findOne(Long id) throws DataNotFoundException {
        log.debug("Request to get Instance : {}", id);
        return Optional.ofNullable(id).map(this.instanceRepo::findOneWithEagerRelationships)
                .orElseThrow(() -> new DataNotFoundException("No instance found for the provided ID", null));
    }

    /**
     * Save a instance.
     *
     * @param instance  the entity to save
     * @return the persisted entity
     */
    @Transactional
    public Instance save(Instance instance) throws DataNotFoundException, XMLValidationException, GeometryParseException, JsonProcessingException, ParseException {
        log.debug("Request to save Instance : {}", instance);

        // First, validate the object
        this.validateInstanceForSave(instance);

        // Don't accept empty geometry value, set whole earth coverage
        if (instance.getGeometry() == null) {
            log.debug("Setting whole-earth coverage");
            instance.setGeometryJson(new ObjectMapper().readTree(wholeWorldGeoJson));
        }

        // Populate the save operation fields if required
        // For new entries
        if(instance.getId() == null) {
            instance.setOrganizationId(this.userContext.getJwtToken().map(UserToken::getOrganisation).orElse(null));
        }

        // If the publication date is missing
        if(StringUtils.isBlank(instance.getPublishedAt())) {
            instance.setPublishedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // And don't forget the last update
        if(StringUtils.isBlank(instance.getLastUpdatedAt())) {
            instance.setLastUpdatedAt(instance.getPublishedAt());
        } else {
            instance.setLastUpdatedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }

        // The save and return
        return this.instanceRepo.save(instance);
    }

    /**
     * Delete the instance by ID.
     *
     * @param id        the ID of the entity
     */
    @Transactional(propagation = Propagation.NESTED)
    public void delete(Long id) throws DataNotFoundException {
        log.debug("Request to delete Instance : {}", id);
        this.instanceRepo.findById(id)
                .ifPresentOrElse(i -> {
                    Optional.ofNullable(this.ledgerRequestService)
                            .ifPresent(lrs -> lrs.deleteByInstanceId(i.getId()));
                    this.instanceRepo.deleteById(i.getId());
                }, () -> {
                    throw new DataNotFoundException("No instance found for the provided ID", null);
                });
    }

    /**
     * Update the status of an instance by ID.
     *
     * @param id        the ID of the entity
     * @param status    the status of the entity
     * @throws Exception any exceptions thrown while updating the status
     */
    @Transactional
    public void updateStatus(Long id, ServiceStatus status) throws DataNotFoundException, JAXBException, XMLValidationException, ParseException, JsonProcessingException, GeometryParseException, DuplicateKeyException {
        log.debug("Request to update status of Instance : {}", id);

        // Try to find if the instance does indeed exist
        Instance instance = Optional.of(id)
                .map(this.instanceRepo::findOneWithEagerRelationships)
                .orElseThrow(() -> new DataNotFoundException("No instance found for the provided ID", null));

        // Update the instance status
        try {
            Xml instanceXml = instance.getInstanceAsXml();
            if (instanceXml != null && instanceXml.getContent() != null) {
                // Unmarshall the XML, update the status and re-marshall the to XML
                ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(instanceXml.getContent());
                serviceInstance.setStatus(status);
                instanceXml.setContent(new G1128Utils<>(ServiceInstance.class).marshalG1128(serviceInstance));
                // Save XML
                this.xmlService.save(instanceXml);
            }
            instance.setStatus(status);
            instance.setInstanceAsXml(instanceXml);
            save(instance);
        } catch (JAXBException | XMLValidationException | ParseException | JsonProcessingException | GeometryParseException | DuplicateKeyException ex) {
            log.error("Problem during instance status update.", ex);
            throw ex;
        }
    }

    /**
     * Update the ledger status of an instance by ID.
     *
     * @param id            the ID of the entity
     * @param ledgerStatus  the ledger status of the entity
     */
    @Transactional
    public LedgerRequest updateLedgerStatus(@NotNull Long id, @NotNull LedgerRequestStatus ledgerStatus, String reason) {
        return Optional.ofNullable(this.ledgerRequestService)
                .map(lss -> {
                    // First make sure the instance is valid
                    final Instance instance = this.findOne(id);

                    // Get a ledger request and if it does not exist create one
                    final LedgerRequest request = Optional.of(instance)
                            .filter(i -> Objects.nonNull(i.getLedgerRequest()))
                            .map(Instance::getLedgerRequest)
                            .orElseGet(() ->  {
                                final LedgerRequest newRequest = new LedgerRequest();
                                newRequest.setServiceInstance(instance);
                                newRequest.setStatus(LedgerRequestStatus.CREATED);
                                return this.ledgerRequestService.save(newRequest);
                            });

                    // Finally, update the status
                    return lss.updateStatus(request.getId(), ledgerStatus, reason);
                })
                .orElseThrow(() -> new LedgerConnectionException(MsrErrorConstant.LEDGER_NOT_CONNECTED, null));
    }

    /**
     * Get all the instances that match a domain specific ID (for example,
     * maritime ID), regardless of their version.
     *
     * @param domainId      the domain specific ID of the instance
     * @return the list of matching entities
     */
    public List<Instance> findAllByDomainId(String domainId) {
        log.debug("Request to get Instance by domain id {} and version {} without restriction");
        return this.instanceRepo.findByDomainId(domainId);
    }

    /**
     * Get one instance by domain specific id (for example, maritime id) and
     * version.
     *
     * @param domainId      the domain specific id of the instance
     * @param version       the version identifier of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findByDomainIdAndVersion(String domainId, String version) {
        log.debug("Request to get Instance by domain id {} and version {}", domainId, version);
        return this.instanceRepo.findByDomainIdAndVersionEagerRelationships(domainId, version)
                .orElseGet(() -> {
                    log.debug("Could not find instance for domain id {} and version {}", domainId, version);
                    return null;
                });
    }

    /**
     * Get one instance by domain specific ID (for example, maritime ID), only
     * return the latest version.
     *
     * @param domainId      the domain specific id of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findLatestVersionByDomainId(String domainId) {
        log.debug("Request to get Instance by domain id {}", domainId);
        return this.instanceRepo.findByDomainIdEagerRelationships(domainId).stream()
                .max(Comparator.comparing(i -> new DefaultArtifactVersion(i.getVersion())))
                .orElseGet(() -> {
                    log.debug("Could not find a live instance for domain id {}", domainId);
                    return null;
                });
    }

    /**
     * Prepare instance for save.
     *
     * <p>This preparation has two phases:</p>
     * <ul>
     *    <li>Validation of XML and parsing basic data (XMLValidationException if fails)</li>
     *    <li>Parsing GeoData (GeometryParseException if fails)</li>
     * </ul>
     *
     * @param instance      the instance to be saved
     * @throws XMLValidationException If fails first phase (Validating and parsing XML)
     * @throws GeometryParseException If fails second phase (Parsing geo data)
     */
    public void validateInstanceForSave(Instance instance) throws XMLValidationException, GeometryParseException {
        if(instance == null) {
            return;
        }

        // Try to find the instance if an ID is provided
        if(instance.getId() != null) {
            this.instanceRepo.findById(instance.getId())
                    .ifPresentOrElse(existingInstance -> {
                        // We need to be able to update instances with providing
                        // the whole instance doc every time. Therefore, if
                        // we just have an ID but not file, we can try to load
                        // the saved doc from the database into the input
                        // instance. Note that we don't actually throw an error
                        // for invalid docs... it's just an ID anyway right?
                        if(Objects.nonNull(instance.getInstanceAsDoc()) && Objects.isNull(instance.getInstanceAsDoc().getFilecontent())) {
                            Optional.of(instance.getInstanceAsDoc())
                                    .map(Doc::getId)
                                    .filter(docId -> Objects.nonNull(existingInstance.getInstanceAsDoc()))
                                    .filter(docId -> docId.equals(existingInstance.getInstanceAsDoc().getId()))
                                    .map(this.docService::findOne)
                                    .ifPresent(doc -> instance.setInstanceAsDoc(doc));
                        }
                    }, () -> {
                        throw new DataNotFoundException("No instance found for the provided ID", null);
                    });
        }
        // Else check for MRN and version conflicts with other instances
        else if(instance.getInstanceId() != null && instance.getVersion() != null) {
            this.instanceRepo.findByDomainIdAndVersion(instance.getInstanceId(), instance.getVersion())
                    .ifPresent(i -> { throw new DuplicateDataException("Duplicated instance with the same MRN and version found.", null); });
        }

        try {
            XmlUtil.validateXml(instance.getInstanceAsXml().getContent(), G1128Utils.SOURCES_LIST);
        } catch (SAXException e) {
            throw new XMLValidationException("Service Instance XML is not valid.", e);
        } catch (IOException e) {
            throw new XMLValidationException("Service Instance XML could not be parsed.", e);
        }

        try {
            this.parseInstanceAttributesFromXML(instance);
        } catch (JAXBException e) {
            throw new XMLValidationException("Service Instance contains invalid attributes.", e);
        }

        try {
            this.parseInstanceGeometryFromXML(instance);
        } catch (JAXBException | ParseException e) {
            throw new GeometryParseException("Service Instance geometry parsing error.", e);
        }
    }

    /**
     * Handles a datatables pagination request and returns the results list in
     * an appropriate format to be viewed by a datatables jQuery table.
     *
     * @param dtPagingRequest the Datatables pagination request
     * @return the paged response
     */
    @Transactional(readOnly = true)
    public Page<Instance> handleDatatablesPagingRequest(DtPagingRequest dtPagingRequest) {
        // Create the search query
        FullTextQuery searchQuery = this.searchInstanceQuery(dtPagingRequest.getSearch().getValue());
        searchQuery.setFirstResult(dtPagingRequest.getStart());
        searchQuery.setMaxResults(dtPagingRequest.getLength());

        // Add sorting if requested
        Optional.of(dtPagingRequest)
                .map(DtPagingRequest::getLucenceSort)
                .filter(ls -> ls.getSort().length > 0)
                .ifPresent(searchQuery::setSort);

        // Map the results to a paged response
        return Optional.of(searchQuery)
                .map(FullTextQuery::getResultList)
                .map(instances -> new PageImpl<Instance>(instances, dtPagingRequest.toPageRequest(), searchQuery.getResultSize()))
                .orElseGet(() -> new PageImpl<>(Collections.emptyList(), dtPagingRequest.toPageRequest(), 0));
    }

    /**
     * Parse instance attributes from the xml payload for search/filtering
     *
     * @param instance      the instance to parse
     * @return an instance with its attributes set
     * @throws JAXBException if the XML is invalid or required attributes not present
     */
    protected void parseInstanceAttributesFromXML(Instance instance) throws JAXBException {
        log.debug("Parsing XML: " + instance.getInstanceAsXml().getContent());
        ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(instance.getInstanceAsXml().getContent());

        // Populate the instance object
        instance.setName(serviceInstance.getName());
        instance.setVersion(serviceInstance.getVersion());
        instance.setInstanceId(serviceInstance.getId());
        instance.setKeywords(serviceInstance.getKeywords());
        // instance.setStatus(InstanceStatus.fromString(serviceInstance.getStatus().value())); // Do we need this?
        instance.setComment(serviceInstance.getDescription());
        instance.setEndpointUri(serviceInstance.getEndpoint());
        instance.setMmsi(serviceInstance.getMMSI());
        instance.setImo(serviceInstance.getIMO());
        instance.setServiceType(serviceInstance.getServiceType());
        instance.setUnlocode(serviceInstance.getCoversAreas()
                .getCoversAreasAndUnLoCodes()
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList()));
        instance.setDesigns(Stream.of(serviceInstance.getImplementsServiceDesign())
                .collect(Collectors.toMap(ServiceDesignReference::getId, ServiceDesignReference::getVersion)));
    }

    /**
     * Parse instance geometry from the xml payload for search/filtering
     *
     * @param instance      the instance to parse
     * @return an instance with its attributes set
     * @throws Exception if the XML is invalid or attributes not present
     */
    protected void parseInstanceGeometryFromXML(Instance instance) throws JAXBException, ParseException {
        log.debug("Parsing XML: " + instance.getInstanceAsXml().getContent());
        ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(instance.getInstanceAsXml().getContent());

        List<String> unLoCode = serviceInstance.getCoversAreas()
                .getCoversAreasAndUnLoCodes()
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        List<CoverageArea> coverageAreas = serviceInstance.getCoversAreas()
                .getCoversAreasAndUnLoCodes()
                .stream()
                .filter(CoverageArea.class::isInstance)
                .map(CoverageArea.class::cast)
                .collect(Collectors.toList());

        // UN/LOCODE and Coverage Geometry are supported simultaneously.
        // However, for geo-searches, Coverage takes precedence over UN/LOCODE.
        if (unLoCode != null && unLoCode.size() > 0) {
            instance.setUnlocode(unLoCode);
        }

        // Check the coverage areas
        if (coverageAreas != null && coverageAreas.size() > 0) {
            List<Geometry> geometryList = new ArrayList();
            for(CoverageArea coverageArea : coverageAreas) {
                JsonNode geoJson = WKTUtil.convertWKTtoGeoJson(coverageArea.getGeometryAsWKT());
                geometryList.add(Optional.of(geoJson)
                        .map(GeometryJSONConverter::convertToGeometry)
                        .orElseThrow(() -> new ParseException("Invalid geometry detected")));
            }
            instance.setGeometry(new GeometryCombiner(geometryList).combine());
        } else if (unLoCode != null && unLoCode.size() > 0) {
            unLoCodeService.applyUnLoCodeMapping(instance, unLoCode);
        }
    }

    /**
     * Constructs a hibernate search query using Lucene based on the provided
     * search test. This query will be based solely on the instances table and
     * will include the following fields:
     * <ul>
     *  <li>Version</li>
     *  <li>Version</li>
     *  <li>Last Updated At</li>
     *  <li>Status</li>
     *  <li>Status</li>
     *  <li>Organization ID</li>
     *  <li>ndpoint URI</li>
     *  <li>MMSI</li>
     *  <li>IMO</li>
     *  <li>Service Type</li>
     * </ul>
     *
     * @param searchText    the text to be searched
     * @return the full text query
     */
    protected FullTextQuery searchInstanceQuery(String searchText) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Instance.class)
                .get();

        Query luceneQuery = queryBuilder
                .keyword()
                .wildcard()
                .onFields(this.searchFields)
                .matching(Optional.ofNullable(searchText).orElse("").toLowerCase() + "*")
                .createQuery();

        return fullTextEntityManager.createFullTextQuery(luceneQuery, Instance.class);
    }

}
