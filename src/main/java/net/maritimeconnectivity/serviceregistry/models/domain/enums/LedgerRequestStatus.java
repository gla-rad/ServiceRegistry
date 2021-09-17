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

import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;

/**
 * The Ledger Request Status Enumeration.
 * <p>
 * This enum contains all available status of process for the MSR ledger registration
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */

public enum LedgerRequestStatus {
    INACTIVE("inactive"), // idle state
    CREATED("created"), // submitted to MSR but vetting procedure not initiated
    VETTING("vetting"), // vetting in progress
    VETTED("vetted"), // passed vetting
    REQUESTING("requesting"), // request being sent to the ledger
    SUCCEEDED("succeeded"), // request being settled successfully
    FAILED("failed"), // request being failed with reason
    REJECTED("rejected"); // request being rejected with reason

    private final String value;

    private LedgerRequestStatus(String v) {
        this.value = v;
    }

    public String value() {
        return this.value;
    }

    public static LedgerRequestStatus fromValue(String v) {
        LedgerRequestStatus[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            LedgerRequestStatus c = var1[var3];
            if (c.value.equals(v)) {
                return c;
            }
        }

        throw new IllegalArgumentException(v);
    }
}
