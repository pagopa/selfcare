package it.pagopa.selfcare.dashboard.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RootParentResponse {
    private String description;
    private String id;
}
