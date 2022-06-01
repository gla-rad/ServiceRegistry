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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.GeometryJSONConverter;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.grad.secom.exceptions.SecomValidationException;
import org.grad.secom.interfaces.DiscoveryServiceInterface;
import org.grad.secom.models.SearchFilterObject;
import org.grad.secom.models.SearchObjectResult;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/secom")
@Slf4j
public class SecomDiscoveryServiceController implements DiscoveryServiceInterface {

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
    @Override
    @SneakyThrows
    @PostMapping(value = DISCOVERY_SERVICE_INTERFACE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SearchObjectResult>> search(@RequestBody @Valid SearchFilterObject searchFilterObject,
                                                           @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable)  {
        log.debug("REST request to search for a page of Instances for search filter object: {}", searchFilterObject);

        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  Optional.ofNullable(searchFilterObject)
                .map(SearchFilterObject::getGeometry)
                .map(this::parseGeometry)
                .orElse(null);

        // Perform the search
        final Page<Instance> page = instanceService.handleSearchQueryRequest(
                searchFilterObject.getQuery(),
                searchFilterObject.getFreetext(),
                searchGeometry,
                pageable);

        // And build the response
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/secom/"  + DISCOVERY_SERVICE_INTERFACE_PATH))
                .body(this.searchObjectResultMapper.convertToList(page.getContent(), SearchObjectResult.class));
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