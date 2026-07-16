package net.maritimeconnectivity.serviceregistry.controllers.g1191.v2;

import jakarta.ws.rs.*;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The MSR IALA G1191 Upload Results Interface.
 * </p>
 * This interface definition can be used by the SECOM-compliant MSRs
 * participating in GMSP to upload result of a SECOM searchService request.
 * Interface placed outside SECOM library as it is not part of the SECOM
 * standard, but unofficially an addition to SECOM used by the MSR.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@RestController
@Slf4j
@Validated
@RequestMapping("/api/g1191/" + SecomConstants.SECOM_VERSION)
public class UploadResultsController {


    /**
     * The GMSP Search Consolidation Service
     */
    @Autowired
    SearchConsolidationService searchConsolidationService;

    /**
     * POST /v2/uploadResults : The purpose of this interface is to upload
     * results to a global searhService request that has been propagated
     * to the MSR over the GMSP.
     *
     * @param transactionId The transaction ID associated with the global search
     * @param searchResults The search filter object
     * @return Http status 200 OK if the results were successfully uploaded
     * @implNote Results with invalid signature in the envelope will be rejected by the middleware
     */
    @PostMapping("/uploadResults/{transactionId}")
    public ResponseEntity<Void>  uploadResults(@PathVariable("transactionId") String transactionId,
                                               @RequestBody List<ServiceInstanceObject> searchResults) {
        log.debug("UPLOADCONTROLLER: Received {} search results for transactionId: {}", searchResults.size(), transactionId);

        //Check that xactId exists
        if (transactionId == null || !this.searchConsolidationService.entryExistsForTransaction(transactionId)) {
            return ResponseEntity.badRequest().build();
        }

        //TODO
        // For any request where the MRN of the sender does not conform to the MSR MRN defined in G1183 (i.e. does not
        // begin with urn:mrn:mcp:msr ) a HTTP response with status code 400 must be returned.

        if (searchResults.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        for (ServiceInstanceObject result : searchResults) {
            log.debug("Service name: {}", result.getName());

            if (result.getSourceMSRs() != null) {
                for (String source : result.getSourceMSRs()) {
                    log.debug("Source MSR {}", source);
                }
            }
        }

        //Validate the results

        //Call to some validationservice

        // Consolidate results based on transactionId cast to searchObjectResult
        List<ServiceInstanceObject> results = searchResults.stream().map(r -> (ServiceInstanceObject) r).toList();
        this.searchConsolidationService.addResults(transactionId, results);
        return ResponseEntity.ok().build();
    }

}
