package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchServiceIndexResponse {

  @JsonProperty("@odata.context")
  private String context;

  @JsonProperty("@odata.count")
  private Long count;

  private List<IpaInstitutionIndex> value;
}
