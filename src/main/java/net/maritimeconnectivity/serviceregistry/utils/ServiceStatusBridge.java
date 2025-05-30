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

package net.maritimeconnectivity.serviceregistry.utils;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.iala_aism.g1128.v1_7.serviceinstanceschema.ServiceStatus;

import java.util.Objects;

/**
 * The Service Status Bridge Class.
 *
 * The Service Status is actually represented by an enum but in the datatables
 * server-side processing we need to be able to manipulate it as a string for
 * the searching, ordering the paging operations. Therefore, the hibernate
 * search ORM package provides the notion of bridges that connect the object
 * with a string.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class ServiceStatusBridge implements ValueBridge<ServiceStatus, String> {

    /**
     * Implement a generic object to string translation, depending on whether
     * the provided object is a ServiceStatus enum or another string.
     *
     * @param value the value to be translated
     * @param context the value bridge indexed value context
     * @return the string representation
     */
    @Override
    public String toIndexedValue(ServiceStatus value, ValueBridgeToIndexedValueContext context) {
        // Check for nulls first
        if(Objects.nonNull(value)) {
            return value.name();
        }
        // Otherwise, it's null
        return null;
    }

}
