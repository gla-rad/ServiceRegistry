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

import org.grad.secomv2.core.utils.SecomPemUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.feign.MirClient;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpEntityBase;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpServiceDto;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.logging.log4j.util.Strings;
import org.grad.secomv2.core.exceptions.SecomNotFoundException;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.SearchServiceServiceInterface;
import org.grad.secomv2.core.models.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


import java.util.*;

/**
 * The SECOM Discovery Service Controller.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@Validated
@Slf4j
public class SecomV2SearchServiceController implements SearchServiceServiceInterface {

    @Value("${info.msr.forceCertificateCheck:false}")
    private boolean forceCertificateCheck;

    @Value("${info.msr.url}")
    private String msrBaseUrl;

    @Value("${info.msr.mrn}")
    private String ownMrn;

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
     * The MIR Client for the certificate operations.
     */
    @Autowired(required = false)
    MirClient mirClient;

    /**
     * The GMSP Client for the global search.
     */
    @Autowired(required = false)
    Gmsp gmspClient;

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
    public ResponseEntity<SearchResult> searchService(@Valid @RequestBody SearchFilterObject searchFilterObject) {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        EnvelopeSearchFilterObject envelopeSearchFilterObject = searchFilterObject.getEnvelope();

        // Extract consumer MRN from certificate
        String consumerMrn = SecomPemUtils.getMrnFromEnvelope(envelopeSearchFilterObject);

        log.info("Extracted MRN from certificate: {}", consumerMrn);

        log.info("Search filter object value {}", envelopeSearchFilterObject.getLocalOnly());

        boolean localSearchOnly = Optional.ofNullable(envelopeSearchFilterObject.getLocalOnly()).orElse(true);

        log.info("Local search only set to: {}", localSearchOnly);

        // Extract the search parameter query
        SearchParameters query = envelopeSearchFilterObject.getQuery();
        String unparsedGeom = envelopeSearchFilterObject.getGeometry();

        //Reject when no query or geometry is provided
        if ((unparsedGeom == null || unparsedGeom.isEmpty()) && (query == null || query.isEmpty())) {
            throw new SecomValidationException("No valid search parameters provided. Please provide either a geometry or a query.");
        }

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.of(envelopeSearchFilterObject)
                .map(EnvelopeSearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);

        // If searching for an MMSI without a design ID, return a 400
        if (envelopeSearchFilterObject.getQuery().getMmsi() != null && !envelopeSearchFilterObject.getQuery().getMmsi().isEmpty()
        && (envelopeSearchFilterObject.getQuery().getDesignId() == null || envelopeSearchFilterObject.getQuery().getDesignId().isEmpty())) {
            throw new SecomValidationException("Searching for an MMSI without DesignId not allowed");
        }

        // If searching for an IMO without a design ID, return a 400
        if (envelopeSearchFilterObject.getQuery().getImo() != null && !envelopeSearchFilterObject.getQuery().getImo().isEmpty()
                && (envelopeSearchFilterObject.getQuery().getDesignId() == null || envelopeSearchFilterObject.getQuery().getDesignId().isEmpty())) {
            throw new SecomValidationException("Searching for an IMO without DesignId not allowed");
        }

        // Perform the search locally
        final Page<Instance> instancesPage = this.instanceService.search(envelopeSearchFilterObject);

        if (instancesPage.isEmpty() && localSearchOnly) {
            log.debug("No instances found for search filter object");
            throw new SecomNotFoundException("No instances found for Local search with search filter " +
                    "object");
        }

        // Create a transaction ID for this transaction
        // TODO: Isn't this login better suited for a service?
        UUID transactionId = UUID.randomUUID();

        //CallbackUrl is  /V2/UPLOADRESULTS/[TRANSACTIONID]
        String callBackEndpoint = String.format("%s/api/g1191/v2/uploadResults/%s",
                msrBaseUrl,
                transactionId);

        //Aggregator

        //Propagate the search to the GMSP if available
        log.debug("gmspClient is null: {}", this.gmspClient == null);
        log.debug("localSearchOnly: {}", localSearchOnly);
        final String gmspRequestUuid;
        if (this.gmspClient != null && !localSearchOnly) {
            gmspRequestUuid = gmspClient.globalSearch(callBackEndpoint,
                    consumerMrn,
                    searchFilterObject,
                    searchGeometry);
            log.debug("global search initiated with GMSP UUID: {}", gmspRequestUuid);
        }

        // Get the search object results and if possible also update the
        // certificates through the MIR.
        List<ServiceInstanceObject> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), ServiceInstanceObject.class);

        // Foreach SearchObjectResult set the sourceMSR
        searchObjectResults.forEach(r -> r.setSourceMSRs(new String[]{ownMrn}));

        // Careful cause depending on the configuration an MIR client might not
        // be available. In those case the mirClient will be null.
        if(this.mirClient != null && this.forceCertificateCheck) {
            for (ServiceInstanceObject searchObject : searchObjectResults) {
                try {
                    // Retrieve the certificates from the MIR
                    McpServiceDto mcpEntity = this.mirClient.getServiceEntity(
                            Optional.of(searchObject)
                                    .map(ServiceInstanceObject::getOrganizationId)
                                    .map(Strings::trimToNull)
                                    .orElse(null),
                            Optional.of(searchObject)
                                    .map(ServiceInstanceObject::getInstanceId)
                                    .map(Strings::trimToNull)
                                    .orElse(null)
                    );
                    // And append the valid ones to the search object
                    searchObject.setCertificates(
                            Optional.ofNullable(mcpEntity)
                                    .map(McpEntityBase::getValidCertificatesAsString) // List<String>
                                    .map(list -> list.toArray(new String[0]))         // convert to String[]
                                    .orElse(new String[0])                             // empty array if null
                    );
                } catch (FeignException ex) {
                    log.error("Error while retrieving certificate for entity {}: {}",
                            searchObject.getInstanceId(),
                            ex.getMessage());
                }
            }
        }
        log.debug("UUID is {}", transactionId);

        // Finally build the response

        //Put placeholders for old code missing correct attributes
        // Normalize legacy data so it validates against OpenAPI
        searchObjectResults.forEach(r -> {
            if (r.getApiDoc() == null) {
                r.setApiDoc("https://example.com");
            }

            if (r.getDescription() == null) {
                r.setDescription("Description placeholder");
            }

            if (r.getOrganizationId() == null) {
                r.setOrganizationId("urn:mrn:mcp:org:mcc:legacy");
            }

            // Trim invalid whitespace
            if (r.getInstanceId() != null) {
                r.setInstanceId(r.getInstanceId().trim());
            }

            if (r.getEndpointUri() != null) {
                r.setEndpointUri(r.getEndpointUri().trim());
            }

            if (r.getName() != null) {
                r.setName(r.getName().trim());
            }

            // Fix clearly broken legacy IDs
            if ("urn:mrn:".equals(r.getInstanceId())) {
                r.setInstanceId("urn:mrn:mcp:entity:mcc:legacy:placeholder");
            }
        });

        // Build the envelope
        final EnvelopeSearchResultObject envelope = new EnvelopeSearchResultObject();
        envelope.setServiceInstance(searchObjectResults);
        envelope.setTransactionId(transactionId);

        // Build the search result
        final SearchResult searchResult = new SearchResult();
        searchResult.setEnvelope(envelope);

        // And return
        return ResponseEntity.ok(searchResult);
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
            } catch (JacksonException ex) {
                throw new SecomValidationException(ex.getMessage());
            }
        }
    }
}