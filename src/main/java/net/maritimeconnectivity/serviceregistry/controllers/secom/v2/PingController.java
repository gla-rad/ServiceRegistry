package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.interfaces.GenericSecomInterface;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


/*
    A controller to check the availability of the MSR itself.
    Useful for frontend applications using the MSR as a backend to check if it is up.
 */
@Component
@Path("/")
@Slf4j
@Validated
public class PingController implements GenericSecomInterface {

    /**
     * The Interface Endpoint Path.
     */
    static final String PING_CONTROLLER_PATH = "/" + SecomConstants.SECOM_VERSION + "/ping";


    // Simple ping to return OK when the MSR is running
    @Path(PING_CONTROLLER_PATH)
    @GET
    @Produces("application/json")
    public Response ping() {
        return Response.ok().build();
    }
}




