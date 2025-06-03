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

package net.maritimeconnectivity.serviceregistry.models.dto.mcp;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * The MCP Device DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class McpDeviceDto extends McpEntityBase {

    // Class Variable
    @NotNull
    private String name;

    /**
     * Instantiates a new Mcp device dto.
     */
    public McpDeviceDto() {
    }

    /**
     * Instantiates a new Mcp device dto.
     *
     * @param name the name
     * @param mrn  the mrn
     */
    public McpDeviceDto(String name, String mrn) {
        super(mrn);
        this.name = name;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpDeviceDto)) return false;
        if (!super.equals(o)) return false;
        McpDeviceDto that = (McpDeviceDto) o;
        return Objects.equals(name, that.name);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
