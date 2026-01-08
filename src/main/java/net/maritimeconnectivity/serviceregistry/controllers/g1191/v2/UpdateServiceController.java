package net.maritimeconnectivity.serviceregistry.controllers.g1191.v2;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.XMLValidationException;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import net.maritimeconnectivity.serviceregistry.services.UpdateServiceService;
import org.grad.secomv2.core.base.SecomConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Validated
@RequestMapping("/mvc/secom/" + SecomConstants.SECOM_VERSION)

public class UpdateServiceController {

    private final UpdateServiceService updateServiceService;

    public UpdateServiceController(UpdateServiceService updateServiceService) {
        this.updateServiceService = updateServiceService;
    }

    // PUT /v2/updateService/{instanceId}
    @PutMapping("/updateService/{instanceId}")
    public ResponseEntity<Void> updateServiceInterface(
            @PathVariable Long instanceId, //spring returns 400 if not a Long
            @Valid @RequestBody UpdateServiceDto updateRequest
    ) throws XMLValidationException {
        log.debug("Received update for instanceId={} with body={}", instanceId, updateRequest);
        log.warn("No RBAC checks are performed on the user calling the updateService interface!");

        updateServiceService.updateService(instanceId, updateRequest);

        return ResponseEntity.ok().build();
    }
}
