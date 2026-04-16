package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class GetAddressRegistroImpreseOKDto {

    @JsonProperty("dateTimeExtraction")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date dateTimeExtraction;

    @JsonProperty("taxId")
    private String taxId;

    @JsonProperty("professionalAddress")
    private ProfessionalAddressDto professionalAddress;

}
