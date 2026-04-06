package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.models.EnvelopeSearchResultObject;
import org.grad.secomv2.core.models.SearchResult;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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

    @Path(RETREIVE_RESULTS_INTERFACE_PATH + "/{transactionId}")
    @GET
    @Produces("application/json")
    public Response retrieveResults(@PathParam("transactionId") UUID transactionId) {

        List<ServiceInstanceObject> services =
                searchConsolidationService.getResults(transactionId.toString());

        if (services == null) {
            log.debug("User tried to retrieve results for unknown transaction {}", transactionId);

            // Build 404 directly avoiding exception mapping as this is not necessary here
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("\"Transaction not found: " + transactionId + "\"")
                    .build();
        } else if (services.isEmpty()) {
            log.debug("User tried to retrieve results but no results exists for transaction {}", transactionId);
        }

        log.debug("Found {} results for transactionId {}", services.size(), transactionId);

        EnvelopeSearchResultObject envelope = new EnvelopeSearchResultObject();
        envelope.setTransactionId(transactionId);
        envelope.setServiceInstance(services); //may be empty, ensures user does not get 404 immediately
        SearchResult searchResult = new SearchResult();
        searchResult.setEnvelope(envelope);
        searchResult.setEnvelopeSignature("This is a signature placeholder"); // No signature is
        // generated for the search result as it is

        log.warn("Returned OK for GS REtrieve results");
        return Response.ok(searchResult).build();
    }

}




