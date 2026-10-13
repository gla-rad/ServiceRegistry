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

import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.springboot4.components.SecomConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import static net.maritimeconnectivity.serviceregistry.components.SecomV2TestCertificates.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * The SECOM v2 Certificate Provider Implementation Test.
 * <p/>
 * Tests the construction of the digital signature certificate and the
 * retrieval of the signing identity private key, using a PKCS12 key store
 * generated for each test.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@ExtendWith(MockitoExtension.class)
class SecomV2CertificateProviderImplTest {

    /**
     * The Tested Component.
     */
    private SecomV2CertificateProviderImpl secomV2CertificateProvider;

    /**
     * The SECOM v2 Trust Store Provider mock.
     */
    @Mock
    private SecomV2TrustStoreProviderImpl secomV2TrustStoreProvider;

    /**
     * The temporary directory holding the generated stores.
     */
    @TempDir
    Path tempDir;

    // Class Variables
    private SecomConfigProperties secomConfigProperties;
    private KeyPair signingKeyPair;
    private X509Certificate rootCertificate;
    private X509Certificate signingCertificate;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Generate the test PKI
        final KeyPair rootKeyPair = generateKeyPair();
        this.rootCertificate = createSelfSignedCertificate(rootKeyPair, "Test Root CA");
        this.signingKeyPair = generateKeyPair();
        this.signingCertificate = createCertificate(this.signingKeyPair, "Test Signing Identity", rootKeyPair.getPrivate(), "Test Root CA");

        // Write the signing identity key store
        final Path keystore = writeKeyStore(this.tempDir.resolve("keystore.p12"),
                this.signingKeyPair.getPrivate(), this.signingCertificate, this.rootCertificate);

        // Configure the SECOM properties
        this.secomConfigProperties = new SecomConfigProperties();
        this.secomConfigProperties.setKeystore(keystore.toString());
        this.secomConfigProperties.setKeystorePassword(STORE_PASSWORD);
        this.secomConfigProperties.setKeystoreType("PKCS12");

        // Create the tested component
        this.secomV2CertificateProvider = new SecomV2CertificateProviderImpl();
        this.secomV2CertificateProvider.secomConfigProperties = this.secomConfigProperties;
        ReflectionTestUtils.setField(this.secomV2CertificateProvider, "secomV2TrustStoreProviderImpl", this.secomV2TrustStoreProvider);
    }

    /**
     * Test that the digital signature certificate is constructed from the
     * signing identity certificate chain, along with the root certificate
     * provided by the trust store provider.
     */
    @Test
    void testGetDigitalSignatureCertificate() throws Exception {
        doReturn(this.rootCertificate).when(this.secomV2TrustStoreProvider).getRootCertificate();
        this.secomV2CertificateProvider.initialise();

        final DigitalSignatureCertificate result = this.secomV2CertificateProvider.getDigitalSignatureCertificate();

        assertNotNull(result);
        assertArrayEquals(new X509Certificate[]{this.signingCertificate, this.rootCertificate}, result.getCertificate());
        assertEquals(this.signingKeyPair.getPublic(), result.getPublicKey());
        assertEquals(this.rootCertificate, result.getRootCertificate());
    }

    /**
     * Test that a runtime exception is raised when the root certificate
     * cannot be retrieved from the trust store provider.
     */
    @Test
    void testGetDigitalSignatureCertificateRootFailure() throws Exception {
        doThrow(KeyStoreException.class).when(this.secomV2TrustStoreProvider).getRootCertificate();
        this.secomV2CertificateProvider.initialise();

        assertThrows(RuntimeException.class, () -> this.secomV2CertificateProvider.getDigitalSignatureCertificate());
    }

    /**
     * Test that an illegal state exception is raised when the key store
     * contains no certificate chain, i.e. only trusted certificate entries.
     */
    @Test
    void testGetDigitalSignatureCertificateNoChain() throws Exception {
        this.secomConfigProperties.setKeystore(writeTrustStore(this.tempDir.resolve("certonly.p12"),
                SIGNING_ALIAS, this.signingCertificate).toString());
        this.secomV2CertificateProvider.initialise();

        assertThrows(IllegalStateException.class, () -> this.secomV2CertificateProvider.getDigitalSignatureCertificate());
    }

    /**
     * Test that the private key of the signing identity is retrieved from
     * the key store.
     */
    @Test
    void testGetPrivateKey() {
        this.secomV2CertificateProvider.initialise();

        assertEquals(this.signingKeyPair.getPrivate(), this.secomV2CertificateProvider.getPrivateKey());
    }

    /**
     * Test that an illegal state exception is raised when the key store
     * entry does not contain a private key.
     */
    @Test
    void testGetPrivateKeyNoKeyEntry() throws Exception {
        this.secomConfigProperties.setKeystore(writeTrustStore(this.tempDir.resolve("certonly.p12"),
                SIGNING_ALIAS, this.signingCertificate).toString());
        this.secomV2CertificateProvider.initialise();

        assertThrows(IllegalStateException.class, () -> this.secomV2CertificateProvider.getPrivateKey());
    }

}
