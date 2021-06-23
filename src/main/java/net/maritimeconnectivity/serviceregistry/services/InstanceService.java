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
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.domain.*;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import net.maritimeconnectivity.serviceregistry.utils.XmlUtil;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.context.InvalidPersistentPropertyPath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/**
 * Service Implementation for managing Instance.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Transactional
public class InstanceService {

    @Autowired
    private InstanceRepo instanceRepo;

    @Autowired
    private XmlService xmlService;

    @Autowired
    @Lazy
    private UnLoCodeService unLoCodeService;

    /**
     * Definition of the whole world area in GeoJSON
     */
    private String wholeWorldGeoJson = "{\n" +
            "  \"type\": \"Polygon\",\n" +
            "  \"coordinates\": [\n" +
            "    [[-180, -90], [-180, 90], [180, 90], [180, -90], [-180, -90]]\n" +
            "  ]\n" +
            "}";

    /**
     * Save a instance.
     *
     * @param instance the entity to save
     * @return the persisted entity
     */
    @Transactional
    public Instance save(Instance instance) throws XMLValidationException, GeometryParseException {
        log.debug("Request to save Instance : {}", instance);

        // First of all validate the object
        this.validateInstanceForSave(instance);

        Instance result = this.instanceRepo.save(instance);
        return result;
    }

    /**
     * Get all the instances.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Instance> findAll(Pageable pageable) {
        log.debug("Request to get all Instances");
        Page<Instance> result = null;
        result = this.instanceRepo.findAll(pageable);
        return result;
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
        Instance instance = this.instanceRepo.findOneWithEagerRelationships(id);
        return instance;
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
     * @throws Exception
     */
    public void updateStatus(Long id, InstanceStatus status) throws Exception {
        log.debug("Request to update status of Instance : {}", id);
        try {
            Instance instance = this.instanceRepo.findOneWithEagerRelationships(id);

            Xml instanceXml = instance.getInstanceAsXml();
            if (instanceXml != null && instanceXml.getContent() != null) {
                String xml = instanceXml.getContent().toString();
                //Update the status value inside the xml definition
                String resultXml = XmlUtil.updateXmlNode(status.getStatus(), xml, "/*[local-name()='serviceInstance']/*[local-name()='status']");
                instanceXml.setContent(resultXml);
                // Save XML
                xmlService.save(instanceXml);
                instance.setInstanceAsXml(instanceXml);
            }

            instance.setStatus(status);
            instance.setInstanceAsXml(instanceXml);
            save(instance);
        } catch (InvalidPersistentPropertyPath e) {
            log.error("Problem during instance status update.", e);
            log.error("   Source: ", e.getSource());
            log.error("   ResolvedPath:  ", e.getResolvedPath());
            log.error("   ResolvedPath:  ", e.getUnresolvableSegment());
            throw e;
        } catch (Throwable e) {
            log.error("Problem during instance status update.", e);
            throw e;
        }
    }


    public Instance findAllByDomainId(String domainId, String version) {
        log.debug("Request to get Instance by domain id {} and version {} without restriction");
        List<Instance> findByDomainIdAndVersion = this.instanceRepo.findByDomainIdAndVersion(domainId, version);
        if (findByDomainIdAndVersion != null && !findByDomainIdAndVersion.isEmpty()) {
            return findByDomainIdAndVersion.get(0);
        }
        return null;
    }

    /**
     * Get one instance by domain specific id (for example, maritime id) and version.
     *
     * @param domainId            the domain specific id of the instance
     * @param version             the version identifier of the instance
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Instance findByDomainId(String domainId, String version) {
        log.debug("Request to get Instance by domain id {} and version {}", domainId, version);
        Instance instance = null;
        try {
            Iterable<Instance> instances;
            instances = this.instanceRepo.findByDomainIdAndVersionEagerRelationships(domainId, version);

            if (instances.iterator().hasNext()) {
                instance = instances.iterator().next();
            }
        } catch (Exception e) {
            log.debug("Could not find instance for domain id {} and version {}", domainId, version);
            e.printStackTrace();
        }
        return instance;
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
        Instance instance = null;
        DefaultArtifactVersion latestVersion = new DefaultArtifactVersion("0.0");
        try {
            Iterable<Instance> instances;
            instances = this.instanceRepo.findByDomainIdEagerRelationships(domainId);

            if (instances.iterator().hasNext()) {
                Instance i = instances.iterator().next();
                //Compare version numbers, save the instance if it's a newer version
                DefaultArtifactVersion iv = new DefaultArtifactVersion(i.getVersion());
                if (iv.compareTo(latestVersion) > 0 && i.getStatus().equals(InstanceStatus.LIVE)) {
                    instance = i;
                    latestVersion = iv;
                }
            }
        } catch (Exception e) {
            log.debug("Could not find a live instance for domain id {}", domainId);
            e.printStackTrace();
        }
        return instance;
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
    public void validateInstanceForSave(Instance instance) throws XMLValidationException, GeometryParseException {
        if(instance == null) {
            return;
        }
        try {
            this.parseInstanceGeometryFromXML(instance);
        } catch (Exception e) {
            throw new GeometryParseException("GeometryParse error.", e);
        }
    }

    /**
     * Parse instance attributes from the xml payload for search/filtering
     *
     * @param instance the instance to parse
     * @return an instance with its attributes set
     * @throws Exception if the XML is invalid or attributes not present
     */
    private Instance parseInstanceAttributesFromXML(Instance instance) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        builder = factory.newDocumentBuilder();
        log.info("Parsing XML: " + instance.getInstanceAsXml().getContent().toString());
        Document doc = builder.parse(new ByteArrayInputStream(instance.getInstanceAsXml().getContent().toString().getBytes(StandardCharsets.UTF_8)));
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        instance.setName(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='name']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setVersion(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='version']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setInstanceId(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='id']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setKeywords(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='keywords']").evaluate(doc, XPathConstants.STRING).toString());
//        instance.setStatus(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='status']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setComment(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='description']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setEndpointUri(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='URL']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setMmsi(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='MMSI']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setImo(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='IMO']").evaluate(doc, XPathConstants.STRING).toString());
        instance.setServiceType(xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='serviceType']").evaluate(doc, XPathConstants.STRING).toString());

        String unLoCode = xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='unLoCode']").evaluate(doc, XPathConstants.STRING).toString();
        if (unLoCode != null && unLoCode.length() > 0) {
            instance.setUnlocode(unLoCode);
        }
        return instance;
    }

    /**
     * Parse instance geometry from the xml payload for search/filtering
     *
     * @param instance the instance to parse
     * @return an instance with its attributes set
     * @throws Exception if the XML is invalid or attributes not present
     */
    private Instance parseInstanceGeometryFromXML(Instance instance) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        builder = factory.newDocumentBuilder();
        log.info("Parsing XML: " + instance.getInstanceAsXml().getContent().toString());
        Document doc = builder.parse(new ByteArrayInputStream(instance.getInstanceAsXml().getContent().toString().getBytes(StandardCharsets.UTF_8)));
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        String unLoCode = xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='coversAreas']/*[local-name()='unLoCode']").evaluate(doc, XPathConstants.STRING).toString();
        String geometryAsWKT = xPath.compile("/*[local-name()='serviceInstance']/*[local-name()='coversAreas']/*[local-name()='coversArea']/*[local-name()='geometryAsWKT']").evaluate(doc);

        //UN/LOCODE and Coverage Geometry are supported simultaneously. However, for geo-searches, Coverage takes precedence over UN/LOCODE.
        if (unLoCode != null && unLoCode.length() > 0) {
            instance.setUnlocode(unLoCode);
        }

        if (geometryAsWKT != null && geometryAsWKT.length() > 0) {
            JsonNode geometryAsGeoJson = WKTUtil.convertWKTtoGeoJson(geometryAsWKT);
            instance.setGeometryJson(geometryAsGeoJson);
        } else if (unLoCode != null && unLoCode.length() > 0) {
            unLoCodeService.applyUnLoCodeMapping(instance, unLoCode);
        }

        return instance;
    }


}
