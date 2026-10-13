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
import org.grad.secomv2.core.base.SecomCertificateProvider;
import org.grad.secomv2.core.utils.KeyStoreUtils;
import org.grad.secomv2.springboot4.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * The SECOM v2 Certificate Provider Implementation Component.
 * <p/>
 * This component implements the SECOM v2 certificate provider interface and
 * is responsible for constructing the digital signature certificate used to
 * sign the outgoing SECOM messages. The certificate chain is retrieved from
 * the signing identity, while the root certificate is retrieved from the
 * maritime identity trust store.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "secom.security.ssl", name="enabled")
public class SecomV2CertificateProviderImpl implements SecomCertificateProvider {

    /**
     * The SECOM SSL Configuration Properties
     */
    @Autowired
    SecomConfigProperties secomConfigProperties;

    /**
     * The SECOM v2 Trust Store Provider.
     */
    @Autowired
    private SecomV2TrustStoreProviderImpl secomV2TrustStoreProviderImpl;

    /**
     * The loaded keystore store.
     */
    private KeyStore keystore;

    /**
     * The SECOM v2 Certificate Provider Implementation constructor.
     */
    @PostConstruct
    public void initialise() {
        // Initialise the keystore
        try {
            // Load the key store from the specified path, password and type
            log.debug("Initializing SECOM v2.0 Key Store from path: {}", this.secomConfigProperties.getKeystore());
            this.keystore = KeyStoreUtils.getKeyStore(
                    this.secomConfigProperties.getKeystore(),
                    this.secomConfigProperties.getKeystorePassword(),
                    this.secomConfigProperties.getKeystoreType());
            log.debug("Loads KeyStore of size {}", this.keystore.size());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            log.error("SECOM v2.0 Trust Store count not be initialized.");
        }
    }

    /**
     * Constructs and returns the digital signature certificate to be used for
     * signing the SECOM messages. This includes the signing certificate chain,
     * the respective public key and the root certificate of the trust store.
     *
     * @return the digital signature certificate
     */
    @Override
    public DigitalSignatureCertificate getDigitalSignatureCertificate() {
        try {
            // Get the keystore certificate aliases and the chain
            final String alias = this.keystore.aliases().nextElement();
            final Certificate[] chain = this.keystore.getCertificateChain(alias);

            //Sanity check
            if (chain == null || chain.length == 0) {
                throw new IllegalStateException("No certificate chain found for alias '" + alias + "'");
            }

            // Populate the digital signature certificate object
            final X509Certificate[] x509Chain = Arrays.stream(chain).toArray(X509Certificate[]::new);
            DigitalSignatureCertificate digitalSignatureCertificate = new DigitalSignatureCertificate();
            digitalSignatureCertificate.setCertificate(x509Chain);
            digitalSignatureCertificate.setPublicKey(x509Chain[0].getPublicKey());
            digitalSignatureCertificate.setRootCertificate(this.secomV2TrustStoreProviderImpl.getRootCertificate());

            // And return
            return digitalSignatureCertificate;
        } catch (KeyStoreException e) {
            log.error("Unable to load root certificate from keystore: {}", e.getMessage());
            throw new RuntimeException("Unable to load root certificate from keystore: {}", e);
        }
    }

    /**
     * Returns the private key of the signing identity.
     *
     * @return the signing identity private key
     */
    public PrivateKey getPrivateKey() {
        try {
            final String alias = this.keystore.aliases().nextElement();
            Key key = this.keystore.getKey(alias, this.secomConfigProperties.getKeystorePassword().toCharArray());

            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("Key for alias '" + alias + "' is not a private key");
            }

            return privateKey;
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new IllegalStateException("Unable to load private key", e);
        }
    }

}
