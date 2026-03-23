package it.pagopa.selfcare.party.registry_proxy.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class InstitutionsResource {

    @ApiModelProperty(value = "${swagger.model.institutions.items}", required = true)
    @JsonProperty(required = true)
    @NotNull
    @Valid
    private List<InstitutionResource> items;

    @ApiModelProperty(value = "${swagger.model.institutions.totalCount}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private Long count;

}
