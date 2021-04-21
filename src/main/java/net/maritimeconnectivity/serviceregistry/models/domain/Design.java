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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * The type Design.
 *
 * Holds a description of a technical design.A design can be compatible to one
 * or more specification templates. It has at least a technical representation
 * of the description in form of an XML and a filled out template as e.g. word
 * document.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "design")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Design implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "published_at", nullable = true)
    private String publishedAt;

    @Column(name = "last_updated_at", nullable = true)
    private String lastUpdatedAt;

    @NotNull
    @Column(name = "comment", nullable = false)
    private String comment;

    @NotNull
    @Column(name = "design_id", nullable = false)
    @JsonProperty("designId")
    private String designId;

    @Column(name = "status")
    private String status;

    @Column(name = "organization_id")
    @JsonProperty("organizationId")
    private String organizationId;

    @OneToOne
    @JoinColumn(unique = true)
    private Xml designAsXml;

    @OneToOne
    @JoinColumn(unique = true)
    private Doc designAsDoc;

    @ManyToOne
    private SpecificationTemplate implementedSpecificationVersion;

    @ManyToMany
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "design_specifications",
            joinColumns = @JoinColumn(name="designs_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="specifications_id", referencedColumnName="ID"))
    private Set<Specification> specifications = new HashSet<>();

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "design_docs",
            joinColumns = @JoinColumn(name="designs_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="docs_id", referencedColumnName="ID"))
    private Set<Doc> docs = new HashSet<>();

    @ManyToMany(mappedBy = "designs")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Instance> instances = new HashSet<>();

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
     * Gets design as xml.
     *
     * @return the design as xml
     */
    public Xml getDesignAsXml() {
        return designAsXml;
    }

    /**
     * Sets design as xml.
     *
     * @param designAsXml the design as xml
     */
    public void setDesignAsXml(Xml designAsXml) {
        this.designAsXml = designAsXml;
    }

    /**
     * Gets design as doc.
     *
     * @return the design as doc
     */
    public Doc getDesignAsDoc() {
        return designAsDoc;
    }

    /**
     * Sets design as doc.
     *
     * @param designAsDoc the design as doc
     */
    public void setDesignAsDoc(Doc designAsDoc) {
        this.designAsDoc = designAsDoc;
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
     * Gets specifications.
     *
     * @return the specifications
     */
    public Set<Specification> getSpecifications() {
        return specifications;
    }

    /**
     * Sets specifications.
     *
     * @param specifications the specifications
     */
    public void setSpecifications(Set<Specification> specifications) {
        this.specifications = specifications;
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
     * Gets instances.
     *
     * @return the instances
     */
    public Set<Instance> getInstances() {
        return instances;
    }

    /**
     * Sets instances.
     *
     * @param instances the instances
     */
    public void setInstances(Set<Instance> instances) {
        this.instances = instances;
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
        if (!(o instanceof Design)) return false;
        Design design = (Design) o;
        return id.equals(design.id);
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
        return "Design{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", publishedAt='" + publishedAt + '\'' +
                ", lastUpdatedAt='" + lastUpdatedAt + '\'' +
                ", comment='" + comment + '\'' +
                ", designId='" + designId + '\'' +
                ", status='" + status + '\'' +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}