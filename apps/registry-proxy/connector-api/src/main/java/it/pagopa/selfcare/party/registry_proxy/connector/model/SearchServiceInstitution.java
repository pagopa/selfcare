package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SearchServiceInstitution {

  private String id;
  private String description;
  private String parentDescription;
  private String taxCode;
  private OffsetDateTime lastModified;

}
