package net.maritimeconnectivity.serviceregistry.models.dto.mms;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.pqc.jcajce.provider.Falcon;
import org.grad.secomv2.core.models.SearchParameters;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class MmsSearchMessageDto {

    private String endpoint; //Should contain transactionID

    @Getter
    private String consumerMRN;

    private SearchParameters searchParameters;

    public MmsSearchMessageDto(String endpoint, String consumerMRN, SearchParameters searchParams) {
        this.endpoint = endpoint;
        this.consumerMRN = consumerMRN;
        this.searchParameters = searchParams;
    }

}