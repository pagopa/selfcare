package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class LegalAddressResponse {

  @JsonProperty("dateTimeExtraction")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date dateTimeExtraction;

  @JsonProperty("taxId")
  private String taxId;

  @JsonProperty("professionalAddress")
  private LegalAddressProfessionalResponse professionalAddress;

}

