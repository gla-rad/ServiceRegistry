package net.maritimeconnectivity.serviceregistry.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UpdateServiceDto {


     private List<String> certificates;
     private String version;
     private String endpointUri;
     private String apiDoc;
     private String statusEndpoint;


}


