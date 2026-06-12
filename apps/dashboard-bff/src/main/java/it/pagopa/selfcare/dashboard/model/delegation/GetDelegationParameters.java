package it.pagopa.selfcare.dashboard.model.delegation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetDelegationParameters {
    private String from;
    private String to;
    private String productId;
    private String search;
    private String taxCode;
    private String order;
    private Integer page;
    private Integer size;
}
