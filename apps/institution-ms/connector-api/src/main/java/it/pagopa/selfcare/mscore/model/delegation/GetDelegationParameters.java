package it.pagopa.selfcare.mscore.model.delegation;

import it.pagopa.selfcare.mscore.constant.Order;
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
    private Order order;
    private Integer page;
    private Integer size;
}