package it.pagopa.selfcare.user_group.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class UserGroupResource {

    @ApiModelProperty(value = "${swagger.user-group.model.id}", required = true)
    @NotBlank
    private String id;

    @ApiModelProperty(value = "${swagger.user-group.model.institutionId}", required = true)
    @NotBlank
    private String institutionId;

    @ApiModelProperty(value = "${swagger.user-group.model.parentInstitutionId}")
    private String parentInstitutionId;

    @ApiModelProperty(value = "${swagger.user-group.model.productId}", required = true)
    @NotBlank
    private String productId;

    @ApiModelProperty(value = "${swagger.user-group.model.name}", required = true)
    @NotBlank
    private String name;

    @ApiModelProperty(value = "${swagger.user-group.model.description}", required = true)
    @NotBlank
    private String description;

    @ApiModelProperty(value = "${swagger.user-group.model.status}", required = true)
    @NotNull
    private UserGroupStatus status;

    @ApiModelProperty(value = "${swagger.user-group.model.members}")
    private List<UUID> members;

    @ApiModelProperty(value = "${swagger.user-group.model.createdAt}")
    private Instant createdAt;

    @ApiModelProperty(value = "${swagger.user-group.model.createdBy}")
    private String createdBy;

    @ApiModelProperty(value = "${swagger.user-group.model.modifiedAt}")
    private Instant modifiedAt;

    @ApiModelProperty(value = "${swagger.user-group.model.modifiedBy}")
    private String modifiedBy;

}
