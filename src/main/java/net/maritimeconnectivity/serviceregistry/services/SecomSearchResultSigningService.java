package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SignatureProviderImpl;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SigningIdentityProvider;
import net.maritimeconnectivity.serviceregistry.components.SecomV2TrustStoreProviderImpl;
import org.grad.secomv2.core.models.EnvelopeSearchResultObject;
import org.grad.secomv2.core.models.SearchResult;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Service implementation for signing SECOM search results obtained from local and global search
 * The payload to be signed is assembled as described in IEC 63173-2 CLause 7.3.4 Creation of
 * envelope signature
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class SecomSearchResultSigningService {

    private final SecomV2SignatureProviderImpl secomV2SignatureProvider;
    private final SecomV2TrustStoreProviderImpl secomV2TrustStoreProvider;
    private final SecomV2SigningIdentityProvider secomV2SigningIdentityProvider;

    private final HexFormat hexFormatter = HexFormat.of();


    public SecomSearchResultSigningService(SecomV2SignatureProviderImpl signatureProvider,
                                           SecomV2TrustStoreProviderImpl trustStoreProvider,
                                           SecomV2SigningIdentityProvider signingIdentityProvider) {
        this.secomV2SignatureProvider = signatureProvider;
        this.secomV2TrustStoreProvider = trustStoreProvider;
        this.secomV2SigningIdentityProvider = signingIdentityProvider;
    }


    public SearchResult signSearchResult(EnvelopeSearchResultObject envelope) {

        envelope.setEnvelopeSignatureCertificate(getSigningCertificateArray());
        envelope.setEnvelopeRootCertificateThumbprint(this.getRootThumbprint());
        envelope.setEnvelopeSignatureTime(Instant.now());

        byte[] payload = envelope.getCsvString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] signature = secomV2SignatureProvider.generateSignature(
                null,
                secomV2SignatureProvider.getSignatureAlgorithm(),
                payload
        );

        SearchResult searchResult = new SearchResult();
        searchResult.setEnvelope(envelope);
        if (signature == null) {
            throw new IllegalStateException("Failed to generate envelope signature");
        }

        searchResult.setEnvelopeSignature(this.hexFormatter.formatHex(signature));

        //check validation of signature
        boolean valid = secomV2SignatureProvider.validateSignature(
                envelope.getEnvelopeSignatureCertificate(),
                secomV2SignatureProvider.getSignatureAlgorithm(),
                signature,
                payload
        );
        log.debug("Signature valid: {}", valid);


        return searchResult;
    }


    private String getRootThumbprint() {
        try {
            KeyStore trustStore = this.secomV2TrustStoreProvider.getTrustStore();
            String alias = secomV2TrustStoreProvider.getCARootCertificateAlias();

            X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);
            if (cert == null) {
                throw new IllegalStateException("No root certificate found for alias " + alias);
            }

            byte[] der = cert.getEncoded();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(der);

            return this.hexFormatter.formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute root certificate thumbprint", e);
        }
    }

    private String[] getSigningCertificateArray() {
        try {
            X509Certificate cert = secomV2SigningIdentityProvider.getSigningCertificate();
            return new String[] {
                    org.grad.secomv2.core.utils.SecomPemUtils.getMinifiedPemFromCert(cert)
            };
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build signing certificate array", e);
        }
    }


}
