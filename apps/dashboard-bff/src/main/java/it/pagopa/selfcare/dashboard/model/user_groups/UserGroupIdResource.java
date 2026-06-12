package it.pagopa.selfcare.dashboard.model.user_groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserGroupIdResource {

    @Schema(description = "${swagger.dashboard.user-group.model.id}")
    @NotBlank
    private String id;
}
