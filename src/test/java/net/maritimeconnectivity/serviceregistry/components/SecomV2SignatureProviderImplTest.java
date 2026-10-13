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

import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static net.maritimeconnectivity.serviceregistry.components.SecomV2TestCertificates.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

/**
 * The SECOM v2 Signature Provider Implementation Test.
 * <p/>
 * Tests the generation and validation of the SECOM v2 digital signatures,
 * using a signing identity issued by a test root CA.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@ExtendWith(MockitoExtension.class)
class SecomV2SignatureProviderImplTest {

    /**
     * The Tested Component.
     */
    @InjectMocks
    private SecomV2SignatureProviderImpl secomV2SignatureProvider;

    /**
     * The SECOM v2 Certificate Provider mock.
     */
    @Mock
    private SecomV2CertificateProviderImpl secomV2CertificateProvider;

    // Class Variables
    private KeyPair signingKeyPair;
    private String signingCertificatePem;
    private String otherCertificatePem;
    private byte[] payload;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Generate the test PKI
        final KeyPair rootKeyPair = generateKeyPair();
        this.signingKeyPair = generateKeyPair();
        this.signingCertificatePem = SecomPemUtils.getMinifiedPemFromCert(
                createCertificate(this.signingKeyPair, "Test Signing Identity", rootKeyPair.getPrivate(), "Test Root CA"));

        // Generate an unrelated certificate to verify against
        final KeyPair otherKeyPair = generateKeyPair();
        this.otherCertificatePem = SecomPemUtils.getMinifiedPemFromCert(
                createCertificate(otherKeyPair, "Test Other Identity", rootKeyPair.getPrivate(), "Test Root CA"));

        // Create the payload to be signed
        this.payload = "hello secom".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Test that the signature algorithm is SHA384 with ECDSA.
     */
    @Test
    void testGetSignatureAlgorithm() {
        assertEquals(DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, this.secomV2SignatureProvider.getSignatureAlgorithm());
    }

    /**
     * Test that a generated signature passes the validation against the
     * signing identity certificate.
     */
    @Test
    void testGenerateAndValidateSignature() {
        doReturn(this.signingKeyPair.getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();

        final byte[] signature = this.secomV2SignatureProvider.generateSignature(null, this.payload);

        assertNotNull(signature);
        assertTrue(this.secomV2SignatureProvider.validateSignature(new String[]{this.signingCertificatePem}, signature, this.payload));
    }

    /**
     * Test that the operations including the signature algorithm delegate
     * to the SECOM v2 ones and produce a valid signature.
     */
    @Test
    void testGenerateAndValidateSignatureWithAlgorithm() {
        doReturn(this.signingKeyPair.getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();
        final DigitalSignatureAlgorithmEnum algorithm = this.secomV2SignatureProvider.getSignatureAlgorithm();

        final byte[] signature = this.secomV2SignatureProvider.generateSignature(null, algorithm, this.payload);

        assertNotNull(signature);
        assertTrue(this.secomV2SignatureProvider.validateSignature(new String[]{this.signingCertificatePem}, algorithm, signature, this.payload));
    }

    /**
     * Test that no signature is generated when the signing identity private
     * key is not compatible with the signature algorithm.
     */
    @Test
    void testGenerateSignatureInvalidKey() throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        doReturn(kpg.generateKeyPair().getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();

        assertNull(this.secomV2SignatureProvider.generateSignature(null, this.payload));
    }

    /**
     * Test that a tampered signature fails the validation.
     */
    @Test
    void testValidateSignatureAlteredSignature() {
        doReturn(this.signingKeyPair.getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();

        final byte[] signature = this.secomV2SignatureProvider.generateSignature(null, this.payload);
        signature[signature.length - 1] ^= 0x01;

        assertFalse(this.secomV2SignatureProvider.validateSignature(new String[]{this.signingCertificatePem}, signature, this.payload));
    }

    /**
     * Test that a valid signature fails the validation against tampered
     * content.
     */
    @Test
    void testValidateSignatureAlteredContent() {
        doReturn(this.signingKeyPair.getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();

        final byte[] signature = this.secomV2SignatureProvider.generateSignature(null, this.payload);

        assertFalse(this.secomV2SignatureProvider.validateSignature(new String[]{this.signingCertificatePem}, signature,
                "hello secom!".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Test that a valid signature fails the validation against a different
     * certificate than the signing one.
     */
    @Test
    void testValidateSignatureWrongCertificate() {
        doReturn(this.signingKeyPair.getPrivate()).when(this.secomV2CertificateProvider).getPrivateKey();

        final byte[] signature = this.secomV2SignatureProvider.generateSignature(null, this.payload);

        assertFalse(this.secomV2SignatureProvider.validateSignature(new String[]{this.otherCertificatePem}, signature, this.payload));
    }

    /**
     * Test that the validation fails when the provided certificate cannot be
     * parsed.
     */
    @Test
    void testValidateSignatureInvalidCertificate() {
        assertFalse(this.secomV2SignatureProvider.validateSignature(new String[]{"invalid"}, new byte[]{0x01}, this.payload));
    }

}
