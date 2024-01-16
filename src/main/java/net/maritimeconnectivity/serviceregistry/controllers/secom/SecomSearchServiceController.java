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

package net.maritimeconnectivity.serviceregistry.controllers.secom;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.BooleanOperator;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.logging.log4j.util.Strings;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.interfaces.SearchServiceSecomInterface;
import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchObjectResult;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;
import java.util.Optional;

/**
 * The SECOM Discovery Service Controller.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@Path("/")
@Validated
@Slf4j
public class SecomSearchServiceController implements SearchServiceSecomInterface {

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

    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, SearchObjectResult> searchObjectResultMapper;

    /**
     * POST /v1/searchService : The purpose of this interface is to search for
     * service instances to consume.
     *
     * @param searchFilterObject The search filter object
     * @param page               the page number to be retrieved
     * @param pageSize           the maximum page size
     * @return the result list of the search
     */
    @Tag(name = "SECOM")
    @Transactional
    @Path(SEARCH_SERVICE_INTERFACE_PATH)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseSearchObject searchService(@Valid SearchFilterObject searchFilterObject,
                                              @QueryParam("page") @Min(0) Integer page,
                                              @QueryParam("pageSize") @Min(0) Integer pageSize)  {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.ofNullable(searchFilterObject)
                .map(SearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);

        // Check if free text
        final boolean isFreeText = Strings.isNotBlank(searchFilterObject.getFreetext()) || Objects.isNull(searchFilterObject.getQuery());
        String query = new String();

        // Now build the query if we have to
        if(isFreeText) {
            query = searchFilterObject.getFreetext();
        } else {
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
            if (Strings.isNotBlank(searchFilterObject.getQuery().getInstanceId()))
            {
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

            // Handle the endpoint URI filter
            if (Objects.nonNull(searchFilterObject.getQuery().getEndpointUri())) {

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

                if (Strings.isNotBlank(searchFilterObject.getQuery().getImo()))
                {
                    query = this.addToQuery(query, "imo", searchFilterObject.getQuery().getImo(), BooleanOperator.AND);
                }
            }

            // Handle the keywords filter
            if (Strings.isNotBlank(searchFilterObject.getQuery().getKeywords())) {
                // Open the sub-query for an OR statement
                query += Strings.isBlank(query) ? "(" : " AND (";

                for(String keyword: searchFilterObject.getQuery().getKeywords().split(",")) {
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
                PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(pageSize).orElse(Integer.MAX_VALUE))
        );

        // And build the response
        ResponseSearchObject responseSearchObject = new ResponseSearchObject();
        responseSearchObject.setSearchServiceResult(this.searchObjectResultMapper.convertToList(instancesPage.getContent(), SearchObjectResult.class));
        return responseSearchObject;
    }

    /**
     * A useful utility function that is able to parse the provided geometry
     * string as both the SECOM-compliant WKT format and the non compliant but
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