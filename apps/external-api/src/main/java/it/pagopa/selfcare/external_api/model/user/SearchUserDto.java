package it.pagopa.selfcare.external_api.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class SearchUserDto {

    @Schema(description = "${swagger.external_api.user.model.fiscalCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String fiscalCode;

    @Schema(description = "${swagger.external_api.user.model.statuses}")
    private List<RelationshipState> statuses;

}
