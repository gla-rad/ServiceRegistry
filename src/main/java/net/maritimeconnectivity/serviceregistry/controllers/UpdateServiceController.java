package net.maritimeconnectivity.serviceregistry.controllers;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import org.grad.secomv2.core.base.SecomConstants;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Slf4j
@Validated
@Path("/")
public class UpdateServiceController {

    static final String UPDATE_SERVICE_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/updateService";

    /**
     * PUT /v2/updateService : The purpose of this interface is to allow the client to make simple updates
     * to an existing service in the MSR.
     *
     * @param instanceId The instance ID of the service to be updated
     * @return Http status 200 OK if the update was successful
     */
    @PUT
    @Path("/{instanceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateServiceInterface(
            @PathParam("instanceId") String instanceId,
            UpdateServiceDto updateRequest
    ) {
        log.debug("Received update for instanceId={} with body={}", instanceId, updateRequest);

        //Get current instance

        //Take copy of that

        //Alter the necessary fields

        //Save




        return Response.ok().build();
    }
}


