package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import com.netflix.discovery.converters.Auto;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.services.SecomSearchResultSigningService;
import net.maritimeconnectivity.serviceregistry.utils.CertificateParsingUtil;
import org.grad.secomv2.core.base.EnvelopeSignatureBearer;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.exceptions.SecomNotFoundException;
import org.grad.secomv2.core.exceptions.SecomValidationException;
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

    @Autowired
    SecomSearchResultSigningService secomSearchResultSigningService;

    @Autowired
    CertificateParsingUtil certificateParsingUtil;


    @Path(RETREIVE_RESULTS_INTERFACE_PATH + "/{transactionId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResult retrieveResult(@PathParam("transactionId") String transactionId,
                                       @Valid RetrieveResultObject retrieveResultObject) {

        EnvelopeRetrieveResultObject envelopeSearchResultObject = retrieveResultObject.getEnvelope();

        String consumerMrn =
                certificateParsingUtil.getMrnFromCertificate(envelopeSearchResultObject.getEnvelopeSignatureCertificate());


        String transactionIdBody = envelopeSearchResultObject.getTransactionId();

        log.debug("Retrieved valid retrieveResults obj for transactionId {}", transactionId);

        if (!transactionId.equals(transactionIdBody)) {
            throw new SecomValidationException("Transaction ID mismatch");
        }



        List<ServiceInstanceObject> services =
                searchConsolidationService.getResults(transactionIdBody, consumerMrn);

        if (services == null) {
            log.debug("User tried to retrieve results for unknown transaction {}", transactionIdBody);
            throw new SecomNotFoundException("Transaction not found: " + transactionIdBody);

        } else if (services.isEmpty()) {
            log.debug("User tried to retrieve results but no results exists for transaction {}", transactionIdBody);
        }

        EnvelopeSearchResultObject envelope = new EnvelopeSearchResultObject();
        envelope.setServiceInstance(services);
        envelope.setTransactionId(UUID.fromString(transactionIdBody));

        SearchResult searchResult = secomSearchResultSigningService.signSearchResult(envelope);

        // And return
        return searchResult;

    }

}




