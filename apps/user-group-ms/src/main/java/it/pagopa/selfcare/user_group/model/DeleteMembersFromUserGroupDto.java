package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class DeleteMembersFromUserGroupDto {

    @Schema(description = "${swagger.user-group.model.institutionId}")
    @JsonProperty(required = true)
    @NotBlank
    private String institutionId;

    @Schema(description = "${swagger.user-group.model.parentInstitutionId}")
    @JsonProperty(required = true)
    @NotBlank
    private String parentInstitutionId;

    @Schema(description = "${swagger.user-group.model.productId}")
    @JsonProperty(required = true)
    @NotBlank
    private String productId;

    @Schema(description = "${swagger.user-group.model.members}")
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;

}
