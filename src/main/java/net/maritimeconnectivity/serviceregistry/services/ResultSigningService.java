package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service implementation for signing search results obtained from local and global search
 * Checks conducted are described in IALA G1191
 * Note - that invalid signatures have already been rejected by the middleware, so this service
 * focuses on signing the results to the client.
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class ResultSigningService {


}
