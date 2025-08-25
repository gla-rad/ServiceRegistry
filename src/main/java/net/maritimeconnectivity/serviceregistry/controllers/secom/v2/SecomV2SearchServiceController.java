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
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.feign.MirClient;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpCertificateDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpEntityBase;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpServiceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.logging.log4j.util.Strings;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.SearchServiceServiceInterface;
import org.grad.secomv2.core.models.ResponseSearchObject;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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

    @Value("${info.msr.url}")
    private String msrBaseUrl;

    @Value("${info.msr.localSearchOnly}")
    private boolean localSearchOnly;

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

    @Autowired
    Gmsp gmspClient;
    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, SearchObjectResult> searchObjectResultMapper;

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
    public ResponseSearchObject searchService(@Valid SearchFilterObject searchFilterObject)  {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.ofNullable(searchFilterObject)
                .map(SearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);


        // Perform the search locally
        final Page<Instance> instancesPage = this.instanceService.search(searchFilterObject);

        String transactionId = UUID.randomUUID().toString();

        //CallbackUrl is  /V2/UPLOADRESULTS/[TRANSACTIONID]
        String callBackEndpoint = String.format("%s/api/secom/v2/uploadResults/%s", msrBaseUrl, transactionId);

        //Propagate the search to the GMSP if available
        String gmspRequestUuid = null;
        if (!localSearchOnly) {
            gmspRequestUuid = gmspClient.globalSearch(callBackEndpoint, "", searchFilterObject, searchGeometry);
        }

        // Get the search object results and if possible also update the
        // certificates through the MIR.
        List<SearchObjectResult> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), SearchObjectResultWithCert.class);

        // Careful cause depending on the configuration an MIR client might not
        // be available. In those case the mirClient will be null.
        if(this.mirClient != null) {
            for (SearchObjectResult searchObject : searchObjectResults) {
                try {
                    // Retrieve the certificates from the MIR
                    McpServiceDto mcpEntity = this.mirClient.getServiceEntity(
                            Optional.of(searchObject)
                                    .map(SearchObjectResult::getOrganizationId)
                                    .map(Strings::trimToNull)
                                    .orElse(null),
                            Optional.of(searchObject)
                                    .map(SearchObjectResult::getInstanceId)
                                    .map(Strings::trimToNull)
                                    .orElse(null),
                            Optional.of(searchObject)
                                    .map(SearchObjectResult::getVersion)
                                    .map(Strings::trimToNull)
                                    .orElse(null)
                    );
                    // And append the valid ones to the search object
                    ((SearchObjectResultWithCert) searchObject).setCertificates(Optional.ofNullable(mcpEntity)
                            .map(McpEntityBase::getCertificates)
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .filter(not(McpCertificateDto::isRevoked))
                            .collect(Collectors.toList()));
                } catch (FeignException ex) {
                    log.error("Error while retrieving certificate for entity {}: {}",
                            searchObject.getInstanceId(),
                            ex.getMessage());
                }
            }
        }

        // Finally build the response
        ResponseSearchObject responseSearchObject = new ResponseSearchObject();
        responseSearchObject.setSearchServiceResult(searchObjectResults);
        return responseSearchObject;
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
}