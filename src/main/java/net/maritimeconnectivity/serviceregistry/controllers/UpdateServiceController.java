package net.maritimeconnectivity.serviceregistry.controllers;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.dto.UpdateServiceDto;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.services.UpdateServiceService;
import org.grad.secomv2.core.base.SecomConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Slf4j
@Validated
@Path("/")
public class UpdateServiceController {

    static final String UPDATE_SERVICE_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/updateService";

    @Autowired
    UpdateServiceService updateServiceService;

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
        log.warn("No RBAC checks are performed on the user calling the updateService interface!");

        Long id = null;
        try {
            id = Long.parseLong(instanceId);
        } catch (NumberFormatException e) {
            log.error("Invalid ID: {}", instanceId);
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid instance ID format").build();
        }

        try {
            updateServiceService.updateService(id, updateRequest);
        } catch (Exception e) {
            log.error("Error while updating instance with id={}", instanceId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating instance").build();
        }


        return Response.ok().build();
    }
}


