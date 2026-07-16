package net.maritimeconnectivity.serviceregistry.controllers.g1191.v2;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import net.maritimeconnectivity.serviceregistry.services.UpdateServiceService;
import org.grad.secomv2.core.base.SecomConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The IALA G1191 Update Service Controller.
 * </p>
 * According to IALA G1191, an MSR must support automated update of some of the
 * service information to ensure that it has an up-to-date certificate,
 * endpoint, API documentation and version information of the service.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@RestController
@Slf4j
@Validated
@RequestMapping("/api/g1191/" + SecomConstants.SECOM_VERSION)
public class UpdateServiceController {

    /**
     * The Update Service Service Controller.
     */
    @Autowired
    UpdateServiceService updateServiceService;

    /**
     *  PUT /v2/updateService/{instanceId} : The purpose of this interface is
     *  to update of some of the service information to ensure that it has an
     *  up-to-date certificate, endpoint, API documentation and version
     *  information of the registered services.
     *
     * @param instanceId The service instance ID to be updated
     * @return Http status 200 OK if the results were successfully uploaded
     * @implNote Results with invalid signature in the envelope will be rejected by the middleware
     */
    @PutMapping("/updateService/{instanceId}")
    public ResponseEntity<Void> updateServiceInterface(@PathVariable Long instanceId,
                                                       @Valid @RequestBody UpdateServiceDto updateRequest) {
        log.debug("Received update for instanceId={} with body={}", instanceId, updateRequest);
        log.warn("No RBAC checks are performed on the user calling the updateService interface!");

        // Update the service as per the request
        updateServiceService.updateService(instanceId, updateRequest);

        // Return the response
        return ResponseEntity.ok().build();
    }
}
