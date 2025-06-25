package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.MmtpFactory;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
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

    @Value("${info.gmsp.search.globalSubject}")
    private String globalSearchSubject;

    @Autowired
    ObjectMapper objectMapper;

    private final MmsEdgeRouter mmsEdgeRouter;

    private final MmtpFactory mmtpFactory;

    public Gmsp(MmsEdgeRouter er, MmtpFactory mmtpFactory) {
        this.mmsEdgeRouter = er;
        this.mmtpFactory = mmtpFactory;

    }

    /*
    Assumes local search is conducted elsewhere
    The DTO passed much contain the endpoint (including transaction ID) to which the response should be sent,


   From UC3, step 4: The consumer's MSR propagates the search request (along with the geometry provided description of the route) to the Global MCP Search Platform.
     */
    public void globalSearch (MmsSearchMessageDto mmsSearchMessageDto) {
        try {
            // Convert MmsSearchMessageDto to JSON string
            String searchMessageJson = writeJsonSearchMessage(mmsSearchMessageDto);


            //Calculate subject based on whether geometry is present
            // TODO: Implement this, but we need to have postgis calculate the intersections of the geometry
            // should be named CalculateSubjectFromGeometry
            List<MmtpMessage> messages = new ArrayList<>();


            if (!mmsSearchMessageDto.hasGeometry()) {
                MmtpMessage msg = this.mmtpFactory.createSendMessage(
                        globalSearchSubject,
                        mmsSearchMessageDto.getConsumerMRN(),
                        searchMessageJson,
                        Duration.ofHours(1)
                );
                messages.add(msg);
            } else {
                // Infer subjects from geometry
                // Add subjects
                messages.add(null);
            }

        // Send each message to the MMS Edge Router
        for (MmtpMessage msg : messages) {
            mmsEdgeRouter.sendMessage(msg);
            log.info("Global search request sent to MMS Router for Endpoint/XactID: {}", mmsSearchMessageDto.getEndpoint());
        }


        } catch (JsonProcessingException e) {
            log.error("Error writing JSON for MmsSearchMessageDto", e);
        } catch (IOException e) {
            log.error("Error sending message via MmsEdgeRouter", e);
        }
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
