package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class UpdateUserGroupDto {
    @Schema(description = "${swagger.user-group.model.name}")
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${swagger.user-group.model.description}")
    @JsonProperty(required = true)
    @NotBlank
    private String description;

    @Schema(description = "${swagger.user-group.model.members}")
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;
}
