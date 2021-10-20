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

package net.maritimeconnectivity.serviceregistry.utils;

import org.hibernate.search.bridge.builtin.StringBridge;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;

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
public class ServiceStatusBridge extends StringBridge {

    /**
     * Implement a generic object to string translation, depending on whether
     * the provided object is a ServiceStatus enum or another string.
     *
     * @param object the object to be translated
     * @return the string representation
     */
    public String objectToString(Object object) {
        // If the object comes from the database and it's an enum then return
        // its name as a string.
        if(ServiceStatus.class.isInstance(object)) {
            return ((ServiceStatus)object).name();
        }
        // Otherwise get the toString function to do out dirty work
        return object.toString();
    }

}
