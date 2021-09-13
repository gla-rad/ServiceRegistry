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

package net.maritimeconnectivity.serviceregistry.models.domain.enums;

/**
 * The Ledger Request Status Enumeration.
 * <p>
 * This enum contains all available status of process for the MSR ledger registration
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */

public enum LedgerRequestStatus {
    INACTIVE("inactive", false), // idle state
    CREATED("created", false), // submitted to MSR but vetting procedure not initiated
    VETTING("vetting", false), // vetting in progress
    VETTED("vetted", false), // passed vetting
    REQUESTING("requesting", true), // request being sent to the ledger
    SUCCEEDED("succeeded", true), // request being settled successfully
    FAILED("failed", true), // request being failed with reason
    REJECTED("rejected", true); // request being rejected with reason

    // Enum Variables
    private final String value;
    private final boolean restricted;

    /**
     * The LedgerRequestStatus constructor.
     *
     * @param value     The string value of the enum
     * @param restricted   Whether local assignment is allowed
     */
    LedgerRequestStatus(String value, boolean restricted) {
        this.value = value;
        this.restricted = restricted;
    }

    /**
     * Value string.
     *
     * @return the string
     */
    public String value() {
        return this.value;
    }

    /**
     * Is restricted boolean.
     *
     * @return the boolean
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Retrieves the enum entry from a string representation.
     *
     * @param value     The string representation of the enum
     * @return the matching enum entry
     */
    public static LedgerRequestStatus fromValue(String value) {
        LedgerRequestStatus[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            LedgerRequestStatus c = var1[var3];
            if (c.value.equals(value)) {
                return c;
            }
        }

        throw new IllegalArgumentException(value);
    }

}
