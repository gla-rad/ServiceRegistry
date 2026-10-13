/*
 * Copyright (c) 2026 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.components;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomSignatureProvider;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.grad.secomv2.springboot4.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The SECOM v2 Signature Provider Implementation Component.
 * <p/>
 * This component implements the SECOM v2 signature provider interface and is
 * responsible for generating the digital signatures of the outgoing SECOM
 * messages, using the private key of the signing identity, as well as for
 * validating the signatures of the incoming ones.
 *
 * @author Lawrence Hughes (email: lawrence.hughes@gla-rad.org)
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "secom.security.ssl", name="enabled")
public class SecomV2SignatureProviderImpl implements SecomSignatureProvider {

    /**
     * The SECOM v2 Trust Store Provider.
     */
    @Autowired
    private SecomV2CertificateProviderImpl secomV2CertificateProvider;

    /**
     * Returns the digital signature algorithm to be used for generating the
     * SECOM message signatures.
     *
     * @return the digital signature algorithm
     */
    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA;
    }

    /**
     * Generates the digital signature of the provided payload, using the
     * private key of the signing identity.
     *
     * @param signatureCertificate  the digital signature certificate
     * @param payload               the payload to be signed
     * @return the generated signature, or null if the operation failed
     */
    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate,
                                    byte[] payload) {
        try {
            Signature sign = Signature.getInstance(this.getSignatureAlgorithm().getValue());
            sign.initSign(this.secomV2CertificateProvider.getPrivateKey());
            sign.update(payload);
            return sign.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
            log.error("Unable to generate signature", ex);
            return null;
        }
    }

    /**
     * Validates the provided signature against the provided content, using
     * the first of the provided PEM signature certificates. The verification
     * algorithm is determined by the algorithm used to sign that certificate.
     *
     * @param signatureCertificates the PEM encoded signature certificates
     * @param signature             the signature to be validated
     * @param content               the signed content
     * @return whether the signature is valid
     */
    @Override
    public boolean validateSignature(String[] signatureCertificates,
                                     byte[] signature,
                                     byte[] content) {
        final X509Certificate cert;
        try {
            cert = SecomPemUtils.getCertFromPem(signatureCertificates[0]);
        } catch (CertificateException e) {
            log.error("Parsing certificate failed", e);
            return false;
        }

        // Extract algorithm used to sign certificate
        String algorithm = cert.getSigAlgName();

        try {
            Signature sign = Signature.getInstance(algorithm);
            sign.initVerify(cert);
            sign.update(content);

            if (sign.verify(signature)) {
                return true;
            }
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
            log.error("Unable to validate signature", ex);
            return false;
        }
        return false;
    }

}
