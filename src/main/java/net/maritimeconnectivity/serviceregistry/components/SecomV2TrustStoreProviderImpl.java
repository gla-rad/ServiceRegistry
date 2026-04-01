package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.SecomTrustStoreProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Component
@Slf4j
public class SecomV2TrustStoreProviderImpl implements SecomTrustStoreProvider {

    @Value("${secom.security.maritimeIdentity.enabled}")
    private boolean maritimeIdentityEnabled;

    @Value("${secom.security.maritimeIdentity.rootCertAlias}")
    private String rootCertAlias;

    @Value("${secom.security.maritimeIdentity.trustStorePath}")
    private String trustStorePath;

    @Value("${secom.security.maritimeIdentity.trustStorePassword}")
    private String trustStorePassword;

    private KeyStore keyStore;


    @Override
    public String getCARootCertificateAlias() {
        return this.rootCertAlias;
    }

    @Override
    public KeyStore getTrustStore() {
        if (keyStore == null) {
            try {
                initializeKeyStore();
            } catch (KeyStoreException | CertificateException | IOException |
                     NoSuchAlgorithmException e) {
                log.error("Error while initializing key store", e);
            }
        }
        return this.keyStore;
    }


    private void initializeKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        if (maritimeIdentityEnabled) {
            // Load the trust store from the specified path and password
            log.debug("Initializing SECOM v2.0 Trust Store from path: {}", trustStorePath);
            KeyStore ks = KeyStore.getInstance("PKCS12");

            //Auto closes fd as FileInputStream implements AutoCloseable
            try (InputStream is = new FileInputStream(trustStorePath)) {
                ks.load(is, trustStorePassword.toCharArray());
            }
            this.keyStore = ks;
            log.debug("Loads KeyStore of size {}", this.keyStore.size());
            log.debug("Root certificate alias: {}", this.rootCertAlias);


        } else {
            log.warn("Maritime Identity is disabled. SECOM v2.0 Trust Store will not be initialized.");
        }
    }
}
