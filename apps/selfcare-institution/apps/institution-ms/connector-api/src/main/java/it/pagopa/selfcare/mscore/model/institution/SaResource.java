package it.pagopa.selfcare.mscore.model.institution;

import lombok.Data;

@Data
public class SaResource {
    private String id;
    private String originId;
    private boolean anacEngaged;
    private boolean anacEnabled;
    private String taxCode;
    private String description;
    private String digitalAddress;
}
