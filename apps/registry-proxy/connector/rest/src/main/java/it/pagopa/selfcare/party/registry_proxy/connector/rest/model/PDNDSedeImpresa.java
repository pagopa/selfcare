package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PDNDSedeImpresa {
  @JsonProperty("ComuneSede")
  private String city;

  @JsonProperty("ProvinciaSede")
  private String county;

  @JsonProperty("CapSede")
  private String zipCode;

  @JsonProperty("ToponimoSede")
  private String toponimoSede;

  @JsonProperty("ViaSede")
  private String viaSede;

  @JsonProperty("NcivicoSede")
  private String ncivicoSede;
}
