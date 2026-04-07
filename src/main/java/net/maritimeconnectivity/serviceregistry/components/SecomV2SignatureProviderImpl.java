package net.maritimeconnectivity.serviceregistry.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomSignatureProvider;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;

@Component
@Slf4j
@ConditionalOnProperty(name = "secom.security.signingIdentity.enabled", havingValue = "true")
public class SecomV2SignatureProviderImpl implements SecomSignatureProvider {

    private final SecomV2SigningIdentityProvider signingIdentityProvider;

    public SecomV2SignatureProviderImpl(SecomV2SigningIdentityProvider signingIdentityProvider) {
        this.signingIdentityProvider = signingIdentityProvider;
    }

    public PrivateKey getPrivateKey() {
        return signingIdentityProvider.getPrivateKey();
    }

    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA;
    }

    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate,
                                    DigitalSignatureAlgorithmEnum algorithm,
                                    byte[] payload) {
        try {
            Signature sign = Signature.getInstance(algorithm.getValue());
            sign.initSign(getPrivateKey());
            sign.update(payload);
            return sign.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
            log.error("Unable to generate signature", ex);
            return null;
        }
    }

    @Override
    public boolean validateSignature(String[] signatureCertificates,
                                     DigitalSignatureAlgorithmEnum algorithm,
                                     byte[] signature,
                                     byte[] content) {

        for (String signatureCertificate : signatureCertificates) {
            try {
                Signature sign = Signature.getInstance(algorithm.getValue());
                sign.initVerify(SecomPemUtils.getCertFromPem(signatureCertificate));
                sign.update(content);

                if (sign.verify(signature)) {
                    return true;
                }
            } catch (NoSuchAlgorithmException | CertificateException | SignatureException | InvalidKeyException ex) {
                log.error("Unable to validate signature", ex);
                return false;
            }
        }
        return false;
    }
}