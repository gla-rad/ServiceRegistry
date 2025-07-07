package net.maritimeconnectivity.serviceregistry.models.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import java.io.Serializable;

@Entity
@Table(name = "search_area")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SearchArea implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "object_id", unique = true)
    private Integer objectId;


    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "geometry", columnDefinition = "geometry")
    private Geometry geometry;

    // ------------------------
    // Getters and Setters
    // ------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }

    public Integer getExternalId() {
        return externalId;
    }

    public void setExternalId(Integer externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    // ------------------------
    // GeoJSON Support Methods
    // ------------------------

    @Transient
    public JsonNode getGeometryJson() {
        return GeometryJSONConverter.convertFromGeometry(this.geometry);
    }

    public void setGeometryJson(JsonNode geometry) throws ParseException {
        this.setGeometry(GeometryJSONConverter.convertToGeometry(geometry));
    }
}
