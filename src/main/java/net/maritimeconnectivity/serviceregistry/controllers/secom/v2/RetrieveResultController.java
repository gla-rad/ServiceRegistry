package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.exceptions.SecomNotFoundException;
import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.RetrieveResultServiceInterface;
import org.grad.secomv2.core.models.*;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
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
     * POST /v2/retrieveResult : The purpose of this interface is pull results of a
     * search transaction for which more results may arrive asynchronously. The search
     * transaction is identified by the transactionId field in the response to the initial
     * searchService request.
     *
     * @param retrieveResultObject The search filter object
     * @return the result object
     */
    @Tag(name = "SECOM")
    @Transactional
    public ResponseEntity<SearchResult> retrieveResult(@Valid @RequestBody RetrieveResultObject retrieveResultObject) {

        // Get the envelope of the retrieve results object
        final EnvelopeRetrieveResultObject envelopeSearchResultObject = retrieveResultObject.getEnvelope();

        // Get the request MRN and Transaction UUID
        final String consumerMrn = this.getRetrieveResultsEnvelopeMrn(envelopeSearchResultObject);
        final String transactionIdBody = this.getRetrieveResultsEnvelopeTransactionID(envelopeSearchResultObject);

        // Parse the transaction UUID
        final UUID transactionUUID;
        try{
            transactionUUID = UUID.fromString(transactionIdBody);
        } catch (Exception ex) {
            throw new SecomValidationException(ex.getMessage());
        }

        // Now try retrieving the results from the consolidation service
        final List<ServiceInstanceObject> services = this.searchConsolidationService
                .getResults(transactionUUID.toString(), consumerMrn);

        // Handle no results yet
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

    /**
     * A helper function that returns the retrieve results envelope MRN if
     * available.
     *
     * @param envelopeRetrieveResultObject the incoming retrieve results envelope object
     * @return the transaction MRN
     */
    protected String getRetrieveResultsEnvelopeMrn(EnvelopeRetrieveResultObject envelopeRetrieveResultObject) {
        return SecomPemUtils.getMrnFromEnvelope(envelopeRetrieveResultObject);
    }

    /**
     * A helper function that returns the retrieve results envelope transaction
     * ID, if available.
     *
     * @param envelopeRetrieveResultObject the incoming retrieve results envelope object
     * @return the transaction ID
     */
    protected String getRetrieveResultsEnvelopeTransactionID(EnvelopeRetrieveResultObject envelopeRetrieveResultObject) {
        return Optional.ofNullable(envelopeRetrieveResultObject)
                .map(EnvelopeRetrieveResultObject::getTransactionId)
                .orElse(null);
    }

}




