package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SignatureProviderImpl;
import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.models.SearchResult;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.springframework.stereotype.Service;

/**
 * Service implementation for signing search results obtained from local and global search
 * Checks conducted are described in IALA G1191
 * Note - that invalid signatures have already been rejected by the middleware, so this service
 * focuses on signing the results to the client.
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class ResultSigningService {

    public ResultSigningService(SecomV2SignatureProviderImpl signatureProvider) {}


    public SearchResult signSearchResult(SearchResult searchResult, DigitalSignatureCertificate signatureCertificate, DigitalSignatureAlgorithmEnum algorithm) {
        //TODO implement signing of search results according to SECOM. Use the
        // SECOMV2SignatureProiverImpl

        return searchResult;
    }

}
