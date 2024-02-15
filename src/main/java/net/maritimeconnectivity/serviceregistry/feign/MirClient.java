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

package net.maritimeconnectivity.serviceregistry.feign;

import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpDeviceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpServiceDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpUserDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpVesselDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The MCP MIR Client Feign Interface.
 * <p/>
 * This client makes it easier to communicate with the MIR and is based around
 * the Springboot OAuth2 client integration. As such, it depends on the
 * FeignClientConfig class, to provide it with the service account credentials.
 * <p/>
 * The MCP MIR exposes the registered entities in five different categories i.e.
 * Devices, Services, Uses, Vessels and Roles. Therefore, this client also
 * provides five different operation to retrieve the appropriate results.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 * @see net.maritimeconnectivity.serviceregistry.config.FeignClientConfig
 */
@Component
@FeignClient(value = "mir", url = "${feign.client.mir.url:https://localhost:8443}")
public interface MirClient {

    @GetMapping("/org/{org}/device/{mrn}")
    McpDeviceDto getDeviceEntity(@PathVariable("org") String org, @PathVariable("mrn") String mrn);

    @GetMapping("/org/{org}/service/{mrn}/{version}")
    McpServiceDto getServiceEntity(@PathVariable("org") String org, @PathVariable("mrn") String mrn, @PathVariable("version") String version);

    @GetMapping("/org/{org}/user/{mrn}")
    McpUserDto getUserEntity(@PathVariable("org") String org, @PathVariable("mrn") String mrn);

    @GetMapping("/org/{org}/vessel/{mrn}")
    McpVesselDto getVesselEntity(@PathVariable("org") String org, @PathVariable("mrn") String mrn);

    @GetMapping("/org/{org}/role/{mrn}")
    McpVesselDto getRoleEntity(@PathVariable("org") String org, @PathVariable("mrn") String mrn);

}
