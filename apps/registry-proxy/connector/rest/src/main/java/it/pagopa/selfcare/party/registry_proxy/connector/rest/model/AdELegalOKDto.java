package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultCodeEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultDetailEnum;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdELegalOKDto {

    @JsonProperty("verificationResult")
    private Boolean verificationResult;

    @JsonProperty("resultDetail")
    private AdEResultDetailEnum resultDetail;

    @JsonProperty("resultDetailMessage")
    private String resultDetailMessage;

    @JsonProperty("resultCode")
    private AdEResultCodeEnum resultCode;
}
