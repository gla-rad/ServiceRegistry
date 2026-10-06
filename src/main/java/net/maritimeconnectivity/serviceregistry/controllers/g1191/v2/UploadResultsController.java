package net.maritimeconnectivity.serviceregistry.controllers.g1191.v2;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.domain.UploadSearchResultObject;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The MSR Upload Results Interface.
 * </p>
 * This interface definition can be used by the SECOM-compliant MSRs participating in GMSP
 * to upload result of a SECOM searchService request.
 * Interface placed outside SECOM libary as it is not part of the SECOM standard, but unoficially an addition to SECOM
 * used by the MSR.
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */

@RestController
@Slf4j
@Validated
@RequestMapping("/api/g1191/" + SecomConstants.SECOM_VERSION)
public class UploadResultsController {


    @Autowired
    SearchConsolidationService searchConsolidationService;

    /**
     * POST /v2/uploadResults : The purpose of this interface is to upload results to a global searhService
     * request that has been propagated to the MSR over the GMSP.
     *
     * @param transactionId The transaction ID associated with the global search
     * @param uploadResult The object containing the results uploaded by the client
     * @return Http status 200 OK if the results were successfully uploaded
     * @implNote Results with invalid signature in the envelope will be rejected by the middleware
     */

    @PostMapping("/uploadResults/{transactionId}")
    public ResponseEntity<Void> uploadResults(
            @PathVariable String transactionId,
            @Valid @RequestBody UploadSearchResultObject uploadResult) {
        if (uploadResult.getEnvelope() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ServiceInstanceObject> searchResults =
                uploadResult.getEnvelope().getServiceInstance();

        if (searchResults == null || searchResults.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("UPLOADCONTROLLER: Received {} search results for transactionId: {}", searchResults.size(), transactionId);


        //Check that xactId exists
        if (!searchConsolidationService.entryExistsForTransaction(transactionId)) {
            throw new InvalidRequestException("No consolidation entry found for transaction id " + transactionId);
        }

        //TODO
        // For any request where the MRN of the sender does not conform to the MSR MRN defined in G1183 (i.e. does not
        // begin with urn:mrn:mcp:msr ) a HTTP response with status code 400 must be returned.


        //Validate the results

        //Call to some validationservice
        

        // Consolidate results based on transactionId cast to searchObjectResult
        searchConsolidationService.addResults(transactionId, searchResults);
        return ResponseEntity.ok().build();
    }

}
