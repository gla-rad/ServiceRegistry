package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.EnvelopeSignatureBearer;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.exceptions.SecomNotFoundException;
import org.grad.secomv2.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Path("/")
@Slf4j
@Validated
public class RetrieveResultController implements GenericSecomInterface {

    /**
     * The Interface Endpoint Path.
     */
    static final String RETREIVE_RESULTS_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/retrieveResult";

    @Autowired
    SearchConsolidationService searchConsolidationService;

    @Path(RETREIVE_RESULTS_INTERFACE_PATH)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResult retrieveResult(@Valid RetrieveResultObject retrieveResultObject) {

        EnvelopeRetrieveResultObject envelopeSearchResultObject = retrieveResultObject.getEnvelope();


        String transactionId = envelopeSearchResultObject.getTransactionId();

        log.debug("Retrieved valid retrieveResults obj for transactionId {}", transactionId);



        List<ServiceInstanceObject> services =
                searchConsolidationService.getResults(transactionId);

        if (services == null) {
            log.debug("User tried to retrieve results for unknown transaction {}", transactionId);
            throw new SecomNotFoundException("Transaction not found: " + transactionId);

        } else if (services.isEmpty()) {
            log.debug("User tried to retrieve results but no results exists for transaction {}", transactionId);
        }

        EnvelopeSearchResultObject envelope = new EnvelopeSearchResultObject();
        envelope.setServiceInstance(services);
        envelope.setTransactionId(UUID.randomUUID());
        envelope.setEnvelopeSignatureCertificate(new String[0]); // empty array
        envelope.setEnvelopeRootCertificateThumbprint("thumbprint"); // empty string
        envelope.setEnvelopeSignatureTime(Instant.now());// empty string

        SearchResult searchResult = new SearchResult();
        searchResult.setEnvelope(envelope);
        searchResult.setEnvelopeSignature("this is a signature placeholder");

        log.debug("Return code 200");

        // And return
        return searchResult;

    }

}




