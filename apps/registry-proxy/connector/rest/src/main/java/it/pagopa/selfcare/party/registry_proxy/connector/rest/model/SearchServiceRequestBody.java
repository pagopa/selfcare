package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import lombok.Data;

@Data
public class SearchServiceRequestBody {
  private String search;
  private String filter;
  private Integer top;
  private Integer skip;
  private String select;
  private String orderby;
  private Boolean count;
}
