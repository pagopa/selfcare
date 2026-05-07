package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class UserResource {

    @Schema(description = "${swagger.dashboard.user.model.id}")
    private UUID id;

    @Schema(description = "${swagger.dashboard.user.model.name}")
    private CertifiedFieldResource<String> name;

    @Schema(description = "${swagger.dashboard.user.model.surname}")
    private CertifiedFieldResource<String> familyName;

    @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
    private CertifiedFieldResource<String> email;

    @Schema(description = "${swagger.dashboard.user.model.institutionalPhone}")
    private CertifiedFieldResource<String> mobilePhone;

    @Schema(description = "${swagger.dashboard.user.model.fiscalCode}")
    private String fiscalCode;

    @Schema(description = "${swagger.dashboard.user.model.workContacts}")
    private Map<String, WorkContactResource> workContacts;

}
