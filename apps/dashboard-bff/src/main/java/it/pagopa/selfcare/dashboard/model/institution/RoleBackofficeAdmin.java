package it.pagopa.selfcare.dashboard.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleBackofficeAdmin implements Serializable {
    private String partyRole;
    @JsonProperty("role")
    private String productRole;
    private String productId;
}
