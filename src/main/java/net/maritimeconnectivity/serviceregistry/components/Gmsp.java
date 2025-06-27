package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpMessage;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import org.grad.secom.core.models.SearchFilterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j

/*
This class implements use case 2 and 3 as defined in IALA Guideline on Maritime Service Registry (MSR) Technical Specification

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

    public Gmsp(MmsEdgeRouter er, OutgoingMmtpFactory mmtpFactory) {
        this.mmsEdgeRouter = er;
        this.mmtpFactory = mmtpFactory;

    }

    /*
    Assumes local search is conducted elsewhere
    The DTO passed much contain the endpoint (including transaction ID) to which the response should be sent,


   From UC3, step 4: The consumer's MSR propagates the search request (along with the geometry provided description of the route) to the Global MCP Search Platform.
     */

    /**
     * Global Search using MMS.
     * @param searchFilterObj The object representing the SECOM searchService call
     * @param endpoint The endpoint to which the response should be sent. The transactionID is part of the URL.
     * TODO: Consider where the check of certificate validity should be done.
     */
    public void globalSearch (String endpoint, String consumerMrn, SearchFilterObject searchFilterObj) {
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
            } else {
                OutgoingMmtpMessage msg = mmtpFactory.createSendMessage(
                        globalSearchSubject, // Use the global search subject
                        consumerMrn,
                        searchMessageJson,
                        Duration.ofMinutes(messageDurationMinutes) // Set a timeout for the message
                );
                messages.add(msg);
            }

        // Send each message to the MMS Edge Router
        for (OutgoingMmtpMessage msg : messages) {
            mmsEdgeRouter.sendMessage(msg);
            log.info("Global search request sent to MMS Router for Endpoint/XactID: {}", endpoint);
        }

        } catch (JsonProcessingException e) {
            log.error("Error writing JSON for MmsSearchMessageDto", e);
        } catch (IOException e) {
            log.error("Error sending message via MmsEdgeRouter", e);
        }
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

    private ArrayList<String> calculateSubjectsFromGeometry(String geometry) {
        // This method should calculate the subjects based on the geometry provided.
        // For now, it returns an empty list as a placeholder.
        return new ArrayList<>();
    }

    private String writeJsonSearchMessage(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }

    public void handleIncomingGlobalSearch(MmsSearchMessageDto dto) {
        log.info("Handling global search request from MMS Router for Endpoint/XactID: {}", dto.getEndpoint());

        // TODO: initiate local search or further processing here

        // TODO: Once local search is done, a response must be uploaded to the endpoint specified in the DTO
    }




    public MmsSearchMessageDto parseSearchDto(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, MmsSearchMessageDto.class);
    }

}
