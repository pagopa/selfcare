package it.pagopa.selfcare.user_group.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

@Data
public class UpdateUserGroupDto {
    @ApiModelProperty(value = "${swagger.user-group.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @ApiModelProperty(value = "${swagger.user-group.model.description}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String description;

    @ApiModelProperty(value = "${swagger.user-group.model.members}", required = true)
    @JsonProperty(required = true)
    @NotEmpty
    private Set<UUID> members;
}
