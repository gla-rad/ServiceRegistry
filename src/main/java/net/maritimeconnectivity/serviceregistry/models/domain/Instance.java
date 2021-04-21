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

package net.maritimeconnectivity.serviceregistry.models.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.locationtech.jts.geom.Geometry;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The type Instance.
 *
 * Holds a description of an service instance.An instance can be compatible to
 * one or more specification templates. It has at least a technical
 * representation of the description in form of an XML and a filled out
 * template as e.g. word document.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "instance")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Instance implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SERVICESTATUS_LIVE = "live";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = true)
    private String name;

    @NotNull
    @Column(name = "version", nullable = true)
    private String version;

    @Column(name = "published_at", nullable = true)
    private String publishedAt;

    @Column(name = "last_updated_at", nullable = true)
    private String lastUpdatedAt;

    @NotNull
    @Column(name = "comment", nullable = true)
    private String comment;

    @Column(name = "geometry")
    private Geometry geometry;

    @Column(name = "geometry_content_type")
    private String geometryContentType;

    @NotNull
    @Column(name = "instance_id", nullable = true)
    @JsonProperty("instanceId")
    private String instanceId;

    @Column(name = "keywords")
    private String keywords;

    @Column(name = "status")
    private String status;

    @Column(name = "organization_id")
    @JsonProperty("organizationId")
    private String organizationId;

    @Column(name = "unlocode")
    private String unlocode;

    @Column(name = "endpoint_uri")
    @JsonProperty("endpointUri")
    private String endpointUri;

    @Column(name = "endpoint_type")
    @JsonProperty("endpointType")
    private String endpointType;

    @Column(name = "mmsi")
    private String mmsi;

    @Column(name = "imo")
    private String imo;

    @Column(name = "service_type")
    @JsonProperty("serviceType")
    private String serviceType;

    @Column(name = "design_id")
    @JsonProperty("designId")
    private String designId;

    @Column(name = "specification_id")
    @JsonProperty("specificationId")
    private String specificationId;

    @OneToOne
    @JoinColumn(unique = true)
    private Xml instanceAsXml;

    @OneToOne
    @JoinColumn(unique = true)
    private Doc instanceAsDoc;

    @ManyToOne
    private SpecificationTemplate implementedSpecificationVersion;

    @ManyToMany(fetch=FetchType.LAZY)
    @JsonIgnore
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "instance_designs",
            joinColumns = @JoinColumn(name="instances_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="designs_id", referencedColumnName="ID"))
    private Set<Design> designs = new HashSet<>();

    @ManyToMany(fetch=FetchType.LAZY)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "instance_docs",
            joinColumns = @JoinColumn(name="instances_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="docs_id", referencedColumnName="ID"))
    private Set<Doc> docs = new HashSet<>();

    @Column(name = "compliant")
    private boolean compliant;

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
    public String getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
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
     * Gets design id.
     *
     * @return the design id
     */
    public String getDesignId() {
        return designId;
    }

    /**
     * Sets design id.
     *
     * @param designId the design id
     */
    public void setDesignId(String designId) {
        this.designId = designId;
    }

    /**
     * Gets specification id.
     *
     * @return the specification id
     */
    public String getSpecificationId() {
        return specificationId;
    }

    /**
     * Sets specification id.
     *
     * @param specificationId the specification id
     */
    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    /**
     * Gets instance as xml.
     *
     * @return the instance as xml
     */
    public Xml getInstanceAsXml() {
        return instanceAsXml;
    }

    /**
     * Sets instance as xml.
     *
     * @param instanceAsXml the instance as xml
     */
    public void setInstanceAsXml(Xml instanceAsXml) {
        this.instanceAsXml = instanceAsXml;
    }

    /**
     * Gets instance as doc.
     *
     * @return the instance as doc
     */
    public Doc getInstanceAsDoc() {
        return instanceAsDoc;
    }

    /**
     * Sets instance as doc.
     *
     * @param instanceAsDoc the instance as doc
     */
    public void setInstanceAsDoc(Doc instanceAsDoc) {
        this.instanceAsDoc = instanceAsDoc;
    }

    /**
     * Gets implemented specification version.
     *
     * @return the implemented specification version
     */
    public SpecificationTemplate getImplementedSpecificationVersion() {
        return implementedSpecificationVersion;
    }

    /**
     * Sets implemented specification version.
     *
     * @param implementedSpecificationVersion the implemented specification version
     */
    public void setImplementedSpecificationVersion(SpecificationTemplate implementedSpecificationVersion) {
        this.implementedSpecificationVersion = implementedSpecificationVersion;
    }

    /**
     * Gets designs.
     *
     * @return the designs
     */
    public Set<Design> getDesigns() {
        return designs;
    }

    /**
     * Sets designs.
     *
     * @param designs the designs
     */
    public void setDesigns(Set<Design> designs) {
        this.designs = designs;
    }

    /**
     * Gets docs.
     *
     * @return the docs
     */
    public Set<Doc> getDocs() {
        return docs;
    }

    /**
     * Sets docs.
     *
     * @param docs the docs
     */
    public void setDocs(Set<Doc> docs) {
        this.docs = docs;
    }

    /**
     * Is compliant boolean.
     *
     * @return the boolean
     */
    public boolean isCompliant() {
        return compliant;
    }

    /**
     * Sets compliant.
     *
     * @param compliant the compliant
     */
    public void setCompliant(boolean compliant) {
        this.compliant = compliant;
    }

    /**
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instance)) return false;
        Instance instance = (Instance) o;
        return id.equals(instance.id);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Overrides the string representation of the object.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "Instance{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", publishedAt='" + publishedAt + '\'' +
                ", lastUpdatedAt='" + lastUpdatedAt + '\'' +
                ", comment='" + comment + '\'' +
                ", geometry=" + geometry +
                ", geometryContentType='" + geometryContentType + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", keywords='" + keywords + '\'' +
                ", status='" + status + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", unlocode='" + unlocode + '\'' +
                ", endpointUri='" + endpointUri + '\'' +
                ", endpointType='" + endpointType + '\'' +
                ", mmsi='" + mmsi + '\'' +
                ", imo='" + imo + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", designId='" + designId + '\'' +
                ", specificationId='" + specificationId + '\'' +
                '}';
    }
}