package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.BooleanOperator;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.util.Arrays;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.models.EnvelopeSearchFilterObject;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@Validated
public class InstanceSearchQueryBuilder {

    @Autowired
    ObjectMapper objectMapper;

    public record QueryParams(String queryString, Geometry geometry) {}

    public QueryParams build(@Valid EnvelopeSearchFilterObject searchFilterObject) {
        Geometry geometry = Optional.ofNullable(searchFilterObject)
                .map(EnvelopeSearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);
        String q = buildQueryString(searchFilterObject);
        return new QueryParams(q, geometry);
    }

    private String buildQueryString(EnvelopeSearchFilterObject searchFilterObject) {
        // If at maximum only one geometry is provided, retrieve it

        // Check if free text
        String query = new String();

        // Now build the query if we have to
        if (Objects.nonNull(searchFilterObject) && Objects.nonNull(searchFilterObject.getQuery())) {
            // Handle the name filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getName())) {
                query = this.addToQuery(query, "name", searchFilterObject.getQuery().getName(), BooleanOperator.AND);
            }

            // Handle the status filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getStatus())) {
                query = this.addToQuery(query, "status", searchFilterObject.getQuery().getStatus(), BooleanOperator.AND);
            }

            // Handle the version filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getVersion())) {
                query = this.addToQuery(query, "version", searchFilterObject.getQuery().getVersion(), BooleanOperator.AND);
            }

            // Handle the description filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getDescription())) {
                query = this.addToQuery(query, "description", searchFilterObject.getQuery().getDescription(), BooleanOperator.AND);
            }

            // Handle the specification filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getSpecificationId())) {
                query = this.addToQuery(query, "specificationId", searchFilterObject.getQuery().getSpecificationId(), BooleanOperator.AND);
            }

            // Handle the design ID filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getDesignId())) {
                query = this.addToQuery(query, "designId", searchFilterObject.getQuery().getDesignId(), BooleanOperator.AND);
            }

            // Handle the instance ID filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getInstanceId())) {
                query = this.addToQuery(query, "instanceId", searchFilterObject.getQuery().getInstanceId(), BooleanOperator.AND);
            }

            // Handle the service Type filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getServiceType())) {
                query = this.addToQuery(query, "serviceType", searchFilterObject.getQuery().getServiceType(), BooleanOperator.AND);
            }

            // Handle the UN/LOCODE filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getUnlocode())) {
                query = this.addToQuery(query, "unlocode", searchFilterObject.getQuery().getUnlocode(), BooleanOperator.AND);
            }

            // Handle the endpoint URI filter - make sure it's not empty
            if (Objects.nonNull(searchFilterObject.getQuery().getEndpointUri()) && Strings.isNotBlank(searchFilterObject.getQuery().getEndpointUri().getPath())) {
                query = this.addToQuery(query, "endpointUri", searchFilterObject.getQuery().getEndpointUri().toString(), BooleanOperator.AND);
            }

            // Handle the data product type filter
            if (Objects.nonNull(searchFilterObject.getQuery().getDataProductType())) {
                query = this.addToQuery(query, "dataProductType", searchFilterObject.getQuery().getDataProductType().name(), BooleanOperator.AND);
            }

            // Handle the combination of MMSI and IMO filters
            if (Strings.isNotBlank(searchFilterObject.getQuery().getMmsi()) && Strings.isNotBlank(searchFilterObject.getQuery().getImo())) {
                // Open the sub-query for an OR statement
                query += Strings.isBlank(query) ? "(" : " AND (";

                // Add the sub-query statement
                query = this.addToQuery(query, "mmsi", searchFilterObject.getQuery().getMmsi(), BooleanOperator.OR);
                query = this.addToQuery(query, "imo", searchFilterObject.getQuery().getImo(), BooleanOperator.OR);

                // Close the sub-query statement
                query += ")";
            }
            // Otherwise, handle the the MMSI and IMO filters separately
            else {
                if (Strings.isNotBlank(searchFilterObject.getQuery().getMmsi())) {
                    query = this.addToQuery(query, "mmsi", searchFilterObject.getQuery().getMmsi(), BooleanOperator.AND);
                }

                if (Strings.isNotBlank(searchFilterObject.getQuery().getImo())) {
                    query = this.addToQuery(query, "imo", searchFilterObject.getQuery().getImo(), BooleanOperator.AND);
                }
            }

            // Handle the keywords filter
            if (!Arrays.isNullOrEmpty(searchFilterObject.getQuery().getKeywords())) {
                // Open the sub-query for an OR statement
                query += Strings.isBlank(query) ? "(" : " AND (";

                for (String keyword : searchFilterObject.getQuery().getKeywords()) {
                    query = this.addToQuery(query, "keywords", keyword, BooleanOperator.AND);
                }

                // Close the sub-query statement
                query += ")";
            }
        }
        return query;
    }

    /**
     * A useful utility function that is able to parse the provided geometry
     * string as both the SECOM-compliant WKT format and the non-compliant but
     * still pretty useful GeoJSON format.
     *
     * @param geometryString the geometry string in WKT or GeoJSON format
     * @return the parsed JTS geometry
     */
    private Geometry parseGeometry(String geometryString) {
        // Check is the geometry is in JSON format
        final boolean jsonFormat = Optional.ofNullable(geometryString)
                .map(gs -> {
                    try {
                        return this.objectMapper.readTree(geometryString);
                    } catch (JacksonException ex) {
                        return null;
                    }
                })
                .isPresent();

        // First check the standard WKT format
        if (!jsonFormat) {
            try {
                return WKTUtil.convertWKTtoGeometry(geometryString);
            } catch (ParseException ex) {
                throw new SecomValidationException(ex.getMessage());
            }
        }
        // Then check the non-standard GeoJSON format
        else {
            try {
                return GeometryJSONConverter.convertToGeometry(this.objectMapper.readTree(geometryString));
            } catch (JsonProcessingException ex) {
                throw new SecomValidationException(ex.getMessage());
            }
        }
    }

    /**
     * A helper function to construct the SECOM discovery search search query.
     * This is composed of various search filters alongside their valued,
     * connected through boolean operators, e.g. AND/OR.
     *
     * @param query       The query constructed so far
     * @param filterName  The new filter name to be added
     * @param filterValue The new filter value to be added
     * @param operator    The boolean operator to be used
     * @return the constructed search query
     */
    private String addToQuery(String query, String filterName, String filterValue, BooleanOperator operator) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(query);
        if (stringBuilder.isEmpty() || query.endsWith("(")) {
            stringBuilder.append(String.format("%s:%s", filterName, filterValue.replaceAll(":", "\\\\:")));
        } else {
            stringBuilder.append(String.format(" %s %s:%s", operator.name(), filterName, filterValue.replaceAll(":", "\\\\:")));
        }
        return stringBuilder.toString();
    }

}
