package net.maritimeconnectivity.serviceregistry.models.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.maritimeconnectivity.serviceregistry.utils.GeometryBinder;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "search_area")
@Indexed
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SearchArea implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @NonStandardField(valueBinder = @ValueBinderRef(type = GeometryBinder.class))
    @Column(name = "geometry", columnDefinition = "geometry")
    private Geometry geometry;

    @ManyToMany(mappedBy = "searchAreas")
    private Set<Instance> instances = new HashSet<>();


    // ------------------------
    // Getters and Setters
    // ------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
