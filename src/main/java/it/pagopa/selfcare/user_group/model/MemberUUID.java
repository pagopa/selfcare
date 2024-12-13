package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class MemberUUID {
    @ApiModelProperty(value = "${swagger.user-group.model.memberId}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private UUID member;
}
