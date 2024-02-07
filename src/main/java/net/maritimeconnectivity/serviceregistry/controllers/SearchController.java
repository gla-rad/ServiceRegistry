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

package net.maritimeconnectivity.serviceregistry.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.DomainDtoMapper;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.InstanceDto;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import net.maritimeconnectivity.serviceregistry.utils.PaginationUtil;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for providing the search facilities.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Deprecated
@Hidden
@RestController
@RequestMapping("/api/_search")
@Slf4j
public class SearchController {

    /**
     * The Instance Service.
     */
    @Autowired
    InstanceService instanceService;

    /**
     * Object Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Instance, InstanceDto> instanceDomainToDtoMapper;

    /**
     * SEARCH /_search/instances?query=:query : search for the instance
     * corresponding to the search query string provided.
     *
     * @param queryString   the query string of the instance search
     * @param geometry      the geometry of the instance search
     * @param geometryWKT   the geometry WKT string of the instance search
     * @param globalSearch  whether the global ledger search facility should be used
     * @return the result of the search
     */
    @GetMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InstanceDto>> searchInstances(@RequestParam("queryString") String queryString,
                                                             @RequestParam(value = "geometry") Optional<Geometry> geometry,
                                                             @RequestParam(value = "geometryWKT") Optional<String> geometryWKT,
                                                             @RequestParam(value = "globalSearch") Optional<Boolean> globalSearch,
                                                             @ParameterObject Pageable pageable) throws URISyntaxException {
        // We only allow one geometry specification method
        if(geometry.isPresent() && geometryWKT.filter(StringUtils::isNotBlank).isPresent()) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("search",
                            "multiplesearchspecifications",
                            "Multiple geometries specifications provided... cannot proceed with the search"))
                    .build();
        }
        // If at maximum only one geometry is provided, retrieve it
        final Geometry searchGeometry =  geometry.filter(Geometry::isValid)
                .orElseGet(() -> geometryWKT.map(wkt -> {
                        try {
                            return WKTUtil.convertWKTtoGeometry(wkt);
                        } catch (Exception ex) {
                            throw new InvalidRequestException(ex.getMessage(), ex);
                        }
                    }).orElse(null)
                );
        // And also parse it as text for the logging
        final String searchGeometryString = Optional.ofNullable(searchGeometry)
                .map(Geometry::toText)
                .orElseGet(() -> geometryWKT.orElse("None "));
        log.debug("REST request to search for a page of Instances for query {} and geometry {}", queryString, searchGeometryString);
        // Perform the search
        final Page<Instance> page = instanceService.handleSearchQueryRequest(queryString, searchGeometry, pageable);
        // And build the response
        return ResponseEntity.ok()
                .headers(PaginationUtil.generatePaginationHttpHeaders(page, "/api/_search/instances"))
                .body(this.instanceDomainToDtoMapper.convertToList(page.getContent(), InstanceDto.class));
    }

}
