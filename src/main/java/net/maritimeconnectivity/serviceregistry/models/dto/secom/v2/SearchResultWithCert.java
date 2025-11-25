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

package net.maritimeconnectivity.serviceregistry.models.dto.secom.v2;

import jakarta.validation.constraints.NotNull;
import org.grad.secomv2.core.models.SearchObjectResult;

import java.util.List;

/**
 * The SearchResultWithCert Class.
 * <p/>
 * This class mirrors the standard SECOM SearchResul object but
 * actually uses the internal SearchObjectResultWithCert objects into
 * the search service results list. This is not actually required in the
 * application runtime, but it's useful for the tests.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class SearchResultWithCert {

    // Class Variables
    private @NotNull String transactionId;
    private List<SearchObjectResultWithCert> services;

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets services.
     *
     * @return the services
     */
    public List<SearchObjectResultWithCert> getServices() {
        return services;
    }

    /**
     * Sets services.
     *
     * @param services the services
     */
    public void setServices(List<SearchObjectResultWithCert> services) {
        this.services = services;
    }
}
