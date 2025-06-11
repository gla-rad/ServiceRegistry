package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Gmsp {

    @Autowired
    ObjectMapper objectMapper;

    private final MmsEdgeRouter mmsEdgeRouter;

    public Gmsp(MmsEdgeRouter er) {
        this.mmsEdgeRouter = er;
    }


    public void globalSearch (MmsSearchMessageDto mmsSearchMessageDto) {
        try {
            String searchMessageJson = writeJsonSearchMessage(mmsSearchMessageDto);
        } catch (JsonProcessingException e) {
            log.error("Error writing JSON for MmsSearchMessageDto", e);
        }

        // Send Via MMS Edge Router

    }

    private String writeJsonSearchMessage(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }


    //Somefunction that can call a callback to client code upon event


}
