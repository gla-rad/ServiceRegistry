package net.maritimeconnectivity.serviceregistry.controllers.g1191.v2;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.util.List;

/**
 * The MSR Upload Results Interface.
 * </p>
 * This interface definition can be used by the SECOM-compliant MSRs participating in GMSP
 * to upload result of a SECOM searchService request.
 * Interface placed outside SECOM libary as it is not part of the SECOM standard, but unoficially an addition to SECOM
 * used by the MSR.
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */

@Component
@Path("/")
@Slf4j
@Validated
public class UploadResultsController {

    /**
     * The Interface Endpoint Path.
     */
    static final String UPLOAD_RESULTS_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/uploadResults";

    @Autowired
    SearchConsolidationService searchConsolidationService;

    /**
     * POST /v2/uploadResults : The purpose of this interface is to upload results to a global searhService
     * request that has been propagated to the MSR over the GMSP.
     *
     * @param transactionId The transaction ID associated with the global search
     * @param searchResults The search filter object
     * @return Http status 200 OK if the results were successfully uploaded
     */

    @Path(UPLOAD_RESULTS_INTERFACE_PATH + "/{transactionId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void searchService(@PathParam("transactionId") String transactionId,
                              List<SearchObjectResultWithCert> searchResults) {

        log.debug("UPLOADCONTROLLER: Received {} search results for transactionId: {}", searchResults.size(), transactionId);
        for (SearchObjectResultWithCert result : searchResults) {
            log.debug("Service name: {}", result.getName());
        }
        // Consolidate results based on transactionId cast to searchObjectResult
        List<SearchObjectResult> results = searchResults.stream().map(r -> (SearchObjectResult) r).toList();
        searchConsolidationService.addResults(transactionId, results);

    }

}
