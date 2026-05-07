package it.pagopa.selfcare.dashboard.model.delegation;

import lombok.Data;

@Data
public class Delegation {
    private String id;
    private String institutionId;
    private String brokerId;
    private String productId;
    private String institutionName;
    private String institutionRootName;
    private String brokerName;
    private DelegationType type;
}
