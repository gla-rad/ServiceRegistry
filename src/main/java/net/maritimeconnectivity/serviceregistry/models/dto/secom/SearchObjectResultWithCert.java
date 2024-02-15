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

package net.maritimeconnectivity.serviceregistry.models.dto.secom;

import net.maritimeconnectivity.serviceregistry.models.dto.mcp.McpCertitifateDto;
import org.grad.secom.core.models.SearchObjectResult;

import java.util.List;

/**
 * The SearchObjectResultWithCert Class.
 * <p/>
 * This class extends the standard SECOM SearchObjectResult object with
 * an additional field that include the list of the services certificates.
 * This will allow the MSR to inform its client on how to contact the service
 * both in terms of endpoint, as well as security.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class SearchObjectResultWithCert extends SearchObjectResult {

    // Class Variables
    List<McpCertitifateDto> certificates;

    /**
     * Gets certificates.
     *
     * @return the certificates
     */
    public List<McpCertitifateDto> getCertificates() {
        return certificates;
    }

    /**
     * Sets certificates.
     *
     * @param certificates the certificates
     */
    public void setCertificates(List<McpCertitifateDto> certificates) {
        this.certificates = certificates;
    }

}
