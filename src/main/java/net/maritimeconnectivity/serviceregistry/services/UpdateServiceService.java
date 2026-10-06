package net.maritimeconnectivity.serviceregistry.services;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.pki.CertificateHandler;
import net.maritimeconnectivity.pki.OCSPVerifier;
import net.maritimeconnectivity.pki.ocsp.OCSPValidationException;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.GeometryParseException;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import org.springframework.stereotype.Service;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Service Implementation G1191 defined updateService interface.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */

@Service
@Slf4j
public class UpdateServiceService {

    private final InstanceService instanceService;

    public UpdateServiceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public void updateService(Long id, UpdateServiceDto dto) {
        // Certificate validation

        if (dto == null || dto.getCertificates() == null || dto.getCertificates().isEmpty()) {
            throw new InvalidRequestException("Certificate chain is required");

        }

        log.warn(dto.getCertificates().toString());

        List<X509Certificate> chain = parseChain(dto.getCertificates());

        if (chain.size() < 2) {
            throw new InvalidRequestException("Certificate chain must include leaf + intermediate CA certificate");
        }

        X509Certificate leaf = chain.get(0);
        X509Certificate issuer = chain.get(1);

        validateCertificate(leaf, issuer);

        // TODO: validate endpoint URI
        // TODO: apiDoc reachable and returns 200
        // TODO: statusEndpoint reachable and returns 200 + valid timestamp
        // TODO: persist/update through instanceService
        try {
            instanceService.updateInstanceFromDto(id, dto);
        } catch (net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException e) {
            throw new DataNotFoundException("Instance not found", e);
        } catch (
                XMLValidationException |
                 GeometryParseException |
                 org.locationtech.jts.io.ParseException e) {
            // 400 – invalid update payload
            throw new InvalidRequestException("Invalid update request payload", e);
        }
    }


    private List<X509Certificate> parseChain(List<String> pemCerts) {
        List<String> nonBlank = pemCerts.stream()
                .filter(s -> s != null && !s.isBlank())
                .toList();

        if (nonBlank.isEmpty()) {
            throw new InvalidRequestException("Certificate chain is required");
        }

        List<X509Certificate> chain = nonBlank.stream()
                .map(CertificateHandler::getCertFromPem)
                .toList();

        if (chain.stream().anyMatch(c -> c == null)) {
            throw new InvalidRequestException("One or more certificates in the provided chain are invalid");
        }

        return chain;
    }


    private void validateCertificate(X509Certificate leaf, X509Certificate issuer) {
        if (leaf == null || issuer == null) {
            throw new InvalidRequestException("Certificate chain contains invalid/empty certificate(s)");
        }
        try {
            leaf.checkValidity();
            issuer.checkValidity();
            OCSPVerifier.verifyCertificateOCSP(leaf, issuer);
        } catch (CertificateNotYetValidException | CertificateExpiredException e) {
            throw new InvalidRequestException("Certificate is not valid at the current time", e);
        } catch (OCSPValidationException e) {
            throw new InvalidRequestException("OCSP validation failed", e);
        }
    }
    }


