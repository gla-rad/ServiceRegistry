package net.maritimeconnectivity.serviceregistry.components;

import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The MMs Edge Router Component
 *
 * This component is responsible for sending MMS messages to a designated MMS Router Network
 * as required by MSR Technical Specifcation Requirement 14.A.
 * The implementation contains the necessary subset of functionalities (described in RTCM 13900.0) for an edge router
 * to subscribe and send subject case messages to the MMS Router Network.
 */
@Component
@Slf4j
public class MmsEdgeRouter {

    @Value("${maritimeconnectivity.mms.router.url")
    private String routerUrl;

    @Autowired
    private final KeyStoreUtil keystoreUtil;



}
