/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.eNav.utils.G1128Utils;
import net.maritimeconnectivity.serviceregistry.exceptions.*;
import net.maritimeconnectivity.serviceregistry.models.domain.*;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.models.dto.datatables.DtPagingRequest;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.iala_aism.g1128.v1_7.serviceinstanceschema.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.io.ParseException;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * The Entity Management Factory.
     */
    @Autowired
    EntityManagerFactory entityManagerFactory;

    // Service Variables
    private final String[] searchFields = new String[] {
            "name",
            "version",
            "comment",
            "instanceId",
            "keywords",
            "status",
            "organizationId",
            "endpointUri",
            "mmsi",
            "imo",
            "serviceTypes",
            "dataProductType",
            "designId",
            "specificationId"
    };
    private final String[] searchFieldsWithSort = new String[] {
            "id",
            "name",
            "lastUpdatedAt",
            "comment",
            "instanceId",
            "keywords"
    };

    /**
     * Allow a common G1128 Utils definitions for the G1128 Instances.
     */
    protected G1128Utils<ServiceInstance> g1128SIUtils = new G1128Utils<>(ServiceInstance.class);

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

        // Don't accept empty geometry value, set empty geometries
        if (instance.getGeometry() == null) {
            log.debug("Setting empty geometry coverage");
            instance.setGeometryJson(GeometryJSONConverter.convertFromGeometry(new GeometryFactory().createEmpty(0)));
        }

        // Populate the save operation fields if required.
        // For new entries, and any that don't already have the organization ID
        if(instance.getId() == null || StringUtils.isBlank(instance.getOrganizationId())) {
            instance.setOrganizationId(this.userContext.getJwtToken()
                    .map(UserToken::getOrganisation)
                    .orElse(null));
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
                .map(Instance::getId)
                .ifPresentOrElse(
                        this.instanceRepo::deleteById,
                        () -> {throw new DataNotFoundException("No instance found for the provided ID", null);}
                );
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
                ServiceInstance serviceInstance = this.g1128SIUtils.unmarshallG1128(instanceXml.getContent());
                serviceInstance.setStatus(status);
                instanceXml.setContent(this.g1128SIUtils.marshalG1128(serviceInstance));
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
     * Get all the instances that match a domain specific ID (for example,
     * maritime ID), regardless of their version.
     *
     * @param domainId      the domain specific ID of the instance
     * @return the list of matching entities
     */
    public List<Instance> findAllByDomainId(String domainId){
        log.debug("Request to get Instances by domain id {}", domainId);
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
    public Instance findByDomainIdAndVersion(String domainId, String version) throws DataNotFoundException {
        log.debug("Request to get Instance by domain id {} and version {}", domainId, version);
        return this.instanceRepo.findByDomainIdAndVersionEagerRelationships(domainId, version)
                .orElseThrow(() -> new DataNotFoundException("No instance found for the provided domain ID and version", null));
    }

    /**
     * Get one instance by domain specific ID (for example, maritime ID), only
     * return the latest version.
     *
     * @param domainId      the domain specific id of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findLatestVersionByDomainId(String domainId) throws DataNotFoundException {
        log.debug("Request to get Instance by domain id {}", domainId);
        return this.instanceRepo.findByDomainIdEagerRelationships(domainId).stream()
                .max(Comparator.comparing(i -> new DefaultArtifactVersion(i.getVersion())))
                .orElseThrow(() -> new DataNotFoundException("No instance found for the provided domain ID", null));
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

        // Non G1128-compliant instance are allowed, where no XML description
        // is provided. In those cases... just let this through
        if(Objects.isNull(instance.getInstanceAsXml())) {
            return;
        }

        try {
            XmlUtil.validateXml(instance.getInstanceAsXml().getContent(), Collections.singletonList(G1128Schemas.INSTANCE.getPath()));
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

        // Update the XML with a formatted version
        Optional.of(instance)
                .map(Instance::getInstanceAsXml)
                .map(Xml::getContent)
                .map(xml -> { try { return g1128SIUtils.unmarshallG1128(xml); } catch (JAXBException e) { return null; } })
                .map(si -> { try { return g1128SIUtils.marshalG1128(si); } catch (JAXBException e) { return null; } })
                .ifPresent(instance.getInstanceAsXml()::setContent);
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
        SearchQuery searchQuery = this.getSearchInstanceQueryByText(
                dtPagingRequest.getSearch().getValue(),
                dtPagingRequest.getLucenceSort(Arrays.asList(searchFieldsWithSort)));

        // Map the results to a paged response
        return Optional.of(searchQuery)
                .map(query -> query.fetch(dtPagingRequest.getStart(), dtPagingRequest.getLength()))
                .map(searchResult -> new PageImpl<Instance>(searchResult.hits(), dtPagingRequest.toPageRequest(), searchResult.total().hitCount()))
                .orElseGet(() -> new PageImpl<>(Collections.emptyList(), dtPagingRequest.toPageRequest(), 0));
    }

    /**
     * Handles a datatables pagination request and returns the results list in
     * an appropriate format to be viewed by a datatables jQuery table.
     *
     * @param queryString
     * @param pageable
     * @return the paged response
     */
    @Transactional(readOnly = true)
    public Page<Instance> handleSearchQueryRequest(String queryString, Geometry geometry, Pageable pageable) {
        // Create the search query - always sort by name
        SearchQuery searchQuery = this.getSearchInstanceQueryByQueryString(queryString, geometry, new Sort(new SortedSetSortField("name_sort", false)));
        // Map the results to a paged response
        return Optional.of(searchQuery)
                .map(query -> query.fetch(pageable.getPageNumber() * pageable.getPageSize(), pageable.getPageSize()))
                .map(searchResult -> new PageImpl<Instance>(searchResult.hits(), pageable, searchResult.total().hitCount()))
                .orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
    }

    /**
     * Parse instance attributes from the xml payload for search/filtering
     *
     * @param instance      the instance to parse
     * @return an instance with its attributes set
     * @throws JAXBException if the XML is invalid or required attributes not present
     */
    protected void parseInstanceAttributesFromXML(Instance instance) throws JAXBException {
        // First safely retrieve the instance XML content
        final String xmlContent = Optional.of(instance)
                .map(Instance::getInstanceAsXml)
                .map(Xml::getContent)
                .orElse("");
        log.debug("Parsing XML: " + xmlContent);

        // Create a new service instance from the XML content
        ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(xmlContent);

        // And update the original instance object
        instance.setName(serviceInstance.getName());
        instance.setVersion(serviceInstance.getVersion());
        instance.setInstanceId(serviceInstance.getId());
        instance.setKeywords(serviceInstance.getKeywords());
        instance.setComment(serviceInstance.getDescription());
        instance.setEndpointUri(serviceInstance.getEndpoint());
        instance.setMmsi(serviceInstance.getMMSI());
        instance.setImo(serviceInstance.getIMO());
        instance.setServiceTypes(serviceInstance.getServiceTypes());
        instance.setUnlocode(Optional.of(serviceInstance)
                .map(ServiceInstance::getCoversAreas)
                .map(CoverageInfo::getCoversAreasAndUnLoCodes)
                .orElse(Collections.emptyList())
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList()));
        instance.setDesigns(Optional.of(serviceInstance)
                .map(ServiceInstance::getImplementsServiceDesigns)
                .map(ServiceInstance.ImplementsServiceDesigns::getImplementsServiceDesigns)
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(SpecReference::getId, SpecReference::getVersion)));
        instance.setSpecifications(Optional.of(serviceInstance)
                .map(ServiceInstance::getDesignsServiceSpecifications)
                .map(ServiceInstance.DesignsServiceSpecifications::getDesignsServiceSpecifications)
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(SpecReference::getId, SpecReference::getVersion)));
    }

    /**
     * Parse instance geometry from the xml payload for search/filtering
     *
     * @param instance      the instance to parse
     * @throws JAXBException if the XML is invalid or attributes not present
     * @throws ParseException if the XML parsing fails for any reason
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

        // Apply coverage areas, or for UnLoCode use its coordinates
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
     *  <li>Name</li>
     *  <li>Version</li>
     *  <li>Last Updated At</li>
     *  <li>Instance ID</li>
     *  <li>Status</li>
     *  <li>Organization ID</li>
     *  <li>Endpoint URI</li>
     *  <li>MMSI</li>
     *  <li>IMO</li>
     *  <li>Service Type</li>
     * </ul>
     *
     * @param searchText    the text to be searched
     * @param sort          the sorting operation to be applied
     * @return the constructed hibernate search query object
     */
    protected SearchQuery<Instance> getSearchInstanceQueryByText(String searchText, Sort sort) {
        SearchSession searchSession = Search.session( entityManager );
        SearchScope<Instance> scope = searchSession.scope( Instance.class );
        return searchSession.search( scope )
                .extension(LuceneExtension.get())
                .where( scope.predicate().wildcard()
                        .fields( this.searchFields )
                        .matching( Optional.ofNullable(searchText).map(st -> "*"+st).orElse("") + "*" )
                        .toPredicate() )
                .sort(f -> f.fromLuceneSort(sort))
                .toQuery();
    }

    /**
     * Constructs a hibernate search query using Lucene based on the provided
     * search query string and the geo-spatial geometry. This query string
     * should follow the Lucene query syntax and the search will include the
     * following fields:
     * <ul>
     *  <li>Name</li>
     *  <li>Version</li>
     *  <li>Last Updated At</li>
     *  <li>Instance ID</li>
     *  <li>Status</li>
     *  <li>Organization ID</li>
     *  <li>Endpoint URI</li>
     *  <li>MMSI</li>
     *  <li>IMO</li>
     *  <li>Service Type</li>
     * </ul>
     * The geo-spatial geometry is a LocationTech Geometry Collection that will
     * be evaluated if it intersects with any of the available instances.
     *
     * @param queryString   The lucene query string to use for the search
     * @param geometry      The geo-spatial geometry to use for the search
     * @param sort          The sorting operation to be applied
     * @return the constructed hibernate search query object
     */
    protected SearchQuery<Instance> getSearchInstanceQueryByQueryString(String queryString, Geometry geometry, Sort sort) {
        // First parse the input string to make sure it's right
        final Query luceneQuery = this.createLuceneQuery(queryString);

        // Also look out for a geometry query that needs to be handled differently
        final Query geoQuery = this.createGeoSpatialQuery(geometry);

        // Then build and return the hibernate-search query
        SearchSession searchSession = Search.session( entityManager );
        SearchScope<Instance> scope = searchSession.scope( Instance.class );
        return searchSession.search( scope )
                .where(f -> f.bool()
                        .must(q1 -> Optional.ofNullable(luceneQuery)
                                .map(q1.extension(LuceneExtension.get())::fromLuceneQuery)
                                .orElseGet(q1::matchAll)
                        )
                        .must(q2 -> Optional.ofNullable(geoQuery)
                                .map(q2.extension(LuceneExtension.get())::fromLuceneQuery)
                                .orElseGet(q2::matchAll)
                        )
                )
                .sort(f -> ((LuceneSearchSortFactory)f).fromLuceneSort(sort))
                .toQuery();
    }

    /**
     * Creates a Lucene query based on the query string provided. The query
     * string should follow the Lucene query syntax.
     *
     * @param queryString   The query string that follows the Lucene query syntax
     * @return The Lucene query constructed
     */
    protected Query createLuceneQuery(String queryString) {
        // First parse the input string to make sure it's right
        MultiFieldQueryParser parser = new MultiFieldQueryParser(this.searchFields, Search.mapping(entityManagerFactory)
                .backend()
                .unwrap(LuceneBackend.class)
                .analyzer( "standard" )
                .map(Analyzer.class::cast)
                .orElseGet(() -> new StandardAnalyzer()));
        parser.setDefaultOperator( QueryParser.Operator.AND );
        parser.setAllowLeadingWildcard(true);
        return Optional.ofNullable(queryString)
                .filter(StringUtils::isNotBlank)
                .map(q -> {
                    try {
                        // TODO: This is a big HACK!!!
                        // Careful, we add a catch-all clause here to account
                        // for lucene not returning anything in unary NOT cases
                        if(q.trim().startsWith("NOT")) {
                            q = String.format("name:* AND %s", q);
                        }
                        return parser.parse(q);
                    } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
                        log.error(ex.getMessage());
                        throw new InvalidRequestException(ex.getMessage(), ex);
                    }
                })
                .orElse(null);
    }

    /**
     * Creates a Lucene geo-spatial query based on the provided geometry. The
     * query isa recursive one based on the maxLevels defined (in this case 11,
     * which result in a sub-meter precision).
     *
     * @param geometry      The geometry to generate the spatial query for
     * @return The Lucene geo-spatial query constructed
     */
    protected Query createGeoSpatialQuery(Geometry geometry) {
        // Initialise the spatial strategy
        JtsSpatialContext ctx = JtsSpatialContext.GEO;
        int maxLevels = 12; //results in sub-meter precision for geohash
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid,"geometry");

        // Create the Lucene GeoSpatial Query
        return Optional.ofNullable(geometry)
                .map(g -> new SpatialArgs(SpatialOperation.Intersects, new JtsGeometry(g, ctx, false , true)))
                .map(strategy::makeQuery)
                .orElse(null);
    }

}
