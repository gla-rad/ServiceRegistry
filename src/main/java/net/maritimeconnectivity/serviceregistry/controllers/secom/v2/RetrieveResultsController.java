package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.grad.secomv2.core.models.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@Path("/")
@Slf4j
@Validated
public class RetrieveResultsController implements GenericSecomInterface {

    /**
     * The Interface Endpoint Path.
     */
    static final String RETREIVE_RESULTS_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/retrieveResults";

    @Autowired
    SearchConsolidationService searchConsolidationService;

    @Path(RETREIVE_RESULTS_INTERFACE_PATH + "/{transactionId}")
    @GET
    @Produces("application/json")
    public SearchResult retrieveResults(@PathParam("transactionId") String transactionId, @Context final HttpServletResponse response) {

        List<SearchObjectResult> services = searchConsolidationService.getResults(transactionId);

        //If empty, return 404
        if (services.isEmpty()) {
            log.error("No results found for transactionId {}", transactionId);

            try {
                response.flushBuffer();
                return null;
            } catch(Exception e){}
        }



        //Wrap in SearchResult
        SearchResult searchResult = new SearchResult();
        searchResult.setTransactionId(transactionId);
        searchResult.setServices(services);
        return searchResult;
    }
}



