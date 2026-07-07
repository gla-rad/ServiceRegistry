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
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.utils.SearchAreaCalculator;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.grad.secomv2.springboot3.components.SecomClient;
import org.grad.secomv2.springboot3.components.SecomConfigProperties;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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
@ConditionalOnProperty(value = "info.gmsp.enabled", havingValue = "true")
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
    InstanceSearchQueryBuilder queryBuilder;

    @Autowired
    DomainDtoMapper<Instance, ServiceInstanceObject> searchObjectResultMapper;

    @Autowired
    InstanceService instanceService;

    @Autowired
    SearchConsolidationService searchConsolidationService;

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

    /**
     * The initialization function of the GMSP Component.
     * <p/>
     * This operation will subscribe the Service Registry to the global search
     * subject of the MMS Edge Router and will initialize the subscription.
     */
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
            String searchMessageJson = mmsSearchMessageDTOtoJSON(searchMessageDto);

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
            this.searchConsolidationService.createConsolidationEntry(uuid, consumerMrn);
            log.debug("Create consolidated entry for transaction ID: {} Consumer {}",
                    uuid, consumerMrn);

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

        // Check whether we can initialise a SECOM client
        if (secomConfigProperties == null) {
            log.error("SecomConfigProperties is null, cannot initialize UploadResultsClient");
            return;
        }

        log.debug("Searching local database");
        //Perform local search, which gives a list of SearchObjectResult objects
        final Page<Instance> instancesPage = this.instanceService.search(dto.getSearchFilterObject().getEnvelope());

        List<ServiceInstanceObject> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), ServiceInstanceObject.class);
        searchObjectResults.forEach(r -> r.setSourceMSRs(new String[]{ownMrn}));
        log.debug("Found {} search results for local database", searchObjectResults.size());

        try {
            uploadResults(URI.create(dto.getEndpoint()).toURL(), secomConfigProperties, searchObjectResults);
        } catch (WebClientResponseException e){
            log.error("Error uploading results via SECOM Upload interface, CODE:", e);
            return;
        }
        log.debug("Uploaded {} results via SECOM Upload interface {}", searchObjectResults.size(), dto.getEndpoint());
    }

    /**
     * This helper function will perform the UploadResults operation using the
     * SECOM Client. The UploadResults is not an official SECOM operation and
     * therefore not supported out of the box from the SECOMLib. We can however
     * use the SECOMLib WebClient to perform a SECOM-like call.
     *
     * @param url the URL to connect the SECOMLib WebClient to
     * @param secomConfigProperties the SECOM Configuration Properties
     * @param searchResults the search results to be uploaded
     * @throws IOException – for IO exceptions
     * @throws KeyStoreException – for exceptions while handling the key-store
     * @throws NoSuchAlgorithmException – for exceptions onthe key-store alghorithm
     * @throws CertificateException – for certificate exceptions
     * @throws UnrecoverableKeyException – for certificate key exceptions
     */
    protected void uploadResults(URL url,
                                        SecomConfigProperties secomConfigProperties,
                                        List<ServiceInstanceObject> searchResults) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        // Create a SECOM client
        final SecomClient secomClient = new SecomClient(
                url,
                secomConfigProperties);

        // Make the bespoke G1191 UploadResults query
        final ResponseEntity<Void> entity = secomClient.getSecomClient()
                .post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchResults)
                .exchangeToMono(response -> response.toBodilessEntity())
                .block();

        // Assert the success of the operation
        assert entity != null;
    }

    /**
     * A common global search callback function that decreases the
     * global search request count.
     *
     * @param uuid the global search request UUID
     */
    public void globalSearchRequestCallback(String uuid) {
        GlobalSearchRequestDto gsr = this.globalSearchRequests.get(uuid);
        if (gsr != null) {
            gsr.decrementCount();
        }
    }

    /**
     * Initializes the previous subscriptions, already stored in the database.
     * This operation is useful to bring back the GMSP to the correct state.
     */
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

    /**
     * Ensurs that the global search request has been successfully sent.
     *
     * @param gsrUuid the global search request UUID
     * @return whether the global search request has been successfully sent or not
     */
    public boolean isSent(String gsrUuid) {
        if  (this.globalSearchRequests.containsKey(gsrUuid)) {
            return this.globalSearchRequests.get(gsrUuid).isSent();
        }
        return false;
    }

    /**
     * Subscribes the GMSP to a specific subject of the MMS Edge Router.
     *
     * @param subject the MMS Edge Router subject to subscribe to
     */
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

    /**
     * Unsubscribes the GMSP from a specific subject of the MMS Edge Router.
     *
     * @param subject the MMS Edge Router subject to unsubscribe from
     */
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

    /**
     * Returns a list of the active subscriptions of the GMSP.
     *
     * @return The actibe GMSP subscriptions
     */
    public Set<String> getSubscriptions() {
        return mmsEdgeRouter.getSubscriptions();
    }

    /**
     * A simple helper function to encode the MMS Seatch Message DTO to JSON.
     *
     * @param mmsSearchMessageDto the MMS Search Message DTO
     * @return the JSON encoding
     * @throws JsonProcessingException on JSON encoding failures
     */
    public String mmsSearchMessageDTOtoJSON(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }

    /**
     * A simple helper function to decode  the MMS Seatch Message DTO from JSON.
     *
     * @param json the JSON encoding of the MMS Search Message DTO
     * @return the MMS Search Message DTO
     * @throws JsonProcessingException on JSON decoding failures
     */
    public MmsSearchMessageDto mmsSearchMessageDTOfromJSON(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, MmsSearchMessageDto.class);
    }

}
