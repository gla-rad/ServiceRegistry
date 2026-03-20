/*
 * Copyright (c) 2025 GLA Research and Development Directorate
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
package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomSignatureProvider;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.cert.CertificateException;


/**
 * The SECOM v2.0 Signature Provider Implementation.
 *
 * In the current e-Navigation Service Architecture, it's the cKeeper
 * microservice that is responsible for generating the validating the
 * SECOM v2.0 message signatures.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@Slf4j
public class SecomV2SignatureProviderImpl implements SecomSignatureProvider {


    // Class Variables
    PrivateKey privateKey;
    private static final String ANS10_MRN_OBJECT_IDENTIFIER = "0.9.2342.19200300.100.1.1";

    /**
     * Returns the digital signature algorithm for the signature provider.
     * In SECOM, by default this should be DSA, but ECDSA should be used
     * to generate smaller signatures.
     *
     * @return the digital signature algorithm for the signature provider
     */
    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA;
    }

    /**
     * This function overrides the interface definition to link the SECOM
     * signature provision with the cKeeper operation. A service can request
     * cKeeper to sign a payload, using a valid certificate based on the
     * provided digital signature certificate information.
     *
     * @param signatureCertificate  The digital signature certificate to be used for the signature generation
     * @param algorithm             The algorithm to be used for the signature generation
     * @param payload               The payload to be signed, (preferably Base64 encoded)
     * @return The signature generated
     */
    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] payload) {
        // Create a new signature to sign the provided content
        try {
            Signature sign = Signature.getInstance(algorithm.getValue());
            sign.initSign(this.privateKey);
            sign.update(payload);

            // Sign and return the signature
            return sign.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    /**
     * The signature validation operation. This should support the provision
     * of the message content (expected in a Base64 format) and the signature
     * to validate the content against.
     *
     * @param signatureCertificates The digital signature certificates to be used for the signature generation
     * @param algorithm             The algorithm used for the signature generation
     * @param content               The context (in Base64 format) to be validated
     * @param signature             The signature to validate the context against
     * @return whether the signature validation was successful or not
     */
    @Override
    public boolean validateSignature(String[] signatureCertificates, DigitalSignatureAlgorithmEnum algorithm, byte[] signature, byte[] content) {
        // Create a new signature to sign the provided content
        for (String signatureCertificate : signatureCertificates) {
            try {
                Signature sign = Signature.getInstance(algorithm.getValue());
                sign.initVerify(SecomPemUtils.getCertFromPem(signatureCertificate));
                sign.update(content);

                // Sign and return the signature
                boolean valid = sign.verify(signature);
                if (valid) {
                    return true;
                }
            } catch (NoSuchAlgorithmException | CertificateException | SignatureException | InvalidKeyException ex) {
                log.error(ex.getMessage());
                return false;
            }
        }
        return false;
    }

}
