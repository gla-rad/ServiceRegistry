package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
@Slf4j
@ConditionalOnProperty(name = "secom.security.signingIdentity.enabled", havingValue = "true")
public class SecomV2SigningIdentityProvider {

    @Value("${secom.security.signingIdentity.enabled}")
    private boolean maritimeIdentityEnabled;

    @Value("${secom.security.signingIdentity.signingIdentityPath}")
    private String signingIdentityPath;

    @Value("${secom.security.signingIdentity.signingIdentityPassword}")
    private String signingIdentityPassword;

    private KeyStore signingIdentityKeyStore;

    public KeyStore getSigningIdentity() {
        if (signingIdentityKeyStore == null) {
            try {
                loadSigningIdentity();
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
                log.error("Error while initializing key store", e);
                throw new IllegalStateException("Unable to load signing identity keystore", e);
            }
        }
        return this.signingIdentityKeyStore;
    }

    private void loadSigningIdentity() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        if (maritimeIdentityEnabled) {
            log.debug("Loading signing identity from {}", signingIdentityPath);
            KeyStore ks = KeyStore.getInstance("PKCS12");

            try (InputStream is = new FileInputStream(signingIdentityPath)) {
                ks.load(is, signingIdentityPassword.toCharArray());
            }

            this.signingIdentityKeyStore = ks;
            log.debug("Loaded signing identity of size {}", this.signingIdentityKeyStore.size());
        } else {
            log.warn("Signing Identity is disabled.");
        }
    }

    public String getSigningAlias() {
        try {
            KeyStore ks = getSigningIdentity();
            var aliases = ks.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    return alias;
                }
            }
            throw new IllegalStateException("No key entry found in signing identity keystore");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to resolve signing alias", e);
        }
    }

    public X509Certificate getSigningCertificate() {
        try {
            KeyStore ks = getSigningIdentity();
            String alias = getSigningAlias();
            return (X509Certificate) ks.getCertificate(alias);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load signing certificate", e);
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            KeyStore ks = getSigningIdentity();
            String alias = getSigningAlias();
            Key key = ks.getKey(alias, signingIdentityPassword.toCharArray());

            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("Key for alias '" + alias + "' is not a private key");
            }

            return privateKey;
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new IllegalStateException("Unable to load private key", e);
        }
    }
}