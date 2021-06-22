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
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import net.maritimeconnectivity.serviceregistry.models.domain.Xml;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.*;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.efficiensea2.maritime_cloud.service_registry.v1.serviceinstanceschema.CoverageArea;
import org.efficiensea2.maritime_cloud.service_registry.v1.serviceinstanceschema.ServiceInstance;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

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
     * The Instance Repository.
     */
    @Autowired
    private InstanceRepo instanceRepo;

    /**
     * The XML Service.
     */
    @Autowired
    private XmlService xmlService;

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
     * Definition of the G1128 Schema Sources.
     */
    List<String> g1128Sources = Arrays.asList(new String[] {
            "xsd/ServiceBaseTypesSchema.xsd",
            "xsd/ServiceDesignSchema.xsd",
            "xsd/ServiceSpecificationSchema.xsd",
            "xsd/ServiceInstanceSchema.xsd"
    });

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
     * Get one instance by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findOne(Long id) {
        log.debug("Request to get Instance : {}", id);
        return this.instanceRepo.findOneWithEagerRelationships(id);
    }

    /**
     * Save a instance.
     *
     * @param instance the entity to save
     * @return the persisted entity
     */
    @Transactional
    public Instance save(Instance instance) throws DataNotFoundException, XMLValidationException, GeometryParseException, JsonProcessingException, ParseException {
        log.debug("Request to save Instance : {}", instance);

        // First of all validate the object
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
        if(instance.getPublishedAt() == null) {
            instance.setPublishedAt(EntityUtils.getCurrentUTCTimeISO8601());
        }
        // And don't forget the last update
        instance.setLastUpdatedAt(instance.getPublishedAt());

        // The save and return
        return this.instanceRepo.save(instance);
    }

    /**
     * Delete the  instance by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Instance : {}", id);
        this.instanceRepo.deleteById(id);
    }

    /**
     * Update the status of an instance by id.
     *
     * @param id     the id of the entity
     * @param status the status of the entity
     * @throws Exception any exceptions thrown while updating the status
     */
    public void updateStatus(Long id, ServiceStatus status) throws DataNotFoundException, JAXBException, XMLValidationException, ParseException, JsonProcessingException, GeometryParseException {
        log.debug("Request to update status of Instance : {}", id);

        // Try to find if the instance does indeed exist
        Instance instance = this.instanceRepo.findById(id)
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
                xmlService.save(instanceXml);
            }
            instance.setStatus(status);
            instance.setInstanceAsXml(instanceXml);
            save(instance);
        } catch (JAXBException | XMLValidationException | ParseException | JsonProcessingException | GeometryParseException e) {
            log.error("Problem during instance status update.", e);
            throw e;
        }
    }

    /**
     * Get all the instances that match a domain specific ID (for example,
     * maritime id), regardless of their version.
     *
     * @param domainId          the domain specific id of the instance
     * @return the list of matching entities
     */
    public List<Instance> findAllByDomainId(String domainId) {
        log.debug("Request to get Instance by domain id {} and version {} without restriction");
        return this.instanceRepo.findByDomainId(domainId);
    }

    /**
     * Get one instance by domain specific id (for example, maritime id) and version.
     *
     * @param domainId            the domain specific id of the instance
     * @param version             the version identifier of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findByDomainIdAndVersion(String domainId, String version) {
        log.debug("Request to get Instance by domain id {} and version {}", domainId, version);
        try {
            return this.instanceRepo.findByDomainIdAndVersionEagerRelationships(domainId, version);
        } catch (Exception e) {
            log.debug("Could not find instance for domain id {} and version {}", domainId, version);
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Get one instance by domain specific id (for example, maritime id), only return the latest version.
     *
     * @param domainId            the domain specific id of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findLatestVersionByDomainId(String domainId) {
        log.debug("Request to get Instance by domain id {}", domainId);
        try {
            return this.instanceRepo.findByDomainIdEagerRelationships(domainId).stream()
                .max(Comparator.comparing(i -> new DefaultArtifactVersion(i.getVersion())))
                .orElseThrow(() -> new DataNotFoundException("No instance found!", null));
        } catch (Exception e) {
            log.debug("Could not find a live instance for domain id {}", domainId);
            log.error(e.getMessage());
        }
        return null;
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
     * @param instance the instance to be saved
     * @throws XMLValidationException If fails first phase (Validating and parsing XML)
     * @throws GeometryParseException If fails second phase (Parsing geo data)
     */
    public void validateInstanceForSave(Instance instance) throws XMLValidationException, GeometryParseException, DataNotFoundException {
        if(instance == null) {
            return;
        }

        // Try to find the instance if an ID is provided
        if(instance.getId() != null) {
            this.instanceRepo.findById(instance.getId())
                .orElseThrow(() -> new DataNotFoundException("No instance found for the provided ID", null));
        }

        try {
            XmlUtil.validateXml(instance.getInstanceAsXml().getContent(), this.g1128Sources);
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
     * Parse instance attributes from the xml payload for search/filtering
     *
     * @param instance the instance to parse
     * @return an instance with its attributes set
     * @throws JAXBException if the XML is invalid or required attributes not present
     */
    private void parseInstanceAttributesFromXML(Instance instance) throws JAXBException {
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
        instance.setUnlocode(serviceInstance.getCoversAreas().getUnLoCode());
    }

    /**
     * Parse instance geometry from the xml payload for search/filtering
     *
     * @param instance the instance to parse
     * @return an instance with its attributes set
     * @throws Exception if the XML is invalid or attributes not present
     */
    private void parseInstanceGeometryFromXML(Instance instance) throws JAXBException, ParseException {
        log.debug("Parsing XML: " + instance.getInstanceAsXml().getContent());
        ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(instance.getInstanceAsXml().getContent());

        String unLoCode = serviceInstance.getCoversAreas().getUnLoCode();
        List<CoverageArea> coverageAreas = serviceInstance.getCoversAreas().getCoversAreas();

        //UN/LOCODE and Coverage Geometry are supported simultaneously.
        // However, for geo-searches, Coverage takes precedence over UN/LOCODE.
        if (unLoCode != null && unLoCode.length() > 0) {
            instance.setUnlocode(serviceInstance.getCoversAreas().getUnLoCode());
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
        } else if (unLoCode != null && unLoCode.length() > 0) {
            unLoCodeService.applyUnLoCodeMapping(instance, unLoCode);
        }
    }

}
