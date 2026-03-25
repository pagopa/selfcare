package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegalAddressProfessionalResponse {

  @JsonProperty("description")
  private String description;

  @JsonProperty("municipality")
  private String municipality;

  @JsonProperty("province")
  private String province;

  @JsonProperty("address")
  private String address;

  @JsonProperty("zip")
  private String zip;

}

