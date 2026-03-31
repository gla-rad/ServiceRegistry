package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.models.SearchResult;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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
    public Response retrieveResults(@PathParam("transactionId") String transactionId) {

        List<ServiceInstanceObject> services = searchConsolidationService.getResults(transactionId);

        if (services == null) {
            log.debug("User tried to retrieve results for unknown transaction {}", transactionId);

            // Build 404 directly avoiding exception mapping as this is not necessary here
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Transaction not found: " + transactionId) // or some error DTO
                    .build();
        } else if (services.isEmpty()) {
            log.debug("User tried to retrieve results but no results exists for transaction {}",
                    transactionId);

            return Response.status(Response.Status.OK)
                    .entity("No results for valid transaction: " + transactionId)
                    .build();
        }

        log.debug("Found {} results for transactionId {}", services.size(), transactionId);

        SearchResult searchResult = new SearchResult();
        searchResult.setTransactionId(transactionId);
        searchResult.setServiceInstance(services); //may be empty, ensures user does not get 404 immediately

        return Response.ok(searchResult).build();
    }

}




