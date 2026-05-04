package it.pagopa.selfcare.mscore.model.institution;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.Data;

@Data
public class Premium {

    @JsonProperty("status")
    private RelationshipState status;

    @JsonProperty("contract")
    private String contract;

}
