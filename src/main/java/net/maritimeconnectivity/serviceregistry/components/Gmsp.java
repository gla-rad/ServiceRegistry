package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpMessage;
import net.maritimeconnectivity.serviceregistry.models.dto.gmsp.GlobalSearchRequestDto;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.utils.WKTUtil;
import org.grad.secom.core.models.SearchFilterObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j

/*
Implements the GMSP (Global Maritime Search Platform) functionality for the Service Registry.
 */
public class Gmsp {

    @Value("${info.mms.mmtp.duration.minutes}")
    private long messageDurationMinutes;

    @Value("${info.gmsp.search.globalSubject}")
    private String globalSearchSubject;

    @Autowired
    ObjectMapper objectMapper;

    private final MmsEdgeRouter mmsEdgeRouter;

    private final OutgoingMmtpFactory mmtpFactory;

    private HashMap<String, GlobalSearchRequestDto> globalSearchRequests;

    public Gmsp(MmsEdgeRouter er, OutgoingMmtpFactory mmtpFactory) {
        this.globalSearchRequests = new HashMap<>();
        this.mmsEdgeRouter = er;
        this.mmtpFactory = mmtpFactory;

    }

    /**
     * Global Search using MMS.
     * @param searchFilterObj The object representing the SECOM searchService call
     * @param endpoint The endpoint to which the response should be sent. The transactionID is part of the URL.
     * @return uuid to uniquely identify the global search request
     * TODO: Consider where the check of certificate validity should be done.
     */
    public String globalSearch (String endpoint, String consumerMrn, SearchFilterObject searchFilterObj) {
        try {
            MmsSearchMessageDto searchMessageDto = new MmsSearchMessageDto(
                    endpoint, // This should contain the transaction ID
                    null,
                    searchFilterObj.getQuery() //Extract the searchParam object
            );
            String searchMessageJson = writeJsonSearchMessage(searchMessageDto);


            List<OutgoingMmtpMessage> messages = new ArrayList<>();

            // Calculate subjects from geometry if it exists
            if (this.containsGeometry(searchFilterObj)) {
                try {

                    ArrayList<String> subjects = calculateSubjectsFromGeometry(searchFilterObj.getGeometry());

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

    private ArrayList<String> calculateSubjectsFromGeometry(String geometryAsWKT) throws ParseException {
        //Parse WKT to Geometry
        Geometry geometry = WKTUtil.convertWKTtoGeometry(geometryAsWKT);

        //Create Luscene query

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
     *
     * @param dto The DTO containing the search request details.
     */
    public void handleIncomingGlobalSearch(MmsSearchMessageDto dto) {
        log.info("Handling global search request from MMS Router for Endpoint/XactID: {}", dto.getEndpoint());

        // TODO: initiate local search or further processing here

        // TODO: Once local search is done, a response must be uploaded to the endpoint specified in the DTO
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
}
