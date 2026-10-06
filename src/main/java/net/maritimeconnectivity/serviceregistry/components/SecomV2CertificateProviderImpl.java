package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomCertificateProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

@Component
@Slf4j
@ConditionalOnProperty(name = "secom.security.signingIdentity.enabled", havingValue = "true")
public class SecomV2CertificateProviderImpl implements SecomCertificateProvider {

    private final SecomV2SigningIdentityProvider signingIdentityProvider;
    private final SecomV2TrustStoreProviderImpl secomV2TrustStoreProviderImpl;

    public SecomV2CertificateProviderImpl(SecomV2SigningIdentityProvider signingIdentityProvider, SecomV2TrustStoreProviderImpl secomV2TrustStoreProviderImpl) {
        this.signingIdentityProvider = signingIdentityProvider;
        this.secomV2TrustStoreProviderImpl = secomV2TrustStoreProviderImpl;
    }

    @Override
    public DigitalSignatureCertificate getDigitalSignatureCertificate() {
        X509Certificate[] chain = signingIdentityProvider.getSigningCertificateChain();

        DigitalSignatureCertificate digitalSignatureCertificate = new DigitalSignatureCertificate();
        digitalSignatureCertificate.setCertificate(chain);
        digitalSignatureCertificate.setPublicKey(chain[0].getPublicKey());
        try {
            digitalSignatureCertificate.setRootCertificate(secomV2TrustStoreProviderImpl.getRootCertificate());
        } catch (KeyStoreException e) {
            log.error("Unable to load root certificate from keystore: {}", e.getMessage());
            throw new RuntimeException("Unable to load root certificate from keystore: {}", e);
        }
        return digitalSignatureCertificate;
    }
}
