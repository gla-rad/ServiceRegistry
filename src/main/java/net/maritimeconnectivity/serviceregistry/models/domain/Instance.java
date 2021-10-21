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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.maritimeconnectivity.serviceregistry.models.JsonSerializable;
import net.maritimeconnectivity.serviceregistry.utils.*;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.*;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Instance.
 * <p>
 * Holds a description of an service instance.An instance can be compatible to
 * one or more specification templates. It has at least a technical
 * representation of the description in form of an XML and a filled out
 * template as e.g. word document.
 * </p>
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "instance", uniqueConstraints = {@UniqueConstraint(name="mrn_version_constraint", columnNames = {"instance_id", "version"})} )
@Cacheable
@Indexed
@NormalizerDef(name = "lowercase", filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class))
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Instance implements Serializable, JsonSerializable {

    private static final long serialVersionUID = 1L;

    @Id
    @NumericField()
    @Field(name = "id_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "id_sort")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Analyzer(impl= KeywordAnalyzer.class)
    @Field()
    @Field(name = "name_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "name_sort")
    @Column(name = "name")
    private String name;

    @NotNull
    @Field()
    @Field(name = "version_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "version_sort")
    @Column(name = "version")
    private String version;

    @Field()
    @Field(name = "publishedAt_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "publishedAt_sort")
    @Column(name = "published_at")
    private String publishedAt;

    @Field()
    @Field(name = "lastUpdatedAt_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "lastUpdatedAt_sort")
    @Column(name = "last_updated_at")
    private String lastUpdatedAt;

    @NotNull
    @Field()
    @Field(name = "comment_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "comment_sort")
    @Column(name = "comment")
    private String comment;

    @JsonSerialize(using = GeometryJSONSerializer.class)
    @JsonDeserialize(using = GeometryJSONDeserializer.class)
    @Column(name = "geometry")
    private Geometry geometry;

    @Column(name = "geometry_content_type")
    private String geometryContentType;

    @NotNull
    @Field()
    @Field(name = "instanceId_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "instanceId_sort")
    @Column(name = "instance_id", updatable = false)
    @JsonProperty("instanceId")
    private String instanceId; //MRN

    @Field()
    @IndexedEmbedded
    @Field(bridge=@FieldBridge(impl= StringListBridge.class), name = "keywords_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "keywords_sort")
    @ElementCollection
    private List<String> keywords;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Field(bridge=@FieldBridge(impl= ServiceStatusBridge.class))
    @Field(bridge=@FieldBridge(impl= ServiceStatusBridge.class), name = "status_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "status_sort")
    @Column(name = "status", columnDefinition = "varchar(30) default 'provisional'")
    private ServiceStatus status;

    @Field()
    @Field(name = "organizationId_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "organizationId_sort")
    @Column(name = "organization_id")
    @JsonProperty("organizationId")
    private String organizationId; // Use the JWT auth token for that

    @Field()
    @IndexedEmbedded
    @Field(bridge=@FieldBridge(impl= StringListBridge.class), name = "unlocode_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "unlocode_sort")
    @ElementCollection
    private List<String> unlocode;

    @Field()
    @Field(name = "endpointUri_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "endpointUri_sort")
    @Column(name = "endpoint_uri")
    @JsonProperty("endpointUri")
    private String endpointUri;

    @Field()
    @Field(name = "endpointType_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "endpointType_sort")
    @Column(name = "endpoint_type")
    @JsonProperty("endpointType")
    private String endpointType;

    @Field()
    @Field(name = "mmsi_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "mmsi_sort")
    @Column(name = "mmsi")
    private String mmsi;

    @Field()
    @Field(name = "imo_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "imo_sort")
    @Column(name = "imo")
    private String imo;

    @Field()
    @IndexedEmbedded
    @Field(bridge=@FieldBridge(impl= StringListBridge.class), name="serviceType_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "serviceType_sort")
    @ElementCollection
    @JsonProperty("serviceType")
    private List<String> serviceType;

    @OneToOne(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(unique = true)
    private Xml instanceAsXml;

    @OneToOne(cascade = {CascadeType.ALL}, orphanRemoval = true, fetch=FetchType.LAZY)
    @JoinColumn(unique = true)
    private Doc instanceAsDoc;

    @ManyToMany(fetch=FetchType.LAZY)
    @IndexedEmbedded(depth = 1)
    @ContainedIn
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "instance_docs",
            joinColumns = @JoinColumn(name="instances_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="docs_id", referencedColumnName="ID"))
    private Set<Doc> docs = new HashSet<>();

    /**
     * The Ledger Request.
     */
    @OneToOne(mappedBy = "serviceInstance")
    private LedgerRequest ledgerRequest;

    /**
     * The Designs.
     */
    @ElementCollection
    Map<String, String> designs = new HashMap<>();

    /**
     * The Specifications.
     */
    @ElementCollection
    Map<String, String> specifications = new HashMap<>();

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
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * Sets keywords.
     *
     * @param keywords the keywords
     */
    public void setKeywords(List<String> keywords) {
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
    public List<String> getUnlocode() {
        return unlocode;
    }

    /**
     * Sets unlocode.
     *
     * @param unlocode the unlocode
     */
    public void setUnlocode(List<String> unlocode) {
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
    public List<String>  getServiceType() {
        return serviceType;
    }

    /**
     * Sets service type.
     *
     * @param serviceType the service type
     */
    public void setServiceType(List<String>  serviceType) {
        this.serviceType = serviceType;
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
     * Gets ledger request.
     *
     * @return the ledger request
     */
    public LedgerRequest getLedgerRequest() {
        return ledgerRequest;
    }

    /**
     * Sets ledger request.
     *
     * @param ledgerRequest the ledger request
     */
    public void setLedgerRequest(LedgerRequest ledgerRequest) {
        this.ledgerRequest = ledgerRequest;
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
     * Gets the geometry as a JSON node object.
     *
     * @return the geometry in JSON format
     */
    public JsonNode getGeometryJson() {
        return GeometryJSONConverter.convertFromGeometry(this.geometry);
    }

    /**
     * Sets the geometry from a JSON node object.
     *
     * @param geometry the geometry in a JSON format
     * @throws ParseException the parse exception
     */
    public void setGeometryJson(JsonNode geometry) throws ParseException {
        this.setGeometry(GeometryJSONConverter.convertToGeometry(geometry));
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
        return Objects.equals(id, instance.id);
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
                ", keywords='" + Optional.ofNullable(keywords).orElse(Collections.emptyList()).stream().collect(Collectors.joining(",")) + '\'' +
                ", status='" + status + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", unlocode='" + Optional.ofNullable(unlocode).orElse(Collections.emptyList()).stream().collect(Collectors.joining(",")) + '\'' +
                ", endpointUri='" + endpointUri + '\'' +
                ", endpointType='" + endpointType + '\'' +
                ", mmsi='" + mmsi + '\'' +
                ", imo='" + imo + '\'' +
                ", serviceType='" + Optional.ofNullable(serviceType).orElse(Collections.emptyList()).stream().collect(Collectors.joining(",")) + '\'' +
                '}';
    }

}
