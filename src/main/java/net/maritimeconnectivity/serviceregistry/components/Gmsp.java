package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpMessage;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.dto.gmsp.GlobalSearchRequestDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.services.InstanceService;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.grad.secomv2.core.models.SearchFilterObject;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.grad.secomv2.springboot3.components.SecomConfigProperties;
import org.grad.secomv2.springboot3.components.UploadResultsClient;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/*
Implements the GMSP (Global Maritime Search Platform) functionality for the Service Registry.
 */
@Component
@Slf4j
public class Gmsp {

    @Autowired
    SecomConfigProperties secomConfigProperties;

    @Value("${info.mms.mmtp.duration.minutes}")
    private long messageDurationMinutes;

    @Value("${info.gmsp.search.globalSubject}")
    @Getter
    private String globalSearchSubject;

    @Autowired
    ObjectMapper objectMapper;

    private final MmsEdgeRouter mmsEdgeRouter;

    private final OutgoingMmtpFactory mmtpFactory;

    private HashMap<String, GlobalSearchRequestDto> globalSearchRequests;

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
     * @param searchFilterObj The object representing the SECOM searchService call
     * @param endpoint The endpoint to which the response should be sent. The transactionID is part of the URL.
     * @return uuid to uniquely identify the global search request
     * TODO: Consider where the check of certificate validity should be done.
     */
    public String globalSearch (String endpoint, String consumerMrn, SearchFilterObject searchFilterObj, Geometry searchGeometry) {
        log.info("Conduct global search for Endpoint: {}", endpoint);


        try {
            MmsSearchMessageDto searchMessageDto = new MmsSearchMessageDto(
                    endpoint, // This should contain the transaction ID
                    consumerMrn,
                    searchFilterObj
            );
            String searchMessageJson = writeJsonSearchMessage(searchMessageDto);


            List<OutgoingMmtpMessage> messages = new ArrayList<>();

            // Calculate subjects if Gemometry param is not null
            if (searchGeometry != null) {
                try {

                    ArrayList<String> subjects = calculateSubjectsFromGeometry(searchGeometry);

                    // Create mms msg for each subject
                    for (String subject : subjects) {
                        OutgoingMmtpMessage msg = mmtpFactory.createSendMessage(
                                subject,
                                consumerMrn,
                                searchMessageJson,
                                Duration.ofMinutes(messageDurationMinutes) // Set a timeout for the message
                        );
                        messages.add(msg);
                    }
                } catch (Exception e) {
                    log.error("Error calculating subjects from geometry: ", e);
                }
            } else {
                log.warn("NO GEOMETRY PROVIDED, USING GLOBAL SEARCH SUBJECT: {}", globalSearchSubject);
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


    private boolean containsGeometry(SearchFilterObject searchFilterObject) {
        // Check if the searchFilterObject contains a geometry
        return searchFilterObject.getQuery() != null && searchFilterObject.getGeometry() != null;
    }

    /**
     * This method calculates the subject based on the geometry provided in the search parameters.
     *
     * @param geometry The geometry string in WKT format from which to calculate the subject.
     * example = "POLYGON ((0.65 51.42, 0.65 52.26, 2.68 52.26, 2.68 51.42, 0.65 51.42))")
     * @return A string representing the subject derived from the geometry.
     */
    private String calculateSubjectFromGeometry(String geometry) {
        return "";
    }

    private ArrayList<String> calculateSubjectsFromGeometry(Geometry searchGeometry) {

        //Create Luscene query d

        //Run the query - should find intersections in order to return areas of interest (only the areas!)

        //Return list of area MRNs for which we need to propagate the request over MMS.



        // This method should calculate the subjects based on the geometry provided.
        // For now, it returns an empty list as a placeholder.


        //Give me all areas where the WKT geometry intersects with the areas of interest.

        return new ArrayList<>();
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

        log.info("Search Filter Object Keywords: {}, Name : {}", q.getKeywords(), q.getName());


        UploadResultsClient uploadSecomClient = new UploadResultsClient(
                URI.create(dto.getEndpoint()).toURL(),
                secomConfigProperties
        );
        if (secomConfigProperties == null) {
            log.error("SecomConfigProperties is null, cannot initialize UploadResultsClient");
            return;
        }

        log.info("Searching local database");
        //Perform local search, which gives a list of SearchObjectResult objects
        final Page<Instance> instancesPage = this.instanceService.search(dto.getSearchFilterObject());

        log.info("Extract filter object");
        List<SearchObjectResult> searchObjectResults = this.searchObjectResultMapper.convertToList(instancesPage.getContent(), SearchObjectResultWithCert.class);
        log.info("Found {} search results for local database", searchObjectResults.size());

        try {
            uploadSecomClient.uploadResults(searchObjectResults);
        } catch (WebClientResponseException e){
            log.error("Error uploading results via SECOM Upload interface, CODE:", e);
            return;
        }
        log.info("Uploaded {} results via SECOM Upload interface {}", searchObjectResults.size(), dto.getEndpoint());
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

    private void subscribe(String subject) {
        // Subscribe to the subject for incoming messages
        OutgoingMmtpMessage subscriptionMessage = mmtpFactory.createSubscribeMessage(subject);
        try {
            mmsEdgeRouter.sendMessage(subscriptionMessage);
            log.info("Subscribed to subject: {}", subject);
        } catch (Exception e) {
            log.error("Error subscribing to subject {}: {}", subject, e.getMessage());
        }
    }
}
