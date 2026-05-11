package it.pagopa.selfcare.dashboard.model.backoffice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrokerInfo {

    private String code;
    private String description;
    private Boolean enabled;
    private Boolean extendedFaultBean;

}
