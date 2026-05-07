package it.pagopa.selfcare.dashboard.model.user_groups;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class PlainUserResource {

    @Schema(description = "${swagger.dashboard.user.model.id}")
    private UUID id;
    @Schema(description = "${swagger.dashboard.user.model.name}")
    private String name;
    @Schema(description = "${swagger.dashboard.user.model.surname}")
    private String surname;

}
