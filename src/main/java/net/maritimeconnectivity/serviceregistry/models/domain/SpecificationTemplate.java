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

import net.maritimeconnectivity.serviceregistry.models.domain.enums.SpecificationTemplateType;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The type Specification template.
 *
 * A SpecificationTemplate contains information on how to define a aspects of
 * a service.It has a type do differentiate between e.g. logical definitions and
 * concrete service instances.Templates will evolve, that's why they have a
 * version.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "specification_template")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SpecificationTemplate implements Serializable {

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SpecificationTemplateType type;

    @Column(name = "comment")
    private String comment;

    @ManyToOne
    private Doc guidelineDoc;

    @ManyToOne
    private Doc templateDoc;

    @ManyToMany
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "specification_template_docs",
            joinColumns = @JoinColumn(name="specification_templates_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="docs_id", referencedColumnName="ID"))
    private Set<Doc> docs = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "specification_template_xsds",
            joinColumns = @JoinColumn(name="specification_templates_id", referencedColumnName="ID"),
            inverseJoinColumns = @JoinColumn(name="xsds_id", referencedColumnName="ID"))
    private Set<Xsd> xsds = new HashSet<>();

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
     * Gets type.
     *
     * @return the type
     */
    public SpecificationTemplateType getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(SpecificationTemplateType type) {
        this.type = type;
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
     * Gets guideline doc.
     *
     * @return the guideline doc
     */
    public Doc getGuidelineDoc() {
        return guidelineDoc;
    }

    /**
     * Sets guideline doc.
     *
     * @param guidelineDoc the guideline doc
     */
    public void setGuidelineDoc(Doc guidelineDoc) {
        this.guidelineDoc = guidelineDoc;
    }

    /**
     * Gets template doc.
     *
     * @return the template doc
     */
    public Doc getTemplateDoc() {
        return templateDoc;
    }

    /**
     * Sets template doc.
     *
     * @param templateDoc the template doc
     */
    public void setTemplateDoc(Doc templateDoc) {
        this.templateDoc = templateDoc;
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
     * Gets xsds.
     *
     * @return the xsds
     */
    public Set<Xsd> getXsds() {
        return xsds;
    }

    /**
     * Sets xsds.
     *
     * @param xsds the xsds
     */
    public void setXsds(Set<Xsd> xsds) {
        this.xsds = xsds;
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
        if (!(o instanceof SpecificationTemplate)) return false;
        SpecificationTemplate that = (SpecificationTemplate) o;
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
        return "SpecificationTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", type=" + type +
                ", comment='" + comment + '\'' +
                '}';
    }
}
