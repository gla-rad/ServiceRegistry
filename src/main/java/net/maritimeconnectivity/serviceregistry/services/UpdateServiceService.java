package net.maritimeconnectivity.serviceregistry.services;


import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.pki.CertificateHandler;
import net.maritimeconnectivity.pki.OCSPVerifier;
import net.maritimeconnectivity.pki.PKIConfiguration;
import net.maritimeconnectivity.pki.RevocationInfo;
import net.maritimeconnectivity.pki.ocsp.OCSPValidationException;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service Implementation G1191 defined updateService interface.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class UpdateServiceService {

    //Constructor
    @Autowired
    public UpdateServiceService(InstanceService instanceService) {

    }

    public void updateService(Long id, UpdateServiceDto dto) throws KeyStoreException, OCSPValidationException, CertificateNotYetValidException, CertificateExpiredException {

        //Certificate validation
        List<X509Certificate> chain = parseChain(dto.getCertificates());

        if (chain.size() < 2) {
            throw new NoSuchElementException("Certificate chain contains no certificate for intermediate CA");
        }
        X509Certificate ownCert = chain.get(0);
        X509Certificate intermediateCert = chain.get(1);
        validateCertificate(ownCert, intermediateCert);

        //Endpoint must be valid URI
        //TODO

        //apiDoc must be available and respond with an HTTP status code 200
        //TODO

        //statusEndpoint must be available and respond with an HTTP status code 200 and include a valid timestamp
        //in the response.
        //TODO

    }

    private List<X509Certificate> parseChain(List<String> pemCerts) {
        try {
            return pemCerts.stream()
                    .map(CertificateHandler::getCertFromPem)
                    .toList();
        } catch (RuntimeException e) {
            throw new InvalidRequestException("One or more certificates in the provided chain are invalid", e);
        }
    }

    private void validateCertificate(X509Certificate certificate, X509Certificate intermediateCert) throws OCSPValidationException, CertificateNotYetValidException, CertificateExpiredException {

        //Check expiry
        certificate.checkValidity();
        intermediateCert.checkValidity();

        //OCSP part
        RevocationInfo revInfo = OCSPVerifier.verifyCertificateOCSP(certificate, intermediateCert);

    }

}

