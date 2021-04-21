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
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The type Specification.
 *
 * Holds a logical description of a service. A specification can be compatible
 * to one or more specification templates. It has at least a technical
 * representation of the service description in form of an XML and a filled out
 * service template as e.g. word document.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "specification")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Specification implements Serializable  {

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

    @Column(name = "keywords")
    private String keywords;

    @NotNull
    @Column(name = "specification_id", nullable = false)
    @JsonProperty("specificationId")
    private String specificationId;

    @Column(name = "status")
    private String status;

    @Column(name = "organization_id")
    @JsonProperty("organizationId")
    private String organizationId;

    @OneToOne
    @JoinColumn(unique = true)
    private Xml specAsXml;

    @OneToOne
    @JoinColumn(unique = true)
    private Doc specAsDoc;

    @ManyToOne
    private SpecificationTemplate implementedSpecificationVersion;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "specification_docs",
            joinColumns = @JoinColumn(name="specifications_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="docs_id", referencedColumnName="ID"))
    private Set<Doc> docs = new HashSet<>();

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
     * Gets spec as xml.
     *
     * @return the spec as xml
     */
    public Xml getSpecAsXml() {
        return specAsXml;
    }

    /**
     * Sets spec as xml.
     *
     * @param specAsXml the spec as xml
     */
    public void setSpecAsXml(Xml specAsXml) {
        this.specAsXml = specAsXml;
    }

    /**
     * Gets spec as doc.
     *
     * @return the spec as doc
     */
    public Doc getSpecAsDoc() {
        return specAsDoc;
    }

    /**
     * Sets spec as doc.
     *
     * @param specAsDoc the spec as doc
     */
    public void setSpecAsDoc(Doc specAsDoc) {
        this.specAsDoc = specAsDoc;
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
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specification)) return false;
        Specification that = (Specification) o;
        return id.equals(that.id);
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
        return "Specification{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", publishedAt='" + publishedAt + '\'' +
                ", lastUpdatedAt='" + lastUpdatedAt + '\'' +
                ", comment='" + comment + '\'' +
                ", keywords='" + keywords + '\'' +
                ", specificationId='" + specificationId + '\'' +
                ", status='" + status + '\'' +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}
