/*
 * Copyright (c) 2026 Digital Maritime Consultancy - A member of Team Aivenautics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.models.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.maritimeconnectivity.serviceregistry.utils.ServiceInstanceCsvUtil;
import org.grad.secomv2.core.models.AbstractEnvelope;
import org.grad.secomv2.core.models.ServiceInstanceObject;

import java.util.List;

/**
 * The EnvelopeUploadSearchResultObject according to MSR G1191 API
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
public class EnvelopeUploadSearchResultObject extends AbstractEnvelope {


    // Class Variables
    @NotNull
    @Valid
    private List<ServiceInstanceObject> serviceInstance;

    /**
     * Gets search service result.
     *
     * @return the search service result
     */
    public List<ServiceInstanceObject> getServiceInstance() {
        return serviceInstance;
    }

    /**
     * Sets search service result.
     *
     * @param serviceInstance the search service result
     */
    public void setServiceInstance(List<ServiceInstanceObject> serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    /**
     * This method should be implemented by all envelop objects to allow the
     * generation of the signature CSV attribute array
     *
     * @return the generated signature CSV attribute array
     */
    @Override
    public Object[] getAttributeArray() {
        return new Object[]{
                ServiceInstanceCsvUtil.toCanonicalBytes(serviceInstance),
                envelopeSignatureCertificate,
                envelopeRootCertificateThumbprint,
                envelopeSignatureTime
        };
    }

}
