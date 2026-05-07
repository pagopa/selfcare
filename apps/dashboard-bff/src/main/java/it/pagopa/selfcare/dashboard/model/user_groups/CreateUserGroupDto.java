package it.pagopa.selfcare.dashboard.model.user_groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateUserGroupDto {

    @Schema(description = "${swagger.dashboard.user-group.model.institutionId}")
    @JsonProperty(required = true)
    @NotBlank
    private String institutionId;

    @Schema(description = "${swagger.dashboard.user-group.model.productId}")
    @JsonProperty(required = true)
    @NotBlank
    private String productId;

    @Schema(description = "${swagger.dashboard.user-group.model.name}")
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${swagger.dashboard.user-group.model.description}")
    @JsonProperty(required = true)
    @NotBlank
    private String description;

    @Schema(description = "${swagger.dashboard.user-group.model.members}")
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;

}
