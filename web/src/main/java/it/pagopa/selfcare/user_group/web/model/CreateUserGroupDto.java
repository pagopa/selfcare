package it.pagopa.selfcare.user_group.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Data
public class CreateUserGroupDto {


    @ApiModelProperty(value = "${swagger.user-group.model.institutionId}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String institutionId;

    @ApiModelProperty(value = "${swagger.user-group.model.productId}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String productId;

    @ApiModelProperty(value = "${swagger.user-group.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @ApiModelProperty(value = "${swagger.user-group.model.description}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String description;

    @ApiModelProperty(value = "${swagger.user-group.model.status}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private UserGroupStatus status;

    @ApiModelProperty(value = "${swagger.user-group.model.members}", required = true)
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;

}
