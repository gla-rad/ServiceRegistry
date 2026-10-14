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

import org.grad.secomv2.springboot4.components.SecomConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static net.maritimeconnectivity.serviceregistry.components.SecomV2TestCertificates.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The SECOM v2 Trust Store Provider Implementation Test.
 * <p/>
 * Tests the loading of the maritime identity trust store and the retrieval
 * of the trusted root certificate, using a PKCS12 trust store generated for
 * each test.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@ExtendWith(MockitoExtension.class)
class SecomV2TrustStoreProviderImplTest {

    /**
     * The Tested Component.
     */
    private SecomV2TrustStoreProviderImpl secomV2TrustStoreProvider;

    /**
     * The temporary directory holding the generated stores.
     */
    @TempDir
    Path tempDir;

    // Class Variables
    private SecomConfigProperties secomConfigProperties;
    private X509Certificate rootCertificate;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Generate the root CA and its trust store
        final KeyPair rootKeyPair = generateKeyPair();
        this.rootCertificate = createSelfSignedCertificate(rootKeyPair, "Test Root CA");
        final Path truststore = writeTrustStore(this.tempDir.resolve("truststore.p12"), ROOT_ALIAS, this.rootCertificate);

        // Configure the SECOM properties
        this.secomConfigProperties = new SecomConfigProperties();
        this.secomConfigProperties.setTruststore(truststore.toString());
        this.secomConfigProperties.setTruststorePassword(STORE_PASSWORD);
        this.secomConfigProperties.setTruststoreType("PKCS12");
        this.secomConfigProperties.setRootCertAlias(ROOT_ALIAS);

        // Create the tested component
        this.secomV2TrustStoreProvider = new SecomV2TrustStoreProviderImpl();
        this.secomV2TrustStoreProvider.secomConfigProperties = this.secomConfigProperties;
    }

    /**
     * Test that the trust store gets loaded from the configured path upon
     * the component initialisation.
     */
    @Test
    void testInitialise() throws Exception {
        this.secomV2TrustStoreProvider.initialise();

        final KeyStore trustStore = this.secomV2TrustStoreProvider.getTrustStore();
        assertNotNull(trustStore);
        assertEquals(1, trustStore.size());
        assertTrue(trustStore.isCertificateEntry(ROOT_ALIAS));
    }

    /**
     * Test that if the trust store cannot be read (i.e. wrong password), the
     * initialisation does not fail, but the trust store remains unset.
     */
    @Test
    void testInitialiseWrongPassword() {
        this.secomConfigProperties.setTruststorePassword("wrong");

        assertDoesNotThrow(() -> this.secomV2TrustStoreProvider.initialise());
        assertNull(this.secomV2TrustStoreProvider.getTrustStore());
    }

    /**
     * Test that if the configured trust store does not exist neither in the
     * filesystem nor the classpath, the SECOMLib key store utilities will
     * initialise an empty trust store.
     */
    @Test
    void testInitialiseMissingFile() throws Exception {
        this.secomConfigProperties.setTruststore(this.tempDir.resolve("missing.p12").toString());

        this.secomV2TrustStoreProvider.initialise();

        assertNotNull(this.secomV2TrustStoreProvider.getTrustStore());
        assertEquals(0, this.secomV2TrustStoreProvider.getTrustStore().size());
    }

    /**
     * Test that the CA root certificate alias is the configured one.
     */
    @Test
    void testGetCARootCertificateAlias() {
        assertEquals(ROOT_ALIAS, this.secomV2TrustStoreProvider.getCARootCertificateAlias());
    }

    /**
     * Test that the root certificate can be retrieved from the trust store
     * using the configured alias.
     */
    @Test
    void testGetRootCertificate() throws Exception {
        this.secomV2TrustStoreProvider.initialise();

        assertEquals(this.rootCertificate, this.secomV2TrustStoreProvider.getRootCertificate());
    }

    /**
     * Test that no root certificate is returned when the configured alias
     * does not exist in the trust store.
     */
    @Test
    void testGetRootCertificateUnknownAlias() throws Exception {
        this.secomConfigProperties.setRootCertAlias("unknown");
        this.secomV2TrustStoreProvider.initialise();

        assertNull(this.secomV2TrustStoreProvider.getRootCertificate());
    }

}
