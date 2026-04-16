package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import lombok.Data;

@Data
public class BrokerResponse {

    private String id;
    @DecryptIfUuid
    private String taxCode;
    private String description;
    private int numberOfDelegations;

}
