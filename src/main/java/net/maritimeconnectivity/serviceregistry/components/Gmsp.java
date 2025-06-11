package net.maritimeconnectivity.serviceregistry.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import org.springframework.beans.factory.annotation.Autowired;

public class Gmsp {

    @Autowired
    ObjectMapper objectMapper;



    public void globalSearch (MmsSearchMessageDto mmsSearchMessageDto) {
        // TODO : Write JSON Object
        // Send Via MMS Edge Router
    }

    private String writeJsonSearchMessage(MmsSearchMessageDto mmsSearchMessageDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mmsSearchMessageDto);
    }



}
