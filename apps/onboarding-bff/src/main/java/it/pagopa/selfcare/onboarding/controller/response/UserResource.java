package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResource {

    @Schema(description = "${openapi.onboarding.user.model.id}")
    private UUID id;

    @Schema(description = "${openapi.onboarding.user.model.name}")
    private String name;

    @Schema(description = "${openapi.onboarding.user.model.surname}")
    private String surname;

    @Schema(description = "${openapi.onboarding.user.model.institutionalEmail}")
    private String email;

    @Schema(description = "${openapi.onboarding.user.model.fiscalCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.user.model.role}")
    private PartyRole role;


    @Schema(description = "${openapi.onboarding.user.model.status}")
    private String status;

    @Schema(description = "${openapi.onboarding.institutions.model.id}")
    private UUID institutionId;

}
