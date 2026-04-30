package it.pagopa.selfcare.user_group.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class UserGroupResource {

    @Schema(description = "${swagger.user-group.model.id}")
    @NotBlank
    private String id;

    @Schema(description = "${swagger.user-group.model.institutionId}")
    @NotBlank
    private String institutionId;

    @Schema(description = "${swagger.user-group.model.parentInstitutionId}")
    private String parentInstitutionId;

    @Schema(description = "${swagger.user-group.model.productId}")
    @NotBlank
    private String productId;

    @Schema(description = "${swagger.user-group.model.name}")
    @NotBlank
    private String name;

    @Schema(description = "${swagger.user-group.model.description}")
    @NotBlank
    private String description;

    @Schema(description = "${swagger.user-group.model.status}")
    @NotNull
    private UserGroupStatus status;

    @Schema(description = "${swagger.user-group.model.members}")
    private List<UUID> members;

    @Schema(description = "${swagger.user-group.model.createdAt}")
    private Instant createdAt;

    @Schema(description = "${swagger.user-group.model.createdBy}")
    private String createdBy;

    @Schema(description = "${swagger.user-group.model.modifiedAt}")
    private Instant modifiedAt;

    @Schema(description = "${swagger.user-group.model.modifiedBy}")
    private String modifiedBy;

}
