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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.grad.secomv2.core.base.EnvelopeSignatureBearer;


/**
 * The UploadSearchResultObject according to MSR G1191 API
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
public class UploadSearchResultObject implements EnvelopeSignatureBearer {

    @NotNull
    private EnvelopeUploadSearchResultObject envelope;
    @NotNull
    @Schema(description = "The signature ot the EnvelopeUploadSearchResultObject in HEX format without whitespace or linebreaks")
    private String envelopeSignature;

    /**
     * Get the envelope
     * @return envelope
     */
    @Override
    public EnvelopeUploadSearchResultObject getEnvelope() {return envelope;}

    /**
     * Sets the envelope
     *
     * @param envelope the envelope search result object
     */
    public void setEnvelope(EnvelopeUploadSearchResultObject envelope) {this.envelope = envelope;}

    /**
     * Gets the envelope signature
     *
     * @return envelopeSignature
     */
    @Override
    public String getEnvelopeSignature() {return envelopeSignature;}

    /**
     * Sets the envelope signature
     *
     * @param envelopeSignature the envelope signature array
     */
    @Override
    public void setEnvelopeSignature(String envelopeSignature) {this.envelopeSignature = envelopeSignature;}


}
