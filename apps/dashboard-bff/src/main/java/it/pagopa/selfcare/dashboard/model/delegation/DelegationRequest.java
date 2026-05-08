package it.pagopa.selfcare.dashboard.model.delegation;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DelegationRequest {
    private String id;
    private String from;
    private String to;
    private String productId;
    private String institutionFromName;
    private String institutionToName;
    private DelegationType type;
}
