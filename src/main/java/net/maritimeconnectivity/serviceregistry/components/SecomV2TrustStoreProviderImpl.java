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
import org.grad.secomv2.core.base.SecomTrustStoreProvider;
import org.grad.secomv2.core.utils.KeyStoreUtils;
import org.grad.secomv2.springboot4.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The SECOM v2 Trust Store Provider Implementation Component.
 * <p/>
 * This component implements the SECOM v2 trust store provider interface and
 * is responsible for loading the maritime identity trust store, as well as
 * providing access to the trusted root certificate.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "secom.security.ssl", name="enabled")
public class SecomV2TrustStoreProviderImpl implements SecomTrustStoreProvider {

    /**
     * The SECOM SSL Configuration Properties
     */
    @Autowired
    SecomConfigProperties secomConfigProperties;

    /**
     * The loaded trust store.
     */
    private KeyStore trustStore;

    /**
     * Instantiates a new Secom v 2 trust store provider.
     */
    @PostConstruct
    public void initialise() {
        try {
            // Load the trust store from the specified path, password and type
            log.debug("Initializing SECOM v2.0 Trust Store from path: {}", this.secomConfigProperties.getTruststore());
            this.trustStore = KeyStoreUtils.getKeyStore(
                    this.secomConfigProperties.getTruststore(),
                    this.secomConfigProperties.getTruststorePassword(),
                    this.secomConfigProperties.getTruststoreType());
            log.debug("Loads KeyStore of size {}", this.trustStore.size());
            log.debug("Root certificate alias: {}", this.getCARootCertificateAlias());
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            log.error("Error while initializing key store", e);
        }
    }

    /**
     * Returns the alias of the CA root certificate in the trust store.
     *
     * @return the CA root certificate alias
     */
    @Override
    public String getCARootCertificateAlias() {
        return this.secomConfigProperties.getRootCertAlias();
    }

    /**
     * Returns the trust store. If this has not been loaded yet, it will be
     * loaded upon the first request.
     *
     * @return the trust store
     */
    @Override
    public KeyStore getTrustStore() {
        return this.trustStore;
    }

    /**
     * Returns the X.509 root certificate from the trust store, based on the
     * configured root certificate alias.
     *
     * @return the root certificate
     * @throws KeyStoreException if the trust store has not been initialized
     */
    public X509Certificate getRootCertificate() throws KeyStoreException {
        KeyStore trustStore = this.getTrustStore();
        Certificate root = trustStore.getCertificate(this.getCARootCertificateAlias());
        return (X509Certificate) root;
    }

}
