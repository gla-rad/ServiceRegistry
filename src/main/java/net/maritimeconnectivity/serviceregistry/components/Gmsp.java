package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpMessage;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.SearchArea;
import net.maritimeconnectivity.serviceregistry.models.dto.gmsp.GlobalSearchRequestDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.SearchAreaCalculator;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.grad.secomv2.springboot3.components.SecomConfigProperties;
import org.grad.secomv2.springboot3.components.UploadResultsClient;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
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

/*
Implements the GMSP (Global Maritime Search Platform) functionality for the Service Registry.
 */
@Component
@Slf4j
public class Gmsp {

    private final String G1191_SEARCHAREA_PREFIX = "urn:mrn:mcp:msr:search:searcharea:";

    @Autowired
    SecomConfigProperties secomConfigProperties;

    @Value("${info.mms.mmtp.duration.minutes}")
    private long messageDurationMinutes;

    @Value("${info.gmsp.search.globalSubject}")
    private String globalSearchSubject;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SearchAreaCalculator searchAreaCalculator;


    @Autowired
    private InstanceSearchQueryBuilder queryBuilder;

    private final MmsEdgeRouter mmsEdgeRouter;

    private final OutgoingMmtpFactory mmtpFactory;

    private final HashMap<String, GlobalSearchRequestDto> globalSearchRequests;

    @Value("${info.msr.mrn}")
    private String ownMrn;

    @Autowired
    DomainDtoMapper<Instance, SearchObjectResult> searchObjectResultMapper;

    @Autowired
    InstanceService instanceService;

    public Gmsp(MmsEdgeRouter er, OutgoingMmtpFactory mmtpFactory) {
        this.globalSearchRequests = new HashMap<>();
        this.mmsEdgeRouter = er;
        this.mmtpFactory = mmtpFactory;
    }

    @PostConstruct
    public void init() {
        this.subscribe(globalSearchSubject);

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
        var q = dto.getSearchFilterObject().getQuery();

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
        final Page<Instance> instancesPage = this.instanceService.search(dto.getSearchFilterObject());

        List<SearchObjectResult> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), SearchObjectResultWithCert.class);
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
}
