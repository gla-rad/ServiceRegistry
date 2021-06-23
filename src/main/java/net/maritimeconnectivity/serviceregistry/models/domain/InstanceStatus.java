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

package net.maritimeconnectivity.serviceregistry.models.domain;

/**
 * The Instance Status enumeration.
 * <p>
 * Defines the different types of status of each instance. For example if
 * an instance is no pending authorisation or is current inactive.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum InstanceStatus {
    PROVISIONAL("provisional"),
    RELEASED("released"),
    DEPRECATED("deprecated"),
    DELETED("deleted");

    // Enum variables
    private String status;

    /**
     * The Instance Status Constructor.
     *
     * @param status the test value of the status
     */
    InstanceStatus(String status) {
        this.status = status;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the Instance Status enum that matches the provided value.
     *
     * @param value the value to create the Instance Status enum from
     * @return the Instance Status enum that matches the provided value
     */
    public static InstanceStatus fromString(String value) {
        for (InstanceStatus s : InstanceStatus.values()) {
            if (s.getStatus().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
