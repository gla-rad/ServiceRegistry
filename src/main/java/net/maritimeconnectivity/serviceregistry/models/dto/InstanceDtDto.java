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

package net.maritimeconnectivity.serviceregistry.models.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.maritimeconnectivity.serviceregistry.models.JsonSerializable;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONDeserializer;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONSerializer;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Instance Datatables DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class InstanceDtDto implements Serializable, JsonSerializable {

    private static final long serialVersionUID = -1173956383783083179L;

    // Class Variables
    private Long id;
    @NotNull
    private String name;
    @NotNull
    private String version;
    private String publishedAt;
    private String lastUpdatedAt;
    @NotNull
    private String comment;
    @JsonSerialize(using = GeometryJSONSerializer.class)
    @JsonDeserialize(using = GeometryJSONDeserializer.class)
    private Geometry geometry;
    private String geometryContentType;
    @NotNull
    private String instanceId; //MRN
    private String keywords;
    @NotNull
    private ServiceStatus status;
    private String organizationId; // Use the JWT auth token for that
    private String unlocode;
    private String endpointUri;
    private String endpointType;
    private String mmsi;
    private String imo;
    private String serviceType;
    private XmlDto instanceAsXml;
    private Long instanceAsDocId;
    private String instanceAsDocName;
    private Long ledgerRequestId;
    private LedgerRequestStatus ledgerRequestStatus;
    private Set<Long> docIds = new HashSet<>();
    private Map<String, String> designs = new HashMap<>();
    private Map<String, String> specifications = new HashMap<>();

    /**
     * Instantiates a new Instance dto.
     */
    public InstanceDtDto() {

    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets published at.
     *
     * @return the published at
     */
    public String getPublishedAt() {
        return publishedAt;
    }

    /**
     * Sets published at.
     *
     * @param publishedAt the published at
     */
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * Gets last updated at.
     *
     * @return the last updated at
     */
    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Sets last updated at.
     *
     * @param lastUpdatedAt the last updated at
     */
    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /**
     * Gets comment.
     *
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets comment.
     *
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets geometry.
     *
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Sets geometry.
     *
     * @param geometry the geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Gets geometry content type.
     *
     * @return the geometry content type
     */
    public String getGeometryContentType() {
        return geometryContentType;
    }

    /**
     * Sets geometry content type.
     *
     * @param geometryContentType the geometry content type
     */
    public void setGeometryContentType(String geometryContentType) {
        this.geometryContentType = geometryContentType;
    }

    /**
     * Gets instance id.
     *
     * @return the instance id
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets instance id.
     *
     * @param instanceId the instance id
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Gets keywords.
     *
     * @return the keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Sets keywords.
     *
     * @param keywords the keywords
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public ServiceStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    /**
     * Gets organization id.
     *
     * @return the organization id
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets organization id.
     *
     * @param organizationId the organization id
     */
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Gets unlocode.
     *
     * @return the unlocode
     */
    public String getUnlocode() {
        return unlocode;
    }

    /**
     * Sets unlocode.
     *
     * @param unlocode the unlocode
     */
    public void setUnlocode(String unlocode) {
        this.unlocode = unlocode;
    }

    /**
     * Gets endpoint uri.
     *
     * @return the endpoint uri
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * Sets endpoint uri.
     *
     * @param endpointUri the endpoint uri
     */
    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    /**
     * Gets endpoint type.
     *
     * @return the endpoint type
     */
    public String getEndpointType() {
        return endpointType;
    }

    /**
     * Sets endpoint type.
     *
     * @param endpointType the endpoint type
     */
    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    /**
     * Gets mmsi.
     *
     * @return the mmsi
     */
    public String getMmsi() {
        return mmsi;
    }

    /**
     * Sets mmsi.
     *
     * @param mmsi the mmsi
     */
    public void setMmsi(String mmsi) {
        this.mmsi = mmsi;
    }

    /**
     * Gets imo.
     *
     * @return the imo
     */
    public String getImo() {
        return imo;
    }

    /**
     * Sets imo.
     *
     * @param imo the imo
     */
    public void setImo(String imo) {
        this.imo = imo;
    }

    /**
     * Gets service type.
     *
     * @return the service type
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets service type.
     *
     * @param serviceType the service type
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Gets instance as xml.
     *
     * @return the instance as xml
     */
    public XmlDto getInstanceAsXml() {
        return instanceAsXml;
    }

    /**
     * Sets instance as xml.
     *
     * @param instanceAsXml the instance as xml
     */
    public void setInstanceAsXml(XmlDto instanceAsXml) {
        this.instanceAsXml = instanceAsXml;
    }

    /**
     * Gets instance as doc id.
     *
     * @return the instance as doc id
     */
    public Long getInstanceAsDocId() {
        return instanceAsDocId;
    }

    /**
     * Sets instance as doc id.
     *
     * @param instanceAsDocId the instance as doc id
     */
    public void setInstanceAsDocId(Long instanceAsDocId) {
        this.instanceAsDocId = instanceAsDocId;
    }

    /**
     * Gets instance as doc name.
     *
     * @return the instance as doc name
     */
    public String getInstanceAsDocName() {
        return instanceAsDocName;
    }

    /**
     * Sets instance as doc name.
     *
     * @param instanceAsDocName the instance as doc name
     */
    public void setInstanceAsDocName(String instanceAsDocName) {
        this.instanceAsDocName = instanceAsDocName;
    }

    /**
     * Gets ledger request id.
     *
     * @return the ledger request id
     */
    public Long getLedgerRequestId() {
        return ledgerRequestId;
    }

    /**
     * Sets ledger request id.
     *
     * @param ledgerRequestId the ledger request id
     */
    public void setLedgerRequestId(Long ledgerRequestId) {
        this.ledgerRequestId = ledgerRequestId;
    }

    /**
     * Gets doc ids.
     *
     * @return the doc ids
     */
    public Set<Long> getDocIds() {
        return docIds;
    }

    /**
     * Sets doc ids.
     *
     * @param docIds the doc idss
     */
    public void setDocIds(Set<Long> docIds) {
        this.docIds = docIds;
    }

    /**
     * Gets designs.
     *
     * @return the designs
     */
    public Map<String, String> getDesigns() {
        return designs;
    }

    /**
     * Sets designs.
     *
     * @param designs the designs
     */
    public void setDesigns(Map<String, String> designs) {
        this.designs = designs;
    }

    /**
     * Gets specifications.
     *
     * @return the specifications
     */
    public Map<String, String> getSpecifications() {
        return specifications;
    }

    /**
     * Sets specifications.
     *
     * @param specifications the specifications
     */
    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }

    /**
     * Gets ledger request status.
     *
     * @return the ledger request status
     */
    public LedgerRequestStatus getLedgerRequestStatus() {
        return ledgerRequestStatus;
    }

    /**
     * Sets ledger request status.
     *
     * @param ledgerRequestStatus the ledger request status
     */
    public void setLedgerRequestStatus(LedgerRequestStatus ledgerRequestStatus) {
        this.ledgerRequestStatus = ledgerRequestStatus;
    }

}
