package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

@Data
public class DeleteMembersFromUserGroupDto {

    @ApiModelProperty(value = "${swagger.user-group.model.institutionId}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String institutionId;

    @ApiModelProperty(value = "${swagger.user-group.model.parentInstitutionId}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String parentInstitutionId;

    @ApiModelProperty(value = "${swagger.user-group.model.productId}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String productId;

    @ApiModelProperty(value = "${swagger.user-group.model.members}", required = true)
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;

}
