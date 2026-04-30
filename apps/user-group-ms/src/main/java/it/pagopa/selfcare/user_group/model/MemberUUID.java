package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MemberUUID {
    @Schema(description = "${swagger.user-group.model.memberId}")
    @JsonProperty(required = true)
    @NotNull
    private UUID member;
}
