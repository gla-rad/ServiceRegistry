package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
@Slf4j
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

        // Create mmtpFactory and register it with the MmsEdgeRouter

    }


    public void globalSearch (MmsSearchMessageDto mmsSearchMessageDto) {
        try {
            // Convert MmsSearchMessageDto to JSON string
            String searchMessageJson = writeJsonSearchMessage(mmsSearchMessageDto);

            //Set TTL to 1 h. Note this is a high level java object and not seconds after epoch
            Duration ttl = Duration.ofHours(1);

            //Calculate subject based on whether geometry is present
            // TODO: Implement this, but we need to have postgis calculate the intersections of the geometry

            MmtpMessage msg = this.mmtpFactory.createSendMessage(
                    globalSearchSubject,
                    mmsSearchMessageDto.getConsumerMRN(),
                    searchMessageJson,
                    ttl
            );

            mmsEdgeRouter.sendMessage(msg);


        } catch (JsonProcessingException e) {
            log.error("Error writing JSON for MmsSearchMessageDto", e);
        } catch (IOException e) {
            log.error("Error sending message via MmsEdgeRouter", e);
        }
    }

    private String writeJsonSearchMessage(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }




    //Somefunction that can call a callback to client code upon event


}
