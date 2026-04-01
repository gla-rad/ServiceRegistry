package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomTrustStoreProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyStore;

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



    @Override
    public String getCARootCertificateAlias() {
        return "";
    }

    @Override
    public KeyStore getTrustStore() {
        return null;
    }

}
