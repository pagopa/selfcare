package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class UserIdResource {

    @Schema(description = "${swagger.dashboard.user.model.id}")
    private UUID id;
}
