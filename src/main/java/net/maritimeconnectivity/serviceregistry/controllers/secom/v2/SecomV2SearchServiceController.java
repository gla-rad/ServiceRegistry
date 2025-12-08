/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.feign.MirClient;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.BooleanOperator;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpEntityBase;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpServiceDto;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.util.Arrays;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.SearchServiceServiceInterface;
import org.grad.secomv2.core.models.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * The SECOM Discovery Service Controller.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@Path("/")
@Validated
@Slf4j
public class SecomV2SearchServiceController implements SearchServiceServiceInterface {

    /**
     * The Object Mapper.
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * The Instance Service.
     */
    @Autowired
    InstanceService instanceService;

    @Autowired(required = false)
    MirClient mirClient;

    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, ServiceInstanceObject> searchObjectResultMapper;

    /**
     * POST /v2/searchService : The purpose of this interface is to search for
     * service instances to consume.
     *
     * @param searchFilterObject The search filter object
     * @return the result list of the search
     */
    @Tag(name = "SECOM")
    @Transactional
    @Path(SEARCH_SERVICE_INTERFACE_PATH)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResult searchService(@Valid SearchFilterObject searchFilterObject)  {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        EnvelopeSearchFilterObject envelopeSearchFilterObject = searchFilterObject.getEnvelope();

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.ofNullable(envelopeSearchFilterObject)
                .map(EnvelopeSearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);

        // Check if free text
        String query = new String();

        // Now build the query if we have to
        if(Objects.nonNull(envelopeSearchFilterObject) && Objects.nonNull(envelopeSearchFilterObject.getQuery())) {
            // Handle the name filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getName())) {
                query = this.addToQuery(query, "name", envelopeSearchFilterObject.getQuery().getName(), BooleanOperator.AND);
            }

            // Handle the status filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getStatus())) {
                query = this.addToQuery(query, "status", envelopeSearchFilterObject.getQuery().getStatus(), BooleanOperator.AND);
            }

            // Handle the version filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getVersion())) {
                query = this.addToQuery(query, "version", envelopeSearchFilterObject.getQuery().getVersion(), BooleanOperator.AND);
            }

            // Handle the description filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getDescription())) {
                query = this.addToQuery(query, "description", envelopeSearchFilterObject.getQuery().getDescription(), BooleanOperator.AND);
            }

            // Handle the specification filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getSpecificationId())) {
                query = this.addToQuery(query, "specificationId", envelopeSearchFilterObject.getQuery().getSpecificationId(), BooleanOperator.AND);
            }

            // Handle the design ID filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getDesignId())) {
                query = this.addToQuery(query, "designId", envelopeSearchFilterObject.getQuery().getDesignId(), BooleanOperator.AND);
            }

            // Handle the instance ID filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getInstanceId())) {
                query = this.addToQuery(query, "instanceId", envelopeSearchFilterObject.getQuery().getInstanceId(), BooleanOperator.AND);
            }

            // Handle the service Type filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getServiceType())) {
                query = this.addToQuery(query, "serviceType", envelopeSearchFilterObject.getQuery().getServiceType(), BooleanOperator.AND);
            }

            // Handle the UN/LOCODE filter
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getUnlocode())) {
                query = this.addToQuery(query, "unlocode", envelopeSearchFilterObject.getQuery().getUnlocode(), BooleanOperator.AND);
            }

            // Handle the endpoint URI filter - make sure it's not empty
            if (Objects.nonNull(envelopeSearchFilterObject.getQuery().getEndpointUri()) && Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getEndpointUri().getPath())) {
                query = this.addToQuery(query, "endpointUri", envelopeSearchFilterObject.getQuery().getEndpointUri().toString(), BooleanOperator.AND);
            }

            // Handle the data product type filter
            if (Objects.nonNull(envelopeSearchFilterObject.getQuery().getDataProductType())) {
                query = this.addToQuery(query, "dataProductType", envelopeSearchFilterObject.getQuery().getDataProductType().name(), BooleanOperator.AND);
            }

            // Handle the combination of MMSI and IMO filters
            if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getMmsi()) && Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getImo())) {
                // Open the sub-query for an OR statement
                query += Strings.isBlank(query) ? "(" : " AND (";

                // Add the sub-query statement
                query = this.addToQuery(query, "mmsi", envelopeSearchFilterObject.getQuery().getMmsi(), BooleanOperator.OR);
                query = this.addToQuery(query, "imo", envelopeSearchFilterObject.getQuery().getImo(), BooleanOperator.OR);

                // Close the sub-query statement
                query += ")";
            }
            // Otherwise, handle the the MMSI and IMO filters separately
            else {
                if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getMmsi())) {
                    query = this.addToQuery(query, "mmsi", envelopeSearchFilterObject.getQuery().getMmsi(), BooleanOperator.AND);
                }

                if (Strings.isNotBlank(envelopeSearchFilterObject.getQuery().getImo())) {
                    query = this.addToQuery(query, "imo", envelopeSearchFilterObject.getQuery().getImo(), BooleanOperator.AND);
                }
            }

            // Handle the keywords filter
            if (!Arrays.isNullOrEmpty(envelopeSearchFilterObject.getQuery().getKeywords())) {
                // Open the sub-query for an OR statement
                query += Strings.isBlank(query) ? "(" : " AND (";

                for (String keyword : envelopeSearchFilterObject.getQuery().getKeywords()) {
                    query = this.addToQuery(query, "keywords", keyword, BooleanOperator.AND);
                }

                // Close the sub-query statement
                query += ")";
            }
        }

        // Perform the search
        final Page<Instance> instancesPage = this.instanceService.handleSearchQueryRequest(
                query,
                searchGeometry,
                PageRequest.of(0, Integer.MAX_VALUE)
        );

        try {
            // Get the search object results and if possible also update the
            // certificates through the MIR.
            List<ServiceInstanceObject> serviceObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), ServiceInstanceObject.class);

        // Careful cause depending on the configuration an MIR client might not
        // be available. In those case the mirClient will be null.
        if(this.mirClient != null) {
            for (ServiceInstanceObject serviceObject : serviceObjectResults) {
                try {
                    // Retrieve the certificates from the MIR
                    McpServiceDto mcpEntity = this.mirClient.getServiceEntity(
                            Optional.of(serviceObject)
                                    .map(ServiceInstanceObject::getOrganizationId)
                                    .map(Strings::trimToNull)
                                    .orElse(null),
                            Optional.of(serviceObject)
                                    .map(ServiceInstanceObject::getInstanceId)
                                    .map(Strings::trimToNull)
                                    .orElse(null),
                            Optional.of(serviceObject)
                                    .map(ServiceInstanceObject::getVersion)
                                    .map(Strings::trimToNull)
                                    .orElse(null)
                    );
                    // And append the valid ones to the search object
                    ((ServiceInstanceObject) serviceObject).setCertificates(new ArrayList<>(Optional.ofNullable(mcpEntity)
                            .map(McpEntityBase::getValidCertificatesAsString)
                            .orElseGet(Collections::emptyList)));
                } catch (FeignException ex) {
                    log.error("Error while retrieving certificate for entity {}: {}",
                            serviceObject.getInstanceId(),
                            ex.getMessage());
                }
            }
        }

        // Finally build the response
        SearchResult responseSearchObject = new SearchResult();
        responseSearchObject.setServiceInstance(serviceObjectResults);
        return responseSearchObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A useful utility function that is able to parse the provided geometry
     * string as both the SECOM-compliant WKT format and the non-compliant but
     * still pretty useful GeoJSON format.
     *
     * @param geometryString the geometry string in WKT or GeoJSON format
     * @return the parsed JTS geometry
     */
    protected Geometry parseGeometry(String geometryString) {
        // Check is the geometry is in JSON format
        final boolean jsonFormat = Optional.ofNullable(geometryString)
                .map(gs -> {
                    try { return this.objectMapper.readTree(geometryString); }
                    catch (JacksonException ex) { return null; }
                })
                .isPresent();

        // First check the standard WKT format
        if(!jsonFormat) {
            try {
                return WKTUtil.convertWKTtoGeometry(geometryString);
            } catch (ParseException ex) {
                throw new SecomValidationException(ex.getMessage());
            }
        }
        // Then check the non-standard GeoJSON format
        else {
            try{
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
     * @param query         The query constructed so far
     * @param filterName    The new filter name to be added
     * @param filterValue   The new filter value to be added
     * @param operator      The boolean operator to be used
     * @return the constructed search query
     */
    protected String addToQuery(String query, String filterName, String filterValue, BooleanOperator operator) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(query);
        if (stringBuilder.isEmpty() || query.endsWith("(")) {
            stringBuilder.append(String.format("%s:%s", filterName, filterValue.replaceAll(":","\\\\:")));
        } else {
            stringBuilder.append(String.format(" %s %s:%s", operator.name(), filterName, filterValue.replaceAll(":","\\\\:")));
        }
        return stringBuilder.toString();
    }

}