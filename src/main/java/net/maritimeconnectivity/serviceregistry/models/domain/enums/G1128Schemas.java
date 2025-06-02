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

package net.maritimeconnectivity.serviceregistry.models.domain.enums;

import org.iala_aism.g1128.v1_7.serviceinstanceschema.ServiceInstance;

/**
 * The G1128 Schemas Enumeration.
 * <p>
 * To allow easy access to the G1128 schema definitions from the G1128
 * package, this enum contains all available XSD file names and classpath
 * locations as an enum.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum G1128Schemas {
    INSTANCE("instance", "xsd/v1_7/ServiceInstanceSchema.xsd", ServiceInstance.class);

    // Enum Variables
    private String name;
    private String path;
    private Class schemaClass;

    /**
     * The G1128 Schema Enumeration Constructor.
     *
     * @param name the schema name
     * @param path the schema path
     */
    G1128Schemas(String name, String path, Class schemaClass) {
        this.name = name;
        this.path = path;
        this.schemaClass = schemaClass;
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
     * Gets path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets path.
     *
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets schema class.
     *
     * @return the schema class
     */
    public Class getSchemaClass() {
        return schemaClass;
    }

    /**
     * Sets schema class.
     *
     * @param schemaClass the schema class
     */
    public void setSchemaClass(Class schemaClass) {
        this.schemaClass = schemaClass;
    }
}
