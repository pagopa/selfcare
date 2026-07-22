package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResource {

    @Schema(description = "${swagger.onboarding.user.model.id}")
    private UUID id;

    @Schema(description = "${swagger.onboarding.user.model.name}")
    private String name;

    @Schema(description = "${swagger.onboarding.user.model.surname}")
    private String surname;

    @Schema(description = "${swagger.onboarding.user.model.institutionalEmail}")
    private String email;

    @Schema(description = "${swagger.onboarding.user.model.fiscalCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.user.model.role}")
    private PartyRole role;


    @Schema(description = "${swagger.onboarding.user.model.status}")
    private String status;

    @Schema(description = "${swagger.onboarding.institutions.model.id}")
    private UUID institutionId;

}
