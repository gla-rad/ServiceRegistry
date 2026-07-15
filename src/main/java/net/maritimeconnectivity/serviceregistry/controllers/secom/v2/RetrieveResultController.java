package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.utils.CertificateParsingUtil;
import org.grad.secomv2.core.exceptions.SecomNotFoundException;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.RetrieveResultServiceInterface;
import org.grad.secomv2.core.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The SECOM Retrieve Results Controller.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@Validated
@Slf4j
public class RetrieveResultController implements RetrieveResultServiceInterface {

    /**
     * The Search Consolidation Service.
     */
    @Autowired
    SearchConsolidationService searchConsolidationService;

    /**
     * The Certificate Parting Utility
     */
    @Autowired
    CertificateParsingUtil certificateParsingUtil;

    @Tag(name = "SECOM")
    @Transactional
    public ResponseEntity<SearchResult> retrieveResult(@Valid @RequestBody RetrieveResultObject retrieveResultObject) {

        EnvelopeRetrieveResultObject envelopeSearchResultObject = retrieveResultObject.getEnvelope();

        // Get the request MRN information
        final String consumerMrn = certificateParsingUtil.getMrnFromCertificate(
                envelopeSearchResultObject.getEnvelopeSignatureCertificate());

        // Get the transaction UUID
        final String transactionIdBody = envelopeSearchResultObject.getTransactionId();
        final UUID transactionUUID;
        try{
            transactionUUID = UUID.fromString(transactionIdBody);
        } catch (Exception ex) {
            throw new SecomValidationException(ex.getMessage());
        }

        List<ServiceInstanceObject> services =
                searchConsolidationService.getResults(transactionUUID.toString(), consumerMrn);

        if (services == null) {
            log.debug("User tried to retrieve results for unknown transaction {}", transactionUUID);
            throw new SecomNotFoundException("Transaction not found: " + transactionUUID);
        } else if (services.isEmpty()) {
            log.debug("User tried to retrieve results but no results exists for transaction {}", transactionUUID);
        }

        // Build the response envelope
        EnvelopeSearchResultObject envelope = new EnvelopeSearchResultObject();
        envelope.setServiceInstance(services);
        envelope.setTransactionId(transactionUUID);

        // Build the search result response
        SearchResult searchResult = new SearchResult();
        searchResult.setEnvelope(envelope);

        // And return
        return ResponseEntity.ok(searchResult);
    }

}




