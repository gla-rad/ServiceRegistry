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
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The type Doc.
 * <p>
 * A doc represents a human readable document that can be attached to various
 * objects.This could be an office document containing guidelines linked,to a
 * service specification, or a Getting Started PDF attached to a service
 * instance.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "document")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Doc implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "comment")
    private String comment;

    @NotNull
    @Column(name = "mimetype", nullable = false)
    private String mimetype;

    @NotNull
    @Lob
    @Column(name = "filecontent", nullable = false)
    private byte[] filecontent;

    @Column(name = "filecontent_content_type", nullable = false)
    private String filecontentContentType;

    @ManyToMany(mappedBy = "docs")
    @JsonIgnore
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Instance> instances = new HashSet<>();

    /**
     * Instantiates a new Doc.
     */
    public Doc() {

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
     * Gets mimetype.
     *
     * @return the mimetype
     */
    public String getMimetype() {
        return mimetype;
    }

    /**
     * Sets mimetype.
     *
     * @param mimetype the mimetype
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Get filecontent byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getFilecontent() {
        return filecontent;
    }

    /**
     * Sets filecontent.
     *
     * @param filecontent the filecontent
     */
    public void setFilecontent(byte[] filecontent) {
        this.filecontent = filecontent;
    }

    /**
     * Gets filecontent content type.
     *
     * @return the filecontent content type
     */
    public String getFilecontentContentType() {
        return filecontentContentType;
    }

    /**
     * Sets filecontent content type.
     *
     * @param filecontentContentType the filecontent content type
     */
    public void setFilecontentContentType(String filecontentContentType) {
        this.filecontentContentType = filecontentContentType;
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

}
