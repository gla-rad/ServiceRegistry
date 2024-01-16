/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.models.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * The type Doc.
 * <p>
 * A doc represents a human readable document that can be attached to various
 * objects.This could be an office document containing guidelines linked,to a
 * service specification, or a Getting Started PDF attached to a service
 * instance.
 * </p>
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "document")
@Indexed
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Doc implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GenericField(name = "id_sort", sortable = Sortable.YES)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @KeywordField(sortable = Sortable.YES)
    @Column(name = "name", nullable = false)
    private String name;

    @FullTextField()
    @KeywordField(name = "comment_sort", normalizer = "lowercase", sortable = Sortable.YES)
    @Column(name = "comment")
    private String comment;

    @NotNull
    @KeywordField(sortable = Sortable.YES)
    @Column(name = "mimetype", nullable = false)
    private String mimetype;

    @NotNull
    @Lob
    @Column(name = "filecontent", nullable = false)
    private byte[] filecontent;

    @Column(name = "filecontent_content_type", nullable = false)
    private String filecontentContentType;

    @OneToOne(fetch = FetchType.EAGER)
    @IndexedEmbedded(includePaths = "id_sort")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Instance instance;

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
     * Gets instance.
     *
     * @return the instance
     */
    public Instance getInstance() {
        return instance;
    }

    /**
     * Sets instance.
     *
     * @param instance the instance
     */
    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Doc)) return false;
        Doc doc = (Doc) o;
        return id.equals(doc.id);
    }

    /**
     * Overrides the string representation of the object.
     *
     * @return the string representation
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
