package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpMessage;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.SearchArea;
import net.maritimeconnectivity.serviceregistry.models.dto.gmsp.GlobalSearchRequestDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.utils.SearchAreaCalculator;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.grad.secomv2.springboot3.components.SecomConfigProperties;
import org.grad.secomv2.springboot3.components.UploadResultsClient;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;

/**
 * Implements the GMSP (Global Maritime Search Platform) functionality for the
 * Service Registry.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Component
@Slf4j
public class Gmsp {

    @Value("${info.msr.mrn}")
    private String ownMrn;

    @Value("${info.mms.mmtp.duration.minutes}")
    private long messageDurationMinutes;

    @Value("${info.gmsp.search.globalSubject}")
    private String globalSearchSubject;

    @Autowired
    SecomConfigProperties secomConfigProperties;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SearchAreaCalculator searchAreaCalculator;

    @Autowired
    InstanceRepo instanceRepo;

    @Autowired
    SearchAreaCalculator sac;

    @Autowired
    private InstanceSearchQueryBuilder queryBuilder;

    @Autowired
    DomainDtoMapper<Instance, ServiceInstanceObject> searchObjectResultMapper;

    @Autowired
    InstanceService instanceService;

    @Autowired
    private SearchConsolidationService searchConsolidationService;

    @Getter
    private boolean running = false;

    // Class Variables
    private final MmsEdgeRouter mmsEdgeRouter;
    private final OutgoingMmtpFactory mmtpFactory;
    private final HashMap<String, GlobalSearchRequestDto> globalSearchRequests;

    /**
     * The GMSP component constructor.
     *
     * @param er the MMS edge router
     * @param mmtpFactory the MMPT factory
     */
    public Gmsp(MmsEdgeRouter er, OutgoingMmtpFactory mmtpFactory) {
        this.globalSearchRequests = new HashMap<>();
        this.mmsEdgeRouter = er;
        this.mmtpFactory = mmtpFactory;
    }

    @PostConstruct
    public void init() {
        if (this.mmsEdgeRouter.isConnected()) {
            this.subscribe(globalSearchSubject);

            //Sub to all areas in DB
            this.initializeSubscriptionsFromDb();
            this.running = true;
        }
    }

    /**
     * Global Search using MMS.
     *
     * @param searchFilterObj The object representing the SECOM searchService call
     * @param endpoint        The endpoint to which the response should be sent. The transactionID is part of the URL.
     * @return uuid to uniquely identify the global search request
     * TODO: Consider where the check of certificate validity should be done.
     */
    public String globalSearch(String endpoint, String consumerMrn, SearchFilterObject searchFilterObj, Geometry searchGeometry) {
        if (!this.running) {
            return null;
        }

        log.info("Conduct global search for Endpoint: {}", endpoint);

        try {
            MmsSearchMessageDto searchMessageDto = new MmsSearchMessageDto(
                    endpoint, // This should contain the transaction ID
                    consumerMrn,
                    searchFilterObj
            );
            String searchMessageJson = writeJsonSearchMessage(searchMessageDto);

            List<OutgoingMmtpMessage> messages = new ArrayList<>();

            // Calculate subjects if Geometry param is not null
            if (searchGeometry != null) {
                try {

                    List<SearchArea> intersectingAreas = searchAreaCalculator.findIntersectingSearchAreas(searchGeometry);
                    ArrayList<String> subjects = searchAreaCalculator.areaToSubjectMapper(intersectingAreas);
                    log.debug("Found {} subjects for provided geometry", subjects.size());

                    // Create mms msg for each subject
                    for (String subject : subjects) {
                        OutgoingMmtpMessage msg = mmtpFactory.createSendMessage(
                                subject,
                                consumerMrn,
                                searchMessageJson,
                                Duration.ofMinutes(messageDurationMinutes) // Set a timeout for the message
                        );
                        log.debug("added message with subject {}", subject);
                        messages.add(msg);
                    }
                } catch (Exception e) {
                    log.error("Error calculating subjects from geometry: ", e);
                }
            } else {
                log.debug("NO GEOMETRY PROVIDED, USING GLOBAL SEARCH SUBJECT: {}", globalSearchSubject);
                OutgoingMmtpMessage msg = mmtpFactory.createSendMessage(
                        globalSearchSubject, // Use the global search subject
                        consumerMrn,
                        searchMessageJson,
                        Duration.ofMinutes(messageDurationMinutes) // Set a timeout for the message
                );
                messages.add(msg);
            }

            //Create GlobalSearchRequest Oject
            GlobalSearchRequestDto gsr = new GlobalSearchRequestDto(messages.size());
            String gsrUuid = UUID.randomUUID().toString();

            //Get uuid part of endpoint
            String uuid = endpoint.substring(endpoint.lastIndexOf('/') + 1);

            //Create consolidated result entry
            this.searchConsolidationService.createConsolidationEntry(uuid);
            log.debug("Create consolidated entry for transaction ID: {}", uuid);

            // Send each message to the MMS Edge Router
            for (OutgoingMmtpMessage msg : messages) {

                msg.setGsrUuid(gsrUuid); // Set the UUID for tracking
                mmsEdgeRouter.sendMessage(msg);
                log.info("Global search request sent to MMS Router for Endpoint/XactID: {}", endpoint);
            }
            this.globalSearchRequests.put(gsrUuid, gsr);

            return gsrUuid;
        } catch (JsonProcessingException e) {
            log.error("Error writing JSON for MmsSearchMessageDto", e);
        } catch (IOException e) {
            log.error("Error sending message via MmsEdgeRouter", e);
        }

        return null;
    }

    private String writeJsonSearchMessage(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }

    /**
     * Callback function to handle incoming global search requests from the MMS Router.
     * @param dto The DTO containing the search request details.
     */
    @Transactional(readOnly = true)
    public void handleIncomingGlobalSearch(MmsSearchMessageDto dto) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        log.info("Handling GMSP requests transaction ID: {}", dto.getEndpoint());

        //Print details of the search requets searchFilterObject
        var q = dto.getSearchFilterObject().getEnvelope().getQuery();

        log.debug("Search Filter Object Keywords: {}, Name : {}", q.getKeywords(), q.getName());


        UploadResultsClient uploadSecomClient = new UploadResultsClient(
                URI.create(dto.getEndpoint()).toURL(),
                secomConfigProperties
        );
        if (secomConfigProperties == null) {
            log.error("SecomConfigProperties is null, cannot initialize UploadResultsClient");
            return;
        }

        log.debug("Searching local database");
        //Perform local search, which gives a list of SearchObjectResult objects
        final Page<Instance> instancesPage = this.instanceService.search(dto.getSearchFilterObject().getEnvelope());

        List<ServiceInstanceObject> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), ServiceInstanceObject.class);
        searchObjectResults.forEach(r -> r.setSourceMSR(this.ownMrn));
        log.debug("Found {} search results for local database", searchObjectResults.size());

        try {
            uploadSecomClient.uploadResults(searchObjectResults);
        } catch (WebClientResponseException e){
            log.error("Error uploading results via SECOM Upload interface, CODE:", e);
            return;
        }
        log.debug("Uploaded {} results via SECOM Upload interface {}", searchObjectResults.size(), dto.getEndpoint());
    }


    public MmsSearchMessageDto parseSearchDto(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, MmsSearchMessageDto.class);
    }


    public void globalSearchRequestCallback(String uuid) {
        GlobalSearchRequestDto gsr = this.globalSearchRequests.get(uuid);
        if (gsr != null) {
            gsr.decrementCount();
        }
    }

    public boolean isSent(String gsrUuid) {
        if  (this.globalSearchRequests.containsKey(gsrUuid)) {
            return this.globalSearchRequests.get(gsrUuid).isSent();
        }
        return false;
    }

    public void subscribe(String subject) {
        // Subscribe to the subject for incoming messages
        OutgoingMmtpMessage subscriptionMessage = mmtpFactory.createSubscribeMessage(subject);
        try {
            mmsEdgeRouter.subscribe(subscriptionMessage);
            log.debug("Subscribed to subject: {}", subject);
        } catch (Exception e) {
            log.error("Error subscribing to subject {}: {}", subject, e.getMessage());
        }
    }

    public void unsubscribe(String subject) {
        // Unsubscribe from the subject for incoming messages
        OutgoingMmtpMessage unsubscriptionMessage = mmtpFactory.createUnsubscribeMessage(subject);
        try {
            mmsEdgeRouter.unsubscribe(unsubscriptionMessage);
            log.debug("Unsubscribed from subject: {}", subject);
        } catch (Exception e) {
            log.error("Error unsubscribing from subject {}: {}", subject, e.getMessage());
        }
    }

    public Set<String> getSubscriptions() {
        return mmsEdgeRouter.getSubscriptions();
    }

    public void initializeSubscriptionsFromDb() {
        List<SearchArea> allAreasInDb = instanceRepo.findAllInstanceSearchAreasUsed();
        ArrayList<String> allSubjectsInDb = this.sac.areaToSubjectMapper(allAreasInDb);
        //Get existing subscriptions
        Set<String> existingSubscriptions = this.getSubscriptions();
        for (String subject : allSubjectsInDb) {
            if(!existingSubscriptions.contains(subject)) {
                this.subscribe(subject);
            }
        }
    }
}
