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

package net.maritimeconnectivity.serviceregistry.controllers.secom;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.interfaces.DiscoveryServiceSecomInterface;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchObjectResult;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
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
public class SecomDiscoveryServiceController implements DiscoveryServiceSecomInterface {

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
     * POST /api/searchService : search for the instance
     * corresponding to the search query string provided.
     *
     * @param searchFilterObject    the search filter object
     * @param pageable              the pageable information
     * @return the result list of the search
     */
    /**
     * POST /v1/searchService : The purpose of this interface is to search for
     * service instances to consume.
     *
     * @param searchFilterObject    The search filter object
     * @param page the page number to be retrieved
     * @param pageSize the maximum page size
     * @return the result list of the search
     */
    @Path(DISCOVERY_SERVICE_INTERFACE_PATH)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<SearchObjectResult> search(@Valid SearchFilterObject searchFilterObject,
                                           @QueryParam("page") @Min(0) Integer page,
                                           @QueryParam("pageSize") @Min(0) Integer pageSize)  {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.ofNullable(searchFilterObject)
                .map(SearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);

        // Perform the search
        final Page<Instance> instancesPage = instanceService.handleSearchQueryRequest(
                searchFilterObject.getQuery(),
                searchFilterObject.getFreetext(),
                searchGeometry,
                PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(pageSize).orElse(Integer.MAX_VALUE))
        );

        // And build the response
        return this.searchObjectResultMapper.convertToList(instancesPage.getContent(), SearchObjectResult.class);
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

}