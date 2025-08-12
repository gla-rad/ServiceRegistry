package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.models.SearchObjectResult;
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
@Slf4j
@Validated
public class UploadResultsController implements GenericSecomInterface {

    /**
     * The Interface Endpoint Path.
     */
    static final String UPLOAD_RESULTS_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/uploadResults";

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
    public void searchService(
            @PathParam("transactionId") String transactionId,
            @Valid List<SearchObjectResult> searchResults) {

            log.info("Received {} search results for transactionId: {}", searchResults.size(), transactionId);

            // Consolidate results based on transactionId

    }

}
